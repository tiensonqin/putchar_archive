(ns share.components.right
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

(rum/defcs books < rum/reactive
  (rum/local false ::expand?)
  [state]
  (let [expand? (get state ::expand?)
        stared_books (citrus/react [:user :current :stared_books])
        books (if stared_books stared_books
                  (citrus/react [:books :latest]))]
    [:div.shadow.column1 {:style {:padding 12
                                  :margin-bottom 12}}
     [:a.row1 {:style {:margin-bottom 12
                  :color colors/primary}
               :href "/books"}
      "Books"
      (ui/icon {:type :star
                :color "#D95653"
                :width 16
                :height 16})]

     (let [item-cp (fn [{:keys [object_id title]}]
                     [:a {:key (str "book-" object_id)
                          :href (str "/book/" object_id)
                          :style {:color colors/primary
                                  :font-size 14
                                  :overflow "hidden"
                                  :max-width 243
                                  :margin-left 3
                                  :margin-bottom 6
                                  :white-space "nowrap"
                                  :text-overflow "ellipsis"}}
                      title])]
       (if (> (count books) 7)
         [:div.column1
          (for [item (take 7 books)]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3}
                       :on-click #(swap! expand? not)}
           (if @expand?
             (t :collapse)
             (t :show-all))]]
         [:div.column1
          (for [item books]
            (item-cp item))]))]))

(rum/defcs papers < rum/reactive
  (rum/local false ::expand?)
  [state]
  (let [expand? (get state ::expand?)
        stared_papers (citrus/react [:user :current :stared_papers])
        papers (if stared_papers stared_papers
                   (citrus/react [:papers :latest]))]
    [:div.shadow.column1 {:style {:padding 12
                                  :margin-bottom 12}}
     [:a.row1 {:style {:margin-bottom 12
                  :color colors/primary}
               :href "/papers"}
      "Papers"
      (ui/icon {:type :star
                :color "#D95653"
                :width 16
                :height 16})]

     (let [item-cp (fn [{:keys [object_id title]}]
                     [:a {:key (str "paper-" object_id)
                          :href (str "/paper/" object_id)
                          :style {:color colors/primary
                                  :font-size 14
                                  :overflow "hidden"
                                  :max-width 243
                                  :margin-left 3
                                  :margin-bottom 6
                                  :white-space "nowrap"
                                  :text-overflow "ellipsis"}}
                      title])]
       (if (> (count papers) 7)
         [:div.column1
          (for [item (take 7 papers)]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3}
                       :on-click #(swap! expand? not)}
           (if @expand?
             (t :collapse)
             (t :show-all))]]
         [:div.column1
          (for [item papers]
            (item-cp item))]))]))

(rum/defc footer < rum/reactive
  []
  (let [locale (citrus/react :locale)
        zh-cn? (= locale :zh-cn)]
    [:div.ubuntu.right-sub {:class "column1 shadow"
                            :style {:font-size 14
                                    :padding 12}}

     [:div.row1 {:style {:align-items "center"}}
      [:a.control {:href "/newest"
                   :on-click (fn []
                               (citrus/dispatch! :citrus/re-fetch :newest {}))}
       (t :new-created)]
      [:a.control {:href "/moderation-logs"
                   :style {:margin-left 24}}
       "Moderation logs"]]

     [:div.row1 {:style {:align-items "center"
                         :margin-top 16}}
      [:a.control
       {:key "about"
        :href "/about"}
       (t :about)]

      [:a.control
       {:style {:margin-left 24}
        :href "mailto:tiensonqin@gmail.com"}
       (t :contact-us)]]

     [:div.row1 {:style {:align-items "center"
                         :flex-wrap "wrap"
                         :margin-top 16}}
      [:a.control {:href "/tag/feature-requests"
                   :style {:margin-right 24}}
       (t :feature-requests)]]



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
                              :color colors/shadow})])

      [:a {:href "https://twitter.com/putchar"
           :style {:margin-left 24}}
       (ui/icon {:type :twitter
                 :color colors/shadow
                 :width 20
                 :height 20})]

      [:a {:href "https://github.com/tiensonqin/putchar"
           :style {:margin-left 24}}
       (ui/icon {:type :github
                 :width 18
                 :color colors/shadow})]

      [:a {:href "https://discord.gg/4FHR3jh"
           :title "Putchar discord group"
           :style {:margin-left 21}}
       (ui/icon {:type :discord
                 :width 30
                 :color "#7289DA"})]]]))

(rum/defc right
  []
  [:div
   (books)
   (papers)
   (footer)])
