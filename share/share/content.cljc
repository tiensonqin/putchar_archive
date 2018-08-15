(ns share.content
  (:require [clojure.string :as s]
            [share.util :as util]
            [share.kit.ui :as ui]
            [rum.core :as rum]
            [share.emoji :as emoji]
            #?(:cljs ["react-dom/server" :as react-dom-server])
            #?(:cljs [sablono.core :as html])
            [appkit.citrus :as citrus]
            [share.asciidoc :as ascii]
            [share.markdown :as md]
            ))

;; TODO: write a parser for asciidoc using cljc

(def youtube-re #"https://youtu\.be/([a-zA-Z0-9-_]+)(\?t=[a-zA-Z0-9]+)*")
(def youtube-re-2 #"https://(www\.)?youtube\.com/watch\?v=([a-zA-Z0-9-_]+)")
(def ascii-youtube-re #"video::([a-zA-Z0-9-_]+)\[youtube\]")

(defn render-dom
  [hiccups]
  #?(:clj (rum/render-html hiccups)
     :cljs (react-dom-server/renderToString (html/html hiccups))))

(defn wrap-html
  [string]
  (str "+++" string "+++"))

(defn wrap-render
  [hiccups body-format]
  (cond-> (render-dom hiccups)
    (= body-format :asciidoc)
    (wrap-html)))

(defn get-first-youtube-video
  [body]
  (when-let [id (if-let [result (re-find youtube-re body)]
                  (second result)
                  (if-let [result (re-find youtube-re-2 body)]
                    (nth result 2)
                    (if-let [result (re-find ascii-youtube-re body)]
                      (second result))))]
    (str "https://youtu.be/" id)))

(defn start->secs
  [s]
  (let [s (s/replace s "?t=" "")
        result (s/split s #"[hms]")]
    (case (count result)
      3
      (+ (* 3600 (util/parse-int (first result)))
         (* 60 (util/parse-int (second result)))
         (util/parse-int (last result)))
      2
      (+ (* 60 (util/parse-int (first result)))
         (util/parse-int (last result)))
      1
      (util/parse-int (last result))

      0)))

(defn build-youtube-frame
  [id start ascii?]
  (let [[width height] (if (util/mobile?)
                         [300 (double (* 300 (/ 315 560)))]
                         [560 315])]
    (str
     (if ascii? "+++\n" "\n")
     "<iframe width=\""
     width
     "\" height=\""
     height
     "\" src=\"https://www.youtube.com/embed/"
     id
     (if start (str "?start=" (start->secs start)) "")
     "\" frameborder=\"0\" allow=\"encrypted-media\" allowfullscreen></iframe>"
     (if ascii? "\n+++\n" "\n"))))

(defn embed-youtube
  [body body-format]
  (let [replace-fn (fn [l]
                     (let [l (remove #{"www."} l)
                           [youtube-id start] (if (= 3 (count l))
                                                (rest l)
                                                [(last l) nil])]
                       (build-youtube-frame youtube-id start (= body-format :asciidoc))))]
    (-> body
        (s/replace youtube-re replace-fn)
        (s/replace youtube-re-2 replace-fn))))

(def quote-pattern #"\[quote, @(\w+),?\s?(\d+)?\]")

(rum/defc quote-header
  [screen_name idx]
  [:div.column.quote-header
   [:a.space-between.no-decoration {:quoteidx idx
                                    :style {:align-items "center"}}
    [:div.row1 {:style {:align-items "center"}}
     (ui/avatar {:src (util/cdn-image screen_name)
                 :class "ant-avatar-sm"})
     [:span {:style {:margin-left 6
                     :font-size 13
                     :color "#666"}}
      (str screen_name ":")]]

    (ui/icon {:type :arrow_upward
              :width 18
              :height 18
              :color "#666"})]])

(defn quotes
  [body body-format]
  (let [replace-fn (fn [[_ screen_name idx]]
                     (str "[role=\"concate\"]\n[quote]\n"
                          (-> (quote-header screen_name idx)
                              (wrap-render body-format))
                          ))]
    (-> body
        (s/replace quote-pattern replace-fn))))

(rum/defc mention
  [screen-name]
  [:a.mention {:href (str "/@" screen-name)}
   (str "@" screen-name)])

(def mention-pattern #"\B@(\w+)")

(defn get-mentions
  [s]
  (some->> s
           (re-seq mention-pattern)
           (map second)
           (set)))

(defn get-last-pattern
  [s start-char pattern]
  (let [valid-char? #(re-find pattern (str %))]
    (cond
      (s/blank? s)
      nil

      :else
      (loop [len (dec (count s))
             result nil]
        (cond
          (< len 0)
          nil

          (and (= start-char (nth s len))
               (or
                (zero? len)
                (contains? #{\space \newline} (nth s (dec len)))))
          (s/reverse (str result ""))

          (not (valid-char? (nth s len)))
          nil

          :else
          (recur (dec len)
                 (str result (nth s len))))))))

(defn get-mention
  [s]
  (get-last-pattern s \@ #"[A-Za-z0-9_]"))

(defn mentions
  [body body-format]
  (-> body
      (s/replace mention-pattern
                 (fn [[_ screen_name]]
                   (-> (mention screen_name)
                       (wrap-render body-format))))))


(def emoji-pattern #"\B:(\w+):(\[(\w+)\])?")

(defn get-emoji
  [s]
  (get-last-pattern s \: #"[A-Za-z0-9_:]"))

(rum/defc emoji
  [name size]
  (when-let [code (emoji/get name)]
    (let [src (if (s/starts-with? code "http")
                code
                (str "https://assets-cdn.github.com/images/icons/emoji/unicode/" code ".png?v8"))
          size (get emoji/size-map size 24)]
      [:img {:class "emoji"
             :src src
             :style {:width size
                     :height size}}])))

(defn emojis
  [body body-format]
  (-> body
      (s/replace emoji-pattern
                 (fn [[s name _ size]]
                   (if-let [code (emoji/get name)]
                     (-> (emoji name size)
                         (wrap-render body-format))
                     s)))))

(defn website-links
  [body body-format]
  body)

(defn pre-transform
  [body body-format]
  (some-> body
          (embed-youtube body-format)
          (quotes body-format)
          (mentions body-format)
          (emojis body-format)
          (website-links body-format)))

(def spec-re #"////([^\[]+)////\s*")

(defn post-transform
  [body body-format]
  (some-> body
          (util/cdn-replace)
          (s/replace-first spec-re "")))

(defn render
  [body body-format]
  (let [body-format (keyword body-format)
        render-fn (if (= body-format :markdown)
                    md/md->html
                    ascii/render)]
    (some-> body
            (pre-transform body-format)
            (render-fn)
            (post-transform body-format))))
