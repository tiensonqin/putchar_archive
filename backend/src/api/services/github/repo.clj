(ns api.services.github.repo
  (:refer-clojure :exclude [get update])
  (:require [api.services.github :as github]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]))

(defn create
  [params & [options]]
  (github/api-call :post "user/repos"
                   []
                   (merge
                    params
                    options)))

(defn add-hook
  [owner repo params & [options]]
  (github/api-call :post "repos/%s/%s/hooks"
                   [owner repo]
                   (merge
                    params
                    options)))
