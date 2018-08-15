(ns share.components.root
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.kit.mixins :as mixins]
            [share.kit.message :as km]
            [share.kit.colors :as colors]
            [share.components.home :as home]
            [share.components.about :as about]
            [share.components.user :as user]
            [share.components.group :as group]
            [share.components.channel :as channel]
            [share.components.post :as post]
            [share.components.comment :as comment]
            [share.components.login :as login]
            [share.components.notifications :as notifications]
            [share.components.search :as search]
            [share.components.report :as report]
            [share.components.docs :as docs]
            [share.components.widgets :as widgets]
            [share.components.pricing :as pricing]
            [share.components.layout :as layout]
            [share.helpers.image :as image]
            [share.util :as util]
            [share.dommy :as dommy]
            [clojure.string :as str]
            [share.dicts :refer [t] :as dicts]
            [bidi.bidi :as bidi]
            [share.routes]
            [share.config :as config]
            #?(:cljs [web.scroll :as scroll])
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [appkit.macros :refer [oget]])))

(def routes-map
  (atom {:home          (fn [params current-user hot-groups]
                          (home/home (assoc params :current-user current-user)))
         :about         (fn [params current-user hot-groups]
                          (about/about params))
         :signup        (fn [params current-user hot-groups]
                          (user/signup params))
         :profile       (fn [params current-user hot-groups]
                          (user/profile (atom current-user)))
         :user          (fn [params current-user hot-groups]
                          (user/user params))
         :comments      (fn [params current-user hot-groups]
                          (user/comments params))
         :links      (fn [params current-user hot-groups]
                       (user/links params))
         :tag           (fn [params current-user hot-groups]
                          (post/tag-posts params))
         :user-tag      (fn [params current-user hot-groups]
                          (post/user-tag-posts params))
         :new-group     (fn [params current-user hot-groups]
                          (group/new))
         :group         (fn [params current-user hot-groups]
                          (group/group params))
         :pricing       (fn [params current-user hot-groups]
                          (pricing/pricing params))
         :members       (fn [params current-user hot-groups]
                          (group/members params))
         :group-edit    (fn [params current-user hot-groups]
                          (group/edit params))
         :groups        (fn [params current-user hot-groups]
                          (group/group-list params))
         :new-channel   (fn [params current-user hot-groups]
                          (channel/new params))
         :channels   (fn [params current-user hot-groups]
                       (channel/channels params))
         :channel       (fn [params current-user hot-groups]
                          (channel/channel params))
         :channel-edit (fn [params current-user hot-groups]
                         (channel/edit params))
         :new-post      (fn [params current-user hot-groups]
                          (post/new))
         :post          (fn [params current-user hot-groups]
                          (post/post params))
         :comment       (fn [params current-user hot-groups]
                          (post/post params))
         :post-edit     (fn [params current-user hot-groups]
                          (post/post-edit params))
         :search        (fn [params current-user hot-groups]
                          (search/search))
         :bookmarks     (fn [params current-user hot-groups]
                          (user/bookmarks params))
         :notifications (fn [params current-user hot-groups]
                          (notifications/notifications params))
         :reports       (fn [params current-user hot-groups]
                          (report/reports params))
         :privacy       (fn [params current-user hot-groups]
                          (docs/privacy))
         :terms         (fn [params current-user hot-groups]
                          (docs/terms))
         :code-of-conduct (fn [params current-user hot-groups]
                            (docs/code-of-conduct))
         :newest (fn [params current-user hot-groups]
                (post/sort-by-new))
         :latest-reply (fn [params current-user hot-groups]
                (post/sort-by-latest-reply))

         :drafts       (fn [params current-user hot-groups]
                         (user/drafts params))
         }))

(rum/defc routes
  [reconciler route params current-user hot-groups]
  (let [f (get @routes-map route)]
    (f params current-user hot-groups)))

