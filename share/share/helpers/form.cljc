(ns share.helpers.form
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            #?(:cljs [goog.object :as gobj])
            #?(:cljs [goog.dom :as gdom])
            [clojure.string :as str]
            [appkit.citrus :as citrus]
            [share.helpers.image :as image]
            [share.dicts :refer [t]]
            [share.util :as util])
  #?(:cljs (:import goog.format.EmailAddress)))

(defn required? [v]
  (not (str/blank? v)))

(defn matches? [regex value]
  (boolean (re-matches regex value)))

(defn email? [v]
  (and v
       #?(:clj (matches? #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" v)
          :cljs (.isValid (EmailAddress. v)))))

(defn code? [v]
  (re-find #"^[0-9]{6,6}$" v))

(defn ev
  [e]
  #?(:cljs (gobj/getValueByKeys e "target" "value")
     :clj nil))

(rum/defc input < rum/static
  [form-data name {:keys [id textarea? label value icon warning disabled placeholder type on-change validators style class auto-focus]
                   :or {disabled false
                        auto-focus false
                        type "text"}
                   :as attrs}]
  (let [validated? (get-in @form-data [:validators name])
        input-tag (if textarea? :textarea :input)]
    [:div {:class "field"
           :id (clojure.core/name id)}
     ;; label
     (if label [:label {:class "label"} label])

     ;; input
     [input-tag (cond-> {:auto-focus auto-focus
                         :tab-index 0
                         :class
                         (str (cond
                                class
                                class
                                disabled
                                "ant-input ant-input-disabled"
                                :else
                                "ant-input")
                              (case validated?
                                true
                                " is-success"

                                false
                                " is-danger"

                                ""))
                         :placeholder placeholder
                         :disabled disabled
                         :type (clojure.core/name type)
                         :on-change (fn [e]
                                      (swap! form-data (fn [v]
                                                         (assoc-in v [:validators name] nil)))
                                      (on-change e))
                         :on-blur (fn [e]
                                    (if validators
                                      (let [v (ev e)]
                                        (swap! form-data (fn [v]
                                                           (assoc-in v [:validators name] true)))

                                        (doseq [validator validators]
                                          (if-not (validator v)
                                            (swap! form-data (fn [v]
                                                               (assoc-in v [:validators name] false))))))))}
                  value (assoc :default-value value)
                  style (assoc :style style))]

     ;; help message
     (if (and (false? validated?) warning)
       [:p {:class "help is-danger"} warning])]))

(rum/defc radio
  [form-data name {:keys [side options on-change]
                   :as opts}]
  (let [current-value (get @form-data name)]
    [:div {:class "field"}
     [:div.row
      (if side
        [:h3 {:style {:margin-right 12}}
         (str side ":")])

      (for [{:keys [value label default]} options]
        (let [checked (if current-value
                        (= current-value value)
                        default)]
          (ui/button {:key (str name "-" value)
                      :on-click (fn []
                                  (swap! form-data assoc name value))
                      :class (if checked "btn-primary btn-sm" "btn btn-sm")
                      :style {:margin-right 24}}
            label)))]]))

(rum/defc checkbox
  [form-data name {:keys [checked? side warning warning-show? disabled on-change]
                   :or {disabled false}}]
  [:div.field
   [:div.row1 {:style {:align-items "center"}}
    [:input
     {:id name
      :tab-index 0
      :name (clojure.core/name name)
      :type "checkbox"
      :on-change (fn [e]
                   (swap! form-data assoc name (.-checked (.-target e)))
                   (if on-change
                     (on-change e)))
      }]

    (if side
      [:label.fa {:for name} side])]

   (if (and warning warning-show?)
     [:div {:class "help is-danger"}
      warning])])

(rum/defcs image <
  (rum/local false ::uploading?)
  (rum/local false ::undo?)
  (rum/local nil ::local-form)
  [state form name {:keys [text png? filename before before-id after on-uploaded show-undo? initial-value icon style button-class show-result? result-cb loading]
                    :or {png? false
                         show-undo? true
                         show-result? true
                         style {:margin-bottom 12}
                         button-class "btn"
                         result-cb nil}
                    :as opts}]
  (let [form (if form form (get state ::local-form))
        uploading? (get state ::uploading?)
        undo? (get state ::undo?)
        id (str name)
        on-click (fn []
                   #?(:cljs
                      (.click (gdom/getElement id))))
        src (get @form name)]
    [:div {:style style}
     (cond
       (and @uploading? loading)
       loading

       @uploading?
       (ui/donut)

       (and src after)
       (after src)

       (and initial-value after)
       [:a {:on-click on-click}
        (after initial-value)]

       (and src show-result? result-cb)
       (result-cb form src)

       (and src show-result?)
       [:div.row
        [:img {:src src
               :style {:max-width 300
                       :max-height 300
                       :object-fit "contain"}}]
        (if (and (false? @undo?) show-undo?)
          [:a {:style {:margin-left 6}
               :on-click (fn []
                           (swap! form assoc name nil))}
           (ui/icon {:type "close"})])]

       before
       [:a (cond-> {:on-click on-click}
             before-id
             (assoc :id before-id))
        before]


       text
       (ui/button {:on-click on-click
                   :class button-class}
         text)

       icon
       [:a {:on-click on-click}
        icon])

     [:input
      {:id id
       :type "file"
       :on-change (fn [e]
                    (image/upload
                     (.-files (.-target e))
                     (fn [file file-form-data]
                       (reset! uploading? true)
                       (if filename
                         (.append file-form-data "name" filename))
                       (if png?
                         (.append file-form-data "png" true))
                       (citrus/dispatch!
                        :image/upload
                        file-form-data
                        (fn [url]
                          (reset! uploading? false)
                          (swap! form assoc name url)
                          (if on-uploaded
                            (on-uploaded form name url)))))))
       :hidden true}]

     (if (:image-upload-error @form)
       [:p {:class "help is-danger"}
        (:image-upload-error @form)])]))

