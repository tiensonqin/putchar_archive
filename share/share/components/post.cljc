(ns share.components.post
  (:require [rum.core :as rum]
            [appkit.citrus :as citrus]
            [share.components.comment :as comment]
            [share.content :as content]
            [clojure.string :as str]
            [bidi.bidi :as bidi]
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [goog.object :as gobj])
            [share.kit.mixins :as mixins]
            [share.kit.ui :as ui]
            [share.kit.query :as query]
            [share.kit.colors :as colors]
            [share.helpers.form :as form]
            [share.dommy :as dommy]
            [share.util :as util]
            [share.dicts :refer [t] :as dicts]
            [appkit.macros :refer [oget]]
            [share.config :as config]
            [share.components.widgets :as widgets]
            [share.components.post-box :as post-box]
            [share.kit.infinite-list :as inf]
            [share.admins :as admins]
            #?(:cljs [web.scroll :as scroll])))

(rum/defcs vote < rum/reactive
  (rum/local 0 ::init-tops)
  [state post]
  (let [theme (citrus/react [:theme])
        init-tops (get state ::init-tops)
        toped-posts (set (citrus/react [:post :toped]))
        toped? (and (set? toped-posts)
                    (toped-posts (:id post)))
        tops (-> (if (:tops post) (:tops post) 0)
                 (+ @init-tops))
        post? (= :post (citrus/react [:router :handler]))
        title (if toped? (t :unvote) (t :vote))
        on-click (fn [e]
                   (util/stop e)
                   (citrus/dispatch! (if toped? :post/untop :post/top) (:id post))
                   (swap! init-tops (if toped? dec inc)))
        hide-votes? (citrus/react [:hide-votes?])]
    [:div.row1
     [:a.scale.row1 {:title title
                     :on-click on-click
                     :style {:align-items "center"}}
      (ui/icon {:width (if post? 24 18)
                :type :thumb_up
                :color (if toped? colors/primary "rgb(127,127,127)")
                :opts {:style {:margin-top -2}}})]
     (when-not hide-votes?
       [:span.number {:style {:margin-left 6
                              :font-weight "500"
                              :color "rgb(127,127,127)"}}
        tops])]))

(rum/defc edit-toolbox < rum/reactive
  []
  (let [form-data (citrus/react [:post :form-data])
        images (:images form-data)
        mobile? (util/mobile?)
        margin (if mobile? 12 24)
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
                    :color colors/shadow})]))
     [:input
      {:id "photo_upload"
       :multiple true
       :accept "image/*"
       :type "file"
       :on-change (fn [e]
                    #?(:cljs
                       (post-box/upload-images (.-files (.-target e)))))
       :hidden true}]]))

(rum/defcs new-post-title < rum/reactive
  [state form-data init auto-focus?]
  (let [title-exists? (citrus/react [:post :post-title-exists?])]
    [:div.new-post-title
     [:input.ubuntu {:type "text"
                     :class "header-text"
                     :autoComplete "off"
                     :autoFocus auto-focus?
                     :on-focus util/set-cursor-end
                     :name "title"
                     :placeholder (t :title)
                     :style {:border "none"
                             :background-color "transparent"
                             :font-size "2em"
                             :font-weight "600"
                             :padding-left 0
                             :width "100%"
                             :padding-right 36
                             :color "#222"}
                     :on-change (fn [e]
                                  (citrus/dispatch! :citrus/set-post-form-data
                                                    {:title-validated? true
                                                     :title (util/ev e)}))
                     :value (or (:title form-data) init "")}]

     (if (false? (get form-data :title-validated?))
       [:p {:class "help is-danger"} (t :post-title-warning)])

     (if title-exists?
       [:p {:class "help is-danger"} (str (:title form-data)
                                          " already exists!")])]))

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
                  :font-size "18px"
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
                              body-format
                              {:font-size "18px"})])]))

