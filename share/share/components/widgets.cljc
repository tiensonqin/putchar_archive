(ns share.components.widgets
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.config :as config]
            [clojure.string :as str]
            [share.dicts :refer [t]]
            [share.kit.colors :as colors]
            [share.helpers.form :as form]
            [share.content :as content]
            [bidi.bidi :as bidi]
            [share.kit.mixins :as mixins]
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
  [{:keys [id name screen_name bio github_handle] :as user}]
  (let [mobile? (util/mobile?)
        current-user (citrus/react [:user :current])
        current-path (citrus/react [:router :handler])
        drafts? (= current-path :drafts)
        self? (and current-user
                   (= screen_name (:screen_name current-user)))
        comments? (= current-path :comments)]
    [:div.column1.auto-padding.user-card {:style (if mobile?
                                                   {:margin-top 24
                                                    :margin-bottom 24
                                                    :padding 12}
                                                   {:margin-top 64
                                                    :margin-bottom 64
                                                    :padding 24})
                                          :class (if mobile?
                                                   ""
                                                   "shadow")}
     [:div {:class "space-between"}
      [:a {:href (str "/@" screen_name)
           :style {:margin-right (if mobile? 12 24)}}
       [:img {:src (util/cdn-image screen_name
                                   :height 100
                                   :width 100)
              :style {:border-radius "50%"
                      :width 64
                      :height 64}}]]
      [:div.column
       [:div.row1 {:style {:flex-wrap "wrap"
                           :align-items "center"}}
        (if name
          [:span {:style {:font-size 18
                          :font-weight "500"
                          :margin-right 6
                          :color "#000"}}
           name])
        [:a.control {:href (str "/@" screen_name)}
         [:span {:style (if name
                          {}
                          {:font-weight "500"
                           :font-size 18
                           :color "#000"
                           :margin-right 6})}
          (str "@" screen_name)]]
        (if github_handle
          [:a {:href (str "https://github.com/" github_handle)
               :style {:margin-left 12}
               :target "_blank"}
           (ui/icon {:type :github
                     :color "rgb(127,127,127)"
                     :width 18})])]

       (if bio
         (transform-content bio {:style {:margin-top 6
                                         :margin-left 1}}))

       (let [url (str config/website "/@" screen_name "/newest.rss")]
         [:div.row1 {:style {:margin-top 12
                             :flex-wrap "wrap"}}
          [:a.tag.row1 {:href url
                        :target "_blank"
                        :style {:align-items "center"
                                :width 95
                                :margin 0
                                :padding "0 6px"}}
           (ui/icon {:type :rss
                     :width 18})
           [:span {:style {:margin-left 3}}
            "Subscribe"]]
          (when self?
            [:a.tag {:href "/drafts"
                     :class (if drafts? "active" "")
                     :style {:align-items "center"
                             :margin 0
                             :margin-left 12
                             :padding "2px 6px 0 6px"}}
             (t :drafts)])
          [:a.tag {:href (str "/@" screen_name "/comments")
                   :class (if comments? "active" "")
                   :style {:align-items "center"
                           :margin 0
                           :margin-left 12
                           :padding "2px 6px 0 6px"}}
           (t :latest-comments)]])]]]))

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
                  :color colors/primary})])


     (when (and alert? new-report?)
       [:div {:style {:position "fixed"
                      :top 128
                      :right 64}}
        [:a
         {:title (t :reports)
          :href "/reports"}
         [:i {:class "fa fa-flag"
              :style {:font-size 20
                      :color "#0000ff"}}]]])

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
                     (util/scroll-to-top))}
        (ui/icon {:type :back-to-top
                  :color "#efefef"})
        ])]))

(rum/defc website-logo < rum/reactive
  []
  (let [current-handler (citrus/react [:router :handler])
        theme (citrus/react [:theme])
        mobile? (util/mobile?)]
    [:a.row1.no-decoration {:href "/"
                            :on-click (fn []
                                        (citrus/dispatch! :citrus/re-fetch :home {}))}
     (ui/icon {:type :logo
               :color colors/primary})
     [:span {:style {:font-size 20
                     :margin-top -6
                     :font-weight "bold"
                     :letter-spacing "0.05em"
                     :color colors/primary}}
      "utchar"]
     (when-not mobile?
       [:span {:style {:margin-left 6
                       :font-size 10
                       :color colors/primary}}
       "beta"])]))

