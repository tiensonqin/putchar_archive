(ns api.handler.query
  (:require [taoensso.timbre :as timbre]
            [api.config :as config]
            [api.util :as util]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.post :as post]
            [api.db.top :as top]
            [api.db.comment :as comment]
            [api.db.report :as report]
            [api.db.util :as du]
            [api.db.refresh-token :as refresh-token]
            [api.db.search :as search]
            [api.db.notification :as notification]
            [api.db.report :as report]
            [api.db.star :as star]
            [api.db.choice :as choice]
            [api.db.stat :as stat]
            [bidi.bidi :as bidi]
            [api.jwt :as jwt]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [clojure.set :as set]
            [share.util :as su]
            [share.content :as content]
            [api.services.slack :as slack]
            [share.dicts :as dicts]))

(defonce not-found (atom false))

(defn- set-not-found!
  [value]
  (if value
    value
    :not-found))


(defn wrap-end?
  [result limit]
  {:result result
   :end? (if (= limit (count result)) false true)})

;; TODO: support recurisve


;; {:user {:fields [:id :name :email
;;                  :conversations
;;                  [:posts {:filter :hot}]]}}
(defn get-current-user
  [{:keys [uid datasource]} data]
  (if uid
    (j/with-db-connection [conn datasource]
      (when-let [user (u/get conn uid)]
        (-> user
            (assoc :has-unread-notifications? (notification/has-unread? uid)))))))

(defn get-hot-groups
  [{:keys [uid datasource]} data]
  (j/with-db-connection [conn datasource]
    (group/hot-groups conn data)))

(defn get-notifications
  [{:keys [uid datasource]} data]
  (if uid
    (notification/get-user-notifications uid)))

(defn get-reports
  [{:keys [uid datasource]} data]
  (if uid
    (j/with-db-connection [conn datasource]
      (report/get-user-reports conn uid (:cursor data)))))

(defn get-stats
  [{:keys [uid datasource]} data]
  (if uid
    (j/with-db-connection [conn datasource]
      (stat/query conn uid))))

;; {:user {:fields :*
;;         :args {:id 123}}}

;; {:user {:fields :*
;;         :args {:id "screen_name"}}}
(defn get-user
  [{:keys [uid datasource]} data]
  (if-let [user (j/with-db-connection [conn datasource]
                  (u/get conn (or (:id data)
                                  (:screen_name data))))]
    (assoc user :tags (post/get-user-tags (:screen_name user)))
    :not-found))

(defn get-group
  [{:keys [uid datasource]} data]
  (->
   (j/with-db-connection [conn datasource]
     (cond
       (:id data)
       (group/get conn (:id data))

       (:name data)
       (group/get conn (str/lower-case (bidi/url-decode (:name data))))

       :else
       nil))
   set-not-found!))

(defn get-members
  [{:keys [uid datasource]} data]
  (j/with-db-connection [conn datasource]
    (star/get-group-members conn (:group_id data) (:cursor data))))

(defn get-groups
  [{:keys [uid datasource]} data]
  (j/with-db-connection [conn datasource]
    (let [f (case (:filter data)
              :new
              group/new-groups
              group/hot-groups)]
      (f conn (:cursor data)))))

;; cache
(defn get-post
  [{:keys [uid datasource]} data]
  (->
   (let [where (if (:id data)
                 {:id (:id data)}
                 {:permalink (su/encode-permalink (:permalink data))})]
     (j/with-db-connection [conn datasource]
       (let [post (post/get conn where)]
         (if post
           (let [post (if uid
                        (assoc post
                              :poll_choice (choice/get-choice-id conn uid
                                                                 (:id post)))
                        post)]
             (assoc post :body (if (:raw_body? data)
                                 (:body post)
                                 (content/render (:body post)
                                  (:body_format post)))))
           post))))
   set-not-found!))

(defn get-comments
  [{:keys [uid datasource]} data]
  (let [result (cond
                 (:post_id data)
                 (j/with-db-connection [conn datasource]
                   (comment/get-post-new-comments conn
                                                  (:post_id data)
                                                  (:cursor data)))

                 (:user_id data)
                 (some->
                  (j/with-db-connection [conn datasource]
                    (comment/get-user-comments conn (:user_id data) (:cursor data)))))]
    (wrap-end? result (get (:cursor data) :limit 10))))


(defn get-tag-posts
  [db data]
  (j/with-db-transaction [conn db]
    (post/get-tag conn (:tag data) (:cursor data))))

(defn wrap-tag-posts
  [{:keys [datasource]} data]
  (let [cursor (update (:cursor data) :limit (fn [v] (if v v 10)))
        result (get-tag-posts datasource data)]
    {:posts (wrap-end? result (get cursor :limit))
     ;; FIXME:
     :count 0}))

(defn get-user-tag-posts
  [db data]
  (j/with-db-transaction [conn db]
    (post/get-user-tag conn (:screen_name data) (:tag data) (:cursor data))))

