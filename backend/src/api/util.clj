(ns api.util
  (:require [api.flake.core :as flake]
            [api.flake.utils :as fu]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [api.services.rss.export :as rss]
            [clj-time
             [coerce :as tc]
             [core :as t]
             [format :as tf]])
  (:import  [java.util UUID]))

(defn production?
  []
  (= "production" (:env env)))

(defn uuid
  "Generate uuid."
  []
  (UUID/randomUUID))

(defn ->uuid
  [s]
  (if (uuid? s)
    s
    (UUID/fromString s)))

(defn update-if
  "Update m if k exists."
  [m k f]
  (if-let [v (get m k)]
    (assoc m k (f v))
    m))

(defn flake-id
  []
  (flake/generate!))

(defn flake-id->str
  []
  (fu/base62-encode (flake-id)))

(defn get-avatar
  [avatar]
  (let [type (cond
               (re-find #"google" avatar)
               :google

               :else
               :s3)]
    (cond
      (= :google type)
      (str/replace avatar "/s120/" "/s360/")

      :else
      avatar)))

(defn ok
  ([body]
   (ok body nil))
  ([body headers]
   (cond->
     {:status 200
      :body body}
     headers
     (assoc :headers headers))))

(defn bad
  [message]
  {:status 400
   :body {:message (str message)}})

(def not-found
  {:status 404
   :message "Not Found"})

(defn ->response
  [[type data]]
  (if (= type :ok)
    (ok data)
    (bad data)))


(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defmacro doseq-indexed
  "loops over a set of values, binding index-sym to the 0-based index of each value"
  ([[val-sym values index-sym] & code]
   `(loop [vals# (seq ~values)
           ~index-sym (long 0)]
      (if vals#
        (let [~val-sym (first vals#)]
          ~@code
          (recur (next vals#) (inc ~index-sym)))
        nil))))

(defn indexed [coll] (map-indexed vector coll))

(defn rss
  [channel items]
  {:status 200
   :body (rss/channel-xml channel
                          items)
   :headers {"Content-Type" "application/rss+xml; charset=utf-8"}})

(def not-found-resp
  {:status 404
   :body "<!DOCTYPE html>\n<html><body>not found</body></html>"})

(defn hours-ago
  [created-at]
  (/ (t/in-seconds (t/interval (tc/to-date-time created-at) (t/now))) 60))

(defn ranking
  "Hacker News ranking:
  Score = (P - 1) / (T+2)^G,
  where,
  P = points of an item (and -1 is to negate submitters vote)
  T = time since submission (in hours)
  G = Gravity, defaults to 1.8 in news.arc"
  ([points created_at]
   (ranking points created_at 1.8))
  ([points created_at gravity]
   (let [hours (hours-ago created_at)
         points (if (= points 1) 0.999999 points)]
     (/
      points
      ;; (dec points)
      (Math/pow (+ 2 hours) gravity)))))
