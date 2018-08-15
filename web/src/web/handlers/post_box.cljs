(ns web.handlers.post-box
  (:require [share.query :as query]
            [share.util :as util]
            [share.config :as config]
            [share.dicts :refer [t]]
            [share.dommy :as dommy]
            [share.content :as content]
            [goog.object :as gobj]
            [web.scroll :as scroll]
            [web.md5 :as md5]
            [clojure.string :as str]))

;; common things between comment, post and item (future support)
;; mentions, pictures, emojis, links

(defn join-mention
  [body pos screen-name]
  (let [before (subs body 0 pos)
        n (count (content/get-mention before))
        head-part (str (subs body 0 (- pos n))
                       screen-name ": ")
        new-pos (count head-part)]
    [new-pos (str head-part (subs body pos))]))

(defn join-emoji
  [body pos keyword]
  (let [before (subs body 0 pos)
        n (count (content/get-emoji before))
        head-part (str (subs body 0 (- pos n))
                       keyword ":")
        new-pos (count head-part)]
    [new-pos (str head-part (subs body pos))]))

(def handlers
  {:post-box/set-cursor-position
   (fn [state v]
     {:state {:cursor-position v}})

   :citrus/auto-complete
   (fn [state body]
     (let [current-user (get-in state [:user :current])
           pos (get-in state [:post-box :cursor-position])
           mention (and body current-user pos
                        (content/get-mention (subs body 0 pos)))
           emoji (and body current-user pos
                      (content/get-emoji (subs body 0 pos)))]
       (cond
         mention
         {:state state
          :dispatch [:search/search :user/search {:q {:screen_name mention}}]}

         emoji
         {:state state
          :dispatch [:search/emojis emoji]}

         :else
         {:state (assoc-in state [:search :result] nil)})))

   :citrus/add-mention
   (fn [state type id screen-name]
     (let [pos (get-in state [:post-box :cursor-position])
           [new-pos body dispatch-data] (case type
                                  :post
                                  (let [[new-pos body] (-> (get-in state [:post :form-data :body])
                                                 (join-mention pos screen-name))]
                                    [new-pos body [:citrus/set-post-form-data {:body body} {:completed? true}]])

                                  :comment
                                  (let [[new-pos body] (-> (get-in state [:comment :drafts id])
                                                 (join-mention pos screen-name))]
                                    [new-pos body [:comment/save-local id body]]))
           state (-> state
                     (assoc-in [:search :result] nil)
                     (assoc-in [:post-box :cursor-position] (count body)))]
       {:state state
        :dispatch [dispatch-data
                   [:post-box/set-focus-position new-pos]]}))

   :citrus/add-emoji
   (fn [state type id keyword]
     (let [pos (get-in state [:post-box :cursor-position])
           [new-pos body dispatch-data] (case type
                                  :post
                                  (let [[new-pos body] (-> (get-in state [:post :form-data :body])
                                                 (join-emoji pos keyword))]
                                    [new-pos body [:citrus/set-post-form-data {:body body} {:completed? true}]])

                                  :comment
                                  (let [[new-pos body] (-> (get-in state [:comment :drafts id])
                                                 (join-emoji pos keyword))]
                                    [new-pos body [:comment/save-local id body]]))
           state (-> state
                     (assoc-in [:search :emojis-result] nil)
                     (assoc-in [:post-box :cursor-position] (count body)))]
       {:state state
        :dispatch [dispatch-data
                   [:post-box/set-focus-position new-pos]]}))

   :post-box/set-focus-position
   (fn [state new-pos]
     (when-let [element (dommy/sel1 "#post-box")]
       (.focus element)
       ;; TODO: :ugly:
       (util/set-timeout
        10
        (fn [] (.setSelectionRange element new-pos new-pos))))
     {:state state})

   :citrus/add-picture
   (fn [state temp-id picture]
     (let [body-format (or (get-in state [:post :form-data :body_format])
                           (get-in state [:latest-body-format])
                           :asciidoc)
           cursor-position (get-in state [:post-box :cursor-position] 0)
           {:keys [body cover images]} (get-in state [:post :form-data])
           new-body (let [[before after] (if (str/blank? body) nil [(subs body 0 cursor-position)
                                                                    (subs body cursor-position (count body))])
                          image-part (if (= :markdown body-format)
                                       (str
                                        (cond
                                          (str/blank? body)
                                          ""

                                          :else
                                          "\n\n")
                                        "!["
                                        (:url picture)
                                        "]("
                                        (util/get-file-base-name (gobj/get (:file picture) "name"))
                                        ")"
                                        "\n")
                                       (str
                                        (cond
                                          (str/blank? body)
                                          ""

                                          :else
                                          "\n\n")
                                        "image::"
                                        (:url picture)
                                        "["
                                        (util/get-file-base-name (gobj/get (:file picture) "name"))
                                        "]"
                                        "\n"))]
                      (if (str/blank? body)
                        image-part
                        (str (str/trim-newline before) image-part (str/trim-newline after))))
           new-cover (if (nil? cover)
                       (:url picture)
                       cover)
           new-images (assoc images
                             temp-id {:processing? false
                                      :url (:url picture)})]
       {:state {:cursor-position (count new-body)}
        :dispatch [:citrus/set-post-form-data {:body new-body
                                               :cover new-cover
                                               :images new-images}]}))
   })
