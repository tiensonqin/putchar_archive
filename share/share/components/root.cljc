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
            [share.components.post :as post]
            [share.components.comment :as comment]
            [share.components.login :as login]
            [share.components.notifications :as notifications]
            [share.components.search :as search]
            [share.components.report :as report]
            [share.components.moderation-logs :as logs]
            [share.components.widgets :as widgets]
            [share.components.right :as right]
            [share.components.tags :as tags]
            [share.helpers.image :as image]
            [share.util :as util]
            [share.dommy :as dommy]
            [clojure.string :as str]
            [share.dicts :refer [t] :as dicts]
            [bidi.bidi :as bidi]
            [share.routes]
            [share.config :as config]
            #?(:cljs [appkit.cookie :as cookie])
            #?(:cljs [web.scroll :as scroll])
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [appkit.macros :refer [oget]])))

(def routes-map
  (atom {:home          (fn [params current-user]
                          (home/home (assoc params :current-user current-user)))
         :about         (fn [params current-user]
                          (about/about params))
         :signup        (fn [params current-user]
                          (user/signup params))
         :login         (fn [params current-user]
                          (login/signin nil))
         :profile       (fn [params current-user]
                          (user/profile (atom current-user)))
         :user          (fn [params current-user]
                          (user/user params))
         :links         (fn [params current-user]
                          (user/links params))
         :comments      (fn [params current-user]
                          (user/comments params))
         :tag           (fn [params current-user]
                          (post/tag-posts params))
         :user-tag      (fn [params current-user]
                          (post/user-tag-posts params))
         :new-post      (fn [params current-user]
                          (post/new))
         :new-link      (fn [params current-user]
                          (post/new-link))
         :post          (fn [params current-user]
                          (post/post params))
         :comment       (fn [params current-user]
                          (post/post params))
         :post-edit     (fn [params current-user]
                          (post/post-edit params))
         :search        (fn [params current-user]
                          (search/search))
         :votes     (fn [params current-user]
                      (user/votes params))
         :notifications (fn [params current-user]
                          (notifications/notifications params))
         :reports       (fn [params current-user]
                          (report/reports params))
         :moderation-logs       (fn [params current-user]
                                  (logs/logs params))
         :latest        (fn [params current-user]
                          (post/latest-posts))
         :hot           (fn [params current-user]
                          (post/hot-posts))
         :drafts        (fn [params current-user]
                          (user/drafts params))
         :draft         (fn [params current-user]
                          (post/post params))
         :tags          (fn [params current-user]
                          (tags/tags params))}))

(rum/defc routes
  [reconciler route params current-user]
  (if-let [f (get @routes-map route)]
    (f params current-user)
    ;; TODO: 404
    ))

(rum/defc search-box < rum/reactive
  [search-mode?]
  (let [q (citrus/react [:search :q])
        current-path (citrus/react [:router :handler])
        search-fn (fn [q current-path]
                    (when (and (not (str/blank? q)) (>= (count q) 1))
                      (do
                        (citrus/dispatch-sync! :search/search :post/search {:q {:post_title q}
                                                                            :limit 20}
                                               :posts)
                        (citrus/dispatch! :router/push {:handler :search} true))))
        close-fn (fn []
                   (citrus/dispatch! :citrus/toggle-search-mode?)
                   (citrus/dispatch-sync! :search/reset)
                   (if (= :search current-path)
                     (citrus/dispatch! :router/back)))]
    [:div#nav-bar
     [:div#head
      [:div.wrap {:class "row space-between"}
       [:input.row.white
        {:id "search"
         :type "search"
         :autoFocus true
         :style (cond->
                    {:border "none"
                     :height 48
                     :line-height "30px"
                     :font-size "18px"
                     :font-weight "600"
                     :background "initial"}
                  (util/mobile?)
                  (assoc :max-width 300))
         :placeholder (t :search-posts)
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
       [:a {:on-click (fn [] (search-fn q current-path))
            :style {:margin-right 24
                    :margin-top 12}}
        (ui/icon {:type "search"
                  :color colors/icon-color})]

       [:a {:on-click close-fn
            :style {:margin-top 12}}
        (ui/icon {:type "close"
                  :color colors/icon-color})]]]]))

(rum/defc drawer <
  {:will-mount (fn [state]
                 (citrus/dispatch! :citrus/open-drawer?)
                 state)
   :will-unmount (fn [state]
                   (citrus/dispatch! :citrus/close-drawer?)
                   state)
   :after-render (fn [state]
                   #?(:cljs (when-let [anchors (dommy/sel ".drawer a")]
                              (doseq [anchor anchors]
                                (dommy/listen! anchor :click
                                               (fn [e]
                                                 (citrus/dispatch! :citrus/close-drawer?))))))
                   state)}
  [current-user]
  [:div.drawer.drawer-left.drawer-open.column {:style {:width 300}}
   [:div.drawer-mask]
   [:div.drawer-content-wrapper.column.swipe-in
    [:div.drawer-content {:style {:width 300}}
     [:div {:style {:padding 16
                    :background "#24292E"}}
      (when-let [name (:screen_name current-user)]
        [:div.space-between {:style {:align-items "center"}}
         [:a {:href (str "/@" name)
              :style {:margin-right 12}}
          [:img {:src (util/cdn-image name
                                      :height 100
                                      :width 100)
                 :style {:border-radius "50%"
                         :width 64
                         :height 64}}]]
         [:a {:href "/votes"}
          (ui/icon {:type :thumb_up
                    :color "#fff"})]
         [:a {:href "/settings"
              :style {:margin-left 24}}
          (ui/icon {:type :settings
                    :color "#fff"})]])]

     [:div {:style {:padding "0 4px"
                    :margin-top 12}}
      (right/tags)
      (if current-user
        (ui/button {:on-click #(citrus/dispatch! :user/logout)
                    :style {:margin "16px 12px"}}
          (t :sign-out)))]]]])

