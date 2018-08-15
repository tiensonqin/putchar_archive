(ns api.db.report
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.util :as au]
            [api.db.util :as util]
            [api.db.group :as group]
            [api.db.comment :as comment]
            [api.db.post :as post]
            [api.db.user :as u]
            [api.db.block :as block]
            [api.db.cache :as cache]
            [api.db.notification :as notification]
            [taoensso.carmine :as car]))

(defonce ^:private table :reports)

;; cache {:group-id pending-reports-number}
(defn group-inc-report
  [group-id number]
  (cache/wcar*
   (car/hincrby "groups_reports" group-id number)))

(defn get-groups-reports
  []
  (when-let [result (cache/wcar*
                     (car/hgetall "groups_reports"))]
    ;; TODO: extract as common lib function
    (let [result (partition 2 result)
          ks (map first result)
          vs (map (comp (fn [v] (Integer/parseInt v)) second) result)]
      (zipmap ks vs))))

(defn has-new?
  [groups]
  (let [reports (get-groups-reports)]
    (seq (filter (fn [id] (if-let [n (reports id)]
                            (> n 0))) (set groups)))))

(defn get-data
  [db {:keys [object_type object_id]}]
  (prn object_type object_id)
  (case object_type
    :post (when-let [post (post/get db object_id)]
            {:group (:group post)
             :post (dissoc post :group)
             :user (:user post)})
    :comment (when-let [comment (comment/get db object_id)]
               (cond
                 (:post_id comment)
                 (let [post (post/get db (:post_id comment))]
                   {:group (group/get db (get-in post [:group :id]))
                    :post post
                    :comment comment
                    :user (u/get db (:user_id comment) [:id :screen_name])})
                 :else
                 {:comment comment
                  :user (u/get db (:user_id comment) [:id :screen_name])}))))

(defn create
  [db m]
  (when-let [data (get-data db m)]
    (let [group-id (get-in data [:group :id])
          result (util/create db table (cond->
                                         (assoc m :data data)
                                         group-id
                                         (assoc :group_id group-id)) :flake? true)]
      (if group-id (group-inc-report group-id 1))
      result)))

(defn delete
  [db report]
  (util/delete db table (:id report))
  (if (:group_id report)
    (group-inc-report (:group_id report) -1)))

(defn get-user-reports
  [db user-id cursor]
  (let [user (u/get db user-id)
        groups-ids (group/get-user-managed-ids db (:screen_name user))]
    (if (seq groups-ids)
      (-> {:select [:*]
           :from [:reports]
           :where [:and
                   [:in :group_id groups-ids]
                   [:= :status "pending"]
                   ]}
          (util/wrap-cursor cursor)
          (->> (util/query db))))))

(defn delete-object
  [db {:keys [object_type object_id data] :as report}]
  ;; notification
  (case (name object_type)
    "post"
    (post/delete db object_id)
    "comment"
    (comment/delete db object_id))

  (util/update db table (:id report) {:status "ok"})
  (if (:group_id report)
    (group-inc-report (:group_id report) -1))

  (notification/create (get-in data [:user :id])
                       {:type :post-or-comment-deleted
                        :post (:post data)
                        :comment (:comment data)
                        :reason (:kind report)
                        :created_at (util/sql-now)}))

(defn block-user
  [db uid report action]
  (when (:group_id report)
    ;; notification
    (block/create db {:report_id (:id report)
                      :user_id (au/->uuid (get-in report [:data :user :id]))
                      :group_id (:group_id report)
                      :action action
                      :group_admin uid})

    (notification/create (get-in report [:data :user :id])
                         {:type :group-blocked
                          :group (get-in report [:data :group])
                          :action action
                          :created_at (util/sql-now)})))
