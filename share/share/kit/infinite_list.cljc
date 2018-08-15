(ns share.kit.infinite-list
  (:require #?(:cljs [goog.dom :as gdom])
            #?(:cljs [goog.style :as style])
            #?(:cljs [appkit.macros :refer [oget]])
            [share.kit.mixins :as mixins]
            [rum.core :as rum]
            [share.util :as util]))

;; copy from https://github.com/burningswell/web/blob/8688f261167d54ad9753a760e6d7d26e4decbde9/src/clj/burningswell/web/ui/infinite_list.cljc

(defn debounce
  "Returns a function that will call f only after threshold has passed without new calls
  to the function. Calls prep-fn on the args in a sync way, which can be used for things like
  calling .persist on the event object to be able to access the event attributes in f"
  ([threshold f] (debounce threshold f (constantly nil)))
  ([threshold f prep-fn]
   #?(:clj nil
      :cljs
      (let [t (atom nil)]
        (fn [& args]
          (when @t (js/clearTimeout @t))
          (apply prep-fn args)
          (reset! t (js/setTimeout #(do
                                      (reset! t nil)
                                      (apply f args))
                                   threshold)))))))

(defn page-height
  "Return the height of the page."
  []
  #?(:cljs js/document.documentElement.clientHeight))

(defn scroller
  "Return the scroller of `event`."
  [state event]
  (let [target (.-target event)]
    (or (aget target "scroller") target)))

(defn scroll-content-height
  "Return the height of the `event` scroller."
  [state event]
  #?(:clj nil
     :cljs
     (or (.-scrollHeight (.-scrollingElement (scroller state event)))
         (.-scrollHeight (.-scrollingElement js/document)))))

(defn scroll-header-height
  "Return the header height of the `event` scroller."
  [state event]
  #?(:clj nil
     :cljs (if-let [header (-> (oget event "target")
                               (oget "header"))]
             (-> (style/getSize header)
                 (oget "height"))
             0)))

(defn scroll-visible-height
  "Return the content height of the `event` scroller."
  [state event]
  (- (page-height) (scroll-header-height state event)))

(defn content-height
  "Return the height of the content."
  [state]
  #?(:cljs (let [node (rum/dom-node state)]
             (.-clientHeight node))))

(defn loading?
  "Returns true if the component is loading new data, otherwise false."
  [state]
  (-> state :scroll-loading? deref))

(defn list-items
  "Return the list items from `state`."
  [state]
  (-> state :rum/args first))

(defn options
  "Return the options from `state`."
  [state]
  (-> state :rum/args second))

(defn on-load-handler
  "Returns the :on-load handler."
  [state]
  (-> state options :on-load))

(defn threshold
  "Returns the :on-load handler."
  [state]
  (or (-> state options :threshold) 100))

(defn bottom-reached?
  "Returns true if the scroll position is near the bottom of the page."
  [state event]
  (neg? (- (scroll-content-height state event)
           (scroll-visible-height state event)
           (util/scroll-top)
           (threshold state))))

(defn load-next
  "Fetch the next page."
  [state]
  (when-let [handler (on-load-handler state)]
    (when-not (loading? state)
      (reset! (:scroll-loading? state) true)
      (handler state))))

(defn on-scroll
  "The scroll event handler."
  [state event]
  (when (bottom-reached? state event)
    (load-next state)))

(defn on-resize
  "The resize event handler."
  [state event]
  (when (< (content-height state) (page-height))
    (load-next state)))

(defn scroll-target
  "Return the scroll target from `state`."
  [state]
  (let [scroll-target (-> state options :scroll-target)]
    #?(:cljs (cond
               (nil? scroll-target)
               js/window
               (string? scroll-target)
               (or (some-> (js/document.getElementsByTagName scroll-target)
                           array-seq first)
                   (gdom/getElementByClass scroll-target))
               (ifn? scroll-target)
               (scroll-target)
               :else scroll-target))))

(defn attach-listeners
  "Attach scroll and resize listeners."
  [state]
  #?(:cljs (when-let [target (scroll-target state)]
             (let [debouced-on-resize (debounce 300 #(on-resize state %))
                   debouced-on-scroll (debounce 300 #(on-scroll state %))]
               (mixins/listen state js/window :resize debouced-on-resize)
               (mixins/listen state target :resize debouced-on-resize)
               (mixins/listen state target :scroll debouced-on-scroll)))
     :clj nil))

(def infinite-list-mixin
  "The infinite list mixin."
  {:did-remount
   (fn [old-state new-state]
     (when (and (loading? new-state)
                (> (-> new-state list-items count)
                   (-> old-state list-items count)))
       (reset! (:scroll-loading? new-state) false))
     (when-not (= (options old-state) (options new-state))
       (mixins/detach old-state)
       (attach-listeners new-state))
     new-state)
   :did-mount
   (fn [state]
     (attach-listeners state)
     state)})

(rum/defcs infinite-list <
  (rum/local false :scroll-loading?)
  (merge (mixins/event-mixin attach-listeners)
         infinite-list-mixin)
  "Render an infinite list."
  [state list-items {:keys [on-load]
                     :as opts}]
  (for [[index list-item] (map-indexed vector list-items)]
    (rum/with-key list-item index)))
