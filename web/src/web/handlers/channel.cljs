(ns web.handlers.channel
  (:require [share.util :as util]
            [share.config :as config]))

(def handlers
  {:channel/new
   (fn [state data]
     {:state {:loading? true}
      :http {:params [:channel/new data]
             :on-load :citrus/channel-new-ready}})

   :citrus/channel-new-ready
   (fn [state result]
     (let [current-group (get-in state [:user :current :stared_groups (:group_id result)])]
       {:state (-> state
                   (assoc-in [:channel :loading?] false)
                   (update-in [:user :current :stared_groups]
                              (fn [groups]
                                (update-in groups [(:group_id result) :channels]
                                           (fn [channels]
                                             (vec (distinct (conj channels result)))))))
                   (update-in [:user :current :stared_channels]
                              (fn [old]
                                (if (:id result)
                                  (vec (distinct (conj old result)))
                                  old))))
        :redirect {:handler :channel
                   :route-params {:group-name (:name current-group)
                                  :channel-name (:name result)}}}))
   :channel/reset
   (fn [state]
     {:state {:current nil}})

   :channel/delete
   (fn [state group id]
     {:http {:params [:channel/delete {:id id}]
             :on-load [:citrus/channel-delete-ready group id]}
      :state (assoc state :loading? true)})

   :citrus/channel-delete-ready
   (fn [state group id result]
     {:state (-> state
                 (assoc-in [:channel :loading?] false)
                 (update-in [:group :by-name (:name group) :channels]
                            (fn [xs]
                              (filter #(= id (:id %)) xs))))})

   :channel/update
   (fn [state group channel data]
     {:http {:params [:channel/update data]
             :on-load [:citrus/channel-update-ready group channel data]}
      :state (assoc state :loading? true)})

   :citrus/channel-update-ready
   ;; refresh
   (fn [state group channel data result]
     (util/set-href! (str config/website "/"
                          (:name group)
                          "/"
                          (or (:name data)
                              (:name channel))))
     {:state state})})
