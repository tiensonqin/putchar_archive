(ns api.handler.http
  (:require [taoensso.timbre :as timbre]
            [clj-social.core :as social]
            [api.config :as config]
            [share.content :as content]
            [api.util :as util]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.channel :as channel]
            [api.db.post :as post]
            [api.db.comment :as comment]
            [api.db.report :as report]
            [api.db.util :as du]
            [api.db.refresh-token :as refresh-token]
            [api.db.token :as token]
            [api.db.search :as search]
            [api.db.cache :as cache]
            [api.db.notification :as notification]
            [api.db.posts-notification :as posts-notification]
            [api.db.bookmark :as bookmark]
            [api.handler.query :as query]
            [api.services.s3 :as s3]
            [api.cookie :as cookie]
            [api.jwt :as jwt]
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
            [share.util :as su]
            [share.dicts :refer [t]]
            [share.emoji :as emoji]
            [share.content :as content]
            [cheshire.core :refer [generate-string]]))

;; TODO: move search to graphql

(defn reject-not-owner-or-admin?
  [db uid table id ok-result]
  (let [owner? (du/owner? db uid table id)
        screen-name (:screen_name (u/get db uid))
        admin? (or
                (= screen-name "tiensonqin")
                (and (= table :groups)
                     screen-name
                     (contains? (group/get-user-managed-ids db screen-name)
                                id)))]
    (cond
      admin?
      ok-result

      (not owner?)
      {:status 401
       :message ""}

      :else
      ok-result)))

(defmulti handle last)


(defmethod handle :data/pull-emojis [[{:keys [uid datasource redis]
                                       :as context} data]]
  (util/ok @emoji/emojis))

(defmethod handle :user/poll [[{:keys [uid datasource redis]
                                       :as context} data]]
  (j/with-db-connection [conn datasource]
    (if-let [user (u/get conn uid)]
      (let [admin-groups (group/get-user-managed-ids conn (:screen_name user))]
        (util/ok
         (cond-> {:has-unread-notifications? (notification/has-unread? uid)}
           (seq admin-groups)
           (assoc :has-unread-reports? (report/has-new? admin-groups)))))
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
       :headers {"Content-Type" "application/lambdahackers-profile"
                 "Content-Disposition" "attachment; filename=lambdahackers_profile.json"}
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

(defmethod handle :user/new [[{:keys [datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (cond
      (and (:screen_name data)
           (du/exists? conn :users {:screen_name (:screen_name data)}))
      (util/bad :username-exists)

      (and (:email data)
           (du/exists? conn :users {:email (:email data)}))
      (util/bad :email-exists)

      :else
      (when-let [user (u/create conn (dissoc data :avatar))]
        ;; upload social avatar to s3
        (future
          (let [avatar (or (let [avatar (:avatar data)]
                             (and (not (str/blank? avatar))
                                  avatar))
                           (rand-nth [(str (:img-cdn config/config) "/11FAh0YZrF.jpg")
                                      (str (:img-cdn config/config) "/11FAjQ9BPF.jpg")
                                      (str (:img-cdn config/config) "/11FAjeuKAb.jpg")]))]
            (s3/save-url-image (:screen_name user) avatar)))
        (future
          (email/aws-with-credential
           (email/send-welcome (:email user)
                               (u/get-code (:email user)))))
        (future
          (slack/new (str "New user: " user)))
        {:status 200
         :body {:user user}
         :cookies (u/generate-tokens conn user)}))))

(defmethod handle :user/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (u/update conn uid data))))

(defmethod handle :user/join-groups [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (doseq [group (:groups data)]
      (try
        (let [group-name (su/internal-name group)
              group-id (group/star conn group-name uid)
              general-channel-id (channel/get-group-general-channel-id conn uid group-id)]
          (channel/star conn general-channel-id uid))
        (catch Exception e
          (slack/error e)))))
  (j/with-db-connection [conn datasource]
    (let [user (u/get conn uid)]
      (util/ok {:current user}))))

(defmethod handle :user/star [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (case (:object_type data)
      :post (post/star conn (:object_id data) uid)
      :group (do
               ;; star group
               (group/star conn (:object_id data) uid)
               ;; star general channel
               (channel/star conn (channel/get-group-general-channel-id conn uid (:object_id data)) uid))
      :channel (channel/star conn (:object_id data) uid)))
  (j/with-db-connection [conn datasource]
    (let [user (u/get conn uid)]
      (util/ok {:current user}))))

(defmethod handle :user/unstar [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (let [result (case (:object_type data)
                   :post (post/unstar conn (:object_id data) uid)
                   :group (group/unstar conn (:object_id data) uid)
                   :channel (channel/unstar conn (:object_id data) uid))]
      (util/ok {:current (u/get conn uid)}))))

(defmethod handle :user/subscribe-pro [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (let [customer (stripe/create-customer data)]
      (u/make-pro conn uid)
      (future (slack/error "Stripe new customer: " customer))
      ;; TODO: handle fail
      (util/ok {:subscribed true}))))

(defmethod handle :report/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if (block/examine conn uid (:group_id data))
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
     (report/delete-object conn data))))

(defmethod handle :report/user-action [[{:keys [uid datasource redis]} {:keys [report action]}]]
  (j/with-db-transaction [conn datasource]
    (util/ok
     (report/block-user conn uid report action))))

(defmethod handle :post/search [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (post/search conn (:q data) :limit (:limit data)))))

(defmethod handle :group/search [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (group/search conn (:q data)
                           data))))

