(ns api.components.schedule
  (:require [com.stuartsierra.component :as component]
            [api.db.user :as user]
            [api.tick :as tick]
            [clojure.java.jdbc :as j]))

(defrecord Schedule [hikari schedules]
  component/Lifecycle
  (start [component]
    (assoc component :schedules (tick/jobs {:datasource (:datasource hikari)})))
  (stop [component]
    (when (seq schedules)
      (doseq [schedule schedules]
        (schedule)))
    (println "schedule stoped!")
    (assoc component :schedules [])))

(defn new-schedule []
  (map->Schedule {}))
