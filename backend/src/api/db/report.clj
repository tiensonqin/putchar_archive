(ns api.db.report
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.util :as au]
            [api.db.util :as util]
            [api.db.comment :as comment]
            [api.db.post :as post]
            [api.db.user :as u]
            [api.db.block :as block]
            [api.db.cache :as cache]
            [api.db.notification :as notification]
            [api.db.moderation-log :as mlog]
            [share.admins :as admins]
            [taoensso.carmine :as car]))

(defonce ^:private table :reports)

(defn get-data
  [db {:keys [object_type object_id]}]
  (case object_type
    :post (when-let [post (post/get db object_id)]
            {:post post
             :user (:user post)})
    :comment (when-let [comment (comment/get db object_id)]
               (cond
                 (:post_id comment)
                 (let [post (post/get db (:post_id comment))]
                   {:post post
                    :comment comment
                    :user (u/get db (:user_id comment) [:id :screen_name])})
                 :else
                 {:comment comment
                  :user (u/get db (:user_id comment) [:id :screen_name])}))))

(defn create
  [db m]
  (when-let [data (get-data db m)]
    (util/create db table (assoc m :data data) :flake? true)))

(defn delete
  [db report]
  (util/delete db table (:id report)))

(defn get-reports
  [db user-id cursor]
  (let [user (u/get db user-id)
        admin? (admins/admin? (:screen-name user))]
    (-> {:select [:*]
         :from [:reports]
         :where [:= :status "pending"]}
        (util/wrap-cursor cursor)
        (->> (util/query db)))))

(defn has-new?
  [db screen-name]
  (util/exists? db table
                [:= :status "pending"]))

(defn delete-object
  [db {:keys [object_type object_id data reason] :as report} moderator]
  ;; notification
  (case (name object_type)
    "post"
    (post/delete db object_id moderator reason)
    "comment"
    (comment/delete db object_id moderator reason))

  (util/update db table (:id report) {:status "ok"})

  (notification/create (get-in data [:user :id])
                       {:type :post-or-comment-deleted
                        :post (:post data)
                        :comment (:comment data)
                        :reason (:kind report)
                        :created_at (util/sql-now)}))

(defn block-user
  [db uid report action]
  (let [user-id (au/->uuid (get-in report [:data :user :id]))
        user (u/get db user-id)
        moderator (u/get db uid)]
    (block/create db {:report_id (:id report)
                      :user_id user-id
                      :action action})


    (when moderator
      (mlog/create db {:moderator (:screen_name moderator)
                       :data {:screen_name (:screen_name user)}
                       :type "User Block"
                       :reason (:reason report)}))


    (notification/create (get-in report [:data :user :id])
                         {:type :blocked
                          :action action
                          :created_at (util/sql-now)})))
