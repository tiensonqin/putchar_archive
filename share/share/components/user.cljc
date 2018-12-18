(ns share.components.user
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.kit.query :as query]
            [share.kit.mixins :as mixins]
            [share.components.post :as post]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.helpers.image :as image]
            [share.util :as util]
            [bidi.bidi :as bidi]
            [clojure.string :as str]
            [share.dicts :refer [t] :as dicts]
            [share.components.widgets :as widgets]
            [share.components.comment :as comment]
            [share.config :as config]
            #?(:cljs [goog.dom :as gdom])
            [appkit.macros :refer [oset!]]
            [share.kit.colors :as colors]))

(defn signup-fields
  [user username-taken? email-taken? email]
  (let [screen-name-warning (if username-taken?
                              (t :screen-name-taken)
                              (t :username-length-warning))
        email-warning (if email-taken?
                        (t :email-exists)
                        (t :invalid-email))]
    {:screen_name  (cond->
                       {:label (t :username)
                        :required? true
                        :placeholder (t :pick-a-username)
                        :warning screen-name-warning
                        :validators [(fn [x]
                                       (and (util/username? x)
                                            (not username-taken?)))]
                        :on-change (fn [form-data v]
                                     (when-not (str/blank? v)
                                       (citrus/dispatch! :citrus/default-update
                                                         [:user :username-taken?] nil)))}
                     (:screen_name user)
                     (assoc :value (:screen_name user)))
     :email        (let [email (or email (:email user) "")]
                     (cond->
                         {:label (t :email)
                          :required? true
                          :placeholder (t :required)
                          :warning email-warning
                          :validators [(fn [x]
                                         (and (not email-taken?)
                                              (form/email? x)))]
                          :on-change (fn [form-data v]
                                       (when-not (str/blank? v)
                                         (citrus/dispatch! :citrus/default-update
                                                           [:user :email-taken?] nil)))
                          :value email}))
     :name         {:label (t :full-name)
                    :placeholder (str/capitalize (t :optional))
                    :warning (t :full-name-warning)
                    :validators [util/optional-non-blank?]
                    :disabled (if (:name user) true)}}))

(rum/defc signup < rum/reactive
  [{:keys [email] :as params}]
  (let [temp-user (citrus/react [:user :temp])]
    [:div.signup.row
     (let [github-avatar (:avatar_url temp-user)
           user (select-keys temp-user [:id :name :email])
           user (-> user
                    (assoc :bio (or (:bio temp-user) (:description temp-user)))
                    (assoc :screen_name (or (:screen_name temp-user) (:login temp-user)))
                    (util/map-remove-nil?))
           username-taken? (citrus/react [:user :username-taken?])
           email-taken? (citrus/react [:user :email-taken?])]
       [:div {:style {:margin "0 auto"}}
        (if (seq user)
          (form/render
            {:init-state user
             :loading? [:user :loading?]
             :title (str (t :welcome) ", " (or (:name user)
                                               (:screen_name user)))
             :fields (signup-fields user username-taken? email-taken? email)
             :submit-text (t :signup)
             :submit-style {:margin-top 12}
             :on-submit (fn [form-data]
                          (let [data (merge
                                      @form-data
                                      {:avatar github-avatar}
                                      {:github_id (str (:id user))
                                       :github_handle (:login temp-user)})]
                            (citrus/dispatch! :user/new data form-data)))})
          (form/render
            {:title (t :welcome)
             :init-state {:email email}
             :loading? [:user :loading?]
             :fields (signup-fields user username-taken? email-taken? email)
             :submit-text (t :signup)
             :on-submit (fn [form-data]
                          (citrus/dispatch! :user/new @form-data form-data))}))])]))

(defn profile-fields
  [form-data]
  {:name         {:label (t :full-name)
                  :value (:name @form-data)}
   :screen_name  {:label (t :username)
                  :icon "user"
                  :disabled true
                  :value (:screen_name @form-data)}
   :email         {:label (t :email)
                   :icon "mail"
                   :placeholder (t :email)
                   :warning (t :invalid-email)
                   :value (:email @form-data)
                   :validators [form/email?]}
   :github_handle {:label (t :github-handle)
                   :value (:github_handle @form-data)}
   :bio           {:label (t :bio)
                   :type :textarea
                   :placeholder (t :bio-placeholder)
                   :style {:resize "none"
                           :height "96px"}
                   :value (:bio @form-data)}})

