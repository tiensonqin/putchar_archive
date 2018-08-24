(ns share.components.post
  (:require [rum.core :as rum]
            [appkit.citrus :as citrus]
            [share.components.comment :as comment]
            [share.content :as content]
            [clojure.string :as str]
            [bidi.bidi :as bidi]
            #?(:cljs [goog.dom :as gdom])
            [share.kit.mixins :as mixins]
            [share.kit.ui :as ui]
            [share.kit.query :as query]
            [share.kit.colors :as colors]
            [share.dommy :as dommy]
            [share.util :as util]
            [share.dicts :refer [t] :as dicts]
            [appkit.macros :refer [oget]]
            [share.config :as config]
            [share.components.widgets :as widgets]
            [share.components.post-box :as post-box]
            [share.kit.infinite-list :as inf]
            #?(:cljs [cljs-drag-n-drop.core :as dnd])
            #?(:cljs [web.scroll :as scroll])))

(rum/defcs vote < rum/reactive
  (rum/local 0 ::init-tops)
  [state post]
  (let [init-tops (get state ::init-tops)
        toped-posts (set (citrus/react [:post :toped]))
        toped? (and (set? toped-posts)
                    (toped-posts (:id post)))
        tops (-> (if (:tops post) (:tops post) 0)
                 (+ @init-tops))
        post? (= :post (citrus/react [:router :handler]))
        title (if toped? (t :unvote) (t :vote))
        on-click (fn [e]
                   (citrus/dispatch! (if toped? :post/untop :post/top) (:id post))
                   (swap! init-tops (if toped? dec inc)))
        hide-votes? (citrus/react [:hide-votes?])]
    [:div.row1
     [:a.scale.control.row1 {:title title
                             :on-click on-click
                             :style {:align-items "center"}}
      (ui/icon {:width (if post? 24 18)
                :type :thumb_up
                :color (if toped? colors/primary "rgb(127,127,127)")
                :opts {:style {:margin-top -2}}})]
     (when-not hide-votes?
       [:span.number {:style {:margin-left 6
                              :font-weight "600"
                              :color "rgb(127,127,127)"}}
        tops])]))

(rum/defcs bookmark-text < rum/reactive
  [state post]
  (let [ bookmarked-posts (set (citrus/react [:post :bookmarked]))
        bookmarked? (and (set? bookmarked-posts)
                         (bookmarked-posts (:id post)))]
    [:a.button-text {:style {:font-size 14}
                     :on-click (fn [e]
                                 (citrus/dispatch! (if bookmarked? :post/unbookmark :post/bookmark)
                                                   (:id post)))}
     (if bookmarked? (t :unbookmark) (t :bookmark))]))

(rum/defcs bookmark < rum/reactive
  (rum/local 0 ::init-bookmarks)
  [state post icon-attrs]
  (let [init-bookmarks (get state ::init-bookmarks)
        bookmarked-posts (set (citrus/react [:post :bookmarked]))
        bookmarked? (and (set? bookmarked-posts)
                         (bookmarked-posts (:id post)))
        bookmarks (-> (if (:bookmarks post) (:bookmarks post) 0)
                      (+ @init-bookmarks))]
    [:a.row1.no-decoration
     {:style {:margin-right 18}
      :title (if bookmarked? (t :unbookmark) (t :bookmark))
      :on-click  (fn [e]
                   (util/stop e)
                   (citrus/dispatch! (if bookmarked? :post/unbookmark :post/bookmark)
                                     (:id post))
                   (swap! init-bookmarks (if bookmarked? dec inc)))}
     (ui/icon (merge
               {:type (if bookmarked?
                        "bookmark"
                        "bookmark_border")
                :color (if bookmarked?
                         colors/primary
                         "#666")}
               icon-attrs))]))

(rum/defc edit-toolbox < rum/reactive
  []
  (let [form-data (citrus/react [:post :form-data])
        poll? (citrus/react [:post :poll?])
        images (:images form-data)
        mobile? (util/mobile?)
        margin 24
        current-path (citrus/react [:router :handler])
        post-edit? (= :post-edit current-path)
        new-post? (= :new-post current-path)
        current-post (citrus/react [:post :current])
        body-format (or (citrus/react [:post :form-data :body_format])
                        (:body_format current-post)
                        (citrus/react [:latest-body-format])
                        :markdown)]
    [:div {:style {:display "flex"
                   :margin-right margin
                   :align-items "center"}}
     (widgets/preview body-format form-data)

     (when post-edit?
       (if (some (fn [[tid image]]
                   (true? (:processing? image))) images)
         [:div {:style {:margin-left 24}}
          (ui/donut)]

         [:a {:title (t :photo-upload)
              :on-click #?(:cljs
                           (fn []
                             (.click (gdom/getElement "photo_upload")))
                           :clj
                           identity)
              :style {:margin-left margin}}
          (ui/icon {:type :photo
                    :color "#666"})]))

     (when post-edit?
       [:a {:title (t :poll)
            :on-click #?(:cljs
                         (fn []
                           (citrus/dispatch! :post/show-poll))
                         :clj
                         identity)
            :style {:margin-left margin}}
        (ui/icon {:type :poll
                  :color (if poll? colors/primary "#666")})])

     [:input
      {:id "photo_upload"
       :multiple true
       :accept "image/*"
       :type "file"
       :on-change (fn [e]
                    #?(:cljs
                       (post-box/upload-images (.-files (.-target e)))))
       :hidden true}]]))

