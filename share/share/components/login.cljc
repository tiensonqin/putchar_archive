(ns share.components.login
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.widgets :as widgets]
            [share.config :as config]
            [share.util :as util]
            [share.dicts :refer [t]]
            [share.kit.colors :as colors]
            [clojure.string :as str]))

(defn email-login-fields
  []
  {:email         {:warning (t :invalid-email)
                   :placeholder "Email"
                   :validators [form/email?]}})

(rum/defc signin
  [background-color]
  [:div.column.ubuntu {:style (cond->
                                {:border-radius "4px"
                                 :padding 24
                                :justify-content "center"
                                 :align-items "center"}
                                background-color
                                (assoc :background background-color))}

   (ui/button {:key "github"
               :class "btn-lg btn-primary"
               :href (str config/website "/auth/github")
               :style {:width 250}}
     [:div.row1 {:style {:align-items "center"}}
      (ui/icon {:type :github
                :color "#FFF"
                :width 18
                :opts {:style {:margin-left -20}}})
      [:span {:class "btn-contents"
              :style {:margin-left 51
                      :font-weight "500"}}
       (t :signin-github)]])

   [:hr.hr-divider.divider-text.divider-text-center {:style {:width 250}
                                                     :data-text "or"}]

   [:div.column1 {:style {:margin-top 10}}
    (form/render
      {:style {:width 250}
       :loading? [:user :loading?]
       :fields (email-login-fields)
       :submit-text (t :signin-with-email)
       :confirm-attrs {:class "btn"
                       :style {:width 250
                               :background "#FFF"}}
       :on-submit (fn [form-data]
                    (citrus/dispatch! :user/request-code @form-data))
       :cancel-button? false})]

   (widgets/transform-content (t :agree-text)
                              {:style {:font-size 14
                                       :margin-top 24
                                       :color colors/shadow
                                       :width 250}})])

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
       (signin mobile?)))))
