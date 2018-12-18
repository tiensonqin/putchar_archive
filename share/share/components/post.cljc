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
            [share.front-matter :as fm]
            #?(:cljs [appkit.storage :as storage])
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
        post-or-comment? (contains? #{:post :comment} (citrus/react [:router :handler]))
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
      (ui/icon {:width (if post-or-comment? 24 18)
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
                    :color colors/icon-color})]))
     [:input
      {:id "photo_upload"
       :multiple true
       :accept "image/*"
       :type "file"
       :on-change (fn [e]
                    #?(:cljs
                       (post-box/upload-images (.-files (.-target e)))))
       :hidden true}]]))

(rum/defc title-exists < rum/reactive
  []
  (let [post-title-exists? (citrus/react [:post :post-title-exists?])]
    (if post-title-exists?
      [:p.row1 {:style {:position "absolute"
                        :top -32
                        :right 0}}
       (ui/icon {:type :warning})
       [:span {:style {:margin-left 6}}
        "Article title already exists."]])))

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
  (let [value (or (:body form-data) init
                  "---
title:
tags:
lang: en
published: false
---")
        latest-height (citrus/react [:post :latest-height])
        mobile? (util/mobile?)
        body-format (or (citrus/react [:post :form-data :body_format])
                        body-format
                        :markdown)]
    [:div.row
     (when-not (and mobile? (:preview? form-data))
       [:div.column {:style {:position "relative"}}
        (title-exists)
        [:div.editor.row {:style {:min-height 800}}
        (post-box/post-box
         :post
         nil
         {:other-attrs {:autoFocus auto-focus?}
          :placeholder (t :post-body-placeholder)
          :style {:border "none"
                  :background-color "transparent"
                  :font-size 18
                  :resize "none"
                  :width "100%"
                  :line-height "1.7"
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
                                              {:body (util/ev e)
                                               :body_format (keyword body-format)}))
          :value value})]])

     (when (and (not mobile?) (:preview? form-data))
       [:div.ver_divider {:style {:margin "0 12px"}}])

     (when (or (and (not mobile?) (:preview? form-data))
               (and mobile? (:preview? form-data)))
       [:div.row {:style {:margin-top 12}}
        (comment/post-preview (fm/remove-front-matter
                               (or (:body form-data) init))
                              body-format
                              {:font-size "18px"})])]))

(rum/defc publish-button < rum/reactive
  [form-data on-publish]
  (let [loading? (citrus/react [:post :loading?])]
    (let [ok? (and
               (not (str/blank? (:body form-data)))
               (>= (count (:body form-data)) 128)
               (let [title (fm/extract-title (:body form-data))]
                 (and title
                      (not (str/blank? title))
                      (<= 4 (count title) 128))))]
      (ui/button {:class (if ok? "btn-primary" "disabled")
                  :style (if ok? {:background colors/primary})
                  :on-click (fn []
                              (when ok?
                                (on-publish)))}
        (if loading?
          (ui/donut-white)
          (t :publish))))))

(rum/defcs publish-to < rum/reactive
  [state]
  (let [form-data (citrus/react [:post :form-data])
        current-user (citrus/react [:user :current])
        current-post (citrus/react [:post :current])
        submit-fn (fn []
                    (let [body-format (if (nil? (:body_format form-data))
                                        :markdown
                                        (:body_format form-data))
                          data {:id (:id current-post)
                                :is_draft false
                                :body_format body-format
                                :body (str/replace-first (:body form-data)
                                                         "published: false"
                                                         "published: true")}]
                      (citrus/dispatch! :post/update data)))]
    [:div {:style {:display "flex"
                   :flex-direction "row"
                   :flex "0 1 1"
                   :align-items "center"}}

     (edit-toolbox)

     (when current-user
       (publish-button form-data submit-fn))]))

(rum/defc new < rum/reactive
  {:will-mount (fn [state]
                 #?(:cljs
                    (citrus/dispatch! :citrus/new-draft))
                 state)}
  []
  (let [form-data (citrus/react [:post :form-data])
        width (citrus/react [:layout :current :width])
        preview? (:preview? form-data)]
    [:div.column {:class "editor"
                  :style {:max-width (if (and preview? (> width 1024))
                                       1160
                                       768)
                          :margin "0 auto"}}
     [:div.auto-padding {:style {:flex 1
                                 :overflow "hidden"
                                 :margin-top 24}}
      (new-post-body form-data nil nil false)]]))

(defn link-fields
  [default-post-language]
  {:link  {:label (str (t :link) ": *")
           :validators [util/link?]
           :auto-focus true
           :on-blur (fn [form-data link]
                      (citrus/dispatch! :post/opengraph-query link
                                        (fn [result]
                                          (when (str/blank? (:title @form-data))
                                              (swap! form-data merge
                                                     {:title (:title result)
                                                      :cover (:image result)
                                                      :tags (str/join ", " (:tags result))})))))}
   :title {:label (str (t :title) ": *")
           :validators [util/non-blank? (util/length? {:min 8
                                                       :max 128})]
           :reactive? true}
   :tags  {:label (str (t :tags) ":")
           :placeholder (t :add-tags)}
   :lang  {:label (str (t :select-primary-language) ":")
           :type :select
           :options dicts/langs
           :select-opts {:style {:width 100}}
           :default default-post-language
           :class "column1"}})