(rum/defcs head
  < rum/reactive
  [state mobile? width current-user preview?]
  (let [show-panel? (citrus/react [:layout :show-panel?])
        search-mode? (citrus/react [:search-mode?])
        {:keys [handler route-params]} (citrus/react [:router])
        current-path handler
        params (citrus/react [:router :route-params])
        new-post? (= :new-post current-path)
        post-edit? (= :post-edit current-path)
        post? (or new-post? post-edit?)
        new-report? (citrus/react [:report :new?])
        unread? (:has-unread-notifications? current-user)
        user-page? (contains? #{:user :drafts :comments :user-tag} handler)
        post-edit-page? (contains? #{:new-post :post-edit} current-path)
        padding 12
        ios? (util/ios?)]
    (if search-mode?
      (rum/with-key (search-box search-mode?) "search-box")
      [:div#nav-bar
       [:div#head {:key "head"
                   :style (cond-> {}
                            (and preview? (not mobile?))
                            (assoc :max-width 1160))}
        [:div.row {:class "wrap"
                   :style {:justify-content "space-between"}}
         [:div.row1 {:style {:align-items "center"}}
          (when mobile?
            (cond
              (and (not= current-path :home)
                   ios?)
              [:a {:style {:margin-right 20}
                   :on-click (fn [] (citrus/dispatch! :router/back))}
               (ui/icon {:type :ios_back
                         :color colors/icon-color})]
              :else
              [:a {:style {:margin-right 20}
                   :on-click (fn []
                               (citrus/dispatch! :citrus/open-drawer?))}
               (ui/icon {:type :menu
                         :color colors/icon-color})]))

          [:div.row1
           (widgets/website-logo)

           (if (and (not mobile?)
                    post-edit-page?)
             [:span {:style {:margin-left 12
                             :font-weight "600"
                             :color colors/icon-color
                             :font-size 13}}
              (t :draft)])]]

         [:div {:class "row1"
                :style {:align-items "center"}
                :id "right-head"}

          (when (not post?)
            (if mobile?
              [:a.row1.no-decoration {:style {:align-items "center"
                                              :color colors/icon-color
                                              :padding-right 12}
                                      :href "/new-link"}
               (ui/icon {:type :edit
                         :color colors/icon-color
                         :width 22})]
              [:div.row1 {:style {:align-items "center"}}
               [:a.control {:href "/new-link"
                            :style {:font-size 14
                                    :font-weight 500}}
                (t :submit-a-link)]
               [:span {:style {:color "#FFF"
                               :margin "0 6px"}}
                "|"]
               [:a.control {:style {:font-size 14
                                    :font-weight 500
                                    :padding-right 12}
                            :href "/new-article"}
                (t :write-new-post)]]))

          ;; publish
          (if post?
            (rum/with-key (post/publish-to) "publish"))

          (when new-report?
            [:a
             {:title (t :reports)
              :href "/reports"
              :style {:padding padding}}
             [:i {:class "fa fa-flag"
                  :style {:font-size 20
                          :color colors/icon-color}}]])

          ;; login or notification
          (when (and current-user unread? (not post?))
            [:a {:href "/notifications"
                 :title (t :notifications)
                 :style {:padding "10px 12px"}}
             (ui/icon {:type "notifications"
                       :color "#7fff00"})])

          ;; search
          (if (not post?)
            [:a {:title (t :search)
                 :on-click #(citrus/dispatch! :citrus/toggle-search-mode?)
                 :style {:padding padding}}
             (ui/icon {:type "search"
                       :color colors/icon-color})])

          (when-not current-user
            [:a.no-decoration {:on-click (fn []
                                           (citrus/dispatch! :user/show-signin-modal?))
                               :style {:padding-left padding
                                       :font-weight "500"
                                       :font-size 15
                                       :color colors/icon-color}}
             (t :signin)])

          (when (and (not post?)
                     (not mobile?)
                     current-user)
            (ui/menu
              [:a {:href (str "/@" (:screen_name current-user))
                   :on-click (fn []
                               (citrus/dispatch! :citrus/re-fetch
                                                 :user
                                                 {:screen_name (:screen_name current-user)}))
                   :style {:margin-left 16
                           :height 32}}
               (ui/avatar {:shape "circle"
                           :class "ant-avatar-mm"
                           :src (util/cdn-image (:screen_name current-user))})]
              [[:a.button-text {:href (str "/@" (:screen_name current-user))
                                :on-click (fn []
                                            (citrus/dispatch! :citrus/re-fetch
                                                              :user
                                                              {:screen_name (:screen_name current-user)}))
                                :style {:font-size 14}}
                (t :go-to-profile)]

               [:a.button-text {:href "/drafts"
                                :on-click (fn []
                                            (citrus/dispatch! :citrus/re-fetch :drafts nil))
                                :style {:font-size 14}}
                (t :drafts)]

               [:a.button-text {:href "/votes"
                                :on-click (fn []
                                            (citrus/dispatch! :citrus/re-fetch :votes nil))
                                :style {:font-size 14}}
                (t :votes)]

               [:a.button-text {:href "/settings"
                                :style {:font-size 14}}
                (t :settings)]

               [:a.button-text {:on-click (fn []
                                            (citrus/dispatch! :user/logout))
                                :style {:font-size 14}}
                (t :sign-out)]]

              {:menu-style {:margin-top 17}}))]]]])))

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

       (mixins/listen state js/window :mousedown
                      (fn [e]
                        (let [target (oget e "target")
                              client-x (oget e "clientX")
                              client-y (oget e "clientY")]

                          (when (and (string? (oget target "className"))
                                     ;; not quote button
                                     (not (re-find #"quote-selection-area" (oget target "className")))
                                     ;; not inside selection range
                                     (not (util/inside-selection? [client-x client-y])))
                            (citrus/dispatch-sync! :comment/clear-selection)))))

       (mixins/listen state js/window :touchstart
                      (fn [e]
                        (citrus/dispatch! :citrus/touch-start e)))

       (mixins/listen state js/window :touchend
                      (fn [e]
                        (citrus/dispatch! :citrus/touch-end e))))
     :clj nil))

