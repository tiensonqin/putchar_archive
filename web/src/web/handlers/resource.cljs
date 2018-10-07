(ns web.handlers.resource
  (:require [share.dicts :refer [t]]
            [share.util :as util]
            [share.config :as config]
            [appkit.macros :refer [oget oset!]]
            [share.dommy :as dommy]
            [clojure.string :as str]
            [goog.dom :as gdom]
            [web.scroll :as scroll]))

(def handlers
  {:resource/new
   (fn [state data form-data]
     {:state (assoc state :loading? true)
      :http {:params [:resource/new data]
             :on-load :resource/new-ready
             :on-error [:resource/new-error form-data]}})

   :resource/new-ready
   (fn [state result]
     (util/set-href! (str config/website "/"
                          (:object_type result)
                          "/"
                          (:object_id result)))
     {:state state})

   :resource/new-error
   (fn [state form-data reply]
     {:state (->
              (cond
                (and (= (:status reply) 400)
                     (= (get-in reply [:body :message]) ":resource-title-exists"))
                (do
                  (swap! form-data assoc-in [:validators :title] false)
                  {:title-taken? true})

                :else
                state)
              (assoc :loading? false))})

   :resource/update
   (fn [state data form-data]
     {:state (assoc state :loading? true)
      :http {:params [:resource/update data]
             :on-load :resource/update-ready
             :on-error [:resource/update-error form-data]}
      })

   :resource/update-ready
   (fn [state result]
     (util/set-href! (str config/website "/"
                          (:object_type result)
                          "/"
                          (:object_id result)))
     {:state state})

   :resource/update-error
   (fn [state form-data reply]
     {:state (->
              (cond
                (and (= (:status reply) 400)
                     (= (get-in reply [:body :message]) ":resource-title-exists"))
                (do
                  (swap! form-data assoc-in [:validators :title] false)
                  {:title-taken? true})

                :else
                state)
              (assoc :loading? false))})

   :citrus/load-more-books
   (fn [state {:keys [last]}]
     {:state state
      :dispatch [:query/send (get-in state [:router :handler])
                 {:q {:books {:fields [:*]}}
                  :args {:books {:cursor {:after (:flake_id last)}}}}
                 true]})

   :citrus/load-more-papers
   (fn [state {:keys [last]}]
     {:state state
      :dispatch [:query/send (get-in state [:router :handler])
                 {:q {:papers {:fields [:*]}}
                  :args {:papers {:cursor {:after (:flake_id last)}}}}
                 true]})

   ;; :resource/delete
   })
