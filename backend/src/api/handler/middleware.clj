(ns api.handler.middleware
  (:require [taoensso.timbre :as timbre]
            ;; [manifold.deferred :as d]
            [ring.middleware.cors :as cors]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.middleware.format-params :refer [wrap-restful-params]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ssr.middleware.etag :refer [wrap-etag]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [share.routes :refer [routes]]
            [ring.util.response :as resp]
            [api.util :as util]
            [api.db.util :as du]
            [api.db.stat :as stat]
            [clojure.string :as str]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [api.cookie :as cookie]
            [api.jwt :as jwt]
            [api.db.user :as u]
            [clojure.java.jdbc :as j]
            [clojure.set :as set]
            [api.services.slack :as slack]
            [api.config :as config]
            [share.config :as share-config]))

(defn inject-context [f context]
  (fn [req]
    (f
     (-> req
         (update :context merge context)))))

;; TODO: rate limit
(def mutation-whitelist
  #{:group/search :post/search :data/pull-emojis
    :post/read
    :user/new :auth/email
    :auth/request-code})

(def query-whitelist
  #{:user :group :groups :post :login :posts :tag :user-tag})

(defn- auth-whitelist?
  [request]
  (or
   ;; options
   (= (:request-method request) :options)

   ;; mutation whitelist
   (contains? mutation-whitelist (first (:body-params request)))

   (and (= (:uri request) "/query")
        (set/superset? query-whitelist (set (keys (:q (:body-params request))))))))

(defn authenticated?
  [request]
  (let [whitelist? (auth-whitelist? request)]
    (let [tokens (cookie/get-token request)
          {:keys [access-token refresh-token]} tokens]
      (if access-token
        (try
          (when-let [user (jwt/unsign access-token)]
            (assoc-in request [:context :uid] (some-> (:id user) util/->uuid)))
          (catch Exception e
            (if (= :exp (:cause (ex-data e)))
              [:expired tokens]
              (if whitelist? request false))))
        (if whitelist? request false)))))

(defn wrap-authenticate [handler]
  (fn [request]
    (let [ret (authenticated? request)]
      (cond
        (false? ret)
        {:status 401
         :body {:message "Invalid token"}}

        (and (vector? ret) (= :expired (first ret)))
        ;; generate new token
        (let [user (jwt/unsign-skip-validation (:access-token (second ret)))
              uid (some-> (:id user) util/->uuid)]
          (if uid
            (j/with-db-connection [conn (get-in request [:context :datasource])]
              (-> (handler request)
                  (assoc :cookies (u/generate-tokens conn {:id uid}))))
            {:status 401
             :body {:message "Invalid token"}
             :cookies cookie/delete-token}))

        :else
        (handler ret)))))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (slack/error e request)
           (util/bad (.getCause e))))))

(defn api-wrap-stats [handler]
  (fn [request]
    (when-let [permalink (get-in request [:params :args :post :permalink])]
      ;; get permalink from post
      (j/with-db-connection [conn (get-in request [:context :datasource])]
        (when-let [post-id (du/select-one-field conn :posts {:permalink permalink} :id)]
          ;; views
          (stat/create conn post-id "view" (:remote-addr request)))))
    (handler request)))


(defn ssr-wrap-stats [handler]
  (fn [request]
    (let [{:keys [handler route-params]} (:ui/route request)]
      (when (and (= handler :post)
                 (:screen_name route-params)
                 (:permalink route-params))
        (let [permalink (str "@" (:screen_name route-params) "/" (:permalink route-params))]
          ;; get permalink from post
          (j/with-db-connection [conn (get-in request [:context :datasource])]
            (when-let [post-id (du/select-one-field conn :posts {:permalink permalink} :id)]
              ;; views
              (stat/create conn post-id "view" (:remote-addr request)))))))
    (handler request)))

;; TODO: Access-Control-Max-Age
(defn custom-wrap-cors [handler]
  (let [access-control (cors/normalize-config [:access-control-allow-origin [(re-pattern (:website-uri config/config))]
                                               :access-control-allow-methods [:get :put :post :delete :options :patch]
                                               :access-control-allow-credentials "true"])]
    (fn [request]
      (if (and (cors/preflight? request) (cors/allow-request? request access-control))
        (let [blank-response {:status 200
                              :headers {}
                              :body "preflight complete"}]
          (cors/add-access-control request access-control blank-response))
        (if (cors/origin request)
          (if (cors/allow-request? request access-control)
            (let [response (handler request)]
              (cors/add-access-control request access-control response)))
          (handler request))))))

(defn wrap-production-etag [handler]
  (fn [req]
    (if-not share-config/development?
      ((wrap-etag handler) req)
      (handler req))))

(defn wrap-production-gzip [handler]
  (fn [req]
    (if share-config/development?
      (handler req)
      ((wrap-gzip handler) req))))

(defn middlewares
  [app context]
  (-> app
      (wrap-reload)
      (wrap-exception)
      (wrap-authenticate)
      (wrap-defaults api-defaults)
      (wrap-multipart-params)
      (wrap-restful-format)
      (wrap-cookies)
      (custom-wrap-cors)
      (inject-context context)
      (api-wrap-stats)
      (wrap-production-etag)
      (wrap-production-gzip)))
