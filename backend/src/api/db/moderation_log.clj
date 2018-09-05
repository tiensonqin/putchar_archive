(ns api.db.moderation-log
  (:require [api.db.util :as util]))

(defonce ^:private table :moderation_logs)

(defn create
  [db m]
  (util/create db table m :flake? true))

(defn get-logs
  [db cursor]
  (-> {:select [:*]
       :from [table]}
      (util/wrap-cursor cursor)
      (->> (util/query db))))