(rum/defc select-language
  [default form-data]
  (let [button-cp (fn [lang value]
                    (ui/button {:class (str "btn-sm "
                                            (if (= value (:lang form-data))
                                              "btn-primary"))
                                :style {:margin-right 12
                                        :margin-bottom 12}
                                :on-click (fn []
                                            (citrus/dispatch! :citrus/set-post-form-data
                                                              {:lang value}))}
                      lang))]
    [:div#select-language {:style {:margin "12px 0"}}
     [:h6
      (str (t :select-primary-language) ":")]

     [:p {:style {:margin-bottom "1em"
                  :font-size 14
                  :color "rgb(127,127,127)"}}
      (t :select-primary-language-explain)]

     (when (and (contains? (set (keys form-data)) :lang)
                (nil? (:lang form-data)))
       [:p.help (t :language-must-be-chosen)])

     [:div.row1 {:style {:flex-wrap "wrap"}}
      (for [[value text] dicts/langs]
        (button-cp text value))]

     (cond
       (and (:lang form-data) (not default))
       (ui/button {:class "btn-primary"
                   :on-click (fn []
                               (citrus/dispatch!
                                :user/set-default-post-language
                                (:lang form-data)))}
         (util/format (t :language-default-choice) (get dicts/langs (:lang form-data))))

       (and default (:lang form-data) (not= default (:lang form-data)))
       [:p {:style {:font-size 15
                    :color colors/shadow}}
        (get dicts/langs default)
        (t :is-my-default-language-choice)
        [:a {:on-click (fn []
                         (citrus/dispatch!
                          :user/set-default-post-language
                          (:lang form-data)))}
         (str (t :change-it-to) (get dicts/langs (:lang form-data)))]
        "."])]))

(rum/defc add-tags
  [form-data style auto-focus?]
  [:div#add-tags {:style style}
   [:span {:style {:margin-right 12
                   :color colors/shadow
                   :font-weight "600"}}
    (str (t :tags) ) ":"]
   [:div
    (ui/input {:class "ant-input ubuntu"
               :type "text"
               :autoComplete "off"
               :auto-focus auto-focus?
               :name "tags"
               :style {:border "none"
                       :border-bottom "1px solid #aaa"
                       :border-radius 0
                       :padding 0
                       :color colors/primary
                       :background "#FFF"
                       :font-size 15}
               :placeholder (t :add-tags)
               :default-value (or (:tags form-data) "")
               :on-change (fn [value]
                            #?(:cljs
                               (let [value (util/ev value)]
                                 (when-not (str/blank? value)
                                   (citrus/dispatch! :citrus/set-post-form-data
                                                     {:tags value})))))})]])

(rum/defc assoc-book < rum/reactive
  [form-data style current-book]
  (let [result (citrus/react [:search :result :books])]
    (when (and current-book
               (nil? (:book_title form-data)))
      (citrus/dispatch! :citrus/set-post-form-data
                        {:book_id (:id current-book)
                         :book_title (:title current-book)}))
    [:div#assoc-book {:style style}
     [:span {:style {:margin-right 12
                     :color colors/shadow
                     :font-weight "600"
                     :width 50}}
      "Related Book(optional): "]
     [:div.column1 {:style {:position "relative"}}
      (ui/input
       {:class "ant-input ubuntu"
        :type "text"
        :autoComplete "off"
        :name "book_id"
        :style {:border "none"
                :border-bottom "1px solid #aaa"
                :border-radius 0
                :padding 0
                :color colors/primary
                :background "#FFF"
                :font-size 15}
        :value (or (:book_title form-data) (:title current-book) "")
        :on-change (fn [value]
                     #?(:cljs
                        (when-let [value (util/ev value)]
                          (citrus/dispatch-sync! :citrus/set-post-form-data
                                                 {:book_title value})
                          (when-not (str/blank? value)
                              (citrus/dispatch! :search/search
                                             :search/search
                                             {:q {:book_title value}}
                                             :books)))))})
      (let [on-select (fn [book]
                        (when-let [id (:book_id book)]
                          (citrus/dispatch! :citrus/set-post-form-data
                                            {:book_id (util/parse-int id)
                                             :book_title (:book_title book)})
                          (citrus/dispatch! :search/reset)))]
        (when (seq result)
          (widgets/autocomplete
           result
           (fn [book]
             (:book_title book))
           [:div {:style {:position "absolute"
                          :bottom 0
                          :left 0}}]
           on-select
           {:item-style {:justify-content "flex-start"}})))]]))

