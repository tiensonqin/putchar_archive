(ns web.handlers.user
  (:require [share.util :as util]
            [share.dicts :refer [t] :as dicts]
            [appkit.caches :as caches]
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

   :user/new
   (fn [state data form-data]
     {:state {:loading? true}
      :http {:params [:user/new data]
             :on-load [:citrus/authenticate-ready true]
             :on-error [:user/new-error form-data]}})

   :user/new-error
   (fn [state form-data reply]
     {:state (->
              (cond
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
                state)
              (assoc :loading? false))})

   :user/login-ready
   (fn [state result]
     {:state {:loading? false
              :current (:user result)
              }
      :dispatch [:user/close-signin-modal?]})

   :citrus/authenticate-ready
   (fn [state new? result]
     (let [user (:user result)]
       {:state (-> state
                   (assoc-in [:user :loading?] false)
                   (assoc-in [:user :current] user))
        :redirect {:handler :home}}))

   :user/update
   (fn [state data]
     {:state {:loading? (if (contains? (set (keys data))
                                       :email_notification)
                          false
                          true)}
      :http  {:params   [:user/update data]
              :on-load  [:user/update-success data]}})

   :user/update-success
   (fn [state data result]
     {:state (-> state
                 (assoc :loading? false)
                 (update :current merge result))
      :dispatch [:notification/add :success (t :profile-updated)]})

   :user/logout
   (fn [state]
     ;; clear caches
     (caches/clear
      #(set! (.-location js/window) "/logout"))
     ;; unregister web worker
     {:state {:current nil}})

   :citrus/set-locale
   (fn [state locale]
     (reset! dicts/locale locale)
     {:state {:locale locale}
      :cookie [:set-forever "locale" (name locale)]})

   :citrus/clear-caches
   (fn [state]
     (caches/clear nil)
     {:state state})

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
       (assoc :dispatch [:citrus/re-fetch :reports {}])))

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

   :user/set-default-post-language
   (fn [state value]
     {:state {:default-post-language value}
      :cookie [:set-forever "default-post-language" value]})

   :user/star
   (fn [state data]
     (let [current-user (:current state)]
       (cond
         current-user
         {:state {:loading? true}
          :http {:params [:user/star data]
                 :on-load [:citrus/star-success data]}}

         :else
         {:state {:signin-modal? true}})))

   :citrus/star-success
   (fn [state data result]
     {:state (-> state
                 (assoc-in [:user :loading?] false)
                 (assoc-in [:user :current]
                           (:current result)))})

   :user/unstar
   (fn [state data]
     (let [current-user (:current state)]
       (cond
         current-user
         {:state {:loading? true}
          :http {:params [:user/unstar data]
                 :on-load [:citrus/unstar-success data]}}

         :else
         {:state {:signin-modal? true}})))

   :citrus/unstar-success
   (fn [state data result]
     {:state (-> state
                 (assoc-in [:user :loading?] false)
                 (assoc-in [:user :current]
                           (:current result)))})

   ;; tags
   :user/follow
   (fn [state tag]
     (if (:current state)
       (let [old-tags (get-in state [:current :followed_tags])
            new-tags        (vec (distinct (conj old-tags tag)))]
        {:state (assoc-in state [:current :followed_tags] new-tags)
         :dispatch [:user/update {:followed_tags new-tags}]})
       {:state state
        :dispatch [:user/update {:followed_tags []}]}))

   :user/unfollow
   (fn [state tag]
     (if (:current state)
       (let [old-tags (get-in state [:current :followed_tags])
             new-tags (vec (remove #(= tag %) old-tags))]
        {:state (assoc-in state [:current :followed_tags] new-tags)
         :dispatch [:user/update {:followed_tags new-tags}]})
       {:state state
        :dispatch [:user/update {:followed_tags []}]}))
   })
