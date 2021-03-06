(ns share.components.comment
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.util :as util]
            [share.components.report :as report]
            [share.components.widgets :as widgets]
            #?(:cljs [goog.dom :as gdom])
            [share.kit.colors :as colors]
            [share.dommy :as dommy]
            [share.kit.mixins :as mixins]
            [share.admins :as admins]
            [share.dicts :refer [t]]
            [clojure.string :as str]
            [share.dommy :as dommy]
            [share.components.post-box :as post-box]
            [share.kit.infinite-list :as inf]
            #?(:cljs [appkit.macros :refer [oget oset!]])))

(rum/defc preview < rum/reactive
  []
  (let [preview? (citrus/react [:comment :form-data :preview?])]
    [:a.no-tabindex {:title (if preview?
                              (t :back)
                              (t :preview))
                     :on-click (fn []
                                 (citrus/dispatch! :citrus/default-update
                                                   [:comment :form-data :preview?]
                                                   (if preview? false true)))}
     (ui/icon {:type "visibility"
               :color (if preview? colors/primary colors/shadow)
               :width 20})]))

(rum/defcs default-select-input <
  (rum/local nil ::ref)
  {:did-mount (fn [state]
                (when-let [ref @(get state ::ref)]
                  (.focus ref)
                  (.select ref))
                 state)}
  [state value]
  (let [ref (get state ::ref)]
    [:input.ant-input {:ref (fn [v] (reset! ref v))
                       :default-value value}]))

(rum/defc post-preview < rum/reactive
  (mixins/disable-others-tabindex ".post-preview a")
  [body body-format style]
  [:div.editor.post-preview.row {:style {:padding 2}}
   (widgets/transform-content body
                              {:body-format body-format
                               :style (merge {:overflow "hidden"}
                                             style)})])

;; refs k => ref
(rum/defcs comment-box < rum/reactive
  [state current-user entity [table fk] current-reply show? disabled?]
  (let [entity-id (:id entity)
        current-box-k (citrus/react [:comment :current-box-k])
        k {:table (if current-reply :replies table)
           :entity-id (if current-reply (:id current-reply) entity-id)}
        body (citrus/react [:comment :drafts k])
        loading? (citrus/react [:comment :loading?])
        preview? (citrus/react [:comment :form-data :preview?])]
    (when (and (not disabled?) (not= k current-box-k))
        (citrus/dispatch! :citrus/default-update
                          [:comment :current-box-k] k))
    [:div.comment-box#post-comment-box {:style (cond->
                                                 {:margin-bottom 48}
                                                 disabled?
                                                 (assoc :opacity 0.5))}
     (cond
       (nil? current-user)
       [:div.column1 {:style {:height 120
                              :border "1px solid #999"
                              :border-radius 4
                              :align-items "center"
                              :justify-content "center"}}
        [:a.control {:href "/login"}
         (t :login-to-comment)]]

       :else
       (if disabled?
         [:div {:style {:border "1px solid #999"
                        :border-radius 4
                        :font-size 16
                        :background "#fff"
                        :width "100%"
                        :padding 12
                        :min-height 100}}]
         [:div.column
         (post-box/post-box
          :comment
          k
          {:placeholder (if current-reply
                          (str "@" (get-in current-reply [:user :screen_name]) " ")
                          (t :your-thoughts-here))

           :min-rows 5

           :style {:border-radius 4
                   :border "1px solid #999"
                   :font-size "16px"
                   :line-height "1.5"
                   :background "transparent"
                   :resize "none"
                   :width "100%"
                   :padding 12
                   :white-space "pre-wrap"
                   :overflow-wrap "break-word"
                   :min-height 100}

           :other-attrs {:auto-focus (if current-reply true false)
                         :ref (fn [v]
                                (citrus/dispatch! :citrus/default-update
                                                  [:comment :refs k] v))}


           :value (or body "")
           :on-change (fn [e]
                        (let [v (util/ev e)]
                          (citrus/dispatch! :comment/save-local k v)))})
         (if preview?
           [:div.column
            [:div.divider {:style {:margin-bottom 12}}]
            (post-preview body :markdown {:font-size 18})])]))

     ;; submit button
     [:div.row {:style {:align-items "center"
                        :justify-content "flex-end"
                        :margin-top 12}}

      (preview)
      [:div.row1 {:style {:align-items "center"
                          :margin-left 24
                          :margin-right 2}}
       (let [on-submit (fn [e]
                         #?(:cljs
                            (let [data {:body (.trim body)
                                        fk entity-id}]
                              (citrus/dispatch! :comment/new [table fk] data)
                              (citrus/dispatch! :comment/clear-item k)
                              (if show? (reset! show? false)))))
             disabled-or-blank? (or (str/blank? body) disabled?)]
         (cond
           loading?
           (ui/button {:id "comment-box-btn"
                       :style {:width 138}}

             (ui/donut-white))

           disabled-or-blank?
           (ui/button {:class #?(:clj ""
                                 :cljs "disabled")
                       :style {:width 138}}
             (t :post-comment))

           :else
           (ui/button {:id "comment-box-btn"
                       :class "btn-primary"
                       :style {:width 138}
                       :on-key-down (fn [e]
                                      (when (= 13 (.-keyCode e))
                                        ;; enter key
                                        (on-submit e)))
                       :on-click (fn [e]
                                   (on-submit e))}

             (t :post-comment))))
       (if show?
         [:a.control {:title (t :close)
                   :on-click #(do
                                (reset! show? false)
                                (citrus/dispatch! :citrus/default-update
                                                  [:comment :reply-box?]
                                                  false))
                   :style {:margin-left 6}}
          (ui/icon {:type "close"
                    :color "#999"})])]]]))

