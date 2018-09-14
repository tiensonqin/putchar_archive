(ns api.handler.http
  (:require [taoensso.timbre :as timbre]
            [clj-social.core :as social]
            [api.config :as config]
            [share.content :as content]
            [api.util :as util]
            [api.db.user :as u]
            [api.db.post :as post]
            [api.db.comment :as comment]
            [api.db.report :as report]
            [api.db.repo :as repo]
            [api.db.util :as du]
            [api.db.refresh-token :as refresh-token]
            [api.db.token :as token]
            [api.db.search :as search]
            [api.db.cache :as cache]
            [api.db.stat :as stat]
            [api.db.notification :as notification]
            [api.db.bookmark :as bookmark]
            [api.handler.query :as query]
            [api.services.s3 :as s3]
            [api.cookie :as cookie]
            [api.jwt :as jwt]
            [share.helpers.form :as form]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [clojure.set :as set]
            [ring.util.response :as resp]
            [api.services.email :as email]
            [api.services.slack :as slack]
            [api.services.stripe :as stripe]
            [api.services.github :as github]
            [api.services.github.commit :as commit]
            [api.services.github.contents :as contents]
            [api.services.github.sync :as sync]
            [api.services.commits :as commits]
            [api.db.block :as block]
            [api.db.task :as task]
            [api.db.moderation-log :as mlog]
            [share.util :as su]
            [share.dicts :refer [t]]
            [share.emoji :as emoji]
            [share.content :as content]
            [cheshire.core :refer [generate-string]]
            [share.admins :as admins]))

;; TODO: move search to graphql

(defn reject-not-owner-or-admin?
  [db uid table id ok-result]
  (let [owner? (du/owner? db uid table id)
        screen-name (:screen_name (u/get db uid))
        admin? (admins/admin? screen-name)]
    (cond
      owner?
      (ok-result false)

      admin?
      (ok-result screen-name)

      (not owner?)
      {:status 401
       :message ""}

      :else
      (ok-result false))))

(defmulti handle last)


(defmethod handle :data/pull-emojis [[{:keys [uid datasource redis]
                                       :as context} data]]
  (util/ok @emoji/emojis))

(defmethod handle :user/poll [[{:keys [uid datasource redis]
                                       :as context} data]]
  (j/with-db-connection [conn datasource]
    (if-let [user (u/get conn uid)]
      (let [admin? (admins/admin? (:screen_name user))]
        (util/ok
         (cond-> {:has-unread-notifications? (notification/has-unread? uid)}
           admin?
           (assoc :has-unread-reports? (report/has-new? conn (:screen_name user))))))
      (util/bad {:error "something is wrong."}))))

(defmethod handle :user/get-current [[{:keys [uid datasource redis]
                                       :as context} data]]
  (j/with-db-connection [conn datasource]
    (util/ok
     (query/get-current-user context data))))

(defmethod handle :user/send-invites [[{:keys [uid datasource redis]
                                        :as context} data]]
  (future
    (j/with-db-connection [conn datasource]
      (email/send-invite conn (:to data)
                         (dissoc data :to))))
  (util/ok {:messge "sent"}))

(defmethod handle :user/export [[{:keys [uid datasource redis]
                                  :as context} data]]
  (j/with-db-connection [conn datasource]
    (let [content (-> (task/export conn uid)
                      (generate-string))]
      {:status 200
       :headers {"Content-Type" "application/putchar-profile"
                 "Content-Disposition" "attachment; filename=putchar_profile.json"}
       :body content})))


(defmethod handle :user/delete-account [[{:keys [uid datasource redis]
                                          :as context} data]]
  (j/with-db-connection [conn datasource]
    (util/ok
     (query/get-current-user context data))))

(defmethod handle :user/mark-notifications-as-read [[{:keys [uid datasource redis]} data]]
  (notification/mark-all-as-read uid)
  (util/ok {:marked true}))

(defmethod handle :auth/request-code [[{:keys [datasource redis]} {:keys [email] :as data}]]
  (if email
    (let [code (u/get-code email)]
      (future (email/send-welcome email code))
      {:status 200
       :body {:result true}})
    (util/bad :invalid-email)))