(rum/defc new-post-title
  [form-data init auto-focus?]
  [:div.new-post-title
   [:input {:type "text"
            :class "header-text"
            :autoComplete "off"
            :autoFocus auto-focus?
            :on-focus util/set-cursor-end
            :name "title"
            :placeholder (t :title)
            :style {:border "none"
                    :background-color "transparent"
                    :font-size 24
                    :font-weight "600"
                    :padding-left 0
                    :width "100%"}
            :on-change (fn [e]
                         (citrus/dispatch! :citrus/set-post-form-data
                                           {:title-validated? true
                                            :title (util/ev e)}))
            :value (or (:title form-data) init "")}]

   (if (false? (get form-data :title-validated?))
     [:p {:class "help is-danger"} (t :post-title-warning)])])

(rum/defcs new-post-body <
  rum/reactive
  {:init (fn [state props]
           #?(:cljs (when-let [post-box (dommy/sel1 "#post-box")]
                      (let [height (oget post-box "scrollHeight")]
                        (citrus/dispatch! :citrus/default-update
                                          [:post :latest-height]
                                          height)
                        (dommy/set-px! post-box :height height))))
           state)}
  [state form-data init body-format auto-focus?]
  (let [value (or (:body form-data) init "")
        latest-height (citrus/react [:post :latest-height])
        mobile? (util/mobile?)
        body-format (or (citrus/react [:post :form-data :body_format])
                        body-format
                        (citrus/react [:lateset-form-data])
                        :markdown)]
    [:div.row
     (when-not (and mobile? (:preview? form-data))
       [:div.editor.row {:style {:margin-top 12
                                 :min-height 800}}
        (post-box/post-box
         :post
         nil
         {:other-attrs {:autoFocus auto-focus?}
          :placeholder (t :post-body-placeholder)
          :style {:border "none"
                  :background-color "transparent"
                  :color "rgba(0,0,0,0.85)"
                  :resize "none"
                  :width "100%"
                  :line-height "1.8"
                  :white-space "pre-wrap"
                  :overflow-wrap "break-word"
                  :overflow-y "hidden"
                  :padding-bottom 48
                  :min-height #?(:clj 1024
                                 :cljs (let [min-height (:height (util/get-layout))]
                                         ;; get scrollHeight
                                         (if latest-height
                                           (max latest-height min-height)
                                           min-height)))}
          :on-change (fn [e]
                       ;; Here we need to sync firstly
                       (citrus/dispatch-sync! :citrus/set-post-form-data
                                              {:body (util/ev e)}))
          :value value})])

     (when (and (not mobile?) (:preview? form-data))
       [:div.ver_divider {:style {:margin "0 12px"}}])

     (when (or (and (not mobile?) (:preview? form-data))
               (and mobile? (:preview? form-data)))
       [:div.row {:style {:margin-top 12}}
        (comment/post-preview (or (:body form-data) init)
                              body-format)])]))

(rum/defc select-group-item < rum/static
  [id form-data group]
  (ui/button {:class "btn-sm"
              :style (cond->
                       {:margin-right 12
                        :margin-bottom 12}
                       (= id (:group_id form-data))
                       (assoc :background-color "#2e2e2e"
                              :color "#FFF"))
              :on-click (fn []
                          (citrus/dispatch! :citrus/set-post-form-data
                                            {:group_id id
                                             :group_name (:name group)}))}
    (util/original-name (:name group))))

(rum/defc add-tags
  [form-data]
  [:div#add-tags {:style {:margin "12px 0"}}
   [:h6 {:style {:margin-bottom "1em"}}
    (t :add-tags-label)]
   (ui/input {:class "ant-input"
              :type "text"
              :autoComplete "off"
              :name "tags"
              :style {:max-width 300}
              :placeholder (t :add-tags)
              :default-value (or (:tags form-data) "")
              :on-change (fn [value]
                           #?(:cljs
                              (citrus/dispatch! :citrus/set-post-form-data
                                                {:tags (util/ev value)})))})])

(rum/defcs add-canonical-url < (rum/local false ::expand?)
  [state form-data]
  (prn form-data)
  (let [expand? (get state ::expand?)
        show? (or (:canonical_url form-data) @expand?)]
    [:div#add-canonical-url.column1 {:style {:margin "12px 0"}}
     (if show?
       [:h6 {:style {:margin-bottom "1em"}}
        (t :add-canonical-url)]
       [:a.control {:style {:font-size 14}
                   :on-click (fn [] (reset! expand? true))}
       (t :add-canonical-url)])
     (when show?
       (ui/input {:class "ant-input"
                  :type "text"
                  :autoComplete "off"
                  :auto-focus true
                  :name "canonical_url"
                  :style {:max-width 300}
                  :placeholder (t :add-canonical-url-placeholder)
                  :default-value (or (:canonical_url form-data) "")
                  :on-change (fn [value]
                               #?(:cljs
                                  (citrus/dispatch! :citrus/set-post-form-data
                                                    {:canonical_url (util/ev value)})))}))]))

