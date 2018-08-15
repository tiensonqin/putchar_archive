(ns web.handlers.notification)

(def handlers
  {
   ;; top notification
   :notification/add
   (fn [state type body]
     {:state {:type type
              :body body}
      :timeout {:duration 2000
                :events [:notification/clear]}})

   :notification/clear
   (fn [state]
     {:state (assoc state
                    :type nil
                    :body nil)})})
