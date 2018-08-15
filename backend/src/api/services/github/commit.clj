(ns api.services.github.commit
  (:refer-clojure :exclude [get update])
  (:require [api.services.github :as github]
            [api.services.github.contents :as contents]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]))

(defn get-commit
  [owner repo sha & [options]]
  (github/api-call :get "repos/%s/%s/commits/%s"
                   [owner repo sha] options))

(defn get-ref
  [owner repo & [options]]
  (github/api-call :get "repos/%s/%s/git/refs/heads/master"
                   [owner repo] options))

(defn- ->body
  [url]
  (-> @(http/get url)
      :body
      (parse-string true)))

(defn create-blob
  [owner repo content encoding & [options]]
  (github/api-call :post "repos/%s/%s/git/blobs"
                   [owner repo]
                   (merge
                    {:content content
                     :encoding encoding}
                    options)))

(defn create-tree
  [owner repo tree base-tree & [options]]
  (github/api-call :post "repos/%s/%s/git/trees"
                   [owner repo]
                   (merge {:tree tree
                           :base_tree base-tree}
                          options)))

(defn create-commit
  [owner repo message parents tree & [options]]
  (github/api-call :post "repos/%s/%s/git/commits"
                   [owner repo]
                   (merge options
                          {:message message
                           :parents parents
                           :tree tree})))

(defn update-ref
  [owner repo sha force & [options]]
  (github/api-call :patch "repos/%s/%s/git/refs/heads/master"
                   [owner repo]
                   (merge options
                          {:sha sha
                           :force force})))

;; http://www.levibotelho.com/development/commit-a-file-with-the-github-api/
(defn auto-commit
  [owner repo path content encoding message & [options]]
  (let [
        ;; 1. Get a reference to HEAD
        {:keys [object]} (get-ref owner repo)
        {:keys [sha url]} object

        ;; 2. Grab the commit that HEAD points to
        {:keys [tree parents] :as result} (->body url)

        ;; 3. Post your new file to the server
        blob (create-blob owner repo
                          content
                          encoding
                          options)

        _ (if (= 404 (:status blob))
            (throw (Exception. "Github commit wrong!")))
        ;; 4. Get a hold of the tree that the commit points to
        tree (->body (:url tree))

        ;; 5. Create a tree containing your new file
        new-tree (create-tree owner repo
                              [{:path path
                                :mode "100644"
                                :type "blob"
                                :sha (:sha blob)}]
                              (:sha tree)
                              options)

        ;; 6. Create a new commit
        commit (create-commit owner repo
                              message
                              (mapv :sha parents)
                              (:sha new-tree)
                              options)]
    ;; 7. Update HEAD
    (update-ref owner repo (:sha commit) true options)

    commit))

(def delete contents/delete)

(comment
  (:require '[api.db.user :as u]))
