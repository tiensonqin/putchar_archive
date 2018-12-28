(ns share.front-matter
  (:require [clojure.string :as str]
            [share.util :as util]))

(defn split-front-matter
  ([content]
   (split-front-matter content 64))
  ([content fm-max-lines]
   (let [orig-lines (str/split-lines (str/trim content))
         non-fm [nil content]]
     (if (= "---" (str/trim (first orig-lines)))
       (loop [lines (rest orig-lines) idx 1]
         (if (>= idx fm-max-lines)
           non-fm
           (if-let [line (first lines)]
             (if (= "---" (str/trim line))
               (let [[fm others] (split-at (inc idx) orig-lines)]
                 [(str/join "\n" fm)
                  (str/join "\n" others)])
               (recur (rest lines) (inc idx)))
             non-fm)))
       non-fm))))

;; (def spec-re #"^---([\S\s]*)---\n?$")

(defn extract
  [content]
  (when-not (str/blank? content)
    (let [[spec _] (split-front-matter content)
          spec (when spec
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
                      (into {})))]
      (-> (assoc spec
                 :body content
                 :lang (or (:language spec) "en")
                 :is_draft (if (:published spec) false true)
                 :cover (:cover_image spec))
          (select-keys [:title :tags :is_draft :lang :description :canonical_url :cover :body])))))

(defn remove-front-matter
  [content]
  (let [[_spec others] (split-front-matter content)]
    others))

(defn extract-title
  [content]
  (when-not (str/blank? content)
    (let [[spec _] (split-front-matter content)]
      (when spec
        (when-let [title (last (re-find #"title:([^\r\n]+)" spec))]
          (str/trim title))))))
