(ns api.services.github.user
  (:refer-clojure :exclude [get update])
  (:require [api.services.github :as github]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]))

(defn get-emails
  [& [options]]
  (github/api-call :get "user/emails"
                   nil options))
