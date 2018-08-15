(ns api.components.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as r]))

(defrecord Redis [spec]
  component/Lifecycle
  (start [component]
    (assoc component :connection spec))
  (stop [component]
    (assoc component :connection nil)))

(defn new-redis [spec]
  (map->Redis {:spec spec}))