(rum/defc submit < rum/reactive
  [on-submit {:keys [submit-text
                     cancel-button?
                     confirm-attrs
                     on-cancel
                     loading?
                     cancel-icon?
                     submit-style]
              :or {cancel-button? true
                   submit-text (t :submit)}
              :as opts}]
  [:div.field {:class "row1"
               :style (merge {:align-items "center"}
                             submit-style)}
   (let [loading? (if loading? (citrus/react loading?) false)]
     (ui/button (merge {:tab-index 0
                        :on-click on-submit
                        :class "btn btn-primary"
                        :on-key-down (fn [e]
                                       (when (= 13 (.-keyCode e))
                                         ;; enter key
                                         (on-submit)))}
                       confirm-attrs)
       (if loading?
         (ui/donut-white)
         submit-text)))

   (cond
     cancel-icon?
     [:a {:style {:margin-left 12}
          :on-click (fn []
                      (if on-cancel
                        (on-cancel)
                        (citrus/dispatch! :router/back)))}
      (ui/icon {:type :close})]
     :else
     (if cancel-button?
           (ui/button {
                       :style {:margin-left 12}
                       :on-click (fn []
                                   (if on-cancel
                                     (on-cancel)
                                     (citrus/dispatch! :router/back)))}
             (t :cancel))))
   ])

(rum/defcs render < (rum/local nil ::form-data)
  {:after-render (fn [state]
                  (util/scroll-to-element)
                  state)}
  [state {:keys [init-state
                 title
                 fields
                 on-submit
                 submit-style
                 submit-text
                 style
                 cancel-button?
                 confirm-attrs
                 header
                 footer
                 loading?]
          :or {submit-text (t :submit)
               cancel-button? true
               style {:padding 12
                      :broder-radius "4px"
                      :min-width 360}}
          :as form}]
  (let [form-data (get state ::form-data)]
    (when (and init-state (map? init-state) (nil? @form-data))
      (reset! form-data init-state))
    [:div.column.form {:style style}
     ;; title
     (if title [:h1 {:class "title"
                     :style {:margin-bottom 24}} title])

     (if header
       (header form-data))

     (when-let [warning (:warning-message @form-data)]
       [:div
        [:p {:class "help is-danger"} warning]])

     ;; fields
     [:div {:style {:margin-bottom 12}}
      (for [[name attrs] fields]
        (let [f (case (:type attrs)
                  :checkbox checkbox
                  :radio radio
                  :image image
                  input)
              attrs (if (= (:type attrs) :textarea)
                      (assoc attrs :textarea? true)
                      attrs)

              attrs (assoc attrs :id name)

              attrs (assoc attrs :value (or (get @form-data name)
                                            (:value attrs)))
              attrs (if (:disabled attrs)
                      attrs
                      (assoc attrs :on-change (fn [e]
                                                (let [v (if (= :checkbox (:type attrs))
                                                          (.-checked (.-target e))
                                                          (ev e))]
                                                  (swap! form-data assoc name v)
                                                  (when-let [on-change (:on-change attrs)]
                                                    (on-change form-data v))))))

              attrs (if (and
                         (= :checkbox (:type attrs))
                         (:warning attrs)
                         (false? (get @form-data name)))
                      (assoc attrs :warning-show? true)
                      attrs)]
          (rum/with-key (cond
                          ;; TODO: not works
                          (= (:type attrs) :select)
                          (let [options (mapv
                                         (fn [{:keys [label value]}]
                                           (ui/option {:key value
                                                       :value value}
                                                      (str label)))
                                         (:options attrs))]
                            (apply ui/select attrs options))


                          (contains? #{input checkbox radio image} f)
                          (f form-data name attrs)

                          :else
                          (f attrs)) name)))]

     (if footer (footer form-data))
     ;; submit
     (submit
      (fn []
        ;; validate
        (doseq [[name {:keys [type validators required?]}] fields]
          (if (and required? (nil? (get @form-data name)))
            (swap! form-data (fn [v]
                               (assoc-in v [:validators name] false))))
          (when (and
                 (or (nil? type)
                     (contains? #{:input :textarea "input" "textarea"} type))
                 (get @form-data name))
            (when validators
              (doseq [validator validators]
                (if (validator (get @form-data name))
                  (swap! form-data (fn [v]
                                     (assoc-in v [:validators name] true)))
                  (swap! form-data (fn [v]
                                     (assoc-in v [:validators name] false))))))))
        (when (every? #(or (true? %) (nil? %)) (vals (:validators @form-data)))
          (swap! form-data dissoc :validators)
          (when @form-data
            (on-submit form-data))))
      {:submit-text submit-text
       :submit-style submit-style
       :cancel-button? cancel-button?
       :confirm-attrs confirm-attrs
       :loading? loading?})]
    ))
