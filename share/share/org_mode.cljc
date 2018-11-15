(ns share.org-mode
  (:refer-clojure :exclude [load])
  (:require #?(:clj [clojure.java.shell :as shell])
            #?(:clj [api.services.slack :as slack])
            #?(:cljs [web.loader :as loader])
            [share.config :as config]
            [clojure.string :as str]
            [share.front-matter :as fm]))

(defn loaded? []
  #?(:cljs js/window.MldocOrg
     :clj true))

(defn load []
  #?(:cljs
     (loader/load (str config/website "/orgmode.js"))))

(defn render
  "Invokes mlorg which returns html if successed."
  [content]
  #?(:clj
     (when (and content (not (str/blank? content)))
       (let [content content
             _ (prn {:content content})
             {:keys [exit out err]}
             (shell/sh "/usr/local/bin/mldoc_org"
                       :in content)]
         (if (not (zero? exit))
           (do
             (slack/error "mldoc_org parser error:" err content)
             content)
           (do
             (prn out)
             out))))
     :cljs
     (when (loaded?)
       (.parseHtml js/window.MldocOrg content))))
