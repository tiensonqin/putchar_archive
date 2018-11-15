(ns share.components.tags
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.components.widgets :as widgets]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.kit.colors :as colors]
            [share.dicts :refer [t]]))

(rum/defc tags < rum/reactive
  [params]
  (let [current-user (citrus/react [:user :current])]
    [:div.column.auto-padding
     [:h1 (t :hot-tags)]

     [:div.divider]

     (let [tags (citrus/react [:hot-tags])]
       (for [[tag count] tags]
         (let [followed? (and current-user
                              (contains? (set (:followed_tags current-user))
                                         tag))]
           [:div.space-between.col-item {:style {:align-items "center"}}
            [:a.no-decoration {:key tag
                               :href (str "/tag/" tag)
                               :style {:color colors/primary
                                       :margin-bottom 12
                                       :margin-left 3}}
             (util/format "%s (%d posts)" (util/tag-decode tag) count)]
            (widgets/follow-tag followed? tag)])))]))
