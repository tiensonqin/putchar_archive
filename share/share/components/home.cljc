(ns share.components.home
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.components.post :as post]
            [share.components.widgets :as widgets]
            [share.kit.mixins :as mixins]
            [appkit.citrus :as citrus]))

(rum/defc home < rum/reactive
  (mixins/query :home)
  [params]
  [:div.column {:style {:padding-bottom 48}}
   (let [posts (citrus/react [:posts :hot])]
     (query/query
       (post/post-list posts
                       {:merge-path [:posts :hot]})))])
