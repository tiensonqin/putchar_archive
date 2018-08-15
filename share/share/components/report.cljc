(ns share.components.report
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.widgets :as w]
            [share.util :as util]
            [share.kit.mixins :as mixins]
            [share.dicts :refer [t] :as dicts]))

(rum/defcs report < rum/reactive
  (rum/local nil ::form-data)
  [state]
  (let [form-data (get state ::form-data)
        {:keys [modal? type id]} (citrus/react [:report])]
    [:div#report
     (if modal?
       (ui/dialog
        {:title "Report"
         :on-close #(citrus/dispatch! :citrus/default-update [:report] nil)
         :visible modal?
         :wrap-class-name "center"
         :style {:width (min 400 (- (:width (util/get-layout)) 48))}
         :animation "zoom"
         :maskAnimation "fade"}
        (let [reasons (dicts/reasons)
              on-click (fn [idx]
                         (fn []
                           (citrus/dispatch! :citrus/default-update [:report] nil)

                           (let [data {:kind idx
                                       :object_type type
                                       :object_id id}]
                             (citrus/dispatch! :report/new data))))]
          [:div.column {:style {:background "#FFF"
                                :justify-content "center"
                                :align-items "center"}}
           (ui/button {:style {:width 300}
                       :on-click (on-click 0)}
             (nth reasons 0))

           (ui/button {:style {:margin-top 24
                               :width 300}
                       :on-click (on-click 1)}
             (nth reasons 1))

           ;; (ui/button {:style {:margin-top 24
           ;;                     :width 300}
           ;;             :on-click (on-click 2)}
           ;;   "It breaks group rules")

           (ui/button {:style {:margin-top 24
                               :margin-bottom 64
                               :width 300}
                       :on-click (on-click 3)}
             (nth reasons 3))])))]))

;; 1. delete post or comment
;; 2. disable user from post for 3 days
;; 3. block user, remove user from group
;; 3 blocks will delete this user.
(rum/defcs reports < rum/reactive
  (mixins/query :reports)
  [state params]
  [:div.column.auto-padding
   (let [reports (citrus/react [:reports])
         user-dialog? (citrus/react [:report :next-user-dialog?])
         delete-dialog? (citrus/react [:report :delete-dialog?])
         reasons (dicts/reasons)]
     (if (seq reports)
       (for [{:keys [id object_type data kind created_at] :as report} reports]
         (let [{:keys [group post comment user]} (util/keywordize data)]
           [:div.col-item {:key id}
            [:div.row {:style {:align-items "center"
                               :justify-content "space-between"}}
             [:div.row {:style {:align-items "center"}}
              ;; group
              [:a {:title (str "Group: " (util/original-name (:name group)))
                   :href (str "/" (:name group))}
               (ui/avatar {:shape "circle"
                           :src (util/group-logo (:name group))})]

              ;; post
              [:a {:key "post"
                   :style {:margin-left 12
                           :color "rgba(0,0,0,0.84)"}
                   :href (str "/" (:permalink post) "/" (:idx comment))}
               (:title post)]]

             [:span {:key "time"
                     :style {:color "#999"}}
              (util/time-ago created_at)]]

            (if comment
              [:div.row {:key "comment"
                         :style {:margin "24px 0"}}
               [:span {:style {:font-weight "600"
                               :width 100}}
                "Comment:"]
               (w/transform-content (:body comment) nil)])

            [:div.row {:key "reason"
                       :style {:margin "24px 0"}}
             [:div {:style {:font-weight "600"
                            :width 100}}
              "Reason: "] (nth reasons kind)]

            [:div.row {:key "user"
                       :style {:align-items "center"}}
             [:div {:style {:font-weight "600"
                            :width 100}}
              "User: "]
             [:a {:href (str "/@" (:screen_name user))}
              [:div {:style {:display "flex"
                             :flex-direction "row"
                             :align-items "flex-start"}}
               (ui/avatar {:class "ant-avatar-sm"
                           :shape "circle"
                           :src (util/cdn-image (:screen_name user))})
               [:span {:style {:color "rgba(0,0,0,0.84)"
                               :margin-left 6
                               :font-size 13}}
                (str "@" (:screen_name user))]]]]

            [:div.space-between {:key "actions"
                                 :style {:margin-top 24}}
             [:a {:class "is-danger"
                  :on-click (fn []
                              (citrus/dispatch! :report/open-delete-dialog?))}
              (str "Delete this " object_type)]

             [:a {:on-click (fn []
                              (citrus/dispatch! :citrus/report-ignore report))}
              "Ignore it"]]

            [:div.divider]

            ;; delete dialog
            (ui/dialog
             {:title (t :report-delete-confirmation)
              :on-close #(citrus/dispatch! :report/close-delete-dialog?)
              :visible delete-dialog?
              :wrap-class-name "center"
              :style {:width (min 600 (- (:width (util/get-layout)) 48))}
              :footer (ui/button
                        {:class "btn-danger"
                         :on-click (fn []
                                     (citrus/dispatch! :citrus/report-delete report))}
                        (t :delete))}
             [:div {:style {:background "#FFF"}}
              (if (= object_type "post")
                [:a {:style {:margin-left 12
                             :color "rgba(0,0,0,0.84)"}
                     :href (str "/" (:permalink post))}
                 (:title post)]

                (w/transform-content (:body comment) nil))])

            ;; user dialog
            (ui/dialog
             {:title "User action"
              :on-close #(citrus/dispatch! :report/close-user-dialog?)
              :visible user-dialog?
              :wrap-class-name "center"
              :style {:width (min 600 (- (:width (util/get-layout)) 48))}
              :animation "zoom"
              :maskAnimation "fade"}
             [:div.column {:style {:background "#FFF"
                                   :justify-content "center"
                                   :align-items "center"}}
              (ui/avatar {:class "ant-avatar-lg"
                          :shape "circle"
                          :src (util/cdn-image (:screen_name user))})
              (ui/button {:style {:width 300
                                  :margin-top 24}
                          :on-click (fn []
                                      (citrus/dispatch! :report/user-action {:report report
                                                                             :action :3d}))}
                "Disable this user for 3 days")

              (ui/button {:style {:width 300
                                  :margin "24px 0"}
                          :on-click (fn []
                                      (citrus/dispatch! :report/user-action {:report report
                                                                             :action :forever}))}
                "Block this user from current group")])]))
       [:h2.ubuntu (t :no-more-reports)]))])
