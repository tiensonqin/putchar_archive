(ns api.db.search
  (:require [clucie.core :as lucene]
            [clucie.analysis :as lucene-ana]
            [clucie.store :as lucene-store]
            [api.config :as config]
            [share.util :as util])
  (:import
   (org.apache.lucene.index Term)
   (org.apache.lucene.search TermQuery
                             BooleanQuery BooleanClause PrefixQuery
                             BooleanClause$Occur FuzzyQuery)))

(defonce analyzer (lucene-ana/standard-analyzer))
(defonce index-store (atom nil))

(defn init! []
  (if (nil? @index-store)
    (reset! index-store
            (lucene-store/disk-store (get-in config/config [:search :index-path])))))

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

(defn add-group [group]
  (when-not (= (:privacy group) "private")
    (let [group {:group_id (:id group)
                 :group_name (util/original-name (:name group))}]
      (lucene/add! @index-store
                   [group]
                   [:group_id :group_name]
                   analyzer))))

(defn delete-group [id]
  (lucene/delete! @index-store
                  :group_id
                  id
                  analyzer))

(defn add-post [post]
  (when (and (not (:is_draft post))
             (not (:is_private post)))
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

          (:group_name q)
          ["group_name" (:group_name q)]

          (:post_title q)
          ["post_title" (:post_title q)])]
    (let [term (Term. k v)
          prefix-q (PrefixQuery. term)
          result (apply search prefix-q opts)]
      (if (:screen_name q)
        (mapv :screen_name result)
        result))))
