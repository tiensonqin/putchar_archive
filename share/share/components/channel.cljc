(ns share.components.channel
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [clojure.string :as str]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.util :as util]
            [share.components.post :as post]
            [share.kit.mixins :as mixins]
            [share.kit.colors :as colors]
            [share.dicts :refer [t]]
            [share.components.widgets :as widgets]
            [bidi.bidi :as bidi]))

(def new-channel-fields
  {:name          {:placeholder (t :channel-name)
                   :warning (t :name-warning)
                   :validators [util/channel-name?]}
   :purpose       {:type :textarea
                   :placeholder (str (t :purpose) "...")
                   :style {:resize "none"
                           :height "120px"}}})

(rum/defc new < rum/reactive
  [params]
  (let [current-group (citrus/react [:group :by-name (:group-name params)])]
    [:div.column {:style {:max-width 600}}
     (form/render
       {:title (t :create-new-channel)
        :loading? [:channel :loading?]
        :fields new-channel-fields
        :submit-text (t :create)
        :on-submit (fn [form-data]
                     (let [data (-> @form-data
                                    (assoc :group_id (:id current-group))
                                    (update :is_private
                                            (fn [v] (if (= "true" v) true false))))]
                       (citrus/dispatch! :channel/new data)))})]))

(rum/defc channel < rum/reactive
  (mixins/query :channel)
  {:will-mount (fn [state]
                 #?(:cljs (citrus/dispatch-sync! :channel/reset))
                 state)
   :will-unmount (fn [state]
                   #?(:cljs (citrus/dispatch-sync! :channel/reset))
                   state)}
  [{:keys [group-name channel-name post-filter]
    :or {post-filter "latest-reply"}
    :as params}]
  (let [current-channel-id (citrus/react [:channel :current])
        current-group-id (citrus/react [:group :current])
        current-group (util/get-group-by-id (citrus/react [:group :by-name]) current-group-id)
        {:keys [id name purpose] :as channel} (citrus/react [:channel :by-id current-channel-id])
        post-filter (keyword post-filter)
        names (util/group-channel params)
        path [:posts :by-channel names post-filter]
        posts (citrus/react path)]
    [:div.column {:style {:padding-bottom 48}}
     (widgets/cover-nav current-group channel)
     (query/query
       (post/post-list posts {:channel_id id
                              :merge-path path}))]))

(rum/defc stared-channels < rum/reactive
  []
  (let [group-id (citrus/react [:group :current])
        channel-id (citrus/react [:channel :current])
        {:keys [stared_channels] :as current-user} (citrus/react [:user :current])
        stared_groups (util/get-stared-groups current-user)
        group (get stared_groups group-id)
        channels (get-in stared_groups [group-id :channels])]
    (if group
      [:div#stared_channels {:class "row1 shadow"
                             :style {:margin "12px 0"
                                     :padding 12
                                     :background "#FFF"}}
       [:div.space-between {:style {:align-items "center"
                                    :margin-bottom 6}}
        [:span.title {:style {:font-weight "500"
                              :color "#999"}}
         (t :stared-channels)]

        [:a {:href (str "/" (:name group) "/channels")
             :title (t :add-more-channels)
             :style {:margin-top 4}}
         (ui/icon {:type :settings
                   :width 20
                   :height 20
                   :color "#999"})]]

       [:div.column1
        (for [channel channels]
          (let [active? (= (:id channel) channel-id)]
            [:a.no-decoration
             {:id (:id channel)
              :key (:id channel)
              :style {:padding "6px 12px"
                      :color (if active? colors/primary "#1a1a1a")
                      :font-weight (if active? "600" "500")}
              :href (str "/" (:name group) "/" (:name channel))}
             (util/channel-name (:name channel))]))]
       ])))

(rum/defc channels < rum/reactive
  (query/query :channels)
  [params]
  (let [current-group (citrus/react [:group :by-name (:group-name params)])
        channels (:channels current-group)
        channel-id (citrus/react [:channel :current])
        {:keys [stared_channels]} (citrus/react [:user :current])
        managed-groups (citrus/react [:group :managed])
        current-user (citrus/react [:user :current])
        me? (util/me? current-user)
        admin? (or (and current-group (contains? managed-groups (:id current-group))) me?)]
    [:div.column.auto-padding
     [:h1 (str (t :all-channels) ": ")]

     (if admin?
       [:div.row1
        [:a.control {:href (str "/" (:name current-group) "/new-channel")}
         (t :create-new-channel)]])

     [:div.divider]

     (for [channel channels]
       (let [stared? (contains? (set stared_channels) (:id channel))]
         [:div.space-between.col-item
          {:key (:id channel)
           :style {:align-items "center"}}
          [:div.row1
           [:a.no-decoration
            {:id (:id channel)
             :style {:color "#1a1a1a"
                     :font-weight "600"
                     :margin-right 12}
             :href (str "/" (:name current-group) "/" (:name channel))}
            (util/channel-name (:name channel))]
           [:a {:title (if stared? (t :unstar) (t :star))
                :on-click #(citrus/dispatch! (if stared?
                                               :user/unstar-channel
                                               :user/star-channel)
                                             (:name current-group)

                                             {:object_type :channel
                                              :object_id (:id channel)})}
            (ui/icon {:type (if stared? :star :star-border)
                      :color (if stared? colors/primary "#1a1a1a")})]]
          (if (and admin? (not (contains? #{"general"} (:name channel))))
            [:div.row1
             [:a.no-decoration {:title (t :edit)
                                :style {:margin-left 24}
                                :href (str "/" (:name current-group) "/"
                                           (:name channel)
                                           "/edit")}
              (ui/icon {:type "edit"
                        :color "rgba(0,0,0,0.6)"})]])]))]))

(defn channel-fields
  [form-data]
  {:name          {:label (t :channel-name)
                   :placeholder (t :channel-name)
                   :value (:name @form-data)
                   :warning (t :name-warning)
                   :validators [util/channel-name?]}
   :purpose       {:type :textarea
                   :label (t :purpose)
                   :value (:purpose @form-data)
                   :placeholder (str (t :purpose) "...")
                   :style {:resize "none"
                           :height "120px"}}})

(rum/defc edit < rum/reactive
  (mixins/query :channel-edit)
  [{:keys [group-name channel-name]
    :as params}]
  (let [current-channel-id (citrus/react [:channel :current])
        {:keys [id name purpose group] :as channel} (citrus/react [:channel :by-id current-channel-id])]
    (query/query
      (form/render
        {:title (t :edit-channel)
         :loading? [:channel :loading?]
         :fields (channel-fields (atom channel))
         :on-submit (fn [form-data]
                      (citrus/dispatch! :channel/update group channel (assoc @form-data :id id)))}))))