(defmethod handle :user/search [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (search/prefix-search (:q data)
                                   (dissoc data :q)))))

;; groups
(defmethod handle :group/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if-let [group (group/create conn (assoc data :user_id uid))]
      (util/ok group))))

(defmethod handle :group/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :groups (:id data)
                                (do
                                  (group/update conn (:id data) (dissoc data :id))
                                  (util/ok data)))))

;; 1. user not exists
;; 2. user not joined
(defmethod handle :group/promote-user [[{:keys [uid datasource redis]} data]]
  (when-not (str/blank? (:screen_name data))
    (j/with-db-transaction [conn datasource]
      (reject-not-owner-or-admin? conn uid :groups (:id data)
                                  (util/->response
                                   (group/add-admin conn
                                                    (:id data)
                                                    uid
                                                    (:screen_name data)))))))

(defmethod handle :group/top [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (u/top-group conn uid (:id data)))))


(defmethod handle :channel/new [[{:keys [uid datasource redis]} data]]
  (if (block/examine datasource uid (:group_id data))
    (when-let [channel
               (j/with-db-transaction [conn datasource]
                 (channel/create conn (assoc data :user_id uid)))]
      (group/update-channels datasource channel)
      (util/ok channel))
    (util/bad "Sorry your account is disabled for now.")))

(defmethod handle :channel/delete [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :channels (:id data)
                                (do
                                  (task/delete-channel conn (:id data))
                                  (util/ok data)))))

(defmethod handle :channel/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (reject-not-owner-or-admin? conn uid :channels (:id data)
                                (do
                                  (channel/update conn (:id data) (dissoc data :id))
                                  (let [group-id (:group_id (channel/get conn (:id data)))]
                                    (group/cache-reload conn group-id))
                                  ;; invalid users
                                  (let [users (j/query conn ["select user_id from stars where object_type = 'channel' and object_id = ?" (:id data)])]
                                    (doseq [{:keys [user_id]} users]
                                      (cache/del "users" user_id)))
                                  (util/ok data)))))

;; posts
(defmethod handle :post/new-draft [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if (block/examine conn uid (:group_id data))
      (when-let [post (post/create conn (assoc data
                                               :user_id uid
                                               :is_draft true))]
        (util/ok post))
      (util/bad "Sorry your account is disabled for now."))))

