(ns api.db.comment
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.util :as au]
            [api.db.user :as u]
            [api.db.post :as post]
            [api.db.like :as like]
            [api.db.notification :as notification]
            [clojure.core.async :as async]
            [share.util :as su]
            [share.dicts :refer [t]]
            [share.content :as content]))

(defonce ^:private table :comments)
(defonce ^:private fields [:*])

(defonce ^:private base-map {:select fields
                             :from [table]})

(defn get
  [db id]
  (util/get db base-map id))

;; post email notification
;; 1. default, email when someone replies or mentions
;; 2. following, email when new comment
;; 3. mute
(defn new-comment
  [db comment]
  (let [post (post/get db (:post_id comment))
        user-id (get-in comment [:user :id])
        post-user-id (get-in post [:user :id])]

    ;; notification,
    ;; 1. post user
    (when (not= post-user-id user-id)
      (notification/create post-user-id
                           {:type :new-comment
                            :post post
                            :comment comment}))

    ;; 2. parent comment
    (if (:reply_to comment)
      (when-let [parent-comment (j/query db ["select * from comments where id = ?" (:reply_to comment)])]
        (when (not (contains? (hash-set user-id post-user-id) (:user_id parent-comment)))
          (notification/create (:user_id parent-comment)
                               {:type :reply-comment
                                :post post
                                :my-comment parent-comment
                                :comment comment}))))))

(defn inc-replies-count
  [db id]
  (util/execute! db
                 {:update table
                  :where [:= :id id]
                  :inc :replies_count}))

(defn dec-replies-count
  [db id]
  (util/execute! db
                 {:update table
                  :where [:= :id id]
                  :dec :replies_count}))

(defn get-last-idx
  [db m]
  (-> (util/query db {:select [:idx]
                      :from [table]
                      :where (cond
                               (:post_id m)
                               [:= :post_id (:post_id m)]

                               :else
                               (throw (ex-info "Invalid comment" (if m m {:data nil}))))
                      :order-by {:created_at :desc}
                      :limit 1
                      })
      first
      :idx
      ))

(defn create
  [db m]
  (let [last-idx (get-last-idx db m)
        m (assoc m :idx (if last-idx (inc last-idx) 1))]
    (cond
     (:post_id m)
     (when-let [post (post/get db (:post_id m))]
       ;; update post last_reply_at
       (let [mentions (some->> (seq (content/get-mentions (:body m)))
                               (u/validate-screen-names db))]
         (when-let [comment (util/create db table (assoc m
                                                         :post_permalink (:permalink post)
                                                         :mentions mentions) :flake? true)]
          (post/inc-comments-count db (:post_id m))
          (post/update db (:id post) {:last_reply_at (util/sql-now)
                                      :last_reply_by (:screen_name (u/get db (:user_id m)))})
          (let [result (-> comment
                           (util/with :user_id #(u/get db % [:id :screen_name])))]
            (when-let [reply-id (:reply_to m)]
              (inc-replies-count db reply-id))
            (new-comment db result)
            result))))

     :else
     nil))
  )

(defn update
  [db id m]
  (util/update db table id (some-> m
                                   (dissoc :flake_id)
                                   (assoc :updated_at (util/sql-now)))))

(defn delete
  [db id]
  (when-let [comment (get db id)]
    (update db id {:del true})
    (when-let [reply-id (:reply_to comment)]
      (dec-replies-count db reply-id))
    (cond
      (:post_id comment)
      (post/dec-comments-count db (:post_id comment))

      :else
      nil
      )))

(defn query-sql
  [order-key order limit]
  (format
   "WITH RECURSIVE tree AS (
        SELECT * FROM comments
        WHERE reply_to IS NULL
        and post_id = ?
        and %s > ?

        UNION ALL

        SELECT
        \"comments\".*
        FROM \"comments\", tree
        WHERE \"comments\".reply_to = tree.id)
   SELECT * FROM tree
   ORDER BY %s %s
   limit %d
"
   order-key
   order-key
   order
   limit))

(defn get-post-comments
  [db post-id {:keys [after order limit order-key]
               :or {order-key "flake_id"
                    order "asc"
                    limit 10
                    after 0}
               :as cursor}]
  (j/query db [(query-sql order-key order limit)
               post-id
               after]))

(defn get-post-new-comments
  [db post-id cursor]
  (-> {:select [:id :flake_id :user_id :body :likes :reply_to :replies_count :created_at :idx]
       :from [table]
       :where [:and
               [:= :post_id post-id]
               [:= :del :false]
               ]}
      (util/wrap-cursor cursor)
      (->> (util/query db))
      (util/with :user_id #(u/get db % [:id :screen_name]))
      ))

(defn get-user-comments
  [db user-id cursor]
  (if user-id
    (-> {:select [:id :idx :flake_id :body :post_permalink :likes :created_at]
        :from [table]
        :where [:and
                [:= :user_id user-id]
                [:= :del false]]
        }
       (util/wrap-cursor cursor)
       (->> (util/query db)))))

(defn like
  [db uid id]
  (when-let [{:keys [likes] :as comment} (get db id)]
    (if (like/like db {:user_id uid
                       :comment_id id})
      (let [new-likes (inc likes)
            data {:likes new-likes}]
        (update db id data)
        data))))

(defn unlike
  [db uid id]
  (when-let [{:keys [likes] :as comment} (get db id)]
    (let [new-likes (if (>= likes 1) (dec likes) likes)
          data {:likes new-likes}]
      (update db id data)
      (like/unlike db {:user_id uid
                       :comment_id id})
      data)))
