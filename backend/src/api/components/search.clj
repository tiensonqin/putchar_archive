(ns api.components.search
  (:require [com.stuartsierra.component :as component]
            [api.db.search :as search]
            [api.config :refer [config]]))

(defrecord Search []
  component/Lifecycle
  (start [component]
    (println "search started!")
    (search/init!)
    ;; TODO: index store
    (assoc component :search nil))
  (stop [component]
    (println "search stoped!")
    (assoc component :search nil)))

(defn new-search []
  (map->Search {}))
