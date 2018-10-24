(ns api.services.github.sync
  (:require [api.config :refer [config]]
            [clojure.java.shell :as shell]
            [share.util :as util]
            [api.services.slack :as slack]))

(defonce path (get-in config [:books :repos-path]))

;; TODO: don't block
(defn clone
  [repo]
  (let [[handle repo-name] (util/get-github-handle-repo repo)
        {:keys [exit]} (shell/sh "bash" "-c" (format "git clone --depth 1 %s %s" repo (str path "/" handle "-" repo-name)))]
    (when (not= exit 0)
      (slack/error "Github sync project error: " repo))))
