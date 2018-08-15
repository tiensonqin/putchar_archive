(ns api.services.github.webhook-push
  (:require [api.services.github :as github]
            [api.services.github.contents :as contents]
            [api.services.github.commit :as commit]
            [api.services.commits :as commits]
            [api.services.slack :as slack]
            [api.db.user :as u]
            [api.db.post :as post]
            [api.db.group :as group]
            [api.db.channel :as channel]
            [clojure.string :as str]
            [share.util :as util]))

;; asciidoctor comment regex
(def spec-re #"////([^\[]+)////")
(defn extract-spec
  [content]
  (when-not (str/blank? content)
    (let [result (re-find spec-re content)
          spec (when (>= (count result) 2)
                 (let [spec (nth result 1)]
                   (->> (str/split-lines spec)
                        (remove str/blank?)
                        (mapv (fn [x]
                                (let [result (->> (str/split x #"\s+")
                                                  (remove str/blank?))
                                      [k v] (if (= ":tags" (first result))
                                              (if (> (count result) 2)
                                                [":tags" (apply str (rest result))]
                                                result)
                                              result)]
                                  [(keyword (subs k 1)) (case v
                                                          "true" true
                                                          "false" false
                                                          v)])))
                        (into {}))))]
      (let [content (str/trim content)
            [title body] (util/split-first-line content)]
        (assoc spec
               :title (str/replace-first title #"[=#\s]*" "")
               :body body)))))

(defn handle
  [db req]
  (let [{:keys [head_commit commits repository ref pusher sender]} (get-in req [:params])
        head-id (:id head_commit)]
    (slack/debug {:head-id head-id
                  :exists? (commits/exists? head-id)})
    ;; pass if commited using web
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
               (when-let [id (get repo-map filename)]
                 (post/delete db id)
                 (u/github-delete-path db (:id user) repo-map filename))

               :else
               (when-let [content (contents/get owner repo filename)]
                 (let [{:keys [encoding content]} content
                       body-format (if (contains? #{"adoc" "asciidoc"}
                                                  (util/get-file-ext filename))
                                     "asciidoc"
                                     "markdown")]
                   (if (= encoding "base64")
                     (let [body (github/base64-decode
                                 (str/replace content "\n" ""))
                           {:keys [title body group channel tags is_draft is_wiki] :as spec} (extract-spec body)
                           group (util/internal-name group)
                           group (if group
                                   (group/get db group))
                           channel (if group
                                     (channel/get db {:group-name (:name group)
                                                      :channel-name (if channel (util/internal-name channel) "general")}))
                           post-data (let [is_draft (if is_draft is_draft false)
                                           permalink (when-not is_draft
                                                       (post/permalink (:screen_name user) title))]
                                       (cond->
                                         {:user_id (:id user)
                                          :user_screen_name (:screen_name user)
                                          :title title
                                          :body body
                                          :body_format body-format
                                          :tags tags
                                          :is_draft is_draft
                                          :is_wiki (if is_wiki is_wiki false)}

                                         group
                                         (assoc :group_id (:id group)
                                                :group_name (:name group)
                                                :channel_id (:id channel)
                                                :channel_name (:name channel))

                                         permalink
                                         (assoc :permalink permalink)))]
                       (slack/debug (merge post-data
                                           {:status status}))
                       (cond
                         (= status "renamed")
                         (when-let [id (get repo-map previous_filename)]
                           (post/update db id (dissoc post-data :permalink))
                           (u/github-rename-path db (:id user) repo-map previous_filename filename))

                         (= status "modified")
                         (when-let [id (get repo-map filename)]
                           (post/update db id (dissoc post-data :permalink)))

                         (= status "added")
                         (when-let [post (post/create db post-data)]
                           (u/github-add-path db (:id user) repo-map filename (:id post)))

                         :else
                         (slack/error "Don't know how to handle this commit: " file)))

                     ;; error report
                     (slack/error "wrong encoding: " encoding ".\nRequest: "
                                  req))))))))))))
