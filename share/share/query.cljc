(ns share.query
  (:require [clojure.string :as str]
            [share.util :as util]))

(def post-fields
  [:id :flake_id :user :title :rank :permalink :created_at :comments_count :tops :cover :video :last_reply_at :last_reply_by :last_reply_idx :tags :frequent_posters :is_article :data])

(defn- get-post-filter
  [state]
  (or (get-in state [:post :filter]) :latest-reply))

(def home-query
  (fn [state args]
    (let [current-user (:current-user args)]
      {:q     {:posts {:fields post-fields
                       :filter :hot}}
       :args  nil
       :merge {:posts [:posts :hot]}})))

(def newest-posts-query
  (fn [state args]
    {:q     {:posts {:fields post-fields
                     :filter :newest}}
     :args  nil
     :merge {:posts [:posts :latest]}}))

(def non-tech-posts-query
  (fn [state args]
    {:q     {:posts {:fields post-fields
                     :filter :non-tech}}
     :args  nil
     :merge {:posts [:posts :non-tech]}}))

(def latest-reply-posts-query
  (fn [state args]
    {:q     {:posts {:fields post-fields
                     :filter :latest-reply}}
     :args  nil
     :merge {:posts [:posts :latest-reply]}}))

(def members-query
  (fn [state args]
    (let [tag-name (str/lower-case (:tag args))]
      {:q    {:tag {:fields [:name
                             [:members {:fields [:*]
                                        :cursor {:limit 100}}]]}}
       :args {:tag {:name tag-name}}})))

(def post-query
  (fn [state args]
    {:q    {:post {:fields [:id
                            :title
                            :body
                            :body_format
                            :tags
                            :permalink
                            :created_at
                            :updated_at
                            :last_reply_at
                            :tops
                            :comments_count
                            :is_article
                            :data
                            [:user {:fields [:id :screen_name :name :bio :website]}]
                            [:comments {:fields [:*]
                                        :cursor {:limit 100}}]]}}
     :args {:post {:permalink (str "@"
                                   (:screen_name args)
                                   "/"
                                   (:permalink args))}}}))

(def post-edit-query
  (fn [state args]
    (let [id (util/uuid (:post-id args))
          q {:q    {:post {:fields [:id
                                    :title
                                    :body
                                    :body_format
                                    :lang
                                    :permalink
                                    :is_draft
                                    :is_article
                                    :tags]}}
             :args {:post {:id id
                           :raw_body? true}}}]
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

(def stats-query
  (fn [state args]
    {:q {:stats {:fields [:*]}}}))

(def user-query
  (fn [state args]
    (let [post-filter :newest]
      {:q {:user {:fields [:id :screen_name :name :bio :website :github_handle :twitter_handle :tags
                           [:posts {:fields post-fields
                                    :filter post-filter}]]}}
       :args {:user {:screen_name (:screen_name args)}}
       :merge {:user-posts [:posts :by-screen-name (:screen_name args) post-filter]}})))

(def drafts-query
  (fn [state args]
    {:q {:current-user {:fields [:id :screen_name :name :bio :website :github_handle :twitter_handle
                         [:drafts {:fields [:*]}]]}}
     :args nil}))

(def comments-query
  (fn [state args]
    {:q {:user {:fields [:id :screen_name :name :bio :website :github_handle :twitter_handle
                         [:comments {:fields [:*]
                                     :cursor {:limit 20}}]]}}
     :args {:user {:screen_name (:screen_name args)}}
     :merge {:user-comments [:comments :by-screen-name (:screen_name args)]}}))

(def votes-query
  (fn [state args]
    (let [post-filter :voted]
      {:q {:current-user {:fields [:id :screen_name :name :bio :website :github_handle :twitter_handle
                                   [:posts {:fields post-fields}]]}}
       :args {:posts {:filter post-filter}}
       :merge {:my-posts [:posts :current-user post-filter]}})))

(def bookmarks-query
  (fn [state args]
    (let [post-filter :bookmarked]
      {:q {:current-user {:fields [:id :screen_name :name :bio :website :github_handle :twitter_handle
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
      {:q     {:user-tag {:fields [:id :screen_name :name :bio :website :tags :github_handle :twitter_handle
                                   [:posts {:fields post-fields}]]}}
       :args  {:user-tag idx}
       :merge {:user-tag [:posts :by-user-tag idx]}})))

;; key: route handler
(def queries
  {:home home-query

   :newest newest-posts-query

   :non-tech non-tech-posts-query

   :latest-reply latest-reply-posts-query

   :members members-query

   :notifications notifications-query

   :reports reports-query

   :moderation-logs moderation-logs-query

   :stats stats-query

   :post      post-query

   :post-edit post-edit-query

   :user user-query

   :drafts drafts-query

   :comments comments-query

   :votes votes-query

   :bookmarks bookmarks-query

   :tag tag-posts-query

   :user-tag user-tag-posts-query
})
