(ns share.components.tags
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.components.post :as post]
            [share.kit.mixins :as mixins]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.kit.colors :as colors]
            [share.dicts :refer [t]]))

(rum/defc tags < rum/reactive
  [params]
  [:div.column.auto-padding
   [:h1 (t :hot-tags)]

   (let [tags (citrus/react [:hot-tags])]
     (for [[tag count] tags]
       [:div.row {:style {:margin-left 3}}
        [:a {:key tag
             :href (str "/tag/" tag)
             :style {:color colors/primary}}
         tag]
        [:span {:style {:margin-left 12}}
         count]]))])
