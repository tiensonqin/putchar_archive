(ns web.handlers.search
  (:require [share.emoji :as emoji]
            [clojure.string :as str]))

(def handlers
  {
   ;; search group {:q {:group_name "xxx"} :limit 10}
   ;; search posts {:q {:post_title "xxx"} :limit 10}
   :search/q
   (fn [state q]
     {:state {:q q}})

   :search/search
   (fn [state api-path q]
     {:state {:loading? true
              :q (let [q (:q q)]
                   (or (:group_name q)
                      (:post_title q)
                      (:screen_name q)))
              :result nil}
      :http {:params [api-path q]
             :on-load :search/ready}})

   :search/ready
   (fn [state result]
     {:state (-> state
                 (assoc :loading? false)
                 (assoc :result result))})

   :search/reset
   (fn [state]
     {:state {:loading? nil
              :result nil
              :q nil}})

   :search/reset-result
   (fn [state]
     {:state (-> state
                 (assoc :loading? nil)
                 (assoc :result nil))})

   :data/pull-emojis
   (fn [state]
     {:state state
      :http {:params [:data/pull-emojis]
             :keywordize? false
             :on-load :data/pull-emojis-ready}})

   :data/pull-emojis-ready
   (fn [state result]
     (reset! emoji/emojis result)
     {:state state
      :local-storage {:action :set
                      :key :emojis
                      :value result}})

   :search/emojis
   (fn [state q]
     {:state {:emojis-result (if (str/blank? q)
                               ;; thumbs up, thumbs down, 100
                               emoji/default-5-emojis
                               (take 5 (emoji/search q)))}})})
