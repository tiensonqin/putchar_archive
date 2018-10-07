(ns web.handlers.ui
  (:require [share.util :as util]
            [clojure.string :as str]
            [appkit.promise :as p]
            [share.dommy :as dommy]
            [share.kit.colors :as colors]))

(def handlers
  {:layout/change
   (fn [state layout]
     {:state {:current layout}})

   :citrus/set-scroll-top
   (fn [state current-url v]
     (let [old-v (get-in state [:last-scroll-top current-url])
           v (if old-v v 0)
           current-path (get-in state [:router :handler])]
       (when (and (zero? v)
                  (not (contains? #{:comment} current-path))
                  (str/blank? js/window.location.hash))
         (.scroll js/window #js {:top 0
                                 ;; :behavior "smooth"
                                 }))
       {:state (assoc-in state [:last-scroll-top current-url] v) })
     )

   :citrus/reset-search-mode?
   (fn [state v]
     {:state (assoc state :search-mode? v)})

   :citrus/toggle-search-mode?
   (fn [state]
     {:state (update state :search-mode? not)})

   :citrus/hide-github-connect
   (fn [state]
     {:state (assoc state :hide-github-connect? true)
      :cookie [:set-forever "hide-github-connect" true]})

   :citrus/hide-votes
   (fn [state]
     {:state (assoc state :hide-votes? true)
      :cookie [:set-forever "hide-votes" true]})

   :citrus/show-votes
   (fn [state]
     {:state (assoc state :hide-votes? false)
      :cookie [:set-forever "hide-votes" false]})

   :citrus/touch-start
   (fn [state e]
     {:state {:touch {:touching? true
                      :start-x (.-screenX e)
                      :start-y (.-screenY e)}}})

   :citrus/touch-end
   (fn [state e]
     (let [{:keys [start-x start-y] :as touch} (:touch state)
           end-x (.-screenX e)
           end-y (.-screenY e)
           offset (- end-x start-x)
           direction (if (> offset 0) :right :left)
           open? (and (= direction :right) (> offset 30)
                      (< start-x 50))
           close? (and (= direction :left) (> offset 30))]
       {:state {:open-drawer? (cond
                                close?
                                false
                                open?
                                true
                                :else
                                false)}}))

   :citrus/open-drawer?
   (fn [state]
     {:state {:open-drawer? true}})

   :citrus/close-drawer?
   (fn [state]
     {:state {:open-drawer? false}})

   :citrus/toggle-drawer?
   (fn [state]
     {:state {:open-drawer? (not (:open-drawer? state))}})



   })
