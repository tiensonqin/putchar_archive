(ns api.db.resource
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]
            [share.util :as su]))

(defonce ^:private table :resources)

(defonce ^:private fields [:*])

(defonce ^:private base-map {:select fields
                             :from [table]})

;; object-pair {:object_type object-type :object_id object-id}
(defn get
  [db object-pair]
  (util/get db base-map object-pair))

(defn update
  [db object-pair m]
  (let [m (if (:tags m)
            (assoc m :tags (su/->tags (:tags m)))
            m)]
    (util/update db table object-pair (some-> m
                                              (dissoc :flake_id :object_id)
                                              (assoc :updated_at (util/sql-now))))))

(defn exists?
  [db m]
  (util/exists? db table m))

(defn get-next-object-id
  [db object-type]
  (let [current-id (or
                    (->
                     (j/query db
                       ["select object_id from resources order by object_id desc limit 1"])
                     first
                     :object_id)
                    0)]
    (inc current-id)))

;; m [:name :description :cover :tags :object_type]
(defn create
  [db m]
  (if (exists? db (select-keys m [:name :object_type]))
    :already-exists
    (let [object-id (get-next-object-id db (:object_type m))
          m (if (:tags m)
              (assoc m :tags (su/->tags (:tags m)))
              m)]
      (util/create db table (assoc m :object_id object-id)
                   :flake? true))))

(defn delete
  [db id]
  (util/update db id {:del true}))

(defn get-resources
  [db object-type cursor]
  (-> (assoc base-map
             :where [:and
                     [:= :object_type object-type]
                     [:= :del :false]])
      (util/wrap-cursor (merge cursor
                               {:order-key :stars
                                :order :desc}))
      (->> (util/query db))))
