(ns share.components.book
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
            [clojure.string :as str]
            [share.kit.query :as query]
            [share.kit.mixins :as mixins]
            [share.kit.infinite-list :as inf]
            [share.components.right :as right]))

(rum/defc book-item
  [book]
  (let [link (str "/book/" (:id book))
        mobile? (util/mobile?)]
    [:a.column1 {:key (:id book)
                 :href link
                 :style {:padding (if mobile?
                                    "24px 0"
                                    "12px 24px 12px 0")
                         :color colors/primary}}
     (if (:cover book)
       (let [style (if mobile?
                     {:width "100%"
                      :background-color "#fefefe"}
                     {:width 230
                      :height 300
                      :background-color "#fefefe"})]
         [:div {:style style}
          [:img.hover-shadow {:src (:cover book)
                              :style (merge
                                      {:object-fit "contain"}
                                      style)}]]))]))

(rum/defc books-stream < rum/reactive
  [books end?]
  (let [current-path (citrus/react [:router :handler])
        loading? (citrus/react [:query :scroll-loading? current-path])]
    [:div.row.books {:style {:flex-wrap "wrap"}}
     (inf/infinite-list (map (fn [book]
                               (book-item book)) books)
                        {:on-load
                         (if end?
                           identity
                           (fn []
                             (citrus/dispatch! :citrus/load-more-books
                                               {:last (last books)})))})
     (when loading?
       [:div.center {:style {:margin "24px 0"}}
        [:div.spinner]])]))

(rum/defc books < rum/reactive
  (mixins/query :books)
  [params]
  [:div#books.auto-padding {:style {:padding-bottom 64}}
   [:h1 {:style {:margin "24px 0"}}
    (t :books)]

   [:a.row1 {:style {:margin "24px 0"
                :color colors/primary
                :font-size 17}
             :href "/new-book"}
    (ui/icon {:type :add})
    (t :add-a-book)]
   (query/query
     (let [{:keys [result end?]} (citrus/react [:books :latest])
           mobile? (util/mobile?)]
       (when (seq result)
         (books-stream result end?))))])

(rum/defc book < rum/reactive
  (mixins/query :book)
  [params]
  (let [id (:book-id params)
        posts-path [:posts :by-book-id id :latest-reply]
        {:keys [id title description cover tags created_at screen_name followers] :as book} (citrus/react [:book :by-id id])
        posts (citrus/react posts-path)
        current-user (citrus/react [:user :current])
        self? (= screen_name (:screen_name current-user))
        height (citrus/react [:layout :current :height])
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))]
    (query/query
      (if book
        [:div#book.column
         [:div.splash.row1
          {:style {:padding (if mobile?
                              "48px 12px"
                              48)
                   :box-shadow "0 3px 8px #ddd"
                   :align-items "center"
                   :width "100%"
                   :position "relative"}}
          (when-not mobile?
            [:div.cover
             [:img.box {:src cover
                        :style {:height 300
                                :min-width 230
                                :width 230
                                :margin-right 12
                                :object-fit "contain"}}]])
          (let [authors [:div.row1.book-authors {:style {:align-items "center"}}
                         (widgets/transform-content (:authors book) {:style {:margin 0}})]
                stared? (contains? (set (map :object_id (:stared_books current-user))) (:id book))]
            [:div.column1
             (when mobile?
               [:div.row1 {:style {:align-items "center"}}
                [:img.box {:src cover
                           :style {:max-height 100
                                   :max-width 100
                                   :object-fit "contain"
                                   :margin-right 12}}]
                [:div.column1
                 [:h2 {:style {:margin 0}} title]
                 authors]])

            (when-not mobile?
              [:h1 {:style {:margin 0}} title])

             (when-not mobile?
               authors)

             [:div {:style {:margin-top 6}}
             (widgets/more-content description (if mobile? 80 360))]

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

             ;; (widgets/subscribe (str "/book/" id "/latest.rss"))

             (when self?
               [:a {:href (str "/book/" id "/edit")
                    :style {:margin-left 12}}
                (ui/icon {:type :edit
                          :width 18})])]

             [:div.row {:style {:margin-top 18
                                :align-items "center"}}

              (when (seq followers)
                (widgets/followers followers (:stars book)))

              (ui/button {:class (if stared? "btn" "btn-primary")
                          :style {:margin-left 12
                                  :width 106}
                          :on-click (fn []
                                      (citrus/dispatch! (if stared? :user/unstar :user/star)
                                                        {:object_type "book"
                                                         :object_id (:id book)}))}
                (if stared? (t :leave) (t :join)))]])]

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
             (right/books)])]]
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
   :cover        {:label (t :cover)
                  :type :image}})

(rum/defc new-book
  [params]
  [:div.auto-padding.column {:style {:margin-top 24
                                     :padding-bottom 48}}
   (form/render {:title (t :add-a-book)
                 :fields (book-fields)
                 :on-submit (fn [form-data]
                              (citrus/dispatch! :resource/new
                                                (assoc @form-data
                                                       :object_type "book")
                                                form-data))
                 :loading? [:resource :loading?]
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
         (form/render {:title (t :edit)
                       :fields (book-edit-fields (atom book))
                       :on-submit (fn [form-data]
                                    (citrus/dispatch! :resource/update
                                                      (assoc @form-data
                                                             :object_type "book"
                                                             :object_id id)
                                                      form-data))
                       :loading? [:resource :loading?]
                       :style {:max-width 512}})
         [:div.auto-padding
          [:h1 "404 NOT FOUND"]])))])
