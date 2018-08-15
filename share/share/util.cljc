(ns share.util
  (:refer-clojure :exclude [format uuid random-uuid read-string])
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [clojure.walk :as w]
            [share.helpers.regex :as reg]
            [bidi.bidi :as bidi]
            [linked.core :as linked]
            [share.version :refer [version]]
            [share.config :as config]
            [share.dicts :refer [t]]
            [share.dommy :as dommy]
            [appkit.macros :refer [oset! oget]]
            [clojure.walk :as walk]
            #?(:clj [clj-time.format :as tf])
            #?(:clj [flatland.ordered.map :as ordered-map])
            #?(:clj [clj-time.coerce :as cc])
            #?(:clj [clj-time.core :as c])
            #?(:clj [environ.core :refer [env]])
            #?(:cljs [goog.object :as gobj])
            #?(:cljs [goog.string])
            #?(:cljs [cljs-time.coerce :as cljscc])
            #?(:cljs [cljs-time.core :as cljst])
            #?(:cljs [cljs-time.format :as cljstf])
            #?(:cljs [goog.dom :as gdom])
            #?(:cljs [web.scroll :as scroll])
            #?(:cljs [cljs.reader :as reader])
            #?(:cljs [web.md5 :as md5])
            )
  #?(:clj (:import  [java.util.UUID]
                    [java.net URL])))

(defn keywordize
  [data]
  #?(:cljs (js->clj data :keywordize-keys true)
     :clj (w/keywordize-keys data)))

(defn map->query
  [m]
  (some->> (seq m)
           sort                     ; sorting makes testing a lot easier :-)
           (map (fn [[k v]]
                  [(name k)
                   "="
                   (str v)]))
           (interpose "&")
           flatten
           (apply str)))

(defn split-param [param]
  (->
   (s/split param #"=")
   (concat (repeat ""))
   (->>
    (take 2))))

(defn query->map
  [qstr]
  (when (not (s/blank? qstr))
    (let [parts (s/split qstr #"\?")]
      (when (>= (count parts) 2)
        (when-let [qstr (nth parts 1)]
         (when (not (s/blank? qstr))
           (some->> (s/split qstr #"&")
                    seq
                    (mapcat split-param)
                    (map bidi/url-decode)
                    (apply hash-map)
                    (w/keywordize-keys))))))
    ))

(defn capitalize-first
  [s]
  (when s
    (apply str (s/capitalize (first s)) (rest s))))

(defn capitalize-first-char
  [s]
  (if s
    (str (.toUpperCase (str (first s)))
        (subs s 1))))

(defn get-date []
  #?(:clj (java.util.Date.)
     :cljs (js/Date.)))

(defn get-time
  []
  (.getTime (get-date)))

(defn date-format
  ([date]
   (date-format date "EEE, dd MMM yyyy"))
  ([date format]
   (if date
     #?(:cljs
        (cljstf/unparse-local (cljstf/formatter format) (cljscc/to-date-time date))
        :clj
        (tf/unparse (tf/formatter format) (cc/to-date-time date))))
   ))

(defn time-ago [date]
  (let [to-date-time #?(:clj cc/to-date-time
                        :cljs cljscc/to-date-time)
        day #?(:clj c/day
               :cljs cljst/day)
        to-long #?(:clj cc/to-long
                   :cljs cljscc/to-long)
        now (get-date)
        now-date (to-date-time now)
        date' (to-date-time date)
        s (-> now .getTime (- (to-long date)) (quot 1000))
        m (quot s 60)
        h (quot m 60)
        d (quot h 24)
        d (if (>= d 1)
            d
            0)]
    (cond
      (>= d 30) (date-format date "dd MMM")
      (>= d 1) (str d (t :d))
      (>= h 1) (str h (t :h))
      (>= m 1) (str m (t :m))
      (>= s 1) (str s (t :s))
      :else (t :now))))


(defn get-layout
  []
  #?(:cljs {:width  (gobj/get js/window "innerWidth")
            :height (gobj/get js/window "innerHeight")}
     :clj {:width 1024
           :height 700}))

(def development?
  config/development?)

#?(:cljs
   (when-not development?
     (set! (.-log js/console)
           (fn [] nil))))

