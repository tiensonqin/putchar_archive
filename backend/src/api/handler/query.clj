(ns api.handler.query
  (:require [taoensso.timbre :as timbre]
            [api.config :as config]
            [api.util :as util]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.channel :as channel]
            [api.db.post :as post]
            [api.db.top :as top]
            [api.db.comment :as comment]
            [api.db.posts-notification :as posts-notification]
            [api.db.report :as report]
            [api.db.util :as du]
            [api.db.refresh-token :as refresh-token]
            [api.db.search :as search]
            [api.db.notification :as notification]
            [api.db.report :as report]
            [api.db.star :as star]
            [api.db.choice :as choice]
            [bidi.bidi :as bidi]
            [api.jwt :as jwt]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [clojure.set :as set]
            [share.util :as su]
            [api.services.slack :as slack]))

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
      (if-let [user (u/get conn uid)]
        (assoc user
               :has-unread-notifications?
               (notification/has-unread? uid))))))

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

(defn get-channel
  [{:keys [uid datasource]} data]
  (->
   (j/with-db-connection [conn datasource]
     (channel/get conn
                  (or (:id data)
                      {:group-name (str/lower-case (:group-name data))
                       :channel-name (str/lower-case (:channel-name data))})))
   set-not-found!))

(defn get-post
  [{:keys [uid datasource]} data]
  (->
   (let [where (if (:id data)
                 {:id (:id data)}
                 {:permalink (su/encode-permalink (:permalink data))})]
     (j/with-db-connection [conn datasource]
       (let [post (post/get conn where)]
         (if (and post uid)
           (assoc post
                  :notification_level (if (:permalink data)
                                        (posts-notification/get-level conn (:permalink data) (:email (u/get conn uid))))
                  :poll_choice (choice/get-choice-id conn uid
                                                     (:id post)))
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
    (j/with-db-transaction [conn datasource]
      (let [result (cond
                     (and uid (true? (:feed? data)))
                     (post/get-user-feed conn uid cursor)

                     (:tag data)
                     (get-tag-posts conn data)

                     (:user-tag data)
                     (get-user-tag-posts conn data)

                     (and (:channel_id data) (= :hot (:filter data)))
                     (post/get-channel-hot conn (:channel_id data) cursor)

                     (and (:channel_id data) (= :wiki (:filter data)))
                     (post/get-channel-wiki conn (:channel_id data) cursor)

                     (and (:channel_id data) (= :newest (:filter data)))
                     (post/get-channel-new conn (:channel_id data) cursor)

                     (and (:channel_id data) (= :latest-reply (:filter data)))
                     (post/get-channel-latest-reply conn (:channel_id data) cursor)


                     (and (:group_id data) (= :hot (:filter data)))
                     (post/get-group-hot conn (:group_id data) cursor)

                     (and (:group_id data) (= :wiki (:filter data)))
                     (post/get-group-wiki conn (:group_id data) cursor)

                     (and (:group_id data) (= :newest (:filter data)))
                     (post/get-group-new conn (:group_id data) cursor)

                     (and (:group_id data) (or (= :latest-reply (:filter data))
                                               (nil? (:filter data))))
                     (post/get-group-latest-reply conn (:group_id data) cursor)


                     (and (:user_id data) (= :hot (:filter data)))
                     (post/get-user-hot conn uid (:user_id data) cursor)

                     (and (:user_id data) (= :newest (:filter data)))
                     (post/get-user-new conn uid (:user_id data) cursor)

                     (and (:user_id data) (= :links (:filter data)))
                     (post/get-user-links conn uid (:user_id data) cursor)

                     (= :voted (:filter data))
                     (post/get-toped conn uid cursor)

                     (= :bookmarked (:filter data))
                     (post/get-bookmarked conn uid cursor)

                     (= :hot (:filter data))
                     (post/get-hot conn cursor)

                     (= :latest-reply (:filter data))
                     (post/get-latest-reply conn cursor)

                     :else
                     (post/get-new conn cursor))
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

   ;; get specific channel
   :channel get-channel

   ;; get group channels
   ;; {:filter (enum :hot :new) :group_id ID}
   ;; :channels get-channels

   ;; args {:user_id ID}
   :notifications get-notifications
   :reports get-reports

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

  ;; channel
  (def q {:channel {:fields [:id :name]}})
  (def args {:channel {:group-name "云南"
                       :channel-name "general"}})
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

  (def q {:channel {:fields [:id :name
                             [:user {:fields [:id :screen_name]}]
                             [:group {:fields [:id :name]}]]}})
  (def args {:channel {:group-name "Great"
                       :channel-name "general"}})
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

  (def q {:channels {:fields [:id :name]}})
  (def args {:channels {:cursor {:limit 5}
                        :filter :hot
                        :group_id #uuid "7b135650-0155-414d-a187-93d6ecc06f6b"}})
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
            [:channel {:fields [:id :name]}]
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
