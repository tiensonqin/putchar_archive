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
                :color (colors/primary-text)
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
                                :color (colors/primary-text)}}
             (if (:cover book)
               [:img {:src (:cover book)
                      :style {:max-width 200
                              :max-height 200}}])
             [:div {:style {:margin-top 8
                            :width 192
                            :text-overflow "ellipsis"
                            :white-space "nowrap"
                            :overflow "hidden"}}
              (:name book)]]))]))])

(rum/defc book < rum/reactive
  (mixins/query :book)
  [params]
  (let [id (:book-id params)
        posts-path [:posts :by-book-id id :latest-reply]
        {:keys [id name description cover tags created_at screen_name] :as book} (citrus/react [:book :by-id id])
        posts (citrus/react posts-path)
        current-user (citrus/react [:user :current])
        self? (= screen_name (:screen_name current-user))]
    (query/query
      (if book
       [:div#book.auto-padding.center-area {:style {:margin-top 24}}
        [:div.row1
         [:img {:src cover
                :style {:max-height 100
                        :max-width 100
                        :margin-right 24}}]
         [:div.column1
          [:h1 {:style {:margin 0}} name]
          (when-let [authors (:authors book)]
            [:div.row1 {:style {:align-items "center"
                                :margin-top 12}}
             (widgets/transform-content authors nil)])
          [:div.space-between {:style {:flex-wrap "wrap"}}
           [:div.row1 {:style {:align-items "center"
                               :color (colors/shadow)}}
            "Posted by: "
            [:a {:href (str "/@" screen_name)
                 :style {:margin-left 4
                         :color (colors/shadow)}}
             screen_name]
            ", at: "
            [:i {:style {:margin-left 4}}
             (util/date-format created_at)]]
           (when self?
             [:a {:href (str "/book/" id "/edit")
                  :style {:margin-left 12
                          :color (colors/shadow)}}
              (t :edit)])]]]

        ;; (when description
        ;;   (widgets/transform-content description {}))


        (post/post-list posts
                        {:book_id id
                         :merge-path posts-path})]
       [:div.auto-padding
        [:h1 "404 NOT FOUND"]]))))

(defn book-fields
  []
  {:name         {:label (str (t :name) ": *")
                  :validators? [util/non-blank?]}
   :authors      {:label (str (t :authors) ": *")
                  :placeholder (t :authors-placeholder)
                  :validators? [util/non-blank?]
                  :type :textarea
                  :style {:height 80
                          :resize "none"}}
   ;; :tags         {:label (t :tags)}
   ;; :description  {:label (t :description)
   ;;                :type :textarea
   ;;                :style {:height 96
   ;;                        :resize "none"}}
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

(rum/defc book-edit < rum/reactive
  (mixins/query :book-edit)
  [params]
  (query/query
    )
  )
