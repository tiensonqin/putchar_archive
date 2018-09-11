(ns share.kit.colors
  (:require #?(:cljs [appkit.cookie :as cookie])
            [clojure.string]))

(def theme (atom (or #?(:cljs (cookie/cookie-get :theme)
                        :clj "white")
                     "white")))

(defn primary
  []
  (if (= @theme "black")
    "#6dd"
    "#071839"))

(defn primary-text
  []
  (if (= @theme "black")
    "#6dd"
    "#071839"))

(defn shadow
  []
  (if (= @theme "black")
    "#bbb"
    "#666"))

(defn icon-color
  []
  (if (= @theme "black")
    "#ccc"
    "#444"))

(defn new-post-color
  []
  (if (= @theme "black")
    "#ddd"
    "#222"))

(defn logo-background
  []
  (if (= @theme "black")
    "#FFF"
    "#000"))

(defn logo-text-background
  []
  (if (= @theme "black")
    "#000"
    "#FFF"
    ))
