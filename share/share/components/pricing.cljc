(ns share.components.pricing
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.kit.query :as query]
            [clojure.string :as str]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.channel :as channel]
            [share.components.post :as post]
            [share.components.widgets :as w]
            [share.kit.mixins :as mixins]
            [share.helpers.image :as image]
            [share.util :as util]
            [bidi.bidi :as bidi]
            [share.dicts :refer [t]]
            [share.config :as config]
            [share.kit.colors :as colors]
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [appkit.macros :refer [oget]])))

(rum/defc pricing < rum/reactive
  [params]
  (let [{:keys [width height]} (citrus/react [:layout :current])
        mobile? (or (util/mobile?) (<= width 768))
        user (citrus/react [:user :current])]
    [:div#pricing {:class (if mobile? "column" "row")}
     ;; PRO
     [:div.column.shadow {:style {:background "#FFF"
                                  :padding 24
                                  :max-height 600}}
      [:h1 (t :groups)]

      [:hr.gradient]

      [:div.editor {:style {:height 336
                            :margin-bottom 24}}
       [:h2 (t :free-features)]
       [:ol.show-style
        [:li (t :public-groups)]
        [:li {:style {:margin-top 12}}
         (t :unlimited-members)]
        [:li {:style {:margin-top 12}}
         (t :up-to-10-moderators)]
        [:li {:style {:margin-top 12}}
         (t :unlimited-channels)]
        [:li {:style {:margin-top 12}}
         (t :search-indexed)]]

       [:div.divider]

       [:h2 (t :private-group)]
       [:p
        (t :each-costs)
        [:span {:style {:background colors/primary
                        :color "#FFF"
                        :border-radius 6
                        :padding "0 6px"
                        :font-size "15px"}}
         (t :ten-per-month)]
        (t :thats-all)]]
      (if user
        (ui/button {:class "btn-primary"
                    :href "/new-group"}
          (t :create-your-group))
        (ui/button {:class "btn-primary"
                    :on-click #(citrus/dispatch! :user/show-signin-modal?)}
          (t :login-to-create-your-group)))]
     [:div.column.shadow {:style {:background "#FFF"
                                  :padding 24
                                  :max-height 600
                                  :margin-left (if mobile? 0 24)
                                  :margin-top (if mobile? 24 0)}}
      (w/pro user)
]]))
