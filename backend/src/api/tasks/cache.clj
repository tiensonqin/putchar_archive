(ns api.tasks.cache
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [api.db.user :as u]
            [api.db.post :as post]
            [taoensso.carmine :as car]
            ))

(defn rebuild
  [db]
  ;; cache all users
  (let [all-ids (j/query db ["select id from users order by id desc"])]
    (doseq [id all-ids]
      (u/cache-reload db (:id id)))))
