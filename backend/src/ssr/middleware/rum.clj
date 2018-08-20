(ns ssr.middleware.rum
  (:require [rum.core :as rum]
            [share.reconciler :as r]
            [ssr.auth :as auth]
            [clojure.java.jdbc :as j]
            [api.db.user :as u]
            [api.db.util :as du]
            [api.db.post :as post]
            [api.db.group :as group]
            [api.db.channel :as channel]
            [api.db.token :as token]
            [ring.util.response :as resp]
            [api.config :as config]
            [api.util :as util]
            [share.config :as c]
            [clj-social.core :as social]
            [ssr.page :as page]
            [api.cookie :as cookie]
            [clojure.string :as str]
            [ssr.sitemap :as sitemap]
            [api.services.rss.export :as rss]
            [api.services.slack :as slack]
            [api.services.stripe :as stripe]
            [api.services.github.webhook-push :as push]
            [api.services.github.repo :as repo]
            [api.services.github.user :as github-user]
            [share.components.root :as root]
            [api.db.task :as task]
            [cheshire.core :refer [generate-string]]
            [cemerick.url :as url]))

(defn get-referer
  [req]
  (if-let [referer (get-in req [:params :referer])]
    (let [m (url/url referer)]
      (if (= (get-in m [:query "utm_source"]) "pwa")
        ""
        referer))
    ""))
(defn- render
  [req resolver ui-root render-page res after-resolver]
  (let [state (resolver req)
        state (if after-resolver (after-resolver state) state)]
    (reset! (:state r/reconciler) state)
    (assoc res
           :body
           (-> (if (:not-found state)
                 nil
                 (ui-root state))
               (rum/render-html)
               (render-page req state)))))

