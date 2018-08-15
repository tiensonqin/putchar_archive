(ns api.db.choice
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]))

(defonce ^:private table :choices)

(defn get-choice-id
  [db user-id post-id]
  (util/select-one-field db table {:user_id user-id
                                   :post_id post-id} :choice_id))

(defn get-choices-ids
  [db user-id post-ids]
  (util/query db
    {:select [:post_id
              :choice_id]
     :from [table]
     :where [:and
             [:= :user_id user-id]
             [:in :post_id post-ids]]}))

(defn exists?
  [db m]
  (util/exists? db table m))

(defn delete
  [db m]
  (util/delete db table m))

(defn create
  [db m]
  (when-not (exists? db m)
    (util/create db table m :flake? true)))
