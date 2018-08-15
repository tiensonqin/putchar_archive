(ns share.components.group
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.kit.query :as query]
            [clojure.string :as str]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.channel :as channel]
            [share.components.post :as post]
            [share.components.widgets :as w]
            [share.kit.mixins :as mixins]
            [share.helpers.image :as image]
            [share.util :as util]
            [bidi.bidi :as bidi]
            [share.dicts :refer [t]]
            [share.config :as config]
            [share.kit.colors :as colors]
            #?(:cljs [goog.dom :as gdom])))

(def new-group-fields
  {:name          {:placeholder (t :group-name)
                   :warning (t :name-warning)
                   :validators [util/group-name?]}
   :purpose       {:type :textarea
                   :placeholder (str (t :purpose) "...")
                   :warning (t :purpose-warning)
                   :validators [(util/length? {:min 2
                                               :max 1024})]
                   :style {:resize "none"
                           :height "120px"}}
   ;; :privacy       {:side (t :privacy)
   ;;                 :type :radio
   ;;                 :options [{:label (t :public)
   ;;                            :value "public"
   ;;                            :default true}
   ;;                           {:label (t :invite-only)
   ;;                            :value "invite"}
   ;;                           {:label (t :private)
   ;;                            :value "private"}]}
   ;; :rule          {:type :textarea
   ;;                 :placeholder (str (t :rules) ", " (t :optional))
   ;;                 :style {:resize "none"
   ;;                         :height "120px"}}
   })

(rum/defcs logo-uploader <
  (rum/local false ::uploading?)
  (rum/local false ::undo?)
  [state form]
  (let [uploading? (get state ::uploading?)
        undo? (get state ::undo?)]
    [:div.logo-uploader {:style {:margin "0 0 24px 0"}}
     (if (:logo @form)
       [:div.row
        [:img {:src (util/group-logo (util/internal-name (:name @form)))
               :style {:max-width 300}}]
        (if (false? @undo?)
          [:a {:style {:margin-left 6}
               :on-click (fn []
                           (swap! form assoc :logo nil))}
           (ui/icon {:type "close"})])]
       [:div
        (if @uploading?
          (ui/donut)
          (ui/button {:on-click (fn []
                                  #?(:cljs
                                     (.click (gdom/getElement "photo_upload"))))}
            (t :upload-a-logo)))

        [:input
         {:id "photo_upload"
          :type "file"
          :on-change (fn [e]
                       (if-not (str/blank? (:name @form))
                         (image/upload
                          (.-files (.-target e))
                          (fn [file file-form-data]
                            (reset! uploading? true)
                            (.append file-form-data "name" (str (str/lower-case (:name @form)) "_logo"))
                            (.append file-form-data "png" true)
                            (citrus/dispatch!
                             :image/upload
                             file-form-data
                             (fn [url]
                               (reset! uploading? false)
                               (swap! form assoc :logo url)))))))
          :hidden true}]

        (if (:logo-error @form)
          [:p {:class "help is-danger"}
           (:logo-error @form)])])]))

(rum/defcs logo-change
  < (rum/local false ::uploading?)
  [state group form]
  (let [uploading? (get state ::uploading?)
        group-logo [:img {:src (util/group-logo (util/internal-name (:name group)))
                          :style {:max-width 100
                                  :max-height 100}}]]
    [:div.logo-uploader {:style {:margin "0 0 12px 0"}}
     (if (:logo @form)
       group-logo
       [:div
        (if @uploading?
          (ui/donut)
          [:a {:title (t :change-group-logo)
               :on-click (fn []
                           #?(:cljs
                              (.click (gdom/getElement "photo_upload"))))}
           group-logo])

        [:input
         {:id "photo_upload"
          :type "file"
          :on-change (fn [e]
                       (if-not (str/blank? (:name group))
                         (image/upload
                          (.-files (.-target e))
                          (fn [file file-form-data]
                            (reset! uploading? true)
                            (.append file-form-data "name" (str (str/lower-case (:name group)) "_logo"))
                            (.append file-form-data "png" true)
                            (.append file-form-data "invalidate" true)
                            (citrus/dispatch!
                             :image/upload
                             file-form-data
                             (fn [url]
                               (reset! uploading? false)
                               (swap! form assoc :logo url)
                               (citrus/dispatch! :notification/add :success (t :logo-changed-notification))))))))
          :hidden true}]

        (if (:logo-error @form)
          [:p {:class "help is-danger"}
           (:logo-error @form)])])]))

