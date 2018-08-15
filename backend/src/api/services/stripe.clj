(ns api.services.stripe
  (:require [org.httpkit.client :as client]
            [api.config :refer [config]]
            [cheshire.core :as cheshire]))

(def api-url "https://api.stripe.com/v1/")

(defn api-call [method endpoint & [params]]
  @(client/request (merge {:method method
                           :url (str api-url endpoint)
                           :basic-auth [(:stripe-secret-key config) ""]
                           :headers {"Content-type" "application/json"
                                     "Accept" "application/json"}}
                          params)))

(defn- ->body
  [resp]
  (some-> (:body resp)
          (cheshire/parse-string true)))

(defn ensure-plans
  []
  (let [{:keys [status body]} (api-call :post
                                        "plans"
                                        {:form-params
                                         {:id "pro-member"
                                          :amount 99 ; $0.99
                                          :currency "usd"
                                          :interval "month"
                                          "product[name]" "Pro member subscription"}})]
    (let [body (cheshire/parse-string body)]
      (if (= "resource_already_exists"
             (get-in body ["error" "code"]))
        (prn "pro-member plan already exists in Stripe.")
        body))))

;; plan, source, email
(defn create-customer
  [{:keys [plan source email]
    :as params}]
  (api-call :post "customers" {:form-params params}))

;; customer description
(defn create-invoice [{:keys [customer description]
                       :as params}]
  (api-call :post "invoices" {:form-params params}))

(defn get-customer
  [customer-id]
  (api-call :get (str "customers/" customer-id)))

(defn get-subscription
  [customer-id subscription-id]
  (api-call :get (str "customers/" customer-id "/subscriptions/" subscription-id)))

(defn get-event [event-id]
  (api-call :get (str "events/" event-id)))
