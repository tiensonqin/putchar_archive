(ns shadow.user
  (:require [shadow.cljs.devtools.api :as api]))

(defn cljs-repl
  []
  (api/repl :app))
