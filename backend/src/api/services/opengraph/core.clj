(ns api.services.opengraph.core
  (:require [net.cgrand.enlive-html :as html :refer [attr=]]
            [clojure.string :as s]
            [org.httpkit.client :as http]
            [api.services.slack :as slack]))

(defonce user-agent
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36")

(defonce connection-timeout 3000)

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn attrs
  [node selector ks]
  (map (fn [node]
         (-> (:attrs node)
             (select-keys ks)
             (vals)
             (first)))
    (html/select node selector)))

(defn get-attrs
  [col ks]
  (map (fn [node]
         (-> (:attrs node)
             (select-keys ks)))
    col))

(defn get-entities
  [node selector k]
  (map (fn [node]
         (prn node)
         (if-let [entity (get node k)]
           (if (coll? entity)
             (first entity)
             entity)
           (-> (:attrs node)
               k)))
    (html/select node selector)))

(defn get-attr
  ([node selector]
   (get-attr node selector :content))
  ([node selector k]
   (let [selector (if (coll? selector) selector [selector])]
     (-> (get-entities node selector k)
         first))))

;; Attributes include:
;; title, description, image

;; image could be cover or logo

;; (def url-rules
;;   {#"" :medium
;;    #"" :github
;;    #"" :amazon})

;; (def website-rules
;;   {:medium {:title selector-or-regex
;;             :description selector-or-regex
;;             :image selector-or-regex}})

;; (defn any-rule
;;   [url]
;;   )

(defn safe-trim
  [s]
  (if (and s (string? s))
    (s/trim s)))

(defn first-match
  [node & selectors]
  (when (and node (seq selectors))
    (->
     (some (fn [selector]
             (if (map? selector)
               (get-attr node
                         (:selector selector)
                         (:attr selector))
               (get-attr node selector)))
           selectors)
     (safe-trim))))

(defn meta-name
  [name]
  [[:meta (attr= :name name)]])

(defn meta-property
  [property]
  [[:meta (attr= :property property)]])

(defn meta-itemprop
  [prop]
  [[:meta (attr= :itemprop prop)]])

(defn get-root-url
  [url]
  (let [url (java.net.URL. url)]
    (str (.getProtocol url)
         "://"
         (.getHost url))))

(defn default-parser
  [url node]
  (when node
    {:url url
     :title (first-match node
                         [:title]
                         (meta-property "og:title")
                         (meta-name "twitter:title")
                         [:.post-title]
                         [:.entry-title]
                         [:h1])
     :description (first-match node
                               (meta-property "og:description")
                               (meta-name "description")
                               (meta-name "twitter:description")
                               [:#description]
                               [:p])
     :image (when-let [image (first-match node
                                        (meta-property "og:image:secure_url")
                                        (meta-property "og:image:url")
                                        (meta-property "og:image")
                                        (meta-name "twitter:image:src")
                                        (meta-name "twitter:image")
                                        (meta-itemprop "image")
                                        {:selector [:article :img]
                                         :attr :src}
                                        {:selector [:#content :img]
                                         :attr :src}
                                        {:selector [:img (attr= :alt "author")]
                                         :attr :src}
                                        ;; {:selector [:img]
                                        ;;  :attr :src}
                                        )]
              (if (= \/ (first image))
                (str (get-root-url url) image)
                image))}))

(defn parse
  "Given a url, return it's open graph metadata."
  [url ok-handler error-handler]
  ;; DONE: filter url
  ;; DONE: http request timeout
  ;; TODO: retry
  (when url
    (try
      (let [test-url (java.net.URL. url)]
        (http/get url {:headers {"User-Agent" user-agent}
                       :as :stream
                       :timeout connection-timeout}
                  (fn [{:keys [status headers body error]}] ;; asynchronous response handling
                    (try
                      (if error
                        (error-handler error)

                        (when-let [node (html/html-resource body)]
                          (ok-handler (default-parser url node))))
                      (catch Exception e
                        (error-handler e))))))
      (catch Exception e
        (error-handler e)))))

(comment
  (def node (fetch-url "https://github.com/cgrand/enlive"))

  (get-attr node :title))
