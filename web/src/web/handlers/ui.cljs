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

   :layout/show-panel
   (fn [state]
     (dommy/set-style! (dommy/sel1 "body") "overflow" "hidden")
     {:state {:show-panel? true}})

   :layout/close-panel
   (fn [state]
     (dommy/set-style! (dommy/sel1 "body") "overflow" "inherit")
     {:state {:show-panel? false}})

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

   })
