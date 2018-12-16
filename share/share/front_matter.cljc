(ns share.front-matter
  (:require [clojure.string :as str]))

(def spec-re #"^---([\S\s]*)---")

(defn extract
  [content]
  (when-not (str/blank? content)
    (let [result (re-find spec-re content)
          spec (when (>= (count result) 2)
                 (let [spec (nth result 1)]
                   (->> (str/split-lines spec)
                        (remove str/blank?)
                        (mapv (fn [x]
                                (let [result (->> (str/split x #": ")
                                                  (remove str/blank?))
                                      [k v] (if (= ":tags" (first result))
                                              (if (> (count result) 2)
                                                [":tags" (apply str (rest result))]
                                                result)
                                              result)]
                                  [(keyword k) (case v
                                                 "true" true
                                                 "false" false
                                                 (if v (str/trim v)))])))
                        (into {}))))]
      (-> (assoc spec
                 :body content
                 :lang (or (:language spec) "en")
                 :is_draft (if (:published spec) false true)
                 :cover (:cover_image spec))
          (select-keys [:title :tags :is_draft :lang :description :canonical_url :cover :body])))))

(defn remove-front-matter
  [content]
  (str/triml content)
  ;; TODO: better regex
  ;; (str/triml (str/replace-first content spec-re ""))
  )

(defn extract-title
  [content]
  (when-not (str/blank? content)
    (let [result (re-find spec-re content)
          spec (and
                (>= (count result) 2)
                (nth result 1))
          title (if spec (last (re-find #"title:([^\r\n]+)" spec)))]
      (if title
        (str/trim title)))))