(defn top-elem
  [xs x]
  (let [[top others] [(get xs x)
                      (apply dissoc xs [x])]]
    (into {x (update top :temp-workaround (fn [x]
                                            (case x
                                              nil true
                                              true false
                                              false true)))} others)))


(defn ev
  [e]
  #?(:cljs (gobj/getValueByKeys e "target" "value")
     :clj nil))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn cdn-image
  [name & {:keys [suffix width height]
           :or {width 80
                height 80
                suffix "jpg"}}]
  (if name
    (cond
      ;; logo
      (= name "lambdahackers")
      (str config/website "/logo-2x.png")

      (s/starts-with? name "deleted-user-")
      (str config/img-cdn "/11FAjQ9BPF.jpg" "?w=" width "&h=" height)

      :else
      (str config/img-cdn "/" name "." suffix "?w=" width "&h=" height))))

(defn group-logo
  ([name]
   (group-logo name 64 64))
  ([name width height]
   (let [params (cond-> {:auto "null"}
                  width (assoc :w width)
                  height (assoc :h height))]
     (str config/img-cdn "/" name "_logo.png" "?"
          (map->query params)))))

(defn format
  [fmt & args]
  #?(:cljs (apply goog.string/format fmt args)
     :clj (apply clojure.core/format fmt args)))

(defn non-blank? [v]
  (and (string? v)
       (not (s/blank? v))))

(defn optional-non-blank?
  [s]
  (or (s/blank? s)
      (non-blank? s)))

(defn username? [v]
  (re-find #"^([A-Za-z]){1}([A-Za-z0-9_]){0,14}$" v))

(defn group-name? [v]
  (re-find (re-pattern
            (format "^@?([0-9-_& %s]){1,24}$" reg/unicode)) v))

(defn encrypted-name? [v]
  (re-find #"^@?([%a-zA-Z0-9]){1,192}$" v))

(defn encrypted-group-name? [v]
  (or (group-name? v)
      (encrypted-name? v)))

(defn length? [{:keys [min max]}]
  (fn [v]
    (if v
      (let [length (.-length v)]
        (and (if min (if (>= length min) true false) true)
             (if max (if (<= length max) true false) true))))))

(defn post-title? [v]
  ((length? {:min 2
             :max 256}) v))

(def channel-name? group-name?)
(def encrypted-channel-name? encrypted-group-name?)

(defn remove-duplicates
  ([entities] (remove-duplicates nil entities))
  ([f entities]
   (let [f (or (and (or (fn? f) (keyword? f)) f) identity)
         meets (transient #{})]
     (persistent!
      (reduce (fn [acc item]
                (let [k (f item)]
                  (if (or (nil? item) (contains? meets k))
                    acc
                    (do (conj! meets k)
                        (conj! acc item)))))
              (transient [])
              entities)))))

#?(:cljs
   (defn normalize
     ([col]
      (normalize :id col))
     ([k col]
      (if (seq col)
        (->> (for [item col]
               [(get item k) item])
             (into (linked/map)))
        col))))

;; TODO: linked/map not support transit encoding well
#?(:clj
   (defn normalize
     ([col]
      (normalize :id col))
     ([k col]
      (if (seq col)
        (->> (for [item col]
               [(get item k) item])
             (into (ordered-map/ordered-map)))
        col))))


(defn indexed [coll] (map-indexed vector coll))

(def cdn-img-re
  (re-pattern (format "%s/[^ ]+.jpg" config/img-cdn)))

(defn cdn-replace
  [body]
  (some-> body
          (s/replace cdn-img-re
                     (fn [x]
                       (str x "?auto=format&fm=jpg")))))

(defn brief-description
  [body]
  (some->>
   (s/replace body #"<[^>]*>" "")
   ;; (s/trim)
   (.trim)
   (take 128)
   (apply str)
   (bidi/url-encode)))

(defonce user-agent (atom nil))

(defn mobile?
  []
  (re-find #"Mobi" #?(:cljs js/navigator.userAgent
                      :clj @user-agent)))

(defn set-timeout [t f]
  #?(:cljs (js/setTimeout f t)
     :clj  nil))

(defn clear-timeout [timeout]
  #?(:cljs (js/clearTimeout timeout)
     :clj  nil))

(defn set-interval [t f]
  #?(:cljs (js/setInterval f t)
     :clj  nil))

(defn clear-interval [interval]
  #?(:cljs (js/clearInterval interval)
     :clj  nil))