(rum/defc search-box < rum/reactive
  [search-mode?]
  (let [q (citrus/react [:search :q])
        current-path (citrus/react [:router :handler])
        groups? (= current-path :groups)
        search-fn (fn [q current-path]
                    (when (and (not (str/blank? q)) (>= (count q) 1))
                      (when (not= current-path :channels)
                        (if groups?
                          (citrus/dispatch-sync! :search/search :group/search {:q {:group_name q}
                                                                               :limit 20})
                          (do
                            (citrus/dispatch-sync! :search/search :post/search {:q {:post_title q}
                                                                         :limit 20})
                            (citrus/dispatch! :router/push {:handler :search} true))))))
        close-fn (fn []
                   (citrus/dispatch! :citrus/toggle-search-mode?)
                   (citrus/dispatch-sync! :search/reset)
                   (if (= :search current-path)
                     (citrus/dispatch! :router/back)))]
    [:div#head {:key "head"}
     [:div.wrap {:class "row space-between"}
      [:input.row {:id "search"
                   :type "search"
                   :autoFocus true
                   :style (cond->
                            {:border "none"
                             :height 48
                             :line-height "30px"
                             :font-size "18px"
                             :font-weight "500"}
                            (util/mobile?)
                            (assoc :max-width 300))
                   :placeholder (case current-path
                                  :groups
                                  (t :search-groups)
                                  :channels
                                  (t :search-channels)
                                  (t :search-posts))
                   :value (or q "")
                   :on-change (fn [e]
                                (let [v (util/ev e)]
                                  (citrus/dispatch! :search/q (util/ev e))))
                   :on-key-down (fn [e]
                                  (case (.-keyCode e)
                                    ;; backspace
                                    8
                                    (citrus/dispatch-sync! :search/reset-result)

                                    ;; Esc
                                    27 (close-fn)
                                    nil)
                                  )
                   :on-key-press (fn [e]
                                   (citrus/dispatch-sync! :search/q (util/ev e))
                                   (when (= (.-key e) "Enter")
                                     (search-fn q current-path)))
                   }]
      [:a.control {:on-click (fn [] (search-fn q current-path))
                   :style {:margin-right 24
                           :margin-top 12}}
       (ui/icon {:type "search"})]

      [:a.control {:on-click close-fn
                   :style {:margin-top 12}}
       (ui/icon {:type "close"})]]]))

(rum/defc modal-panel
  <
  {:after-render (fn [state]
                   #?(:cljs (when-let [anchors (dommy/sel "#modal-panel a:not(.expand)")]
                              (doseq [anchor anchors]
                                (dommy/listen! anchor :click
                                               (fn [e]
                                                 (citrus/dispatch! :layout/close-panel))))))
                   state)
   }
  [width mobile? unread? new-report? current-user groups group group-path? member? group-or-channel?]
  [:div#modal-panel {:style {:width width
                             :padding 24
                             :z-index 999
                             :background-color "#f6f6f6"
                             :padding-bottom 180
                             :height "100vh"
                             :max-height "100vh"
                             :overflow-y "auto"}}
   [:div
    [:div.row1 {:style {:justify-content "space-between"
                        :align-items "center"
                        :margin-bottom 24
                        :margin-top 24}}
     (when current-user
       [:a {:href (str "/@" (:screen_name current-user))}
        (ui/avatar {:shape "circle"
                    :src (util/cdn-image (:screen_name current-user))})])

     (when group-path?
       [:a {:href "/"
            :title (t :popular)
            :style {:padding 12}}
        (ui/icon {:type :logo
                  :width 48
                  :height 48})])

     (when current-user
       [:a {:href "/bookmarks"}
        (ui/icon {:type :bookmark_border})])

     (when (and current-user unread?)
       [:a.control {:href "/notifications"}
        (ui/icon {:type "notifications"
                  :color colors/primary})])

     (when (and current-user new-report?)
       [:a {:href "/reports"}
        [:i {:class "fa fa-flag"
             :style {:font-size 20
                     :color colors/primary}}]])

     (when current-user
       [:a {:href "/settings"}
        (ui/icon {:type :settings
                  :color "#666"})])]


    (group/stared-groups false groups group)

    (when group-or-channel? (group/stared-channels group))
    (when group-or-channel? (group/group-members group member?))

    (layout/right-footer)

    (if current-user
      (ui/button {:on-click #(citrus/dispatch! :user/logout)
                  :style {:color "#777"
                          :margin-top 48}}
        (t :sign-out)))]])

