(ns api.services.github.contents
  (:refer-clojure :exclude [get update])
  (:require [api.services.github :as github]))

(defn get
  [owner repo path & [options]]
  (github/api-call :get "repos/%s/%s/contents/%s"
                   [owner repo path] options))

;; create a file
(defn create
  [owner repo path message content & [options]]
  (github/api-call :put "repos/%s/%s/contents/%s"
                   [owner repo path]
                   (merge {:path path
                           :message message
                           :content (github/base64-encode content)}
                          options)))

;; update a file
(defn update
  [owner repo path message content & [options]]
  (when-let [sha (:sha (get owner repo path))]
    (github/api-call :put "repos/%s/%s/contents/%s"
                     [owner repo path]
                     (merge {:path path
                             :message message
                             :content (github/base64-encode content)
                             :sha sha}
                            options))))

;; delete a file
(defn delete
  [owner repo path message & [options]]
  (when-let [sha (:sha (get owner repo path))][]
            (github/api-call :delete  "repos/%s/%s/contents/%s"
                             [owner repo path]
                             (merge {:path path
                                     :message message
                                     :sha sha}
                                    options))))
