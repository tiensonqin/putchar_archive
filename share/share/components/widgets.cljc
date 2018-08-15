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
            #?(:cljs [appkit.macros :refer [oget]])))

(rum/defc avatar
  [user {:keys [class title pro?]}]
  [:div.user-avatar
   [:a {:title (or title (:name user) (:screen_name user))
        :href (str "/@" (:screen_name user))}
    [:div.column1
     (ui/avatar (cond->
                  {:src (util/cdn-image (:screen_name user))
                   :shape "circle"}
                  class
                  (assoc :class class)))
     (if pro?
       [:span {:style {:background colors/primary
                       :color "#FFF"
                       :padding "0 6px"
                       :border-radius 6
                       :font-size 10
                       :text-align "center"}}
        "PRO"])]]])

(rum/defc raw-html
  [html]
  [:div {:dangerouslySetInnerHTML {:__html html}}])

(rum/defc transform-content < rum/reactive
  {:after-render (fn [state]
                   (util/highlight!)
                   state)}
  [body {:keys [style
                body-format
                render-opts
                on-mouse-up]
         :or {body-format :asciidoc}
         :as attrs}]
  [:div.column (cond->
                 {:class "editor"
                  :style (merge
                          {:word-wrap "break-word"}
                          style)
                  :dangerouslySetInnerHTML {:__html
                                            (if (str/blank? body)
                                              ""
                                              (content/render body body-format))}}
                 on-mouse-up
                 (assoc :on-mouse-up on-mouse-up))])

(rum/defc user-card < rum/reactive
  [{:keys [id name screen_name bio website github_handle twitter_handle] :as user}]

  (let [mobile? (util/mobile?)
        current-user (citrus/react [:user :current])]
    [:div.space-between.auto-padding
     {:style {:padding-top "24px"
              :padding-bottom "24px"
              :align-items "center"}}
     [:div.column
      [:div.row1
       (if name
         [:span {:style {:font-size (if mobile? 24 33)
                        :color "rgba(0,0,0,0.84)"}}
         name])
       [:a.control {:href (str "/@" screen_name)
                    :style {:margin-top (if mobile? 8 17)}}
        [:span {:style {:margin-left (if name 12 0)}}
         (str "@" screen_name)]]]
      [:span {:style {:font-size 16
                      :margin-left 3
                      :margin-top 8
                      :color "rgba(0,0,0,0.7)"}}
       bio]

      (if website
        [:a {:style {:margin-left 3
                     :margin-top 6}
             :href website}
         website])

      [:div.row1 {:style {:margin-top 24}}
       (let [url (str config/website "/@" screen_name "/new.rss")]
         [:a {:title "RSS"
              :href url
              :target "_blank"}
          (ui/icon {:type :rss
                    :color "#666"})])

       (if github_handle
         [:a {:href (str "https://github.com/" github_handle)
              :target "_blank"
              :style {:margin-left 24}}
          (ui/icon {:type :github
                    :color "#666"
                    :width 20})])

       (if twitter_handle
         [:a {:href (str "https://twitter.com/" twitter_handle)
              :target "_blank"
              :style {:margin-left 24}}
          (ui/icon {:type :twitter
                    :width 26
                    :height 26})])]]
     [:img {:src (util/cdn-image screen_name
                                 :height 100
                                 :width 100)
            :style {:border-radius "50%"
                    :width "6rem"
                    :height "6rem"}}]]))

