(ns share.kit.mixins
  (:require [rum.core :as rum]
            [appkit.citrus :as citrus]
            [share.util :as util]
            [share.query :refer [queries]]
            [share.dommy :as dommy]
            #?(:cljs [appkit.db :as db])
            #?(:cljs ["prop-types" :as prop-types])
            #?(:cljs [goog.dom :as dom]))
  #?(:cljs (:import [goog.events EventHandler])))

(defn detach
  "Detach all event listeners."
  [state]
  #?(:cljs (some-> state ::event-handler .removeAll)
     :clj nil))

(defn listen
  "Register an event `handler` for events of `type` on `target`."
  [state target type handler & [opts]]
  #?(:cljs (when-let [event-handler (::event-handler state)]
             (.listen event-handler target (name type) handler (clj->js opts)))
     :clj nil))

(def event-handler-mixin
  "The event handler mixin."
  {:will-mount
   (fn [state]
     (assoc state ::event-handler #?(:cljs (EventHandler.)
                                     :clj nil)))
   :will-unmount
   (fn [state]
     (detach state)
     (dissoc state ::event-handler))})

(defn timeout-mixin
  "The setTimeout mixin."
  [name t f]
  {:will-mount
   (fn [state]
     (assoc state name (util/set-timeout t f)))
   :will-unmount
   (fn [state]
     (let [timeout (get state name)]
       (util/clear-timeout timeout)
       (dissoc state name)))})

(defn interval-mixin
  "The setInterval mixin."
  [name t f]
  {:will-mount
   (fn [state]
     (assoc state name (util/set-interval t f)))
   :will-unmount
   (fn [state]
     (when-let [interval (get state name)]
       (util/clear-interval interval))
     (dissoc state name))})

(def scroll-to-bottom
  {:did-update (fn [state]
                 #?(:cljs
                    (if-let [node (rum/dom-node state)]
                      (set! (.-scrollTop node) (.-scrollHeight node))))
                 state)})

(defn esc-listeners
  [state open? & {:keys [on-close stop?]}]
  #?(:clj
     nil

     :cljs
     (let [node (rum/dom-node state)]
       (when open?
         (listen state js/window "click"
                 (fn [e]
                   ;; If the click target is outside of current node
                   (if (and (or (nil? stop?)
                                (false? @stop?))
                            (not (dom/contains node (.. e -target))))
                     (on-close e))))

         (listen state js/window "keydown"
                 (fn [e]
                   (case (.-keyCode e)
                     ;; Esc
                     27 (on-close e)
                     nil)))))))

(defn event-mixin
  [attach-listeners]
  (merge
   event-handler-mixin
   {:did-mount (fn [state]
                 (attach-listeners state)
                 state)
    :did-remount (fn [old-state new-state]
                   (detach old-state)
                   (attach-listeners new-state)
                   new-state)}))

(defn dispatch-on-mount
  ([event]
   (dispatch-on-mount event identity))
  ([event data-fn]
   {:did-mount
    (fn [state]
      (util/debug :did-mount {:event event})
      (citrus/dispatch! event (data-fn (first (:rum/args state))))
      state)
    :did-remount
    (fn [old state]
      (util/debug :did-remount {:event event})
      (when (not= (:rum/args old) (:rum/args state))
        (citrus/dispatch! event (data-fn (first (:rum/args state)))))
      state)}))

(defn disable-others-tabindex
  [css-selector]
  {:after-render (fn [state]
                   ;; disable tab-index
                   (let [anchors (dommy/sel css-selector)]
                     (when (seq anchors)
                       (doseq [anchor anchors]
                         (dommy/set-attr! anchor :tabindex "-1"))))
                   state)
   :will-unmount (fn [state]
                   (let [anchors (dommy/sel "a:not(.no-tabindex)")]
                     (when (seq anchors)
                       (doseq [anchor anchors]
                         (dommy/set-attr! anchor :tabindex "0"))))
                   state)})

(defn query
  [route-handler]
  (when-let [q (get queries route-handler)]
    {:child-context (fn [state]
                      {:route-handler (name route-handler)
                       :args (pr-str (first (:rum/args state)))})
     :static-properties {:childContextTypes
                         {:route-handler #?(:cljs prop-types/string
                                            :clj nil)
                          :args #?(:cljs prop-types/string
                                            :clj nil)}}
     :did-mount
     (fn [state]
       #?(:cljs
          (citrus/dispatch! :citrus/send-query route-handler q (first (:rum/args state))))
       state)
     :did-remount
     (fn [old state]
       #?(:cljs
          (when (not= (:rum/args old) (:rum/args state))
            (citrus/dispatch! :citrus/send-query route-handler q (first (:rum/args state)))))
       state)}))

(defn form []
  {:wrap-render (fn [render-fn]
                  (fn [state]
                    (render-fn (assoc state ::form-data {}))))})