(rum/defc new < rum/reactive
  []
  [:div.column {:style {:max-width 600}}
   (form/render
     {:footer (fn [form-data]
                (logo-uploader form-data))
      :title (t :create-new-group)
      :loading? [:group :loading?]
      :fields new-group-fields
      :submit-text (t :create)
      :on-submit (fn [form-data]
                   (if (:logo @form-data)
                     (citrus/dispatch! :citrus/group-new (-> @form-data
                                                             (dissoc :logo :logo-error)
                                                             (update :name util/internal-name)))
                     (swap! form-data assoc :logo-error (t :please-upload-a-logo))))})])

(rum/defc group-common < rum/reactive
  [group-name post-filter]
  (let [group-name (if group-name (str/lower-case group-name))
        {:keys [id name purpose channels posts] :as group} (citrus/react [:group :by-name group-name])
        path [:posts :by-group group-name post-filter]
        posts (citrus/react path)]
    [:div.column {:style {:padding-bottom 24}}
     (w/cover-nav group nil)

     (if group
       (query/query
         (post/post-list posts {:group_id id
                                :merge-path path}))
       [:div.row1 {:style {:justify-content "center"}}
        (ui/donut)])]))

(rum/defc group < rum/reactive
  (mixins/query :group)
  {:will-mount (fn [state]
                 #?(:cljs
                    (let [params (first (:rum/args state))]
                      (citrus/dispatch-sync!
                      :post/set-filter
                      (if-let [filter (:post-filter params)]
                        (keyword filter)
                        :latest-reply))
                      ))
                 state)}
  [{:keys [group-name post-filter]
    :or {post-filter "latest-reply"}
    :as params}]
  (group-common group-name (keyword post-filter)))

(rum/defc group-item < rum/static
  [current-user stared_groups group]
  (if (:name group)
    (let [stared? (contains? (set (keys stared_groups)) (:id group))]
      [:div.row1.group-item.col-item {:key (:id group)}
       [:a {:style {:margin-top 6
                    :min-width 64}
            :href (str "/" (:name group))}
        [:span.item {:style {:width 64
                             :overflow "hidden"}}
         [:img {:src (util/group-logo (util/internal-name (:name group)))
                :style {:max-width 64
                        :max-height 64}}]]]
       [:div.column {:style {:margin-left 12}}
        [:div.row1 {:style {:align-items "center"
                            :justify-content "space-between"}}
         [:div
          [:a {:style {:color "rgba(0,0,0,.64)"
                       :font-size 20
                       :font-weight "600"}
               :href (str "/" (:name group))}
           (util/original-name (:name group))]]

         [:span {:style {:color "#999"}}
          (str (:stars group) " " (str/lower-case (t :group-members)))]]

        (if (:purpose group)
          (w/transform-content (:purpose group) {:body-format :asciidoc
                                                 :style {:margin-top 12
                                                        :font-size 14}}))

        [:div.row1 {:style {:justify-content "flex-end"}}
         (w/join-button current-user group stared? 120)]]])))