(rum/defc assoc-paper < rum/reactive
  [form-data style current-paper]
  (let [result (citrus/react [:search :result :papers])]
    (when (and current-paper
               (nil? (:paper_title form-data)))
      (citrus/dispatch! :citrus/set-post-form-data
                        {:paper_id (:id current-paper)
                         :paper_title (:title current-paper)}))
    [:div#assoc-paper {:style style}
     [:span {:style {:margin-right 12
                     :color colors/shadow
                     :font-weight "600"
                     :width 50}}
      "Related Paper(optional): "]
     [:div.column1 {:style {:position "relative"}}
      (ui/input
       {:class "ant-input ubuntu"
        :type "text"
        :autoComplete "off"
        :name "paper_id"
        :style {:border "none"
                :border-bottom "1px solid #aaa"
                :border-radius 0
                :padding 0
                :color colors/primary
                :background "#FFF"
                :font-size 15}
        :value (or (:paper_title form-data) (:title current-paper) "")
        :on-change (fn [value]
                     #?(:cljs
                        (when-let [value (util/ev value)]
                          (citrus/dispatch-sync! :citrus/set-post-form-data
                                                 {:paper_title value})
                          (when-not (str/blank? value)
                              (citrus/dispatch! :search/search
                                             :search/search
                                             {:q {:paper_title value}}
                                             :papers)))))})
      (let [on-select (fn [paper]
                        (when-let [id (:paper_id paper)]
                          (citrus/dispatch! :citrus/set-post-form-data
                                            {:paper_id (util/parse-int id)
                                             :paper_title (:paper_title paper)})
                          (citrus/dispatch! :search/reset)))]
        (when (seq result)
          (widgets/autocomplete
           result
           (fn [paper]
             (:paper_title paper))
           [:div {:style {:position "absolute"
                          :bottom 0
                          :left 0}}]
           on-select
           {:item-style {:justify-content "flex-start"}})))]]))

(rum/defcs publish-dialog < rum/reactive
  (rum/local false ::language-select-update?)
  [state form-data current-book current-paper]
  (let [language-select-update? (get state ::language-select-update?)
        images (:images form-data)
        images? (seq images)
        default-post-language (citrus/react [:user :default-post-language])]
    [:div.column.ubuntu#publish-dialog
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

     (add-tags form-data {:padding "12px 0"} true)
     (when (not (:paper_title form-data))
         (assoc-book form-data {:padding "12px 0"} current-book))
     (when (not (:book_title form-data))
         (assoc-paper form-data {:padding "12px 0"} current-paper))

     (if (and default-post-language
              (not @language-select-update?))
       [:p {:style {:margin-top 12
                    :margin-bottom 0
                    :font-size 15
                    :color colors/shadow}}
        (str (t :post-primary-language) ": ") (get dicts/langs default-post-language)
        [:a {:style {:margin-left 24
                     :font-size 15}
             :on-click (fn []
                         (reset! language-select-update? true))}
         (t :change)]]
       (select-language default-post-language form-data))]))

(rum/defc publish-button < rum/reactive
  [form-data]
  (let [loading? (citrus/react [:post :loading?])]
    (let [ok? (and (util/post-title? (:title form-data))
                   (not (str/blank? (:body form-data))))]
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
  [state]
  (let [form-data (citrus/react [:post :form-data])
        modal? (citrus/react [:post :publish-modal?])
        current-user (citrus/react [:user :current])
        current-post (citrus/react [:post :current])
        current-book (citrus/react [:book :current])
        current-paper (citrus/react [:paper :current])
        submit-fn (fn []
                    (prn "submit form-data: " form-data)
                    (let [data (cond->
                                   (merge {:id (:id current-post)
                                           :is_draft false}
                                          (select-keys form-data
                                                       [:title :body :body_format :lang :tags :book_id :book_title :paper_id :paper_title]))


                                 (:cover form-data)
                                 (assoc :cover (:cover form-data)))
                          data (if (nil? (:body_format data))
                                 (assoc data :body_format :markdown)
                                 data)
                          data (util/map-remove-nil? data)]
                      (citrus/dispatch! :post/update data)
                      (citrus/dispatch!
                       :citrus/default-update
                       [:post :publish-modal?]
                       false)))]

    (let [tags (:tags current-post)
          data (cond-> {}
                 (and tags (nil? (:tags form-data)))
                 (assoc :tags (str/join "," tags))
                 (and current-book (nil? (:book_id form-data)))
                 (assoc :book_id (:id current-book)
                        :book_title (:book_title current-book))
                 (and current-paper (nil? (:paper_id form-data)))
                 (assoc :paper_id (:id current-paper)
                        :paper_title (:paper_title current-paper)))]
      (when (seq data)
        (citrus/dispatch-sync! :citrus/set-post-form-data
                               data)))

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
         :style {:width (min 600 (- (:width (util/get-layout)) 48))
                 ;; :height (- (:height (util/get-layout)) 100)
                 :overflow-y "auto"}
         :animation "zoom"
         :maskAnimation "fade"
         :footer
         (form/submit submit-fn
                      {:submit-text (t :publish)
                       :cancel-button? false
                       :confirm-attrs {:style {:width "100%"}}})}
        (publish-dialog form-data current-book current-paper)))]))

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
                                 :overflow "hidden"
                                 :margin-top 48}}

      (new-post-title form-data nil true)

      (new-post-body form-data nil nil false)]]))

