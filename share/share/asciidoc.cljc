(ns share.asciidoc
  (:require #?(:clj [api.asciidoc])
            #?(:cljs [cljs.core.async :as async])
            #?(:cljs [goog.net.jsloader :as jsloader])
            #?(:cljs [goog.html.TrustedResourceUrl :as url])
            #?(:cljs [goog.string.Const :as sc])
            #?(:cljs [web.loader :as loader])
            #?(:cljs [asciidoctor.js :as asciidoctor])
            [share.config :as config]
            [clojure.string :as s])
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])))


(defn render [str]
  #?(:clj
     (api.asciidoc/render str)
     :cljs
     (.convert (asciidoctor) str (clj->js {:attributes {:showTitle true
                                                                     :hardbreaks true
                                                                     :icons "font"
                                                                     :source-highlighter "highlightjs"}}))))