(defmethod handle :user/new [[{:keys [datasource redis]} data req]]
  (let [locale (name (su/get-locale req))]
    (j/with-db-transaction [conn datasource]
     (let [github-sync? (:setup-github-sync? data)
           data (dissoc data :setup-github-sync?)]
       (cond
         (and (:screen_name data)
              (du/exists? conn :users {:screen_name (:screen_name data)}))
         (util/bad :username-exists)

         (and (:email data)
              (du/exists? conn :users {:email (:email data)}))
         (util/bad :email-exists)

         (not (form/email? (:email data)))
         (util/bad :invalid-email)

         :else
         (let [languages (vec (set ["en" locale]))]
           (when-let [user (u/create conn (-> data
                                              (assoc :languages languages)
                                              (dissoc :avatar)))]
            ;; upload social avatar to s3
            (future
              (let [avatar (or (let [avatar (:avatar data)]
                                 (and (not (str/blank? avatar))
                                      avatar))
                               (rand-nth [(str (:img-cdn config/config) "/4VuWB7D1Ht.jpg")
                                          (str (:img-cdn config/config) "/4VuXdNeNnt.jpg")]))]
                (s3/save-url-image (:screen_name user) avatar)))
            (future
              (email/aws-with-credential
               (email/send-welcome (:email user)
                                   (u/get-code (:email user)))))
            (future
              (slack/new (str "New user: " user)))

            (when github-sync?
              (future
                (j/with-db-connection [conn datasource]
                  (repo/setup! nil conn (:id user)
                               (:github_id data)
                               (:github_handle data)
                               true))))
            {:status 200
             :body {:user user}
             :cookies (u/generate-tokens conn user)})))))))

(defmethod handle :user/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (u/update conn uid data))))

(defmethod handle :user/star [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (case (:object_type data)
      :post (post/star conn (:object_id data) uid)
      (slack/error "star wrong type: " (:object_type data) uid)))
  (j/with-db-connection [conn datasource]
    (let [user (u/get conn uid)]
      (util/ok {:current user}))))

(defmethod handle :user/unstar [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (let [result (case (:object_type data)
                   :post (post/unstar conn (:object_id data) uid)
                   (slack/error "unstar wrong type: " (:object_type data) uid))]
      (util/ok {:current (u/get conn uid)}))))

(defmethod handle :report/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if (block/examine conn uid)
      (do
        (future (slack/new (str "New report: "
                                "Data: " data
                                ".")))
        (util/ok
         (report/create conn (assoc data :user_id uid))))
      (util/bad "Sorry your account is disabled for now."))))

(defmethod handle :report/ignore [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok
     (report/delete conn data))))

(defmethod handle :report/delete-object [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok
     (let [current-user (u/get conn uid)]
       (report/delete-object conn data (:screen_name current-user))))))

(defmethod handle :report/user-action [[{:keys [uid datasource redis]} {:keys [report action]}]]
  (j/with-db-transaction [conn datasource]
    (util/ok
     (report/block-user conn uid report action))))

(defmethod handle :post/search [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (post/search conn (:q data) :limit (:limit data)))))

(defmethod handle :user/search [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (search/prefix-search (:q data)
                                   (dissoc data :q)))))

(defmethod handle :post/read [[{:keys [uid datasource redis]} data request]]
  (j/with-db-transaction [conn datasource]
    (let [post-id (:id data)]
      (stat/create conn post-id "read" (:remote-addr request))
      (util/ok {:read true}))))

(defmethod handle :post/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if (block/examine conn uid)
      (when-let [post (post/create conn (assoc data
                                               :user_id uid))]
        (util/ok post))
      (util/bad "Sorry your account is disabled for now."))))

