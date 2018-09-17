(ns share.components.home
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.components.post :as post]
            [share.kit.mixins :as mixins]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.kit.colors :as colors]
            [share.dicts :refer [t]]))

(rum/defc home < rum/reactive
  (mixins/query :home)
  [params]
  [:div.column.home {:style {:padding-bottom 48}}
   [:h6.auto-padding {:style {:margin-top 0
                              :color (colors/shadow)
                              :margin-bottom 24}}
    (t :slogan)]
   (let [posts (citrus/react [:posts :hot])]
     (query/query
       (post/post-list posts
                       {:merge-path [:posts :hot]})))])
