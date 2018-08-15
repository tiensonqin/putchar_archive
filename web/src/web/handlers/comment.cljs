(ns web.handlers.comment
  (:require [share.dicts :refer [t]]
            [share.util :as util]
            [share.config :as config]
            [appkit.macros :refer [oget oset!]]
            [share.dommy :as dommy]
            [clojure.string :as str]
            [goog.dom :as gdom]
            [web.scroll :as scroll]))

(defn save-local
  ([state k body]
   (save-local state k body nil))
  ([state k body {:keys [completed?]
                  :or {completed? false}}]
    (when (not (str/blank? body))
      (when-let [btn (dommy/sel1 "#comment-box-btn")]
        (when (dommy/has-class? btn "disabled")
          (dommy/remove-class! btn "disabled")
          (dommy/add-class! btn "btn-primary"))))
    (cond->
      {:state (assoc-in state [:drafts k] body)
       :local-storage {:action :assoc
                       :key :comments-drafts
                       :assoc-key k
                       :assoc-value body}}
      (and body (not completed?))
      (assoc :dispatch [:citrus/auto-complete body]))))

(def handlers
  {:comment/reply
   (fn [state comment]
     {:state {:reply comment
              :reply-box? true}
      :dispatch [:message/focus]})

   :comment/like
   (fn [state comment [table fk]]
     {:state state
      :http {:params [:comment/like {:id (:id comment)}]
             :on-load [:comment/like-success comment [table fk]]}})

   :comment/like-success
   (fn [state comment [table fk] result]
     (let [id (:id comment)]
       {:state (-> state
                   (update :liked-comments (fn [v] (vec (conj v id))))
                   (update-in [table (get comment fk) :result (:id comment)]
                              (fn [comment]
                                (update comment :likes inc))))
        :local-storage {:action :conj
                        :key    :liked-comments
                        :value  id}}))

   :comment/unlike
   (fn [state comment [table fk]]
     {:state state
      :http {:params [:comment/unlike {:id (:id comment)}]
             :on-load [:comment/unlike-success comment [table fk]]}})

   :comment/unlike-success
   (fn [state comment [table fk] result]
     (let [id (:id comment)]
       {:state (-> state
                   (update :liked-comments (fn [v] (vec (remove #(= % id) v))))
                   (update-in [table (get comment fk) :result (:id comment)]
                              (fn [comment]
                                (if (> (:likes comment) 0)
                                  (update comment :likes dec)
                                  comment))))
        :local-storage {:action :disj
                        :key    :liked-comments
                        :value  id}}))


   :comment/new
   (fn [state [table fk] data]
     {:state (assoc state :loading? true)
      :http {:params [:comment/new (if (:reply state)
                                     (let [reply-to (:id (:reply state))]
                                       (assoc data
                                              :reply_to reply-to))
                                     data)]
             :on-load [:comment/new-ready [table fk]]
             :on-error :comment/new-error}})

   :comment/new-ready
   (fn [state [table fk] result]
     {:state (-> state
                 (assoc :reply nil
                        :loading? false
                        :reply-box? false)
                 (update-in [table (get result fk) :result]
                            (fn [comments]
                              (into (or (and (seq comments) comments) {}) [[(:id result) result]])))
                 (update-in [table (get result fk) :count-delta]
                            inc))
      :dispatch [:comment/scroll-into (:idx result)]})

   :comment/scroll-into
   (fn [state idx]
     (if-let [element (dommy/sel1 (str "#" "comment_" idx))]
       (do
         (dommy/add-class! element "highlight-area")
         (js/setTimeout #(dommy/remove-class! element "highlight-area")
                        1000)
         (let [comment (dommy/sel1 (str "#" "comment_" idx))]
           (scroll/into-view comment))
         {:state state})
       {:state state
        :timeout {:duration 50
                  :events [:comment/scroll-into idx]}}))

   :comment/new-error
   (fn [state error]
     {:state {:loading? false
              :reply-box? false}})

   :comment/update
   (fn [state data [table fk]]
     {:state {:loading? true}
      :http {:params [:comment/update data]
             :on-load [:comment/update-ready [table fk]]}})

   :comment/update-ready
   (fn [state [table fk] result]
     (let [path [table (get result fk) :result (:id result)]]
       {:state (-> state
                   (merge {:reply-box? false
                           :loading? false})
                   (update-in path merge result))}))

   :comment/receive-update
   (fn [state path result]
     {:state (-> state
                 (update-in path merge result))})

   :comment/delete
   (fn [state comment entity [table fk]]
     {:state state
      :http {:params [:comment/delete {:id (:id comment)}]
             :on-load [:citrus/delete-ready comment entity [table fk]]}})

   :citrus/delete-ready
   (fn [state comment entity [table fk] result]
     (util/set-href! (str config/website "/"
                          (if (= table :posts)
                            (:permalink entity)
                            (str "items/" (:id entity)))))
     {:state state})


   :comment/save-local
   save-local

   :comment/clear-item
   (fn [state k]
     (when-let [ref (get-in state [:refs k])]
       (oset! ref "value" ""))
     {:state (assoc-in state [:drafts k] nil)
      :local-storage {:action :dissoc
                      :key :comments-drafts
                      :assoc-key k}})

   :comment/quote
   (fn [state text]
     (let [{:keys [screen_name idx]} (:selection state)]
       (if-let [current-box-k (:current-box-k state)]
        (let [ref (get-in state [:refs current-box-k])
              v (oget ref "value")
              body (str v
                        (if-not (str/blank? v) "\n" "")
                        (util/format "[quote, @%s%s]\n" screen_name (if idx (str ", " idx) ""))
                        "____\n"
                        text
                        "\n____\n")]
          (oset! ref "value" body)
          {:state state
           :dispatch [[:comment/save-local current-box-k body]
                      [:comment/clear-selection]]})
        {:state state
         :dispatch [:comment/clear-selection]})))

   :comment/set-selection
   (fn [state selection]
     {:state {:selection (assoc selection :mode? true)}})

   :comment/clear-selection
   (fn [state]
     (when (get-in state [:selection :mode?])
       (util/remove-selection-ranges))
     {:state {:selection nil}}
     )

   :citrus/load-more-comments
   (fn [state {:keys [table fk id last] :as params}]
     {:state state
      :dispatch [:query/send (get-in state [:router :handler])
                 {:q {:comments {:fields [:*]}}
                  :args {:comments {fk id
                                    :cursor {:after (:flake_id last)}}}
                  :merge {:comments [:comment table id]}}
                 true]})
   })