(defmethod handle :post/update [[{:keys [uid datasource redis]} data]]
  (let [id (:id data)]
    (j/with-db-connection [conn datasource]
      (reject-not-owner-or-admin?
       conn uid :posts id
       (fn [moderator]
         (if (and (not (str/blank? (:title data)))
                 (du/exists? conn :posts [:and
                                          [:= :title (:title data)]
                                          [:= :user_id uid]
                                          [:<> :id id]]))
          (util/bad :post-title-exists)
          (let [publish? (false? (:is_draft data))
                old-post (post/get conn id)
                diff (su/map-difference (select-keys data [:title :body :tags :non_tech :lang])
                                        (select-keys old-post [:title :body :tags :non_tech :lang]))
                data (if (and publish? (nil? (:permalink old-post)))
                       (assoc data :permalink (post/permalink (:user_screen_name old-post)
                                                              (or
                                                               (:title data)
                                                               (:title old-post))))
                       data)
                data (if (:body data)
                       (assoc data
                              :video (content/get-first-youtube-video (:body data)))
                       data)
                post (post/update conn id (dissoc data :id))]
            (when (and moderator (seq diff))
              (mlog/create conn {:moderator moderator
                                 :post_permalink (:permalink old-post)
                                 :type "Post Update"
                                 :data {:new diff
                                        :old (select-keys old-post (vec (keys diff)) )}}))
            (cond
              (and (or
                    (:title data)
                    (:body data)) publish?)
              (future
                (search/add-post post)

                (j/with-db-connection [conn datasource]
                  (let [post (post/get conn id)
                        {:keys [github_id github_repo]} (u/get conn uid)
                        repo-map (some-> (du/select-one-field conn :users uid :github_repo_map)
                                         (read-string))]

                    (if publish?
                      (slack/new (str "New post: "
                                      "Title: " (:title data)
                                      ", Link: <" (str "https://putchar.org/" (:permalink post))
                                      ">.")))

                    (when (and github_id github_repo)
                      (when-let [token (token/get-token conn github_id)]
                        (let [[github_handle github_repo] (su/get-github-handle-repo github_repo)
                              body-format (or (:body_format post) "asciidoc")
                              markdown? (= "markdown" body-format)
                              [type path] (if-let [path (u/get-github-path conn uid (:id post))]
                                            [:update path]
                                            [:new (str
                                                   (:title post)
                                                   (if markdown? ".md" ".adoc"))])]
                          (do
                            (when (:title data)
                              (let [old-title (:title old-post)]
                                (when (and old-title (not= (:title data) old-title))
                                  (let [old-path (str
                                                  old-title
                                                  (if (= "markdown" (or (:body_format old-post) "asciidoc"))
                                                    ".md"
                                                    ".adoc"))
                                        result (commit/delete github_handle github_repo
                                                              old-path
                                                              (str "Delete post: " (:title post))
                                                              {:oauth-token token})]
                                    (when-let [commit-id (get-in result [:commit "sha"])]
                                      (commits/add-commit commit-id)
                                      (if repo-map
                                        (u/github-delete-path conn uid repo-map old-path)))))))

                            (let [commit (commit/auto-commit github_handle github_repo
                                                             path
                                                             (str
                                                              (if markdown? "# " "= ")
                                                              (:title post)
                                                              "\n\n"

                                                              (:body post))
                                                             "utf-8"
                                                             (str (if (= type :update)
                                                                    "Update"
                                                                    "New")
                                                                  " post: " (:title post))
                                                             {:oauth-token token})]
                              (commits/add-commit (:sha commit))

                              (u/github-add-path conn uid repo-map path (:id post))))))))
                  ))

              (or (:title data) (:body data))
              (future (search/update-post post))

              :else
              nil)
            (util/ok post))))))))

(defmethod handle :post/delete [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :posts (:id data)
                                (fn [moderator]
                                  (post/delete conn (:id data) moderator nil)

                                  (future
                                    (j/with-db-connection [conn datasource]
                                      (let [post (post/get conn (:id data))
                                            {:keys [github_handle github_id github_repo]} (u/get conn uid)]
                                        (when (and github_id github_repo)
                                          (when-let [token (token/get-token conn github_id)]
                                            (let [[github_handle github_repo] (su/get-github-handle-repo github_repo)
                                                  repo-map (du/select-one-field conn :users uid :github_repo_map)
                                                  path (u/get-github-path conn uid (:id post))
                                                  result (commit/delete github_handle github_repo
                                                                        path
                                                                        (str "Delete post: " (:title post))
                                                                        {:oauth-token token})]
                                              (slack/debug "delete result: " result)
                                              (when-let [commit-id (get-in result [:commit "sha"])]
                                                (commits/add-commit commit-id)
                                                (u/github-delete-path conn uid repo-map path))))))))
                                  (util/ok {:result true})))))

