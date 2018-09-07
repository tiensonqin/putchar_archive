(ns api.db.repo
  (:require [api.db.user :as u]
            [ssr.auth :as auth]
            [api.db.token :as token]
            [api.services.github.repo :as repo]
            [ring.util.response :as resp]))

(defn setup!
  [req conn uid github_id github_handle api?]
  (let [repo-name "putchar-blog"
        m {:name repo-name
           :description "My blog."
           :homepage (str "https//putchar.org/@" github_handle)
           :private false
           :has_issues true
           :has_projects true
           :has_wiki true}
        token (token/get-token conn github_id)]
    (if token
      (do
        ;; 1. create a repo
        (repo/create m
                     {:oauth-token token})

        ;; 2. setup a webhook which listens to all push events.
        (repo/add-hook github_handle repo-name
                       {:name "web"
                        :active true
                        :events ["push"]
                        :config {:url "https://putchar.org/github/push",
                                 :content_type "json"}}
                       {:oauth-token token})

        (u/update conn uid {:github_repo (str "https://github.com/" github_handle "/" repo-name)
                            :github_id github_id
                            :github_handle github_handle}))
      ;; FIXME: token not found, user already revoked access, should ask for permissions again.
      )
    (when-not api?
      (-> {:status 302}
         (resp/header "Location" (or (let [url (get-in req [:params :referer])]
                                       (if url
                                         (str url "#github-repo")))
                                     ""))))))
