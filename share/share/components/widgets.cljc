(ns share.components.widgets
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.config :as config]
            [clojure.string :as str]
            [share.asciidoc :as ascii]
            [share.dicts :refer [t]]
            [share.kit.colors :as colors]
            [share.helpers.form :as form]
            [share.content :as content]
            [bidi.bidi :as bidi]
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [appkit.macros :refer [oget]])
            #?(:cljs [cljs.core.async :as async]))
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])))

(rum/defc avatar
  [user {:keys [class title]}]
  [:div.user-avatar
   [:a {:title (or title (:name user) (:screen_name user))
        :href (str "/@" (:screen_name user))}
    [:div.column1
     (ui/avatar (cond->
                  {:src (util/cdn-image (:screen_name user))
                   :shape "circle"}
                  class
                  (assoc :class class)))]]])

(rum/defc raw-html
  [opts html]
  [:div (merge {:dangerouslySetInnerHTML {:__html html}}
               opts)])

(rum/defcs transform-content < rum/reactive
  {:init (fn [state props]
           #?(:cljs
              (let [ascii-loaded? (ascii/ascii-loaded?)
                    adoc-format? (= :asciidoc (keyword (:body-format (second (:rum/args state)))))]
                (when (and adoc-format? (not ascii-loaded?))
                  (citrus/dispatch-sync! :citrus/default-update
                                         [:ascii-loaded?]
                                         false)
                  (go
                    (async/<! (ascii/load-ascii))
                    (citrus/dispatch! :citrus/default-update
                                      [:ascii-loaded?]
                                      true)))))
           state)
   :after-render (fn [state]
                   (util/highlight!)
                   state)}
  [state body {:keys [style
                body-format
                render-opts
                on-mouse-up]
         :or {body-format :markdown}
               :as attrs}]
  (let [ascii-loaded? (citrus/react [:ascii-loaded?])
        body-format (keyword body-format)]
    (if (false? ascii-loaded?)
      [:div (t :loading)]
      [:div.column
      (cond->
          {:class (str "editor " (name body-format))
           :style (merge
                   {:word-wrap "break-word"}
                   style)
           :dangerouslySetInnerHTML {:__html
                                     (if (str/blank? body)
                                       ""
                                       (content/render body body-format))}}
        on-mouse-up
        (assoc :on-mouse-up on-mouse-up))])))

(rum/defc user-card < rum/reactive
  [{:keys [id name screen_name bio website github_handle twitter_handle] :as user}]
  (let [mobile? (util/mobile?)
        current-user (citrus/react [:user :current])]
    [:div.column1.auto-padding.user-card {:style {:padding-top (if mobile? 24 64)
                                                  :padding-bottom "24px"}}
     [:div.space-between
      [:div.column
       [:div.row1 {:style {:flex-wrap "wrap"}}
        (if name
          [:span {:style {:font-size (if mobile? 24 33)
                          :font-weight "600"
                          :color (colors/new-post-color)
                          :margin-right 12}}
           name])
        [:a.control {:href (str "/@" screen_name)
                     :style {:margin-top (if mobile? 8 17)}}
         [:span {:style (if name
                          {}
                          {:font-size 24
                           :color (colors/new-post-color)})}
          (str "@" screen_name)]]]

       [:div.row1 {:style {:margin-left 3
                           :flex-wrap "wrap"
                           :margin-top 12}}
        (let [url (str config/website "/@" screen_name "/newest.rss")]
          [:a.ubuntu {:href url
                      :target "_blank"}
           (ui/icon {:type :rss
                     :color "rgb(127,127,127)"})])

        (if github_handle
          [:a {:href (str "https://github.com/" github_handle)
               :style {:margin-left 20
                       :margin-top 1}
               :target "_blank"}
           (ui/icon {:type :github
                     :color "rgb(127,127,127)"
                     :width 19})])

        (if twitter_handle
          [:a {:href (str "https://twitter.com/" twitter_handle)
               :target "_blank"
               :style {:margin-left 24
                       :margin-top 3}}
           (ui/icon {:type :twitter
                     :width 21
                     :height 21
                     :color "#1DA1F3"})])

        (if (and website (not mobile?))
          [:a {:style {:margin-left 24
                       :font-size "18px"}
                       :href website}
           website])]

       (when (and website mobile?)
         [:a {:style {:font-size "18px"
                      :margin-top 12}
              :href website}
          website])]
      [:img {:src (util/cdn-image screen_name
                                  :height 100
                                  :width 100)
             :style {:border-radius "50%"
                     :width 90
                     :height 90}}]]
     (if bio
       (transform-content bio {:style {:margin-left 6
                                       :margin-top 16}}))]
    ))

(rum/defc posts-comments-header < rum/reactive
  [screen_name]
  (let [current-path (citrus/react [:router :handler])
        current-user? (= screen_name (citrus/react [:user :current :screen_name]))
        posts? (= current-path :user)
        drafts? (= current-path :drafts)
        comments? (= current-path :comments)
        zh-cn? (= :zh-cn (citrus/react [:locale]))]
    [:div.auto-padding.posts-headers {:style {:margin-top 12
                                              :margin-bottom 12}}
     [:div.divider]
     [:div.row1.ubuntu.user-buttons {:style {:font-weight (if zh-cn? "500" "600")}}
      [:a.control {:class (if posts? "is-active" "")
                   :href (str "/@" screen_name)}
       (t :latest-posts)]

      (if current-user?
        [:a.control {:class (if drafts? "is-active" "")
                     :href "/drafts"
                     :style {:margin-left 24}}
         (t :drafts)])

      [:a.control {:class (if comments? "is-active" "")
                   :style {:margin-left 24}
                   :href (str "/@" screen_name "/comments")}
       (t :latest-comments)]]]))

