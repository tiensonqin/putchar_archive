(ns api.db.search
  (:require [clucie.core :as lucene]
            [clucie.analysis :as lucene-ana]
            [clucie.store :as lucene-store]
            [api.config :as config]
            [share.util :as util]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import
   (org.apache.lucene.index Term IndexWriter IndexWriterConfig)
   (org.apache.lucene.document.Field$Store)
   (org.apache.lucene.document DoublePoint NumericDocValuesField StoredField)
   (org.apache.lucene.search TermQuery
                             BooleanQuery BooleanClause PrefixQuery
                             BooleanClause$Occur FuzzyQuery
                             Sort SortField SortField$Type)
   (org.apache.lucene.queryparser.classic QueryParser)))


(defonce analyzer (atom nil))
(defonce index-store (atom nil))

(defn init! []
  (when (nil? @analyzer)
    (reset! analyzer
            (lucene-ana/standard-analyzer)))
  (when (nil? @index-store)
    (reset! index-store
            (lucene-store/disk-store (get-in config/config [:search :index-path])))))

(defn delete-dir! [dir-or-file]
  (when (.isDirectory dir-or-file)
    (doseq [child (.listFiles dir-or-file)]
      (delete-dir! child)))
  (.delete dir-or-file))

(defn finish! []
  (let [path (get-in config/config [:search :index-path])]
    (when @analyzer
      (reset! analyzer nil))
    (when @index-store
      (lucene-store/close! @index-store)
      (when path
        (delete-dir! (io/file path)))
      (reset! index-store nil))))

(defn add-user [user]
  (let [user {:screen_name (:screen_name user)}]
    (lucene/add! @index-store
                 [user]
                 [:screen_name]
                 @analyzer)))

(defn delete-user [screen-name]
  (lucene/delete! @index-store
                  :screen_name
                  screen-name
                  @analyzer))

(defn post-rank->fields
  [rank]
  (let [key "post_rank"
        value (double rank)]
    [(DoublePoint. key (double-array 1 value))
     (NumericDocValuesField. key (Double/doubleToRawLongBits value))
     ;; (StoredField. key value)
     ]))

(defn add-post [post]
  (when (not (:is_draft post))
    (let [post {:post_id (:id post)
                :post_title (:title post)
                :post_tags (:tags post)
                :clucie.core/raw-fields (post-rank->fields (:rank post))}]
      (lucene/add! @index-store
                   [post]
                   [:post_id :post_rank :post_title :post_tags]
                   @analyzer))))

(defn delete-post [id]
  (lucene/delete! @index-store
                  :post_id
                  id
                  @analyzer))

(defn update-post [post]
  (when (and (:title post) (:id post))
    (delete-post (:id post))
    (add-post post)))

(defn search
  ([q]
   (search nil))
  ([q {:keys [limit page sort per-page]
       :or {limit 100
            page 0
            per-page 10}}]
   (let [per-page (if per-page per-page limit)]
     (lucene/search @index-store
                    q
                    limit ; max-num
                    @analyzer
                    page
                    per-page
                    sort))))

(defn search-posts-order-by-rank
  ([q]
   (search-posts-order-by-rank q nil))
  ([q {:keys [page limit per-page]
       :or {limit 100
            page 0
            per-page 10}}]
   (let [stf (SortField. "post_rank" SortField$Type/DOUBLE true)
         sort (Sort. stf)]
     (search q
             {:limit limit
              :page page
              :per-page per-page
              :sort sort}))))

(defn prefix-search
  [q {:keys [limit]
      :or {limit 5}
      :as opts}]
  (let [[k v]
        (cond
          (:screen_name q)
          ["screen_name" (:screen_name q)]

          (:post_title q)
          ["post_title" (:post_title q)])]
    (let [term (Term. k v)
          prefix-q (PrefixQuery. term)
          result (apply search prefix-q opts)]
      (if (:screen_name q)
        (mapv :screen_name result)
        result))))
