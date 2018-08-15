(ns share.components.login
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.config :as config]
            [share.util :as util]
            [share.dicts :refer [t]]
            [clojure.string :as str]))

(defn email-login-fields
  []
  {:email         {:label (t :email)
                   :warning (t :invalid-email)
                   :validators [form/email?]}})

(rum/defc signin
  [mobile? background-color]
  [:div.column.ubuntu {:style {:background background-color
                               :border-radius "4px"
                               :padding 24
                               :justify-content "center"
                               :align-items "center"}}

   (ui/button {:key "github"
               :class "btn-lg"
               :href (str config/website "/auth/github")
               :style {:width 250}}
     [:div.row1 {:style {:align-items "center"}}
      (ui/icon {:type :github
                :color "#24292E"
                :width 18
                :opts {:style {:margin-left -20}}})
      [:span {:class "btn-contents"
              :style {:margin-left 51
                      :font-weight "500"}}
       (t :signin-github)]])

   [:hr.hr-divider.divider-text.divider-text-center {:style {:width 250}
                                                     :data-text "or"}]

   [:div.column
    (form/render
      {:style {:width 250}
       :loading? [:user :loading?]
       :fields (email-login-fields)
       :submit-text (t :signin)
       :confirm-attrs {:style {:width 250}}
       :on-submit (fn [form-data]
                    (citrus/dispatch! :user/request-code @form-data))
       :cancel-button? false})]])

(rum/defc signin-modal < rum/reactive
  [mobile?]
  (let [locale (citrus/react [:locale])
        modal? (citrus/react [:user :signin-modal?])
        width (citrus/react [:layout :current :width])]
    (if modal?
      (ui/dialog
       {:title (t :signin)
        :style (if mobile?
                 {:overflow-y "scroll"
                  :max-width 360
                  :width (- width 24)}
                 {:max-width 360})
        :on-close #(citrus/dispatch! :user/close-signin-modal?)
        :visible modal?
        :wrap-class-name "center"
        :animation "zoom"
        :maskAnimation "fade"}
       (signin mobile? "#FFF")
       ))))