(rum/defcs update-comment-box < rum/reactive
  [state {:keys [id post_id body] :as comment} show? [table fk]]
  (let [preview? (citrus/react [:comment :form-data :preview?])
        loading? (citrus/react [:comment :loading?])
        k {:table :comments
           :entity-id id}
        body (or (citrus/react [:comment :drafts k]) body)]
    [:div.comment-box {:style {:margin-bottom 48}}
     (ui/textarea-autosize {:input-ref (fn [v] (citrus/dispatch! :citrus/default-update
                                                                 [:comment :refs k] v))
                            :auto-focus true
                            :style {:border-radius 4
                                    :background "transparent"
                                    :font-size "16px"
                                    :line-height "1.5"
                                    :resize "none"
                                    :width "100%"
                                    :padding 12
                                    :white-space "pre-wrap"
                                    :overflow-wrap "break-word"
                                    :min-height 100}
                            :default-value body
                            :on-change (fn [e]
                                         (let [v (util/ev e)]
                                           (citrus/dispatch! :comment/save-local k v)))})

     (when preview?
       [:div.column
        [:div.divider {:style {:margin-bottom 12}}]
        (post-preview body :markdown nil)]
)
     ;; submit button
     [:div.row1 {:style {:justify-content "flex-end"
                         :margin-top 6}}
      [:div.row1 {:style {:align-items "center"}}
       (preview)

       (let [on-submit (fn [e]
                         #?(:cljs
                            (do
                              (citrus/dispatch! :comment/update
                                                (cond->
                                                  {:id id
                                                   :body (.trim body)}
                                                  post_id (assoc :post_id post_id))
                                                [table fk])
                              (citrus/dispatch! :comment/clear-item k)
                              (if show? (reset! show? false)))))]
         (ui/button {:class (if (str/blank? body)
                              "disabled"
                              "btn-primary")
                     :style {:margin-left 24
                             :margin-top 6
                             :width 138}
                     :on-key-down (fn [e]
                                   (when (= 13 (.-keyCode e))
                                     ;; enter key
                                     (on-submit e)))
                     :on-click on-submit}

          (if loading?
            (ui/donut-white)
            (t :update))))
       (if show?
         [:a {:title (t :close)
              :on-click #(do
                           (reset! show? false)
                           (citrus/dispatch! :citrus/default-update
                                             [:comment :reply-box?]
                                             false))
              :style {:margin-left 6}}
          (ui/icon {:type "close"
                    :color "#999"})])]

      ]]))

