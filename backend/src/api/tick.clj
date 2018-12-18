(ns api.tick
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [api.db.post :as post]
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
     (car/del post/hot-tags-k)
     (doseq [[screen_name tags] tags]
       (car/hset post/tags-k
                 screen_name tags)
       (doseq [[tag c] tags]
         (car/zincrby post/hot-tags-k c tag))))))

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
   (recompute-tags-job db)
   ;; (compute-stats db)
   ])
