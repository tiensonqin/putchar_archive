(ns web.handlers.report
  (:require [share.dicts :refer [t]]))

(def handlers
  {:report/new
   (fn [state data]
     {:state {:loading? true}
      :http {:params [:report/new data]
             :on-load [:report/new-success data]}})

   :report/new-success
   (fn [state data result]
     {:state {:loading? false}
      :dispatch [:notification/add :success (t :report-sent)]})

   :report/receive-new
   (fn [state data]
     {:state {:new? true}})

   :citrus/report-ignore
   (fn [state report]
     (let [reports (remove #(= (:id report) (:id %)) (:reports state))
           end? (zero? (count reports))]
       {:state (cond->
                 {:reports reports}
                 end?
                 (assoc-in [:report :new?] false))
        :http {:params [:report/ignore report]
               :on-load :citrus/report-ignore-success}}))

   :citrus/report-ignore-success
   (fn [state result]
     {:state state})

   :citrus/report-delete
   (fn [state report]
     {:state state
      :http {:params [:report/delete-object report]
             :on-load [:citrus/report-delete-success report]
             :on-error [:citrus/report-delete-failed report]}})

   :citrus/report-delete-success
   (fn [state report result]
     ;; open next user modal
     {:state (assoc-in state [:report :next-user-dialog?] true)})

   :citrus/report-delete-failed
   ;; notification
   (fn [state report error]
     ;; open next user modal
     {:state state})

   :report/close-user-dialog?
   (fn [state]
     {:state {:next-user-dialog? false}})

   :report/open-delete-dialog?
   (fn [state]
     {:state {:delete-dialog? true}})

   :report/close-delete-dialog?
   (fn [state]
     {:state {:delete-dialog? false}})

   :report/user-action
   (fn [state data]
     {:state state
      :http {:params [:report/user-action data]
             :on-load [:citrus/report-user-action-success (:report data)]
             :on-error [:report/user-action-failed (:report data)]}})

   :citrus/report-user-action-success
   (fn [state report result]
     {:state (let [reports (remove #(= (:id report) (:id %)) (:reports state))
                   end? (zero? (count reports))]
               (cond->
                 (-> state
                     (assoc-in [:report :next-user-dialog?] false)
                     (assoc-in [:report :delete-dialog?] false)
                     (assoc :reports reports))
                 end?
                 (assoc-in [:report :new?] false)))})

   :report/user-action-failed
   (fn [state report error]
     ;; open next user modal
     {:state (assoc state :error error)})})
