(ns api.db.like
  (:require [api.db.util :as util]
            [api.db.user :as u]
            [api.db.cache :as cache]
            [taoensso.carmine :as car]))

(defonce ^:private table :likes)

(defn like
  [db m]
  (when-not (util/exists? db table m)
    (let [result (util/create db table m :flake? true)]
      (cache/wcar*
       (car/zadd (cache/redis-key "users" (:user_id m) "liked_comments") (:flake_id result) (:comment_id m)))
      result)))

(defn unlike
  [db m]
  (when-let [result (util/delete db table m)]
    (cache/wcar*
     (car/zrem (cache/redis-key "users" (:user_id m) "liked_comments") (:comment_id m)))
    result))