(declare comment-item)

(rum/defc comments-cp
  [entity comments]
  (let [[table fk] [:posts :post_id]]
    (map (partial comment-item entity [table fk] nil true) comments)))

(rum/defcs ops < rum/reactive
  (rum/local false ::expand-replies?)
  (rum/local false ::show-link?)
  [state entity comments show-comment-box? comment [table fk] owner?]
  (let [show-link? (get state ::show-link?)
        expand-replies? (get state ::expand-replies?)
        liked-comments (set (citrus/react [:comment :liked-comments]))
        liked? (contains? liked-comments (:id comment))
        [like-title like-button-type like-event] (if liked?
                                                   [(t :unlike-it) "heart" :comment/unlike]
                                                   [(t :like-it) "heart-o" :comment/like])
        likes (get comment :likes 0)
        idx (:idx comment)
        replies-count (:replies_count comment)
        has-replies? (> replies-count 0)
        replies (if has-replies?
                  (filter (fn [x] (= (:id comment)
                                     (:reply_to x)))
                          comments)
                  nil)]
    [:div.column {:style {:margin-top 6}}
     [:div.row {:style {:align-items "center"
                        :justify-content (if (seq replies) "space-between" "flex-end")}}
      (when (and (seq replies) has-replies?)
        [:a.control.row1 {:on-click #(swap! expand-replies? not)
                          :style {:align-items "flex-end"
                                  :font-size 14}}
         [:span {:style {:font-weight "500"}}
          (str replies-count)]
         [:span {:style {:margin-left 4
                         :margin-right 4}}
          (if (= 1 replies-count)
            (str/capitalize (t :reply))
            (str/capitalize (t :replies)))]
         [:i {:class "fa fa-chevron-down"}]])

      [:div.row1 {:style {:align-items "center"}}
       [:div.row1 {:style {:margin-right 24}}
        ;; like
        [:a.like.scale {:title (if liked? (t :unlike) (t :like))
                        :on-click (fn []
                                    (citrus/dispatch! like-event comment [table fk]))}
         (ui/icon {:type (if liked?
                           "favorite"
                           "favorite_border")
                   :color (if liked?
                            colors/like
                            colors/shadow)
                   :width 20
                   :height 20})]

        [:span.number {:style {:margin-left 6
                               :font-size 14
                               :color colors/shadow
                               :font-weight "600"}}
         likes]]

       (ui/menu
         [:a {:on-click (fn [])
              :style {:margin-right 24}}
          (ui/icon {:type :more
                    :color "#999"
                    :width 20
                    :height 20})]
         [(if (:permalink entity)
            [:a.button-text {:style {:font-size 14}
                             :on-click (fn []
                                         (let [text (:body comment)
                                               link (util/comment-link entity idx)]
                                           (util/share {:text text :url link}))
                                         (when-not (util/mobile?)
                                           (reset! show-link? true)))}
             (t :share)])

          [:a.button-text {:style {:font-size 14}
                           :on-click (fn []
                                       (citrus/dispatch! :citrus/default-update [:report]
                                                         {:type :comment
                                                          :id (:id comment)
                                                          :modal? true}))}
           (t :report-this-comment)]

          (when owner?
            [:a.button-text {:on-click #(citrus/dispatch! :comment/delete comment entity [table fk])
                             :style {:font-size 14}}
             (t :delete-this-comment)])]
         {:menu-style {:width 200}
          :other-attrs {:trigger ["click"]}})

       [:a.control {:on-click (fn []
                                           (citrus/dispatch! :comment/reply comment)
                                           (reset! show-comment-box? true))}
        (ui/icon {:type "reply"
                  :color colors/shadow})]]]

     (when @show-link?
       [:div.row1 {:style {:margin-top 12
                           :justify-content "flex-end"}}
        (default-select-input (util/comment-link entity idx))
        [:a {:on-click #(reset! show-link? false)}
         (ui/icon {:type :close
                      :color "#999"})]])

     (when (and has-replies?
                @expand-replies?)
       [:div.column {:style {:margin-top 12}}
        (comments-cp entity replies)

        [:a.control.row1 {:on-click #(swap! expand-replies? not)
                          :style {:align-items "flex-end"
                                  :margin-top 24
                                  :font-size 14}}
         [:span (t :collapse)]
         [:i {:style {:margin-left 4}
              :class "fa fa-chevron-up"}]]])]))

(rum/defcs comment-item
  < {:key-fn (fn [entity [table fk] comments reply? comment admin?] (:id comment))}
  rum/reactive
  (rum/local false ::show-comment-box?)
  (rum/local false ::edit-mode?)
  (rum/local false ::expand-parent?)
  [state entity [table fk] comments reply? {:keys [id idx user body created_at del reply_to] :as comment} admin?]
  (if comment
    (let [entity_id (:id entity)
          edit-mode? (::edit-mode? state)
          expand-parent? (::expand-parent? state)
          show-comment-box? (::show-comment-box? state)
          current-user (citrus/react [:user :current])
          owner? (= (:id current-user) (:id user))
          seperator [:span {:style {:padding "0 8px 0 5px"}} "|"]
          parent (if reply_to (citrus/react [:comment table entity_id :result reply_to]))]
      [:div.comment.col-item {:id (str "comment_" idx)}
       [:div {:key "comment-item-content"
              :style {:display "flex"
                      :flex-direction "row"
                      :flex 1
                      :align-item "center"}}

        [:div.row {:style {:align-item "center"}}
         [:a {:href (str "/@" (:screen_name user))}
          (ui/avatar {:src (util/cdn-image (:screen_name user))
                      :shape "circle"})]
         [:div {:style {:display "flex"
                        :flex-direction "column"
                        :flex 1}}
          [:div.space-between {:style {:line-height "14px"}}
           [:div
            (if (:screen_name user)
              [:a.control {:href (str "/@" (:screen_name user))
                           :style {:font-size 14
                                   :padding "0 3px 0 8px"}}
               (:screen_name user)])

            (if (or owner? admin?)
              [:span
               [:a.control {:style {:margin-left 8
                                    :font-size 13}
                            :on-click (fn []
                                (reset! edit-mode? true)
                                (citrus/dispatch! :citrus/default-update
                                                  [:comment :reply-box?]
                                                  true))}
                (t :edit)]])]
           [:div.row1 {:style {:align-items "center"
                               :font-size "14px"
                               :color "#999"}}
            (when (and parent (not reply?))
              (let [{:keys [screen_name]} (:user parent)]
                [:a.icon-button.no-decoration
                 {:title screen_name
                  :on-click (fn []
                              (citrus/dispatch!
                               :comment/scroll-into
                               (:idx parent)))
                  :style {:margin-right 24}}
                 [:div.row1 {:style {:align-items "center"
                                     :color "#999"}}
                  [:i {:class "fa fa-mail-forward"
                       :style {:margin-right 3}}]
                  (ui/avatar {:src (util/cdn-image screen_name)
                              :shape "circle"
                              :class "ant-avatar-sm"})]]))

            (util/time-ago created_at)] ]

          [:div {:style {:padding "8px 0 0 8px"}}
           (if @edit-mode?
             (update-comment-box comment edit-mode? [table fk])
             (widgets/transform-content body
                                        {:on-mouse-up (fn [e]
                                                       (let [text (util/get-selection-text)]
                                                         (when-not (str/blank? text)
                                                           (citrus/dispatch! :comment/set-selection
                                                                             {:screen_name (:screen_name user)
                                                                              :idx idx}))))}))

           (ops entity comments show-comment-box? comment [table fk] owner?)]]]]

       (if @show-comment-box?
         [:div {:key "comment-box"}
          [:div.divider]
          (comment-box current-user entity [table fk] comment show-comment-box? false)])])))

(rum/defc comments-stream < rum/reactive
  [entity [table fk] comments end? admin?]
  (let [current-path (citrus/react [:router :handler])
        loading? (citrus/react [:query :scroll-loading? current-path])]
    [:div.column.comments {:style {:font-size "16px"}}
    (inf/infinite-list (map (fn [comment]
                              (comment-item entity [table fk] comments false comment admin?)) comments)
                       {:on-load
                        (if end?
                          identity
                          (fn []
                            (citrus/dispatch! :citrus/load-more-comments
                                              {:table table
                                               :fk fk
                                               :id (:id entity)
                                               :last (last comments)})))})
     (when loading?
       [:div.center {:style {:margin "24px 0"}}
        [:div.spinner]])]))

(rum/defc comment-list < rum/reactive
  {:after-render (fn [state]
                   #?(:cljs (when-let [anchors (dommy/sel "div.quote-header a")]
                              (doseq [anchor anchors]
                                (dommy/listen! anchor :click
                                               (fn [e]
                                                 (if-let [idx (dommy/attr anchor "quoteidx")]
                                                   (citrus/dispatch! :comment/scroll-into idx)
                                                   (util/scroll-to-top)))))))
                   state)}
  "Render a comment list."
  [entity]
  (let [current-user (citrus/react [:user :current])
        entity-id (:id entity)
        [table fk] [:posts :post_id]
        admin? (admins/admin? (:screen_name current-user))
        {:keys [result end? count-delta]}(citrus/react [:comment table entity-id])
        comments (remove nil? (vals result))
        comments (sort-by :created_at comments)
        comments (map (fn [x] (assoc x fk entity-id)) comments)
        reply-box? (citrus/react [:comment :reply-box?])]
    [:div.center-area
     [:div.auto-padding
      (comment-box current-user entity [table fk] nil nil
                   (if reply-box?
                     true
                     false))]

     [:div.comment-list
      (let [comments-count (get entity :comments_count 0)]
        (if (> comments-count 0)
          [:div.space-between.auto-padding {:style {:align-items "center"
                                       :margin-bottom 12}}
           [:h2 {:style {:margin 0}}
            (when-not (zero? (count comments))
              (str (+ (:comments_count entity) (if count-delta count-delta 0)) " " (str/capitalize (t :replies))))]]))

      (when (seq comments)
        (comments-stream entity [table fk] comments end? admin?))]]))

