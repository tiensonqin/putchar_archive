(ns share.components.home
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.components.post :as post]
            [share.components.widgets :as widgets]
            [share.kit.mixins :as mixins]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.kit.colors :as colors]
            [share.dicts :refer [t]]))

(rum/defc home < rum/reactive
  (mixins/query :home)
  [params]
  (let [current-user (citrus/react [:user :current])
        post-filter (if current-user :feed :hot)]
    [:div.column.home
     (widgets/posts-header)
     (let [posts (citrus/react [:posts post-filter])]
      (query/query
        (post/post-list posts
                        {:merge-path [:posts post-filter]})))]))
