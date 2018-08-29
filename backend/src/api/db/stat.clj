(ns api.db.stat
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]))

(defonce ^:private table :stats)

(defn create
  [db post-id type ip]
  (let [m {:post_id post-id
           :type type
           :ip ip}]
    (when-not (util/exists? db table m)
      (util/create db table m))))

(comment
  (create user/db
          #uuid "1c580d67-7e11-4280-af1e-fc47a966ac92"
          "view"
          "127.1.1.1"))
