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
        [title description canonical-url photo]
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
             (:canonical_url post)
             (if (:cover post)
               (:cover post)
               (util/cdn-image (get-in post [:user :screen_name]) :width 300 :height 300))])

          :group
          ;; by-name
          (let [name (util/internal-name (:group-name route-params))
                group (get-in state [:group :by-name name])]
            [(util/original-name name) (get-description (:purpose group)) nil (util/group-logo name 300 300)])

          :user
          ;; by-screen-name
          (let [screen-name (:screen_name route-params)
                user (get-in state [:user :by-screen-name screen-name])]
            [(or (:name user) screen-name) (:bio user) nil (util/cdn-image screen-name :width 300 :height 300)])

          :groups
          [(t :groups) (t :lambdahackers-hot-groups) nil website-logo]

          :new-post
          [(t :write-new-post) (t :new-post-description) nil website-logo]

          ;; default
          [(t :root-title) (t :root-description) nil website-logo])]
    (if photo
      [title description canonical-url photo]
      [title description canonical-url website-logo])))
