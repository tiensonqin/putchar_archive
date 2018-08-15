(ns api.db.block
  (:require [api.db.util :as util]
            [clj-time.core :as c]
            [clj-time.coerce :as cc]))

(defonce ^:private table :blocks)

(defn create
  [db m]
  (util/delete db table (select-keys m [:user_id :group_id]))
  (util/create db table m :flake? true))

(defn examine
  [db user_id group_id]
  (let [result (util/query db {:select [:action :created_at]
                               :from [table]
                               :where [:and
                                       [:= :user_id user_id]
                                       [:= :group_id group_id]]})]
    (if (seq result)
      (every?
       #(and (not= (:action %) "forever")
             (c/after?
              (c/plus (cc/to-date-time (:created_at %)) (c/days 3))
              (c/now)))
       result)
      true)))
