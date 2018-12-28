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

(rum/defcs tags < rum/reactive
  (rum/local false ::expand?)
  [state]
  (let [expand? (get state ::expand?)
        tags (citrus/react [:user :current :followed_tags])
        tags (if tags tags
                 (map first (citrus/react [:hot-tags])))
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))
        more? (> (count tags) 12)]
    [:div.column1 {:style {:padding "6px 12px"}}
     [:div.row1 {:style {:margin-bottom 12
                         :color colors/primary
                         :align-items "center"}}
      [:span {:style {:font-size 15}}
       (t :followed-tags)]

      [:a.control {:href "/tags"
                   :style {:margin-left 6
                           :font-size 14}}
       "(" (t :hot) ")"]]

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
                      (util/tag-decode tag)])]
       (if (and more? (not @expand?))
         [:div.row {:style {:flex-wrap "wrap"}}
          (for [item (take 12 tags)]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3
                               :padding-right 3}
                       :on-click #(swap! expand? not)}
           (t :show-all)]]
         [:div.row {:style {:flex-wrap "wrap"}}
          (for [item tags]
            (item-cp item))
          [:a.control {:style {:font-size 14
                               :margin-left 3
                               :padding-right 3}
                       :on-click #(swap! expand? not)}
           (if more? (t :collapse))]]))]))

;; (rum/defc footer < rum/reactive
;;   [padding]
;;   (let [locale (citrus/react :locale)
;;         path (citrus/react [:router :handler])
;;         zh-cn? (= locale :zh-cn)
;;         hot? (= path :hot)
;;         latest? (= path :latest)]
;;     [:div.right-sub.column1 {:style {:font-size 14
;;                                      :padding padding}}


;;      [:div.row1 {:style {:align-items "center"
;;                          :flex-wrap "wrap"
;;                          :margin-top 12}}
;;       (widgets/subscribe "/hot.rss")

;;       [:a {:href "https://github.com/tiensonqin/putchar"
;;            :style {:margin-left 14}}
;;        (ui/icon {:type :github
;;                  :width 18
;;                  :color colors/shadow})]

;;       [:a.control
;;        {:style {:margin-left 12}
;;         :href "mailto:tiensonqin@gmail.com"}
;;        (ui/icon {:type :mail
;;                  :color colors/shadow
;;                  :width 22
;;                  :height 22
;;                  :opts {:style {:margin-top 1}}})]

;;       (ui/dropdown {:overlay (ui/button {:style {:margin-top 6}
;;                                          :on-click (fn [e]
;;                                                      (util/stop e)
;;                                                      (citrus/dispatch! :citrus/set-locale (if zh-cn?
;;                                                                                             :en
;;                                                                                             :zh-cn)))}
;;                                (if zh-cn?
;;                                  "English"
;;                                  "简体中文"))
;;                     :animation "slide-up"}
;;                    [:a {:style {:margin-left 10
;;                                 :margin-top 1}}
;;                     (ui/icon {:type :translate
;;                               :width 20
;;                               :height 20
;;                               :color colors/shadow})])]]))

(rum/defc right
  []
  [:div {:style {:position "fixed"
                 :top 56
                 :width 243}}
   (tags)])
