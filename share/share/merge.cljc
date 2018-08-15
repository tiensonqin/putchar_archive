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

(defmethod mergef :group [state route-handler q {:keys [group] :as result} _k]
  (cond-> (update-merge state [:group :by-name (:name group)] group)
    (:id group)
    (assoc-in [:group :current] (:id group))))

(defmethod mergef :group-edit [state route-handler q {:keys [group] :as result} _k]
  (mergef state :group-edit q result :group))

(defmethod mergef :channel [state route-handler q {:keys [channel]} _k]
  (let [id (:id channel)
        group (:group channel)]
    (-> state
        (assoc-in [:group :current] (:id group))
        (update-merge [:group :by-name (:name group)] group)
        (assoc-in [:channel :current] id)
        (assoc-in [:channel :by-id id] channel))))

(defmethod mergef :channels [state route-handler q {:keys [group] :as result} _k]
  (-> state
      (assoc-in [:group :current] (:id group))
      (update-merge [:group :by-name (:name group)] group)))

(defmethod mergef :members [state route-handler q {:keys [group] :as result} _k]
  (-> state
      (assoc-in [:group :current] (:id group))
      (update-merge [:group :by-name (:name group)] group)))

(defmethod mergef :groups [state route-handler q {:keys [groups] :as result} _]
  (-> state
      (assoc-in [:group :hot] groups)))

(defmethod mergef :post [state route-handler q {:keys [post] :as result} _k]
  (let [group-id (get-in post [:group :id])
        channel-id (get-in post [:channel :id])
        permalink (:permalink post)
        post-id (:id post)]
    (-> state
        (update-merge [:post :by-permalink permalink] (dissoc post :comments))
        (update-merge [:post :current] (dissoc post :comments))
        (assoc-in [:group :current] group-id)
        (assoc-in [:channel :current] channel-id)
        (assoc-in [:comment :posts post-id] (if (:comments post)
                                              (update (:comments post) :result util/normalize)
                                              (:comments post)))
        (update-merge [:group :by-name (get-in post [:group :name])] (:group post)))))

(defmethod mergef :post-edit [state route-handler q {:keys [post] :as result} _k]
  (mergef state :post-edit q result :post))

(defmethod mergef :posts [state route-handler q {:keys [posts] :as result} _k]
  (update-in state (get-in q [:merge :posts])
             (fn [old]
               {:result (merge-concat (:result old) (:result posts))
                :end? (:end? posts)})))

(defmethod mergef :group-posts [state route-handler q {:keys [group] :as result} _k]
  (let [m (get-in q [:merge :group-posts])
        q (-> q
              (util/dissoc-in [:merge :group-posts])
              (assoc-in [:merge :posts] m))]
    (-> state
        (assoc-in [:post :filter] (last m))
        (mergef :group q {:group (dissoc group :posts)} :group)
        (mergef :posts q {:posts (:posts group)} :posts))))

(defmethod mergef :channel-posts [state route-handler q {:keys [channel] :as result} _k]
  (let [m (get-in q [:merge :channel-posts])
        q (-> q
              (util/dissoc-in [:merge :channel-posts])
              (assoc-in [:merge :posts] m))]
    (-> state
        (assoc-in [:post :filter] (last m))
        (mergef :channel q {:channel (dissoc channel :posts)} :channel)
        (mergef :posts q {:posts (:posts channel)} :posts))))


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
