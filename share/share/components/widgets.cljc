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
  [opts html]
  [:div (merge {:dangerouslySetInnerHTML {:__html html}}
               opts)])

(rum/defcs transform-content < rum/reactive
  {:after-render (fn [state]
                   (util/highlight!)
                   state)}
  [state body {:keys [style
                body-format
                render-opts
                on-mouse-up]
         :or {body-format :markdown}
               :as attrs}]
  (let [body-format (keyword body-format)]
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
       (assoc :on-mouse-up on-mouse-up))]))

(rum/defc user-card < rum/reactive
  [{:keys [id name screen_name bio website github_handle twitter_handle] :as user}]
  (let [mobile? (util/mobile?)
        current-user (citrus/react [:user :current])]
    [:div.space-between.auto-padding
     {:style {:padding-top "24px"
              :padding-bottom "24px"
              :align-items "center"}}
     [:div.column
      [:div.row1 {:style {:flex-wrap "wrap"}}
       (if name
         [:span {:style {:font-size (if mobile? 24 33)
                        :color "rgba(0,0,0,0.84)"}}
         name])
       [:a.control {:href (str "/@" screen_name)
                    :style {:margin-top (if mobile? 8 17)}}
        [:span {:style (if name
                         {:margin-left 12}
                         {:color "#000"
                          :font-size 24})}
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

      [:div.row1 {:style {:margin-top 12
                          :margin-left 3
                          :align-items "center"}}
       (if github_handle
         [:a {:href (str "https://github.com/" github_handle)
              :target "_blank"}
          (ui/icon {:type :github
                    :color "#666"
                    :width 19})])

       (if twitter_handle
         [:a {:href (str "https://twitter.com/" twitter_handle)
              :target "_blank"
              :style {:margin-left 24}}
          (ui/icon {:type :twitter
                    :width 26
                    :height 26})])

       (let [url (str config/website "/@" screen_name "/newest.rss")]
         [:a.control.ubuntu {:href url
                             :target "_blank"
                             :style {:margin-left 24}}
          (ui/icon {:type :rss
                    :color "rgb(127,127,127)"})])]]
     [:img {:src (util/cdn-image screen_name
                                 :height 100
                                 :width 100)
            :style {:border-radius "50%"
                    :width "6rem"
                    :height "6rem"}}]]))

(rum/defc posts-comments-header < rum/reactive
  [screen_name]
  (let [current-path (citrus/react [:router :handler])
        current-user? (= screen_name (citrus/react [:user :current :screen_name]))
        posts? (= current-path :user)
        drafts? (= current-path :drafts)
        comments? (= current-path :comments)
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

      [:a.control {:class (if comments? "active" "")
                   :style {:margin-left 24}
                   :href (str "/@" screen_name "/comments")}
       (t :latest-comments)]]]))

(rum/defc rule
  [group rule-expand?]
  (if (and @rule-expand? (not (util/mobile?)))
    (let [rule (:rule group)]
      [:div.fadein {:style {:max-height 400}}
       [:div.divider]
       (transform-content rule {:body-format :markdown
                                :style {:font-size 15}})
       [:div.divider]])))

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
     {:on-close #(reset! promote? false)
      :visible @promote?}
     [:div {:key "input"
            :style {:background "#FFF"
                    :min-width 300}}
      [:h3 {:style {:margin "0 0 1em 0"}}
       (t :promote-member)]

      [:input
       {:class "ant-input"
        :autoFocus true
        :placeholder (str (t :username) "...")
        :on-change (fn [e]
                     (if error (citrus/dispatch! :group/clear-error))
                     (reset! promote-user (util/ev e)))}]

      (if error
        [:div {:class "help is-danger"}
         error])

      (ui/button
        {:class "btn-primary"
         :style {:margin-top 24}
         :on-click (fn []
                     (when (and (not (str/blank? @promote-user))
                                (not= @promote-user (:screen_name current-user)))
                       (citrus/dispatch! :group/promote-user (:name group)
                                         {:id (:id group)
                                          :screen_name @promote-user}
                                         promote?)))}
        (t :promote))])))

(rum/defc join-button < rum/reactive
  [current-user group stared? width]
  (cond
    (and current-user stared?)
    nil

    :else
    (ui/button {:style {:width width}
                :href (str "/" (:name group))
                :on-click #(citrus/dispatch! :user/star-group {:object_type :group
                                                               :object_id (:id group)})}
      (t :join))
    ))