(rum/defc pro < rum/reactive
  [user]
  [:div
   [:h1 "PRO"]

   [:hr.gradient]

   [:div.editor {:style {:margin-bottom 24
                         :height 336}}
    [:p (t :we-are-working-hard)]
    [:p (t :here-is-what)]

    [:ol.show-style {:style {:margin-bottom 48}}
     [:li
      (t :no-ads)]
     [:li {:style {:margin-top 12}}
      (t :a-pro-badge)]
     [:li {:style {:margin-top 12}}
      (t :more-to-come)]]
    [:p {:style {:margin-top 12}}
     (t :pro-costs)
     [:span {:style {:background colors/primary
                     :color "#FFF"
                     :border-radius 6
                     :padding "0 6px"
                     :font-size "15px"}}
      (t :nine-nine-per-month)]
     (t :you-can-cancel)]]
   (if user
     (ui/button {:class "btn-primary"
                 :on-click (fn []
                             #?(:cljs
                                (citrus/dispatch! :user/upgrade-to-pro
                                                  (fn [token]
                                                    (when-let [id (oget token "id")]
                                                      (citrus/dispatch! :user/subscribe-pro
                                                                        {:plan "pro-member"
                                                                         :source id
                                                                         :email (:email user)}))))))}
       (t :upgrade-to-pro))
     (ui/button {:class "btn-primary"
                 :on-click #(citrus/dispatch! :user/show-signin-modal?)}
       (t :login-to-upgrade-to-pro)))])

(rum/defc pro-modal < rum/reactive
  []
  (let [modal? (citrus/react [:user :pro-modal?])
        user (citrus/react [:user :current])]
    [:div#pro-modal
     (ui/dialog
      {:title "Upgrade to Pro"
       :on-close #(citrus/dispatch! :user/close-pro-modal?)
       :visible modal?
       :wrap-class-name "center"
       :style {:max-width 600}
       :animation "zoom"
       :maskAnimation "fade"}
      (pro user))]))

(rum/defc posts-comments-header < rum/reactive
  [screen_name]
  (let [current-path (citrus/react [:router :handler])
        current-user? (= screen_name (citrus/react [:user :current :screen_name]))
        posts? (= current-path :user)
        drafts? (= current-path :drafts)
        comments? (= current-path :comments)
        links? (= current-path :links)
        zh-cn? (= :zh-cn (citrus/react [:locale]))]
    [:div.auto-padding.posts-headers {:style {:margin-top 12
                                :margin-bottom 24}}
     [:div.row1.ubuntu {:style {:font-weight (if zh-cn? "500" "600")}}
      [:a.control {:class (if posts? "active" "")
                   :href (str "/@" screen_name)}
       (t :latest-posts)]

      (if current-user?
        [:a.control {:class (if drafts? "active" "")
                     :href "/drafts"
                     :style {:margin-left 24}}
         (t :drafts)])

      [:a.control {:class (if links? "active" "")
                   :style {:margin-left 24}
                   :href (str "/@" screen_name "/links")}
       (t :links)]

      [:a.control {:class (if comments? "active" "")
                   :style {:margin-left 24}
                   :href (str "/@" screen_name "/comments")}
       (t :latest-comments)]]]))

(rum/defc rule
  [group rule-expand?]
  (if @rule-expand?
    (let [rule (:rule group)]
      [:div {:style {:max-height 400}}
       [:div.divider]
       (transform-content rule {:body-format :asciidoc
                                :style {:font-size 15}})])))

(rum/defc share < rum/reactive
  [group]
  (let [mobile? (util/mobile?)]
    [:div.row1 {:style {:align-items "center"
                        :margin-left 24}}
    ;; link
    (when mobile?
      [:a.icon-button {:title (t :share)
                       :on-click (fn []
                                   (let [title (util/original-name (:name group))
                                         link (str config/website "/" (:name group))]
                                     (util/share {:title title :url link})))}
       (ui/icon {:type :share
                 :width 18
                 :height 18
                 :color "rgb(127,127,127)"})])

     (when-not mobile?
       (let [url (str "https://twitter.com/share?url=" (bidi/url-encode (str config/website "/" (:name group)))
                     "&text=Join group " (util/original-name (:name group)) "! #"
                     (:name group))]
        [:a.hover-opacity.twitter {:href url
                                   :title (str "Tweet " (util/original-name (:name group)))
                                   :target "_blank"}
         (ui/icon {:type :twitter
                   :width 24
                   :height 24})]))]))

