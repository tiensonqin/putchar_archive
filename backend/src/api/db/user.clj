(ns api.db.user
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [honeysql.core :as sql]
            [api.db.refresh-token :as token]
            [api.db.search :as search]
            [api.jwt :as jwt]
            [api.cookie :as cookie]
            [taoensso.carmine :as car]
            [api.util]
            [share.util :as su]
            [api.services.slack :as slack]
            [clj-time.core :as ct]
            [clj-time.coerce :as cc]
            [api.db.util :as du]))

;; karma rules
;; post tops + comments tops

(defonce ^:private table :users)
(def ^:private fields [:id :name :screen_name :email :language :website :bio :karma :type
                       :github_id :stared_groups :stared_channels :created_at :github_handle :github_repo :twitter_handle :last_seen_at :email_notification])

(def ^:private base-map {:select fields
                         :from [table]})

(defonce pro-members (atom {}))

(defn load-pro!
  [db]
  (reset! pro-members
          (let [users (j/query db ["select screen_name from users where block is false and type = ? order by created_at desc" "pro"])]
            (set (map :screen_name users)))))

(defn get-user-stared-channels
  [db ids]
  (if (seq ids)
    (util/query db {:from [:channels]
                    :select [:id :name :group_id :purpose :is_private]
                    :where [:in :id ids]})))