(rum/defc ops-twitter
  [post zh-cn?]
  (when-not zh-cn?
    (let [url (str "https://twitter.com/share?url="
                   (bidi/url-encode #?(:cljs js/location.href
                                       :clj (util/post-link post)))
                   "&text="
                   (bidi/url-encode (:title post)))]
      [:a {:title (t :tweet)
           :href url
           :target "_blank"
           :style {:margin-right 24}}
       (ui/icon {:type :twitter
                 :width 20
                 :height 20
                 :color "#1DA1F3"})])))

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
             :color colors/shadow})])

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
    [:div
     [:a {:style {:margin-left 12}
          :href (str "/" (:permalink post))
          :on-click (fn [e]
                      (util/stop e))}
      (:title post)]])))

(rum/defc ops-menu
  [post self?]
  (ui/menu
    [:a {:style {:margin-top 2
                 :margin-right 12}
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
                                    (util/stop e)
                                    (util/set-href! (str config/website "/p/" (:id post) "/edit")))}
        (t :edit)])

     ;; delete
     (when self?
       [:a.button-text {:style {:font-size 14}
                        :on-click (fn [e]
                                    (util/stop e)
                                    (citrus/dispatch! :post/open-delete-dialog? post))}
        (t :delete)])

     ;; report
     (when (not self?)
       (ops-flag post))]
    {:menu-style {:width 200}
     :other-attrs {:trigger ["click"]}}))

(rum/defc tags
  [tags opts tag-style]
  (if (seq tags)
    [:span.ubuntu opts
     (for [tag tags]
       (when-not (str/blank? tag)
         [:a.tag
          {:key (util/random-uuid)
           :href (str "/tag/" (name tag))
           :style (merge {:margin-right 12
                          :white-space "nowrap"}
                         tag-style)}
          (util/tag-decode (name tag))]))]))

