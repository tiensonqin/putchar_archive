(ns api.components.repl
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.nrepl.server :as nrepl]))

(defrecord Repl [repl]
  component/Lifecycle
  (start [component]
    (assoc component :repl
           (nrepl/start-server
            :port 8992
            :bind "127.0.0.1")))
  (stop [component]
    (if repl (nrepl/stop-server repl))
    (dissoc component :repl)))

(defn new-repl []
  (map->Repl {}))
