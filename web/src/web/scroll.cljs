(ns web.scroll
  (:require [appkit.macros :refer [oget oset!]]))

(def on-scroll-switch (atom true))

(defn close!
  []
  (reset! on-scroll-switch false))

(defn open!
  []
  (reset! on-scroll-switch true))

(defn into-view
  [element]
  (close!)

  (let [scroll-top (oget element "offsetTop")
        scroll-top (- scroll-top 80)]
    ;; set scrollTop
    (oset! js/window "scrollTop" scroll-top)

    (.scroll js/window #js {:top scroll-top
                            ;; :behavior "smooth"
                            }))

  (js/setTimeout open! 1000))
