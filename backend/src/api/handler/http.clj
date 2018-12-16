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
            [api.db.util :as du]
            [api.db.refresh-token :as refresh-token]
            [api.db.token :as token]
            [api.db.search :as search]
            [api.db.cache :as cache]
            [api.db.stat :as stat]
            [api.db.notification :as notification]
            [api.handler.query :as query]
            [api.services.s3 :as s3]
            [api.services.opengraph :as opengraph]
            [api.cookie :as cookie]
            [api.jwt :as jwt]
            [share.helpers.form :as form]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [clojure.set :as set]
            [ring.util.response :as resp]
            [api.services.email :as email]
            [api.services.slack :as slack]
            [api.db.block :as block]
            [api.db.task :as task]
            [api.db.moderation-log :as mlog]
            [share.util :as su]
            [share.dicts :refer [t]]
            [share.emoji :as emoji]
            [share.content :as content]
            [cheshire.core :refer [generate-string]]
            [share.admins :as admins]
            [share.front-matter :as fm]))

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

(defmethod handle :opengraph/query [[{:keys [datasource redis]} {:keys [link] :as data}]]
  (if link
    (if-let [data (opengraph/block-query link (fn [e url]
                                                (slack/error "Opengraph link: " url ", Error: " e)))]
      {:status 200
       :body data}
      (util/bad :invalid-link))
    (util/bad :invalid-link)))

(defmethod handle :user/new [[{:keys [datasource redis]} data req]]
  (let [locale (name (su/get-locale req))]
    (j/with-db-transaction [conn datasource]
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

            {:status 200
             :body {:user user}
             :cookies (u/generate-tokens conn user)}))))))

(defmethod handle :user/update [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (u/update conn uid data))))

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

(defmethod handle :search/search [[{:keys [uid datasource redis]} data]]
  (j/with-db-connection [conn datasource]
    (util/ok (search/search {:q (:q data)} :limit (:limit data)))))

(defmethod handle :post/read [[{:keys [uid datasource redis]} data request]]
  (j/with-db-transaction [conn datasource]
    (let [post-id (:id data)]
      (stat/create conn post-id "read" (:remote-addr request))
      (util/ok {:read true}))))

(defmethod handle :post/new-link [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if (block/examine conn uid)
      (let [user (u/get conn uid)
            screen-name (:screen_name user)
            permalink (post/permalink screen-name (:title data))]
        (cond
          (and (not (str/blank? (:title data)))
               (du/exists? conn :posts [:and
                                        [:= :title (:title data)]
                                        [:= :user_id uid]]))
          (util/bad :post-title-exists)

          (and permalink
               (du/exists? conn :posts [:= :permalink permalink]))
          (util/bad :post-permalink-exists)

         (and (not (str/blank? (:link data)))
              (du/exists? conn :posts [:= :link (:link data)]))
         (util/bad :post-link-exists)

         :else
         (when-let [post (post/create conn (assoc data
                                                  :user_id uid
                                                  :permalink permalink
                                                  :is_draft false))]
           (util/ok post))))
      (util/bad "Sorry your account is disabled for now."))))

(defmethod handle :post/new [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (if (block/examine conn uid)
      (when-let [post (post/create conn (assoc data
                                               :user_id uid))]
        (when (not (:is_draft post))
          (future
            (search/add-post post)))
        (util/ok post))
      (util/bad "Sorry your account is disabled for now."))))

(defmethod handle :post/update [[{:keys [uid datasource redis]} data]]
  (let [id (:id data)]
    (j/with-db-connection [conn datasource]
      (reject-not-owner-or-admin?
       conn uid :posts id
       (fn [moderator]
         (let [{:keys [title is_draft] :as m} (fm/extract (:body data))]
           (if (and (not (str/blank? title))
                   (du/exists? conn :posts [:and
                                            [:= :title title]
                                            [:= :user_id uid]
                                            [:<> :id id]]))
            (util/bad :post-title-exists)
            (let [post (post/update conn id (dissoc data :id))]
              (when (not (:is_draft post))
                (future (search/update-post post)))
              (util/ok post)))))))))

(defmethod handle :post/delete [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (reject-not-owner-or-admin? conn uid :posts (:id data)
                                (fn [moderator]
                                  (post/delete conn (:id data) moderator nil)
                                  (util/ok {:result true})))))

(defmethod handle :post/top [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (post/top conn uid (:id data)))))

(defmethod handle :post/untop [[{:keys [uid datasource redis]} data]]
  (j/with-db-transaction [conn datasource]
    (util/ok (if-let [result (post/untop conn uid (:id data))]
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
                     offline-emails (u/filter-offline-emails conn (->> [(:email parent)]
                                                                       (remove nil?)))]
                 (when (seq offline-emails)
                   (when (contains? offline-emails (:email parent))
                     (email/send-comment [(:email parent)] (assoc data :title (format "%s replied to you on Putchar." (:screen_name user))))))
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