(rum/defcs head
  < rum/reactive
  [state group-path? mobile? width current-user preview? groups group member? group-or-channel?]
  (let [show-panel? (citrus/react [:layout :show-panel?])
        last-scroll-top (citrus/react [:last-scroll-top (util/get-current-url)])
        search-mode? (citrus/react [:search-mode?])
        {:keys [handler route-params]} (citrus/react [:router])
        current-path handler
        params (citrus/react [:router :route-params])
        group-name (:group-name params)
        show-modal? (citrus/react [:show-modal?])
        new-post? (= :new-post current-path)
        post-edit? (= :post-edit current-path)
        post? (or new-post? post-edit?)
        unread? (:has-unread-notifications? current-user)
        stared_groups (util/get-stared-groups current-user)
        current-group-id (citrus/react [:group :current])
        groups (citrus/react [:group :by-name])
        current-group (or
                       (and group-name (get groups group-name))
                       (util/get-group-by-id groups current-group-id))

        join-group? (and current-group (contains? (set (map first (seq stared_groups))) (:id current-group)))
        new-report? (citrus/react [:report :new?])]
    (if search-mode?
      (rum/with-key (search-box search-mode?) "search-box")
      [:div#head {:key "head"}
       [:div.row {:class (if preview?
                           "bigger-wrap"
                           "wrap")
                  :style {:justify-content "space-between"}}
        (if (and (= current-path :post)
                 (and last-scroll-top (> last-scroll-top 100))
                 (not mobile?))
          (let [{:keys [screen_name permalink]} params
                permalink (util/encode-permalink (str "@" screen_name "/" permalink))
                current-post (citrus/react [:post :by-permalink permalink])]
            [:div.column1 {:style {:justify-content "center"}}
             [:span {:style {:font-size 18
                             :max-width 600
                             :font-weight "600"
                             :overflow "hidden"
                             :text-overflow "ellipsis"
                             :white-space "nowrap"
                             :display "block"
                             :height "32px"
                             :line-height "32px"
                             :margin-right 24}}
              (:title current-post)]
             [:a.control {:style {:font-size 13
                                  :margin-right 24}
                          :href (str "/" (:name current-group))}
              (util/original-name (:name current-group))]])
          [:div.row1 {:style {:align-items "center"}}
           (when (and (not= current-path :home)
                      (util/ios?))
             [:a {:style {:margin-right 12}
                  :on-click (fn [] (citrus/dispatch! :router/back))}
              (ui/icon {:type :ios_back})])
           (cond
            (contains? #{:new-post :post-edit} current-path)
            [:div.row1 {:style {:align-items "center"}}
             (widgets/website-logo)

             (if (>= width 375)
               [:span {:style {:margin-left 12
                              :font-weight "500"
                              :color "#666"
                              :font-size 13}}
               (t :draft)])]
            (= current-path :groups)
            (widgets/website-logo)

            (and (= current-path :user)
                 (and last-scroll-top (> last-scroll-top 100))
                 (not= (:screen_name params) (:screen_name current-user)))
            [:div.row1 {:style {:align-items "center"
                                :cursor "pointer"}}
             (ui/avatar {:src (util/cdn-image (:screen_name params))
                         :class "ant-avatar-mm"})
             [:span {:style {:font-size 18
                             :margin-left 12}}
              (str "@" (:screen_name params))]]

            group-path?
            (group/group-logo join-group? current-group width mobile?)

            :else
            (widgets/website-logo))]
          )

        [:div {:class "row1"
               :style {:align-items "center"}
               :id "right-head"}

         (when (and new-report? (not mobile?))
           [:a.control
            {:title (t :reports)
             :href "/reports"
             :style {:padding 12}}
            [:i {:class "fa fa-flag"
                 :style {:font-size 20
                         :color colors/primary}}]])

         (when (and (not mobile?) group-path?)
           [:a {:href "/"
                :title (t :popular)
                :style {:padding 12}}
            (ui/icon {:type :logo
                      :width 26
                      :height 26})])

         ;; search
         (if (not post?)
           [:a.control {:title (t :search)
                        :on-click #(citrus/dispatch! :citrus/toggle-search-mode?)
                        :style {:padding 12}}
            (ui/icon {:type "search"})])

         ;; publish
         (if post?
           (rum/with-key (post/publish-to) "publish"))

         ;; login or notification
         (when-not post?
           (if current-user
             (when (and unread?
                        (not mobile?))
               [:a.control {:href "/notifications"
                            :title (t :notifications)
                            :style {:padding 12}}
                (ui/icon {:type "notifications"
                          :color colors/primary})])

             (when-not group-path?
               (ui/button {:on-click (fn []
                                       (citrus/dispatch! :user/show-signin-modal?))
                           :style {:margin-left 12
                                   :margin-right 12
                                   :font-size 15}}
                 (t :signin)))))

         ;; new post
         (when (and current-user
                    (not (contains? #{:post-edit :new-post} current-path)))
           (if mobile?
             [:a {:href "/new-post"
                  :style {:padding 8}}
              (ui/icon {:type :edit})]
             (ui/button {:icon :add
                         :style {:margin-left 12}
                         :href "/new-post"}
               (t :write-new-post))))

         (when (and (not post?)
                    (not mobile?)
                    current-user)
           (ui/menu
             [:a {:href (str "/@" (:screen_name current-user))
                  :style {:margin-left 24}}
              (ui/avatar {:shape "circle"
                          :class "ant-avatar-mm"
                          :src (util/cdn-image (:screen_name current-user))})]
             [[:a.button-text {:href (str "/@" (:screen_name current-user))
                               :style {:font-size 14}}
               (t :go-to-profile)]

              [:a.button-text {:href (str "/drafts")
                               :style {:font-size 14}}
               (t :my-drafts)]

              [:a.button-text {:href "/bookmarks"
                               :style {:font-size 14}}
               (t :bookmarks)]

              [:a.button-text {:href (str "/@" (:screen_name current-user) "/links")
                               :style {:font-size 14}}
               (t :links)]

              [:a.button-text {:href "/settings"
                               :style {:font-size 14}}
               (t :settings)]

              [:a.button-text {:on-click (fn []
                                           (citrus/dispatch! :user/logout))
                               :style {:font-size 14}}
               (t :sign-out)]]

             nil))

         (when (and (not post?)
                    mobile?)
           (ui/dropdown
            {:trigger ["click"]
             :visible show-panel?
             :overlay (modal-panel width mobile? unread? new-report? current-user groups group group-path? member? group-or-channel?)
             :animation "slide-up"}
            [:a.control {:style {:padding "12px 0 12px 12px"
                                 :margin-top 2}
                         :on-click (fn []
                                     (citrus/dispatch! (if show-panel? :layout/close-panel :layout/show-panel)))}
             (ui/icon {:type "menu"})]))]]])))

(rum/defc head-title
  [mobile? title href]
  [:div.head-title
   [:a {:title (t :add-more-groups)
        :style {:display "flex"
                :flex-direction "row"
                :justify-content "space-between"
                :align-items "center"
                :color "#cecece"
                :padding "0 12px 6px 12px"}
        :href href}
    [:span.title {:style {:font-weight "500"
                          :font-size 18}}
     title]
    [:span {:key "dm-span"
            :class "direct-message"
            :style {:margin-top 6}}
     (ui/icon {:key "plus-circle"
               :type "add_circle_outline"
               :style {:font-size 20}})]]])

(defn attach-listeners
  [state]
  #?(:cljs
     (do
       (mixins/listen state js/window :resize
                      (fn []
                        (citrus/dispatch! :layout/change (util/get-layout))))

       (mixins/listen state js/window :keydown
                      (fn [e]
                        (let [k (.-keyCode e)
                              any-one? (or (oget e "altKey")
                                           (oget e "ctrlKey")
                                           (oget e "shiftKey")
                                           (oget e "metaKey"))
                              action (when-not any-one?
                                       (case k
                                         37
                                         :prev
                                         39
                                         :next
                                         nil))]
                          (cond
                            (and (contains? #{:prev :next} action)
                                 (not (contains? #{"INPUT" "TEXTAREA"} e.target.nodeName)))
                            (do
                              (citrus/dispatch! :citrus/switch action)
                              (.preventDefault e))

                            ;; alt-p preview
                            (and (oget e "altKey")
                                 (= k 80))
                            (citrus/dispatch!
                             :citrus/toggle-preview))
                          )))

       (mixins/listen state js/window :touchstart
                      (fn [e]
                        (citrus/dispatch! :citrus/touch-start e)))

       (mixins/listen state js/window :touchend
                      (fn [e]
                        (citrus/dispatch! :citrus/touch-end e)))

       (mixins/listen state js/window :mousedown
                      (fn [e]
                        (when (and (string? (oget (oget e "target") "className"))
                                   (not (re-find #"quote-selection-area" (oget (oget e "target") "className"))))
                          (citrus/dispatch-sync! :comment/clear-selection))))

       (.addEventListener js/window "scroll"
                          (fn []
                            (when @scroll/on-scroll-switch
                              (let [scroll-top (util/scroll-top)]
                                (citrus/dispatch-sync! :citrus/set-scroll-top (util/get-current-url) scroll-top))))))
     :clj nil))

(def rendered? (atom false))

(rum/defc notification < rum/reactive
  []
  (let [notification (citrus/react :notification)]
    (when notification
      (km/message (:type notification) (:body notification) nil))))

(rum/defc root < rum/reactive
  (mixins/event-mixin attach-listeners)
  {:will-mount (fn [state]
                 #?(:cljs
                    (scroll/close!))
                 state)
   :after-render (fn [state]
                   #?(:cljs
                      (do
                        (util/scroll-to-element)

                        (let [reconciler (-> state :rum/args first)
                              router (bidi/match-route share.routes/routes
                                                       js/window.location.pathname)
                              current-handler (:handler router)]
                          (when (not= current-handler :comment)
                            (when-let [last-position (get-in @reconciler
                                                             [:last-scroll-top (util/get-current-url)])]
                              (.scrollTo js/window 0 last-position)))

                          (.addEventListener js/window "popstate"
                                             (fn [e]
                                               (citrus/dispatch-sync! :query/into-back-mode))))

                        (scroll/open!)))
                   state)}
  [reconciler]
  (let [{:keys [current show-panel?]} (citrus/react :layout)
        {:keys [width height]} current
        {route :handler params :route-params} (citrus/react :router)
        current-user (citrus/react [:user :current])
        loading? (if current-user
                   (citrus/react [:user :loading?])
                   (citrus/react [:group :loading?]))
        hot-groups (citrus/react [:group :hot])
        stared-groups (util/get-stared-groups current-user)
        groups (if current-user
                 stared-groups
                 (util/normalize hot-groups))
        current-group-id (citrus/react [:group :current])
        group (some->> (vals (citrus/react [:group :by-name]))
                       (filter #(= (:id %) current-group-id))
                       first)
        member? (contains? (set (keys stared-groups))
                           (:id group))
        mobile? (or (util/mobile?) (<= width 768))
        preview? (and
                  (= route :post-edit)
                  (citrus/react [:post :form-data :preview?]))
        post-page? (= route :post)
        group-path? (contains? #{:post :comment :group :group-edit :channel :channel-edit :new-channel :group-hot-posts :group-new-posts :channels :channel-new-posts :channel-hot-posts :members} route)
        group-or-channel? (contains? #{:group :channel} route)
        hide-github-connect? (citrus/react [:hide-github-connect?])]
    [:div.column
     [:div.main {:style {:background "#FFF"}}

      (notification)

      (head group-path? mobile? width current-user preview?
        groups group member? group-or-channel?)
      [:div.row {:class (cond
                          post-page?
                          ""

                          preview?
                          "bigger-wrap"

                          :else
                          "wrap")}
       ;; left
       [:div#left {:key "left"
                   :class "row full-height"
                   :style {:margin-top (if mobile? 96 116)}}
        (routes reconciler route params current-user hot-groups)]


       (when (and (not mobile?) (not (contains? #{:signup :user :new-post :post-edit :post :comment :comments :pricing :links :drafts :user-tag :tag} route)))
         [:div#right {:key "right"
                      :class "column1"
                      :style {:margin-top 116
                              :margin-left 24
                              :width 276}}
          [:div.column
           (group/stared-groups loading? groups group)

           (when group-or-channel? (group/stared-channels group))
           (when group-or-channel? (group/group-members group member?))
           (layout/right-footer)

           (when (and
                  current-user
                  (not (:github_repo current-user))
                  (not hide-github-connect?))
             [:div {:style {:margin-top 12
                            :position "relative"}}
              (widgets/github-connect)
              [:a {:title (t :close)
                   :style {:position "absolute"
                           :top 0
                           :right 0}
                   :on-click (fn []
                               #?(:cljs (citrus/dispatch! :citrus/hide-github-connect)))}
               (ui/icon {:type :close
                         :color "#666"})]])]])
       ]]

     (widgets/invite-modal group)
     ;; (widgets/pro-modal)
     (login/signin-modal mobile?)
     ;; report modal
     (report/report)
     (when-not mobile?
       (widgets/back-to-top))]))
