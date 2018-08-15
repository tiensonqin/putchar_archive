(ns share.components.comment
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.util :as util]
            [share.components.report :as report]
            [share.components.widgets :as widgets]
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [web.scroll :as scroll])
            [share.kit.colors :as colors]
            [share.dommy :as dommy]
            [share.kit.mixins :as mixins]
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
               :color (if preview? colors/primary "#999")
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
  [body body-format]
  (let [width (citrus/react [:layout :current :width])]
    [:div.editor.post-preview {:style {:padding 2}}
     [:div.row
      (widgets/transform-content body
                                 {:body-format body-format
                                  :style {:overflow "hidden"}})]]))

;; refs k => ref
(rum/defcs comment-box < rum/reactive
  [state current-user entity [table fk] current-reply show? disabled?]
  (let [entity-id (:id entity)
        current-box-k (citrus/react [:comment :current-box-k])
        k {:table (if current-reply :replies table)
           :entity-id (if current-reply (:id current-reply) entity-id)}
        _ (when (not= k current-box-k)
            (citrus/dispatch! :citrus/default-update
                              [:comment :current-box-k] k))
        body (citrus/react [:comment :drafts k])
        loading? (citrus/react [:comment :loading?])
        group-id (get-in entity [:group :id])
        stared? (contains? (set (keys
                                 (util/get-stared-groups current-user))) group-id)
        invite? (= "invite" (get-in entity [:group :privacy]))
        preview? (citrus/react [:comment :form-data :preview?])]
    [:div.comment-box#post-comment-box {:style (cond->
                                                 {:margin-bottom 48}
                                                 disabled?
                                                 (assoc :opacity 0.5))}
     (cond
       (and group-id (not stared?))
       [:div.column1 {:style {:height 120
                              :border "1px solid #ddd"
                              :border-radius 4
                              :align-items "center"
                              :justify-content "center"}}
        (if invite?
          [:p {:style {:color "#666"}}
           (t :join-to-comment)]
          [:a.control {:on-click #(citrus/dispatch! :user/star-group {:object_type :group
                                                                      :object_id group-id})}
           (t :join-to-comment)])]

       :else
       [:div.column
        (post-box/post-box
         :comment
         k
         {:placeholder (if current-reply
                         (str "@" (get-in current-reply [:user :screen_name]) " ")
                         (t :your-thoughts-here))

          :min-rows 5

          :style {:border-radius 4
                  :font-size 15
                  :background "#fff"
                  :resize "none"
                  :width "100%"
                  :padding 12
                  :white-space "pre-wrap"
                  :overflow-wrap "break-word"
                  :min-height 180}

          :other-attrs {:class (if disabled? "disabled")
                        :auto-focus (if current-reply true false)
                        :ref (fn [v] (citrus/dispatch! :citrus/default-update
                                                       [:comment :refs k] v))}


          :value (or body "")
          :on-change (fn [e]
                       (let [v (util/ev e)]
                         (citrus/dispatch! :comment/save-local k v)))})
        (if preview?
          [:div.column
           [:div.divider {:style {:margin-bottom 12}}]

           (post-preview body "asciidoc")])])

     ;; submit button
     [:div.row {:style {:align-items "center"
                        :justify-content "flex-end"
                        :margin-top 12}}

      (preview)
      [:div.row1 {:style {:align-items "center"
                          :margin-left 24}}
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
                       :style {:width 138}
                       :on-key-down (fn [e]
                                      (when (= 13 (.-keyCode e))
                                        ;; enter key
                                        (on-submit e)))
                       :on-click (fn [e]
                                   (on-submit e))}

             (t :post-comment))))
       (if show?
         [:a.nohl {:title (t :close)
                   :on-click #(do
                                (reset! show? false)
                                (citrus/dispatch! :citrus/default-update
                                                  [:comment :reply-box?]
                                                  false))
                   :style {:margin-left 6}}
          (ui/icon {:type "close"
                    :color "#999"})])]]]))

