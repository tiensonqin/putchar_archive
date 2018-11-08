(ns api.services.opengraph
  (:require [api.services.opengraph.core :refer [parse block-parse]]
            [api.services.opengraph.cache :as cache]))

;; TODO: run as a standalone process

(defn query
  [url ok-handler error-handler]
  (if-let [result (cache/get url)]
    (ok-handler result)
    (parse url
           (fn [data]
             (cache/insert url data)
             (ok-handler data))
           error-handler)))

(defn block-query
  [url error-handler]
  (if-let [result (cache/get url)]
    result
    (block-parse url error-handler)))

(comment
  (query "https://putchar.org"
    prn prn))