(def link-re
  (str "https?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|" reg/unicode
       "]*"))
(def direct-link-re #"(?![^<]*</)(https?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|])")

(defn parse-int
  [x]
  #?(:cljs (js/parseInt x)
     :clj (Integer/parseInt x)))

;; TODO: [^\]]* not works
(defn link?
  [s]
  (and s (re-find (re-pattern (str "^" link-re "\\[*\\]*" "$")) s)))

(comment
  (def asciidoc-link "https://lambdahackers.com[lambdahackers]"))

(def email-re #"[\w._%+-]+@[\w.-]+\.[\w]{2,4}")

(defn- image?
  [link]
  (contains? #{"jpg" "png" "gif"}
             (->> (take-last 3 link)
                  (apply str)
                  (s/lower-case))))

(defn- web-post?
  [link]
  (re-find (re-pattern (str config/website "/@"))
           link))

(defn uuid [s]
  (if s
    #?(:cljs (cljs.core/uuid s)
      :clj (java.util.UUID/fromString s))))

(def random-uuid
  #?(:cljs cljs.core/random-uuid
     :clj #(java.util.UUID/randomUUID)))

(defn deep-merge
  "Like merge, but merges maps recursively."
  [& maps]
  (if (every? #(or (map? %) (nil? %)) maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(deep-merge {:a {:b 1}} {:a {:b 2 :c 3}})

;; TODO: pprint
(defn debug
  [& args]
  #?(:cljs
     (. js/console log (apply str "%c " args " ") "background: #2e2e2e; color: white")

     :clj
     nil))

(defn map-keys
  "Apply function F to the keys of map M, returning a new map."
  [f m]
  (reduce-kv #(assoc %1 (f %2) %3) {} m))

(defn map-remove-nil?
  [m]
  (reduce
   (fn [i [k v]]
     (if (nil? v)
       i
       (assoc i k v)))
   {}
   m))

(defn group-channel
  [params]
  {:group-name (s/lower-case (:group-name params))
   :channel-name (s/lower-case (:channel-name params))})

(defn get-file-base-name
  [s]
  (->> (s/split s #"\.")
       (drop-last)
       (interleave )
       (s/join "_")))

(defn get-file-ext
  [s]
  (->> (s/split s #"\.")
       last))

(defn get-domain
  [link]
  (let [d (second (s/split link #"//"))
        d (first (s/split d #":"))
        d (first (s/split d #"\?"))
        d (first (s/split d #"/"))
        d (first (s/split d #"\#"))
        parts (s/split d #"\.")
        parts (if (= (count parts) 2)
                parts
                (drop 1 parts))]
    (s/join "." parts)))

(defn capitalize
  [s]
  (some-> (name s)
          (s/replace #"[-_]+" " ")
          (s/split #" ")
          (->>
           (map s/capitalize)
           (interpose " ")
           (apply str))))

(defn post-link
  [post]
  (let [permalink (:permalink post)]
    (if permalink
      (str config/website "/" permalink))))

(defn comment-link
  [entity idx]
  (cond
    (:permalink entity)
    (str config/website "/" (:permalink entity) "/" idx)

    (:item_id entity)
    (str config/website "/item/" (:id entity) "/" idx)

    :else
    nil))


(defn original-name
  [name]
  (if name
    (some->> (s/split name #"-")
             (map s/capitalize)
             (interpose " ")
             (map #(s/replace % "Ocaml" "OCaml"))
             (apply str)
             (bidi/url-decode))))

(defn internal-name
  [name]
  (-> name
      (s/split #"\(")
      (first)
      (s/split #",")
      (first)
      (s/trim)
      (s/replace #"[\。\，\"\"]+" "")
      (s/replace #"[-&\(\)\[\],\.\;\*\`\。\，\s\"\"]+" "-")
      (s/lower-case)
      ;; (bidi/url-encode)
      ))

(defn split-comma
  [s]
  (->> (s/split s #"[,\， ]+")
       (remove nil?)))

(defn tag-encode
  [tag]
  (some-> tag
          s/trim
          s/lower-case
          (s/replace #"[_&\(\)\[\],\.\;\*\`\。\，\s]+" "-")
          (bidi/url-encode)))

(defn tag-decode
  [tag-name]
  (if tag-name
    (some->> (s/split tag-name #"-")
             (map s/capitalize)
             (interpose " ")
             (apply str)
             (bidi/url-decode))))

(defn cdn-version
  [x]
  (if development?
    x
    (str "/" version x)))

(defn scroll-to-top []
  #?(:clj nil
     :cljs (.scrollTo js/window 0 0)))

(defn scroll-top []
  #?(:clj nil
     :cljs
     (if (exists? (.-pageYOffset js/window))
       (.-pageYOffset js/window)
       (.-scrollTop (or (.-documentElement js/document)
                        (.-parentNode (.-body js/document))
                        (.-body js/document))))))

(defn page-height
  "Return the height of the page."
  []
  #?(:clj 1024
     :cljs js/document.documentElement.clientHeight))

(defn scroll-height
  []
  #?(:clj 1024
     :cljs js/document.documentElement.scrollHeight))

(defn get-prev
  [ids id]
  (cond
    (nil? id)
    (first ids)

    (= (count ids) 1)
    id

    (= (first ids) id)
    (last ids)

    :else
    (loop [ids ids p (first ids)]
      (cond
        (not (seq ids))
        nil

        (= (first ids) id)
        p

        :else
        (recur (next ids) (first ids))))))

(defn get-next
  [ids id]
  (cond
    (nil? id)
    (first ids)

    (= (count ids) 1)
    id

    (= (last ids) id)
    (first ids)

    :else
    (loop [ids ids]
      (cond
        (not (seq ids))
        nil

        (= (first ids) id)
        (first (next ids))

        :else
        (recur (next ids))))))

(defn abs
  [x]
  #?(:clj (Math/abs x)
     :cljs (js/Math.abs x)))

(defn encode-permalink
  [permalink]
  (if (re-find #"%" permalink)
    permalink
    (let [[screen-name title] (s/split permalink #"/")]
      (str screen-name "/" (bidi/url-encode title)))))

(defn decode-permalink
  [permalink]
  (let [[screen-name title] (s/split permalink #"/")]
    {:screen_name (subs screen-name 1)
     :permalink title})
  )

(defn encode
  [tag]
  (if (re-find #"%" tag)
    tag
    (bidi/url-encode tag)))

(defn stop [e]
  (doto e (.preventDefault) (.stopPropagation)))

(defn highlight!
  []
  #?(:cljs
     (if js/window.hljs
       (doseq [x (dommy/sel "pre code")]
         (js/window.hljs.highlightBlock x)))))

(defn get-current-url
  []
  #?(:cljs
     js/window.location.href))

(defn get-pathname
  []
  #?(:cljs
     js/window.location.pathname))

(defn get-github-repo-name
  [url]
  (-> (s/split url #"/")
      (last)
      (s/replace ".git" "")))

(defn get-github-handle-repo
  [url]
  (let [[handle repo] (->> (s/split url #"/")
                           (take-last 2))]
    [handle (s/replace repo ".git" "")]))

(defn set-href!
  [link]
  #?(:cljs (oset! js/window.location "href" link)))

(defn set-title!
  [title]
  #?(:cljs (oset! js/document "title" title)))

(defn scroll-to-element
  []
  #?(:cljs
     (when-let [hash js/window.location.hash]
       (when-let [element (gdom/getElement (subs hash 1))]
         (scroll/into-view element)))))

;; TODO: remove this
(defn me?
  [user]
  (= (:screen_name user) "tiensonqin"))

(defn read-string
  [s]
  (if (string? s)
    #?(:cljs (reader/read-string s)
       :clj (clojure.core/read-string s))
    s))

(defn get-selection
  "Note: boundary not works with textarea."
  []
  #?(:cljs
     (when-let [selection (.getSelection js/document)]
       (when-not (zero? (oget selection "rangeCount"))
         {:text (.toString selection)
         :boundary (let [range (.getRangeAt selection 0)

                         b (.getBoundingClientRect range)]
                     {:top (oget b "top")
                      :bottom (oget b "bottom")
                      :left (oget b "left")
                      :right (oget b "right")}
                     )}))))

(defn get-selection-text
  []
  #?(:cljs
     (when-let [selection (.getSelection js/document)]
       (.toString selection))))

(defn remove-selection-ranges
  []
  #?(:cljs
     (when-let [selection (.getSelection js/document)]
       (.removeAllRanges selection))))

(defn transform-text
  [s start end new]
  (str (subs s 0 start)
       new
       (subs s end (count s))))

(defn get-stared-groups
  [current-user]
  (when-let [groups (:stared_groups current-user)]
    #?(:clj (normalize groups)
       :cljs (if (map? groups)
               groups
               (normalize groups)))))

(defn distinct-by
  [k col]
  (reduce
   (fn [acc x]
     (let [v (get x k)]
       (if (some #(= v (get % k)) acc)
         acc
         (vec (conj acc x)))))
   []
   col))

(defn strip-tags [s]
  (when (string? s)
    (-> s (s/replace #"<[^>]*>" ""))))

(defn delete-spaces [s]
  (when (string? s)
    (s/replace s #"[\n ]{2,}" "\n")))

(defn full-strip [s]
  (some-> s
      strip-tags
      delete-spaces))

(defn channel-name [s]
  (if s
    (cond (= "general" s)
          (t :general)

          :else
          s)))

(defn get-group-by-id
  [groups current-group-id]
  (some->> (vals groups)
           (filter #(= (:id %) current-group-id))
           first))

(defn set-cursor-end
  [e]
  #?(:cljs (let [v (ev e)]
             (set! (.-value (.-target e)) "")
             (set! (.-value (.-target e)) v))))

(defn remove-v-nil?
  [k col]
  (if (seq col)
    (vec (remove #(nil? (get % k)) col))))

(defn jsx->clj
  [x]
  #?(:cljs (into {} (for [k (.keys js/Object x)] [k (aget x k)]))))

(defn share
  "NOTE: only work directly with `on-click`, otherwise there'll be an error like this:
  `Error sharing: DOMException: Must be handling a user gesture to perform a share request.`"
  [m]
  #?(:cljs
     (if js/navigator.share
       (js/navigator.share (clj->js m)))))

(defn get-locale
  [req]
  (if-let [locale (get-in req [:cookies "locale" :value])]
    (keyword locale)
    (if-let [accept-language (get-in req [:headers "accept-language"])]
      (if-let [v (first (s/split accept-language #","))]
        (cond (s/starts-with? v "en-")
              :en

              (let [v (-> v
                          (s/replace "_" "-")
                          (s/lower-case))]
                (re-find #"zh-cn" v))
              :zh-cn

              :else
              (-> v
                  (s/replace "_" "-")
                  (s/lower-case)
                  (keyword v)))
        :en)
      :en)))

#?(:cljs
   (defn md5-query
     [q]
     (-> q str md5/md5)))

(defn ->tree
  [col parent-key reserve-child?]
  (loop [col col
         paths {}
         new-col {}]
    (if-let [item (first col)]
      (let [id (:id item)
            parent-id (parent-key item)]
        (recur (rest col)
               (if parent-id          ; is child
                 (assoc paths id (-> (get paths parent-id)
                                     (concat [:children id])
                                     (vec)))
                 (assoc paths id [id]))
               (let [new-col (if parent-id
                               (let [p (concat (get paths parent-id)
                                               [:children id])]
                                 (assoc-in new-col p item))
                               new-col)]
                 (if (or (not parent-id) (and parent-id reserve-child?))
                   (update new-col id merge item)
                   new-col))))
      new-col)))

(defn split-first-line
  [s]
  (when-not (s/blank? s)
    (loop [first-line "" s (seq s)]
      (let [c (first s)]
        (if (or (empty? s)
                (contains? #{\newline \return} c))
          [first-line (apply str (rest s))]
          (recur (str first-line c) (rest s)))))))

(defn get-first-true
  "Returns the first logical true value of x for any x in coll,
  else nil."
  {:added "1.0"
   :static true}
  [pred coll]
  (when (seq coll)
    (if (pred (first coll))
      (first coll)
      (recur pred (next coll)))))

(defn split-tags
  [tags-string]
  (if tags-string
    (s/split tags-string #"[,，]+[\s]*")))

(defn ios? []
  (if (re-find #"iPhone|iPad|iPod" #?(:cljs js/navigator.userAgent
                                      :clj @user-agent))
    true
    false))
