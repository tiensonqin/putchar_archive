(ns api.db.cache
  (:refer-clojure :exclude [get])
  (:require [taoensso.carmine :as car]
            [api.config :refer [config]]
            [clojure.string :as str]))

(defmacro wcar*
  [& body]
  `(car/wcar (:redis ~config) ~@body))

(defn redis-key
  [& parts]
  (str/join ":" (map #(if (keyword? %) (name %) %) parts)))

(defn reload
  [k id db-get-fn expire fields]
  (let [d (db-get-fn id)
        k (redis-key k id)]
    (when (map? d)
      (wcar*
       (car/set k d)
       (if expire (car/expire k expire))))))

;; TODO: optimize hmget
(defn get
  "Get entity from cache, if not hit, read from db and then load it into the cache."
  ([key id db-get-fn expire]
   (get key id db-get-fn expire :all))
  ([key id db-get-fn expire fields]
   (let [k (redis-key key id)
         record (wcar* (car/get k))
         record (if record
                  record
                  (do
                    (reload key id db-get-fn expire fields)
                    (wcar* (car/get k))))]
     (cond
       (= :all fields)
       record

       (or (keyword? fields) (string? fields))
       (let [k1 (keyword fields)
             k2 (name fields)]
         {k1 (clojure.core/get record fields)})

       :else
       (select-keys record fields)))))

(defn del
  [key id]
  (wcar* (car/del (redis-key key id))))

;; cron job to fix corrupted data every day

(defn cursor
  [key {:keys [after before limit order]
        :or {limit 10
             order :desc}
        :as cursor}]
  (let [desc? (= order :desc)
        after (if (and after (instance? java.math.BigDecimal after))
                (double after)
                after)
        before (if (and before (instance? java.math.BigDecimal before))
                (double before)
                before)]
    (cond
     (and (nil? after) (nil? before))
     (if desc?
       (wcar* (car/zrevrange key 0 (dec limit)))
       (wcar* (car/zrange key 0 (dec limit))))

     after
     (some-> (if desc?
               (wcar* (car/zrevrangebyscore key after "-inf" "limit" 0 (inc limit)))
               (wcar* (car/zrangebyscore key after "+inf" "limit" 0 (inc limit))))
             (rest)
             (vec))

     :else
     (some-> (if desc?
               (wcar* (car/zrangebyscore key before "+inf" "limit" 0 (inc limit)))
               (wcar* (car/zrevrangebyscore key before "-inf" "limit" 0 (inc limit))))
             (rest)
             (vec)))))
