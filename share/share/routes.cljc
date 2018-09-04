(ns share.routes
  (:require [clojure.string :as str]
            [bidi.bidi :as bidi]
            [share.util :as util]
            [share.config :as config]))

(def routes
  ["/" [[""                                                       :home]
        ["about"                                                 :about]
        ["signup"                                                 :signup]
        ["login"                                                  :login]
        ["auth"                                                   :auth]
        ["notifications"                                          :notifications]
        ["settings"                                               :profile]
        [["@" :screen_name]                                       :user]
        ["votes"                                                  :votes]
        ["bookmarks"                                              :bookmarks]
        ["stats"                                                  :stats]
        ["newest"                                                 :newest]
        ["non-tech"                                               :non-tech]
        ["latest-reply"                                           :latest-reply]
        ;; rss
        ["newest.rss"                                             :new-rss]
        ["hot.rss"                                                :hot-rss]
        ["non-tech.rss"                                           :non-tech-rss]
        ["latest-reply.rss"                                       :latest-reply-rss]
        [["@" :screen_name "/newest.rss"]                         :user-latest-rss]
        [[[#"[^\/]+" :group-name] "/newest.rss"]                  :group-latest-rss]
        [[[#"[^\/]+" :group-name] "/hot.rss"]                     :group-hot-rss]
        [[[#"[^\/]+" :group-name] "/latest-reply.rss"]            :group-latest-reply-rss]

        ["search"                                                 :search]
        ["reports"                                                :reports]
        ["new-post"                                                    :new-post]
        ["privacy"                                                :privacy]
        ["terms"                                                  :terms]
        ["code-of-conduct"                                        :code-of-conduct]
        [["@" :screen_name "/comments"]   :comments]

        [["tag/" [#"[^\/]+" :tag]]                                             :tag]

        [["@" :screen_name "/tag/" [#"[^\/]+" :tag]]                          :user-tag]

        ["drafts"                                                 :drafts]
        [["p/" :post-id "/edit"]                                              :post-edit]
        [["@" :screen_name "/" [#"[^\/]+" :permalink]]                        :post]
        [["@" :screen_name "/" [#"[^\/]+" :permalink] "/" :comment-idx]        :comment]


        ["groups"                                                         :groups]
        ["new-group"                                                       :new-group]
        ;; ["pricing"                                                             :pricing]
        [[[#"[^\/]+" :group-name]]                                             :group]
        [[[#"[^\/]+" :group-name] "/" [#"(newest|hot|latest-reply)" :post-filter]]           :group]
        [[[#"[^\/]+" :group-name] "/members"]                                  :members]
        [[[#"[^\/]+" :group-name] "/edit"]                                     :group-edit]]])

(defn match-route-with-query-params
  [path & {:as options}]
  (let [query-params (util/query->map path)
        result (bidi/match-route routes path)]
    (update result :route-params
            (fn [params]
              (cond-> params
                (:tag params)
                (update :tag bidi/url-decode)

                (seq query-params)
                (merge query-params))))))

(def login-routes
  #{:me :notifications :profile
    :votes :reports :post-edit
    :new-group :group-edit
    })

;; (extend-protocol bidi/ParameterEncoding
;;   #?(:clj java.util.UUID
;;      :cljs cljs.core/UUID)
;;   (bidi/encode-parameter [s] (str s)))