(rum/defcs promote-dialog < rum/reactive
  (rum/local false ::promote-user)
  [state group promote?]
  (let [promote-user (::promote-user state)
        current-user (citrus/react [:user :current])
        error (citrus/react [:group :error])]
    (ui/dialog
    {:title (t :promote-member)
     :on-close #(reset! promote? false)
     :visible @promote?
     :wrap-class-name "center"
     :style {:width (min 600 (- (:width (util/get-layout)) 48))}
     :animation "zoom"
     :maskAnimation "fade"
     :footer (ui/button
               {:class "btn-primary"
                :on-click (fn []
                            (when (and (not (str/blank? @promote-user))
                                       (not= @promote-user (:screen_name current-user)))
                              (citrus/dispatch! :group/promote-user (:name group)
                                                {:id (:id group)
                                                 :screen_name @promote-user}
                                                promote?)))}
               (t :promote))}
    [:div {:key "input"
           :style {:background "#FFF"}}
     [:input
      {:class "ant-input"
       :autoFocus true
       :placeholder (str (t :username) "...")
       :on-change (fn [e]
                    (if error (citrus/dispatch! :group/clear-error))
                    (reset! promote-user (util/ev e)))}]

     (if error
       [:div {:class "help is-danger"}
        error])])))

(rum/defcs cover < rum/reactive
  (rum/local false ::rule-expand?)
  (rum/local false ::promote-modal?)
  [state group channel admin? member?]
  (let [rule-expand? (::rule-expand? state)
        promote? (::promote-modal? state)
        current-path (citrus/react [:router :handler])]
    (if (contains? #{:home :newest :latest-reply} current-path)
      [:div.ubuntu
       [:h1.heading-1 {:style {:margin-top 0
                               :margin-bottom "16px"}}
        "Lambdahackers"]

       [:p {:style {:font-size "1.125em"}}
        (t :slogan)]]

      [:div.ubuntu {:style {:padding-bottom 24}}
       [:h1.heading-1 {:style {:margin-top 0
                               :margin-bottom "16px"}}
        (util/original-name (:name group))]

       [:div {:style {:font-size "1.125em"}}
        (transform-content (:purpose group)
                           {:body-format :asciidoc})]


       [:div.row1 {:style {:align-items "center"}}
        [:a.no-decoration {:key "rules"
                           :style {:color "#1a1a1a"}
                           :on-click (fn [] (swap! rule-expand? not))}
         [:span.row1 {:style {:align-items "center"
                              :font-size 14}}
          (t :rules)
          (ui/icon {:type (if @rule-expand?
                            "expand_less"
                            "expand_more")
                    :opts {:style {:margin-top 2}}})]]

        (share group)

        (if admin?
          (ui/menu
            [:a {:on-click (fn [e])
                 :style {:margin-left 24}}
             (ui/icon {:type :more
                       :color "rgb(127,127,127)"})]
            [(if admin?
               [:a.button-text {:href (str "/" (:name group) "/edit")
                                :style {:font-size 14}}
                (t :edit)])

             (if admin?
               [:a.button-text {:on-click (fn []
                                            (reset! promote? true))
                                :style {:font-size 14}}
                (t :promote-member)])

             (if member?
               [:a.button-text {
                                :on-click #(citrus/dispatch! :user/unstar-group {:object_type :group
                                                                                 :object_id (:id group)})
                                :style {:font-size 14}}
                (t :leave-group)])]
            {:menu-style {:width 200}}))]

       (rule group rule-expand?)

       (promote-dialog group promote?)])))

(rum/defc join-button < rum/reactive
  [current-user {:keys [privacy]
                 :as group} stared? width]
  (let [invited-group (citrus/react [:group :invited-group])
        invited? (and invited-group (= invited-group (:name group)))
        invite? (= (:privacy group) "invite")]
    (cond
      (and current-user stared?)
      nil

      (or (= (:privacy group) "public")
          (and invite? invited?))
      (ui/button {:style {:width width}
                  :href (str "/" (:name group))
                  :on-click #(citrus/dispatch! :user/star-group {:object_type :group
                                                                 :object_id (:id group)})}
        (t :join))

      invite?
      (ui/button {:style {:width width}
                  :class "disabled"}
        (t :invite-only))

      :else
      nil
      )))

