(ns api.db.invite
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]))

(defonce ^:private table :invites)

(defn get-group-name
  [db token]
  (util/select-one-field db table {:token token} :group_name))

(defn exists?
  [db m]
  (util/exists? db table m))

(defn delete
  [db m]
  (util/delete db table m))

(defn new-token
  []
  (str (uuid) "-"
       (uuid)))

(defn create
  [db group-name]
  (let [token (new-token)
        m {:group_name group-name
           :token token}]
    (if (exists? db {:token token})
      (create db group-name)
      (util/create db table m))))
