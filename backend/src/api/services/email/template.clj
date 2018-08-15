(ns api.services.email.template
  (:require [hiccup.page :as page]
            [hiccup.core :refer [html]]
            [api.config :refer [config]]))

(defn head
  [title]
  [:head
   [:meta
    {:name "viewport",
     :content "width=device-width, initial-scale=1.0"}]
   [:meta
    {:http-equiv "Content-Type", :content "text/html; charset=UTF-8"}]
   [:title title]])

(defn body
  [{:keys [title body action-link action-text]}]
  [:body
   {:style
    "-webkit-text-size-adjust: none; box-sizing: border-box; color: #74787E; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; height: 100%; line-height: 1.4; margin: 0; width: 100% !important;",
    :bgcolor "#F2F4F6"}
   [:style
    {:type "text/css"}
    "\nbody {\nwidth: 100% !important; height: 100%; margin: 0; line-height: 1.4; background-color: #F2F4F6; color: #74787E; -webkit-text-size-adjust: none;\n}\n@media only screen and (max-width: 600px) {\n  .email-body_inner {\n    width: 100% !important;\n  }\n  .email-footer {\n    width: 100% !important;\n  }\n}\n@media only screen and (max-width: 500px) {\n  .button {\n    width: 100% !important;\n  }\n}\n"]
   [:span.preheader
    {:style
     "box-sizing: border-box; display: none !important; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size: 1px; line-height: 1px; max-height: 0; max-width: 0; mso-hide: all; opacity: 0; overflow: hidden; visibility: hidden;"}
    "Thanks for Join in."]
   [:table.email-wrapper
    {:width "100%",
     :cellpadding "0",
     :cellspacing "0",
     :style
     "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; margin: 0; padding: 0; width: 100%;",
     :bgcolor "#F2F4F6"}
    [:tr
     [:td
      {:align "center",
       :style
       "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; word-break: break-word;"}
      [:table.email-content
       {:width "100%",
        :cellpadding "0",
        :cellspacing "0",
        :style
        "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; margin: 0; padding: 0; width: 100%;"}
       [:tr
        [:td.email-masthead
         {:style
          "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; padding: 25px 0; word-break: break-word;",
          :align "center"}
         [:a.email-masthead_name
          {:href "https://lambdahackers.com",
           :style
           "box-sizing: border-box; color: #bbbfc3; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size: 16px; font-weight: bold; text-decoration: none; text-shadow: 0 1px 0 white;"}
          [:span
           [:img {:src (str (:website-uri config) "/images/logo.png")
                  :height 64
                  :alt "lambdahackers.com"}]]]]]
       [:tr
        [:td.email-body
         {:width "100%",
          :cellpadding "0",
          :cellspacing "0",
          :style
          "-premailer-cellpadding: 0; -premailer-cellspacing: 0; border-bottom-color: #EDEFF2; border-bottom-style: solid; border-bottom-width: 1px; border-top-color: #EDEFF2; border-top-style: solid; border-top-width: 1px; box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; margin: 0; padding: 0; width: 100%; word-break: break-word;",
          :bgcolor "#FFFFFF"}
         [:table.email-body_inner
          {:align "center",
           :width "570",
           :cellpadding "0",
           :cellspacing "0",
           :style
           "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; margin: 0 auto; padding: 0; width: 570px;",
           :bgcolor "#FFFFFF"}
          [:tr
           [:td.content-cell
            {:style
             "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; padding: 35px; word-break: break-word;"}
            [:h1
             {:style
              "box-sizing: border-box; color: #2F3133; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size: 19px; font-weight: bold; margin-top: 0;",
              :align "left"}
             title]
            [:div
             {:style
              "box-sizing: border-box; color: #74787E; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size: 16px; line-height: 1.5em; margin-top: 0;",
              :align "left"}
             body]

            (when (and action-link action-text)
              [:table.body-action
               {:align "center",
                :width "100%",
                :cellpadding "0",
                :cellspacing "0",
                :style
                "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; margin: 30px auto; padding: 0; text-align: center; width: 100%;"}
               [:tr
                [:td
                 {:align "center",
                  :style
                  "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; word-break: break-word;"}
                 [:table
                  {:width "100%",
                   :border "0",
                   :cellspacing "0",
                   :cellpadding "0",
                   :style
                   "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif;"}
                  [:tr
                   [:td
                    {:align "center",
                     :style
                     "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; word-break: break-word;"}
                    [:table
                     {:border "0",
                      :cellspacing "0",
                      :cellpadding "0",
                      :style
                      "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif;"}
                     [:tr
                      [:td
                       {:style
                        "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; word-break: break-word;"}
                       [:a.button.button--
                        {:href action-link
                         :target "_blank",
                         :style
                         "-webkit-text-size-adjust: none; background: #3869D4; border-color: #3869d4; border-radius: 3px; border-style: solid; border-width: 10px 18px; box-shadow: 0 2px 3px rgba(0, 0, 0, 0.16); box-sizing: border-box; color: #FFF; display: inline-block; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; text-decoration: none;"}
                        action-text]]]]]]]]]])
            [:p
             {:style
              "box-sizing: border-box; color: #74787E; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size: 16px; line-height: 1.5em; margin-top: 0;",
              :align "left"}
             "Thanks,\n                        "
             [:br]
             "lambdahackers Team"]]]]]]
       [:tr
        [:td
         {:style
          "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; word-break: break-word;"}
         [:table.email-footer
          {:align "center",
           :width "570",
           :cellpadding "0",
           :cellspacing "0",
           :style
           "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; margin: 0 auto; padding: 0; text-align: center; width: 570px;"}
          [:tr
           [:td.content-cell
            {:align "center",
             :style
             "box-sizing: border-box; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; padding: 35px; word-break: break-word;"}
            [:p.sub.align-center
             {:style
              "box-sizing: border-box; color: #AEAEAE; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size: 12px; line-height: 1.5em; margin-top: 0;",
              :align "center"}
             "© 2018 lambdahackers.com. All rights reserved."]]]]]]]]]]])

(defn template
  [title data & {:keys [lang]
                 :or {lang "en"}}]
  (html {:mode :xml}
    (page/xml-declaration "UTF-8")
    (page/doctype :xhtml-transitional)
    (page/xhtml-tag
     lang
     (head title)
     (body (merge {:title title}
                  data)))))
