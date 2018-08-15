(ns api.db.token
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]))

(defonce ^:private table :tokens)

(defn get-token
  [db github-id]
  (util/select-one-field db table {:github_id github-id} :token))

(defn exists?
  [db m]
  (util/exists? db table m))

(defn delete
  [db m]
  (util/delete db table m))

(defn create
  [db m]
  (let [m (select-keys m [:github_id])]
    (if (exists? db m) (delete db m)))
  (util/create db table m))
