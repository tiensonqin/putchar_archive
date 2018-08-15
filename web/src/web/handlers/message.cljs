(ns web.handlers.message)

(def handlers
  {
   :message/set-ref
   (fn [state ref]
     {:state {:ref ref}})

   :message/focus
   (fn [state]
     (if (:ref state)
       (.focus (:ref state)))
     {:state state})
   })
