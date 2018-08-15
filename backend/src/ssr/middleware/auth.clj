(ns ssr.middleware.auth
  (:require [api.cookie :as cookie]
            [api.jwt :as jwt]
            [api.util :as util]
            [api.db.user :as u]
            [api.db.util :as du]
            [clojure.java.jdbc :as j]
            [ring.util.response :as resp]
            [api.services.slack :as slack]
            ;; [clojure.repl]
            [clojure.set :as set]
            [share.routes :refer [login-routes]]
            ))

;; route-handler
(def query-whitelist
  #{:user :group :groups :channel :channels :post :posts})

(defn- auth-blocked?
  [request]
  (or
   (contains? login-routes (get-in request [:ui/route :handler]))

   (and
    (= (:uri request) "/query")
    (not (set/superset? query-whitelist (set (keys (:q (:body-params request)))))))))

(defn wrap-auth
  [handler]
  (fn [req]
    (let [tokens (cookie/get-token req)
          {:keys [access-token refresh-token]} tokens]
      (j/with-db-connection [conn (get-in req [:context :datasource])]
        (if access-token
         (try
           (let [user (jwt/unsign access-token)
                 uid (some-> (:id user) util/->uuid)]
             (when uid (u/update conn uid {:last_seen_at (du/sql-now)}))
             (-> (assoc-in req [:context :uid] uid)
                 (handler)))
           (catch Exception e
             (if (= :exp (:cause (ex-data e)))
               (let [user (jwt/unsign-skip-validation access-token)
                     uid (some-> (:id user) util/->uuid)]
                 (-> (assoc-in req [:context :uid] uid)
                     (handler)
                     (assoc :cookies (u/generate-tokens conn {:id uid}))))
               (do
                 (slack/error e)
                 (resp/redirect "/error.html")))))

         (if (auth-blocked? req)
           {:status 401
            :body {:message "unauthorized"}}
           (handler req)))))))
