(ns ssr.page
  (:import [java.io ByteArrayOutputStream])
  (:require [hiccup.page :as h]
            [cognitect.transit :as t]
            [clojure.data.json :as json]
            [api.config :refer [config]]
            [api.handler.query :as query]
            [share.util :as util]
            [share.dicts :refer [t]]
            [clojure.java.io :as io]
            [rum.core :as rum]
            [share.version :refer [version]]
            [share.kit.ui :as ui]
            [share.kit.colors :as colors]
            [share.components.widgets :as widgets]
            [share.seo :as seo]
            [api.services.slack :as slack]))

;; encode state hash into Transit format
(defn state->str [state]
  (let [out (ByteArrayOutputStream.)]
    (t/write (t/writer out :json) state)
    (json/write-str (.toString out))))

(def style
  (memoize
   (fn [name]
     (if util/development?
       [:link {:rel "stylesheet"
               :href (str "/css/" name)}]
       (let [content (slurp (io/resource (str "public/" name)))]
         [:style { :type "text/css" :dangerouslySetInnerHTML { :__html content }}])))))

(defonce debug-state (atom nil))

(defn head
  [req handler zh-cn? seo-content seo-title seo-image]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:meta {:name "apple-mobile-web-app-capable"
           :content "yes"}]
   [:meta {:name "mobile-web-app-capable"
           :content "yes"}]
   [:meta {:http-equiv "X-UA-Compatible"
           :content "IE=edge"}]

   ;; google verification
   ;; [:meta {:name "google-site-verification"
   ;;         :content "DdPD50TQmEAnOV2hW2GT8cSYrsC4RP9KA-YESrKDgu8"}]

   ;; twitter
   [:meta {:name "twitter:card"
           :content "summary"}]

   [:meta {:name "twitter:description"
           :content seo-content}]

   [:meta {:name "twitter:site"
           :content "@lambdahackers"}]

   [:meta {:name "twitter:title"
           :content seo-title}]

   [:meta {:name "twitter:image:src"
           :content seo-image}]

   [:meta {:name "twitter:image:alt"
           :content seo-title}]

   ;; open graph
   [:meta {:property "og:title"
           :content seo-title}]

   [:meta {:property "og:type"
           :content (if (= handler :post)
                      "article"
                      "site")}]
   [:meta {:property "og:url"
           :content (str (:website-uri config) (:uri req))}]
   [:meta {:property "og:image"
           :content seo-image}]
   [:meta {:property "og:description"
           :content seo-content}]
   [:meta {:property "og:site_name"
           :content "lambdahackers"}]

   [:title seo-title]
   [:meta {:name "description"
           :content seo-content}]
   [:meta {:name "theme-color"
           :content "#FFFFFF"}]

   [:link
    {:href "/apple-touch-icon.png",
     :sizes "180x180",
     :rel "apple-touch-icon"}]

   [:link
    {:href "/favicon-32x32.png?v=3",
     :sizes "32x32",
     :type "image/png",
     :rel "icon"}]

   [:link
    {:href "/favicon-16x16.png?v=3",
     :sizes "16x16",
     :type "image/png",
     :rel "icon"}]

   [:link
    {:color "#5bbad5",
     :href "/safari-pinned-tab.svg",
     :rel "mask-icon"}]

   [:meta {:content "#1a1a1a", :name "msapplication-TileColor"}]
   [:meta {:content "#ffffff", :name "theme-color"}]

   [:link {:rel "manifest"
           :href "/manifest.json"}]
   (if util/development?
     [:link {:rel "stylesheet"
             :href "/css/style.css"}]
     [:link {:rel "stylesheet"
             :href (str "/style-" version ".css")}])
   (when-not zh-cn?
     [:link {:rel "stylesheet"
             :href "https://fonts.googleapis.com/css?family=Noto+Serif:400,400italic,700,700italic%7CUbuntu:400,400i,600,600i"}])
   ])