(rum/defc back-to-top < rum/reactive
  []
  (let [url (util/get-current-url)
        current-user (citrus/react [:user :current])
        new-report? (citrus/react [:report :new?])
        unread? (:has-unread-notifications? current-user)
        last-scroll-top (citrus/react [:last-scroll-top url])
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))
        alert? (and (not mobile?)
                    last-scroll-top
                    (> last-scroll-top 60))]
    [:div.column1
     (when (and alert? unread?)
       [:a.fadein {:href "/notifications"
                   :title (t :notifications)
                   :style {:position "fixed"
                           :top 64
                           :right 64
                           :z-index 9999}}
        (ui/icon {:type "notifications"
                  :color (colors/primary)})])


     (when (and alert? new-report?)
       [:div {:style {:position "fixed"
                      :top 128
                      :right 64}}
        [:a
         {:title (t :reports)
          :href "/reports"}
         [:i {:class "fa fa-flag"
              :style {:font-size 20
                      :color (colors/primary)}}]]])

     (if (and last-scroll-top (> last-scroll-top 200))
       [:a#back-to-top.fadein
        {:style {:position "fixed"
                 :bottom 20
                 :right 20
                 :width 36
                 :height 36
                 :z-index 9999
                 :border-radius 2
                 :padding-top 6
                 :text-align "center"}
         :on-click (fn []
                     (citrus/dispatch! :citrus/set-scroll-top url 0))}
        (ui/icon {:type :back-to-top
                  :color "#efefef"})
        ])]))

(rum/defc website-logo < rum/reactive
  []
  (let [current-handler (citrus/react [:router :handler])]
    [:a {:href "/"
         :on-click (fn []
                     (citrus/dispatch! :citrus/re-fetch :home {}))}
     (ui/icon {:type :logo
               :width 70})]))

(rum/defc preview < rum/reactive
  [body-format form-data]
  (let [markdown? (= :markdown (keyword body-format))]
    [:div.row1 {:style {:align-items "center"}}
     (when-not (util/mobile?)
       (ui/dropdown
        {:overlay (ui/button {:style {:margin-top 20}
                              :on-click (fn []
                                          (let [format (if markdown? :asciidoc :markdown)]
                                            (citrus/dispatch-sync! :citrus/set-post-form-data
                                                                   {:body_format format})
                                            (citrus/dispatch! :citrus/save-latest-body-format
                                                              format)))}
                    (str (t :switch-to) " "
                         (if markdown?
                           "Asciidoc"
                           "Markdown")))
         :animation "slide-up"}
        [:a.no-decoration.control.ubuntu {:style {:padding 12
                                                  :font-size 14
                                                  :color (colors/icon-color)}}
         (if markdown?
           "Markdown"
           "Asciidoc")]))

    [:a {:title (if (:preview? form-data)
                  "Back"
                  (t :preview))
         :on-click (fn []
                     (citrus/dispatch!
                      :citrus/default-update
                      [:post :form-data :preview?]
                      (not (:preview? form-data))))}
     (ui/icon {:type "visibility"
               :color (if (:preview? form-data) (colors/primary) (colors/shadow))})]]))

(rum/defc github-connect
  []
  (ui/button {:class "btn-primary"
              :on-click (fn []
                          (util/set-href!
                           (str config/website "/github/setup-sync")))}
    (t :connect-github)))

(rum/defcs tags <
  (rum/local false ::expand?)
  [state screen-name tags current-tag]
  (when (seq tags)
    (let [expand? (get state ::expand?)
          tags-count (count tags)
          number 12
          has-more? (> tags-count number)
          show-expand? (and has-more? (not @expand?))
          tags (if (and has-more? (not @expand?))
                 (take 12 tags)
                 tags)]
      [:div#tags.auto-padding.ubuntu {:class "row1"
                                      :style {:flex-wrap "wrap"
                                              :align-items "center"
                                              :margin-top 6
                                              :margin-bottom 6}}

       (for [[tag count] tags]
         (let [this? (= current-tag (name tag))]
           [:a.tag.no-decoration {:key tag
                                  :href (str "/@" screen-name "/tag/" (name tag))
                                  :style {:align-items "center"}
                                  :class (if this? "active")}
            (util/tag-decode (name tag))
            [:span {:style {:margin-left 6
                            :font-size "0.8em"
                            :vertical-align "super"}}
             count]]))

       (cond
         show-expand?
         [:a.row1.control {:style {:margin-left 12}
                   :on-click (fn [e]
                               (reset! expand? true))}
          (t :expand)
          " >>"
          ]

         @expand?
         [:a.row1.control {:style {:margin-left 12}
                           :on-click (fn [e]
                                       (reset! expand? false))}
          "<< "
          (t :collapse)
          ]

         :else
         nil)])))

(rum/defc non-tech-label
  []
  [:span {:style {:padding "2px 6px"
                  :border "1px solid"
                  :border-radius "3px"
                  :font-size 13}}
   (t :non-tech)])
