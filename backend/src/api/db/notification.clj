(ns api.db.notification
  (:require [taoensso.carmine :as car]
            [api.db.cache :as cache]
            [api.db.util :as util]
            [clojure.java.jdbc :as j]))

;; only store recent 100 notifications
(defonce ^:private cache-key "notifications")

(defn create
  [user-id data]
  (when (and user-id data)
    (let [k (cache/redis-key cache-key user-id)]
      (cache/wcar*
       (car/lpush k data)
       (car/ltrim k 0 99)))))

(defn get-user-notifications
  [user-id]
  (let [k (cache/redis-key cache-key user-id)]
    (cache/wcar*
     (car/lrange k 0 99))))

(defn mark-all-as-read
  [user-id]
  (let [k (cache/redis-key cache-key user-id)]
    (cache/wcar*
     (car/del k))))

(defn has-unread?
  [user-id]
  (let [k (cache/redis-key cache-key user-id)]
    (not
     (zero?
      (cache/wcar*
       (car/exists k))))))

(comment
  (create user/me
          {:type :comment
           :data comment})
  (get-user-notifications user/me)
  (mark-all-as-read user/me))
