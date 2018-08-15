(ns api.db.channel
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [honeysql.core :as sql]
            [api.db.user :as u]
            [api.db.star :as star]
            [clojure.string :as str]
            [share.util :as su]))

(defonce ^:private table :channels)
(defonce ^:private fields [:*])

(defonce ^:private base-map {:select fields
                             :from [table]})

(defn db-get
  [db id]
  (util/get db base-map id))

(defn cache-reload
  [db id]
  (cache/reload table id (partial db-get db) false :all))

;; get by channel id or {:group_name x :channel_name y}
(defn get
  ([db id]
   (get db id :all))
  ([db id keys]
   (let [id (if (uuid? id)
              id
              (and (map? id)
                   (util/get-id-by-field db table
                                         {:group_id (util/get-id-by-field
                                                     db
                                                     :groups
                                                     {:name (:group-name id)})
                                          :name (:channel-name id)})))]
     (cache/get table id (partial db-get db) false keys))))

(defn star
  [db channel-id user-id]
  (when-let [channel (get db channel-id)]
    (when-let [user (u/get db user-id)]
      (let [m {:user_id user-id
               :screen_name (:screen_name user)
               :object_type "channel"
               :object_id channel-id}]
        (when-not (util/exists? db :stars m)
          (star/star db m)

          (j/execute! db ["update channels set stars = stars + 1 where id = ?" channel-id])

          (j/execute! db ["update users set stared_channels = stared_channels || ? where id = ?;"
                          channel-id user-id])
          (cache-reload db channel-id)
          (u/cache-reload db user-id))))))

(defn unstar
  [db channel-id user-id]
  (let [m {:user_id user-id
           :object_type "channel"
           :object_id channel-id}]
    (when (util/exists? db :stars m)
      (star/unstar db m)
      (j/execute! db ["update channels set stars = stars - 1 where id = ?" channel-id])

      (j/execute! db ["update users set stared_channels = array_remove(stared_channels, ?) where id = ?"
                      channel-id user-id])

      (cache-reload db channel-id)
      (u/cache-reload db user-id))))

(defn create
  [db m]
  (let [m (if (:name m)
            (clojure.core/update m :name su/internal-name)
            m)]
    (when-let [channel (util/create db table m :flake? true)]
      (star db (:id channel) (:user_id m))

      channel)))

(defn update
  [db id m]
  (let [result (util/update db table id (dissoc m :del :flake_id :is_private))]
    (cache-reload db id)
    result))

(defn get-group-general-channel-id
  [db user-id group-id]
  (if-let [channel-id (util/select-one-field db table
                                             {:group_id group-id
                                              :name "general"}
                                             :id)]
    channel-id
    ;; create it
    (let [channel (create db {:user_id user-id
                              :group_id group-id})]
      (:id channel))))

(defn group-new-channels
  [group-id cursor]
  (-> base-map
      (assoc :where [:and
                     [:= :group_id group-id]
                     [:= :is_private false]])
      (util/wrap-cursor cursor)))

(defn group-hot-channels
  [group-id cursor]
  (-> base-map
      (assoc :where [:and
                     [:= :group_id group-id]
                     [:= :is_private false]])
      (util/wrap-cursor (merge cursor
                               {:order-key :stars
                                :order :desc}))))
