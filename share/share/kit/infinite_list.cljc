(ns share.kit.infinite-list
  (:require #?(:cljs [goog.dom :as gdom])
            #?(:cljs [goog.style :as style])
            #?(:cljs [appkit.macros :refer [oget]])
            [share.kit.mixins :as mixins]
            [rum.core :as rum]
            [share.util :as util]
            [appkit.citrus :as citrus]))

#?(:cljs
   (defn on-scroll
     [on-load]
     (let [cont js/document.body
           full-height (oget cont "scrollHeight")
           viewport-height js/window.innerHeight
           scrolled (or js/window.pageYOffset
                        js/document.documentElement.scrollTop
                        js/document.body.scrollTop)
           scrolled-bottom (+ scrolled viewport-height)
           bottom-reached? (>= scrolled-bottom (- full-height 500))]
       (let [scroll-top (util/scroll-top)]
         (citrus/dispatch! :citrus/set-scroll-top (util/get-current-url) scroll-top))
       (when bottom-reached?
         (on-load)))))

(defn attach-listeners
  "Attach scroll and resize listeners."
  [state]
  #?(:cljs (let [opts (-> state :rum/args second)
                 debounced-on-scroll (util/debounce 500 #(on-scroll (:on-load opts)))]
             (mixins/listen state js/window :scroll debounced-on-scroll))
     :clj nil))

(rum/defcs infinite-list <
  (mixins/event-mixin attach-listeners)
  "Render an infinite list."
  [state list-items {:keys [on-load]
                     :as opts}]
  (for [[index list-item] (map-indexed vector list-items)]
    (rum/with-key list-item index)))