(rum/defcs post-item < {:key-fn (fn [post]
                                  (:id post))}
  rum/static
  rum/reactive
  [state post show-avatar? opts]
  (if post
    (let [user (:user post)]
      (let [current-path (citrus/react [:router :handler])
            width (citrus/react [:layout :current :width])
            mobile? (or (util/mobile?) (<= width 768))
            current-user (citrus/react [:user :current])
            current-user-id (:id current-user)
            user-id (or (:id user) (:user_id post))
            self? (and current-user-id (= user-id current-user-id))
            user-link (str "/@" (:screen_name user))
            drafts-path? (= current-path :drafts)
            user-draft? (contains? #{:user :drafts} current-path)
            paper? (= current-path :paper)
            book? (= current-path :book)
            [post-link router] (if drafts-path?
                                 [(str "/p/" (:id post) "/edit")
                                  {:handler :post-edit
                                   :route-params {:post-id (str (:id post))}}]
                                 [(str "/" (:permalink post))
                                  {:handler :post
                                   :route-params (util/decode-permalink (:permalink post))}])

            {:keys [last_reply_at created_at]} post
            self? (and current-user self?)
            first-tag (if-let [tag (first (:tags post))]
                        (str/capitalize tag)
                        nil)]
        [:div.post-item.col-item {:style {:position "relative"}
                                  :on-click (fn [e]
                                              (citrus/dispatch! :router/push router true))}
         (cond
           (and (not book?)
                (:book_id post)
                (:book_title post))
           [:a {:href (str "/book/" (:book_id post))
                :on-click util/stop
                :style {:margin-right 12
                        :font-size 14
                        :margin-bottom 6
                        :color colors/primary
                        :display "block"}}
            (:book_title post)]
           (and (not paper?)
                (:paper_id post)
                (:paper_title post))
           [:a {:href (str "/paper/" (:paper_id post))
                :on-click util/stop
                :style {:margin-right 12
                        :font-size 14
                        :margin-bottom 6
                        :color colors/primary
                        :display "block"}}
            (:paper_title post)])
         (if user-draft?
           [:div.space-between {:style {:flex-wrap "wrap"
                                        :align-items "center"}}
            (let [link (:link post)]
                 [:a.post-title.no-decoration (if link
                                                {:style {:margin-right 6}
                                                 :on-click (fn [e]
                                                             (.stopPropagation e))
                                                 :href link
                                                 :target "_blank"}

                                                {:style {:margin-right 6}
                                                 :on-click util/stop
                                                 :href post-link})
                  (:title post)
                  (if link
                    (ui/icon {:type :link
                              :width 16
                              :height 16
                              :color colors/shadow
                              :opts {:style {:margin-left 6
                                             :display "inline-block"}}}))])
            [:span {:style {:color colors/shadow
                            :font-size 13
                            :font-weight 400}}
             (util/date-format (:created_at post) "M/dd")]]
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
             [:div.column {:style {:justify-content "center"}}
              [:div.space-between
               (let [link (:link post)]
                 [:a.post-title.no-decoration (if link
                                                {:style {:margin-right 6}
                                                 :on-click (fn [e]
                                                             (.stopPropagation e))
                                                 :href link
                                                 :target "_blank"}

                                                {:style {:margin-right 6}
                                                 :on-click util/stop
                                                 :href post-link})
                  (:title post)
                  (if link
                    (ui/icon {:type :link
                              :width 16
                              :height 16
                              :color colors/shadow
                              :opts {:style {:margin-left 6
                                             :display "inline-block"}}}))])

               [:a.control {:href (str post-link "#comments")
                            :title (str (:comments_count post)
                                        " "
                                        (t :replies))
                            :on-click util/stop
                            :style {:margin-left 24}}
                [:span.number {:style {:font-weight "600"
                                       :font-size 18}}
                 (:comments_count post)]]]

              (if-let [cover (:cover post)]
                [:a {:href post-link
                     :on-click util/stop}
                 [:img.hover-shadow {:src (str cover "?w=" 200)
                                     :style {:max-width 200
                                             :border-radius 4
                                             :margin-top 8
                                             :margin-bottom 6}}]])]

             [:div.space-between.ubuntu {:style {:align-items "center"
                                                 :margin-top 8}}
              [:div.row1 {:style {:align-items "center"}}
               (vote post)]


              [:div.row1 {:style {:color "rgb(127,127,127)"
                                  :font-size 14}}
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
                               :on-click util/stop
                               :style {:margin-right 6}}
                           (ui/avatar {:class "ant-avatar-sm"
                                       :src (util/cdn-image poster)})]))])))

               (when first-tag
                 [:a.control {:href (str "/tag/" (first (:tags post)))
                              :style {:margin-right 12}
                              :on-click util/stop}
                  first-tag])

               [:a.no-decoration.control {:title (if last_reply_at
                                                   (str
                                                    (t :created-at) ": " (util/date-format created_at)
                                                    "\n"
                                                    (t :last-reply-at) ": " (util/date-format last_reply_at)
                                                    "\n"
                                                    "By: " (:last_reply_by post))
                                                   (str
                                                    (t :created-at) ": " (util/date-format created_at)))
                                          :on-click util/stop
                                          :href (if-let [last-reply-idx (:last_reply_idx post)]
                                                  (str post-link "/" last-reply-idx)
                                                  post-link)}
                (if last_reply_at
                  (util/time-ago (:last_reply_at post))
                  (util/time-ago created_at))]]]]])]))))


(rum/defc posts-stream < rum/reactive
  [posts show-avatar? end? opts loading?]
  (let [permalink-posts (citrus/react [:post :by-permalink])
        posts (mapv (fn [post]
                      (->> (select-keys (get permalink-posts (:permalink post)) [:tops :comments_count])
                           (util/map-remove-nil?)
                           (merge post)))
                    posts)]
    [:div.posts
     (inf/infinite-list (map (fn [post]
                               (post-item post show-avatar? opts)) posts)
                        {:on-load
                         (if end?
                           identity
                           (fn []
                             (citrus/dispatch! :citrus/load-more-posts
                                               opts)))})
     (when loading?
       [:div.center {:style {:margin "24px 0"}}
        [:div.spinner]])]))

