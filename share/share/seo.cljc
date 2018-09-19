(ns share.seo
  (:require [share.util :as util]
            [share.dicts :refer [t]]
            [share.config :refer [img-cdn] :as config]
            [share.markdown :as md]))

(defn get-description
  [body]
  (if body
    (str (apply str (take 300 (util/full-strip (md/render body))))
        "...")))

(defn seo-title-content
  [handler route-params state]
  (let [website-logo (str config/website "/logo-2x.png")
        [title description photo]
        (case handler
          :post
          ;; by-permalink
          (let [post (get-in state [:post :by-permalink
                                    (str "@"
                                         (:screen_name route-params)
                                         "/"
                                         (:permalink route-params))])]
            [(:title post)
             (get-description (:body post))
             (if (:cover post)
               (:cover post)
               (util/cdn-image (get-in post [:user :screen_name])))])

          :user
          ;; by-screen-name
          (let [screen-name (:screen_name route-params)
                user (get-in state [:user :by-screen-name screen-name])]
            [(or (:name user) screen-name) (:bio user) (util/cdn-image screen-name)])

          :new-post
          [(t :write-new-post) (t :new-post-description) website-logo]

          ;; default
          [(t :root-title) (t :root-description) website-logo])]
    (if photo
      [title description photo]
      [title description website-logo])))
