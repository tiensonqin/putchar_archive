(ns api.tasks.fixs
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [api.db.user :as u]
            [api.db.post :as post]
            [api.db.comment :as comment]
            [api.db.search :as search]
            [api.tasks.cache :as c]
            [clojure.string :as str]
            [taoensso.carmine :as car]
            [api.util :as au]
            [share.util :as su]
            [share.config :as config]
            [api.services.s3 :as s3]
            [clojure.java.shell :as shell]))

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
            (comment/delete db id false)))))))

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

(defn rebuild-search
  [db]
  (let [posts (j/query db ["select id, title from posts where is_draft is false"])]
    (doseq [post posts]
      (search/add-post post)))

  (let [users (clojure.java.jdbc/query db ["select * from users"])]
    (doseq [user users]
      (api.db.search/add-user user)))
  (let [resources (clojure.java.jdbc/query db ["select * from resources where del is false"])]
    (doseq [resource resources]
      (cond
        (= (:object_type resource) "book")
        (api.db.search/add-book resource)
        (= (:object_type resource) "paper")
        (api.db.search/add-paper resource)
        :else
        nil))))

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
        (post/delete db (:id post) false)))
    ))

(defn change-text
  [s]
  (if s
    (-> s
        (str/replace "lambdahackers.imgix.net" "img.putchar.org")
        (str/replace "lambdahackers" "putchar"))))

(defn lambdahackers->putchar
  [db]
  (let [posts (j/query db ["select * from posts"])]
    (doseq [{:keys [id cover body] :as post} posts]
      (post/update db id {:cover (change-text cover)
                          :body (change-text body)}))))

(defn images
  []
  (let [images (read-string (slurp "/home/tienson/images"))]
    (doseq [image images]
     (shell/sh "wget" "-P" "/home/tienson/photos" ))))

(defn links
  [db]
  (let [posts (j/query db ["select * from posts"])]
    (doseq [{:keys [id body] :as post} posts]
      (if-let [link (post/extract-link body)]
        (post/update db id {:link link})))))

(defn generate-post-body-html
  [db]
  (let [posts (j/query db ["select * from posts"])]
    (doseq [{:keys [id body body_format] :as post} posts]
      (post/update db id {:body_html (post/body->html body body_format)}))))

(defn post-images
  [db]
  (let [posts (j/query db ["select * from posts"])]
    (doseq [{:keys [id body body_format] :as post} posts]
      (when body
        (let [body (str/replace body
                                (str config/img-cdn "/pics/pics")
                                (str config/img-cdn "/pics"))]
         (post/update db id {:body (post/body->html body body_format)
                             :body_html (post/body->html body body_format)}))))))