(defn status-template
  [text]
  (h/html5
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport"
               :content "width=device-width, initial-scale=1"}]
       [:meta {:name "apple-mobile-web-app-capable"
               :content "yes"}]
       [:meta {:name "mobile-web-app-capable"
               :content "yes"}]
       [:meta {:http-equiv "X-UA-Compatible"
               :content "IE=edge"}]

       [:link
        {:href "/apple-touch-icon.png",
         :sizes "180x180",
         :rel "apple-touch-icon"}]

       [:link
        {:href "/favicon-32x32.png?v=3",
         :sizes "32x32",
         :type "image/png",
         :rel "icon"}]

       [:link
        {:href "/favicon-16x16.png?v=3",
         :sizes "16x16",
         :type "image/png",
         :rel "icon"}]

       [:link
        {:color "#5bbad5",
         :href "/safari-pinned-tab.svg",
         :rel "mask-icon"}]

       [:meta {:content "#00a300", :name "msapplication-TileColor"}]
       [:meta {:content "#ffffff", :name "theme-color"}]]
      [:body {:style {:height "100%"
                      :width "100%"
                      :display "flex"
                      :flex-direction "column"
                      :margin 0
                      :background-size "cover"
                      :position "relative"
                      :align-items "center"
                      :justify-content "center"
                      :padding-right "0 !important"}}
       [:a {:style {:margin-top "24px"}
            :href "/"}
        [:img {:src "/images/logo.png"}]]
       [:h1 {:style {:margin-top "200px"}}
        text]]))

(defn render-page [content req state]
  (when util/development?
    (reset! debug-state state))

  (if (:not-found state)
    (status-template "404 Not Found")

    (let [locale (:locale state)
         {:keys [handler route-params]} (:ui/route req)
         current-user (get-in state [:user :current])
         [seo-title seo-content seo-image] (seo/seo-title-content handler route-params state)
         zh-cn? (= locale :zh-cn)]
     (h/html5
      (head req handler zh-cn? seo-content seo-title seo-image)
      [:body {:style {:min-height "100%"}}
       [:div#app content]
       [:link {:rel "preload"
               :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"
               :as "style"
               :onload "this.onload=null;this.rel='stylesheet'"}]

       [:noscript
        [:link {:rel "stylesheet"
                :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
        (when-not zh-cn?
          [:link {:rel "stylesheet"
                  :href "https://fonts.googleapis.com/css?family=Noto+Serif:400,400italic,700,700italic%7CUbuntu:400,400i,600,600i"}])]
       [:script "!function(t){\"use strict\";t.loadCSS||(t.loadCSS=function(){});var e=loadCSS.relpreload={};if(e.support=function(){var e;try{e=t.document.createElement(\"link\").relList.supports(\"preload\")}catch(t){e=!1}return function(){return e}}(),e.bindMediaToggle=function(t){function e(){t.media=a}var a=t.media||\"all\";t.addEventListener?t.addEventListener(\"load\",e):t.attachEvent&&t.attachEvent(\"onload\",e),setTimeout(function(){t.rel=\"stylesheet\",t.media=\"only x\"}),setTimeout(e,3e3)},e.poly=function(){if(!e.support())for(var a=t.document.getElementsByTagName(\"link\"),n=0;n<a.length;n++){var o=a[n];\"preload\"!==o.rel||\"style\"!==o.getAttribute(\"as\")||o.getAttribute(\"data-loadcss\")||(o.setAttribute(\"data-loadcss\",!0),e.bindMediaToggle(o))}},!e.support()){e.poly();var a=t.setInterval(e.poly,500);t.addEventListener?t.addEventListener(\"load\",function(){e.poly(),t.clearInterval(a)}):t.attachEvent&&t.attachEvent(\"onload\",function(){e.poly(),t.clearInterval(a)})}\"undefined\"!=typeof exports?exports.loadCSS=loadCSS:t.loadCSS=loadCSS}(\"undefined\"!=typeof global?global:this);"]

       [:script {:src (if util/development?
                        "/js/compiled/main.js"
                        (str "/main-" version ".js"))}]
       [:script
        (str "web.core.init(" (state->str state) ")")]

       [:script
        (format
         "
// Check that service workers are registered
if ('serviceWorker' in navigator) {
  // Use the window load event to keep the page load performant
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw%s.js');
  });
}
"
         (if util/development?
           "_dev"
           ""))]

       ;; Google analytics
       (when-not zh-cn?
         [:script {:async true
                   :defer true
                   :src "https://www.googletagmanager.com/gtag/js?id=UA-123974000-1"}])

       (when-not zh-cn?
         [:script "window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());

  gtag('config', 'UA-123974000-1');"])
       ]))))
