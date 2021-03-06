(ns appkit.citrus.macros
  (:refer-clojure :exclude [doseq]))

;; copied from https://github.com/roman01la/citrus

(defmacro doseq
  "Lightweight `doseq`"
  [[item coll] & body]
  `(let [coll# ~coll
         result# (cljs.core/array)]
     (loop [xs# coll#]
       (when-some [~item (first xs#)]
         (.push result# (do ~@body))
         (recur (next xs#))))
     (seq result#)))