(rum/defc sort-buttons < rum/reactive
  [current-user group stared-group?]
  (let [current-channel (citrus/react [:channel :current])
        post-filter (citrus/react [:post :filter])
        {:keys [handler route-params]} (citrus/react [:router])
        zh-cn? (= (citrus/react [:locale]) :zh-cn)
        [path new-path hot-path latest-reply-path wiki-path] (case handler
                              :channel
                              (let [{:keys [group-name channel-name]} route-params
                                    path (str "/" group-name "/" channel-name "/")]
                                [path (str path "newest") (str path "hot") (str path "latest-reply") (str path "wiki")])

                              :group
                              (let [path (str "/" (:name group) "/")]
                                [path (str path "newest") (str path "hot") (str path "latest-reply") (str path "wiki")])

                              ["/" "/newest" "/" "/latest-reply" nil])
        post-filter (cond
                      (= handler :newest)
                      :newest
                      (= handler :home)
                      :hot
                      (= handler :latest-reply)
                      :latest-reply
                      :else
                      post-filter)
        latest-reply [:a.control.no-decoration {:key "latest-reply"
                                  :class (if (= post-filter :latest-reply) "is-active")
                                  :href latest-reply-path}
                      (t :latest-reply)]
        hot [:a.control.no-decoration {:key "hot"
                         :class (if (= post-filter :hot) "is-active")
                         :href hot-path}
             (t :hot)]

        new [:a.control.no-decoration {:key "newest"
                         :class (if (= post-filter :newest) "is-active")
                         :href new-path}
             (t :new-created)]
        wiki [:a.control.no-decoration {:key "wiki"
                          :class (if (= post-filter :wiki) "is-active")
                          :href wiki-path}
              "wiki"]]
    [:div.row1#sort-buttons.ubuntu {:style (cond->
                                      {:flex-wrap "wrap"
                                       :align-items "center"
                                       :font-weight (if zh-cn? "500" "700")
                                       :margin-bottom 24})}

     [:div.row1 {:style {:align-items "center"}}
      [:span {:style {:font-size "1.125rem"}}
       (if group latest-reply hot)]
      [:span {:style {:font-size "1.125rem"
                      :margin-left 24}} (if group hot latest-reply)]
      [:span {:style {:margin-left 24
                      :font-size "1.125rem"}} new]
      (when (contains? #{:group :channel} handler)
        [:span {:style {:margin-left 24
                        :font-size "1.125rem"}} wiki])]

     (if (and (util/mobile?) current-user group)
       (join-button current-user group stared-group? 80)

       ;; rss
       (when-not (util/mobile?)
         (let [url (str config/website "/hot.rss")]
          [:a {:title "RSS"
               :href (str config/website path (clojure.core/name (if (= post-filter :newest)
                                                                   :newest
                                                                   post-filter)) ".rss")
               :target "_blank"
               :style {:margin-left 24
                       :margin-right -2}}
           (ui/icon {:type :rss
                     :color "rgb(127,127,127)"})])))

     ]))

(rum/defc cover-nav < rum/reactive
  [group channel]
  (let [{:keys [stared_channels] :as current-user} (citrus/react [:user :current])
        stared_groups (util/get-stared-groups current-user)
        managed-groups (citrus/react [:group :managed])
        stared-group? (contains? (set (keys stared_groups)) (:id group))
        admin? (or (and group stared-group? (contains? managed-groups (:id group)))
                   (util/me? current-user))
        member? (contains? (set (keys stared_groups))
                           (:id group))]
    [:div.auto-padding
     (cover group channel admin? member?)

     (sort-buttons current-user group stared-group?)]))