(rum/defc group-list < rum/reactive
  (mixins/query :groups)
  {:will-mount (fn [state]
                 #?(:cljs (do
                            (citrus/dispatch! :search/reset)
                            (citrus/dispatch! :citrus/reset-search-mode? true))
                    :clj nil)
                 state)
   :will-unmount (fn [state]
                   #?(:cljs (do
                              (citrus/dispatch! :search/reset)
                              (citrus/dispatch! :citrus/reset-search-mode? false))
                      :clj nil)
                   state)}
  [params]
  (let [{:keys [loading? result q]} (citrus/react [:search])
        current-user (citrus/react [:user :current])
        stared_groups (util/get-stared-groups current-user)
        width (citrus/react [:layout :current :width])
        mobile? (or (util/mobile?) (<= width 768))]
    (query/query
      (if loading?
        [:div.row {:style {:justify-content "center"
                           :margin "24px 0"}}
         (ui/donut)]

        (let [hot-groups (citrus/react [:group :hot])
              groups (if (or (nil? loading?) (str/blank? q))
                       hot-groups
                       result)]
          [:div.column.auto-padding {:style {:margin-bottom 100}}
           (if (and q
                    result
                    (empty? (filter #(= (str/lower-case q) (:name %)) result)))
             [:div
              [:div {:style {:font-size 20}}
               [:span (str (t :group) " ")]
               [:span {:style {:font-size 24
                               :font-weight "bold"}}
                q]
               (t :not-exists-yet)]

              [:div {:style {:height 24}}]

              (ui/button {:class "btn-primary"
                          :style {:width 200}
                          :href "/new-group"}
                (t :create-it-yourself))])

           (for [group groups]
             (rum/with-key (group-item current-user stared_groups group) (:id group)))])))))

(defn group-fields
  [form-data]
  {:purpose       {:type :textarea
                   :label (str (t :purpose) ":")
                   :placeholder (str (t :purpose) "...")
                   :value (:purpose @form-data)
                   :warning (t :purpose-warning)
                   :validators [(util/length? {:min 2
                                               :max 254})]
                   :style {:resize "none"
                           :height "120px"}}
   :rule          {:type :textarea
                   :label (str (t :rules) ":")
                   :placeholder (str (t :rules) ", " (t :optional))
                   :value (:rule @form-data)
                   :style {:resize "none"
                           :height "240px"}}})

(rum/defc edit < rum/reactive
  (mixins/query :group-edit)
  [params]
  (query/query
    (let [group (citrus/react [:group :by-name (:group-name params)])]
      [:div.column {:style {:max-width 600
                            :padding-bottom 200}}
       (form/render
         {:title (t :update-group)
          :loading? [:group :loading?]
          :header (fn [form-data]
                    (logo-change group form-data))
          :fields (group-fields (atom group))
          :on-submit (fn [form-data]
                       (let [data (cond->
                                    {:id (:id group)}
                                    (not (str/blank? (:purpose @form-data)))
                                    (assoc :purpose (:purpose @form-data))

                                    (not (str/blank? (:rule @form-data)))
                                    (assoc :rule (:rule @form-data)))]
                         (citrus/dispatch! :group/update (:name group) data)))})])))

(rum/defc members-cp
  [members]
  [:div.row1 {:style {:flex-wrap "wrap"}}
   (for [{:keys [screen_name pro?] :as member} members]
     [:div {:key screen_name
            :style {:padding 6}}
      (w/avatar member {:pro? pro?
                        :class "ant-avatar"})])])

(rum/defcs group-logo
  < rum/reactive
  [state stared-group? current-group width mobile?]
  (if current-group
    (let [current-user (citrus/react [:user :current])
          {route :handler params :route-params} (citrus/react :router)
          group-name (or (:name current-group)
                         (:group-name params))
          new-post? (= route :new-post)
          edit-post? (= route :post-edit)
          text [:span.logo {:style {:max-width (if mobile?
                                                 80
                                                 300)}}
                (util/original-name group-name)]
          logo [:div {:key "group-logo"}
                [:img {:src (util/group-logo group-name)
                       :style {:max-height 36
                               :max-width 64}}]]
          logo-text [:div.row1 {:style {:align-items "center"}}
                     logo
                     text]]
      [:div {:class "logo-area"
             :key "group-logo"
             :style {:align-items "center"}}

       [:a {:href (str "/" group-name)
            :on-click (fn []
                        (citrus/dispatch! :citrus/re-fetch :group {:group-name group-name})
                        (citrus/dispatch! :citrus/default-update [:channel :current] nil))}
        (if (and mobile? (or new-post? edit-post?))
          logo

          logo-text)]

       (when (and current-group
                  (not stared-group?)
                  (not new-post?)
                  (not (util/mobile?)))
         [:div {:style {:margin-left 24}}
          (w/join-button current-user current-group stared-group? 80)])])

    (w/website-logo)))

(rum/defc stared-group-item < rum/static
  [group-id group current-group]
  (if (:name group)
    [:div {:key group-id
           :data-id (str group-id)
           :style {:padding 6
                   :display "block"}}
     [:a {:href (str "/" (:name group))
          :title (util/original-name (:name group))
          :on-click (fn []
                      (citrus/dispatch-sync! :citrus/default-update
                                             [:group :current]
                                             group-id)
                      (citrus/dispatch-sync! :citrus/default-update
                                             [:channel :current]
                                             nil))
          :class (if (and group-id (= group-id current-group))
                   "is-active")}
      (ui/avatar {:src (util/group-logo (:name group))})]]))

(rum/defc stared-groups < rum/reactive
  {:after-render (fn [state]
                   #?(:cljs
                      (ui/new-sortable
                       "#my_groups"
                       {:animation 150
                        :onStart (fn [e]
                                   ;; expand
                                   (citrus/dispatch! :citrus/default-update [:group :expand?] true))
                        :onUpdate (fn [e]
                                    (this-as this
                                      (let [ids (.toArray this)]
                                        (citrus/dispatch! :user/reset-group-orders
                                                          ids))))
                        }))
                   state)
   :will-unmount (fn [state]
                   (citrus/dispatch! :citrus/default-update [:group :expand?] false)
                   state)}
  [loading? groups group]
  (let [expand? (citrus/react [:group :expand?])
        current-group (citrus/react [:group :current])]
    [:div#joined_groups.row1.right-sub
     [:div.space-between {:style {:align-items "center"
                                  :margin-bottom 6}}
      [:h5 (t :groups)]

      [:a {:href "/groups"
           :title (t :add-more-groups)
           :style {:margin-top 4
                   :margin-bottom 8}}
       (ui/icon {:type :add_circle_outline
                 :width 20
                 :height 20
                 :color "#666"})]]

     (if (seq groups)
       (let [n (count groups)
             number 12
             has-more? (> n number)
             show-expand-ok? (and has-more? (not expand?))
             groups (if show-expand-ok? (take number groups) groups)]
         [:div
          [:div#my_groups {:class "row1"
                           :style {:justify-content "space-between"
                                   :flex-wrap "wrap"
                                   :margin-left 0}}
           (for [[group-id group] groups]
             (rum/with-key
               (stared-group-item group-id group current-group) group-id))]
          [:div.row1 {:style {:margin-bottom -4
                              :justify-content "flex-end"}}

           (cond
             show-expand-ok?
             [:a.expand {:on-click (fn [e]
                              (util/stop e)
                              (citrus/dispatch! :citrus/default-update [:group :expand?] true))}
              (ui/icon {:type "expand_more"})]

             expand?
             [:a.expand {:on-click (fn [e]
                              (util/stop e)
                                     (citrus/dispatch! :citrus/default-update [:group :expand?] false))}
              (ui/icon {:type "expand_less"})]

             :else
             nil)]]))]))

