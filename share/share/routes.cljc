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
        [["@" :screen_name "/links"]                             :links]
        ["votes"                                                  :votes]
        ["bookmarks"                                                  :bookmarks]
        ["newest"                                                 :newest]
        ["latest-reply"                                           :latest-reply]
        ;; rss
        ["newest.rss"                                             :new-rss]
        ["hot.rss"                                                :hot-rss]
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
        [[[#"[^\/]+" :group-name] "/" [#"(newest|hot|latest-reply|wiki)" :post-filter]]           :group]
        [[[#"[^\/]+" :group-name] "/members"]                                  :members]
        [[[#"[^\/]+" :group-name] "/edit"]                                     :group-edit]
        [[[#"[^\/]+" :group-name] "/new-channel"]                              :new-channel]
        [[[#"[^\/]+" :group-name] "/channels"]                                 :channels]
        [[[#"[^\/]+" :group-name] "/" [#"[^\/]+" :channel-name]]               :channel]
        [[[#"[^\/]+" :group-name] "/" [#"[^\/]+" :channel-name] "/newest.rss"] :channel-latest-rss]
        [[[#"[^\/]+" :group-name] "/" [#"[^\/]+" :channel-name] "/hot.rss"]    :channel-hot-rss]
        [[[#"[^\/]+" :group-name] "/" [#"[^\/]+" :channel-name] "/latest-reply.rss"]    :channel-latest-reply-rss]
        [[[#"[^\/]+" :group-name] "/" [#"[^\/]+" :channel-name] "/" [#"(newest|hot|latest-reply|wiki)" :post-filter]]               :channel]
        [[[#"[^\/]+" :group-name] "/" [#"[^\/]+" :channel-name] "/edit"]       :channel-edit]
        ]])

(defn match-route-with-query-params
  [path & {:as options}]
  (let [query-params (util/query->map path)
        result (bidi/match-route routes path)]
    (update result :route-params
            (fn [params]
              (cond-> params
                (:group-name params)
                (update :group-name bidi/url-decode)
                (:channel-name params)
                (update :channel-name bidi/url-decode)
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
