(ns share.front-matter
  (:require [clojure.string :as str]
            [share.util :as util]
            #?(:clj [cheshire.core :as json]))
  #?(:clj (:import (java.io BufferedReader StringReader))))

#?(:clj
   (defn- extract-spec-from-html
     [content]
     (let [first-line (first (line-seq (BufferedReader. (StringReader. content))))
           start "<!-- directives: "
           end " -->"]
       (if (and (str/starts-with? first-line start)
                (str/ends-with? first-line end))
         (->> (subs first-line (count start) (- (count first-line)
                                                (count end)))
              (json/parse-string)
              (into {})
              (util/map-keys (fn [k]
                               (let [k (keyword (str/lower-case k))]
                                 (case k
                                   :keywords :tags
                                   k)))))))))

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

#?(:clj
   (defn extract
     [content html]
     (when-not (str/blank? content)
       (let [[spec _] (split-front-matter content)
             spec (if spec
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
                                   [(keyword k) (if v (str/trim v))])))
                         (into {}))
                    ;; html
                    (extract-spec-from-html html))]
         (-> (assoc spec
                    :body content
                    :lang (or (:language spec) "en")
                    :is_draft (if (= (:published spec) "true") false true)
                    :cover (:cover_image spec))
             (select-keys [:title :subtitle :tags :is_draft :lang :description :canonical_url :cover :body]))))))

(defn remove-front-matter
  [content]
  (let [[_spec others] (split-front-matter content)]
    others))

(defn extract-org-mode-title
  [content]
  (let [result (re-find #"^#\+TITLE:\s+(.*)" content)]
    (when (>= (count result) 2)
      (nth result 1))))

(defn extract-asciidoc-title
  [content]
  (let [result (re-find #"^=\s+(.*)" content)]
    (when (>= (count result) 2)
      (nth result 1))))

(defn extract-title
  [content body-format]
  (when-not (str/blank? content)
    (if-let [title (let [[spec _] (split-front-matter content)]
                     (when spec
                       (when-let [title (last (re-find #"title:([^\r\n]+)" spec))]
                         (str/trim title))))]
      title
      (case (keyword body-format)
        :org-mode (extract-org-mode-title content)
        :asciidoc (extract-asciidoc-title content)
        nil))))