(defn wrap-user-tag-posts
  [{:keys [datasource]} data]
  (let [result (get-user-tag-posts datasource data)
        result (some->
                (u/get datasource (:screen_name data))
                (assoc :tags (post/get-user-tags (:screen_name data)))
                (assoc :posts (wrap-end? result (get (:cursor data) :limit 10))))]
    result))

(defn get-drafts
  [{:keys [uid datasource]} data]
  (-> (j/with-db-transaction [conn datasource]
        (post/get-drafts conn uid (:cursor data)))
      (wrap-end? (get (:cursor data) :limit 50))
      (assoc :end? true)))

(defn get-posts
  [{:keys [uid datasource]} data]
  (let [cursor (update (:cursor data) :limit (fn [v] (if v v 10)))]
    ;; get locale and current user's languages
    (j/with-db-transaction [conn datasource]
      (let [languages (if uid
                        (let [langs (:languages (u/get conn uid))]
                          (if (seq langs)
                            langs
                            ["en"]))
                        (let [locale @dicts/locale]
                          (vec (set ["en" (name locale)]))))
            result (cond
                     (:tag data)
                     (get-tag-posts conn data)

                     (:user-tag data)
                     (get-user-tag-posts conn data)

                     (= :non-tech (:filter data))
                     (post/get-non-tech conn
                                   [:and
                                    [:= :is_draft false]
                                    [:= :non_tech true]
                                    [:in :lang languages]]
                                   cursor)

                     (and (:group_id data) (= :hot (:filter data)))
                     (post/get-hot conn
                                   [:and
                                    [:= :group_id (:group_id data)]
                                    [:= :is_draft false]
                                    [:= :non_tech false]
                                    [:in :lang languages]]
                                   cursor)

                     (and (:group_id data) (= :newest (:filter data)))
                     (post/get-new conn
                                   [:and
                                    [:= :group_id (:group_id data)]
                                    [:= :is_draft false]
                                    [:= :non_tech false]
                                    [:in :lang languages]]
                                   cursor)

                     (and (:group_id data) (or (= :latest-reply (:filter data))
                                               (nil? (:filter data))))
                     (post/get-latest-reply conn
                                   [:and
                                    [:= :group_id (:group_id data)]
                                    [:= :is_draft false]
                                    [:in :lang languages]]
                                   cursor)

                     (and (:user_id data) (= :newest (:filter data)))
                     (let [self? (= (:user_id data) uid)]
                       (post/get-user-new conn (:user_id data)
                                          (if self?
                                            [:and
                                             [:= :user_id (:user_id data)]
                                             [:= :is_draft false]]
                                            [:and
                                             [:= :user_id (:user_id data)]
                                             [:= :is_draft false]
                                             [:in :lang languages]])
                                         cursor))

                     (= :bookmarked (:filter data))
                     (post/get-bookmarked conn uid cursor)

                     (= :hot (:filter data))
                     (post/get-hot conn
                                   [:and
                                    [:= :is_draft false]
                                    [:in :lang languages]]
                                   cursor)

                     :else
                     (post/get-new conn
                                   [:and
                                    [:= :is_draft false]
                                    [:in :lang languages]]
                                   cursor))
            result result
            choices-ids (when-let [post-ids (seq (map :id result))]
                          (su/normalize :post_id (choice/get-choices-ids conn uid post-ids)))
            result (mapv (fn [post]
                           (if-let [choice-id (get-in choices-ids [(:id post) :choice_id])]
                             (assoc post :poll_choice choice-id)
                             post))
                         result)]
        (wrap-end? result (get cursor :limit))))))

(def resolvers
  {
   ;; get current user
   :current-user get-current-user

   ;; get specific user
   :user get-user

   ;; get specific post by permalink
   :post get-post

   :tag wrap-tag-posts

   :user-tag wrap-user-tag-posts

   ;; get specific group
   :group get-group

   ;; get groups
   ;; {:filter (enum :hot :new) :user_id ID}
   :groups get-groups

   :members get-members

   ;; args {:user_id ID}
   :notifications get-notifications
   :reports get-reports
   :stats get-stats

   ;; args {:filter (enum :hot :new) :user_id ID}
   :posts get-posts
   :drafts get-drafts

   :comments get-comments})

(defn one-to-many?
  [field]
  (contains? #{:posts :drafts :groups :members
               :comments
               :notifications :conversations} field))

