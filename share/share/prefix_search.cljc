(ns share.prefix-search
  (:refer-clojure :exclude [get]))

(defn populate
  [tree m]
  (reduce (fn [t [k v]]
            (assoc-in t (concat k [:val]) [k v])) tree m))

(defn prefix-search
  [tree prefix]
  (letfn
      [(search
         [node]
         (mapcat (fn [[k v]]
                   (if (= :val k) [v] (search v)))
                 node))]
    (let [result (search (get-in tree prefix))]
      (when (seq result)
        (let [{equal-one true others false} (group-by (fn [[k v]]
                                               (= k prefix)) result)]
          (if (seq equal-one)
            (concat equal-one others)
            others))))))

(defn get
  [tree k]
  (when-let [v (get-in tree k)]
    (:val v)))
