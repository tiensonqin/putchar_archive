(ns share.components.stats
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.kit.colors :as colors]
            [share.util :as util]
            [share.kit.mixins :as mixins]
            [share.kit.query :as query]
            [share.dicts :refer [t] :as dicts]
            [clojure.string :as str]))

(rum/defc item-cp < rum/static
  [mobile? {:keys [post_id post_title stats views reads] :as item}]
  (if mobile?
    [:div.column
     [:h4 post_title]
     [:div.row1
      [:span {:style {:font-weight "bold"
                      :color colors/shadow}}
       views]
      [:span {:style {:margin-left 6}}
       (t :views)]
      [:span {:style {:font-weight "bold"
                      :color colors/shadow
                      :margin-left 12}}
       reads]
      [:span {:style {:margin-left 6}}
       (t :reads)]]]
    [:tr {:key post_id}
     [:td
      [:div
       [:a.control
        [:h4 post_title]]]]
     [:td {:style {:text-align "right"}}
      [:span {:style {:font-weight "bold"
                      :color colors/shadow}}
       views]]
     [:td {:style {:text-align "right"}}
      [:span {:style {:font-weight "bold"
                      :color colors/shadow}}
       reads]]]))

(rum/defc stats < rum/reactive
  (mixins/query :stats)
  [params]
  (let [mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))]
    [:div.column.auto-padding.center-area#stats {:style {:width "100%"
                                                         :align-items "center"}}
     [:h1 (t :stats)]
     (query/query
       (let [stats (citrus/react [:stats])]
         (if (seq stats)
          [:div.center {:style {:width "100%"}}
           (let [all-views (apply + (map :views stats))
                 all-reads (apply + (map :reads stats))]
             [:h5 {:style {:margin-top 48
                           :margin-bottom 12
                           :color colors/shadow}}
              (str (str/capitalize (t :views)) " ")
              [:span {:style {:margin-left 6}}
               all-views]
              [:span {:style {:margin-left 24}}
               (str (str/capitalize (t :reads)) " ")]
              [:span {:style {:margin-left 6}}
               all-reads]])

           [:div.divider]

           ;; header
           [:table {:style {:margin-top 12
                            :margin-bottom 128
                            :width "100%"}}
            (when-not mobile?
              [:thead
               [:tr
                [:th {:style {:text-align "left"}} (t :posts)]
                [:th {:style {:text-align "right"}} (str/capitalize (t :views))]
                [:th {:style {:text-align "right"}} (str/capitalize (t :reads))]]])
            [:tbody
             (for [item stats]
               (rum/with-key (item-cp mobile? item)
                 (:id (:post_id item))))]]]
          [:h2.ubuntu (t :no-stats-yet)])))]))
