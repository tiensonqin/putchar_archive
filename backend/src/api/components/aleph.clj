;; (ns api.components.aleph
;;   (:require [com.stuartsierra.component :as component]
;;             [aleph.http :refer [start-server]]
;;             [api.handler :as handler]
;;             [api.components.sente :as sente]))

;; (defrecord WebServer [sente server http-port]
;;   component/Lifecycle
;;   (start [component]
;;     (let [handler (handler/app-handler (sente/sente-routes sente)
;;                                        (:context sente))
;;           server (start-server handler {:port http-port
;;                                         :join? false})]
;;       (assoc component :server server)))
;;   (stop [component]
;;     (when server
;;       (.close server))
;;     component))

;; (defn new-web-server
;;   [http-port]
;;   (map->WebServer {:http-port http-port}))
