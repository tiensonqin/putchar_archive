(ns api.services.github.webhook-push
  (:require [api.services.github :as github]
            [api.services.github.contents :as contents]
            [api.services.github.commit :as commit]
            [api.services.commits :as commits]
            [api.services.slack :as slack]
            [share.dicts :as dicts]
            [api.db.user :as u]
            [api.db.post :as post]
            [clojure.string :as str]
            [share.util :as util]
            [share.front-matter :as fm]))

(defn handle
  [db req]
  (let [{:keys [head_commit commits repository ref pusher sender]} (get-in req [:params])
        head-id (:id head_commit)]
    ;; (slack/debug {:head-id head-id
    ;;               :exists? (commits/exists? head-id)})
    ;; pass if commited using web
    (try
      (when-not (commits/exists? head-id)
       (let [[owner repo] (some-> (:full_name repository)
                                  (str/split #"/"))
             {:keys [name email]} pusher
             github-id (str (:id sender))
             {:keys [github_repo_map] :as user} (and github-id (u/get-by-github-id db github-id))
             repo-map (if github_repo_map (read-string github_repo_map) {})]
         (doseq [{:keys [id]} commits]
           (let [{:keys [files]} (commit/get-commit owner repo id)]
             (doseq [{:keys [status previous_filename filename additions changes deletions] :as file} files]
               (cond
                 (and (= status "renamed")
                      (zero? additions)
                      (zero? changes)
                      (zero? deletions))                   ; do nothing
                 (u/github-rename-path db (:id user) repo-map previous_filename filename)

                 (= status "removed")
                 nil

                 :else
                 (when-let [content (contents/get owner repo filename)]
                   (let [{:keys [encoding content]} content
                         ext (util/get-file-ext filename)
                         body-format (cond (contains? #{"org"} ext)
                                           "org-mode"

                                           (contains? #{"markdown" "md"} ext)
                                           "markdown"

                                           :else
                                           (do
                                             (slack/error "not supported file ext: " filename ", user: " user)
                                             nil))]
                     (when body-format
                       (if (= encoding "base64")
                        (let [body (github/base64-decode
                                    (str/replace content "\n" ""))
                              m (fm/extract body)
                              post-data (merge m {:user_id (:id user)
                                                  :user_screen_name (:screen_name user)
                                                  :body_format body-format})]
                          (cond
                            (= status "renamed")
                            (when-let [id (get repo-map previous_filename)]
                              (post/update db id post-data)
                              (u/github-rename-path db (:id user) repo-map previous_filename filename))

                            (= status "modified")
                            (when-let [id (get repo-map filename)]
                              (post/update db id post-data))

                            (= status "added")
                            (when-let [post (post/create db post-data)]
                              (u/github-add-path db (:id user) repo-map filename (:id post)))

                            :else
                            (slack/error "Don't know how to handle this commit: " file)))

                        ;; error report
                        (slack/error "wrong encoding: " encoding ".\nRequest: "
                                     req)))))))))))
      (catch Exception e
        (slack/error "Github push error: " e)))))
