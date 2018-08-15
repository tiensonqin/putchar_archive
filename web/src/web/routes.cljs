(ns web.routes
  (:require [appkit.citrus.core :as citrus]
            [clojure.string :as str]
            [bidi.bidi :as bidi]
            [web.pushy :as pushy]
            [goog.events :as events]
            [share.util :as util]
            [share.routes :as routes]))

(defn push-state!
  ([state title]
   (.pushState js/history state title))
  ([state title path]
   (.pushState js/history state title path)))

(defn start! [reconciler]
  (let [history (pushy/pushy
                 #(citrus/dispatch! reconciler :router/push % false)
                 (fn [uri]
                   (citrus/dispatch! reconciler :user/close-signin-modal?)
                   (if (re-find #"/auth" uri)
                     nil
                     (routes/match-route-with-query-params uri))))]
    (pushy/start! history)
    history))
