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
    "#2156a5"))

(defn shadow
  []
  (if (= @theme "black")
    "#bbb"
    "#555"))

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
