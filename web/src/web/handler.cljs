(ns web.handler
  (:require [web.handlers.default :as default]
            [web.handlers.ui :as ui]
            [web.handlers.router :as router]
            [web.handlers.user :as user]
            [web.handlers.group :as group]
            [web.handlers.item :as item]
            [web.handlers.channel :as channel]
            [web.handlers.post :as post]
            [web.handlers.post-box :as post-box]
            [web.handlers.message :as message]
            [web.handlers.search :as search]
            [web.handlers.image :as image]
            [web.handlers.comment :as comment]
            [web.handlers.report :as report]
            [web.handlers.notification :as notification]
            [web.handlers.query :as query]
            ))

(def handler
  (atom
   (merge
    default/handlers
    ui/handlers
    router/handlers
    user/handlers
    group/handlers
    item/handlers
    channel/handlers
    post/handlers
    post-box/handlers
    message/handlers
    search/handlers
    image/handlers
    comment/handlers
    report/handlers
    notification/handlers
    query/handlers)))
