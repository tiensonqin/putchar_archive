(ns share.org-mode
  (:refer-clojure :exclude [load])
  (:require #?(:clj [clojure.java.shell :as shell])
            #?(:clj [api.services.slack :as slack])
            #?(:cljs [web.loader :as loader])
            [share.config :as config]
            [clojure.string :as str]
            [share.front-matter :as fm]))

(defn loaded? []
  #?(:cljs js/window.Orgmode
     :clj true))

(defn load []
  #?(:cljs
     (loader/load (str config/website "/orgmode.js"))))

(defn render
  "Invokes mlorg which returns html if successed."
  [content]
  #?(:clj
     (when (and content (not (str/blank? content)))
       (let [content (fm/remove-front-matter content)
             {:keys [exit out err]}
             (shell/sh "/usr/local/bin/mlorg"
                       :in content)]
         (if (not (zero? exit))
           (do
             (slack/error "mlorg parser error:" err content)
             content)
           out)))
     :cljs
     (when (loaded?)
       (.parse js/window.Orgmode content))))
