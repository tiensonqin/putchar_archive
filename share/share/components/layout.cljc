(ns share.components.layout
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.components.login :as login]
            [share.components.post :as post]
            [share.components.widgets :as widgets]
            [share.kit.mixins :as mixins]
            [appkit.citrus :as citrus]
            [share.dicts :refer [t]]
            [share.util :as util]
            [share.config :as config]
            [share.kit.colors :as colors]
            [clojure.string :as str]))

(rum/defc theme < rum/reactive
  []
  (let [theme (citrus/react [:theme])
        black? (= theme "black")]
    [:a.control {:style {:margin-top 16
                         :display "block"}
                 :on-click (fn [e]
                             (util/stop e)
                             (citrus/dispatch! :citrus/set-theme (if black?
                                                                   "white"
                                                                   "black")))}
     (str (t :switch-to)
          (if black?
            (t :light-theme)
            (t :dark-theme)))]))

(rum/defc right-footer < rum/reactive
  []
  (let [locale (citrus/react :locale)
        zh-cn? (= locale :zh-cn)]
    [:div.ubuntu.shadow.right-sub {:class "column1"
                                   :style {:font-size 14}}

     [:div.row1 {:style {:align-items "center"}}
      [:a.control {:href "/newest"
                   :on-click (fn []
                               (citrus/dispatch! :citrus/re-fetch :newest {}))}
       (t :new-created)]
      [:a.control {:href "/moderation-logs"
                   :style {:margin-left 24}}
       "Moderation logs"]]

     (theme)

     [:div.row1 {:style {:align-items "center"
                         :margin-top 16}}
      [:a.control
       {:key "about"
        :href "/about"}
       (t :about)]

      [:a.control
       {:style {:margin-left 24}
        :href "mailto:tiensonqin@gmail.com"}
       (t :contact-us)]

      [:a.control {:href "/privacy"
                   :style {:margin-left 24}
                   :on-click (fn []
                               (util/set-href! (str config/website "/privacy")))}
       (t :privacy)]]

     [:div.row1 {:style {:align-items "center"
                         :flex-wrap "wrap"
                         :margin-top 16}}
      [:a.control {:href "/tag/feature-requests"
                   :style {:margin-right 24}}
       (t :feature-requests)]

      [:a.control {:href "https://github.com/tiensonqin/putchar/issues"
                   :style {:margin-right 24}}
       (t :bugs)]]



     [:div.row1 {:style {:align-items "center"
                         :flex-wrap "wrap"
                         :margin-top 16}}
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
                   [:a
                    (ui/icon {:type :translate
                              :width 20
                              :height 20
                              :color (colors/shadow)})])

      [:a {:href "https://twitter.com/putchar"
           :style {:margin-left 24}}
       (ui/icon {:type :twitter
                 :color (colors/shadow)
                 :width 20
                 :height 20})]

      [:a {:href "https://github.com/tiensonqin/putchar"
           :style {:margin-left 24}}
       (ui/icon {:type :github
                 :width 18
                 :color (colors/shadow)})]

      [:a {:href "https://discord.gg/4FHR3jh"
           :title "Putchar discord group"
           :style {:margin-left 21}}
       (ui/icon {:type :discord
                 :width 30
                 :color "#7289DA"})]]]))
