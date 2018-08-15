(ns api.services.imgix
  (:require [org.httpkit.client :as client]
            [api.config :refer [config]]
            [cheshire.core :refer [generate-string]]
            [clojure.string :as str]
            [api.services.slack :as slack]))

(defonce api-url "https://api.imgix.com/v2")

(defn path
  [& args]
  (str/join "/" (cons api-url args)))

(defn with-credentials
  [m]
  (merge {:basic-auth [(:imgix-key config) ""]
          :headers {"Content-type" "application/json"
                    "Accept" "application/json"}}
         m))

(defn- try-post
  [& args]
  (loop [times 3]
    (let [resp (apply client/post args)
          will-retry? (and (not (zero? times))
                           (not= 200 (:status resp)))]
      (if will-retry?
        (recur (dec times))
        resp))))

(defn purger
  [url]
  (let [params {:url (str (:img-cdn config) url)}
        {:keys [status body]} @(try-post (path "image" "purger")
                          (with-credentials {:body (generate-string params)}))]
    (if (not= status 200)
      (slack/error "imgix purger error: " {:status status
                                           :body body
                                           :url url}))))
