(ns web.handlers.group
  (:require [share.util :as util]
            [share.dicts :refer [t]]
            [share.helpers.form :as form]
            [share.query :as query]
            [share.config :as config]))

(def handlers
  {
   :citrus/group-new
   (fn [state data]
     (let [current-user (get-in state [:user :current])
           type (:type current-user)
           non-pro-private? (and
                             (= "private"(:privacy data))
                             (not= "pro" type))]
       (if non-pro-private?
         {:state (assoc-in state [:user :pro-modal?] true)}
         {:state (assoc-in state [:group :loading?] true)
          :http {:params [:group/new data]
                 :on-load :citrus/group-new-ready}})))

   :citrus/group-new-ready
   (fn [state result]
     {:state (-> state
                 (update-in [:group :loading?] (fn [_] false))
                 (update-in [:user :current :stared_groups]
                            (fn [old]
                              (assoc old (:id result) result))))
      :redirect {:handler :group
                 :route-params {:group-name (:name result)}}})

   :citrus/group-top
   (fn [state group-id]
     {:state (-> state
                 (assoc-in [:group :top] group-id)
                 (update-in [:user :current :stared_groups]
                            (fn [groups]
                              (util/top-elem groups group-id))))
      :http {:params [:group/top {:id group-id}]
             :on-load :citrus/group-top-ready}})

   :citrus/group-top-ready
   (fn [state result]
     {:state state})

   :citrus/reset-current-group-channel
   (fn [state]
     {:state (-> state
                 (assoc-in [:group :current] nil)
                 (assoc-in [:channel :current] nil))})

   :citrus/reset-first-group
   (fn [state]
     {:state (-> state
                 (assoc-in [:group :current] (ffirst (get-in state [:user :current :stared_groups])))
                 (assoc-in [:channel :current] nil))})


   :group/update
   (fn [state group-name data]
     {:state {:loading? true}
      :http {:params [:group/update data]
             :on-load [:citrus/group-update-ready group-name data]}
      :dispatch [:citrus/clean-cache :group {:group-name group-name}]})

   :citrus/group-update-ready
   (fn [state group-name data result]
     {:state (-> state
                 (assoc-in [:group :loading?] false)
                 (update-in
                  [:group :by-name group-name]
                  merge
                  data))
      :dispatch [[:router/back]
                 [:notification/add :success (t :group-updated)]]})

   :group/send-invites
   (fn [state {:keys [to who group-name self-email] :as data} invite?]
     (let [to (->> (util/split-comma @to)
                   (remove #{self-email})
                   (filter form/email?)
                   (take 20)
                   (vec))]
       (if (seq to)
         {:state {:loading? true}
          :http {:params [:user/send-invites {:to to
                                              :who who
                                              :group-name group-name}]
                 :on-load [:group/send-invites-ready]
                 :on-error [:group/send-invites-failed data]}}
         {:state state})
       ))

   :group/send-invites-ready
   (fn [state result]
     {:state {:loading? false
              :invite-modal? false}
      :dispatch [:notification/add :success (t :invites-sent)]})

   :group/send-invites-failed
   (fn [state data {:keys [body]}]
     {:state (assoc state :error (t :bad-happened))})

   :group/promote-user
   (fn [state group-name data promote?]
     {:state state
      :http {:params [:group/promote-user data]
             :on-load [:group/promote-ready group-name data promote?]
             :on-error [:group/promote-failed data]}})

   :group/promote-ready
   (fn [state group-name data promote? result]
     (reset! promote? false)
     {:state (update-in state [:by-name group-name]
                        assoc :admins result)
      :dispatch [:notification/add :success (str (:screen_name data) (t :has-been-promoted))]})

   :group/promote-failed
   (fn [state data {:keys [body]}]
     {:state (assoc state :error
                    (case (:message body)
                      :user-not-exists
                      (t :user-not-exists)
                      :user-not-joined
                      (t :user-not-joined)
                      (t :bad-happened)))})

   :group/clear-error
   (fn [state]
     {:state (assoc state :error nil)})

   :citrus/switch
   (fn [state action]
     (let [current-handler (get-in state [:router :handler])
           get-fn (if (= action :prev)
                    util/get-prev
                    util/get-next)]
       (cond
         ;; group
         (contains? #{:home :group :channel} current-handler)
         (if-let [user (get-in state [:user :current])]
           (let [groups (:stared_groups user)
                 current-id (get-in state [:group :current])
                 new-id (get-fn (keys groups) current-id)
                 new-group (get groups new-id)]
             (if (not= current-id new-id)
               {:state (-> state
                           (assoc :switch-mode? true)
                           (assoc-in [:group :current] new-id))
                :redirect {:handler :group
                           :route-params {:group-name (:name new-group)}}
                :timeout {:duration 1000
                          :events [:citrus/leave-switch-mode]}}
               {:state state}))
           {:state state})

         ;; item
         (contains? #{:item} current-handler)
         (if-let [current-id (get-in state [:item :current])]
           (let [collection-id (get-in state [:collection :current])
                 items (get-in state [:collection :by-id collection-id :items])
                 new-id (get-fn (mapv :id items) current-id)
                 new-item (-> (filter #(= (:id %) current-id) items) first)]
             (if (and new-item (not= current-id new-id))
               {:state (-> state
                           (assoc :switch-mode? true)
                           (assoc-in [:item :current] new-id))
                :redirect {:handler :item
                           :route-params {:item-id (str new-id)}}
                :timeout {:duration 1000
                          :events [:citrus/leave-switch-mode]}}
               {:state state}))
           {:state state})

         :else
         {:state :state})))

   :citrus/leave-switch-mode
   (fn [state]
     {:state {:switch-mode? false}})

   :group/save-cover
   (fn [state group cover-settings edit-mode?]
     (let [group-name (:name group)
           data {:id (:id group)
                 :cover_settings (if cover-settings
                                   (pr-str cover-settings))}]
       {:state (-> state
                   (assoc :loading? true)
                   (update-in
                    [:by-name group-name]
                    merge
                    data))
        :http {:params [:group/update data]
               :on-load [:group/save-cover-ready group-name data edit-mode?]}
        :dispatch [:citrus/clean-cache :group {:group-name group-name}]}))

   :group/save-cover-ready
   (fn [state group-name data edit-mode? result]
     (if edit-mode? (reset! edit-mode? false))
     {:state {:loading? false}
      :dispatch [:notification/add :success (t :group-cover-changed)]})

   :group/add-item
   (fn [state form-data]
     {:state {:loading? true}
      :http {:params [:item/new @form-data]
             :on-load [:group/add-item-ready form-data]
             :on-error [:group/add-item-failed form-data]}})

   :group/add-item-ready
   (fn [state form-data result]
     (let [group-name (:group_name @form-data)]
       (reset! form-data nil)
       {:state (-> state
                   (update-in [:by-name group-name :items]
                              (fn [items]
                                (conj items (select-keys result [:id :name]))))
                   (assoc :loading? false))
        :redirect {:handler :item
                   :route-params {:item-id (str (:id result))}}
        }))

   :group/add-item-failed
   (fn [state form-data reply]
     {:state (cond
               (and (= (:status reply) 400)
                    (= (get-in reply [:body :message]) ":item-exists"))
               {:item-exists? true}

               :else
               state)})

   :group/update-item
   (fn [state item form-data]
     {:state {:loading? true}
      :http {:params [:item/update (assoc @form-data :id (:id item))]
             :on-load [:group/update-item-ready item form-data]
             :on-error [:group/update-item-failed item form-data]}})

   :group/update-item-ready
   (fn [state item form-data result]
     (let [group-name (get-in item [:group :name])]
       (util/set-href! (str config/website "/item/"
                            (:id item)))
       {:state state}))

   :group/update-item-failed
   (fn [state form-data reply]
     {:state (cond
               (and (= (:status reply) 400)
                    (= (get-in reply [:body :message]) ":item-exists"))
               {:item-exists? true}

               :else
               state)})

   :group/delete-item
   (fn [state item]
     {:state {:loading? true}
      :http {:params [:item/delete {:id (:id item)}]
             :on-load [:group/delete-item-ready item]}})

   :group/delete-item-ready
   (fn [state item result]
     (let [group-name (get-in item [:group :name])]
       (util/set-href! (str config/website "/"
                            group-name))
       {:state state}))


   })
