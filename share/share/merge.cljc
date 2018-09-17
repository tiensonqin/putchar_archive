(ns share.merge
  (:require [share.util :as util]))

(defn- update-merge
  [state path m]
  (update-in state path util/deep-merge m))

(defn- merge-concat
  [old new]
  (->> (concat old new)
       (remove nil?)
       (distinct)
       (vec)))

(defmulti mergef (fn [state route-handler q result k] k))

(defmethod mergef :members [state route-handler q {:keys [tag] :as result} _k]
  (-> state
      (update-merge [:tag (:name tag)] tag)))

(defmethod mergef :post [state route-handler q {:keys [post] :as result} _k]
  (let [permalink (:permalink post)
        post-id (:id post)]
    (-> state
        (update-merge [:post :by-permalink permalink] (dissoc post :comments))
        (update-merge [:post :current] (dissoc post :comments))
        (assoc-in [:comment :posts post-id] (if (:comments post)
                                              (update (:comments post) :result util/normalize)
                                              (:comments post))))))

(defmethod mergef :post-edit [state route-handler q {:keys [post] :as result} _k]
  (mergef state :post-edit q result :post))

(defmethod mergef :posts [state route-handler q {:keys [posts] :as result} _k]
  (update-in state (get-in q [:merge :posts])
             (fn [old]
               {:result (merge-concat (:result old) (:result posts))
                :end? (:end? posts)})))

(defmethod mergef :me [state route-handler q {:keys [current-user] :as result} _k]
  (update-merge state [:user :current] current-user))

(defmethod mergef :user [state route-handler q {:keys [user] :as result} _k]
  (-> state
      (update-merge [:user :by-screen-name (:screen_name user)] user)))

(defmethod mergef :user-posts [state route-handler q {:keys [user] :as result} _k]
  (let [q (let [m (get-in q [:merge :user-posts])]
            (-> q
                (util/dissoc-in [:merge :user-posts])
                (assoc-in [:merge :posts] m)))]
    (-> state
        (mergef :user q {:user (dissoc user :posts)} :user)
        (mergef :posts q {:posts (:posts user)} :posts))))

(defmethod mergef :book-posts [state route-handler q {:keys [book] :as result} _k]
  (let [q (let [m (get-in q [:merge :book-posts])]
            (-> q
                (util/dissoc-in [:merge :book-posts])
                (assoc-in [:merge :posts] m)))]
    (-> state
        (mergef :book q {:book (dissoc book :posts)} :book)
        (mergef :posts q {:posts (:posts book)} :posts))))

(defmethod mergef :book [state route-handler q {:keys [book] :as result} _k]
  (-> state
      (update-merge [:book :by-id (:id book)] book)))

(defmethod mergef :books [state route-handler q {:keys [books] :as result} _k]
  (-> state
      (assoc-in [:books :hot] books)))

(defmethod mergef :papers [state route-handler q {:keys [papers] :as result} _k]
  (-> state
      (assoc-in [:papers :hot] papers)))

(defmethod mergef :paper-posts [state route-handler q {:keys [paper] :as result} _k]
  (let [q (let [m (get-in q [:merge :paper-posts])]
            (-> q
                (util/dissoc-in [:merge :paper-posts])
                (assoc-in [:merge :posts] m)))]
    (-> state
        (mergef :paper q {:paper (dissoc paper :posts)} :paper)
        (mergef :posts q {:posts (:posts paper)} :posts))))

(defmethod mergef :paper [state route-handler q {:keys [paper] :as result} _k]
  (-> state
      (update-merge [:paper :by-id (:id paper)] paper)))


(defmethod mergef :comments [state route-handler q {:keys [comments] :as result} _k]
  (update-in state (get-in q [:merge :comments])
             (fn [old]
               {:result (util/normalize (merge-concat (vals (:result old)) (:result comments)))
                :end? (:end? comments)})))

(defmethod mergef :user-comments [state route-handler q {:keys [user] :as result} _k]
  (let [q (let [m (get-in q [:merge :user-comments])]
            (-> q
                (util/dissoc-in [:merge :user-comments])
                (assoc-in [:merge :comments] m)))]
    (-> state
        (mergef :user q {:user (dissoc user :comments)} :user)
        (mergef :comments q {:comments (:comments user)} :comments))))

(defmethod mergef :my-posts [state route-handler q {:keys [current-user] :as result} _k]
  (let [q (let [m (get-in q [:merge :my-posts])]
            (-> q
                (util/dissoc-in [:merge :my-posts])
                (assoc-in [:merge :posts] m)))]
    (-> state
        (mergef :me q {:user (dissoc current-user :posts)} :user)
        (mergef :posts q {:posts (:posts current-user)} :posts))))

(defmethod mergef :notifications [state route-handler q {:keys [notifications] :as result} _]
  (assoc state :notifications notifications))

(defmethod mergef :reports [state route-handler q {:keys [reports] :as result} _]
  (assoc state :reports reports))

(defmethod mergef :moderation-logs [state route-handler q {:keys [moderation-logs] :as result} _]
  (assoc state :moderation-logs moderation-logs))

(defmethod mergef :stats [state route-handler q {:keys [stats] :as result} _]
  (assoc state :stats stats))

(defmethod mergef :home [state route-handler q result _k]
  state)

(defmethod mergef :drafts [state route-handler q {:keys [current-user] :as result} _k]
  (-> state
      (mergef :user q {:user (dissoc current-user :drafts)} :user)
      (assoc :drafts (:drafts current-user))))

(defmethod mergef :user-tag [state route-handler q {:keys [user-tag] :as result} _k]
  (let [q (let [m (get-in q [:merge :user-tag])]
            (-> q
                (util/dissoc-in [:merge :user-tag])
                (assoc-in [:merge :posts] m)))]
    (-> state
        (mergef :user q {:user (dissoc user-tag :posts)} :user)
        (mergef :posts q {:posts (:posts user-tag)} :posts))))

(defmethod mergef :tag-posts [state route-handler q {:keys [tag] :as result} _k]
  (let [q (let [m (get-in q [:merge :tag-posts])]
            (-> q
                (util/dissoc-in [:merge :tag-posts])
                (assoc-in [:merge :posts] m)))]
    (-> state
        (mergef :posts q {:posts (:posts tag)} :posts))))