(rum/defc new-link < rum/reactive
  []
  (let [width (citrus/react [:layout :current :width])
        title-exists? (citrus/react [:post :post-title-exists?])
        link-exists? (citrus/react [:post :post-link-exists?])
        permalink-exists? (citrus/react [:post :post-permalink-exists?])
        default-post-language (citrus/react [:user :default-post-language])]

    [:div.auto-padding.column.center {:style {:margin-top 24}}
     (form/render {:title (t :submit-a-link)
                   :fields (link-fields default-post-language)
                   :on-submit (fn [form-data]
                                (let [form-data (do
                                                  (swap! form-data update :title util/capitalize-first-char)
                                                  form-data)]
                                  (citrus/dispatch! :post/new-link form-data)))
                   :loading? [:post :loading?]
                   :footer (fn [form-data]
                             [:div
                              (cond
                                permalink-exists?
                                [:p {:class "help is-danger"}
                                 "Permalink already exists!"]

                                title-exists?
                                [:p {:class "help is-danger"}
                                 "Title already exists!"]

                                link-exists?
                                [:p {:class "help is-danger"}
                                 "Link already exists!"]

                                :else
                                nil)])
                   :style {:width (min (- width 48) 600)}})]))

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
                {:class "btn-primary"
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
     (when (and self? (not (:link post)))
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
  [tags opts]
  (if (seq tags)
    [:span opts
     (for [tag tags]
       (when-not (str/blank? tag)
         [:a
          {:key (util/random-uuid)
           :href (str "/tag/" (name tag))
           :on-click (fn [e] (util/stop e))
           :style {:margin-right 12
                   :white-space "nowrap"
                   :color colors/primary}}
          (str "#" (util/tag-decode tag))]))]))

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
            user-draft? (contains? #{:user :drafts :links} current-path)
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
                        (util/tag-decode tag)
                        nil)]
        [:div.post-item.col-item {:style {:position "relative"}
                                  :on-click (fn [e]
                                              (citrus/dispatch! :router/push router true))}
         (if user-draft?
           [:span
            [:span {:style {:margin-right 12}}
             (util/date-format (:created_at post))]

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
               (:title post)])

            (when (and self? drafts-path?)
              (ui/menu
                [:a {:style {:font-size 14
                             :position "absolute"
                             :right (if mobile? 12 0)}
                     :on-click (fn [e]
                                 (util/stop e))}
                 (ui/icon {:type :more
                           :color "#999"
                           :width 20
                           :height 20})]
                [[:a.button-text {:style {:font-size 14}
                                  :on-click (fn [e]
                                              (util/stop e)
                                              (citrus/dispatch! :post/open-delete-dialog? post))}
                  (t :delete)]]
                {:other-attrs {:trigger ["click"]}}))]

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
                  (:title post)])

               [:a.control {:href (str post-link "#comments")
                            :title (str (:comments_count post)
                                        " "
                                        (t :replies))
                            :on-click util/stop
                            :style {:margin-left 24}}
                [:span.number {:style {:font-weight "600"
                                       :font-size 18}}
                 (:comments_count post)]]]]

             [:div.space-between {:style {:align-items "center"
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
                  (str "#" first-tag)])

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
        [:div.spinner]])
     (ops-delete-dialog)]))

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
        posts (if (= current-filter :latest)
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
      [:div.empty-posts.auto-padding
       (if empty-widget
         empty-widget
         [:a {:href "/new-article"
              :style {:margin-top 24
                      :color colors/primary}}
          [:span {:style {:margin-top 3
                          :font-size 18}}
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
  {:will-mount (fn [state]
                 #?(:cljs
                    (let [emojis (storage/get :emojis)]
                      (when (nil? emojis)
                        (citrus/dispatch! :data/pull-emojis))))
                 state)
   :will-unmount (fn [state]
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
      [:div.column.center-area.auto-padding {:class "post-edit editor"
                                             :style (if (and preview? (> width 1024))
                                                      {:max-width 1160
                                                       :margin-top 24}
                                                      {:margin-top 24})}
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
        link (:link post)
        self? (= (:screen_name current-user)
                 (get-in post [:user :screen_name]))]
    [:div.row {:id "toolbox"
               :style {:padding 0
                       :align-items "center"
                       :margin-bottom (if link 24 48)
                       :margin-top (if link 24 64)}}

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
  {:init
   (fn [state props]
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
  [state {:keys [screen_name permalink post-id] :as params}]
  (let [current-user (citrus/react [:user :current])
        mobile? (util/mobile?)]
    (query/query
      (let [post (cond
                   (and screen_name permalink)
                   (let [permalink (util/encode-permalink (str "@" screen_name "/" permalink))]
                     (citrus/react [:post :by-permalink permalink]))
                   post-id
                   (citrus/react [:post :by-id post-id])
                   :else nil)]
        (if post
          (let [{:keys [user link]} post
                current-reply (citrus/react [:comment :reply])
                avatar (util/cdn-image (:screen_name user))]
            [:div.column.center-area {:key "post"}
             [:div.auto-padding {:style {:margin-top 24}}
              (when (seq (:tags post))
                [:div.row {:style {:margin-bottom 24}}
                 (tags (:tags post) nil)])

              [:div.column1 {:style (if (or mobile? link)
                                      {}
                                      {:align-items "center"
                                       :justify-content "center"
                                       :margin-top 48})}
               (if link
                 [:span {:style {:margin-bottom 6}}
                  [:a {:href link
                       :style {:color colors/primary
                               :font-size "1.4em"}}
                   (util/capitalize-first-char (:title post))]
                  [:a {:href link
                       :style {:color colors/primary}}
                   [:span {:style {:color "#999"
                                   :margin-left 6
                                   :font-size 14}}
                    (str "("
                         (util/get-domain link)
                         ")")]]]
                 (if (:title post)
                   [:h1.post-page-title
                    (util/capitalize-first-char (:title post))]))

               [:div#post-user {:style {:font-style "italic"
                                        :font-size (if link 15 "1.1em")}}
                [:a {:href (str "/@" (:screen_name user))
                     :style {:color colors/primary}}
                 (if (:name user)
                   (:name user)
                   (str "@" (:screen_name user)))]

                [:span {:style {:margin-left 12
                                :color colors/primary}}
                 (util/date-format (:created_at post))]

                (if (and
                     (not link)
                     (or
                      (= (:id current-user) (:id user))
                      (admins/admin? (:screen_name current-user))))
                  [:a {:style {:color colors/primary
                               :margin-left 12}
                       :on-click (fn [e]
                                   (util/set-href! (str config/website "/p/" (:id post) "/edit")))}
                   (t :edit)])]

               (when-not link
                 [:div.divider])

               ;; (when (and (:link post) (:cover post))
               ;;   [:div.editor
               ;;    [:img {:src (:cover post)}]])
               ]
              [:div.post
               (if (:body_html post)
                 (widgets/raw-html {:on-mouse-up (fn [e]
                                                   (let [text (util/get-selection-text)]
                                                     (when-not (str/blank? text)
                                                       (citrus/dispatch! :comment/set-selection
                                                                         {:screen_name (:screen_name user)}))))
                                    :class (str "editor " (name (:body_format post)))
                                    :style {:word-wrap "break-word"
                                            :font-size "1.127em"}
                                    :id "post-body"}
                                   (:body_html post))
                 (if (:link post)
                   (let [s (content/embed-youtube (:link post))]
                     (when (not= s (:link post))
                       (widgets/raw-html {:style {:margin-top 24}} s)))))]

              [:div.center-area
               (toolbox post)

               (quote-selection current-user)]

              (read-post post)]

             [:div#comments
              (comment/comment-list post)]])

          [:div.row {:style {:justify-content "center"}}
           (ui/donut)])))))

(rum/defc latest-posts < rum/reactive
  (mixins/query :latest)
  []
  [:div.column {:style {:padding-bottom 48}}

   (let [posts (citrus/react [:posts :latest])]
     (query/query
       (post-list posts
                  {:merge-path [:posts :latest]})))])

(rum/defc hot-posts < rum/reactive
  (mixins/query :hot)
  []
  [:div.column {:style {:padding-bottom 48}}

   (let [posts (citrus/react [:posts :hot])]
     (query/query
       (post-list posts
                  {:merge-path [:posts :hot]})))])

(rum/defc tag-posts < rum/reactive
  (mixins/query :tag)
  [{:keys [tag]
    :as params}]
  (let [path [:posts :by-tag tag]
        posts (citrus/react path)
        current-user (citrus/react [:user :current])
        followed? (and current-user
                       (contains? (set (:followed_tags current-user))
                                  tag))]
    [:div.column.center-area {:style {:margin-bottom 48}}
     [:div.space-between.auto-padding {:style {:align-items "center"
                                               :margin-top 24}}
      [:h1 {:style {:margin 0}}
       (str "#" (util/tag-decode tag))]
      [:div.row1 {:style {:align-items "center"}}
       [:a {:href (str "/tag/" tag "/latest.rss")
            :target "_blank"
            :style {:margin-right 12}}
        (ui/icon {:type :rss
                  :color "#666"
                  :width 20
                  :height 20})]

       (widgets/follow-tag followed? tag)]]

     [:div.divider]

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
