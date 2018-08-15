(ns api.components.schedule
  (:require [com.stuartsierra.component :as component]
            [api.tick :as tick]
            [api.db.user :as user]
            [tick.schedule :as schedule]
            [clojure.java.jdbc :as j]))

(defrecord Schedule [hikari schedules]
  component/Lifecycle
  (start [component]
    ;; TODO: move to seperate component
    (j/with-db-connection [conn {:datasource (:datasource hikari)}]
      (user/load-pro! conn))
    (assoc component :schedules (tick/jobs {:datasource (:datasource hikari)})))
  (stop [component]
    (when (seq schedules)
      (doseq [schedule schedules]
        (schedule/stop schedule)))
    (println "schedule stoped!")
    (assoc component :schedules [])))

(defn new-schedule []
  (map->Schedule {}))
