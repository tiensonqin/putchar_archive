(ns web.handlers.query
  (:require [share.merge :as merge]
            [share.util :as util]
            [share.query :as query]
            [share.seo :as seo]))

(defn mergef
  [q state route-handler result]
  (reduce (fn [state [k _]]
            (merge/mergef
             state
             route-handler
             q
             result
             k))
          state
          (if (:merge q)
            (:merge q)
            {route-handler true}))

  )

(defonce latest-qid (atom nil))

(def handlers
  {:citrus/send-query
   (fn [state route-handler query args]
     (if-let [q (query state args)]
       {:state state
        :dispatch [:query/send route-handler q]}
       {:state state}))

   :query/send
   (fn [state route-handler q scroll?]
     (let [qid (util/md5-query q)
           last-reply-at (get-in state [:cache qid :last-reply-at])
           just-requested? (and last-reply-at
                                (< (- (util/get-time) last-reply-at) (* 10 60 1000)))]
       (reset! latest-qid qid)
       (cond
         (and just-requested? (get-in state [:cache qid :result]))
         {:state state
          :dispatch [:citrus/cache-request q route-handler (get-in state [:cache qid :result])]}

         :else
         {:state (cond->
                   (assoc state :q q)

                   scroll?
                   (assoc-in [:scroll-loading? route-handler] true)

                   (not (false? (:clear? q)))
                   (assoc-in [:result route-handler] nil))

          :http {:endpoint "query"
                 :params (dissoc q :merge :clear?)
                 :on-load [:citrus/query-success route-handler q scroll?]
                 :on-error [:citrus/query-failed route-handler q]}})))

   :citrus/cache-request
   (fn [state q route-handler cache-result]
     {:state (mergef q state route-handler cache-result)})

   :citrus/query-success
   (fn [state route-handler q scroll? result]
     (util/debug {:handler route-handler
                  :query q
                  :qid (util/md5-query q)})
     (let [new-state (cond-> (assoc-in state
                                       [:query :cache (util/md5-query q)]
                                       {:last-reply-at (util/get-time)
                                        :result result})
                       scroll?
                       (assoc-in [:query :scroll-loading? route-handler] false))
           new-state (mergef q new-state route-handler result)]

       ;; set title
       (let [[title description picture] (seo/seo-title-content route-handler
                                                                (get-in state [:router :route-params])
                                                                new-state)]
         (util/set-title! title))
       {:state new-state}
       ))

   :citrus/query-failed
   (fn [state route-handler reply]
     (prn "query failed: " {:route-handler route-handler
                            :reply reply})
     {:state state})

   :citrus/clean-cache
   (fn [state route-handler params]
     (let [qid (util/md5-query ((get query/queries route-handler) state params))]
       {:state (update-in state [:query :cache] dissoc qid)}))

   :citrus/re-fetch
   (fn [state route-handler params]
     (let [q ((get query/queries route-handler) state params)
           qid (util/md5-query q)]
       {:state (-> state
                   (update-in [:query :cache] dissoc qid)
                   (assoc :posts nil
                          :comments nil))
        :dispatch [:query/send route-handler q]}))

   :citrus/update-cache
   (fn [state route-handler params ks data]
     (let [qid (util/md5-query ((get query/queries route-handler) state params))]
       {:state (update-in state (concat [:query :cache qid] ks) merge data)}))

   :query/into-back-mode
   (fn [state]
     {:state {:back-mode? true}
      :timeout {:duration 1000
                :events [:query/leave-back-mode]}})

   :query/leave-back-mode
   (fn [state]
     {:state {:back-mode? false}})

   :citrus/cache-server-first-reply
   (fn [{:keys [query initial-query-result] :as state}]
     (if-let [q (:q query)]
       {:state (-> state
                   (assoc-in [:query :cache
                              (util/md5-query (select-keys query [:q :args :merge]))]
                             {:last-reply-at (util/get-time)
                              :result initial-query-result}))}
       {:state state}))})
