(ns web.loader
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [goog.net.jsloader :as jsloader]
            [goog.html.legacyconversions :as conv]
            [share.config :as config]
            [share.util :as util]
            [share.dommy :as dommy]))

(defn load [url]
  (let [ch (async/promise-chan)
        loader ^js (jsloader/safeLoad (conv/trustedResourceUrlFromString (str url)))]
    (.addCallback loader (fn [_result]
                           (async/put! ch :loaded)))
    ch))

(defn load-highlight
  []
  (let [url (str config/website "/highlight.min.js")]
    (go
      (async/<! (load url))
      (util/highlight!))))

(defn load-math
  []
  (load "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.4/MathJax.js?config=TeX-MML-AM_CHTML"))

(defn load-stripe-checkout
  []
  (load "https://checkout.stripe.com/checkout.js"))
