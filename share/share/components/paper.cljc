(ns share.components.paper
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.widgets :as widgets]
            [share.components.post :as post]
            [share.config :as config]
            [share.util :as util]
            [share.dicts :refer [t]]
            [share.kit.colors :as colors]
            [clojure.string :as s]
            [share.kit.query :as query]
            [share.kit.mixins :as mixins]
            [share.kit.infinite-list :as inf]
            [share.components.right :as right]))

(rum/defc paper-item
  [paper]
  (let [link (str "/paper/" (:id paper))]
    [:div.column1 {:style {:margin-bottom 24}
                   :key (:id paper)}

     [:div.row1 {:style {:flex-wrap "wrap"
                         :align-items "center"}}
      [:a {:key (:id paper)
           :href link
           :style {:color colors/primary
                   :font-size 18
                   :margin-right 12}}
       (:title paper)]

      (for [tag (:tags paper)]
        [:a {:id tag
             :style {:margin "0 12px 0 0"
                     :color colors/primary}
             :key tag}
         (str "#" tag)])]]))

(rum/defc papers-stream < rum/reactive
  [papers end?]
  (let [current-path (citrus/react [:router :handler])
        loading? (citrus/react [:query :scroll-loading? current-path])]
    [:div.column
     (inf/infinite-list (map (fn [paper]
                               (paper-item paper)) papers)
                        {:on-load
                         (if end?
                           identity
                           (fn []
                             (citrus/dispatch! :citrus/load-more-papers
                                               {:last (last papers)})))})
     (when loading?
       [:div.center {:style {:margin "24px 0"}}
        [:div.spinner]])]))

(rum/defc papers < rum/reactive
  (mixins/query :papers)
  [params]
  [:div#papers.auto-padding.column.center-area
   [:h1 {:style {:margin "24px 0"}}
    (t :papers)]

   [:a.row1 {:style {:color colors/primary
                     :font-size 17}
             :href "/new-paper"}
    (ui/icon {:type :add})
    (t :add-a-paper)]

   [:div.divider]

   (query/query
     (let [{:keys [result end?]} (citrus/react [:papers :latest])
           mobile? (util/mobile?)]
       (when (seq result)
         (papers-stream result end?))))])

(rum/defc paper < rum/reactive
  (mixins/query :paper)
  [params]
  (let [id (:paper-id params)
        posts-path [:posts :by-paper-id id :latest-reply]
        {:keys [id title description tags created_at screen_name followers] :as paper} (citrus/react [:paper :by-id id])
        posts (citrus/react posts-path)
        current-user (citrus/react [:user :current])
        self? (= screen_name (:screen_name current-user))
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))
        stared? (contains? (set (map :object_id (:stared_papers current-user))) (:id paper))]
    (query/query
      (if paper
        [:div#paper.column
         [:div.row1
          [:div.row1.splash
           {:style {:padding (if mobile? "12px 12px 24px 12px" 48)
                    :box-shadow "0 3px 8px #ddd"
                    :align-items "center"
                    :width "100%"
                    :position "relative"}}
           [:div.column1
            [:h1 {:style {:margin 0}} title]
            (when-let [authors (:authors paper)]
              [:div.row1.paper-authors {:style {:align-items "center"
                                                :margin-top 12
                                                }}
               (widgets/transform-content authors {:style {:margin 0}})])
            [:div {:style {:margin-top -16}}
             (widgets/more-content description 360)]

            [:div.row1 {:style {:align-items "center"
                                :flex-wrap "wrap"}}
             (t :posted-by)
             [:a {:href (str "/@" screen_name)
                  :style {:margin-left 4
                          :color colors/primary}}
              screen_name]
             ", "
             [:i {:style {:margin-left 4
                          :margin-right 12}}
              (util/date-format created_at)]
             ;; (widgets/subscribe (str "/paper/" id "/latest.rss"))

             (when self?
               [:a {:href (str "/paper/" id "/edit")
                    :style {:margin-left 12}}
                (ui/icon {:type :edit
                          :width 18})])]

            [:div.row {:style {:margin-top 18
                               :align-items "center"}}

             (when (seq followers)
               (widgets/followers followers (:stars paper)))

             (ui/button {:class (if stared? "btn" "btn-primary")
                         :style {:margin-left 12
                                 :width 106}
                         :on-click (fn []
                                     (citrus/dispatch! (if stared? :user/unstar :user/star)
                                                       {:object_type "paper"
                                                        :object_id (:id paper)}))}
               (if stared? (t :leave) (t :join)))]]]]

         [:div.row {:style {:margin-top 24}}
          (post/post-list posts
                          {:book_id id
                           :merge-path posts-path})
          (when-not mobile?
            [:div#right {:key "right"
                         :class "column1"
                         :style {:margin-left 12
                                 :margin-right 3
                                 :width 243}}
             (right/papers)])]]
        [:div.auto-padding
         [:h1 "404 NOT FOUND"]])))
  )

(defn paper-fields
  []
  {:title {:label (str (t :title) ": *")
           :validators? [util/non-blank?]}
   :authors      {:label (str (t :authors) ": *")
                  :placeholder (t :authors-placeholder)
                  :validators? [util/non-blank?]
                  :type :textarea
                  :style {:height 80
                          :resize "none"}}
   :tags         {:label (t :tags)}
   :description  {:label (t :description)
                  :type :textarea
                  :style {:height 96
                          :resize "none"}}})

(rum/defc new-paper
  [params]
  [:div.column.auto-padding {:style {:margin-top 24}}
   (form/render {:title (t :add-a-paper)
                 :fields (paper-fields)
                 :on-submit (fn [form-data]
                              (citrus/dispatch! :resource/new
                                                (assoc @form-data
                                                       :object_type "paper")
                                                form-data))
                 :loading? [:resource :loading?]
                 :style {:max-width 512}})])

(defn paper-edit-fields
  [form-data]
  {:title {:label (str (t :title) ": *")
           :validators [util/non-blank?]
           :value (:title @form-data)}
   :authors      {:label (str (t :authors) ": *")
                  :placeholder (t :authors-placeholder)
                  :value (:authors @form-data)
                  :validators [util/non-blank?]
                  :type :textarea
                  :style {:height 80
                          :resize "none"}}
   :tags         {:label (t :tags)
                  :value (:tags @form-data)}
   :description  {:label (t :description)
                  :type :textarea
                  :value (:description @form-data)
                  :style {:height 96
                          :resize "none"}}})

(rum/defc paper-edit < rum/reactive
  (mixins/query :paper-edit)
  [params]
  [:div.auto-padding.column
   (query/query
     (let [id (:paper-id params)
           {:keys [id title description tags created_at screen_name] :as p} (citrus/react [:paper :by-id id])
           paper (cond-> p
                   tags
                   (assoc :tags (s/join ", " tags)))]
       (if paper
         (form/render {:title (t :edit)
                       :fields (paper-edit-fields (atom paper))
                       :on-submit (fn [form-data]
                                    (citrus/dispatch! :resource/update
                                                      (assoc @form-data
                                                             :object_type "paper"
                                                             :object_id id)
                                                      form-data))
                       :loading? [:resource :loading?]
                       :style {:max-width 512}})
         [:div.auto-padding
          [:h1 "404 NOT FOUND"]])))])
