(ns api.db.task
  (:require [api.db.user :as u]
            [api.db.post :as post]
            [api.db.comment :as comment]
            [clojure.java.jdbc :as j]
            [api.db.group :as group]
            [api.db.star :as star]
            [api.db.search :as search]
            [api.util :as util]
            [api.db.util :as du]))

(defn export
  [db user-id]
  (some-> (u/get db user-id)
          (assoc :posts (j/query db ["select group_name, title, body, tops, tags, comments_count, permalink, created_at from posts where user_id = ?" user-id])
                 :comments (j/query db ["select post_permalink, body, likes, created_at from comments where del = false and user_id = ?" user-id]))))

(defn create-identity-user
  [conn]
  (let [id (util/flake-id->str)]
    (u/create conn {:name "Deleted User"
                    :screen_name (str "deleted-user-" id)
                    :email (str id "-not-exists@putchar.org")
                    :bio "I'm no longer a putchar user."})))

(defn delete-account
  [db user-id]
  ;;
  (let [{:keys [email screen_name] :as user} (u/get db user-id)]
    (j/with-db-connection [conn db]
      (j/execute! conn ["delete from blocks where user_id = ?" user-id])
      (j/execute! conn ["delete from likes where user_id = ?" user-id])
      (j/execute! conn ["delete from refresh_tokens where user_id = ?" user-id])
      (doseq [{:keys [object_type object_id]} (j/query conn ["select * from stars where user_id = ?" user-id])]
        (case object_type
          "group"
          (group/unstar conn object_id user-id)

          nil))
      (j/execute! conn ["delete from stars where user_id = ?" user-id])

      (doseq [{:keys [post_id]} (j/query conn ["select * from tops where user_id = ?" user-id])]
        (post/untop conn user-id post_id))

      (j/execute! conn ["delete from users where id = ?" user-id])

      (let [user (create-identity-user conn)
            new-id (:id user)
            new-email (:email user)
            new-screen-name (:screen_name user)]
        (j/execute! conn ["update posts set user_id = ?, user_screen_name = ? where user_id = ?" new-id new-screen-name user-id])
        (j/execute! conn ["update comments set user_id = ? where user_id = ?" new-id user-id])
        ;; TODO: admins
        (j/execute! conn ["update groups set user_id = ? where user_id = ?" new-id user-id])
        (j/execute! conn ["update reports set user_id = ? where user_id = ?" new-id user-id])
        )

      (search/delete-user screen_name)
      )))

(defn danger-delete-account
  [db user-id]
  ;;
  (let [{:keys [email screen_name] :as user} (u/get db user-id)]
    (j/with-db-connection [conn db]
      (j/execute! conn ["delete from blocks where user_id = ?" user-id])
      (j/execute! conn ["delete from likes where user_id = ?" user-id])
      (j/execute! conn ["delete from refresh_tokens where user_id = ?" user-id])
      (doseq [{:keys [object_type object_id]} (j/query conn ["select * from stars where user_id = ?" user-id])]
        (case object_type
          "group"
          (group/unstar conn object_id user-id)

          nil))
      (j/execute! conn ["delete from stars where user_id = ?" user-id])

      (doseq [{:keys [post_id]} (j/query conn ["select * from tops where user_id = ?" user-id])]
        (post/untop conn user-id post_id))

      (j/execute! conn ["delete from users where id = ?" user-id]))))