(rum/defcs update-comment-box < rum/reactive
  [state {:keys [id post_id item_id body] :as comment} show? [table fk]]
  (let [preview? (citrus/react [:comment :form-data :preview?])
        loading? (citrus/react [:comment :loading?])
        k {:table :comments
           :entity-id id}
        body (or (citrus/react [:comment :drafts k]) body)]
    [:div.comment-box {:style {:margin-bottom 48}}
     (if preview?
       [:div.column {:style {:min-height 130
                             :justify-content "space-between"}}
        (post-preview body "asciidoc")
        [:div.divider {:style {:margin-bottom 12}}]]

       (ui/textarea-autosize {:input-ref (fn [v] (citrus/dispatch! :citrus/default-update
                                                                   [:comment :refs k] v))
                              :min-rows 5
                              :auto-focus true
                              :style {
                                      :overflow "hidden"
                                      :border-radius 4
                                      :font-size 15
                                      :background "#fff"
                                      :resize "none"
                                      :width "100%"
                                      :padding 12
                                      :white-space "pre-wrap"
                                      :overflow-wrap "break-word"}
                              :default-value body
                              :on-change (fn [e]
                                           (let [v (util/ev e)]
                                             (citrus/dispatch! :comment/save-local k v)))}))

     ;; submit button
     [:div.space-between
      [:div.row1 {:style {:align-items "center"}}
       (let [on-submit (fn [e]
                         #?(:cljs
                            (do
                              (citrus/dispatch! :comment/update
                                                (cond->
                                                  {:id id
                                                   :body (.trim body)}
                                                  post_id (assoc :post_id post_id)
                                                  item_id (assoc :item_id item_id))
                                                [table fk])
                              (citrus/dispatch! :comment/clear-item k)
                              (if show? (reset! show? false)))))]
         (ui/button {:class (str "btn"
                                 (if (not (str/blank? body))
                                   ""
                                   " disabled"))
                     :style {:margin-top 6
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
         [:a.nohl {:title (t :close)
                   :on-click #(do
                                (reset! show? false)
                                (citrus/dispatch! :citrus/default-update
                                                  [:comment :reply-box?]
                                                  false))
                   :style {:margin-left 6}}
          (ui/icon {:type "close"
                    :color "#999"})])]

      (preview)]]))

(declare comment-item)

(rum/defc comments-cp
  [entity comments]
  (let [[table fk] (if (:permalink entity)
                     [:posts :post_id]
                     [:items :item_id])]
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
    [:div.column.ubuntu {:style {:margin-top 6}}
     [:div.row {:style {:align-items "center"
                        :justify-content (if (seq replies) "space-between" "flex-end")}}
      (when (and (seq replies) has-replies?)
        [:a.hover-activated.row1 {:on-click #(swap! expand-replies? not)
                                  :style {:align-items "flex-end"
                                          :font-size 14}}
         [:span {:style {:font-weight "600"}}
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
                            "#999")
                   :width 20
                   :height 20})]


        [:span.number {:style {:margin-left 6
                               :color "rgb(127,127,127)"
                               :font-weight "600"}}
         likes]]

       (ui/menu
         [:a {:on-click (fn [])
              :style {:margin-right 24}}
          (ui/icon {:type :more
                    :color "#666"})]
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
         {:menu-style {:width 200}})

       [:a.row1.no-decoration {:style {:align-items "center"}
                               :on-click (fn []
                                           (citrus/dispatch! :comment/reply comment)
                                           (reset! show-comment-box? true))}
        (ui/icon {:type "reply"
                  :color "#666"})
        [:span {:style {:color "#666"
                        :font-size 14}}
         (t :reply)]]]]

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

        [:a.hover-activated.row1 {:on-click #(swap! expand-replies? not)
                                  :style {:align-items "flex-end"
                                          :margin-top 24
                                          :font-size 14}}
         [:span (t :collapse)]
         [:i {:style {:margin-left 4}
              :class "fa fa-chevron-up"}]]])]))

(rum/defcs comment-item
  < {:key-fn (fn [entity [table fk] comments reply? comment] (:id comment))}
  rum/reactive
  (rum/local false ::show-comment-box?)
  (rum/local false ::edit-mode?)
  (rum/local false ::expand-parent?)
  [state entity [table fk] comments reply? {:keys [id idx user body created_at del reply_to] :as comment}]
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
          [:div.pervasive-head {:style {:display "flex"
                                        :flex 1
                                        :justify-content "space-between"
                                        :line-height "14px"}}
           [:div
            (if (:screen_name user)
              [:span {:style {:padding "0 3px 0 8px"
                              :font-weight 500}}
               [:a.control {:href (str "/@" (:screen_name user))
                            :style {:color "rgba(0,0,0,0.84)"
                                    :font-size 14}}
                (str "@" (:screen_name user))]])

            (if owner?
              [:span
               [:a {:style {:margin-left 8}
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
                                        {:style {:color "rgba(0,0,0,0.84)"
                                                :font-size "16px"}
                                         :on-mouse-up (fn [e]
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
  [entity [table fk] comments end?]
  (let [current-path (citrus/react [:router :handler])
        loading? (citrus/react [:query :scroll-loading? current-path])]
    [:div.column.comments
    (inf/infinite-list (map (fn [comment]
                              (comment-item entity [table fk] comments false comment)) comments)
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
       [:div.center (t :loading)])]))

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
        [table fk] (if (:permalink entity)
                     [:posts :post_id]
                     [:items :item_id])
        {:keys [result end? count-delta]}(citrus/react [:comment table entity-id])
        comments (remove nil? (vals result))
        comments (sort-by :created_at comments)
        comments (map (fn [x] (assoc x fk entity-id)) comments)
        reply-box? (citrus/react [:comment :reply-box?])]
    [:div.center-area
     [:div.comment-list
      (comment-box current-user entity [table fk] nil nil
                   (if reply-box?
                     true
                     false))

      (let [comments-count (get entity :comments_count 0)]
        (if (> comments-count 0)
          [:div.space-between {:style {:align-items "center"
                                       :margin-bottom 12}}
           [:h3 {:style {:color "#666"
                         :margin 0}}
            (when-not (zero? (count comments))
              (str (+ (:comments_count entity) (if count-delta count-delta 0)) " " (t :replies)))]]))

      (when (seq comments)
        (comments-stream entity [table fk] comments end?))]]))

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
            (if (:post_permalink comment)
              [:a {:href (str "/" (:post_permalink comment) "/" idx)}
               (:post_permalink comment)]
              [:a {:href (str "/item/" (:item_id comment) "/" idx)}
               (str "item/" (:item_id comment) "/" idx)])

            [:span {:style {:color "#999"
                            :font-size "0.9em"}}
             (util/time-ago created_at)]]

           [:div {:style {:padding "16px 8px 8px 8px"}}
            (if @edit-mode?
              (let [[table fk] (if (:permalink comment)
                                 [:posts :post_id]
                                 [:items :item_id])]
                (update-comment-box comment edit-mode? [table fk]))
              (widgets/transform-content body
                                         {:style {:color "rgba(0,0,0,0.84)"
                                                 :font-size "1.2em"}}))
            ]]]]]])))

(rum/defc user-comments-list
  [user-id {:keys [result end?]}]
  [:div#comments-list
   (if (seq result)
     (map (partial user-comment-item user-id) result)
     [:div
      [:span {:style {:padding 24
                      :font-size "24"}}
       (t :no-comments-yet)]])])
