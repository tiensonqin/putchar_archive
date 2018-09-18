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

(defn get-book-followers
  [db book-id cursor]
  (let [members (-> {:select [:screen_name]
                     :from [table]
                     :where [:and
                             [:= :object_type "book"]
                             [:= :object_id book-id]]}
                    (util/wrap-cursor cursor)
                    (->> (util/query db)))]
    (->> members
         (remove (fn [x]
                   (nil? (:screen_name x)))))))

(defn get-paper-followers
  [db paper-id cursor]
  (let [members (-> {:select [:screen_name]
                     :from [table]
                     :where [:and
                             [:= :object_type "paper"]
                             [:= :object_id paper-id]]}
                    (util/wrap-cursor cursor)
                    (->> (util/query db)))]
    (->> members
         (remove (fn [x]
                   (nil? (:screen_name x)))))))
