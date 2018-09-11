(ns share.dicts
  (:require [tongue.core :as tongue]
            [share.dicts.en :as en]
            [share.dicts.zh-cn :as zh-cn]
            [share.dicts.zh-tw :as zh-tw]
            [linked.core :as linked]))

(defonce langs
  (linked/map
    "en" "English"
    "zh-cn" "简体中文"
    "zh-tw" "正體中文"
    "japanese" "Japanese"
    "german" "German"
    "french" "French"
    "spanish" "Spanish"
    "russian" "Russian"
    "italian" "Italian"))

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
   (t :other-issues)])

(comment
  (defonce s2t (read-string (slurp "/home/tienson/codes/projects/putchar/temp/s2t.edn")))

  (defn ->t
    []
    (->>
     (for [[k v] (:zh-cn dicts)]
       [k (if (> (count v) 1)
            (apply str (map #(get s2t (str %)) v))
            (get s2t v))])
     (into {})))

  (translate :en :japanese))