(defn skip-relation
  [key]
  (contains? #{:user-tag :tag} key))

(defn select
  [col ks]
  (if (= (seq ks) '(:*))
    col
    (if (sequential? col)
      (mapv #(select-keys % ks) col)
      (select-keys col ks))))

;; TODO: parallel
(defn rel-resolve
  [result parent context rel-fields args]
  (if (skip-relation parent)
    result
    (loop [rel-fields rel-fields result result]
      (if (seq rel-fields)
        (let [[rel-field {:keys [fields]
                          :as opts}] (first rel-fields)
              [fk param-key] (if (one-to-many? rel-field)
                               [:id (keyword (str (name parent) "_id"))]
                               [(keyword (str (name rel-field) "_id")) :id])
              id (get result fk)]
          (if id
            (let [resolve-f (get resolvers rel-field)
                  new-args (-> (get args rel-field)
                               (merge (dissoc opts :fields))
                               (assoc param-key id))
                  entity (resolve-f context new-args)
                  rel-entity (if (boolean? (:end? entity))
                               (update entity :result (fn [v] (select v fields)))
                               (select entity fields))]
              (recur (rest rel-fields)
                     (assoc result rel-field rel-entity)))
            ;; TODO: log error, tell this to client
            (recur (rest rel-fields)
                   result)))
        result))))

(defn query
  [context q args]
  (let [result (if (seq q)
     (loop [q q m {}]
       (if (seq q)
         (let [[resolver-key {:keys [fields cursor]
                              :as opts}] (first q)]
           (if-let [resolver-f (get resolvers resolver-key)]
             (let [opts (dissoc opts :fields)
                   new-args (merge opts (get args resolver-key))
                   [key-fields rel-fields] [(remove coll? fields)
                                            (filter coll? fields)]
                   all-fields (set/union (set key-fields) (set (mapv first rel-fields)))
                   result (resolver-f context new-args)
                   result (cond
                            (not result)
                            result

                            (= :not-found result)
                            result

                            :else
                            (let [result (rel-resolve result resolver-key context rel-fields (merge opts (dissoc args resolver-key)))]
                              (if (boolean? (:end? result))
                                (update result :result (fn [v] (select v all-fields)))
                                (select result all-fields))))]
               (recur (rest q)
                      (assoc m resolver-key result)))
             [:bad "no resolver found"]))
         [:ok m]))
     [:bad "q can't be empty."])]
    result))

(defn handler
  [{:keys [context params]
    :as req}]
  (let [{:keys [q args]} params]
    (let [[query result] (query context q args)]
      (if (= query :ok)
        (util/ok result)
        (util/bad result)))))

(comment

  (def q {:current-user {:fields [:id :name :email]}, :post {:fields [:id :title :permalink :comments {:fields [:id :body :created_at], :cursor {:limit 100}}]}})
  (def args {:post {:permalink "hello-1fa889ca716a49a48a30df2f522c9ddd"}})
  (query context q args)

  (def q {:user {:fields [:id :name]}})
  (def args {:user {:screen_name "tiensonqin"}})
  (query context q args)

  ;; group
  (def q {:group {:fields [:id :name]}})
  (def args {:group {:name "云南"}})
  (query context q args)

  ;; relationship
  (def q {:post {:fields [:id :title :permalink [:user {:fields [:id :screen_name]}]]}})
  (def args {:post {:permalink "hello-1fa889ca716a49a48a30df2f522c9ddd"}})
  (query context q args)

  ;; group
  (def q {:group {:fields [:id :name
                           [:user {:fields [:id :screen_name]}]]}})
  (def args {:group {:name "Great"}})
  (query context q args)

  (def q {:post {:fields [:id :title :permalink [:comments {:fields [:id :body :created_at], :cursor {:limit 100}}]]}})
  (def args {:post {:permalink "great-c112b34111be48798091ca555e756d41"}})
  (query context q args)

  ;; posts
  (def q {:posts {:fields [:user_id]}})
  (def args {:posts {
                     ;; :cursor {:limit 5}
                     :filter :hot
                     ;; :group_id #uuid "e68df776-9342-4228-aa17-242d1ee91247"
                     }})
  (query context q args)


  ;; groups
  (def q {:groups {:fields [:id :name]}})
  (def args {:groups {:cursor {:limit 5}
                      :filter :hot}})
  (query context q args)

  ;; get user
  (def q {:current-user {:fields [:id :name :email
                                  [:conversations {:fields [:*]
                                                   :cursor {:limit 5}}]
                                  [:notifications {:fields [:*]
                                                   :cursor {:limit 5}}]
                                  [:posts {:fields [:id :title]
                                           :filter :hot
                                           :cursor {:limit 5}}]]}})
  (def args nil)
  (query context q args)

  (def q {
          ;; :current-user {:fields [:id :name :email]},
          :post
          {:fields
           [:id
            :title
            :body
            :permalink
            :created_at
            [:group {:fields [:id :name]}]
            [:comments
             {:fields [:id :body :created_at]
              :cursor {:limit 100}}]]}})
  (def args {:post {:permalink "great-8036c607d6ae4383b6041f9c2c9a02fc"}})
  (query context q args)

  (def q {:notifications {:fields [:*]}})
  (def args nil)
  (query context q args)

  (def context {:datasource user/db :uid user/me})

  (defn query'
    [{:keys [q args]}]
    (query context q args))
  )