;; render web app
(defn wrap-rum [handler ui-root resolver render-page]
  (fn [req]
    (let [uri (:uri req)
          route (:ui/route req)
          datasource (get-in req [:context :datasource])
          uid (get-in req [:context :uid])
          website-path (fn [& others] (apply str c/website "/" others))]
      (cond
        ;; export
        (= "/user/lambdahackers_profile.json" (:uri req))
        (j/with-db-connection [conn datasource]
          (let [content (-> (task/export conn uid)
                            (generate-string))]
            {:status 200
             :headers {"Content-Type" "application/x-lambdahackers-profile"
                       "Content-Disposition" "attachment; filename=lambdahackers_profile.json"}
             :body content}))

        (= "/user/delete_request" (:uri req))
        (j/with-db-connection [conn datasource]
          (task/delete-account conn uid)
          (-> (resp/redirect (:website-uri config/config))
              (assoc :cookies cookie/delete-token)))

        (= "/stripe_webhook" (:uri req))
        (let [params (:params req)]
          (slack/debug params)
          ;; (cond
          ;;   (= (:type params) "source.chargeable")
          ;;   (let [{:keys [amount type status id livemode metadata]} (get-in params [:data :object])]
          ;;     (when (and (= type "alipay")
          ;;                (= status "chargeable"))
          ;;       (slack/error (stripe/create-customer {:plan "pro-member"
          ;;                                             :source id
          ;;                                             :email (:email metadata)}))))

          ;;   (= (:type params) "charge.succeeded")
          ;;   (do
          ;;     ;; TODO: send email
          ;;     nil)

          ;;   :else
          ;;   nil)
          {:status 200
           :body "ok"})

        ;; github push events
        (= "/github/push" (:uri req))
        (do
          (j/with-db-connection [conn datasource]
            (push/handle conn req))
          {:status 200
           :body "ok"})

        (= "/github/setup-sync" (:uri req))
        (let [uid (get-in req [:context :uid])]
          (cond
            (nil? uid)
            {:status 400
             :body {:message "Please login first."}}

            :else
            (let [{:keys [app-key app-secret]} (get-in config/config [:oauth :github])
                  tt (social/make-social :github app-key app-secret
                                         (str (get-in config/config
                                                      [:oauth :github :redirect-uri])
                                              "?ask_public_repo=true&referer=" (get-in req [:headers "referer"] ""))
                                         :state (str (util/uuid))
                                         :scope "public_repo")
                  url (social/getAuthorizationUrl tt)]
              (resp/redirect url))))

        ;; github login
        (= "/auth/github" (:uri req))
        (let [uid (get-in req [:context :uid])]
          (cond
            ;; granted public repo permission
            (and uid
                 (:code (:params req))
                 (:state (:params req))
                 (get-in req [:params :ask_public_repo]))
            (j/with-db-connection [conn datasource]
              (let [info (auth/github (:context req) (:params req))
                    [github_id github_handle] (if (uuid? (:id info))
                                  [(get info :github_id) (get info :github_handle)]
                                  [(str (:id info)) (:login info)])
                    repo-name "lambdahackers-blog"
                    m {:name repo-name,
                       :description "My blog."
                       :homepage (str "https//lambdahackers.com/@" github_handle)
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
                                   :config {:url "https://lambdahackers.com/github/push",
                                            :content_type "json"}}
                                  {:oauth-token token})

                   (u/update conn uid {:github_repo (str "https://github.com/" github_handle "/" repo-name)
                                       :github_id github_id
                                       :github_handle github_handle}))
                 ;; FIXME: token not found, user already revoked access, should ask for permissions again.
                 )
               (-> {:status 302}
                   (resp/header "Location" (or (let [url (get-in req [:params :referer])]
                                                 (if url
                                                   (str url "#github-repo")))
                                               "")))))

            ;; login
            (and uid
                 (:code (:params req))
                 (:state (:params req)))
            (let [info (auth/github (:context req) (:params req))
                  [id handle] (if (uuid? (:id info))
                                [(get info :github_id) (get info :github_handle)]
                                [(:id info) (:login info)])]
              (j/with-db-connection [conn datasource]
                (u/update conn uid {:github_id id
                                    :github_handle handle}))
              (-> {:status 302}
                  (resp/header "Location" (or (let [url (get-in req [:params :referer])]
                                                (if url
                                                  (str url "#github-repo")))
                                              ""))))

            (and (:code (:params req))
                 (:state (:params req)))
            (let [user (auth/github (:context req) (:params req))]
              (if (uuid? (:id user))
                (let [uid (:id user)]
                  (-> req
                      (assoc-in [:context :uid] uid)
                      (assoc-in [:ui/route :handler] :home)
                      (render resolver ui-root render-page (handler req)
                        (fn [state] (assoc-in state [:router :handler] :home)))
                      (assoc :cookies
                             (j/with-db-connection [conn datasource]
                               (u/generate-tokens conn user))
                             :status 302)
                      (resp/header "Location" (get-referer req))))
                (let [
                      ;; user (if (:email user)
                      ;;        user
                      ;;        (let [token (j/with-db-connection [conn datasource]
                      ;;                      (token/get-token conn (str (:id user))))
                      ;;              emails (github-user/get-emails {:oauth-token token})]
                      ;;          (prn emails)
                      ;;          user))
                      user user]
                  (-> req
                      (assoc-in [:ui/route :handler] :signup)
                      (render resolver ui-root render-page (handler req)
                        (fn [state]
                          (-> state
                              (assoc-in [:user :temp] user)
                              (assoc-in [:router :handler] :signup))))))))

            :else
            (let [{:keys [app-key app-secret]} (get-in config/config [:oauth :github])
                  tt (social/make-social :github app-key app-secret
                                         (str (get-in config/config
                                                      [:oauth :github :redirect-uri])
                                              "?referer=" (get-in req [:headers "referer"] ""))
                                         :state (str (util/uuid))
                                         ;; :scope "public_repo"
                                         )
                  url (social/getAuthorizationUrl tt)]
              (resp/redirect url))))


        (= "/email_confirmation" (:uri req))
        (if-let [code (get-in req [:params :code])]
          (j/with-db-transaction [conn (get-in req [:context :datasource])]
            (let [email (u/validate-code conn code)]
              (cond
                (nil? email)
                {:status 200
                 :body (page/render-page
                        [:div.center
                         [:h1 "Sorry, this confirmation link is expired."]]
                        {}
                        {})}

                (not (du/exists? conn :users {:email email}))
                (resp/redirect (str (:website-uri config/config)
                                    "/signup?"
                                    "email="
                                    email))

                ;; old user with cookie
                :else
                (if-let [user (u/get-by-email conn email)]
                  (-> (resp/redirect (:website-uri config/config))
                      (assoc :cookies
                             (u/generate-tokens conn user)
                             :status 302))
                  (resp/redirect (str (:website-uri config/config) "/not-found"))))))
          (resp/redirect (str (:website-uri config/config) "/not-found")))

        (= "/logout" (:uri req))
        (-> (resp/redirect (:website-uri config/config))
            (assoc :cookies cookie/delete-token))

        ;; whole site rss
        (= :new-rss (:handler route))
        (util/rss {:title "lambdahackers"
                   :link (website-path "newest")
                   :description "Latest posts on lambdahackers.com"}
                  (j/with-db-connection [conn datasource]
                    (post/->rss (post/get-new conn {:limit 20}))))

        (= :latest-reply-rss (:handler route))
        (util/rss {:title "lambdahackers"
                   :link (website-path "latest-reply")
                   :description "Latest replied posts on lambdahackers.com"}
                  (j/with-db-connection [conn datasource]
                    (post/->rss (post/get-latest-reply conn {:limit 20}))))

        (= :hot-rss (:handler route))
        (util/rss {:title "lambdahackers"
                   :link (website-path "hot")
                   :description "Hot posts on lambdahackers.com"}
                  (j/with-db-connection [conn datasource]
                    (post/->rss (post/get-hot conn {:limit 20}))))

        ;; user rss
        (= :user-latest-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [screen-name (get-in route [:route-params :screen_name])
                u (u/get conn screen-name)]
            (if u
              (util/rss {:title (:name u)
                         :link (website-path (str "@" screen-name))
                         :description (:bio u)}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-user-new conn nil (:id u) {:limit 20}))))
              util/not-found-resp)))

        ;; group rss
        (= :group-latest-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [group-name (get-in route [:route-params :group-name])
                group (group/get conn group-name)]
            (if group
              (util/rss {:title group-name
                         :link (website-path group-name)
                         :description (:purpose group)}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-group-new conn (:id group) {:limit 20}))))
              util/not-found-resp)))

        ;; group rss
        (= :group-hot-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [group-name (get-in route [:route-params :group-name])
                group (group/get conn group-name)]
            (if group
              (util/rss {:title group-name
                         :link (website-path group-name)
                         :description (:purpose group)}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-group-hot conn (:id group) {:limit 20}))))
              util/not-found-resp)))

        ;; group rss
        (= :group-latest-reply-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [group-name (get-in route [:route-params :group-name])
                group (group/get conn group-name)]
            (if group
              (util/rss {:title group-name
                         :link (website-path group-name)
                         :description (:purpose group)}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-group-latest-reply conn (:id group) {:limit 20}))))
              util/not-found-resp)))

        (= :channel-latest-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [{:keys [group-name channel-name] :as params} (get-in route [:route-params])
                channel (channel/get conn params)]
            (if channel
              (util/rss {:title (str group-name " - " channel-name)
                         :link (website-path (str group-name "/" channel-name))
                         :description (if-let [purpose (:purpose channel)]
                                        purpose
                                        (str group-name " - " channel-name))}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-channel-new conn (:id channel) {:limit 20}))))
              util/not-found-resp)))

        (= :channel-hot-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [{:keys [group-name channel-name] :as params} (get-in route [:route-params])
                channel (channel/get conn params)]
            (if channel
              (util/rss {:title (str group-name " - " channel-name)
                         :link (website-path (str group-name "/" channel-name))
                         :description (if-let [purpose (:purpose channel)]
                                        purpose
                                        (str group-name " - " channel-name))}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-channel-hot conn (:id channel) {:limit 20}))))
              util/not-found-resp)))

        (= :channel-latest-reply-rss (:handler route))
        (j/with-db-connection [conn datasource]
          (let [{:keys [group-name channel-name] :as params} (get-in route [:route-params])
                channel (channel/get conn params)]
            (if channel
              (util/rss {:title (str group-name " - " channel-name)
                         :link (website-path (str group-name "/" channel-name))
                         :description (if-let [purpose (:purpose channel)]
                                        purpose
                                        (str group-name " - " channel-name))}
                        (j/with-db-connection [conn datasource]
                          (post/->rss (post/get-channel-latest-reply conn (:id channel) {:limit 20}))))
              util/not-found-resp)))

        (= "/sitemap.xml" (:uri req))
        {:status 200
         :headers { "Content-type" "text/xml; charset=utf-8" }
         :body (sitemap/sitemap datasource) }

        (not (:ui/route req))
        ;; not found
        util/not-found-resp

        :else
        (render req resolver ui-root render-page (handler req) nil)))))
