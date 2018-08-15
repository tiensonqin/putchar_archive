(ns emoji
  (:require [cheshire.core :refer [parse-string]]
            [clojure.string :as str]))

;; parse from github emojis
;; https://api.github.com/emojis

(def emojis (->> (slurp "https://api.github.com/emojis")
                 (parse-string)
                 (reduce-kv (fn [m k v]
                              (assoc m k (-> v
                                             (str/replace "https://assets-cdn.github.com/images/icons/emoji/unicode/"
                                                          "")
                                             (str/replace "?v8" "")
                                             (str/replace ".png" "")))) {})
                 (spit "/tmp/emojis.edn")
                 ))
