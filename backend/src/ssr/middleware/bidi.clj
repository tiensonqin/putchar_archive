(ns ssr.middleware.bidi
  (:require [bidi.bidi :as bidi]
            [share.routes :as routes]))

(defn wrap-bidi [handler routes]
  (fn [req]
    (if-let [route (routes/match-route-with-query-params (cond-> (:uri req)
                                                           (:query-string req)
                                                           (str "?" (:query-string req))))]
      (-> req
          (assoc :ui/route route)
          handler)
      (handler req))))