(defmethod handle :post/update [[{:keys [uid datasource redis]} data]]
  (let [id (:id data)]
    (j/with-db-connection [conn datasource]
      (reject-not-owner-or-admin? conn uid :posts id
                                  (let [publish? (false? (:is_draft data))
                                        old-post (post/get conn id)
                                        data (if (and publish? (nil? (:permalink old-post)))
                                               (assoc data :permalink (post/permalink (:user_screen_name old-post)
                                                                                      (or
                                                                                       (:title data)
                                                                                       (:title old-post))))
                                               data)
                                        group-id (get-in old-post [:group :id])
                                        data (if (:body data)
                                               (assoc data
                                                      :video (content/get-first-youtube-video (:body data)))
                                               data)
                                        post (post/update conn id (dissoc data :id))]
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
                                                              ", Link: <" (str "https://lambdahackers.com/" (:permalink post))
                                                              ">.")))

                                            (when (and github_id github_repo)
                                              (when-let [token (token/get-token conn github_id)]
                                                (let [[github_handle github_repo] (su/get-github-handle-repo github_repo)
                                                      body-format (or (:body_format post) "asciidoc")
                                                      markdown? (= "markdown" body-format)
                                                      link (:link post)
                                                      [type path] (cond
                                                                    link
                                                                    [:new-link (str
                                                                                (if-let [group-name (:group_name post)]
                                                                                  (str group-name "/links.adoc")
                                                                                  "links.adoc"))]
                                                                    :else
                                                                    (if-let [path (u/get-github-path conn uid (:id post))]
                                                                      [:update path]
                                                                      [:new (str
                                                                             (if-let [group-name (:group_name post)]
                                                                               (str group-name "/"))
                                                                             (:title post)
                                                                             (if markdown? ".md" ".adoc"))]))]
                                                  (if link
                                                    (let [content (contents/get github_handle github_repo path)]
                                                      (let [{:keys [encoding content] :as cont} content]
                                                        (if (= encoding "base64") ; exists
                                                          (let [old-content (github/base64-decode
                                                                             (str/replace content "\n" ""))]
                                                            ;; commit
                                                            (let [commit (commit/auto-commit github_handle github_repo
                                                                                             path
                                                                                             (str old-content
                                                                                                  "\n* "
                                                                                                  (:title post)
                                                                                                  " +\n"
                                                                                                  link)
                                                                                             "utf-8"
                                                                                             (str "Add link: " link)
                                                                                             {:oauth-token token})]
                                                              (commits/add-commit (:sha commit))))

                                                          (let [commit (commit/auto-commit
                                                                        github_handle github_repo
                                                                        path
                                                                        (str
                                                                         (if-let [group-name (get-in post [:group :name])]
                                                                           (su/format
                                                                            "= %s links\n\n%s[image:%s[]]\n"
                                                                            (su/original-name group-name)
                                                                            (str "https://lambdahackers.com/" group-name)
                                                                            (su/group-logo group-name 128 128))
                                                                           "")
                                                                         "\n* "
                                                                         (:title post)

                                                                         " +\n"
                                                                         link)
                                                                        "utf-8"
                                                                        (str "Add link: " link)
                                                                        {:oauth-token token})]
                                                            (commits/add-commit (:sha commit))))))
                                                    (do
                                                      (when (:title data)
                                                        (let [old-title (:title old-post)]
                                                          (when (and old-title (not= (:title data) old-title))
                                                            (let [old-path (str
                                                                            (if-let [group-name (:group_name old-post)]
                                                                              (str group-name "/"))
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

                                                        (u/github-add-path conn uid repo-map path (:id post)))))))))
                                          ))

                                      (or (:title data) (:body data))
                                      (future (search/update-post post))

                                      :else
                                      nil)
                                    (util/ok post))))))

(defmethod handle :post/delete [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :posts (:id data)
                                (do
                                  (post/delete conn (:id data))
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

(defmethod handle :post/vote-choice [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (post/poll-choose conn (assoc data
                                  :user_id uid)))
  (util/ok data))

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

(defmethod handle :post/set-notification-level [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (posts-notification/set-notification conn
                                           {:permalink (:permalink data)
                                            :email (:email (u/get conn uid))
                                            :level (:level data)}
                                           ))))

(defmethod handle :post/view [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (post/view conn uid (:id data))
    (util/ok {:result true})))

(defmethod handle :comment/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (let [post (du/get conn {:select [:title :permalink]
                             :from [:posts]}
                       (:post_id data))
          group-id (-> (j/query conn ["select group_id from posts where id = ?" (:post_id data)])
                       first
                       :group_id)
          user (u/get conn uid [:email :screen_name])]
      (if (block/examine conn uid (:group_id data))
        (let [comment (comment/create conn (assoc data :user_id uid))]
          (do
            (future
             ;; only send email if user is offline.
             (j/with-db-connection [conn datasource]
               (let [exclude-emails (atom #{})
                     data {
                           :post-title (:title post)
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
                     followers-emails  (when-let [emails (->> (posts-notification/get-watched-emails (:permalink post))
                                                              (remove (set @exclude-emails))
                                                              (seq))]
                                         emails)
                     offline-emails (u/filter-offline-emails conn (->> (concat [(:email parent)] mention-emails followers-emails)
                                                                       (remove nil?)))
                     muted-emails (posts-notification/get-muted-emails (:permalink post))
                     offline-emails (set/difference offline-emails muted-emails)]
                 (when (seq offline-emails)
                   (when (contains? offline-emails (:email parent))
                     (email/send-comment [(:email parent)] (assoc data :title (format "%s replied to you on Lambdahackers." (:screen_name user)))))

                   (when-let [mention-emails (seq (set/intersection offline-emails (set mention-emails)))]
                     (email/send-comment (vec mention-emails) (assoc data :title (format "%s mentions you on Lambdahackers." (:screen_name user)))))

                   (when-let [followers-emails (seq (set/intersection offline-emails (set followers-emails)))]
                     (email/send-comment (vec followers-emails) (assoc data :title (str "New comment on " (:title post))))))
                 (reset! exclude-emails nil))))
            (util/ok comment)))
        (util/bad "Sorry your account is disabled for now.")))))

(defmethod handle :comment/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :comments (:id data)
                                (do
                                  (comment/update conn (:id data) (dissoc data :id))
                                  (util/ok data)))))

(defmethod handle :comment/delete [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :comments (:id data)
                                (do
                                  (comment/delete conn (:id data))
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
