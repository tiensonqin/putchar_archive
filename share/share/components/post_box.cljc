(ns share.components.post-box
  (:require [rum.core :as rum]
            [appkit.citrus :as citrus]
            [share.content :as content]
            [clojure.string :as str]
            #?(:cljs [goog.object :as gobj])
            #?(:cljs [goog.dom :as gdom])
            [share.helpers.image :as image]
            [share.kit.ui :as ui]
            [share.kit.mixins :as mixins]
            [share.dommy :as dommy]
            [share.util :as util]
            [share.dicts :refer [t] :as dicts]
            [appkit.macros :refer [oget oset!]]
            #?(:cljs [cljs-drag-n-drop.core :as dnd])))

;; don't support image uploading on comment
(defn upload-images
  [files]
  #?(:cljs
     (image/upload
      files
      (fn [file file-form-data]
        (let [temp-id (long (str (.getTime (util/get-date)) (rand-int 100)))]
          (citrus/dispatch!
           :citrus/default-update
           [:post :form-data :images temp-id :processing?]
           true)

          (citrus/dispatch!
           :image/upload
           file-form-data
           (fn [url]
             (citrus/dispatch! :citrus/add-picture
                               temp-id
                               {:url url
                                :file file}))))))))

(defonce current-idx (atom nil))
(defonce tab-pressed? (atom false))
(defonce enter-pressed? (atom false))

(defn attach-listeners
  [state]
  #?(:cljs
     (do
       (mixins/listen state js/window :keydown
                      (fn [e]
                        (let [code (.-keyCode e)]
                          ;; 38 up, 40 down
                          (when (contains? #{9 38 40 13} code)
                            (util/stop e)
                            (case code
                              9         ; confirmation
                              (reset! tab-pressed? true)
                              13
                              (reset! enter-pressed? true)
                              38
                              (if (>= @current-idx 1)
                                (swap! current-idx dec))
                              40
                              (if (nil? @current-idx)
                                (reset! current-idx 1)
                                (swap! current-idx inc))))))))))

;; tab or enter for confirmation, up/down to navigate
(rum/defc autocomplete-cp < rum/reactive
  (mixins/event-mixin attach-listeners)
  (mixins/disable-others-tabindex "a:not(.complete-item)")
  {:will-mount (fn [state]
                 (reset! current-idx 0)
                 (reset! tab-pressed? false)
                 (reset! enter-pressed? false)
                 state)
   :will-unmount (fn [state]
                   (reset! current-idx 0)
                   (reset! tab-pressed? false)
                   (reset! enter-pressed? false)
                   state)}
  [col item-cp on-select]
  #?(:cljs
     (let [width (citrus/react [:layout :current :width])
           tab-pressed? (rum/react tab-pressed?)
           enter-pressed? (rum/react enter-pressed?)
           current-idx (or (rum/react current-idx) 0)
           cursor-position (citrus/react [:post-box :cursor-position])]
       (when (seq col)
         (when tab-pressed?
           (on-select (first col)))
         (when enter-pressed?
           (on-select (nth col current-idx)))
         (let [c-idx (if current-idx (min current-idx (dec (count col))))
               textarea (dommy/sel1 "#post-box")
               coordinates (ui/get-caret-coordinates textarea cursor-position)
               top (+ (oget coordinates "top")
                     12)
               left (if (> width 768)
                      (+ 180 (oget coordinates "left"))
                      (oget coordinates "left"))]
          (ui/menu [:span {:style
                           {:position "absolute"
                            :top top
                            :left left}}]
            (for [[idx item] (util/indexed col)]
              (item-cp item (= idx c-idx)))
            {:visible true
             :placement "bottomRight"
             :menu-style {:width 180}}))))))