(rum/defc sort-buttons < rum/reactive
  [current-user group stared-group?]
  (let [post-filter (citrus/react [:post :filter])
        {:keys [handler route-params]} (citrus/react [:router])
        zh-cn? (= (citrus/react [:locale]) :zh-cn)
        [path new-path hot-path latest-reply-path wiki-path]
        (case handler
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
        drafts (and current-user
                    [:a.control.no-decoration {:key "drafts"
                                               :href "/drafts"}
                     (str/lower-case (t :drafts))])
        bookmarks (and current-user
                       [:a.control.no-decoration {:key "bookmarks"
                                                  :href "/bookmarks"}
                        (str/lower-case (t :bookmarks))])]
    [:div.row1#sort-buttons.ubuntu {:style (cond->
                                      {:flex-wrap "wrap"
                                       :align-items "center"
                                       :font-weight (if zh-cn? "500" "700")})}

     (if group
       [:div.row1 {:style {:align-items "center"}}
        [:span {:style {:font-size "1.125rem"}}
         latest-reply]
        [:span {:style {:font-size "1.125rem"
                        :margin-left 24}} hot]
        [:span {:style {:margin-left 24
                        :font-size "1.125rem"}} new]]
       [:div.row1 {:style {:align-items "center"}}
        [:span {:style {:font-size "1.125rem"}}
         hot]
        [:span {:style {:margin-left 24
                        :font-size "1.125rem"}} new]
        (if drafts
          [:span {:style {:margin-left 24
                          :font-size "1.125rem"}} drafts])
        (if bookmarks
          [:span {:style {:margin-left 24
                          :font-size "1.125rem"}} bookmarks])])

     (when (and (util/mobile?) current-user group)
       (join-button current-user group stared-group? 80))]))

(rum/defcs cover-nav < rum/reactive
  (rum/local false ::rule-expand?)
  (rum/local false ::promote-modal?)
  [state group]
  (let [rule-expand? (::rule-expand? state)
        promote? (::promote-modal? state)
        current-path (citrus/react [:router :handler])
        current-user (citrus/react [:user :current])
        stared_groups (util/get-stared-groups current-user)
        managed-groups (citrus/react [:group :managed])
        stared-group? (contains? (set (keys stared_groups)) (:id group))
        admin? (or (and group stared-group? (contains? managed-groups (:id group)))
                   (util/me? current-user))
        member? (contains? (set (keys stared_groups))
                           (:id group))]
    [:div.auto-padding.ubuntu
     (if (contains? #{:home :newest :latest-reply} current-path)
       [:div {:style {:margin-bottom 12}}
        [:h1.heading-1 {:style {:margin-top 0
                                :margin-bottom "16px"}}
         "Lambdahackers"]

        [:p {:style {:font-size "1.125em"}}
         (t :slogan)]

        (sort-buttons current-user nil false)]

       [:div {:style {:padding-bottom 8}}
        [:div.space-between {:style {:flex-wrap "wrap"}}
         [:h1.heading-1 {:style {:margin-top 0
                                 :margin-bottom "16px"}}
          (util/original-name (:name group))]

         [:div.row1 {:style {:align-items "center"
                             :margin-left 12}}
          (if (:stars group)
            [:a.control {:title (t :see-all)
                         :href (str "/" (:name group) "/members")}
             [:span {:style {:margin-right 12}}
              (let [stars (:stars group)]
                (if (= stars 0) 1 stars))
              " "
              (str/capitalize (t :members))]])

          ;; rules
          (when-not (util/mobile?)
            [:a.control {:on-click (fn [] (swap! rule-expand? not))
                         :style {:margin-right 12}}
             (t :rules)])

          (when (not (util/mobile?))
            [:a {:target "_blank"
                 :href (str config/website
                            (cond
                              group
                              (str "/" (:name group) "/newest.rss")
                              :else
                              "/hot.rss"))}
             (ui/icon {:type :rss
                       :color "rgb(127,127,127)"})])]
         ]

        (rule group rule-expand?)

        [:div {:style {:font-size "1.125em"}}
         (transform-content (:purpose group) nil)]

        [:div.space-between {:style {:flex-wrap "wrap"}}
         (sort-buttons current-user group stared-group?)
         (ui/menu
           [:a {:on-click (fn [e])
                :style {:margin-left 24}}
            (ui/icon {:type :more
                      :color "rgb(127,127,127)"})]
           [(if member?
              [:a.button-text {:href (str "/" (:name group) "/wiki")
                               :style {:font-size 14}}
               "Wiki"])

            (if admin?
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
           {:menu-style {:width 200}})]

        (promote-dialog group promote?)])]))

(rum/defc back-to-top < rum/reactive
  []
  (let [url (util/get-current-url)]
    (when-let [scroll-top (citrus/react [:last-scroll-top url])]
      (if (> scroll-top 200)
        [:a#back-to-top.fadein
         {:style {:position "fixed"
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
                                                  :font-size 13
                                                  :font-weight "600"}}
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
                                              :margin-bottom 12
                                              :align-items "center"}}

       (for [[tag count] tags]
         (let [this? (= current-tag (name tag))]
           [:div.row1 {:key tag
                       :style {:padding "12px 12px 12px 0"
                               :align-items "center"}}
            [:a.tag.control {:class (if this?
                                  "active")
                     :href (str "/@" screen-name "/tag/" (name tag))}
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