(rum/defc select-group < rum/reactive
  [form-data stared-groups choices skip?]
  (let [images (:images form-data)
        images? (seq images)
        wiki? (citrus/react [:post :form-data :is_wiki])]
    [:div.column.ubuntu
     [:div.row1 {:style {:align-items "center"}}
      [:h4 {:style {:margin-bottom "1em"}}
       (if @skip?
         (t :skip-group-selection)
         (t :select-group))]
      [:a {:on-click (fn []
                       (if @skip?
                         (reset! skip? false)
                         (reset! skip? true))
                       (citrus/dispatch! :post/clear-group))
           :style {:margin-left 12
                   :font-weight "600"
                   :color (if @skip?
                            colors/primary
                            "rgb(127,127,127)")}}
       (util/format "(%s)" (if @skip?
                             (t :undo)
                             (t :skip)))]]

     (when-not @skip?
       (let [c (count stared-groups)
            cp (fn [groups]
                 (for [[id group] groups]
                   [:div {:key id}
                    (select-group-item id form-data group)]))]
        (cond
          (<= c 12)
          [:div#select-groups {:class "row"
                               :style {:flex-wrap "wrap"}}
           (cp stared-groups)]

          :else
          (apply ui/select {:animation "slide-up"
                            :default-value (:group_name form-data)
                            :placeholder "choose group"
                            :on-select (fn [name]
                                         (reset! skip? false)
                                         (let [group (some->> (vals stared-groups)
                                                              (filter #(= (:name %) name))
                                                              (first))]
                                           (citrus/dispatch! :citrus/set-post-form-data
                                                             {:group_id (:id group)
                                                              :group_name (:name group)})))
                            :style {:width 300}}
            (for [[id group] stared-groups]
              (ui/option {:key id
                          :value (:name group)}
                         (:name group)))))))

     (if images? [:div.divider])

     (if images?
       [:div#set-cover
        [:h6 {:style {:margin-bottom "1em"}}
         (str (t :cover) ":")]
        (for [[id image] (:images form-data)]
          [:a.hover-opacity {:key id
                             :title (t :set-as-cover)
                             :on-click (fn []
                                         #?(:cljs (if (:url image)
                                                    (citrus/dispatch! :citrus/set-post-form-data
                                                                      {:cover (:url image)}))))}
           [:img {:src (:url image)
                  :style (cond->
                           {:width 100
                            :height 75
                            :object-fit "cover"
                            :margin-right 12}
                           (= (:url image) (:cover form-data))
                           (assoc :border "4px solid #999"))}]])])

     (add-tags form-data)

     (add-canonical-url form-data)]))

(rum/defc publish-button < rum/reactive
  [form-data]
  (let [loading? (citrus/react [:post :loading?])
        choices (citrus/react [:post :form-data :choices])]
    (let [ok? (and (util/post-title? (:title form-data))
                   (or (not (str/blank? (:body form-data)))
                       (and (seq choices)
                            (some->> (map :v choices)
                                     (remove nil?)
                                     (count)
                                     (>= 2)))
                       ))]
      (ui/button {:class (str (if ok?
                                " btn-primary "
                                " disabled"))
                  :on-click (fn []
                              (if ok?
                                (citrus/dispatch!
                                 :citrus/default-update
                                 [:post :publish-modal?]
                                 true)))}
        (if loading?
          (ui/donut-white)
          (t :publish))))))

(rum/defcs publish-to < rum/reactive
  (rum/local false ::skip?)
  [state]
  (let [skip? (get state ::skip?)
        form-data (citrus/react [:post :form-data])
        modal? (citrus/react [:post :publish-modal?])
        current-user (citrus/react [:user :current])
        current-group (citrus/react [:group :current])
        current-post (citrus/react [:post :current])
        choices (citrus/react [:post :form-data :choices])
        stared-groups (util/get-stared-groups current-user)
        group-id (or (:group_id form-data)
                     current-group
                     (-> stared-groups ffirst))

        initial-group (get stared-groups group-id)

        submit-fn (fn []
                    (let [data (cond->
                                 (merge {:id (:id current-post)
                                         :is_draft false}
                                        (select-keys form-data
                                                     [:title :body :choices :body_format]))

                                 (:group_id form-data)
                                 (merge (select-keys form-data [:group_id :group_name]))

                                 (:cover form-data)
                                 (assoc :cover (:cover form-data))

                                 (:tags form-data)
                                 (assoc :tags (:tags form-data))

                                 (and (:canonical_url form-data)
                                      (re-find (re-pattern util/link-re) (:canonical_url form-data)))
                                 (assoc :canonical_url (:canonical_url form-data)))
                          data (if (nil? (:body_format data))
                                 (assoc data :body_format :markdown)
                                 data)
                          data (util/map-remove-nil? data)
                          data (if @skip?
                                 (assoc data
                                        :group_id nil
                                        :group_name nil)
                                 data)]
                      (citrus/dispatch! :post/update data)
                      (citrus/dispatch!
                       :citrus/default-update
                       [:post :publish-modal?]
                       false)))]

    (when-let [tags (:tags current-post)]
      (when (nil? (:tags form-data))
          (citrus/dispatch-sync! :citrus/set-post-form-data
                              {:tags (str/join "," (:tags current-post))})))

    (when (and
           (false? @skip?)
           (or (nil? (:group_id form-data))
               (nil? (:group_name form-data))))
      (citrus/dispatch! :citrus/set-post-form-data
                        {:group_id group-id
                         :group_name (:name initial-group)}))
    [:div {:style {:display "flex"
                   :flex-direction "row"
                   :flex "0 1 1"
                   :align-items "center"}
           :on-key-down (fn [e]
                          (when (= 13 (.-keyCode e))
                            ;; enter key
                            (submit-fn)))}

     (edit-toolbox)

     (publish-button form-data)

     (if modal?
       (ui/dialog
        {:key "publish-modal"
         :title (t :publish-to)
         :on-close #(citrus/dispatch! :citrus/default-update [:post :publish-modal?] false)
         :visible modal?
         :wrap-class-name "center"
         :style {:width (min 600 (- (:width (util/get-layout)) 48))}
         :animation "zoom"
         :maskAnimation "fade"
         :footer
         (ui/button
           {:tab-index 0
            :class "btn-primary"
            :on-click submit-fn}
           (t :publish))}
        (select-group form-data stared-groups choices skip?)))]))

