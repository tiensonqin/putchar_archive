#!/usr/bin/env lumo

(ns deploy.core
  (:require [cljs.reader :as reader]
            [cljs.nodejs :as nodejs]
            [clojure.string :as str]
            [clojure.set :as set]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.object :as gobj]))

;; TODO: add rollback, mv _next/* to archive, mv _prev/* to _next

(nodejs/enable-util-print!)
(def fs (js/require "fs"))

(def child-process (js/require "child_process"))
(def exec-sync (gobj/get child-process "execSync"))
(def exec (gobj/get child-process "exec"))

(def uuid-re #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

(defonce version-path "../share/share/version.cljc")
(defonce pre-version (atom nil))
(defonce new-version (random-uuid))

(defn -main [& args]
  (exec-sync "rm -f public/main*.js")
  (exec-sync "rm -f public/*.css")
  (exec-sync "rm -f ../backend/resources/public/main*.js")
  (exec-sync "rm -f ../backend/resources/public/style-*.css")
  (exec-sync "yarn release")
  (exec-sync (str "cp public/js/compiled/main.js "  (str "public/main-" new-version ".js")))
  (exec-sync "cleancss -o public/style.css public/css/style.css")
  (exec-sync (str "mv public/style.css "  (str "public/style-" new-version ".css")))
  (exec-sync "cd public && cp -R .well-known *.html *.png *.txt *.xml *.ico images manifest.json *.css main*.js orgmode.js ../../backend/resources/public"))

(set! *main-cli-fn* -main)
