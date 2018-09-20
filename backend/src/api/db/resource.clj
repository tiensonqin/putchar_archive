(ns api.db.resource
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]
            [share.util :as su]
            [api.db.user :as u]
            [api.db.star :as star]))

(defonce ^:private table :resources)

(defonce ^:private fields [:*])

(defonce ^:private base-map {:select fields
                             :from [table]})

;; object-pair {:object_type object-type :object_id object-id}
(defn get
  [db object-pair]
  (when-let [resource (util/get db base-map object-pair)]
    (assoc resource :followers
           (star/get-followers db
                             (:object_type object-pair)
                             (:object_id object-pair)
                             {:limit 20}))))

(defn update
  [db object-pair m]
  (let [m (if (:tags m)
            (assoc m :tags (vec (su/->tags (:tags m))))
            m)]
    (first (j/update! db table (some-> m
                                 (dissoc :flake_id :object_id)
                                 (assoc :updated_at (util/sql-now)))
                ["object_type = ? and object_id = ?"
                 (:object_type object-pair)
                 (:object_id object-pair)]))))

(defn exists?
  [db m]
  (util/exists? db table m))

(defn get-next-object-id
  [db object-type]
  (let [current-id (or
                    (->
                     (j/query db
                       ["select object_id from resources where object_type = ? order by object_id desc limit 1"
                        object-type])
                     first
                     :object_id)
                    0)]
    (inc current-id)))

(defn get-resources
  ([db object-type cursor]
   (get-resources db object-type cursor fields))
  ([db object-type cursor fields]
   (-> (assoc {:from [table]}
              :select fields
              :where [:and
                      [:= :object_type object-type]
                      [:= :del :false]])
       (util/wrap-cursor cursor)
       (->> (util/query db)))))

(defn star
  [db object-type object-id user-id]
  (when-let [user (u/get db user-id)]
    (let [object-id (int object-id)
          m {:user_id user-id
             :screen_name (:screen_name user)
             :object_type object-type
             :object_id object-id}]
      (when-not (util/exists? db :stars m)
        (star/star db m)

        (j/execute! db ["update resources set stars = stars + 1 where object_type = ? and object_id = ?" object-type object-id]))

      (case object-type
        "book"
        (j/execute! db ["update users set stared_books = stared_books || ? where id = ?"
                        object-id user-id])
        "paper"
        (j/execute! db ["update users set stared_papers = stared_papers || ? where id = ?;"
                        object-id user-id])
        nil)

      (u/cache-reload db user-id))))

(defn unstar
  [db object-type object-id user-id]
  (let [object-id (int object-id)
        m {:user_id user-id
           :object_type object-type
           :object_id object-id}]
    (when (util/exists? db :stars m)
      (star/unstar db m)
      (j/execute! db ["update resources set stars = stars - 1 where object_type = ? and object_id = ?" object-type object-id])

      (case object-type
        "book"
        (j/execute! db ["update users set stared_books = array_remove(stared_books, ?) where id = ?"
                        object-id user-id])
        "paper"
        (j/execute! db ["update users set stared_papers = array_remove(stared_papers, ?) where id = ?"
                        object-id user-id])
        nil)

      (u/cache-reload db user-id))))

(defn create
  [db m]
  (if (exists? db (select-keys m [:title :object_type]))
    :already-exists
    (let [object-id (get-next-object-id db (:object_type m))
          m (if (:tags m)
              (assoc m :tags (su/->tags (:tags m)))
              m)
          result (util/create db table (assoc m :object_id object-id)
                              :flake? true)]
      (star db (:object_type result) (:object_id result) (:user_id result))
      result)))

(defn delete
  [db id]
  (when-let [resource (get db id)]
    (let [result (util/update db id {:del true})]
      (unstar db (:object_type resource) (:object_id resource) (:user_id resource))
     result)))