(rum/defc email-notification-settings
  [user]
  [:div#email-notification-settings {:style {:padding "24px 12px"}}
   [:h3 {:style {:margin-bottom 24}}
    (t :email-notification-settings)]
   [:div.row1
    [:a {:on-click (fn []
                     (citrus/dispatch! :user/update {:email_notification (not (:email_notification @user))}))}
     [:i {:class (if (:email_notification @user)
                   "fa fa-check-square"
                   "fa fa-square-o")
          :style {:font-size 20
                  :margin-right 12}}]]
    (t :email-notification-settings-text)]])


(rum/defcs languages-settings <
  (rum/local nil ::languages)
  [state {:keys [languages] :as user}]
  (let [languages-atom (get state ::languages)
        _ (when (nil? @languages-atom)
            (reset! languages-atom languages))
        button-cp (fn [lang value]
                    (let [followed? (contains? (set @languages-atom) value)]
                      (ui/button {:class (str "btn-sm "
                                              (if followed? "btn-primary"))
                                  :style {:margin-right 12
                                          :margin-bottom 12}
                                  :on-click (fn []
                                              (let [value (if followed?
                                                            (vec (distinct (remove #{value} @languages-atom)))
                                                            (vec (distinct (conj @languages-atom value))))]
                                                (reset! languages-atom value)
                                                (citrus/dispatch! :user/update {:languages value})))}
                        lang)))]
    [:div#languages {:style {:padding "24px 12px"}}
     [:h3 (t :languages)]
     [:p {:style {:margin-bottom "24px"
                  :font-size 16}}
      (t :select-which-languages)]

     [:div.row1 {:style {:flex-wrap "wrap"}}
      (button-cp "English" "en")
      (button-cp "中文"     "zh")
      (button-cp "Japanese" "japanese")
      (button-cp "German" "german")
      (button-cp "French" "french")
      (button-cp "Spanish" "spanish")
      (button-cp "Russian" "russian")
      (button-cp "Italian" "italian")]]))

(rum/defc misc-settings < rum/reactive
  []
  (let [hide-votes? (citrus/react [:hide-votes?])]
    [:div#misc {:style {:padding "24px 12px"}}
     [:h3 {:style {:margin-bottom 24}}
      (t :misc)]

     [:div.row1
      [:a {:on-click (fn []
                       (citrus/dispatch!
                        (if hide-votes?
                          :citrus/show-votes
                          :citrus/hide-votes)))}
       [:i {:class (if hide-votes?
                     "fa fa-check-square"
                     "fa fa-square-o")
            :style {:font-size 20
                    :margin-right 12}}]]
      (t :dont-show-vote-numbers)]]))

(rum/defcs profile < rum/reactive
  < (rum/local false ::uploading?)
  (rum/local false ::delete-dialog?)
  [state user]
  (let [uploading? (get state ::uploading?)
        delete-dialog? (get state ::delete-dialog?)]
    [:div.column {:style {:max-width 600
                          :margin-bottom 300}}
     [:input
      {:id "photo_upload"
       :type "file"
       :on-change (fn [e]
                    (image/upload
                     (.-files (.-target e))
                     (fn [file file-form-data]
                       (reset! uploading? true)
                       (.append file-form-data "name" (str (:screen_name @user)))
                       (.append file-form-data "invalidate" true)
                       (citrus/dispatch!
                        :image/upload
                        file-form-data
                        (fn [url]
                          (reset! uploading? false)
                          (citrus/dispatch! :notification/add :success (t :cached-change-avatar))
                          nil)))
                     :max-width 100
                     :max-height 100))
       :hidden true}]

     (form/render
       {:loading? [:user :loading?]
        :inner-style {:padding 40
                      :broder-radius "4px"
                      :min-width 500
                      :background "#FFF"}
        :header (fn [form]
                  (let [avatar (util/cdn-image (:screen_name @user))]
                    (if @uploading?
                      [:div {:style {:padding "12px 0"}}
                       (ui/donut)]

                      [:div.space-between {:style {:padding "12px 0"}}
                       [:a {:title (t :change-avatar)
                            ;; :style {:cursor "pointer"}
                            :on-click #?(:cljs
                                         (fn []
                                           (.click (gdom/getElement "photo_upload")))
                                         :clj
                                         identity)}
                        [:span
                         {:class "ant-avatar ant-avatar-circle" }
                         [:img {:src avatar}]]]])))

        :title (t :update-profile)
        :fields (profile-fields user)
        :on-submit (fn [form-data]
                     (let [data @form-data]
                       (citrus/dispatch! :user/update data)))})

     ;; email notification settings
     (email-notification-settings user)

     (languages-settings @user)

     (misc-settings)

     [:div {:style {:padding "24px 12px"}}
      [:h3 {:style {:margin-bottom 24}}
       (t :my-data)]
      (ui/button {:on-click (fn []
                              #?(:cljs
                                 (oset! js/window.location "href" "/user/putchar_profile.json")))}
        (t :export-my-data))
      (ui/button {:style {:margin-top 40}
                  :on-click (fn []
                              (reset! delete-dialog? true))}
        (t :delete-this-account))]

     ;; delete confirmation dialog
     (ui/dialog
      {:title (t :user-delete-confirm)
       :on-close #(reset! delete-dialog? false)
       :visible @delete-dialog?
       :wrap-class-name "center"
       :style {:width (min 600 (- (:width (util/get-layout)) 48))}
       :animation "zoom"
       :maskAnimation "fade"
       :footer (ui/button
                 {:class "btn-danger"
                  :on-click (fn []
                              #?(:cljs
                                 (oset! js/window.location "href" "/user/delete_request")))}
                 (t :delete))}
      (widgets/transform-content
       "### What happens when I delete my account on putchar?

* As soon as you confirm that you would like to delete your account, the following events happen immediately:

. 1. All of your personal profile information will be deleted in our database. This includes your name, username, email, profile photo, description, and any connections to 3rd party social networks (that you use to sign in to putchar).
You will be logged out and returned to the putchar home page.
The posts and comments that you have posted will not be deleted, in order to preserve the integrity of the public nature of discussions on putchar. Any posts or comments that remain undeleted will not be identifiable as yours.

. 2. If you wish to delete any posts or comments on putchar, please do this prior to deleting your account. You can view your profile.

. 3. All backups containing personal information are deleted after 30 days."
       {}))]))

