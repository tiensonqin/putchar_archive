(ns ssr.components.app
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.resource :as resource]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ssr.middleware.bidi :refer [wrap-bidi]]
            [ssr.middleware.rum :refer [wrap-rum]]
            [ssr.middleware.auth :refer [wrap-auth]]
            [api.handler.middleware :refer [inject-context wrap-exception wrap-authenticate custom-wrap-cors wrap-production-etag wrap-production-gzip ssr-wrap-stats]]
            [ring.middleware.reload :refer [wrap-reload]]
            [aleph.http :refer [start-server]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [share.config :as config]
            [backend.servers :refer [start-server-or-die!]]
            [api.db.stat :as stat]
            [api.db.util :as du]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            ;; only for dev usage
            [api.components.aleph :as api]
            [share.util :as su]))

(def app
  (fn [req]
    {:status  200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body nil}))

(defn wrap-resource [handler]
  (fn [req]
    (cond->
      (if config/development?
        ((wrap-file handler "../web/public") req)
        ((resource/wrap-resource handler "public") req))
      (str/ends-with? (:uri req) ".js")
      (assoc-in [:headers "Content-Type"] "application/javascript"))))

(defn wrap-dev-api [handler]
  (fn [req]
    (if (and config/development? (str/starts-with? (:uri req) "/api/"))
      (let [result (@api/api-dev-handler (update req :uri (fn [x]
                                                            (if x
                                                              (subs x 4)
                                                              x))))]
        result)
      (handler req))))


;; TODO: reload not works
(defn middlewares
  [app context root-ui routes resolver render-page]
  (-> app
      (wrap-reload)
      ;; (wrap-exception)
      (wrap-rum root-ui resolver render-page)
      (wrap-dev-api)
      (wrap-resource)
      (wrap-production-etag)
      (wrap-auth)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-restful-format)
      (ssr-wrap-stats)
      (inject-context context)
      (wrap-bidi routes)
      (wrap-cookies)
      (custom-wrap-cors)
      (wrap-production-gzip)))

(defrecord Ssr [hikari redis ssr-server ssr-port root-ui routes resolver render-page rpc]
  component/Lifecycle
  (start [component]
    (let [context {:redis (:connection redis)
                   :datasource {:datasource (:datasource hikari)}}
          handler (-> #'app
                      (middlewares context root-ui routes resolver render-page))
          ssr-server (start-server-or-die!
                      :ssr-server
                      ssr-port
                      (fn []
                        (start-server handler {:port ssr-port
                                               :join? false})))]
      (assoc component
             :ssr-server ssr-server)))
  (stop [component]
    (when ssr-server
      (.close ssr-server))
    (dissoc component :ssr-server)))

(defn new-ssr-server [ssr-port root-ui routes resolver render-page]
  (map->Ssr {:ssr-port ssr-port
             :root-ui root-ui
             :routes routes
             :resolver resolver
             :render-page render-page}))
