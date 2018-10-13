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

(defn load-mlorg
  []
  (load "https://putchar.org/mlorg.js"))
