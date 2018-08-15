(ns share.emoji
  (:refer-clojure :exclude [get])
  (:require [share.prefix-search :as search]
            #?(:clj [clojure.java.io :as io])))

;; emoji:heart[lg]

;; SIZE_MAP = {'1x' => 17, 'lg' => 24, '2x' => 34, '3x' => 50, '4x' => 68, '5x' => 128}
;; SIZE_MAP.default = 24
;; example from https://github.com/asciidoctor/asciidoctor-extensions-lab/blob/master/lib/emoji-inline-macro/sample.adoc

(defonce size-map {"1x" 17
                   "lg" 24
                   "2x" 34
                   "3x" 50
                   "4x" 68
                   "5x" 128})

(defonce emojis #?(:cljs (atom nil)
                   :clj (atom (read-string
                               (slurp (io/resource "emojis_tree.edn"))))))

(defn search
  [prefix]
  (search/prefix-search @emojis prefix))

(defn get
  [k]
  (-> (search/get @emojis k)
      last))

(defonce default-5-emojis
  [["thumbsup" "1f44d"]
   ["100" "1f4af"]
   ["heart" "2764"]
   ["grin" "1f601"]
   ["thumbsdown" "1f44e"]])
