(ns api.services.rss.import
  (:use [clojure.xml :as xml]
        [clojure.set :refer [difference]]
        [clojure.string :as str]
        [clojure.java.io :as io]))

(comment
  (def hackernews-uri "https://news.ycombinator.com/rss"))

(defn- get-val [entry tag]
  (-> (filter #(= tag (:tag %)) (:content entry))
      first
      :content
      first))

(defn- get-entry
  [entry]
  {:title (get-val entry :title)
   :link (get-val entry :link)
   :pubDate (get-val entry :pubDate)
   :comments (get-val entry :comments)})

(defn parse-rss
  [uri]
  (some->> uri
          (xml/parse)
          (xml-seq)
          (filter #(= :item (:tag %)))
          (map get-entry)))
