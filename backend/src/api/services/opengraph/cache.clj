(ns api.services.opengraph.cache
  (:refer-clojure :exclude [get])
  (:require [api.db.cache :as cache]
            [taoensso.carmine :as car]))

(defonce cache-key "opengraph-cache")
;; hset

(defn get
  [url]
  (cache/wcar*
   (car/hget cache-key url)))

(defn insert
  [url data]
  (cache/wcar*
   (car/hset cache-key url data)))