(rum/defc choices-cp < rum/static
  [{:keys [poll_choice poll_closed choices] :as post} choices-style]
  (when (seq choices)
    [:div.column1 {:style (assoc choices-style :position "relative")}
     (if poll_closed
       [:a {:title (t :locked)}
        (ui/icon {:type :lock
                  :color "#999"
                  :width 20
                  :height 20
                  :opts {:style {:position "absolute"
                                 :right 0
                                 :top 12}}})])

     [:div.column
      (for [{:keys [id v votes]} choices]
        (let [chosen? (= id poll_choice)]
          [:a.control.row1.no-decoration.scale
           {:style {:margin-bottom 24
                    :align-items "center"
                    :color (if (nil? poll_choice)
                             "#1a1a1a"
                             "#666")}
            :key id
            :on-click (fn [e]
                        (when-not (or poll_closed poll_choice)
                          (util/stop e))
                        (if (nil? poll_choice)
                          (citrus/dispatch! :post/vote-choice
                                            (:permalink post)
                                            {:post_id (:id post)
                                             :choice_id id})))}
           (if chosen?
             [:i {:class "fa fa-check-square-o"
                  :style {:font-size 20
                          :margin-right 12}}]
             [:span.radio-button {:style {:border-radius 10}}])
           v
           (let [votes (if votes votes 0)]
             [:span.number {:style {:font-weight "600"
                                    :margin-left 12
                                    :color "#999"
                                    :padding-top 4}}
              votes])]))]]))

(rum/defcs edit-choices < rum/reactive
  (rum/local false ::expand?)
  [state post choices]
  (let [expand? (get state ::expand?)
        locked? (get post :poll_closed)
        edit? (boolean (seq choices))
        choices (or (citrus/react [:post :form-data :choices]) choices)
        choices (cond
                  (and (= 1 (count choices))
                       (seq choices))
                  (vec (conj choices
                             {:id (util/random-uuid)}))

                  (seq choices)
                  choices

                  :else
                  [{:id (util/random-uuid)}
                   {:id (util/random-uuid)}])]
    [:div.column1.choices {:style {:position "relative"}}
     (for [[idx {:keys [id v]}] (util/indexed choices)]
       [:div.row1 {:key id}
        (ui/input (cond->
                    {:class (if locked?
                              "ant-input ant-input-disabled"
                              "ant-input")
                     :type "text"
                     :autoComplete "off"
                     :name id
                     :style {:max-width 300
                             :margin-bottom 12
                             :padding "0 6px"}
                     :placeholder (str (t :choice) " " (inc idx))
                     :default-value (or v "")
                     :on-change (fn [e]
                                  (when-not locked?
                                    (let [v (util/ev e)]
                                      (when-not (str/blank? v)
                                        (citrus/dispatch! :post/add-or-update-choice choices id v)))))}))
        (if (and (not locked?)
                 (> (count choices) 2))
          [:a {:title (t :delete)
               :style {:margin-left 12}
               :on-click (fn []
                           (citrus/dispatch! :post/delete-choice id))}
           (ui/icon {:type "delete"
                     :color "#999"
                     :width 20})])])

     (when-not locked?
       [:a {:on-click (fn []
                        (citrus/dispatch! :post/add-or-update-choice
                                          choices
                                          (util/random-uuid) nil))}
        [:span (t :add-a-choice)]])

     (when-not @expand?
       [:a {:style {:position "absolute"
                    :right 0
                    :top 0}
            :on-click #(reset! expand? true)}
        (ui/icon {:type :more
                  :color "#999"})])

     (when (and (not locked?)
                (not (:is_draft post))
                @expand?)
       [:a {:style {:position "absolute"
                    :right 48
                    :top 1}
            :title (t :lock-this-poll)
            :on-click (fn []
                        (citrus/dispatch! :post/disable-poll
                                          post))}
        (ui/icon {:type :lock
                  :color "#999"
                  :width 22
                  :height 22})])

     (when @expand?
       [:a {:style {:position "absolute"
                    :right 0
                    :top 0}
            :title (t :delete-this-poll)
            :on-click (fn []
                        (citrus/dispatch! :post/delete-poll
                                          post))}
        (ui/icon {:type :delete
                  :color "#999"})])]))

(rum/defc new < rum/reactive
  {:will-mount (fn [state]
                 #?(:cljs (citrus/dispatch! :citrus/set-default-body-format))
                 state)
   :will-unmount (fn [state]
                   state)}
  []
  (let [form-data (citrus/react [:post :form-data])
        width (citrus/react [:layout :current :width])
        preview? (:preview? form-data)]
    [:div.column {:class "editor"
                  :style {:max-width (if (and preview? (> width 1024))
                                       1238
                                       768)
                          :margin "0 auto"}}
     [:div.auto-padding {:style {:flex 1
                                 :overflow "hidden"}}

      (new-post-title form-data nil true)

      (new-post-body form-data nil nil false)]]))


(rum/defc ops-twitter
  [post zh-cn?]
  (when-not zh-cn?
    (let [url (str "https://twitter.com/share?url="
                   (bidi/url-encode #?(:cljs js/location.href
                                       :clj (util/post-link post)))
                   "&text="
                   (bidi/url-encode (:title post))

                   " #"
                   (get-in post [:group :name]))]
      [:a {:title (t :tweet)
           :href url
           :target "_blank"
           :style {:margin-right 24}}
       (ui/icon {:type :twitter
                 :width 27
                 :height 27})])))

(rum/defc ops-link
  [post]
  [:a.icon-button {:title (t :link)
                   :on-click #?(:cljs
                                (fn []
                                  (let [title (:title post)
                                        link (util/post-link post)]
                                    (util/share {:title title :url link})))
                                :clj
                                identity)
                   :style {:margin-right 24
                           :margin-top 4}}
   (ui/icon {:type :share
             :width 18
             :color "#666"})])

