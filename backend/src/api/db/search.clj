(ns api.db.search
  (:require [clucie.core :as lucene]
            [clucie.analysis :as lucene-ana]
            [clucie.store :as lucene-store]
            [api.config :as config]
            [share.util :as util])
  (:import
   (org.apache.lucene.index Term IndexWriter IndexWriterConfig)
   (org.apache.lucene.search TermQuery
                             BooleanQuery BooleanClause PrefixQuery
                             BooleanClause$Occur FuzzyQuery)
   ))

(defonce analyzer (lucene-ana/standard-analyzer))
(defonce index-store (atom nil))

(defn init! []
  (if (nil? @index-store)
    (reset! index-store
            (lucene-store/disk-store (get-in config/config [:search :index-path])))))

(defn create-idx-writer-config []
  (let [iwc (IndexWriterConfig. analyzer)]
    (.setRAMBufferSizeMB iwc 256.0)
    iwc))

(defn delete-all
  []
  (let [dir @index-store
        iwc (create-idx-writer-config)
        writer (IndexWriter. dir iwc)]
    (doto writer
      (.deleteAll)
      (.commit)
      (.close writer))))

(defn add-user [user]
  (let [user {:screen_name (:screen_name user)}]
    (lucene/add! @index-store
                 [user]
                 [:screen_name]
                 analyzer)))

(defn delete-user [screen-name]
  (lucene/delete! @index-store
                  :screen_name
                  screen-name
                  analyzer))

(defn add-book [book]
  (let [book {:book_id (:object_id book)
              :book_title (:title book)}]
    (lucene/add! @index-store
                 [book]
                 [:book_id :book_title]
                 analyzer)))

(defn update-book [book]
  (when (:title book)
    (let [book {:book_title (:title book)}]
      (lucene/update! @index-store
                      book
                      [:book_title]
                      :book_id
                      (:object_id book)
                      analyzer))))

(defn delete-book [book-title]
  (lucene/delete! @index-store
                  :book-title
                  book-title
                  analyzer))

(defn add-paper [paper]
  (let [paper {:paper_id (:object_id paper)
              :paper_title (:title paper)}]
    (lucene/add! @index-store
                 [paper]
                 [:paper_id :paper_title]
                 analyzer)))

(defn update-paper [paper]
  (when (:title paper)
    (let [paper {:paper_title (:title paper)}]
      (lucene/update! @index-store
                      paper
                      [:paper_title]
                      :paper_id
                      (:object_id paper)
                      analyzer))))

(defn delete-paper [paper-title]
  (lucene/delete! @index-store
                  :paper-title
                  paper-title
                  analyzer))

(defn add-post [post]
  (when (not (:is_draft post))
    (let [post {:post_id (:id post)
                :post_title (:title post)}]
      (lucene/add! @index-store
                   [post]
                   [:post_id :post_title]
                   analyzer))))

(defn update-post [post]
  (when (:title post)
    (let [post {:post_title (:title post)}]
      (lucene/update! @index-store
                      post
                      [:post_title]
                      :post_id
                      (:id post)
                      analyzer))))

(defn delete-post [id]
  (lucene/delete! @index-store
                  :post_id
                  id
                  analyzer))

(defn search
  [q & {:keys [limit]}]
  (prn {:q q
        :limit limit})
  (let [limit (if limit limit 5)]
    (lucene/search @index-store
                   q
                   limit ; max-num
                   analyzer
                   0 ; page
                   limit))) ; max-num-per-page

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
