(ns api.db.posts-notification
  (:require [api.db.util :as util]
            [api.db.user :as u]
            [api.db.cache :as cache]
            [api.services.slack :as slack]
            [clojure.java.jdbc :as j]
            [taoensso.carmine :as car]))

(defonce ^:private table :posts_notifications)

;; level could be "default" | "watch" | "mute"
(defn set-notification
  [db {:keys [permalink email level]
       :as m}]
  (try
    (let [item (util/exists? db table (select-keys m [:permalink :email]))]
      (if item (util/delete db table (select-keys m [:permalink :email])))
      (let [result (util/create db table m :flake? true)
            watch-key (cache/redis-key "posts" (:permalink m) "watched_users")
            mute-key (cache/redis-key "posts" (:permalink m) "muted_users")]
       (cache/wcar*
        (case level
          "watch"
          (do
            (car/zadd watch-key (:flake_id result) (:email m))
            (car/zrem mute-key (:email m)))

          "mute"
          (do
            (car/zrem watch-key (:flake_id result) (:email m))
            (car/zadd mute-key (:email m)))

          "default"
          (do
            (car/zrem watch-key (:flake_id result) (:email m))
            (car/zrem mute-key (:email m)))))
       result))
    (catch Exception e
      (slack/error e))))

(defn get-watched-emails
  [permalink]
  (set (cache/wcar*
        (car/zrange (cache/redis-key "posts" permalink "watched_users") 0 -1))))

(defn get-muted-emails
  [permalink]
  (set (cache/wcar*
        (car/zrange (cache/redis-key "posts" permalink "muted_users") 0 -1))))

(defn get-level
  [db permalink email]
  (util/select-one-field db table {:permalink permalink
                                   :email email}
                         :level))