(rum/defc back-to-top < rum/reactive
  []
  (let [url (util/get-current-url)]
    (when-let [scroll-top (citrus/react [:last-scroll-top url])]
      (if (> scroll-top 200)
        [:a#back-to-top {:style {:position "fixed"
                                 :bottom 20
                                 :right 20
                                 :width 36
                                 :height 36
                                 :background "#aaa"
                                 :z-index 9999
                                 :border-radius 2
                                 :padding-top 6
                                 :text-align "center"}
                         :on-click (fn []
                                     (citrus/dispatch! :citrus/set-scroll-top url 0))}
         (ui/icon {:type :back-to-top
                   :color "#efefef"})
         ]))))

(rum/defcs invite-modal < rum/reactive
  (rum/local false ::invite-emails)
  [state group]
  (let [current-user (citrus/react [:user :current])
        invite? (citrus/react [:group :invite-modal?])
        error (citrus/react [:group :error])
        invite-emails (::invite-emails state)]
    (ui/dialog
     {:title (t :invite)
      :on-close #(citrus/dispatch! :citrus/default-update
                                   [:group :invite-modal?] false)
      :visible invite?
      :wrap-class-name "center"
      :style {:width (min 600 (- (:width (util/get-layout)) 48))}
      :animation "zoom"
      :maskAnimation "fade"
      :footer (ui/button
                {:class "btn-primary"
                 :on-click (fn []
                             (when (not (str/blank? @invite-emails))
                               (citrus/dispatch! :group/send-invites {:to invite-emails
                                                                      :self-email (:email current-user)
                                                                      :who (:screen_name current-user)
                                                                      :group-name (:name group)}
                                                 invite?)))}
                (t :send))}
     [:div {:key "input"
            :style {:background "#FFF"}}
      (ui/textarea-autosize {:class "shadow"
                             :min-rows 3
                             :max-rows 12
                             :auto-focus true
                             :placeholder (str (t :invite-members-placeholder) "...")
                             :style {:border "none"
                                     :font-size 15
                                     :background "#fff"
                                     :resize "none"
                                     :width "100%"
                                     :padding 12
                                     :white-space "pre-wrap"
                                     :overflow-wrap "break-word"}
                             :default-value ""
                             :on-change (fn [e]
                                          (if error (citrus/dispatch! :group/clear-error))
                                          (reset! invite-emails (util/ev e)))})

      (if error
        [:div {:class "help is-danger"}
         error])])))

(rum/defc website-logo < rum/reactive
  []
  (let [current-handler (citrus/react [:router :handler])]
    [:a.hover-opacity.row1.no-decoration {:href "/"
                                          :on-click (fn []
                                                      (citrus/dispatch! :citrus/reset-first-group)
                                                      (citrus/dispatch! :citrus/re-fetch :home {}))
                                          :style {:margin-left -2
                                                  :align-items "center"}}
     [:div.row1 {:style {:align-items "center"}}
      (ui/icon {:type :logo
                :width 36
                :height 36})

      (when-not (or (util/mobile?)
                    (contains? #{:new-post :post-edit} current-handler))
        [:span.ubuntu {:style {:font-size 20
                               :color "#333"
                               :font-weight 600
                               :margin-top 2
                               :margin-left 6
                               :letter-spacing "0.03em"}}
         "HACKERS"])]]))

(rum/defc preview
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
        [:a.no-decoration.control {:style {:padding 12
                                           :font-size 13}}
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
               :color (if (:preview? form-data) colors/primary "#666")})]]))

(rum/defc github-connect
  []
  (ui/button {:on-click (fn []
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
                                              :margin-bottom 12
                                              :align-items "center"}}
       (ui/icon {:type :label_outline
                 :color "rgb(127,127,127)"
                 :opts {:style {:margin-right 12}}})

       (for [[tag count] tags]
         (let [this? (= current-tag (name tag))]
           [:div.row1 {:key tag
                       :style {:padding "12px 12px 12px 0"}}
            [:a.control {:class (if this?
                                  "active")
                         :href (str "/@" screen-name "/tag/" (name tag))
                         :style {:border "1px solid #666"
                                 :border-radius 6
                                 :padding "0 6px"
                                 :font-size 14}}
             (util/tag-decode (name tag))]
            [:span {:style {:margin-left 6
                           :color "#999"}}
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
