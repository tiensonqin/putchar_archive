(ns web.stripe
  (:require [cljs.core.async :as async]
            [web.loader :as loader]
            [share.config :as config]
            [appkit.promise :as promise]
            [appkit.macros :refer [oset! oget]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; (def stripe-js "https://js.stripe.com/v3/")
(def checkout-js "https://checkout.stripe.com/checkout.js")

(defn checkout-config [token-callback close-callback email]
  {:key config/stripe-publishable-key
   :token token-callback
   :name "Pay Securely"
   :locale "auto"
   :amount "99"
   :description "Secured and Encrypted by Stripe"
   :panelLabel "Subscribe for"
   :email email
   :closed close-callback})

(defn open-checkout [email token-callback close-callback & [extra-config]]
  (go
    ;; (when-not js/window.StripeCheckout
    ;;   (async/<! (loader/load checkout-js)))
    (-> (checkout-config token-callback
                         close-callback
                         email)
      (merge extra-config)
      clj->js
      (js/window.StripeCheckout.configure)
      (.open))))

;; (defn create-source
;;   [m]
;;   (go
;;     (when-not js/window.Stripe
;;       (async/<! (loader/load stripe-js)))
;;     (let [stripe ^js (js/Stripe. config/stripe-publishable-key)]
;;       (-> (.createSource stripe
;;                         (clj->js m))
;;           (promise/then (fn [result]
;;                           (let [url (aget result "source" "redirect" "url")]
;;                            (oset! js/window.location "href" url))))))))

;; (create-source {:type "alipay"
;;                 :amount 299
;;                 :currency "usd"
;;                 :redirect {:return_url "http://localhost:3401"}
;;                 :metadata {:email "abc@lambdahackers.com"}})
