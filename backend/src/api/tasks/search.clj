(ns api.tasks.search
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.post :as post]
            [api.db.search :as search]))

(defn rebuild
  [db]
  (let [users (j/query db ["select screen_name from users"])]
    (doseq [user users]
      (search/add-user user)))

  (let [groups (j/query db ["select id, name from groups where del = false"])]
    (doseq [group groups]
      (search/add-group group)))

  (let [posts (j/query db ["select id, group_id, title from posts where is_draft = false and is_private = false"])]
    (doseq [post posts]
      (search/add-post post))))
