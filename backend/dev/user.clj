(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [com.stuartsierra.component :as component]
            [backend.application :as app]
            [api.config :refer [config]]
            [reloaded.repl :refer [system init]]
            [hikari-cp.core :as hikari]))

(defn dev-system []
  (app/app-system config))

(set-refresh-dirs "src" "dev")
(reloaded.repl/set-init! #(dev-system))

;; Set up aliases so they don't accidentally
;; get scrubbed from the namespace declaration
(def start reloaded.repl/start)
(def stop reloaded.repl/stop)
(def go reloaded.repl/go)
(def reset reloaded.repl/reset)
(def reset-all reloaded.repl/reset-all)

;; (defonce db {:datasource (hikari/make-datasource (:hikari-spec config))})

(defn make-db
  []
  {:datasource (hikari/make-datasource (:hikari-spec config))})

;; (defonce db {:datasource (hikari-cp.core/make-datasource (:hikari-spec api.config/config))})

(def me #uuid "d2f42955-e9e7-4cf1-aea4-91a7e163f293")
