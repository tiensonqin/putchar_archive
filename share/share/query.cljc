(ns share.query
  (:require [clojure.string :as str]
            [share.util :as util]))

(def post-fields
  [:id :flake_id :user :title :rank :permalink :link :created_at :comments_count :tops :cover :last_reply_at :last_reply_by :last_reply_idx :tags :frequent_posters])

(def home-query
  (fn [state args]
    (if-let [current-user (:current-user args)]
      ;; feed
      {:q     {:posts {:fields post-fields
                       :filter :feed}}
       :args  nil
       :merge {:posts [:posts :feed]}}
      {:q     {:posts {:fields post-fields
                       :filter :hot}}
       :args  nil
       :merge {:posts [:posts :hot]}})))

(def latest-posts-query
  (fn [state args]
    {:q     {:posts {:fields post-fields
                     :filter :latest}}
     :args  nil
     :merge {:posts [:posts :latest]}}))

(def hot-posts-query
  (fn [state args]
    {:q     {:posts {:fields post-fields
                     :filter :hot}}
     :args  nil
     :merge {:posts [:posts :hot]}}))

(def post-query
  (fn [state args]
    (let [args (if (and (:screen_name args)
                        (:permalink args))
                 {:permalink (str "@"
                                  (:screen_name args)
                                  "/"
                                  (:permalink args))}
                 {:id (:post-id args)})]
      {:q    {:post {:fields [:id
                             :title
                             :body_html
                             :body_format
                             :tags
                             :permalink
                             :created_at
                             :updated_at
                             :last_reply_at
                             :tops
                             :comments_count
                             :link :cover
                             [:user {:fields [:id :screen_name :name :bio]}]
                             [:comments {:fields [:*]
                                         :cursor {:limit 100}}]]}}
       :args {:post args}})))

(def post-edit-query
  (fn [state args]
    (let [id (:post-id args)
          q {:q    {:post {:fields [:id
                                    :title
                                    :body
                                    :body_format
                                    :lang
                                    :permalink
                                    :is_draft
                                    :tags]}}
             :args {:post {:id id}}}]
      #?(:clj q
         :cljs (let [current (get-in state [:post :current])]
                 (when (or (not current)
                           (not= id (get current :id)))
                   q))))))

(def notifications-query
  (fn [state args]
    {:q {:notifications {:fields [:*]}}}))

(def reports-query
  (fn [state args]
    {:q {:reports {:fields [:*]
                   :cursor {:limit 100}}}}))

(def moderation-logs-query
  (fn [state args]
    {:q {:moderation-logs {:fields [:*]
                           :cursor {:limit 20}}}}))

(def user-query
  (fn [state args]
    (let [post-filter :latest]
      {:q {:user {:fields [:id :screen_name :name :bio :github_handle :tags
                           [:posts {:fields post-fields
                                    :filter post-filter
                                    :cursor {:limit 100}}]]}}
       :args {:user {:screen_name (:screen_name args)}}
       :merge {:user-posts [:posts :by-screen-name (:screen_name args) post-filter]}})))

(def links-query
  (fn [state args]
    (let [post-filter :links]
      {:q {:user {:fields [:id :screen_name :name :bio :github_handle :tags
                           [:posts {:fields [:id :flake_id :title :permalink :link :created_at :tags]
                                    :filter post-filter
                                    :cursor {:limit 100}}]]}}
       :args {:user {:screen_name (:screen_name args)}}
       :merge {:user-posts [:posts :by-screen-name (:screen_name args) post-filter]}})))

(def drafts-query
  (fn [state args]
    {:q {:current-user {:fields [:id :screen_name :name :bio  :github_handle
                         [:drafts {:fields [:*]}]]}}
     :args nil}))

(def comments-query
  (fn [state args]
    {:q {:user {:fields [:id :screen_name :name :bio :github_handle
                         [:comments {:fields [:*]
                                     :cursor {:limit 20}}]]}}
     :args {:user {:screen_name (:screen_name args)}}
     :merge {:user-comments [:comments :by-screen-name (:screen_name args)]}}))

(def votes-query
  (fn [state args]
    (let [post-filter :toped]
      {:q {:current-user {:fields [:id :screen_name :name :bio :github_handle
                                   [:posts {:fields post-fields}]]}}
       :args {:posts {:filter post-filter}}
       :merge {:my-posts [:posts :current-user post-filter]}})))

(def tag-posts-query
  (fn [state args]
    (let [tag (str/lower-case (:tag args))]
      {:q     {:tag {:fields [:count
                              [:posts {:fields post-fields}]]}}
       :args  {:tag {:tag tag}}
       :merge {:tag-posts [:posts :by-tag tag]}})))

(def user-tag-posts-query
  (fn [state args]
    (let [tag (str/lower-case (:tag args))
          screen_name (str/lower-case (:screen_name args))
          idx {:screen_name screen_name
               :tag tag}]
      {:q     {:user-tag {:fields [:id :screen_name :name :bio  :tags :github_handle
                                   [:posts {:fields post-fields}]]}}
       :args  {:user-tag idx}
       :merge {:user-tag [:posts :by-user-tag idx]}})))

;; key: route handler
(def queries
  {:home home-query

   :latest latest-posts-query
   :hot    hot-posts-query

   :notifications notifications-query

   :reports reports-query

   :moderation-logs moderation-logs-query

   :post      post-query

   :post-edit post-edit-query

   :user user-query

   :links links-query

   :drafts drafts-query

   :comments comments-query

   :votes votes-query

   :tag tag-posts-query

   :user-tag user-tag-posts-query
})
