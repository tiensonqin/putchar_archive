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
       [:a.no-decoration {:key tag
                          :href (str "/tag/" tag)
                          :style {:color colors/primary
                                  :font-size "1.2em"
                                  :margin-bottom 12
                                  :margin-left 3}}
        (util/format "%s (%d posts)" (util/tag-decode tag) count)]))])
