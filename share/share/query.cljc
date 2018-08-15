(ns share.query
  (:require [clojure.string :as str]
            [share.util :as util]))

(def post-fields
  [:id :flake_id :user :group :channel :title :rank :permalink :created_at :comments_count :tops :link :choices :poll_choice :poll_closed :cover :video :last_reply_at :last_reply_by :tags])

(defn group-fields
  [post-filter]
  [:id :name :purpose :rule :admins :stars :related_groups :privacy :cover_settings
   [:channels {:fields [:id :name :stars]}]
   [:posts {:fields post-fields
            :filter post-filter}]
   [:members {:fields [:*]}]])

(def photo-fields
  [:*])

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

(def latest-reply-posts-query
  (fn [state args]
    {:q     {:posts {:fields post-fields
                     :filter :latest-reply}}
     :args  nil
     :merge {:posts [:posts :latest-reply]}}))

(def channel-query
  (fn [state args]
    (let [names (util/group-channel args)
          post-filter (or (keyword (:post-filter args)) :latest-reply)]
      {:q     {:channel {:fields [:id :name :purpose
                                  [:group {:fields [:id :name :purpose :rule :admins :stars :privacy :related_groups :channels :cover_settings :members]}]
                                  [:posts {:fields post-fields
                                           :filter post-filter}]]}}
       :args  {:channel names}
       :merge {:channel-posts [:posts :by-channel names post-filter]}})))

(def channels-query
  (fn [state args]
    {:q    {:group {:fields [:id :name :purpose :admins :stars :privacy :related_groups
                             [:channels {:fields [:id :name :purpose :stars]
                                         :filter :hot
                                         :cursor {:limit 100}}]]}}
     :args {:group {:name (str/lower-case (:group-name args))}}}))

(def members-query
  (fn [state args]
    (let [group-name (str/lower-case (:group-name args))]
      {:q    {:group {:fields [:id :name :purpose :admins :stars :privacy :related_groups
                               [:channels {:fields [:id :name :purpose :stars]
                                           :filter :hot
                                           :cursor {:limit 100}}]
                               [:members {:fields [:*]
                                          :cursor {:limit 100}}]]}}
       :args {:group {:name group-name}}})))

(def group-query
  (fn [state args]
    (let [group-name (str/lower-case (:group-name args))
          post-filter (or (keyword (:post-filter args)) :latest-reply)]
      {:q     {:group {:fields (group-fields post-filter)}}
       :args  {:group {:name group-name}}
       :merge {:group-posts [:posts :by-group group-name post-filter]}})))

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
                            :link
                            :notification_level
                            :poll_choice
                            :poll_closed
                            :choices
                            [:user {:fields [:id :screen_name :name :bio :website]}]
                            [:group {:fields [:id :name :purpose :rule :admins :stars :related_groups :created_at :privacy]}]
                            [:channel {:fields [:id :name]}]
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
                                    :permalink
                                    :is_draft
                                    :is_wiki
                                    :link
                                    :tags
                                    :notification_level
                                    :poll_choice
                                    :poll_closed
                                    :choices
                                    [:group {:fields [:id :name]}]
                                    [:channel {:fields [:id :name]}]]}}
             :args {:post {:id id}}}]
      #?(:clj q
         :cljs (let [current (get-in state [:post :current])]
                 (when (or (not current)
                           (not= id (get current :id)))
                   q))))))

(def groups-query
  (fn [state args]
    {:q {:groups {:fields [:id :name :purpose :stars :privacy]
                  :filter :hot
                  :cursor {:limit 20}}}}))

(def notifications-query
  (fn [state args]
    {:q {:notifications {:fields [:*]}}}))

(def reports-query
  (fn [state args]
    {:q {:reports {:fields [:*]
                   :cursor {:limit 100}}}}))

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

(def links-query
  (fn [state args]
    (let [post-filter :links]
      {:q {:user {:fields [:id :screen_name :name :bio :website :github_handle :twitter_handle
                           [:posts {:fields post-fields
                                    :filter post-filter}]]}}
       :args {:user {:screen_name (:screen_name args)}}
       :merge {:user-posts [:posts :by-screen-name (:screen_name args) post-filter]}})))

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

   :latest-reply latest-reply-posts-query

   :channel channel-query

   :channel-edit channel-query

   :channels channels-query

   :group group-query

   :members members-query

   :group-edit group-query

   :groups groups-query

   :notifications notifications-query

   :reports reports-query

   :post      post-query

   :post-edit post-edit-query

   :user user-query

   :drafts drafts-query

   :comments comments-query

   :links links-query

   :votes votes-query

   :bookmarks bookmarks-query

   :tag tag-posts-query

   :user-tag user-tag-posts-query
})
