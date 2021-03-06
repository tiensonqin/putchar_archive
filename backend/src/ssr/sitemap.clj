(ns ssr.sitemap
  (:require
   [clojure.data.xml :as xml]
   [share.config :as config]
   [clojure.java.jdbc :as j]
   [share.util :as util]))

(defonce hostname config/website)

(def emit (comp xml/indent-str xml/sexp-as-element))

(defn sitemap [db]
  (->
   (j/with-db-connection [conn db]
     (let [posts (j/query db ["select permalink from posts order by created_at desc limit 10000"])
           users (j/query db ["select screen_name from users order by created_at asc limit 10000"])]
       [:urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
        [:url {} [:loc {} hostname]]
        ;; users
        (for [{:keys [screen_name]} users]
          [:url {} [:loc {} (format "%s/@%s" hostname screen_name)]])
        ;; posts
        (for [{:keys [permalink]} posts]
          (when permalink
            [:url {} [:loc {} (format "%s/%s" hostname permalink)]]))]))
   (emit)))