(rum/defcs ops-notification
  [state post email]
  (let [level (:notification_level post)]
    (ui/menu
      [:a {:style {:margin-right 24}
           :on-click (fn [])}
      (case level
        "mute"
        (ui/icon {:type :notifications_off
                  :color colors/primary})
        "watch"
        (ui/icon {:type :notifications
                  :color colors/primary})
        (ui/icon {:type :notifications_none
                  :color "#666"}))]
      [[:a.button-text {:style {:font-size 14}
                        :on-click (fn []
                                    (citrus/dispatch! :post/set-notification-level
                                                      {:permalink (:permalink post)
                                                       :email email
                                                       :level "watch"}))}
        [:div.row1
         (ui/icon {:type :notifications})
         [:div.column1 {:style {:margin-left 12}}
          [:span {:style {:font-weight "600"
                          :font-size "1.3em"
                          :margin-bottom 4}}
           (t :watch)]
          (t :watch-text)]]]

       [:a.button-text {:style {:font-size 14}
                        :on-click (fn []
                                    (citrus/dispatch! :post/set-notification-level
                                                      {:permalink (:permalink post)
                                                       :email email
                                                       :level "default"}))}
        [:div.row1
         (ui/icon {:type :notifications_none})
         [:div.column1 {:style {:margin-left 12}}
          [:span {:style {:font-weight "600"
                          :font-size "1.3em"
                          :margin-bottom 4}}
           (t :default)]
          (t :default-text)]]]

       [:a.button-text {:style {:font-size 14}
                        :on-click (fn []
                                    (citrus/dispatch! :post/set-notification-level
                                                      {:permalink (:permalink post)
                                                       :email email
                                                       :level "mute"}))}
        [:div.row1
         (ui/icon {:type :notifications_off})
         [:div.column1 {:style {:margin-left 12}}
          [:span {:style {:font-weight "600"
                          :font-size "1.3em"
                          :margin-bottom 4}}
           (t :mute)]
          (t :mute-text)]]]]
      {})))

(rum/defc ops-delete
  [post current-user-id]
  (if (= current-user-id (get-in post [:user :id]))
    [:a.button-text {:on-click #(citrus/dispatch! :post/open-delete-dialog? post)
                     :style {:font-size 14}}
     (t :delete-this-post)]))

(rum/defc ops-flag
  [post]
  [:a.button-text {:on-click #(citrus/dispatch! :citrus/default-update [:report]
                                                {:type :post
                                                 :id (:id post)
                                                 :modal? true})
                   :style {:font-size 14}}
   (t :report-this-post)])

(rum/defc ops-delete-dialog < rum/reactive
  []
  (let [delete-dialog? (citrus/react [:post :delete-dialog?])
        post (citrus/react [:post :delete-post])]
    (ui/dialog
    {:title (t :post-delete-confirm)
     :on-close #(citrus/dispatch! :post/close-delete-dialog?)
     :visible (and delete-dialog? post)
     :wrap-class-name "center"
     :style {:width (min 600 (- (:width (util/get-layout)) 48))}
     :footer (ui/button
               {:class "btn-danger"
                :on-click (fn [e]
                            (util/stop e)
                            (citrus/dispatch! :post/delete post))}
               (t :delete))}
    [:div {:style {:background "#FFF"}}
     [:a {:style {:margin-left 12
                  :color "rgba(0,0,0,0.84)"}
          :href (str "/" (:permalink post))
          :on-click (fn [e]
                      (util/stop e))}
      (:title post)]])))

(rum/defc ops-menu
  [post self? mobile? absolute?]
  (ui/menu
    [:a {:style (if absolute?
                  {:position "absolute"
                   :right (if mobile? 12 0)
                   :margin-right -2
                   :margin-top -2}
                  {:margin-top 2
                   :margin-right 12})
         :on-click (fn [e]
                     (util/stop e))}
     (ui/icon {:type :more
               :color "#999"
               :width 20
               :height 20})]
    [
     ;; edit
     (when self?
       [:a.button-text {:style {:font-size 14}
                        :on-click (fn [e]
                                    (util/set-href! (str config/website "/p/" (:id post) "/edit")))}
        (t :edit)])

     ;; delete
     (when self?
       [:a.button-text {:style {:font-size 14}
                        :on-click (fn [e]
                                    (citrus/dispatch! :post/open-delete-dialog? post))}
        (t :delete)])

     ;; bookmark
     (bookmark-text post)

     ;; report
     (when (not self?)
       (ops-flag post))]
    {:menu-style {:width 200}}))

(rum/defc tags
  [tags opts tag-style]
  (if (seq tags)
    [:span.ubuntu opts
     (for [tag tags]
       [:a.tag.control
        {:key (util/random-uuid)
         :href (str "/tag/" (name tag))
         :style (merge {:margin-right 12
                        :white-space "nowrap"}
                       tag-style)}
        (util/tag-decode (name tag))])]))

