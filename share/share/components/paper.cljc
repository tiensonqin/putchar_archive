(ns share.components.paper
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.widgets :as widgets]
            [share.components.resource :as resource]
            [share.components.post :as post]
            [share.config :as config]
            [share.util :as util]
            [share.dicts :refer [t]]
            [share.kit.colors :as colors]
            [clojure.string :as s]
            [share.kit.query :as query]
            [share.kit.mixins :as mixins]))

(rum/defc papers < rum/reactive
  (mixins/query :papers)
  [params]
  [:div#papers.auto-padding.column.center-area {:style {:padding-bottom 64}}
   [:h1 {:style {:margin "24px 0"}}
    "Papers"]

   [:a.row1 {:style {:margin "24px 0"
                     :color colors/primary
                     :font-size 17}
             :href "/new-paper"}
    (ui/icon {:type :add})
    "Add new paper"]

   [:div.divider]

   (query/query
     (let [papers (citrus/react [:papers :hot :result])]
       [:div.column
        (for [paper papers]
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
                [:a.tag {:id tag
                         :style {:margin "0 12px 0 0"}
                         :key tag}
                 tag])]
             [:div {:style {:margin-top 6}}
              (widgets/transform-content (:authors paper) nil)]]))]))])

(rum/defc paper < rum/reactive
  (mixins/query :paper)
  [params]
  (let [id (:paper-id params)
        posts-path [:posts :by-paper-id id :latest-reply]
        {:keys [id title description tags created_at screen_name] :as paper} (citrus/react [:paper :by-id id])
        posts (citrus/react posts-path)
        current-user (citrus/react [:user :current])
        self? (= screen_name (:screen_name current-user))]
    (query/query
      (if paper
        [:div#paper.column
         [:div.row
          [:div.row1.splash
           {:style {:min-height "40vh"
                    :padding 48
                    :box-shadow "0 3px 8px #ddd"
                    :background "#efefef"
                    :background-image "radial-gradient(at 1% 100%, #ADC0CF, #FFF)"
                    :align-items "center"
                    :width "100%"
                    :position "relative"}}
           (let [stared? (contains? (set (map :object_id (:stared_papers current-user))) (:id paper))]
             [:a {:style {:position "absolute"
                          :left 12
                          :top 53}
                  :on-click (fn []
                              (citrus/dispatch! (if stared? :user/unstar :user/star)
                                                {:object_type "paper"
                                                 :object_id (:id paper)}))}
              (ui/icon (if stared?
                         {:type :star
                          :color "#D95653"}
                         {:type :star-border}))])
           [:div.column1
            [:h1 {:style {:margin 0}} title]
            (when-let [authors (:authors paper)]
              [:div.row1.paper-authors {:style {:align-items "center"
                                                :margin-top 12
                                                }}
               (widgets/transform-content authors {:style {:margin 0}})])
            (when-let [link (:link paper)]
              [:span {:style {:margin-top 24}}
               "Website: "
               [:a {:href link
                    :target "_blank"
                    :style {:color colors/primary}}
                link]])
            [:div.row1 {:style {:align-items "center"
                                :flex-wrap "wrap"
                                :margin-top 6}}
             "Posted by: "
             [:a {:href (str "/@" screen_name)
                  :style {:margin-left 4
                          :color colors/primary}}
              screen_name]
             ", "
             [:i {:style {:margin-left 4}}
              (util/date-format created_at)]]

            (when self?
              [:a {:href (str "/paper/" id "/edit")
                   :style {:margin-top 6
                           :color colors/primary}}
               (t :edit)])]]]

         ;; (when description
         ;;   (widgets/transform-content description {}))


         [:div.center-area.auto-padding
          (post/post-list posts
                          {:paper_id id
                           :merge-path posts-path})]]
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
   :link         {:label (str (t :link) ": *")
                  :validators [util/link? util/non-blank?]}
   :tags         {:label (t :tags)}
   :description  {:label (t :description)
                  :type :textarea
                  :style {:height 96
                          :resize "none"}}})

(rum/defc new-paper
  [params]
  [:div.column.auto-padding {:style {:margin-top 24}}
   (form/render {:title "Add a paper"
                 :fields (paper-fields)
                 :on-submit (fn [form-data]
                              (citrus/dispatch! :resource/new
                                                (assoc @form-data
                                                       :object_type "paper")
                                                form-data))
                 :loading? [:paper :loading?]
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
   :link         {:label (t :link)
                  :validators [util/link?]
                  :value (:link @form-data)}
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
         (form/render {:title "Edit"
                       :fields (paper-edit-fields (atom paper))
                       :on-submit (fn [form-data]
                                    (citrus/dispatch! :resource/update
                                                      (assoc @form-data
                                                             :object_type "paper"
                                                             :object_id id)
                                                      form-data))
                       :loading? [:paper :loading?]
                       :style {:max-width 512}})
         [:div.auto-padding
          [:h1 "404 NOT FOUND"]])))])