(rum/defc preview < rum/reactive
  [body-format form-data]
  (let [body-format (keyword (if body-format body-format :markdown))
        markdown? (= :markdown (keyword body-format))]
    [:div.row1 {:style {:align-items "center"}}
     (when-not (util/mobile?)
       (let [on-click (fn [body-format]
                        (citrus/dispatch-sync! :citrus/set-post-form-data
                                               {:body_format body-format})
                        (citrus/dispatch! :citrus/save-latest-body-format
                                          body-format))
             all-formats [:markdown :org-mode]
             others (remove #{body-format} all-formats)]
         (ui/menu
           [:a.no-decoration.control {:style {:padding 12
                                              :font-size 14}}
            (str/capitalize (name body-format))]
           (for [body-format others]
             [:a.button-text {:style {:font-size 14}
                              :on-click (fn [] (on-click body-format))}
              (str (t :switch-to) " "
                   (str/capitalize (name body-format)))])
           {})))

    [:a {:title (if (:preview? form-data)
                  (t :back)
                  (t :preview))
         :on-click (fn []
                     (citrus/dispatch!
                      :citrus/default-update
                      [:post :form-data :preview?]
                      (not (:preview? form-data))))}
     (ui/icon {:type "visibility"
               :color (if (:preview? form-data) colors/primary colors/shadow)})]]))

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
          number 6
          has-more? (> tags-count number)
          show-expand? (and has-more? (not @expand?))
          tags (if (and has-more? (not @expand?))
                 (take 6 tags)
                 tags)]
      [:div#tags.auto-padding {:class "row1"
                                      :style {:flex-wrap "wrap"
                                              :align-items "center"}}

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

(defn empty-posts
  []
  [:div
   [:h5.auto-padding {:style {:color colors/shadow}}
    "Empty."]
   [:a {:title "Typewriter"
        :href "https://xkcd.com/477/"}
    [:img {:src "https://imgs.xkcd.com/comics/typewriter.png"}]]])

(defonce current-idx (atom nil))
(defonce tab-pressed? (atom false))
(defonce enter-pressed? (atom false))
(defonce show-autocomplete? (atom true))

(defn attach-listeners
  [state]
  #?(:cljs
     (do
       (mixins/listen state js/window :keydown
                      (fn [e]
                        (let [code (.-keyCode e)]
                          (when (contains? #{9 38 40 13 27} code)
                            (util/stop e)
                            (case code
                              9         ; confirmation
                              (reset! tab-pressed? true)
                              13
                              (reset! enter-pressed? true)
                              38        ; up
                              (if (>= @current-idx 1)
                                (swap! current-idx dec))
                              40        ; down
                              (if (nil? @current-idx)
                                (reset! current-idx 1)
                                (swap! current-idx inc))
                              27        ; esc
                              (reset! show-autocomplete? false)))))))))

;; tab or enter for confirmation, up/down to navigate
(rum/defcs autocomplete < rum/reactive
  (mixins/event-mixin attach-listeners)
  (mixins/disable-others-tabindex "a:not(.complete-item)")
  {:will-mount (fn [state]
                 (reset! current-idx 0)
                 (reset! tab-pressed? false)
                 (reset! enter-pressed? false)
                 state)
   :will-unmount (fn [state]
                   (reset! current-idx 0)
                   (reset! tab-pressed? false)
                   (reset! enter-pressed? false)
                   (reset! show-autocomplete? true)
                   state)}
  [state col item-cp element on-select menu-opts]
  #?(:cljs
     (let [show? (rum/react show-autocomplete?)]
       (when show?
        (let [width (citrus/react [:layout :current :width])
              tab-pressed? (rum/react tab-pressed?)
              enter-pressed? (rum/react enter-pressed?)
              current-idx (or (rum/react current-idx) 0)]
          (prn {:tab tab-pressed?
                :enter enter-pressed?})
          (when (seq col)
            (when tab-pressed?
              (on-select (first col)))
            (when enter-pressed?
              (on-select (nth col current-idx)))
            (let [c-idx (if current-idx (min current-idx (dec (count col))))]
              (ui/menu
                element
                (for [[idx item] (util/indexed col)]
                  [:a.button-text.row1.complete-item
                   {:key idx
                    :tab-index 0
                    :class (if (= c-idx idx) "active" "")
                    :style {:padding 12
                            :display "block"}
                    :on-click #(on-select item)
                    :on-key-down (fn [e]
                                   (when (= 13 (.-keyCode e))
                                     (on-select item)))}
                   (item-cp item)])
                (merge {:visible true}
                       menu-opts)))))))))

(rum/defcs more-content
  < (rum/local false ::expand?)
  [state content limit]
  (when content
    (let [expand? (get state ::expand?)]
      (cond
        (and @expand?
             (> (count content) limit))
        [:p content
         [:a {:style {:margin-left 3}
              :on-click #(reset! expand? false)}
          "(less)"]]
       (and (not @expand?)
            (> (count content) limit))
       [:p (util/trimr-punctuations (subs content 0 limit))
        [:a {:style {:margin-left 3}
             :on-click #(reset! expand? true)}
         "...more"]]
       :else
       [:p content]))))

(rum/defc followers
  [followers]
  (when (seq followers)
    [:div.row1 {:style {:flex-wrap "wrap"}}
     (for [follower followers]
       [:div {:key follower
              :style {:margin-right 6}}
        (avatar {:screen_name follower}
                {:class "ant-avatar-mm"})])]))
