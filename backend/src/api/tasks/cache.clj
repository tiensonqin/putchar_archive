(ns api.tasks.cache
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.channel :as channel]
            [api.db.post :as post]
            [taoensso.carmine :as car]
            ))

(defn rebuild
  [db]
  ;; cache all users
  (let [all-ids (j/query db ["select id from users order by id desc"])]
    (doseq [id all-ids]
      (u/cache-reload db (:id id))))

  ;; cache all groups
  (let [all-ids (j/query db ["select id from groups order by id desc"])]
    (doseq [id all-ids]
      (group/cache-reload db (:id id))))

  ;; cache all channels
  (let [all-ids (j/query db ["select id from channels order by id desc"])]
    (doseq [id all-ids]
      (channel/cache-reload db (:id id))))

  )
