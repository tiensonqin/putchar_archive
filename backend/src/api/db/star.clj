(ns api.db.star
  (:require [api.db.util :as util]
            [api.db.user :as u]
            [clojure.java.jdbc :as j]))

(defonce ^:private table :stars)

(defn star
  [db m]
  (try
    (util/create db table m :flake? true)
    (catch Exception e
      nil)))

(defn unstar
  [db m]
  (util/delete db table m))

(defn get-followers
  [db object-type object-id cursor]
  (let [members (-> {:select [:screen_name]
                     :from [table]
                     :where [:and
                             [:= :object_type object-type]
                             [:= :object_id object-id]]}
                    (util/wrap-cursor cursor)
                    (->> (util/query db)))]
    (->> members
         (map :screen_name))))
