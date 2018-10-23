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
                  (citrus/react [:books :latest]))
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))
        more? (> (count books) 7)]
    [:div.column1 {:style {:padding 12
                           :margin-top (if mobile? 24 0)}}
     [:a.row1 {:style {:margin-bottom 12
                       :color colors/primary
                       :font-size 15
                       :align-items "center"}
               :href "/books"}
      (ui/icon {:type :library_books
                ;; :color "#D95653"
                :width 16
                :height 16
                :opts {:style {:margin-right 6}}})

      (t :books)]

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
       (if (and more? (not @expand?))
         [:div.column1
          (for [item (take 7 books)]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3}
                       :on-click #(swap! expand? not)}
           (t :show-all)]]
         [:div.column1
          (for [item books]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3}
                       :on-click #(swap! expand? not)}
           (if more? (t :collapse))]]))]))

(rum/defcs tags < rum/reactive
  (rum/local false ::expand?)
  [state]
  (let [expand? (get state ::expand?)
        tags (citrus/react [:user :current :followed_tags])
        tags (if tags tags
                 (map first (citrus/react [:hot-tags])))
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))
        more? (> (count tags) 12)]
    [:div.column1 {:style {:padding 12}}
     [:div.row1 {:style {:margin-bottom 12
                         :color colors/primary
                         :align-items "center"}}
      [:span {:style {:padding-left 2
                      :margin-right 6
                      :font-weight "bold"
                      :font-size 18}}
       "#"]

      [:a.row1 {:style {:color colors/primary
                        :font-size 15}
                :href "/tags"}
       (t :tags)]]

     (let [item-cp (fn [tag]
                     [:a {:key (str "tag-" tag)
                          :href (str "/tag/" tag)
                          :style {:color colors/primary
                                  :font-size 14
                                  :overflow "hidden"
                                  :margin-left 3
                                  :padding-right 3
                                  :margin-bottom 6
                                  :white-space "nowrap"
                                  :text-overflow "ellipsis"}}
                      tag])]
       (if (and more? (not @expand?))
         [:div.row {:style {:flex-wrap "wrap"
                            :width 243}}
          (for [item (take 12 tags)]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3
                               :padding-right 3}
                       :on-click #(swap! expand? not)}
           (t :show-all)]]
         [:div.row {:style {:flex-wrap "wrap"
                            :width 243}}
          (for [item tags]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3
                               :padding-right 3}
                       :on-click #(swap! expand? not)}
           (if more? (t :collapse))]]))]))

(rum/defc footer < rum/reactive
  []
  (let [locale (citrus/react :locale)
        zh-cn? (= locale :zh-cn)
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))]
    [:div {:style {:font-size 14
                   :padding 12
                   :position "fixed"
                   :bottom 24}}

     [:div.row1 {:style {:align-items "center"
                         :flex-wrap "wrap"}}
      [:a.control
       {:key "about"
        :href "/about"}
       (t :about)]

      [:a {:href "/hot.rss"
           :target "_blank"
           :style {:margin-left 16}}

       (ui/icon {:type :rss
                 :width 22
                 :color colors/shadow})]

      [:a {:href "https://github.com/tiensonqin/putchar"
           :style {:margin-left 16}}

       (ui/icon {:type :github
                 :width 18
                 :color colors/shadow})]

      [:a {:href "mailto:tiensonqin@gmail.com"
           :style {:margin-left 16}}
       (ui/icon {:type :mail_outline
                 :color colors/shadow
                 :width 22
                 :height 22
                 :opts {:style {:margin-top 2}}})]

      (ui/dropdown {:overlay (ui/button {:style {:margin-top 0}
                                         :on-click (fn [e]
                                                     (util/stop e)
                                                     (citrus/dispatch! :citrus/set-locale (if zh-cn?
                                                                                            :en
                                                                                            :zh-cn)))}
                               (if zh-cn?
                                 "English"
                                 "简体中文"))
                    :animation "slide-up"}
                   [:a {:style {:margin-left 15
                                :margin-top 3}}
                    (ui/icon {:type :translate
                              :width 20
                              :height 20
                              :color colors/shadow})])]]))

(rum/defc right
  []
  [:div {:style {:position "fixed"
                 :top 76}}
   (books)
   (tags)
   (footer)])