;; with channels
(defn get-user-stared-groups
  [db id-or-user]
  (let [{:keys [stared_channels stared_groups]} (if (map? id-or-user)
                                                  id-or-user
                                                  (util/get db base-map id-or-user))]
    (when (seq stared_groups)
      (let [channels (get-user-stared-channels db stared_channels)
            groups (su/normalize
                    (util/query db {:from [:groups]
                                    :select [:id :name :purpose :privacy]
                                    :where [:in :id stared_groups]}))]
        (for [group_id (take 20 stared_groups)]
          (-> (clojure.core/get groups group_id)
              (assoc :channels (filter #(= (:group_id %) group_id) channels))))))))

(defn db-get
  [db id]
  (when-let [user (util/get db base-map id)]
    (assoc user :stared_groups
           (get-user-stared-groups db (:id user)))))

(defn cache-reload
  [db id]
  (cache/reload table id (partial db-get db) false :all))

(defn get
  "Get user's info by id."
  ([db id]
   (get db id :all))
  ([db id keys]
   (let [id (if (uuid? id) id
                (and (string? id)
                     (util/get-id-by-field db table {:screen_name id})))]
     (cache/get table id (partial db-get db) false keys))))

(defn get-by-email
  "Get user's info by email."
  [db email]
  (let [id (util/get-id-by-field db table {:email email})]
    (cache/get table id (partial db-get db) false :all)))

(defn get-by-github-id
  "Get user's info by github-id."
  [db github-id]
  (let [id (util/get-id-by-field db table {:github-id github-id})]
    (util/get db {:select [:*]
                  :from [table]} id)))

(defn create
  [db m]
  (let [m (dissoc m :id)
        m (clojure.core/update m :screen_name str/lower-case)]
    (when-let [result (util/create db table m :flake? true)]
      (search/add-user result)
      (get db (:id result)))))

(defn update
  [db id m]
  (let [result (util/update db table id (dissoc m :screen_name :block :flake_id))]
    (cache-reload db id)
    (get db id)))

(defn oauth-authenticate
  [db type id]
  (let [key (case type
              :github :github_id)]
    (db-get db {key id})))

;; expiration: 24 hours
(defn get-code
  [email]
  (if-let [v (cache/wcar*
              (car/get email))]
    v
    (let [code (-> (str (api.util/flake-id->str) (api.util/uuid))
                   (str/replace "-" ""))]
      (cache/wcar*
       (car/set email code)
       (car/expire email (* 24 3600))
       (car/set code email)
       (car/expire code (* 24 3600)))
      code)))

(defn validate-code
  [db code]
  (cache/wcar*
   (car/get code)))

(defn generate-tokens
  [db user]
  (cookie/token-cookie
   {:access-token  (jwt/sign (select-keys user [:id :screen_name]))
    :refresh-token (token/create db (:id user))}))

(defn block
  [db id]
  (util/update db table id {:block true}))

(defn unblock
  [db id]
  (util/update db table id {:block false}))

(defn new-users
  [cursor]
  (-> base-map
      (util/wrap-cursor cursor)))

(defn messages
  [id cursor]
  (-> {:select [:*]
       :from [:messages]
       :where [:= :user_id id]}
      (util/wrap-cursor cursor)))

(defn- me-rule
  [id table]
  {:select [:*]
   :from [table]
   :where [:and
           [:= :user_id id]]})

(defn posts
  [id cursor]
  (-> (me-rule id :posts)
      (util/wrap-cursor cursor)))

(defn comments
  [id cursor]
  (-> (me-rule id :comments)
      (util/wrap-cursor cursor)))

(defn inc-karma
  [db id]
  (util/execute! db
                 {:update table
                  :where [:= :id id]
                  :inc :karma})
  (cache-reload db id))

(defn dec-karma
  [db id]
  (util/execute! db
                 {:update table
                  :where [:= :id id]
                  :dec :karma})
  (cache-reload db id))

(defn top-group
  [db id group-id]
  (let [stared-groups (util/select-one-field db table id :stared_groups)
        new-groups (->> (remove #{ group-id } stared-groups)
                        (concat [group-id])
                        (vec))]
    (update db id {:stared_groups new-groups})))

(defn make-pro
  [db uid]
  (update db uid {:type "pro"})
  (when-let [user (get db uid)]
    (reset! pro-members
            (set (conj @pro-members (:screen_name user))))))

(defn get-github-path
  [db user-id post-id]
  (when-let [repo-map (util/select-one-field db table user-id :github_repo_map)]
    (let [repo-map (read-string repo-map)]
      (-> (su/get-first-true (fn [[k v]]
               (= v post-id))
                repo-map)
          (first)))))

(defn github-add-path
  [db user-id repo-map path post-id]
  (let [repo-map (if repo-map repo-map {})]
    (util/update db table user-id
                 {:github_repo_map (pr-str (assoc repo-map path post-id))})))

(defn github-delete-path
  [db id repo-map path]
  (let [repo-map (if repo-map repo-map {})]
    (util/update db table id
                 {:github_repo_map (pr-str (dissoc repo-map path))})))

(defn github-rename-path
  [db id repo-map from-path to-path]
  (let [repo-map (if repo-map repo-map {})]
    (util/update db table id
                 {:github_repo_map (pr-str (if-let [id (clojure.core/get repo-map from-path)]
                                             (-> repo-map
                                                 (dissoc from-path)
                                                 (assoc to-path id))
                                             (do
                                               (slack/error "from-path lost: " from-path ".\n User id: " id)
                                               repo-map)))})))

(defn validate-screen-names
  [db screen-names]
  (some->>
   (util/query db
     {:select [:screen_name]
      :from [:users]
      :where [:in :screen_name screen-names]})
   (map :screen_name)
   (seq)))


(defn get-emails-by-screen-names
  [db screen-names]
  (some->>
   (util/query db
     {:select [:email]
      :from [:users]
      :where [:in :screen_name screen-names]})
   (map :email)))

(defn filter-offline-emails
  [db emails]
  (when (seq emails)
    (let [emails (some->>
                  (util/query db
                    {:select [:email]
                     :from [:users]
                     :where [:and
                             [:in :email emails]
                             [:= :email_notification true]
                             [:>= :last_seen_at (-> (ct/minus (ct/now) (ct/minutes 10))
                                                    (du/->sql-time))]]})
                  (map :email)
                  )]
      (if (seq emails)
        (set emails)
        nil))))
