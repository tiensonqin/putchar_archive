(ns api.db.search2
  (:require [api.config :refer [config]])
  (:import [java.security Security]
           [com.algolia.search ApacheAPIClientBuilder]))
;; index settings

;; from algolia, https://www.algolia.com/doc/api-client/java/getting-started/
;; WARNING: The JVM has an infinite cache on successful DNS resolution. As our hostnames points to multiple IPs, the load could be not evenly spread among our machines, and you might also target a dead machine.

;; You should change this TTL by setting the property networkaddress.cache.ttl. For example to set the cache to 60 seconds:
(Security/setProperty "networkaddress.cache.ttl" "60")

(def index-settings
  {:posts [[:id :string]
           [:user_screen_name :string]
           [:title :string]
           [:body :string]
           [:tops :int]
           [:rank :float]
           [:permalink :string]
           [:created_at :date]
           [:lang :string]
           [:cover :string]
           [:tags :string]
           [:book_id :int]
           [:book_title :string]]
   :books [[:id :int]
           [:screen_name :string]
           [:title :string]
           [:description :string]
           [:cover :string]
           [:tags :string]
           [:stars :string]]
   :users [[:id :string]
           [:screen_name :string]
           [:github_handle :string]
           [:bio :string]]})

(defonce client
  (let [{:keys [application-id key]} (:algolia config)]
    (.build (ApacheAPIClientBuilder. application-id key))))

(defn init-index
  [index-name klass]
  (.initIndex client index-name klass))

(defonce users-index
  (init-index "putchar_users" (Class/forName "SearchUser")))

;; (Class/forName "SearchUser")

;; Index<Contact> index = client.initIndex("your_index_name", Contact.class);