(rum/defc mentions-cp < rum/reactive
  [type id mentions]
  (let [on-select (fn [item] (citrus/dispatch! :citrus/add-mention type id item))]
    (autocomplete-cp mentions
                    (fn [screen-name focus?]
                      (let [add-mention-fn (fn []
                                             (citrus/dispatch! :citrus/add-mention type id screen-name))]
                        [:a.button-text.row1.complete-item {:key (str "mention-" screen-name)
                                                            :tab-index 0
                                                            :style {:padding 12}
                                                            :class (if focus? "active" "")
                                                            :on-click add-mention-fn
                                                            :on-key-down (fn [e]
                                                                           (when (= 13 (.-keyCode e))
                                                                             (add-mention-fn)))}
                         (ui/avatar {:class "ant-avatar-sm"
                                     :src (util/cdn-image screen-name)})
                         [:span {:style {:margin-left 12
                                         :font-weight "500"}}
                          screen-name]]))
                    on-select)))

(rum/defc emojis-cp < rum/static
  [type id emojis]
  (let [on-select (fn [[keyword unicode-or-src]] (citrus/dispatch! :citrus/add-emoji type id keyword))]
    (autocomplete-cp emojis
                     (fn [[keyword unicode-or-src] focus?]
                      (let [add-emoji-fn (fn [] (on-select [keyword unicode-or-src]))]
                        [:a.button-text.row1.complete-item {:key (str "emoji-" keyword)
                                                            :tab-index 0
                                                            :style {:padding 12}
                                                            :class (if focus? "active" "")
                                                            :on-click add-emoji-fn
                                                            :on-key-down (fn [e]
                                                                           (when (= 13 (.-keyCode e))
                                                                             (add-emoji-fn)))}
                         (content/emoji keyword "lg")

                         [:span {:style {:margin-left 12
                                         :font-weight "500"}}
                          keyword]]))
                    on-select)))

(rum/defc post-box < rum/reactive
  {:will-mount (fn [state]
                 (citrus/dispatch! :search/reset nil)
                 state)
   :after-render (fn [state]
                   #?(:cljs
                      (let [[type id {:keys [value]}] (:rum/args state)]
                        (when (= type :post)
                          (if (dommy/sel1 "#post-box")
                           (dnd/subscribe!
                            (dommy/sel1 "#post-box")
                            :upload-images
                            {:drop (fn [e files]
                                     (upload-images files))})))

                        (let [element (dommy/sel1 "#post-box")]
                          (when (and value (str/blank? (oget element "value")))
                            (oset! element "value" value))
                          )
                        ))
                   state)
   :will-unmount (fn [state]
                   #?(:cljs
                      (let [[type id _] (:rum/args state)]
                        (when (= type :post)
                          (if (dommy/sel1 "#post-box")
                            (dnd/unsubscribe!
                             (dommy/sel1 "#post-box")
                             :upload-images)))))
                   (citrus/dispatch! :search/reset nil)
                   state)
   }
  [type id {:keys [placeholder style on-change value other-attrs]}]
  (let [{:keys [width height]} (citrus/react [:layout :current])
        mentions (citrus/react [:search :result])
        emojis (citrus/react [:search :emojis-result])]
    [:div {:key "post-box"
           :style {:display "flex"
                   :flex-direction "row"
                   :flex 1
                   :position "relative"}}

    (ui/textarea
     (merge
      {:id "post-box"
       :style style
       :placeholder placeholder
       :on-key-down (fn [e]
                      #?(:cljs
                         (let [code (oget e "keyCode")]
                           (when (contains? #{37 38 39 40} code)
                             (citrus/dispatch! :post-box/set-cursor-position
                                               (oget (oget e "target") "selectionEnd"))))))
       :on-click (fn [e]
                   #?(:cljs
                      (citrus/dispatch! :post-box/set-cursor-position
                                        (oget (oget e "target") "selectionEnd"))))

       :on-change (fn [e]
                    #?(:cljs
                       (let [textarea (.-target e)]
                         (citrus/dispatch-sync! :post-box/set-cursor-position
                                                (oget textarea "selectionEnd"))
                         (when (< (.-clientHeight textarea) (.-scrollHeight textarea))
                           (let [new-height (+ (.-scrollHeight textarea) (if (= type :post)
                                                                           400
                                                                           28))]
                             (citrus/dispatch! :citrus/default-update
                                               [:post :latest-height]
                                               new-height)
                             (dommy/set-px! textarea :height new-height)))))
                    (on-change e))

       :value value}
      other-attrs))

     (when (seq mentions)
       (mentions-cp type id mentions))

     (when (seq emojis)
       (emojis-cp type id emojis))]))
