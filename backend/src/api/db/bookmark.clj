(ns api.db.bookmark
  (:require [api.db.util :as util]
            [api.db.user :as u]
            [api.db.cache :as cache]
            [taoensso.carmine :as car]))

(defonce ^:private table :bookmarks)

(defn bookmark
  [db m]
  (when-not (util/exists? db table m)
    (when-let [result (util/create db table m :flake? true)]
      (cond
        (:post_id m)
        (cache/wcar*
         (car/zadd (cache/redis-key "users" (:user_id m) "bookmarked_posts") (:flake_id result) (:post_id m)))

        (:item_id m)
        (cache/wcar*
         (car/zadd (cache/redis-key "users" (:user_id m) "bookmarked_items") (:flake_id result) (:item_id m)))

        :else
        nil)
      result)))

(defn unbookmark
  [db m]
  (when-let [result (util/delete db table m)]
    (cond
      (:post_id m)
      (cache/wcar*
       (car/zrem (cache/redis-key "users" (:user_id m) "bookmarked_posts") (:post_id m)))

      (:item_id m)
      (cache/wcar*
       (car/zrem (cache/redis-key "users" (:user_id m) "bookmarked_items") (:item_id m)))

      :else nil
      )
    result))

(defn get-bookmarked-posts
  [user-id]
  (cache/wcar*
   (car/zrange (cache/redis-key "users" user-id "bookmarked_posts") 0 100)))

(defn get-bookmarked-items
  [user-id]
  (cache/wcar*
   (car/zrange (cache/redis-key "users" user-id "bookmarked_items") 0 100)))