(rum/defcs user-comment-item
  < {:key-fn (fn [user-id [id comment]] id)}
  rum/reactive
  (rum/local false ::edit-mode?)
  [state user-id [id {:keys [id idx body created_at] :as comment}]]
  (if comment
    (let [edit-mode? (::edit-mode? state)
          current-user (citrus/react [:user :current])
          owner? (= (:id current-user) user-id)
          seperator [:span {:style {:padding "0 8px 0 5px"}} "|"]]
      [:div.comment.col-item
       [:div
        [:div {:style {:display "flex"
                       :flex-direction "row"
                       :flex 1
                       :align-item "center"}}

         [:div.row {:style {:align-item "center"}}
          [:div {:style {:display "flex"
                         :flex-direction "column"
                         :flex 1}}
           [:div.space-between
            [:a {:href (str "/" (:post_permalink comment) "/" idx)
                 :style {:color colors/primary}}
             (:post_permalink comment)]
            [:span {:style {:color "#999"
                            :font-size "0.9em"}}
             (util/time-ago created_at)]]

           [:div {:style {:padding "16px 8px 8px 8px"}}
            (if @edit-mode?
              (let [[table fk] [:posts :post_id]]
                (update-comment-box comment edit-mode? [table fk]))
              (widgets/transform-content body nil))]]]]]])))

(rum/defc user-comments-list
  [user-id {:keys [result end?]}]
  [:div#comments-list
   (if (seq result)
     (map (partial user-comment-item user-id) result)
     [:div
      [:h5.auto-padding {:style {:color colors/shadow}}
       "Empty."]
      [:a {:title "Real Programmers"
           :href "https://xkcd.com/378/"}
       [:img {:src "https://imgs.xkcd.com/comics/real_programmers.png"}]]
      ])])
