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
  [:div.column.home
   (let [posts (citrus/react [:posts :hot])]
     (query/query
       (post/post-list posts
                       {:merge-path [:posts :hot]})))])
