(ns api.tick
  (:require [tick.core :as tick]
            [tick.timeline :refer [timeline periodic-seq]]
            [tick.clock :as clock]
            [tick.schedule :as schedule]
            [api.db.post :as post]
            [api.db.group :as group]
            [api.db.cache :as cache]
            [taoensso.carmine :as car]
            [clojure.java.jdbc :as j]))

(defn- schedule-job
  [db period f]
  (let [timeline (timeline (periodic-seq (clock/now) period))
        schedule (schedule/schedule f timeline)]
    (schedule/start schedule (clock/clock-ticking-in-seconds))
    schedule))

(defn recalculate-posts-rank
  [db]
  (schedule-job db (tick/hours 1) (fn [_tick-date]
                                    (j/with-db-connection [conn db]
                                      (post/recalculate-rank conn)))))

(defn compute-group-posts-count
  [db]
  (schedule-job db (tick/hours 24) (fn [tick-date]
                                     (j/with-db-connection [conn db]
                                       (let [groups (j/query conn ["select count(id) as week_count, group_id from posts
WHERE created_at BETWEEN
    NOW()::DATE-EXTRACT(DOW FROM NOW())::INTEGER-7
    AND NOW()::DATE-EXTRACT(DOW from NOW())::INTEGER
group by group_id
limit 100
"])]
                                         (doseq [{:keys [week_count group_id]} groups]
                                           (group/update conn group_id {:week_posts_count week_count})))))))

(defn recompute-stars
  [db]
  ;; each group
  (let [groups (j/query db ["select id from groups"])]
    (doseq [{:keys [id]} groups]
      (let [stars (:count (first (j/query db ["select count(*) from stars where object_type = 'group' and object_id = ?" id])))]
        (group/update db id {:stars stars})))))

(defn recompute-stars-job
  [db]
  (schedule-job db (tick/hours 12) (fn [_tick-date]
                                     (j/with-db-connection [conn db]
                                       (recompute-stars db)))))

(defn recompute-tags
  [db]
  (let [posts (j/query db ["select user_screen_name, tags from posts where is_private is false and link is null and tags is not null"])
        tags (reduce
              (fn [acc {:keys [user_screen_name tags]}]
                (if-let [old-tags (get acc user_screen_name)]
                  (assoc acc user_screen_name
                         (reduce
                          (fn [acc tag]
                            (if (get old-tags tag)
                              (update acc tag inc)
                              (assoc acc tag 1)))
                          old-tags
                          tags))
                  (assoc acc user_screen_name (zipmap tags (repeat 1)))))
              {}
              posts)]
    (cache/wcar*
     (doseq [[screen_name tags] tags]
       (car/hset post/tags-k
                 screen_name tags)))))

(defn recompute-tags-job
  [db]
  (schedule-job db (tick/hours 36) (fn [_tick-date]
                                     (j/with-db-connection [conn db]
                                       (recompute-tags db)))))

(defn jobs
  [db]
  [(recalculate-posts-rank db)
   (compute-group-posts-count db)
   (recompute-stars-job db)
   (recompute-tags-job db)])
