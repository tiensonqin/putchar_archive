(ns api.db.group
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.util :as au]
            [api.db.cache :as cache]
            [honeysql.core :as sql]
            [api.db.star :as star]
            [api.db.channel :as channel]
            [api.db.user :as u]
            [api.db.search :as search]
            [api.db.notification :as notification]
            [clj-time.core :as t]
            [clojure.string :as str]
            [share.util :refer [normalize]]
            [taoensso.carmine :as car]))

(defonce ^:private table :groups)
(def ^:private fields [:id :flake_id :name :user_id :privacy :purpose :rule :related_groups :type :stars :channels :admins :created_at :week_posts_count :cover_settings])

(def ^:private base-map {:select fields
                         :from [table]})

(defn db-get
  [db id]
  (when-let [group (util/get db base-map id)]
    (let [channels (util/get-by-ids db :channels (:channels group)
                                    {:fields [:id :name]
                                     :order :asc})]
      (assoc group :channels channels))))

(defn cache-reload
  [db id]
  (cache/reload table id (partial db-get db) false :all))

(defn get
  ([db id]
   (get db id :all))
  ([db id keys]
   (let [id (if (uuid? id) id
                (and (string? id)
                     (util/get-id-by-field db table {:name id})))
         group (cache/get table id (partial db-get db) false keys)]
     (some-> group
             (clojure.core/update :admins
                           (fn [xs]
                             (map (fn [x]
                                    {:screen_name x
                                     :pro? (contains? @u/pro-members (:screen_name x))})
                               xs)))))))

(defn star
  [db group-id user-id]
  (when-let [group (get db group-id)]
    (when-let [user (u/get db user-id)]
      (let [group-id (:id group)
            m {:user_id user-id
               :screen_name (:screen_name user)
               :object_type "group"
               :object_id group-id}]

        (when-not (util/exists? db :stars m)
          (star/star db m)

          (j/execute! db ["update groups set stars = stars + 1 where id = ?" group-id]))

        (j/execute! db ["update users set stared_groups = stared_groups || ? where id = ?;"
                        group-id user-id])

        (cache-reload db group-id)
        (u/cache-reload db user-id)
        group-id))))

(defn unstar
  [db group-id user-id]
  (let [m {:user_id user-id
           :object_type "group"
           :object_id group-id}]
    (when (util/exists? db :stars m)

      ;; unstar channels
      ;; FIX:
      (let [channels (get-in (u/get db user-id) [:stared_groups group-id])]
        (doseq [channel channels]
          (channel/unstar db (:id channel) user-id)))

      (star/unstar db m)
      (j/execute! db ["update groups set stars = stars - 1 where id = ?" group-id])

      (j/execute! db ["update users set stared_groups = array_remove(stared_groups, ?) where id = ?"
                      group-id user-id])

      (cache-reload db group-id)
      (u/cache-reload db user-id))))

(defn remove-invalid-names
  [db names]
  (let [valid-names (some->>
                     (util/query db {:select [:name]
                                     :from [table]
                                     :where [:in :name names]})
                     (mapv :name)
                     (set))]
    (filter valid-names names)))

(defn update
  [db id m]
  (let [m (cond-> m
            (seq (:related_groups m))
            (assoc :related_groups (remove-invalid-names db (:related_groups m))))
        group (get db id)
        group-name (:name group)
        result (util/update db table id (dissoc m :del :flake_id :privacy))]
    (cache-reload db id)
    result))

(defn update-channels
  [db {:keys [group_id] :as channel}]
  (let [channels (map :id (:channels (db-get db group_id)))]
    (util/update db table group_id
                 {:channels (set (conj channels (:id channel)))})
    (cache-reload db group_id)))

(defn create
  [db m]
  (let [user_id (:user_id m)
        screen_name (:screen_name (u/get db user_id))
        m (assoc m :admins [screen_name])
        m (clojure.core/update m :name str/lower-case)
        group (util/create db table m :flake? true)
        ;; create default channel
        general-channel (channel/create db {:user_id user_id
                                            :group_id (:id group)})]

    (cache/wcar*
     (car/zadd (cache/redis-key "user" screen_name "managed_groups") (:flake_id group) (:id group)))

    (star db (:id group) user_id)

    (search/add-group group)

    (update db (:id group) {:channels #{(:id general-channel)}})

    (assoc group
           :stars 1
           :channels [(select-keys general-channel [:id :user_id :name :purpose :stars :created_at])])))

(defn new-groups
  [db cursor]
  (->>
   (-> base-map
       (assoc :where [:<> :privacy "private"])
       (util/wrap-cursor cursor))
   (util/query db)))

(defn hot-groups
  [db cursor]
  (->>
   (-> base-map
       (assoc :where [:<> :privacy "private"])
       (util/wrap-cursor (merge cursor
                                {:order-key :stars
                                 :order :desc})))
   (util/query db)))

(defn search
  [db q {:keys [limit where enrich?]
         :or {limit 10
              enrich? true
              where [:and [:= :del false]]}}]
  (let [result (when-not (str/blank? (:group_name q))
                 (let [result (search/search q :limit limit)]
                   (if enrich?
                     (when (seq result)
                       (let [ids (->> (filter :group_id result)
                                      (mapv (comp au/->uuid :group_id)))
                             results (util/get-by-ids db :groups ids {:where where
                                                                      :order? false})]
                         (->>
                          (for [id ids]
                            (filter #(= (:id %) id) results))
                          (flatten)
                          (remove nil?))))
                     result)))]
    (if (seq result)
      result
      [])))

(defn get-user-managed-ids
  [db screen-name]
  (->
   (cache/wcar*
    (car/zrange (cache/redis-key "user" screen-name "managed_groups")
                0 -1))
   (set)))

(defn add-admin
  [db id who screen-name]
  (if-let [user (u/get db screen-name)]
    (if-not (contains? (set (map :id (:stared_groups user))) id)
      [:error :user-not-joined]
      (let [{:keys [admins flake_id] :as group} (get db id)
            new-admins (if admins
                         (vec (distinct (conj (mapv :screen_name admins) screen-name)))
                         [screen-name])]
        (update db id {:admins new-admins})
        (cache/wcar*
         (car/zadd (cache/redis-key "user" screen-name "managed_groups") flake_id id))
        ;; send notification
        (notification/create (:id user)
                             {:type :group-admin-promote
                              :who (u/get db who [:id :name :screen_name])
                              :group (select-keys group [:id :name])
                              :created_at (util/sql-now)})
        [:ok (set new-admins)]))
    [:error :user-not-exists]))

(defn delete
  [db id]
  ;; delete group, delete channel, delete from search, cache, stars and associated posts

  (let [users (j/query db ["select user_id from stars where object_type = 'group' and object_id = ?" id])
        channels (j/query db ["select id from channels where group_id = ?" id])]
    (doseq [{:keys [user_id]} users]
      (unstar db id user_id)
      (doseq [{:keys [id]} channels]
        (channel/unstar db id user_id))))
  (j/execute! db ["delete from channels where group_id = ?" id])
  (j/execute! db ["delete from stars where object_type = 'group' and object_id = ?" id])
  (j/execute! db ["delete from posts where group_id = ?" id])
  (cache/wcar* (car/del (cache/redis-key "group" id)))
  (search/delete-group id)
  (j/execute! db ["delete from groups where id = ?" id]))
