(ns share.routes
  (:require [clojure.string :as str]
            [bidi.bidi :as bidi]
            [share.util :as util]
            [share.config :as config]
            #?(:cljs [web.scroll :as scroll])
            #?(:cljs [goog.dom :as gdom])))

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
        ["votes"                                              :votes]
        ["stats"                                                  :stats]
        ["newest"                                                 :newest]
        ["latest-reply"                                           :latest-reply]
        ;; rss
        ["newest.rss"                                             :new-rss]
        ["hot.rss"                                                :hot-rss]
        ["latest-reply.rss"                                       :latest-reply-rss]
        [["@" :screen_name "/newest.rss"]                         :user-latest-rss]

        ["search"                                                 :search]
        ["reports"                                                :reports]
        ["moderation-logs"                                                :moderation-logs]
        ["new-post"                                                    :new-post]
        [["@" :screen_name "/comments"]   :comments]

        [["tag/" [#"[^\/]+" :tag]]                                             :tag]

        [["@" :screen_name "/tag/" [#"[^\/]+" :tag]]                          :user-tag]

        ["drafts"                                                 :drafts]
        [["p/" :post-id "/edit"]                                              :post-edit]
        [["@" :screen_name "/" [#"[^\/]+" :permalink]]                        :post]
        [["@" :screen_name "/" [#"[^\/]+" :permalink] "/" :comment-idx]        :comment]

        ;; books
        ["books"                                                 :books]
        [["book/" [ #"\d+" :book-id ]]                                      :book]
        ["new-book"                                                 :new-book]
        [["book/" [ #"\d+" :book-id ] "/edit"]                              :book-edit]

        ;; papers
        ["papers"                                                 :papers]
        [["paper/" [ #"\d+" :paper-id ]]                                     :paper]
        ["new-paper"                                                 :new-paper]
        [["paper/" [ #"\d+" :paper-id ] "/edit"]                             :paper-edit]]])

(defn match-route-with-query-params
  [path & {:as options}]
  (let [query-params (util/query->map path)
        result (bidi/match-route routes path)]
    (update result :route-params
            (fn [params]
              (let [params (if (:tag params)
                             (update params :tag bidi/url-decode)
                             params)
                    params (if (seq query-params)
                             (merge params query-params)
                             params)
                    params (if (:paper-id params)
                             (update params :paper-id util/parse-int)
                             params)
                    params (if (:book-id params)
                             (update params :book-id util/parse-int)
                             params)
                    params (if (:comment-idx params)
                             (update params :comment-idx util/parse-int)
                             params)
                    params (if (:post-id params)
                             (update params :post-id util/uuid)
                             params)]
                params)))))

(def login-routes
  #{:me :notifications :profile
    :votes :reports :post-edit})

;; (extend-protocol bidi/ParameterEncoding
;;   #?(:clj java.util.UUID
;;      :cljs cljs.core/UUID)
;;   (bidi/encode-parameter [s] (str s)))