(def rendered? (atom false))

(rum/defc notification < rum/reactive
  []
  (let [notification (citrus/react :notification)]
    (when notification
      (km/message (:type notification) (:body notification) nil))))

(rum/defc root < rum/reactive
  (mixins/event-mixin attach-listeners)
  {:after-render (fn [state]
                   #?(:cljs
                      (let [reconciler (-> state :rum/args first)
                            current-url (util/get-current-url)
                            last-position (get-in @reconciler
                                                  [:last-scroll-top current-url])
                            hash-part js/window.location.hash]
                        (cond
                          (and hash-part (not (str/blank? (str/trim hash-part))))
                          (util/scroll-to-element hash-part)

                          last-position
                          (.scrollTo js/window 0 last-position)

                          :else
                          (do
                            (.scrollTo js/window 0 0)
                            (citrus/dispatch-sync!
                             :citrus/set-scroll-top!
                             current-url
                             0)))))
                   state)}
  [reconciler]
  (let [open-drawer? (citrus/react [:open-drawer?])
        width (citrus/react [:layout :current :width])
        {route :handler params :route-params} (citrus/react :router)
        current-user (citrus/react [:user :current])
        loading? (if current-user
                   (citrus/react [:user :loading?]))
        mobile? (or (util/mobile?) (<= width 768))
        preview? (and
                  (contains? #{:new-post :post-edit} route)
                  (citrus/react [:post :form-data :preview?]))
        post-page? (= route :post)]
    [:div.column
     [:div.main

      (notification)

      (head mobile? width current-user preview?)

      [:div.row {:class (cond
                          preview?
                          "bigger-wrap"
                          :else
                          "wrap")
                 :style {:overflow-y "hidden"}}
       ;; left
       [:div#left {:key "left"
                   :class "row"
                   :style {:margin-top 56
                           :padding-bottom 100}}
        (routes reconciler route params current-user)]

       (when (and (not mobile?) (not (contains? #{:signup :user :new-link :new-post :post-edit :post :comment :comments :drafts :draft :user-tag :login :links} route)))
         [:div#right {:key "right"
                      :class "column1"
                      :style {:margin-top 0
                              :margin-left 12
                              :width 243
                              :border-left "1px solid #ddd"}}
          (right/right)])]]
     (login/signin-modal mobile?)
     ;; report modal
     (report/report)
     (when-not mobile?
       (widgets/desktop-alerts))
     (if open-drawer? (drawer current-user))]))
