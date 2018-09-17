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
