(ns api.tasks.search
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.user :as u]
            [api.db.post :as post]
            [api.db.search :as search]))

(defn rebuild
  [db]
  (search/delete-all)
  (let [users (j/query db ["select screen_name from users"])]
    (doseq [user users]
      (search/add-user user)))

  (let [posts (j/query db ["select id, title from posts where is_draft = false"])]
    (doseq [post posts]
      (search/add-post post)))

  (let [resources (j/query db ["select object_id, object_type, title from resources"])]
    (doseq [{:keys [object_id object_type title] :as resource} resources]
      (case object_type
        "book"
        (search/add-book resource)
        "paper"
        (search/add-paper resource))))
  )
