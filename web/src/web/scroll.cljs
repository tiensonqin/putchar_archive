(ns web.scroll
  (:require [appkit.macros :refer [oget oset!]]))

(defn into-view
  [element]
  (let [scroll-top (oget element "offsetTop")
        scroll-top (- scroll-top 80)]
    ;; set scrollTop
    (oset! js/window "scrollTop" scroll-top)

    (.scroll js/window #js {:top scroll-top
                            ;; :behavior "smooth"
                            })))
