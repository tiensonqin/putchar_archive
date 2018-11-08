(ns api.components.aleph
  (:require [com.stuartsierra.component :as component]
            [aleph.http :refer [start-server]]
            [api.handler :as handler]
            [share.util :as util]))

(defonce api-dev-handler (atom nil))

(defrecord WebServer [hikari redis server http-port]
  component/Lifecycle
  (start [component]
    (let [context {:redis (:connection redis)
                   :datasource {:datasource (:datasource hikari)}}
          handler (handler/app-handler context)
          server (start-server handler {:port http-port
                                        :join? false})]
      (when util/development?
        (reset! api-dev-handler handler))
      (assoc component :server server)))
  (stop [component]
    (when server
      (.close server))
    component))

(defn new-web-server
  [http-port]
  (map->WebServer {:http-port http-port}))
