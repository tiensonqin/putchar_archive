(ns share.kit.ui
  (:require [rum.core :as rum]
            [share.util :as util]
            [share.dommy :as dommy]
            [appkit.rum :as r]
            [appkit.citrus :as citrus]
            [share.kit.icons :as icons]
            [clojure.string :as str]
            [share.dicts :refer [t]]
            #?(:cljs [goog.object :as gobj])
            #?(:cljs ["react" :as react])
            #?(:cljs ["rc-dialog" :as rc-dialog])
            #?(:cljs ["rc-select" :as rc-select])
            #?(:cljs ["rc-dropdown" :as rc-dropdown])
            #?(:cljs ["react-textarea-autosize" :as textarea])
            ;; cdn
            #?(:cljs ["sortablejs" :as sortable])
            #?(:cljs ["/web/caret_coordinates" :as caret-coordinates])
            [appkit.macros :refer [oget]]
            ))


#?(:cljs
   (def get-caret-coordinates caret-coordinates/getCaretCoordinates))

(rum/defc svg
  [opts html]
  [:div (cond-> {:dangerouslySetInnerHTML {:__html html}}
          opts
          (merge opts))])

(rum/defc icon
  [{:keys [type opts width height color class]
    :or {color "#1a1a1a"
         width 24
         height 24
         class ""}
    :as attrs}]
  (if-let [f (get icons/icons (keyword type))]
    (svg (assoc-in opts [:style :height] height)
      (f {:width width
          :height height
          :fill color
          :class class}))
    (do
      (prn "Icon not found: " {:type type})
      [:div "You haven't provided the needed icon."])))

(rum/defc avatar
  [{:keys [src class style]}]
  [:span.item.round
   {:style (merge style {:display "block"})
    :class (str "ant-avatar"  " " class)}
   [:img {:src src}]])

(rum/defc logo
  [{:keys [src class style]}]
  [:span.item.round {:class class}
   [:img {:src src
          :style style}]])

(defn button
  ([text]
   (button {} text))
  ([{:keys [class style on-click icon-attrs tab-index font-style title]
     :or {tab-index 0}
     :as attrs} text]
   [:a (-> attrs
           (dissoc :icon :icon-attrs)
           (assoc :class (str "btn " class)
                  :tab-index tab-index))

    [:span.row1 {:style {:justify-content "center"
                         :align-items "center"}}
     (if (:icon attrs)
       (icon (merge
              {:type (:icon attrs)}
              icon-attrs)))

     (if (string? text)
       [:span.btn-contents.system-font-stack {:style font-style}
        text]
       text)]]))

#?(:cljs
   (defn new-sortable
     [id opts]
     (when-let [el (dommy/sel1 id)]
       (.create sortable el
                (clj->js opts)))))

(rum/defc svg-loader
  []
  (icon {:type :loading
            :width 50
            :height 50
            :color "#aaa"}))

;; copy from https://atomiks.github.io/30-seconds-of-css
(rum/defc donut
  []
  [:div.donut])

(rum/defc donut-white
  []
  [:div.donut-white])

#?(:cljs
   (do
     (defonce Dialog (r/adapt-class rc-dialog))
     (rum/defc dialog
       < {:did-remount (fn [old-state new-state]
                         (let [visible (:visible (first (:rum/args new-state)))]
                           (citrus/dispatch-sync! :citrus/default-update
                                             :show-modal? visible))
                         new-state)
          :will-unmount (fn [state]
                          (citrus/dispatch! :citrus/default-update
                                            :show-modal? false)
                          state)}
       [opts & children]
       (apply Dialog opts children)))
   :clj
   (rum/defc dialog [& opts]
     [:div]))

#?(:cljs
   (def select (r/adapt-class (gobj/get rc-select "default")))
   :clj
   (rum/defc select [& opts]
     [:div {:style {:min-width 120}}]))

(defn- force-update-input
  [comp opts]
  (assoc (-> opts (dissoc :on-change))
         :on-change (fn [e]
                      (if-let [on-change (:on-change opts)]
                        (do
                          (on-change e)
                          (.forceUpdate comp))
                        (.forceUpdate comp)))))

(rum/defcc textarea-autosize
  [comp opts]
  #?(:clj
     ;; too-lazy
     [:textarea (merge
                 opts
                 {:placeholder "Your thoughts here"
                  :style {:border "none"
                          :font-size 15
                          :background-color "#FFF"
                          :resize "none"
                          :width "100%"
                          :padding 12
                          :height 134}}
                 )]
     :cljs
     (let [autosize (r/adapt-class (gobj/get textarea "default"))]
       (autosize opts))))

(rum/defcc textarea
  "Notice: update should use `dispatch-sync!`"
  [comp opts]
  [:textarea (force-update-input comp opts)])

(rum/defcc input
  [comp opts]
  [:input (force-update-input comp opts)])

#?(:cljs
   (def option (r/adapt-class (gobj/get rc-select "Option")))
   :clj
   (rum/defc option [& opts]
     [:div]))

#?(:cljs (def Dropdown (r/adapt-class rc-dropdown)))
(rum/defc dropdown
  [opts child]
  #?(:clj
     child
     :cljs
     (Dropdown (if (util/mobile?)
                 (assoc opts
                        :trigger ["click"])
                 opts) child)))

;; dropdown
(rum/defc menu
  [element items {:keys [menu-style item-style visible placement]
                  :or {placement "bottomRight"}}]
  (dropdown
   (cond->
     {:placement placement
      :overlay [:ul.menu.shadow
               {:style (merge
                        {:margin-top 12
                         :background "#FFF"
                         :border-radius 4
                         :width 276
                         :font-weight 600
                         :z-index 9}
                        menu-style)}
               (for [item items]
                 [:li.menu-item.row {:key (or (:id item)
                                              (util/random-uuid))
                                     :style (merge
                                             {:justify-content "flex-end"}
                                             item-style)}
                  item])]
      :animation "slide-up"}
     visible
     (assoc :visible visible))
   element))
