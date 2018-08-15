(ns api.db.top
  (:require [api.db.util :as util]
            [api.db.user :as u]
            [api.db.cache :as cache]
            [taoensso.carmine :as car]))

(defonce ^:private table :tops)

(defn top
  [db m]
  (when-not (util/exists? db table m)
    (when-let [result (util/create db table m :flake? true)]
      (cond
        (:post_id m)
        (cache/wcar*
         (car/zadd (cache/redis-key "users" (:user_id m) "toped_posts") (:flake_id result) (:post_id m)))

        (:item_id m)
        (cache/wcar*
         (car/zadd (cache/redis-key "users" (:user_id m) "toped_items") (:flake_id result) (:item_id m)))

        :else
        nil)
      result)))

(defn untop
  [db m]
  (when-let [result (util/delete db table m)]
    (cond
      (:post_id m)
      (cache/wcar*
       (car/zrem (cache/redis-key "users" (:user_id m) "toped_posts") (:post_id m)))

      (:item_id m)
      (cache/wcar*
       (car/zrem (cache/redis-key "users" (:user_id m) "toped_items") (:item_id m)))

      :else nil
      )
    result))

(defn get-toped-posts
  [user-id]
  (cache/wcar*
   (car/zrange (cache/redis-key "users" user-id "toped_posts") 0 100)))

(defn get-toped-items
  [user-id]
  (cache/wcar*
   (car/zrange (cache/redis-key "users" user-id "toped_items") 0 100)))
