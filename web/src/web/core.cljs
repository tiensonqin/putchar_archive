(ns web.core
  (:require [rum.core :as rum]
            [goog.dom :as dom]
            [appkit.citrus :as citrus]
            [share.reconciler :refer [reconciler]]
            [share.config :as config]
            [share.dicts :as dicts]
            [share.util :as util]
            [share.components.root :as root]
            [web.routes :as routes]
            [appkit.cookie :as cookie]
            [appkit.storage :as storage]
            [bidi.bidi :as bidi]
            [share.routes]
            [share.asciidoc :as asciidoc]
            [appkit.db :as db]
            [cognitect.transit :as t]
            [cljs.core.async :as async]
            [web.loader :as loader]
            [web.stripe :as stripe]
            [share.emoji :as emoji])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defn start
  [user?]
  (citrus/dispatch-sync! :router/setup-pushy-history
                         (routes/start! reconciler))

  (when (and js/history js/history.scrollRestoration)
    (set! (.-scrollRestoration js/history) "manual"))

  (citrus/dispatch-sync! :citrus/cache-server-first-reply)

  (rum/hydrate (root/root reconciler)
               (dom/getElement "app")))

(defn ^:export init [state]
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (let [state (t/read (t/reader :json) state) ;; read server state
        layout (util/get-layout)
        locale           (or
                          (keyword (cookie/cookie-get :locale))
                          (:locale state))
        zh-cn? (= locale :zh-cn)
        current-user (get-in state [:user :current])
        emojis (storage/get :emojis)]
    (reset! dicts/locale locale)
    (if emojis (reset! emoji/emojis emojis))
    (reset! db/state
            (-> state
                (assoc :layout {:current layout})
                (assoc :latest-body-format (if-let [format (storage/get :latest-body-format)]
                                             format
                                             :markdown))
                (assoc-in [:last-scroll-top (util/get-current-url)] (util/scroll-top))
                (update-in [:group :by-name]
                           (fn [m]
                             (into (util/normalize :name
                                                   (->> (concat (get-in state [:group :hot])
                                                                (get-in state [:user :current :stared_groups]))
                                                        (remove nil?)))
                                   m)))
                (update-in [:user :current]
                           (fn [m]
                             (if m
                               (update m :stared_groups util/normalize))))
                (assoc :locale locale)
                (assoc-in [:comment :liked-comments]
                          (storage/get :liked-comments))
                (assoc-in [:comment :drafts]
                          (storage/get :comments-drafts))))

    (go
      (async/<! (asciidoc/load-ascii))
      (start (some? current-user))

      (loader/load-highlight)

      ;; load MathJax
      (loader/load-math)

      ;; stripe checkout
      ;; (loader/load-stripe-checkout)
      )

    (when (nil? emojis)
      (citrus/dispatch! :data/pull-emojis))

    (when current-user
      (citrus/dispatch! :user/poll))))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
