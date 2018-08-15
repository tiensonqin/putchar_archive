(ns share.asciidoc
  (:require #?(:clj [api.asciidoc])
            #?(:cljs [cljs.core.async :as async])
            #?(:cljs [goog.net.jsloader :as jsloader])
            #?(:cljs [goog.html.TrustedResourceUrl :as url])
            #?(:cljs [goog.string.Const :as sc])
            #?(:cljs [web.loader :as loader])
            [share.config :as config]
            [clojure.string :as s])
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])))


(defn ascii-loaded? []
  #?(:cljs js/window.Asciidoctor
     :clj true))

(defn load-ascii []
  #?(:cljs
     (loader/load (str config/website "/asciidoctor.min.js"))))

(defn render [str]
  #?(:clj
     (api.asciidoc/render str)
     :cljs
     (if (ascii-loaded?)
       (.convert (js/window.Asciidoctor.) str (clj->js {:attributes {:showTitle true
                                                                     :hardbreaks true
                                                                     :icons "font"
                                                                     :source-highlighter "highlightjs"}}))
       (prn "error"))))
