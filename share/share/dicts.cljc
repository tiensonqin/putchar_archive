(ns share.dicts
  (:require [tongue.core :as tongue]
            [share.dicts.en :as en]
            [share.dicts.zh-cn :as zh-cn]
            [share.dicts.zh-tw :as zh-tw]))

(def langs
  [:english
   :german
   :simplified_chinese
   :traditional_chinese
   :japanese
   :french
   :spanish
   :turkish
   :italian
   :portuguese
   :swedish
   :danish
   :norwegian
   :dutch
   :polish
   :indonesian
   :hindi
   :russian
   :korean
   :thai
   :arabic
   :bengali
   :punjabi
   ])

(def dicts
  {:en    en/dicts
   :zh-cn zh-cn/dicts
   :zh_tw zh-tw/dicts
   :tongue/fallback :en})

(def locale (atom :en))

(def translate ;; [locale key & args] => string
  (tongue/build-translate dicts))

(defn t
  [k]
  (translate @locale k))

(defn reasons
  []
  [(t :spam)
   (t :abusive)
   (t :break-group-rules)
   (t :other-issues)])

(comment
  (defonce s2t (read-string (slurp "/home/tienson/codes/projects/lambdahackers/temp/s2t.edn")))

  (defn ->t
    []
    (->>
     (for [[k v] (:zh-cn dicts)]
       [k (if (> (count v) 1)
            (apply str (map #(get s2t (str %)) v))
            (get s2t v))])
     (into {})))

  (translate :en :japanese))
