(ns api.services.org-mode
  (:require [clojure.java.shell :as shell]
            [api.services.slack :as slack]
            [clojure.string :as str]))

(defn render
  "Invokes mlorg which returns html if successed."
  [content]
  (when (and content (not (str/blank? content)))
    (let [{:keys [exit out err]}
         (shell/sh "/usr/local/bin/mlorg"
                   :in content)]
     (if (not (zero? exit))
       (do
         (slack/error "mlorg parser error:" err content)
         content)
       out))))
