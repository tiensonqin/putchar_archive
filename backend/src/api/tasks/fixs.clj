(ns api.tasks.fixs
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.post :as post]
            [api.db.comment :as comment]
            [api.db.search :as search]
            [api.tasks.cache :as c]
            [clojure.string :as str]
            [taoensso.carmine :as car]
            [api.util :as au]
            [share.util :as su]
            [api.services.s3 :as s3]))

(defn rebuild-admins
  [db]
  (j/execute! db
              "alter table groups alter admins type text[]")
  (let [groups (j/query db ["select id, flake_id, admins from groups"])]
    (doseq [{:keys [id flake_id admins] :as group} groups]
      (doseq [screen_name admins]
        (cache/wcar*
         (car/zadd (cache/redis-key "user" screen_name "managed_groups") flake_id id))))))

(defn post-links
  [db]
  (let [posts (j/query db ["select * from posts"])]
    (doseq [{:keys [id permalink body link]} posts]
      (cond
        (not (su/link? body))
        (post/update db id {:link nil})

        (and (su/link? body) (nil? link))
        (do
          (prn body)
          (post/update db id {:link body}))

        :else
        nil))))


(defn update-groups-lower-case
  [db]
  (let [groups (j/query db ["select id, flake_id, admins, name from groups"])]
    (doseq [{:keys [id flake_id admins name] :as group} groups]
      (api.db.group/update db id {:name (clojure.string/lower-case name)})))

  (let [posts (j/query db ["select id, group_name from posts"])]
    (doseq [{:keys [id group_name] :as post} posts]
      (api.db.post/update db id {:group_name (clojure.string/lower-case group_name)}))))


(defn post-permalinks
  [db]
  (let [posts (j/query db ["select * from posts"])]
    (doseq [{:keys [id permalink user_screen_name]} posts]
      (post/update db id {:id id
                          :permalink (str "@" user_screen_name "/" permalink)}))))

(defn comment-post_permalink
  [db]
  (let [comments (j/query db ["select id, post_id from comments"])]
    (doseq [{:keys [id post_id]} comments]
      (let [{:keys [permalink]} (post/get db post_id)]
        (if permalink
          (comment/update db id {:post_permalink permalink})
          (do
            (prn "Wrong: without permalink, comment id: " id)
            (comment/delete db id)))))))

(defn oauth_id
  [db]
  (let [users (j/query db ["select id, oauth_type, oauth_id from users"])]
    (doseq [{:keys [id oauth_type oauth_id]} users]
      (when-let [k (case oauth_type
                     "google" :google_id
                     "twitter" :twitter_id
                     "github" :github_id
                     nil)]
        (u/update db id {k oauth_id})))

    (j/execute! db ["alter table users drop column oauth_type;"])
    (j/execute! db ["alter table users drop column oauth_id;"])))

(defn test-dash-name
  [db]
  (let [groups (j/query db ["select id, name from groups"])]
    (doseq [{:keys [id name]} groups]
      (let [new-name (-> name
                         (str/replace "__" "-")
                         (str/replace "_" "-"))]
        (when (not= new-name name)
          (prn {:name name
                :new-name new-name
                :url (str "https://d15mmb60wiwqvv.cloudfront.net/pics/"
                          name
                          "_logo.png")
                })))))

  (c/rebuild db))

(defn dash-name
  [db]
  (let [groups (j/query db ["select id, name from groups"])]
    (doseq [{:keys [id name]} groups]
      (when (re-find #"-" name)
        (prn
         (s3/save-url-image (str name "_logo")
                            (str "https://d15mmb60wiwqvv.cloudfront.net/pics/"
                                 name
                                 ".png")
                            "png"
                            "image/png")))))

  (c/rebuild db))

(defn rebuild-search
  [db]
  (let [groups (j/query db ["select id, name from groups where del is false and privacy <> ?" "private"])]
    (doseq [group groups]
      (search/add-group group)))

  (let [posts (j/query db ["select id, group_id, title from posts where del is false and is_private is false"])]
    (doseq [post posts]
      (search/add-post post)))

  (let [users (clojure.java.jdbc/query db ["select * from users"])]
    (doseq [user users]
      (api.db.search/add-user user))))

(defn stars-screen-name
  [db]
  (let [stars (j/query db ["select id, user_id from stars"])]
    (doseq [{:keys [user_id id]} stars]
      (when-let [screen-name (:screen_name (u/get db user_id))]
        (util/update db :stars id {:screen_name screen-name})))))

(defn compute-frequent-posters
  [db]
  (let [comments (j/query db ["select user_id, post_id from comments"])]
    (doseq [{:keys [user_id post_id]} comments]
      (when-let [screen-name (:screen_name (u/get db user_id))]
        (let [post (post/get db post_id)
              posters (if-let [posters (:frequent_posters post)]
                        (let [posters (read-string posters)]
                          (assoc posters screen-name (if-let [n (get posters screen-name)]
                                                       (inc n)
                                                       1)))
                        {screen-name 1})]
          (util/update db :posts post_id {:frequent_posters (pr-str posters)}))))))

(defn delete-my-links
  [db]
  (let [posts (j/query db ["select * from posts where user_screen_name = ?" "tiensonqin"])]
    (doseq [post posts]
      (when (and
             (:body post)
             (su/link? (str/trim (:body post))))
        ;; delete
        (post/delete db (:id post))))
    ))