(rum/defc user < rum/reactive
  (mixins/query :user)
  [params]
  (let [screen-name (:screen_name params)
        posts-path [:posts :by-screen-name screen-name :latest]
        user (citrus/react [:user :by-screen-name screen-name])
        posts (citrus/react posts-path)]
    (if user
      (let [{:keys [id name screen_name bio]} user
            avatar (util/cdn-image screen_name)]
        [:div.column.center-area {:class "user-posts"
                                  :style {:margin-bottom 48}}
         (widgets/user-card user)

         ;; posts
         [:div
          (widgets/tags screen_name (:tags user) nil)

          (query/query
            (post/user-post-list id posts posts-path))]])
      [:div.row {:style {:justify-content "center"
                         :margin-top 24}}
       (ui/donut)])))

(rum/defc links < rum/reactive
  (mixins/query :links)
  [params]
  (let [screen-name (:screen_name params)
        posts-path [:posts :by-screen-name screen-name :links]
        user (citrus/react [:user :by-screen-name screen-name])
        posts (citrus/react posts-path)]
    (if user
      (let [{:keys [id name screen_name bio]} user
            avatar (util/cdn-image screen_name)]
        [:div.column.center-area {:class "user-posts"
                                  :style {:margin-bottom 48}}
         (widgets/user-card user)

         ;; posts
         [:div
          (widgets/tags screen_name (:tags user) nil)

          (query/query
            (post/user-post-list id posts posts-path))]])
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])))

(rum/defc drafts < rum/reactive
  (mixins/query :drafts)
  [params]
  (if-let [user (citrus/react [:user :current])]
    (if user
      (let [posts (citrus/react [:drafts])]
        (let [{:keys [id name screen_name bio]} user
              avatar (util/cdn-image screen_name)]
          [:div.column.center-area {:class "user-posts"
                                    :style {:margin-bottom 48}}
           (widgets/user-card user)

           ;; posts
           [:div
            (query/query
              (post/user-post-list id posts nil))]]))
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])
    [:h1 "Signin first"]))

(rum/defc comments < rum/reactive
  (mixins/query :comments)
  [params]
  (let [screen-name (:screen_name params)
        comments-path [:comments :by-screen-name screen-name]
        user (citrus/react [:user :by-screen-name screen-name])
        comments (citrus/react comments-path)]
    (if user
      (let [{:keys [id name screen_name bio]} user
            avatar (util/cdn-image screen_name)]
        [:div.column.center-area {:style {:margin-bottom 48}}
         (widgets/user-card user)

         (query/query
           (comment/user-comments-list id comments))])
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])))

(rum/defc votes < rum/reactive
  (mixins/query :votes)
  [params]
  (let [post-filter :toped
        path [:posts :current-user post-filter]
        user (citrus/react [:user :current])
        posts (citrus/react path)]
    [:div.column
     [:h1.auto-padding {:style {:margin-top 24
                                :margin-bottom 24}}
      (str (t :votes) ":")]
     (query/query
       (post/post-list posts {:filter post-filter
                              :merge-path path}
                       :show-avatar? true
                       :empty-widget [:h2
                                      (t :no-votes-yet)]))]))
