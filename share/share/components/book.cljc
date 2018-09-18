(ns share.components.book
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
            [clojure.string :as str]
            [share.kit.query :as query]
            [share.kit.mixins :as mixins]))

(rum/defc books < rum/reactive
  (mixins/query :books)
  [params]
  [:div#books.auto-padding {:style {:padding-bottom 64}}
   [:h1 {:style {:margin "24px 0"}}
    "Books"]

   [:a.row1 {:style {:margin "24px 0"
                :color colors/primary
                :font-size 17}
             :href "/new-book"}
    (ui/icon {:type :add})
    "Add new book"]

   (query/query
     (let [books (citrus/react [:books :hot :result])]
       [:div.row {:style {:flex-wrap "wrap"}}
        (for [book books]
          (let [link (str "/book/" (:id book))]
            [:a.column {:key (:id book)
                        :href link
                        :style {:padding "12px 24px 12px 0"
                                :color colors/primary}}
             (if (:cover book)
               [:img {:src (:cover book)
                      :style {:max-width 200
                              :max-height 200
                              :object-fit "contain"}}])
             [:div {:style {:margin-top 8
                            :width 192
                            :text-overflow "ellipsis"
                            :white-space "nowrap"
                            :overflow "hidden"}}
              (:title book)]]))]))])

(rum/defc book < rum/reactive
  (mixins/query :book)
  [params]
  (let [id (:book-id params)
        posts-path [:posts :by-book-id id :latest-reply]
        {:keys [id title description cover tags created_at screen_name] :as book} (citrus/react [:book :by-id id])
        posts (citrus/react posts-path)
        current-user (citrus/react [:user :current])
        self? (= screen_name (:screen_name current-user))]
    (query/query
      (if book
        [:div#book.column
         [:div.row
          [:div.row1.splash
           {:style {:min-height "60vh"
                    :padding 48
                    :box-shadow "0 3px 8px #ddd"
                    :background "#efefef"
                    :background-image "radial-gradient(at 1% 100%, #ADC0CF, #FFF)"
                    :align-items "center"
                    :width "100%"}}
           [:div {:style {:position "relative"}}
            [:a.cover {:href (:link book)
                       :target "_blank"}
             [:img.box {:src cover
                        :style {:max-height 300
                                :max-width 300
                                :margin-right 12
                                :object-fit "contain"}}]]
            (let [stared? (contains? (set (map :object_id (:stared_books current-user))) (:id book))]
              [:a {:style {:position "absolute"
                          :top 0}
                   :on-click (fn []
                               (citrus/dispatch! (if stared? :user/unstar :user/star)
                                                 {:object_type "book"
                                                  :object_id (:id book)}))}
               (ui/icon (if stared?
                          {:type :star
                           :color "#D95653"}
                          {:type :star-border}))])]
           [:div.column1
            [:h1 {:style {:margin 0}} title]
            (when-let [authors (:authors book)]
              [:div.row1.book-authors {:style {:align-items "center"
                                               :margin-top 12
                                               }}
               (widgets/transform-content authors {:style {:margin 0}})])
            (when-let [link (:link book)]
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
              [:a {:href (str "/book/" id "/edit")
                   :style {:margin-top 6
                           :color colors/primary}}
               (t :edit)])]]]

         ;; (when description
         ;;   (widgets/transform-content description {}))


         [:div.center-area.auto-padding
          (post/post-list posts
                          {:book_id id
                           :merge-path posts-path})]]
       [:div.auto-padding
        [:h1 "404 NOT FOUND"]]))))

(defn book-fields
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
   :cover        {:label (t :cover)
                  :type :image}})

(rum/defc new-book
  [params]
  [:div.auto-padding.column {:style {:margin-top 24
                                     :padding-bottom 48}}
   (form/render {:title "Add a book"
                 :fields (book-fields)
                 :on-submit (fn [form-data]
                              (citrus/dispatch! :resource/new
                                                (assoc @form-data
                                                       :object_type "book")
                                                form-data))
                 :loading? [:book :loading?]
                 :style {:max-width 512}})])

(defn book-edit-fields
  [form-data]
  {:cover        {:label (t :cover)
                  :type :image
                  :before (if-let [cover (:cover @form-data)]
                            [:img {:src cover
                                   :style {:max-width 100
                                           :max-height 100}}])}
   :title {:label (str (t :title) ": *")
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
   :link         {:label (t :link)
                  :validators [util/link?]
                  :value (:link @form-data)}
   :description  {:label (t :description)
                  :type :textarea
                  :value (:description @form-data)
                  :style {:height 96
                          :resize "none"}}})

(rum/defc book-edit < rum/reactive
  (mixins/query :book-edit)
  [params]
  [:div.auto-padding.column
   (query/query
     (let [id (:book-id params)
           {:keys [id title description cover tags created_at screen_name] :as book} (citrus/react [:book :by-id id])
           book (if tags
                  (assoc book :tags (str/join ", " tags))
                  book)]
       (if book
         (form/render {:title "Edit"
                       :fields (book-edit-fields (atom book))
                       :on-submit (fn [form-data]
                                    (citrus/dispatch! :resource/update
                                                      (assoc @form-data
                                                             :object_type "book"
                                                             :object_id id)
                                                      form-data))
                       :loading? [:book :loading?]
                       :style {:max-width 512}})
         [:div.auto-padding
          [:h1 "404 NOT FOUND"]])))])
