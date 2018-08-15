(ns share.components.layout
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.kit.colors :as colors]
            [share.components.group :as group]
            [share.components.login :as login]
            [share.components.post :as post]
            [share.components.widgets :as widgets]
            [share.kit.mixins :as mixins]
            [appkit.citrus :as citrus]
            [share.dicts :refer [t]]
            [share.util :as util]
            [share.config :as config]
            [clojure.string :as str]))

(rum/defc right-footer < rum/reactive
  []
  (let [locale (citrus/react :locale)
        zh-cn? (= locale :zh-cn)]
    [:div.ubuntu.right-footer.right-sub {:class "column1"
                                         :style {:padding 12
                                                 :font-size 14}}

     [:div.row1 {:style {:align-items "center"}}
      [:a.control
       {:key "about"
        :href "/about"}
       (t :about)]

      [:a.control
       {:style {:margin-left 24}
        :href "mailto:tiensonqin@gmail.com"}
       (t :contact-us)]

      (ui/dropdown {:overlay (ui/button {:style {:margin-top 6}
                                         :on-click (fn [e]
                                                     (util/stop e)
                                                     (citrus/dispatch! :citrus/set-locale (if zh-cn?
                                                                                            :en
                                                                                            :zh-cn)))}
                               (if zh-cn?
                                 "English"
                                 "简体中文"))
                    :animation "slide-up"}
                   [:a.no-decoration {:key "language"
                                      :style {:margin-left 24}}
                    [:div.row1
                     (ui/icon {:type :translate
                               :width 18
                               :height 18
                               :color "#666"})]]
                   )]

     [:div.row1 {:style {:align-items "center"
                         :flex-wrap "wrap"
                         :margin-top 16}}
      [:a.control {:href "/privacy"
                   :style {:margin-right 24}
           :on-click (fn []
                       (util/set-href! (str config/website "/privacy")))}
       (t :privacy)]
      [:a.control {:href "/terms"
                   :style {:margin-right 24}
                   :on-click (fn []
                       (util/set-href! (str config/website "/terms")))}
       (t :terms)]
      [:a.control {:href "/code-of-conduct"
                   :on-click (fn []
                       (util/set-href! (str config/website "/code-of-conduct")))}
       (t :code-of-conduct)]]

     [:div.row1 {:style {:align-items "center"
                         :flex-wrap "wrap"
                         :margin-top 16}}
      [:a {:href "https://twitter.com/lambdahackers"
           :style {:margin-right 24}}
       (ui/icon {:type :twitter})]

      [:a {:href "https://github.com/tiensonqin/lambdahackers"
           :style {:margin-right 24}}
       (ui/icon {:type :github
                 :width 18})]]]))
