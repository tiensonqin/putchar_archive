(ns api.tick
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [api.db.post :as post]
            [api.db.group :as group]
            [api.db.cache :as cache]
            [api.db.stat :refer [build-stats]]
            [api.services.slack :as slack]
            [taoensso.carmine :as car]
            [clojure.java.jdbc :as j])
  (:import [org.joda.time DateTimeZone]))

(defn- schedule-job
  [db period f]
  (chime-at (rest
             (periodic-seq (t/now) period))
            f
            {:error-handler (fn [e]
                              (slack/error "schedule job failed: "  e))}))

(defn- cron-job
  [db hour f]
  (chime-at (->> (periodic-seq (.. (t/now)
                                   (withZone (DateTimeZone/forID "UTC"))
                                   (withTime hour 0 0 0))
                               (-> 1 t/days)))
            f
            {:error-handler (fn [e]
                              (slack/error "schedule cron job failed: "  e))}))

(defn recalculate-posts-rank
  [db]
  (schedule-job db (t/hours 1) (fn [_time]
                                 (j/with-db-connection [conn db]
                                   (post/recalculate-rank conn)))))

(defn compute-group-posts-count
  [db]
  (schedule-job db (t/hours 24) (fn [_time]
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
  (schedule-job db (t/hours 12) (fn [_time]
                                  (j/with-db-connection [conn db]
                                    (recompute-stars conn)))))

(defn recompute-tags
  [db]
  (let [posts (j/query db ["select user_screen_name, tags from posts where tags is not null"])
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
  (schedule-job db (t/hours 24) (fn [_time]
                                  (j/with-db-connection [conn db]
                                    (recompute-tags conn)))))

(defn compute-stats
  [db]
  (cron-job db 0 (fn [_time]
                   (j/with-db-connection [conn db]
                     (build-stats conn)))))

(defn jobs
  [db]
  [(recalculate-posts-rank db)
   (compute-group-posts-count db)
   (recompute-stars-job db)
   (recompute-tags-job db)
   (compute-stats db)])