(defmethod handle :post/top [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (post/top conn uid (:id data)))))

(defmethod handle :post/untop [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (if-let [result (post/untop conn uid (:id data))]
               result
               {}))))

(defmethod handle :post/bookmark [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (bookmark/bookmark conn {:user_id uid
                                      :post_id (:id data)}))))

(defmethod handle :post/unbookmark [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (if-let [result (bookmark/unbookmark conn {:user_id uid
                                                        :post_id (:id data)})]
               result
               {}))))

(defmethod handle :post/view [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (post/view conn uid (:id data))
    (util/ok {:result true})))

(defmethod handle :comment/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (let [post (du/get conn {:select [:title :permalink]
                             :from [:posts]}
                       (:post_id data))
          user (u/get conn uid [:email :screen_name])]
      (if (block/examine conn uid)
        (let [comment (comment/create conn (assoc data :user_id uid))]
          (do
            (future
             ;; only send email if user is offline.
             (j/with-db-connection [conn datasource]
               (let [exclude-emails (atom #{})
                     data {:post-title (:title post)
                           :screen_name (:screen_name user)
                           :body (:body comment)
                           :created_at (su/date-format (:created_at comment))
                           :comment_url (su/comment-link post (:idx comment))}
                     _ (swap! exclude-emails conj (:email user))
                     parent (when-let [reply_to (:reply_to comment)]
                              (when-let [user-id (du/select-one-field conn :comments (:reply_to comment) :user_id)]
                                (when (not= user-id uid)
                                  (when-let [user (u/get conn user-id)]
                                    (swap! exclude-emails conj (:email user))
                                    user))))
                     mention-emails (when-let [mentions (:mentions comment)]
                                      (when-let [emails (->> (u/get-emails-by-screen-names conn mentions)
                                                             (remove (set @exclude-emails))
                                                             (seq))]
                                        (swap! exclude-emails concat emails)
                                        emails))
                     offline-emails (u/filter-offline-emails conn (->> (concat [(:email parent)] mention-emails)
                                                                       (remove nil?)))]
                 (when (seq offline-emails)
                   (when (contains? offline-emails (:email parent))
                     (email/send-comment [(:email parent)] (assoc data :title (format "%s replied to you on Putchar." (:screen_name user)))))

                   (when-let [mention-emails (seq (set/intersection offline-emails (set mention-emails)))]
                     (email/send-comment (vec mention-emails) (assoc data :title (format "%s mentions you on Putchar." (:screen_name user))))))
                 (reset! exclude-emails nil))))
            (util/ok comment)))
        (util/bad "Sorry your account is disabled for now.")))))

(defmethod handle :comment/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :comments (:id data)
                                (fn [moderator]
                                  (when moderator
                                    (when-let [old-comment (comment/get conn (:id data))]
                                      (when (not= (:body old-comment) (:body data))
                                        (mlog/create conn {:type "Comment Update"
                                                           :moderator moderator
                                                           :post_permalink (:post_permalink old-comment)
                                                           :comment_idx (:idx old-comment)
                                                           :data {:old-body (:body old-comment)
                                                                  :new-body (:body data)}
                                                           ;; :reason reason
                                                           }))))
                                  (comment/update conn (:id data) (dissoc data :id))
                                  (util/ok data)))))

(defmethod handle :comment/delete [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :comments (:id data)
                                (fn [moderator]
                                  (comment/delete conn (:id data) moderator)
                                  (util/ok {:result true})))))

(defmethod handle :comment/like [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (comment/like conn uid (:id data))
    (util/ok {:result true})))

(defmethod handle :comment/unlike [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (comment/unlike conn uid (:id data))
    (util/ok {:result true})))

(defmethod handle :default [[_ data]]
  (util/ok data))

(defn handler
  [{:keys [context body-params]
    :as req}]
  (handle [context (second body-params) req (first body-params)]))

(defn upload
  [{:keys [context params]
    :as req}]
  (let [{:keys [tempfile size]} (:file params)]
    (if-let [name (s3/put-image {:tempfile tempfile
                                 :length  size
                                 :name (:name params)
                                 :png? (:png params)
                                 :invalidate? (:invalidate params)})]
      (util/ok {:url name})
      (util/bad "Upload failed!"))))
