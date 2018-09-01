(ns api.db.star
  (:require [api.db.util :as util]
            [api.db.user :as u]
            [clojure.java.jdbc :as j]))

(defonce ^:private table :stars)

(defn star
  [db m]
  (util/create db table m :flake? true))

(defn unstar
  [db m]
  (util/delete db table m))

(defn get-group-members
  [db group-id cursor]
  (let [members (-> {:select [:screen_name]
                     :from [table]
                     :where [:and
                             [:= :object_type "group"]
                             [:= :object_id group-id]]}
                    (util/wrap-cursor cursor)
                    (->> (util/query db)))]
    (->> members
        (remove (fn [x]
                  (nil? (:screen_name x)))))))
