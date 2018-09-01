(ns api.db.stat
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.post :as post]
            [honeysql.core :as sql]
            [api.util :refer [uuid]]))

(defonce ^:private table :stats)

(defn create
  [db post-id type ip]
  (let [m {:post_id post-id
           :type type
           :ip ip}]
    (when-not (util/exists? db table m)
      (util/create db table m))))

;; SELECT location, COUNT(*),
;; histogram(temperature, 60.0, 85.0, 5)
;; FROM conditions
;; WHERE time > NOW() - interval '7 days'
;; GROUP BY location;

;; ({:one_day #inst "2018-09-01T00:00:00.000-00:00", :count 1} {:one_day #inst "2018-08-31T00:00:00.000-00:00", :count 5} {:one_day #inst "2018-08-30T00:00:00.000-00:00", :count 8} {:one_day #inst "2018-08-29T00:00:00.000-00:00", :count 7})
(defn build-stats
  [db]
  ;; delete old data
  (j/execute! db
              ["delete from stats where time <= NOW() - interval '7 days'"])

  (let [posts (j/query db ["select id from posts where is_draft = false"])]
    (doseq [{:keys [id]} posts]
      (let [view-stat (first (j/query db
                               ["select COUNT(*) as last_day_count from stats where post_id = ? and type = ? and time > NOW() - interval '1 day'"
                                id "view"]))
            read-stat (first (j/query db
                               ["select COUNT(*) as last_day_count from stats where post_id = ? and type = ? and time > NOW() - interval '1 day'"
                                id "read"]))]
        (when (or view-stat read-stat)
          ;; insert into stats_agg and updates `reads` and `views`
          (let [last-day-views (get view-stat :last_day_count 0)
                last-day-reads (get read-stat :last_day_count 0)]
            (if-let [post-stat (first (j/query db
                                       ["select * from stats_agg where post_id = ?" id]))]
             (let [{:keys [stats views reads]} post-stat
                   ;; spec: {:reads [bigint] :views [bigint]}
                   stats (read-string stats)]
               (j/update! db "stats_agg"
                          {:views (+ last-day-views views)
                           :reads (+ last-day-reads reads)
                           :stats (pr-str {:views (take-last 7 (conj (:views stats) last-day-views))
                                    :reads (take-last 7 (conj (:views stats) last-day-reads))})}
                          {:post_id id}))
             ;; no stats yet, create new record
             (let [post (post/get db id)]
               (j/insert! db "stats_agg"
                         {:stats (pr-str {:views [last-day-views]
                                          :reads [last-day-reads]})
                          :post_id id
                          :post_created_at (:created_at post)
                          :user_id (get-in post [:user :id])
                          :views last-day-views
                          :reads last-day-reads})))))))))

(defn query
  [db user-id]
  (j/query db
    ["select post_id, stats, views, reads, post_created_at from stats_agg where user_id = ? order by post_created_at desc" user-id]))

(comment
  (create user/db
          #uuid "1c580d67-7e11-4280-af1e-fc47a966ac92"
          "view"
          "127.1.1.1"))
