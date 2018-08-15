(ns share.seo
  (:require [share.util :as util]
            [share.dicts :refer [t]]
            [share.config :refer [img-cdn] :as config]
            [share.asciidoc :as ascii]))

(defn get-ascii-description
  [body]
  (if body
    (str (apply str (take 300 (util/full-strip (ascii/render body))))
        "...")))

(defn seo-title-content
  [handler route-params state]
  (let [website-logo (str config/website "/logo-2x.png")
        [title description photo] (case handler
                                    :post
                                    ;; by-permalink
                                    (let [post (get-in state [:post :by-permalink
                                                              (str "@"
                                                                   (:screen_name route-params)
                                                                   "/"
                                                                   (:permalink route-params))])]
                                      [(:title post)
                                       (get-ascii-description (:body post))
                                       (if (:cover post)
                                         (:cover post)
                                         (util/cdn-image (get-in post [:user :screen_name]) :width 300 :height 300))])

                                    :group
                                    ;; by-name
                                    (let [name (util/internal-name (:group-name route-params))
                                          group (get-in state [:group :by-name name])]
                                      [(util/original-name name) (get-ascii-description (:purpose group)) (util/group-logo name 300 300)])

                                    :channel
                                    ;; by-id
                                    (let [channel-id (get-in state [:channel :current])
                                          channel (get-in state [:channel :by-id channel-id])
                                          group-name (get-in channel [:group :name])]
                                      [(str group-name
                                            " - "
                                            (:name channel))
                                       (:purpose channel)
                                       (util/group-logo group-name 300 300)])

                                    :user
                                    ;; by-screen-name
                                    (let [screen-name (:screen_name route-params)
                                          user (get-in state [:user :by-screen-name screen-name])]
                                      [(:name user) (:bio user) (util/cdn-image screen-name :width 300 :height 300)])

                                    :groups
                                    [(t :groups) (t :lambdahackers-hot-groups) website-logo]

                                    :new-post
                                    [(t :write-new-post) (t :new-post-description) website-logo]

                                    ;; default
                                    [(t :root-title) (t :root-description) website-logo])]
    (if photo
      [title description photo]
      [title description website-logo])))