(rum/defc members < rum/reactive
  (mixins/query :members)
  [params]
  (let [{:keys [admins members]} (citrus/react [:group :by-name (:group-name params)])]
    [:div.column.auto-padding
     [:h1 (str (t :all-members) ": ")]

     ;; admins
     [:div.h2 {:style {:margin-bottom 12}}
      (str/upper-case (t :admins))]
     [:div.row1 {:style {:flex-wrap "wrap"}}
      (members-cp admins)]


     [:div.divider]

     ;; members
     [:div.h2 {:style {:margin-bottom 12}}
      (str/upper-case (t :members))]
     (members-cp members)]))

(rum/defc group-members
  [group member?]
  [:div#group-members.row1.right-sub
   [:div.space-between {:style {:align-items "center"
                                :margin-bottom 6}}
    [:div.row1 {:style {:align-items "center"}}
     [:h5
      (str/capitalize (t :members))]

     [:a.control {:href (str "/" (:name group) "/members")
                  :style {:margin-left 6
                          :font-size 13}}
      (util/format "(%s)" (t :see-all))]]

    (if member?
      [:a {:title (t :invite)
           :style {:margin-right 1}
           :on-click (fn []
                       (citrus/dispatch! :citrus/default-update
                                         [:group :invite-modal?] true))}
       (ui/icon {:type :person_add
                 :width 20
                 :height 20
                 :color "#666"})])]
   [:div.row {:style {:flex-wrap "wrap"}}
    (for [{:keys [screen_name pro?]} (:admins group)]
      [:div {:key (str "admin-" screen_name)
             :class "is-active"
             :style {:padding 6}}
       (w/avatar {:screen_name screen_name} {:pro? pro?
                                             :title (str (t :admin) ": " screen_name)
                                             :class "ant-avatar-mm"})])
    (let [admins (set (map :screen_name (:admins group)))]
      (for [{:keys [screen_name pro?]} (take 20 (filter
                                                 (fn [{:keys [screen_name]}]
                                                   (not (contains? admins screen_name)))
                                                 (:members group)))]
        [:div {:key (str "member-" screen_name)
               :style {:padding 6}}
         (w/avatar {:screen_name screen_name} {:pro? pro?
                                               :class "ant-avatar-mm"})]))]])

(rum/defc stared-channels < rum/reactive
  [group]
  (let [group-id (:id group)
        channel-id (citrus/react [:channel :current])
        {:keys [stared_channels] :as current-user} (citrus/react [:user :current])
        stared_groups (util/get-stared-groups current-user)
        channels (if current-user
                   (get-in stared_groups [group-id :channels])
                   (:channels group))
        channels (remove nil? channels)]
    [:div#group-stared-channels.row1.right-sub
     [:div.space-between {:style {:align-items "center"
                                  :margin-bottom 6}}
      [:h5
       (str/capitalize (t :stared-channels))]

      [:a {:title (t :add-more-channels)
           :href (str "/" (:name group) "/channels")}
       (ui/icon {:type "add_circle_outline"
                 :width 20
                 :height 20
                 :color "#666"})]]
     [:div.column
      (for [channel channels]
        (if (:name channel)
          (let [active? (= (:id channel) channel-id)]
            [:a.no-decoration {:key (:id channel)
                               :href (str "/" (:name group) "/" (:name channel))
                               :style {:color (if active? "#1a1a1a" "rgb(127,127,127)")
                                       :font-size 15
                                       :font-weight "600"
                                       :padding 6}}
             (util/channel-name (:name channel))])))]]))
