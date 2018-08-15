(ns backend.application
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            ;; [api.components.aleph :as aleph]

            [api.components.http-kit :as http-kit]
            [api.components.hikari :as hikari]
            [api.components.redis :as redis]
            [api.components.repl :as repl]
            [api.components.flake :as flake]
            [api.components.search :as search]
            [api.components.schedule :as schedule]
            [ssr.components.app :as ssr]
            [ssr.page :refer [render-page]]
            [ssr.resolver :refer [make-resolver]]
            [api.config :as config]
            [api.util :as util]
            [share.routes :refer [routes]]
            [share.components.root :as root-ui]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]))

(defn app-system [{:keys [http-port
                          ssr-port
                          env
                          hikari-spec
                          redis]
                   :as config}]
  (component/system-map
   :flake          (flake/new-flake)
   :search         (search/new-search)
   :hikari         (hikari/new-hikari-cp hikari-spec)
   :repl           (repl/new-repl)
   :redis          (redis/new-redis redis)

   :schedule       (component/using
                    (schedule/new-schedule)
                    [:hikari])

   :web-server     (component/using
                    ;; (aleph/new-web-server http-port)
                    (http-kit/new-web-server http-port)
                    [:hikari :redis])
   :ssr-server     (component/using
                    ;; (aleph/new-web-server http-port)
                    (ssr/new-ssr-server ssr-port root-ui/root routes make-resolver render-page)
                    [:hikari :redis])))

(defn -main [& _]
  ;; log
  (timbre/merge-config! (cond->
                          {:appenders {:spit (appenders/spit-appender {:fname (:log-path config/config)})}}
                          (util/production?)
                          (assoc :output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}}))))

  (-> (app-system config/config)
      component/start))
