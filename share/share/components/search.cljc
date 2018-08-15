(ns share.components.search
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.components.post :as post]
            [share.components.group :as group]
            [share.components.channel :as channel]
            [share.dicts :refer [t] :as dicts]))

(rum/defc search < rum/reactive
  []
  (let [{:keys [loading? result q]} (citrus/react [:search])
        current-path (citrus/react [:router :handler])]
    (cond
      (or (nil? q) (and (string? q) (<= (count q) 1)))
      [:div.column]

      :else
      [:div.column.auto-padding
       [:h1 {:style {:font-size 36
                     :margin-top 12
                     :margin-bottom 24}}
        (t :search-result)]
       (if loading?
         [:div.row {:style {:justify-content "center"}}
          (ui/donut)]
         (post/post-list {:result result
                          :end? true}
                         {}
                         :empty-widget [:div (t :empty-search-result)]
                         :show-group? true))])
    ))