(rum/defcs post-list < rum/static
  (rum/local nil ::last-post)
  rum/reactive
  {:after-render (fn [state]
                   #?(:cljs (when-let [anchors (dommy/sel ".post-item .editor a")]
                              (doseq [anchor anchors]
                                (dommy/listen! anchor :click
                                               (fn [e]
                                                 (.stopPropagation e))))))
                   state)}
  "Render a post list."
  [state {:keys [result end?]
          :as posts} opts & {:keys [empty-widget
                                    show-avatar?]
                             :or {show-avatar? true}}]
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
       end?
       (assoc opts :last last-post)
       scroll-loading?)
      [:div.empty-posts
       (if empty-widget
         empty-widget
         [:a {:href "/new-post"
              :style {:margin-top 24
                      :color colors/primary}}
          [:span.ubuntu {:style {:margin-top 3}}
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
               :empty-widget (widgets/empty-posts))))

(rum/defcs post-edit < rum/reactive
  (mixins/query :post-edit)
  (mixins/interval-mixin :post-auto-save
                         5000
                         (fn [] (citrus/dispatch! :post/save)))
  {:will-unmount (fn [state]
                   (citrus/dispatch! :post/reset-form-data)
                   state)}
  [state params]
  (let [form-data (citrus/react [:post :form-data])
        clear-interval? (citrus/react [:post :clear-interval?])
        width (citrus/react [:layout :current :width])
        preview? (:preview? form-data)]
    (when clear-interval?
      (when-let [interval (get state :post-auto-save)]
        (util/clear-interval interval)))
    (let [post (citrus/react [:post :current])]
      (when (and post (nil? (:title form-data)))
        (citrus/dispatch! :citrus/set-post-form-data
                          (cond->
                              (select-keys post [:title :body :body_format :lang :book_id :book_title :paper_id :paper_title])
                            (:body form-data)
                            (assoc :body (:body form-data)))))
      [:div.column.center-area.auto-padding {:class "post-edit editor"
                                             :style (if (and preview? (> width 1024))
                                                      {:max-width 1238
                                                       :margin-top 48}
                                                      {:margin-top 48})}
       (new-post-title form-data (or
                                  (:title form-data)
                                  (:title post)) (not (str/blank? (:title post))))

       (new-post-body form-data
                      (:body post)
                      (:body_format post)
                      (not (str/blank? (:body post))))])))

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
        scroll-top (citrus/react [:last-scroll-top (util/get-current-url)])
        self? (= (:screen_name current-user)
                 (get-in post [:user :screen_name]))]
    [:div.row {:id "toolbox"
               :style {:padding 0
                       :align-items "center"
                       :margin-bottom 48
                       :margin-top 64}}

     [:div.row {:style {:align-items "center"}}
      (vote post)]

     (if (util/mobile?)
       (ops-link post)
       (ops-twitter post zh-cn?))

     (ops-menu post self?)

     (ops-delete-dialog)]))

