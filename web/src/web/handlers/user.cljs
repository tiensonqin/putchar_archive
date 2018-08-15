(ns web.handlers.user
  (:require [share.util :as util]
            [share.dicts :refer [t] :as dicts]
            [appkit.caches :as caches]
            [web.stripe :as stripe]
            [appkit.macros :refer [oget]]))

(def handlers
  {:user/request-code
   (fn [state data]
     {:state {:loading? true}
      :http {:params [:auth/request-code data]
             :on-load :user/request-code-ready}})

   :user/request-code-ready
   (fn [state result]
     {:state {:loading? false}
      :dispatch [:notification/add :success (t :please-check-your-email)]})

   :user/upgrade-to-pro
   (fn [state token-callback]
     (let [{:keys [email]} (:current state)]
       (stripe/open-checkout email
                             token-callback
                             (fn []
                               (prn "closed")))
       {:state {:pro-modal? false}}))

   :user/new
   (fn [state data form-data]
     {:state {:loading? true}
      :http {:params [:user/new data]
             :on-load [:citrus/authenticate-ready true]
             :on-error [:user/new-error form-data]}})

   :user/new-error
   (fn [state form-data reply]
     {:state (cond
               (and (= (:status reply) 400)
                    (= (get-in reply [:body :message]) ":username-exists"))
               (do
                 (swap! form-data assoc-in [:validators :screen_name] false)
                 {:username-taken? true})

               (and (= (:status reply) 400)
                    (= (get-in reply [:body :message]) ":email-exists"))
               (do
                 (swap! form-data assoc-in [:validators :email] false)
                 {:email-taken? true})

               :else
               state)})

   :user/login-ready
   (fn [state result]
     {:state {:loading? false
              :current (update (:user result) :stared_groups util/normalize)
              }
      :dispatch [:user/close-signin-modal?]})

   :citrus/authenticate-ready
   (fn [state new? result]
     (let [user (:user result)
           social? (:github_id user)]
       (cond
         (and new? social?)
         {:state (-> state
                     (assoc-in [:user :signup-step] :pick-groups)
                     (assoc-in [:user :loading?] false)
                     (assoc-in [:user :temp] user))}

         ;; email
         (and new? (not social?))
         {:state (-> state
                     (assoc-in [:user :signup-step] :add-avatar)
                     (assoc-in [:user :loading?] false)
                     (assoc-in [:user :temp] user))}

         :else
         {:state (-> state
                     (assoc-in [:user :loading?] false)
                     (assoc-in [:user :current]
                               (update user :stared_groups util/normalize)))
          :redirect {:handler :home}})))

   :user/reset-group-orders
   (fn [state groups-ids]
     (if (:current state)
       {:state {:loading? true}
        :http  {:params   [:user/update {:stared_groups (mapv util/uuid groups-ids)}]
                :on-load  :user/reset-group-orders-success}}
       {:state state}))

   :user/reset-group-orders-success
   (fn [state result]
     {:state (-> state
                 (assoc :loading? false))
      :dispatch [:notification/add :success (t :groups-order-saved)]})

   :user/update
   (fn [state data]
     (prn data)
     {:state {:loading? (if (or (contains? (set (keys data))
                                           :email_notification)
                                (contains? (set (keys data))
                                           :github_repo))
                          false
                          true)}
      :http  {:params   [:user/update data]
              :on-load  [:user/update-success data]}})

   :user/update-success
   (fn [state data result]
     {:state (-> state
                 (assoc :loading? false)
                 (update :current merge result)
                 (update-in [:current :stared_groups] util/normalize))
      :dispatch [:notification/add :success (t :profile-updated)]})

   :user/star-group
   (fn [state data]
     (let [current-user (:current state)
           type (:type current-user)]
       (cond
         (and current-user (>= (count (:stared_groups current-user)) 20))
         {:state state
          :dispatch [:notification/add :error (t :already-20-groups)]}

         current-user
         {:state {:loading? true}
          :http {:params [:user/star data]
                 :on-load [:citrus/star-group-success data]}}

         :else
         {:state {:signin-modal? true}})))

   :citrus/star-group-success
   (fn [state data result]
     {:state (-> state
                 (assoc-in [:user :loading?] false)
                 (assoc-in [:user :current]
                           (update (:current result) :stared_groups util/normalize))
                 (assoc-in [:group :current] (:object_id data)))})

   :user/star-channel
   (fn [state group-name data]
     {:state {:loading? true}
      :http {:params [:user/star data]
             :on-load [:citrus/star-channel-success data]}})

   :citrus/star-channel-success
   (fn [state data result]
     {:state (-> state
                 (assoc-in [:user :loading?] false)
                 (assoc-in [:user :current]
                           (update (:current result) :stared_groups util/normalize))
                 (assoc-in [:channel :current] (str (:object_id data))))})

   :user/unstar-group
   (fn [state data]
     {:state {:loading? true}
      :http {:params [:user/unstar data]
             :on-load [:citrus/unstar-group-success data]}})

   :citrus/unstar-group-success
   (fn [state data result]
     {:state (-> state
                 (assoc-in [:user :loading?] false)
                 (assoc-in [:user :current]
                           (update (:current result) :stared_groups util/normalize)))
      :redirect {:handler :home}})

   :user/unstar-channel
   (fn [state group-name data]
     {:state {:loading? true}
      :http {:params [:user/unstar data]
             :on-load [:citrus/unstar-channel-success group-name data]}})

   :citrus/unstar-channel-success
   (fn [state group-name data result]
     {:state (-> state
                 (assoc-in [:user :loading?] false)
                 (assoc-in [:user :current]
                           (update (:current result) :stared_groups util/normalize)))})

   :user/logout
   (fn [state]
     ;; clear caches
     (caches/clear
      #(set! (.-location js/window) "/logout"))
     ;; unregister web worker
     {:state {:current nil}})

   :user/signup-join-group
   (fn [state group]
     {:state {:signup-groups (vec (distinct (conj (:signup-groups state) group)))}})

   :user/signup-leave-group
   (fn [state group]
     {:state {:signup-groups (->> (:signup-groups state)
                                  (remove #(= % group))
                                  (vec))}})

   :user/signup-join-groups
   (fn [state]
     {:state {:loading? true}
      :http {:params [:user/join-groups {:groups (:signup-groups state)}]
             :on-load :user/signup-join-groups-success}})

   :user/signup-join-groups-success
   (fn [state result]
     {:state {:loading? false
              :current (:current result)}
      :redirect {:handler :home}})

   :citrus/set-locale
   (fn [state locale]
     (reset! dicts/locale locale)
     {:state {:locale locale}
      :cookie [:set-forever "locale" (name locale)]})

   :citrus/clear-caches
   (fn [state]
     (caches/clear nil)
     {:state state})

   :user/close-pro-modal?
   (fn [state]
     {:state {:pro-modal? false}})

   :user/show-pro-modal?
   (fn [state]
     {:state {:pro-modal? true}})

   :user/show-signin-modal?
   (fn [state]
     (if (:current state)
       {:state state}
       {:state {:signin-modal? true}}))

   :user/close-signin-modal?
   (fn [state]
     {:state {:signin-modal? false}})

   :citrus/log-redirect-url
   (fn [state]
     {:state state
      :local-storage {:action :set
                      :key "redirect-url"
                      :value js/window.location.pathname}})

   :user/subscribe-pro
   (fn [state customer]
     {:state {:loading? true}
      :http {:params [:user/subscribe-pro customer]
             :on-load :user/subscribe-pro-success
             :on-error :user/subscribe-pro-failed}})

   :user/subscribe-pro-success
   (fn [state result]
     {:state {:loading? false
              :current (assoc (:current state)
                              :type "pro")}
      :dispatch [:notification/add :success (t :thanks-for-your-support)]})

   :user/subscribe-pro-failed
   (fn [state result]
     ;; TODO:
     {:state {:loading? false}
      })

   :user/poll
   (fn [state]
     {:state state
      :http {:params [:user/poll]
             :on-load :citrus/poll-success
             :on-error :user/poll-failed}})

   :citrus/poll-success
   (fn [state result]
     (cond->
       {:state (-> state
                   (assoc-in [:user :current :has-unread-notifications?] (:has-unread-notifications? result))
                   (assoc-in [:report :new?] (:has-unread-reports? result)))
        :timeout {:duration 30000
                  :events [:user/poll]}}

       (and
        (not (get-in state [:user :current :has-unread-notifications?]))
        (:has-unread-notifications? result))
       (assoc :dispatch [:citrus/re-fetch :notifications {}])

       (and (:has-unread-reports? result)
            (not (get-in state [:report :new?])))
       (assoc :dispatch [:citrus/re-fetch :reports {}])
       ))

   :user/poll-failed
   (fn [state error]
     ;; FIXME:
     {:state state
      :timeout {:duration 30000
                :events [:user/poll]}})


   :citrus/clear-notifications
   (fn [state]
     {:state (assoc state :notifications nil)
      :http {:params  [:user/mark-notifications-as-read {}]
             :on-load :user/clear-notifications-success}})

   :user/clear-notifications-success
   (fn [state result]
     {:state (assoc-in state [:current :has-unread-notifications?] false)})

   })