(rum/defcs post-item < {:key-fn (fn [post]
                                  (:id post))}
  rum/static
  rum/reactive
  [state post show-avatar? show-group? opts]
  (if post
    (let [current-path (citrus/react [:router :handler])
          width (citrus/react [:layout :current :width])
          mobile? (or (util/mobile?) (<= width 768))
          post-link (str "/" (:permalink post))
          current-user (citrus/react [:user :current])
          current-user-id (:id current-user)
          user (:user post)
          user-id (or (:id user) (:user_id post))
          self? (and current-user-id (= user-id current-user-id))
          user-link (str "/@" (:screen_name user))

          link (:link post)
          group-name (get-in post [:group :name])
          user? (contains? #{:user :links :drafts :user-tag} current-path)
          drafts-path? (= current-path :drafts)
          post-link (if drafts-path?
                      (str "/p/" (:id post) "/edit")
                      post-link)
          {:keys [last_reply_at created_at]} post
          self? (and current-user self?)]
      [:div.post-item.col-item {:style {:position "relative"}}
       (when user?
         (ops-menu post self? mobile? true))

       [:div.row
        (if show-avatar?
          [:a {:href user-link
               :on-click util/stop
               :title (str (t :posted-by) (:screen_name user))
               :style {:margin-right 12
                       :padding-top 5}}
           (ui/avatar {:src (util/cdn-image (:screen_name user))
                       :shape "circle"})])
        [:div.column
         (when user?
           [:div.row1 {:style {:margin-bottom 12
                               :color "#666"
                               :font-size 15}}
            (util/date-format (:created_at post))

            (when group-name
              [:span {:style {:margin "0 12px"}}
               "/"])

            (when group-name
              [:a.control {:href (str "/" group-name)
                           :on-click (fn [e] (util/stop e))}
               (util/original-name group-name)])])

         [:div.column {:style {:justify-content "center"}}
          [:div.space-between
           [:div.ubuntu
            [:a.no-decoration.post-title {:href post-link
                               :style {:margin-right 6}}
             (if (:choices post)
               (str "[" (str/lower-case (t :poll)) "] "
                    (:title post))
               (:title post))]

            (when-not mobile?
              (tags (:tags post)
                   {:style (cond-> {:vertical-align "middle"}
                             mobile?
                             (assoc :display "flex"
                                    :flex-directon "row"
                                    :flex-wrap "wrap"))}
                   {:font-size "8pt"
                    :height "16px"
                    :margin "6px 6px 6px 0"}))

            (if link
              [:a.control {:on-click (fn []
                               (util/set-href! link))
                           :href link
                           :style {:text-decoration "none"
                                   :font-style "italic"
                                   :vertical-align "text-top"
                                   :font-size 11}}
               (util/get-domain link)])]

           [:a.comments_count {:href post-link
                               :title (str (:comments_count post)
                                           " "
                                           (t :replies))
                               :on-click util/stop
                               :style {:margin-left 24}}
            [:span.number {:style {:font-weight "600"
                                   :color "#919191"
                                   :font-size 18}}
             (:comments_count post)]]]

          (if-let [cover (:cover post)]
            [:a {:href post-link}
             [:img.hover-shadow {:src (str cover "?w=" 200)
                                 :style {:max-width 200
                                         :border-radius 4
                                         :margin-top 8
                                         :margin-bottom 6}}]])]

         (when (:choices post)
           [:div {:style {:margin-top 24
                          :margin-left 6
                          :margin-bottom -12}}
            (choices-cp (update post :choices util/read-string) {:align-items "flex-start"})])

         (when-let [video (:video post)]
           (when-let [id (last (str/split video #"/"))]
             [:div {:style {:margin-top 12}}
              (widgets/raw-html (content/build-youtube-frame id nil false))]))

         (when-not user?
           [:div.space-between.ubuntu {:style {:align-items "center"
                                               :margin-top 12}}
            [:div.row1 {:style {:align-items "center"}}
             (vote post)
             ]


            [:div.row1 {:style {:color "rgb(127,127,127)"
                                :font-size 14}}
             [:div.row1 {:style {:align-items "center"
                                 :flex-wrap "wrap"
                                 :margin-left 12}}
              (when show-group?
                [:a.control {:href (str "/" group-name)
                             :style {:margin-right 12}
                             :on-click (fn [e] (util/stop e))}

                 (util/original-name group-name)])]

             (when-not mobile?
               (let [last-reply-by (:last_reply_by post)
                     frequent_posters (-> (remove (hash-set (:screen_name user) last-reply-by)
                                                  (:frequent_posters post))
                                          (conj last-reply-by))
                     frequent_posters (->> (remove nil? frequent_posters)
                                           (take 5))]
                 (when (seq frequent_posters)
                   [:div.row1 {:style {:margin-right 6}}
                    (for [poster frequent_posters]
                      (if poster
                        [:a {:href (str "/@" poster)
                             :key (str "frequent-poster-" poster)
                             :title (str (t :frequent-poster) poster)
                             :style {:margin-right 6}}
                         (ui/avatar {:class "ant-avatar-sm"
                                     :src (util/cdn-image poster)})]))])))

             (when-not mobile?
               (ops-menu post self? mobile? false))

             [:a.no-decoration.control {:title (if last_reply_at
                                                 (str
                                                  (t :created-at) ": " (util/date-format created_at)
                                                  "\n"
                                                  (t :last-reply-at) ": " (util/date-format last_reply_at)
                                                  "\n"
                                                  "By: " (:last_reply_by post))
                                                 (str
                                                  (t :created-at) ": " (util/date-format created_at)))
                                        :href (if-let [last-reply-idx (:last_reply_idx post)]
                                                (str post-link "/" last-reply-idx)
                                                post-link)}
              (if last_reply_at
                (util/time-ago (:last_reply_at post))
                (util/time-ago created_at))]]
            ])]]])))

(rum/defc posts-stream < rum/reactive
  [posts show-avatar? show-group? end? opts loading?]
  (let [permalink-posts (citrus/react [:post :by-permalink])
        posts (mapv (fn [post]
                      (->> (select-keys (get permalink-posts (:permalink post)) [:choices :tops :comments_count :poll_closed])
                           (util/map-remove-nil?)
                           (merge post)))
                    posts)]
    [:div.posts
     (inf/infinite-list (map (fn [post]
                               (post-item post show-avatar? show-group? opts)) posts)
                        {:on-load
                         (if end?
                           identity
                           (fn []
                             (citrus/dispatch! :citrus/load-more-posts
                                               opts)))})
     (when loading?
       (ui/bouncing-loader))

     (ops-delete-dialog)]
    ))

(rum/defcs post-list < rum/static
  (rum/local nil ::last-post)
  rum/reactive
  "Render a post list."
  [state {:keys [result end?]
          :as posts} opts & {:keys [empty-widget
                                    show-avatar?
                                    show-group?]
                             :or {show-avatar? true
                                  show-group? false}}]
  (let [last-post (get state ::last-post)
        posts result
        current-filter (citrus/react [:post :filter])
        current-path (citrus/react [:router :handler])
        posts (if (= current-filter :newest)
                (reverse (sort-by :created_at posts))
                posts)
        posts (util/remove-duplicates :id posts)
        scroll-loading? (citrus/react [:query :scroll-loading? current-path])]
    (when (not= @last-post (last posts))
      (reset! last-post (last posts)))
    (if (seq posts)
      (posts-stream
       posts
       show-avatar?
       show-group?
       end?
       (assoc opts :last last-post)
       scroll-loading?)
      [:div.empty-posts
       (if empty-widget
         empty-widget
         [:a.row1.auto-padding {:href "/new-post"
                                :style {:font-size 20
                                        :margin-top 24
                                        :align-items "center"}}
          (ui/icon {:type :edit
                    :width 22
                    :height 22
                    :opts {:style {:margin-right 12}}})
          [:span.ubuntu {:style {:margin-top 3
                                 :color "#000000"}}
           (t :be-the-first)]])])))

(rum/defc user-post-list <
  rum/static
  rum/reactive
  [user-id {:keys [result end?]
            :as posts} posts-path]
  (let [current-handler (citrus/react [:router :handler])]
    (post-list posts
               {:user_id user-id
                :merge-path posts-path}
               :show-avatar? false
               :show-group? true
               :empty-widget [:div
                              [:span {:style {:padding 24
                                              :font-size "24"}}
                               (case current-handler
                                 :drafts (t :no-drafts-yet)
                                 :links (t :no-links-yet)
                                 :bookmarks (t :no-bookmarks-yet)
                                 (t :no-posts-yet))]])))

(rum/defcs post-edit < rum/reactive
  (mixins/query :post-edit)
  (mixins/interval-mixin :post-auto-save
                         5000
                         (fn [] (citrus/dispatch! :post/save)))
  [state params]
  (let [form-data (citrus/react [:post :form-data])
        clear-interval? (citrus/react [:post :clear-interval?])
        width (citrus/react [:layout :current :width])
        preview? (:preview? form-data)]
    (when clear-interval?
      (when-let [interval (get state :post-auto-save)]
        (util/clear-interval interval)))
    (query/query
      (let [{:keys [choices] :as post} (citrus/react [:post :current])
            show-poll? (or (seq choices)
                           (citrus/react [:post :poll?]))]
        (when (and post (nil? (:title form-data)))
          (citrus/dispatch! :citrus/set-post-form-data
                            (cond->
                              (select-keys post [:title :body :choices :body_format :canonical_url])
                              (:group post)
                              (assoc :group_name (get-in post [:group :name])
                                     :group_id (get-in post [:group :id]))
                              (:body form-data)
                              (assoc :body (:body form-data)))))
        [:div.column.center-area.auto-padding {:class "post-edit editor"
                                               :style (if (and preview? (> width 1024))
                                                        {:max-width 1238}
                                                        {})}
         (new-post-title form-data (or
                                    (:title form-data)
                                    (:title post)) (not (str/blank? (:title post))))

         (if show-poll?
           [:div.divider])

         (if show-poll?
           (edit-choices post (or (:choices form-data)
                                  choices)))

         (if show-poll?
           [:div.divider])

         (new-post-body form-data
                        (:body post)
                        (:body_format post)
                        (not (str/blank? (:body post))))]))))

(rum/defcs toolbox < rum/reactive
  (rum/local nil ::form-data)
  [state post]
  (let [current-user (citrus/react [:user :current])
        current-user-id (:id current-user)
        email (:email current-user)
        form-data (get state ::form-data)
        zh-cn? (= :zh-cn (citrus/react :locale))
        width (citrus/react [:layout :current :width])
        mobile? (or (util/mobile?) (<= width 768))
        scroll-top (citrus/react [:last-scroll-top (util/get-current-url)])]
    [:div.row {:id "toolbox"
               :style {:padding 0
                       :align-items "center"
                       :margin-bottom 48}}

     [:div.row {:style {:align-items "center"}}
      (vote post)]

     (if (util/mobile?)
       (ops-link post)
       (ops-twitter post zh-cn?))

     (ops-notification post email)

     (bookmark post nil)

     (ui/menu
       [:a {:on-click (fn [])}
        (ui/icon {:type :more
                  :color "#999"})]
       [(ops-flag post)
        (ops-delete post current-user-id)]
       {:menu-style {:width 200}})]))

(rum/defc quote-selection < rum/reactive
  [current-user]
  (let [selection-mode? (citrus/react [:comment :selection :mode?])]
    (when (and current-user selection-mode?)
      (let [selection (util/get-selection)]
        (when-let [text (:text selection)]
          [:a#quote-selection.no-decoration.quote-selection-area
           {:style {:padding "6px 12px"
                    :border-radius 4
                    :background "#bbb"
                    :color "#FFF"
                    :position "fixed"
                    :top (+ (get-in selection [:boundary :bottom]) 12)
                    :left (+ (get-in selection [:boundary :left]))}
            :on-click (fn [e]
                        (citrus/dispatch! :comment/quote text)
                        #?(:cljs
                           (when-let [element (gdom/getElement "post-comment-box")]
                             (scroll/into-view element)
                             ;; focus input
                             (.focus element.firstChild))))}
           [:div.row1.quote-selection-area {:style {:align-items "center"}}
            [:i.quote-selection-area {:class "fa fa-quote-left"}]
            [:span.quote-selection-area {:style {:margin-left 12}}
             "Quote"]]])))))

(rum/defcs post < rum/reactive
  (rum/local false ::raw?)
  (mixins/query :post)
  {:after-render
   (fn [state]
     #?(:cljs
        (do
          (when-let [idx (:comment-idx (first (:rum/args state)))]
            (citrus/dispatch! :comment/scroll-into idx))
          state)
        :clj
        state)
     state)
   :will-unmount
   (fn [state]
     (citrus/dispatch! :citrus/default-update
                       [:post :current] nil)
     state)}
  [state {:keys [screen_name permalink] :as params}]
  (let [permalink (util/encode-permalink (str "@" screen_name "/" permalink))
        current-user (citrus/react [:user :current])
        raw? (get state ::raw?)]
    (query/query
      (let [post (citrus/react [:post :by-permalink permalink])]
        (if post
          (let [{:keys [group user choices]} post
                current-reply (citrus/react [:comment :reply])
                avatar (util/cdn-image (:screen_name user))]
            [:div.column.auto-padding {:key "post"}
             [:div {:style {:padding "12px 0"}}
              [:div.center-area
               [:div.ubuntu
                {:style {:display "flex"
                         :flex-direction "row"
                         :padding-bottom 24}}
                [:a {:href (str "/@" (:screen_name user))}
                 (ui/avatar {:src avatar
                             :shape "circle"})]

                [:div {:style {:margin-left 12
                               :margin-top -4}}
                 [:div.row1
                  (if (:name user)
                    [:span {:style {:font-weight "600"
                                    :color "rgba(0,0,0,0.64)"}}
                     (:name user)])
                  [:a {:href (str "/@" (:screen_name user))
                       :color "#666"}
                   [:span {:style (if (:name user)
                                    {:margin-left 6
                                     :font-size 15
                                     :color "#666"}
                                    {:font-weight "600"
                                     :color "rgba(0,0,0,0.64)"
                                     :font-size 18})}
                    (if (:name user) " @" "@") (:screen_name user)]]]
                 [:div.row {:style {:color "#666"
                                    :margin-top 6
                                    :max-height 60
                                    :overflow "hidden"
                                    :font-size 15}}
                  (:bio user)]]]

               [:div.space-between.ubuntu {:style {:padding "0 3px"}}
                [:p.number {:style {:font-size 14
                                    :margin-bottom 6}}
                 (util/date-format (:created_at post))]
                (if (= (:id current-user) (:id user))
                  [:a.control {:on-click (fn [e]
                                           (util/set-href! (str config/website "/p/" (:id post) "/edit")))
                               :style {:font-size 14}}
                   (t :edit)]
                  (if @raw?
                    [:a.control {:on-click (fn [e]
                                             (reset! raw? false))
                                 :style {:font-size 14
                                         :color "#000"}}
                     (t :back)]
                    [:a.control {:on-click (fn [e]
                                             (reset! raw? true))
                                 :style {:font-size 14}}
                     "raw"]))]

               [:h1.system-font-stack {:style {:font-weight "600"
                                               :margin-top "0.5em"}}

                (if (:choices post)
                  [:span
                   (str "[" (str/lower-case (t :poll)) "] "
                        (:title post))]
                  (:title post))]]

              [:div.post
               (if @raw?
                 [:div.fadein
                  (widgets/transform-content (str "....\n"
                                                  (:body post)
                                                  "\n....")
                                             {:body-format :asciidoc
                                              :style {:margin "24px 0"}})]
                 (widgets/transform-content (:body post)
                                           {:body-format (:body_format post)
                                            :style {:overflow "hidden"}
                                            :on-mouse-up (fn [e]
                                                           (let [text (util/get-selection-text)]
                                                             (when-not (str/blank? text)
                                                               (citrus/dispatch! :comment/set-selection
                                                                                 {:screen_name (:screen_name user)})))

                                                           )}))]

              [:div.center-area
               [:div {:style {:margin "24px 0"}}
                (choices-cp post {:align-items "center"})]

               (tags (:tags post)
                     {:style {:display "block"
                              :margin "24px 0"}}
                     nil)

               ;; [:p.number {:style {:font-size 14}}
               ;;  [:span (t :updated-at) ": "]
               ;;  (util/date-format (:updated_at post))]

               (toolbox post)

               (quote-selection current-user)]

              [:div {:style {:margin-top 24}}
               (comment/comment-list post)]]
             ])

          [:div.row {:style {:justify-content "center"}}
           (ui/donut)]
          )))))

(rum/defc sort-by-new < rum/reactive
  (mixins/query :newest)
  []
  [:div.column {:style {:padding-bottom 48}}

   (widgets/cover-nav nil)

   (let [posts (citrus/react [:posts :latest])]
     (query/query
       (post-list posts
                  {:merge-path [:posts :latest]}
                  :show-group? true)))])

(rum/defc sort-by-latest-reply < rum/reactive
  (mixins/query :latest-reply)
  []
  [:div.column {:style {:padding-bottom 48}}
   (widgets/cover-nav nil)
   (let [posts (citrus/react [:posts :latest-reply])]
     (query/query
       (post-list posts
                  {:merge-path [:posts :latest-reply]}
                  :show-group? true)))])

(rum/defc tag-posts < rum/reactive
  (mixins/query :tag)
  [{:keys [tag]
    :as params}]
  (let [path [:posts :by-tag tag]
        posts (citrus/react path)]
    [:div.column.auto-padding.center-area {:style {:margin-bottom 48}}

     [:h1 (str "Tag: " (util/tag-decode tag))]

     (query/query
       (post-list posts {:tag tag
                         :merge-path path}
                  :show-avatar? true))]))

(rum/defc user-tag-posts < rum/reactive
  (mixins/query :user-tag)
  [{:keys [screen_name tag]
    :as params}]
  (let [idx {:screen_name screen_name
             :tag tag}
        path [:posts :by-user-tag idx]
        user (citrus/react [:user :by-screen-name screen_name])
        posts (citrus/react path)]
    (if user
      [:div.column.auto-padding.center-area {:class "user-posts"
                                             :style {:margin-bottom 48}}

       (widgets/user-card user)

       (widgets/posts-comments-header screen_name)

       [:div
        (widgets/tags screen_name (:tags user) tag)

        (query/query
          (post-list posts {:user-tag idx
                            :merge-path path}
                     :show-avatar? false))]]
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])))
