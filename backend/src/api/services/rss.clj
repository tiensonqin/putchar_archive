(ns api.services.rss
  (:refer-clojure :exclude [get])
  (:require [api.services.rss.import :refer [parse-rss]]
            [api.services.slack :as slack]))

;; Map uri {:posts [] :last_fetched_at datetime}
(def cache (atom {}))

(defn get
  [uri]
  (if uri
    (if-let [posts (cache uri)]

     (if-let [result (parse-rss uri)]
       (do
         (swap! cache assoc uri result)
         result)
       (do
         (slack/error "RSS no results: " uri)
         [])))))
