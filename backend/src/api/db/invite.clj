(ns api.db.invite
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]))

(defonce ^:private table :invites)

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
  [db]
  (let [token (new-token)
        m {:token token}]
    (if (exists? db {:token token})
      (create db)
      (util/create db table m))))
