(ns share.components.notifications
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.kit.query :as query]
            [share.kit.mixins :as mixins]
            [share.components.widgets :as w]
            [appkit.citrus :as citrus]
            [share.dicts :refer [t] :as dicts]
            [share.util :as util]
            [clojure.string :as str]))

(rum/defc post-or-comment-deleted < rum/reactive
  [{:keys [post item comment reason created_at]}]
  (let [current-user (citrus/react [:user :current])
        reasons (dicts/reasons)]
    [:div.column {:key (util/random-uuid)}
     [:div.space-between
      [:h4
       (str (t :your) (str/lower-case
                       (if comment (t :comment) (t :post)))
            (t :as-below-deleted))]
      [:div {:style {:align-self "flex-start"
                     :color "#999"
                     :font-size 12}}
       (util/time-ago created_at)]]

     [:div.space-between {:style {:align-items "center"}}
      (if post
        [:div.row {:style {:margin "12px 0"}}
         [:span {:style {:font-weight "600"
                         :width 100}}
          (str (t :post) ":")]
         [:a {:key "post"
              :style {:color "rgba(0,0,0,0.84)"}
              :href (str "/" (:permalink post))}
          (:title post)]])]

     (if comment
       [:div.row {:key "comment"
                  :style {:margin "12px 0"}}
        [:span {:style {:font-weight "600"
                        :width 100}}
         (str (t :comment) ":")]
        (w/transform-content (:body comment) nil)])

     [:div.row
      [:span {:style {:font-weight "600"
                      :width 100}}
       (str (t :reason) ":")]
      (nth reasons reason)]]))

(rum/defc group-blocked < rum/reactive
  [{:keys [group action]}]
  [:div.row
   (case action
     :3d
     [:div (str (t :disable-account-notification)
                (util/original-name (:name group)) ".")]
     [:div (str (t :block-notification) (util/original-name (:name group)) ".")])])

(rum/defc group-admin-promote
  [notification]
  (let [{:keys [who group created_at]} notification]
    [:div {:key (util/random-uuid)}
     [:div.row {:style {:margin-top 12
                        :align-items "center"}}
      [:div.row
       [:a {:href (str "/@" (:screen_name who))}
        (ui/avatar {:shape "circle"
                    :src (util/cdn-image (:screen_name who))})]
       [:span {:style {:margin "0 6px"}}
        (t :promote-notification)]

       [:a {:href (str "/" (:name group))}
        (ui/avatar {:shape "circle"
                    :src (util/group-logo (:name group))})]]

      [:div {:style {:align-self "flex-start"
                     :color "#999"
                     :font-size 12}}
       (util/time-ago created_at)]]]))

(rum/defc comment-c < rum/reactive
  [notification]
  (let [current-user (citrus/react [:user :current])
        {:keys [post comment my-comment]} notification]
    [:div {:key (:id comment)}
     [:div.column
      [:div.row1
       [:span (t :new-comment)]

       [:a {:key "post"
            :style {:color "rgba(0,0,0,0.84)"
                    :margin-left 12}
            :href (str "/" (str (:permalink post) "/" (:idx comment)))}
        (:title post)]]


      [:div.row {:style {:margin-top 12}}
       (let [screen-name (get-in comment [:user :screen_name])]
         (ui/avatar {:shape "circle"
                     :src (util/cdn-image screen-name)}))

       [:div.column {:style {:margin-left 12}}
        [:div.row1 {:style {:justify-content "space-between"
                            :color "#999"
                            :font-size 13}}
         [:div {:style {:margin-top -3}}
          (str "@" (get-in comment [:user :screen_name]))]

         (util/time-ago (:created_at comment))]

        (w/transform-content (:body comment) {})]]]]))


(rum/defc notifications < rum/reactive
  (mixins/query :notifications)
  [params]
  [:div.column.auto-padding
   (let [notifications (citrus/react [:notifications])]
     (query/query
       (if (seq notifications)
         [:div.column
          ;; mark
          (ui/button {:class "btn-primary"
                      :on-click (fn []
                                  (citrus/dispatch! :citrus/clear-notifications))
                      :style {:margin "12px 0 0 0"}}
            (t :dismiss-all))

          [:div.divider]

          (for [{:keys [type] :as notification} notifications]
            [:div.col-item
             {:key (util/random-uuid)}
             (case type
               :new-comment
               (comment-c notification)

               :reply-comment
               (comment-c notification)

               :group-admin-promote
               (group-admin-promote notification)

               :post-or-comment-deleted
               (post-or-comment-deleted notification)

               :group-blocked
               (group-blocked notification))])]
         [:div.center
          [:h2 {:style {:font-weight 500}}
           (t :no-more-notifications)]])))])