(rum/defc quote-selection < rum/reactive
  [current-user]
  (let [selection-mode? (citrus/react [:comment :selection :mode?])]
    (when (and current-user selection-mode?)
      (let [selection (util/get-selection)]
        (when-let [text (:text selection)]
          [:a#quote-selection.no-decoration.quote-selection-area
           {:style {:padding "6px 12px"
                    :border-radius 4
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

(rum/defc read-post < rum/reactive
  {:did-mount (fn [state]
                #?(:cljs
                   (when-let [post-body (dommy/sel1 "#post-body")]
                     (let [offset-top (oget post-body "offsetTop")
                           client-height (oget post-body "offsetHeight")]
                       (when (<= (+ client-height offset-top)
                                (gobj/get js/window "innerHeight"))
                         (citrus/dispatch! :post/read
                                           (first (:rum/args state)))))))
                state)
   :after-render (fn [state]
                   #?(:cljs
                      (let [post (first (:rum/args state))
                            read? @(citrus/subscription [:post :read-list (:id post)])]
                        (let [scroll-top (util/scroll-top)]
                          (when (nil? read?)
                            (when-let [post-body (dommy/sel1 "#post-body")]
                             (let [scroll-top (util/scroll-top)
                                   offset-top (oget post-body "offsetTop")]
                               (when (>= scroll-top offset-top)
                                 (citrus/dispatch! :post/read post)))))
                          state))
                      :clj state))
   }
  [post]
  (let [scroll-top (citrus/react [:last-scroll-top (util/get-current-url)])]
    [:div.read-post-placeholder {:style {:display "none"}}
     scroll-top]))

(rum/defcs post < rum/reactive
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
        current-user (citrus/react [:user :current])]
    (query/query
      (let [post (citrus/react [:post :by-permalink permalink])]
        (if post
          (let [{:keys [user]} post
                current-reply (citrus/react [:comment :reply])
                avatar (util/cdn-image (:screen_name user))]
            [:div.column.auto-padding.center-area {:key "post"}
             [:div {:style {:padding "12px 0"
                            :margin-top (cond
                                          (util/mobile?)
                                          0
                                          :else
                                          64)}}
              [:div {:style {:margin-bottom 64}}
               [:h1.post-page-title
                (:title post)]

               [:div#post-user {:style {:text-align "center"
                                        :font-style "italic"
                                        :font-size "1.1em"}}
                [:a {:href (str "/@" (:screen_name user))
                     :style {:color colors/primary}}
                 (if (:name user)
                   (:name user)
                   (str "@" (:screen_name user)))]

                [:span {:style {:margin-left 12
                                :color colors/primary}}
                 (util/date-format (:created_at post))]

                (if (or
                     (= (:id current-user) (:id user))
                     (admins/admin? (:screen_name current-user)))
                  [:a {:style {:color colors/primary
                               :margin-left 12}
                       :on-click (fn [e]
                                   (util/set-href! (str config/website "/p/" (:id post) "/edit")))}
                   (t :edit)])
]]

              (cond
                (and (:book_id post)
                     (:book_title post))
                [:a {:href (str "/book/" (:book_id post))
                     :style {:color colors/primary
                             :font-size 14
                             :display "block"
                             :margin-bottom 12}}
                 (:book_title post)]
                (and (:paper_id post)
                     (:paper_title post))
                [:a {:href (str "/paper/" (:paper_id post))
                     :style {:color colors/primary
                             :font-size 14
                             :display "block"
                             :margin-bottom 12}}
                 (:paper_title post)])

              [:div.post
               (when (:body post)
                 (widgets/raw-html {:on-mouse-up (fn [e]
                                                   (let [text (util/get-selection-text)]
                                                     (when-not (str/blank? text)
                                                       (citrus/dispatch! :comment/set-selection
                                                                         {:screen_name (:screen_name user)}))))
                                    :class (str "editor " (name (:body_format post)))
                                    :style {:word-wrap "break-word"
                                            :font-size "1.127em"}
                                    :id "post-body"}
                                   (:body post)))]

              [:div.center-area
               (when (seq (:tags post))
                 [:div.row1 {:style {:margin "24px 0"}}
                  (tags (:tags post)
                        nil
                        nil)])

               (toolbox post)

               (quote-selection current-user)]

              [:div#comments {:style {:margin-top 96}}
               (comment/comment-list post)]

              (read-post post)]])

          [:div.row {:style {:justify-content "center"}}
           (ui/donut)]
          )))))

(rum/defc sort-by-new < rum/reactive
  (mixins/query :newest)
  []
  [:div.column {:style {:padding-bottom 48}}

   (let [posts (citrus/react [:posts :latest])]
     (query/query
       (post-list posts
                  {:merge-path [:posts :latest]})))])

(rum/defc sort-by-latest-reply < rum/reactive
  (mixins/query :latest-reply)
  []
  [:div.column {:style {:padding-bottom 48}}
   (let [posts (citrus/react [:posts :latest-reply])]
     (query/query
       (post-list posts
                  {:merge-path [:posts :latest-reply]})))])

(rum/defc tag-posts < rum/reactive
  (mixins/query :tag)
  [{:keys [tag]
    :as params}]
  (let [path [:posts :by-tag tag]
        posts (citrus/react path)]
    [:div.column.center-area {:style {:margin-bottom 48}}

     [:h1.auto-padding (str "Tag: " (util/tag-decode tag))]

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
      [:div.column.center-area {:class "user-posts"
                                :style {:margin-bottom 48}}

       (widgets/user-card user)

       [:div
        (widgets/tags screen_name (:tags user) tag)

        (query/query
          (post-list posts {:user-tag idx
                            :merge-path path}
                     :show-avatar? false))]]
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])))
