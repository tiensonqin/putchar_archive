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
            [share.components.widgets :as widgets]))

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

(defn- get-complete-coords
  [cursor-position width]
  #?(:cljs
     (let [textarea (dommy/sel1 "#post-box")
           coordinates (ui/get-caret-coordinates textarea cursor-position)
           top (+ (oget coordinates "top")
                  12)
           left (if (> width 768)
                  (+ 180 (oget coordinates "left"))
                  (oget coordinates "left"))]
       {:top top
        :left left})))

(rum/defc emojis-cp < rum/reactive
  [type id emojis]
  (let [on-select (fn [[keyword unicode-or-src]] (citrus/dispatch! :citrus/add-emoji type id keyword))
        width (citrus/react [:layout :current :width])
        cursor-position (citrus/react [:post-box :cursor-position])
        element [:span {:style
                        (merge {:position "absolute"}
                               (get-complete-coords cursor-position width))}]]
    (widgets/autocomplete
     emojis
     (fn [[keyword unicode-or-src]]
       [:div.row1
        (content/emoji keyword "lg")

        [:span {:style {:margin-left 12
                        :font-weight "500"}}
         keyword]])
     element
     on-select
     {:placement "bottomRight"
      :menu-style {:width 180}
      :item-style {:justify-content "flex-start"}})))

(rum/defc post-box < rum/reactive
  {:after-render (fn [state]
                   #?(:cljs
                      (let [[type id {:keys [value]}] (:rum/args state)]
                        (let [element (dommy/sel1 "#post-box")]
                          (when (and value (str/blank? (oget element "value")))
                            (oset! element "value" value)))))
                   state)
   :will-unmount (fn [state]
                   (citrus/dispatch! :search/reset nil)
                   state)
   }
  [type id {:keys [placeholder style on-change value other-attrs]}]
  (let [{:keys [width height]} (citrus/react [:layout :current])
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

     (when (seq emojis)
       (emojis-cp type id emojis))]))
