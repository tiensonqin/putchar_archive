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
                     (= (get-in reply [:body :message]) ":resource-name-exists"))
                (do
                  (swap! form-data assoc-in [:validators :name] false)
                  {:name-taken? true})

                :else
                state)
              (assoc :loading? false))})

   ;; :resource/edit
   ;; :resource/delete
   })
