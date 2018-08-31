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

(rum/defcs add-avatar < rum/reactive
  < (rum/local false ::uploading?)
  [state user]
  (let [uploading? (get state ::uploading?)
        avatar (get state ::avatar)]
    [:div.column {:style {:align-items "center"}}
     [:h1 (t :click-circle-add-avatar)]
     (if @uploading?
       [:div {:style {:padding "12px 0"}}
        (ui/donut)]

       [:div {:style {:padding "12px 0"
                      :cursor "pointer"}
              :on-click #?(:cljs
                           (fn []
                             (.click (gdom/getElement "photo_upload")))
                           :clj
                           identity)}
        [:div {:style {:border-radius "50%"
                       :width "8rem"
                       :height "8rem"
                       :background "#ccc"}}]])

     (ui/button {:style {:margin-top 24
                         :width 250}
                 :on-click (fn []
                             (citrus/dispatch! :citrus/default-update
                                               [:user :signup-step]
                                               :pick-groups))}
       (t :skip))
     [:input
      {:id "photo_upload"
       :type "file"
       :on-change (fn [e]
                    (image/upload
                     (.-files (.-target e))
                     (fn [file file-form-data]
                       (reset! uploading? true)
                       (.append file-form-data "name" (str (:screen_name user)))
                       (citrus/dispatch!
                        :image/upload
                        file-form-data
                        (fn [url]
                          (reset! uploading? false)
                          (citrus/dispatch! :citrus/default-update
                                            [:user :signup-step]
                                            :pick-groups))))))
       :hidden true}]]))

(rum/defc groups-bar
  [groups signup-groups show-name?]
  [:div.row {:style {:flex-wrap "wrap"
                     :align-itmes "flex-end"}}
   (for [{:keys [id name] :as group} groups]
     (let [joined? (and (seq signup-groups)
                        (contains? (set signup-groups) name))]
       [:div.column1 {:key id
                      :style {:margin-right 24
                              :margin-bottom 24
                              :justify-content "center"}}
        [:a {:on-click #(citrus/dispatch! (if joined?
                                            :user/signup-leave-group
                                            :user/signup-join-group) name)
             :title (util/original-name name)}
         [:img.hover-shadow {:src (util/group-logo name 120 120)
                             :class (if joined? "joined" "")}]]
        (if show-name?
          [:div.ubuntu {:style {:font-size 15
                                :font-weight "600"
                                :max-width 120
                                :text-overflow "ellipsis"
                                :overflow "hidden"
                                :white-space "nowrap"
                                :margin-top 12}}
           (util/original-name name)])]))])

(rum/defc pick-groups < rum/reactive
  (mixins/query :groups)
  [user]
  (let [locale (citrus/react [:locale])
        signup-groups (citrus/react [:user :signup-groups])
        loading? (citrus/react [:user :loading?])
        width (citrus/react [:layout :current :width])
        mobile? (or (util/mobile?) (<= width 768))
        groups-width (- (min 1104 width) (if mobile? 48 0))
        groups (citrus/react [:group :hot])]
    [:div.column {:style {:padding (if mobile?
                                     "24px 24px 48px 24px"
                                     "24px 0 48px 0")
                          :margin "0 auto"}}
     [:div.row1 {:style {:align-items "center"}}
      (if (:screen_name user)
        [:div {:style {:margin-right 12}}
         (ui/avatar {:shape "circle"
                     :src (util/cdn-image (:screen_name user) false)
                     :class "ant-avatar-lg"})])

      [:h2 {:style {:margin-bottom 48}}
       (t :join-at-least)]]

     [:div.pick-groups-item {:style {:width groups-width}}
      (groups-bar groups signup-groups true)]

     (if (> (count signup-groups) 0)
       (ui/button {:class "btn-primary btn-lg"
                   :on-click (fn []
                               (citrus/dispatch! :user/signup-join-groups))
                   :style {:margin-top 24}}
         (if loading?
           [:div {:style {:margin-top 2}}
            (ui/donut-white)]
           (t :done))))]))

(rum/defc signup < rum/reactive
  [{:keys [email] :as params}]
  (let [signup-step (citrus/react [:user :signup-step])
        temp-user (citrus/react [:user :temp])
        setup-github-sync? (citrus/react [:setup-github-sync?])]
    [:div.signup.row
     (case signup-step
      :add-avatar
      (add-avatar temp-user)

      :pick-groups
      (pick-groups temp-user)

      (let [github-avatar (:avatar_url temp-user)
            user (select-keys temp-user [:id :name :email])
            user (-> user
                     (assoc :bio (or (:bio temp-user) (:description temp-user)))
                     (assoc :screen_name (or (:screen_name temp-user) (:login temp-user)))
                     (assoc :website (or (:website temp-user) (:blog temp-user)))
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
                                        :github_handle (:login temp-user)
                                        :setup-github-sync? setup-github-sync?})]
                             (citrus/dispatch! :user/new data form-data)))})
           (form/render
             {:title (t :welcome)
              :init-state {:email email}
              :loading? [:user :loading?]
              :fields (signup-fields user username-taken? email-taken? email)
              :submit-text (t :signup)
              :on-submit (fn [form-data]
                           (citrus/dispatch! :user/new @form-data form-data))}))]))]))

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
   :website       {:label (t :website)
                   :placeholder (t :website-placeholder)
                   :value (:website @form-data)}
   :twitter_handle {:label (t :twitter-handle)
                    :value (:twitter_handle @form-data)}
   :github_handle {:label (t :github-handle)
                   :value (:github_handle @form-data)}
   :bio           {:label (t :bio)
                   :type :textarea
                   :placeholder (t :bio-placeholder)
                   :style {:resize "none"
                           :height "150px"}
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

(def github-repo-fields
  {:link {:validators [util/link?]
          :placeholder (t :github-repo-link-placeholder)}})

(rum/defcs github-repo < rum/reactive
  (rum/local false ::expand?)
  [state user]
  (let [expand? (get state ::expand?)]
    [:div#github-repo {:style {:padding "24px 12px"}}
     [:h3 {:style {:margin-bottom 24}}
      (t :github-sync)]

     (cond
       (and (:github_repo @user)
            (:github_handle @user))
       [:div.row {:style {:align-items "center"
                          :justify-content "space-between"}}
        [:div.row1 {:style {:align-items "center"}}
         (ui/icon {:type "github"})
         [:a {:style {:margin-left 12
                      :font-weight "500"}
              :href (:github_repo @user)}
          (util/get-github-repo-name (:github_repo @user))]]

        [:a {:title (t :edit)
             :on-click #(reset! expand? true)}
         (ui/icon {:type "edit"
                   :color "#999"})]]

       :else
       [:div.column1
        (widgets/github-connect)

        (widgets/transform-content
         (t :github-connect-text)
         {:style {:margin-top 24}
          :body-format "asciidoc"})])

     (if @expand?
       [:div {:style {:margin-top 24}}
        (form/render
          {:fields github-repo-fields
           :cancel-button? false
           :on-submit (fn [form-data]
                        (citrus/dispatch! :user/update {:github_repo (:link @form-data)})
                        (reset! expand? false))
           :style {:padding 0}
           :loading? [:user :loading?]})])]))

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
    [:div {:style {:padding "24px 12px"}}
    [:h3 (t :languages)]
     [:p {:style {:margin-bottom "24px"
                  :font-size 16}}
      (t :select-which-languages)]

     [:div.row1 {:style {:flex-wrap "wrap"}}
      (button-cp "English" "en")
      (button-cp "简" "zh-cn")
      (button-cp "繁" "zh-tw")
      (button-cp "Japanese" "japanese")
      (button-cp "German" "german")
      (button-cp "French" "french")
      (button-cp "Spanish" "spanish")
      (button-cp "Russian" "russian")
      (button-cp "Italian" "italian")]]))

(rum/defc misc-settings < rum/reactive
  []
  (let [hide-votes? (citrus/react [:hide-votes?])]
    [:div {:style {:padding "24px 12px"}}
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
                          nil)))))
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

     ;; connect with github
     (github-repo user)

     (languages-settings @user)

     (misc-settings)

     [:div {:style {:padding "24px 12px"}}
      [:h3 {:style {:margin-bottom 24}}
       (t :my-data)]
      (ui/button {:on-click (fn []
                              #?(:cljs
                                 (oset! js/window.location "href" "/user/lambdahackers_profile.json")))}
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
       "### What happens when I delete my account on lambdahackers?

* As soon as you confirm that you would like to delete your account, the following events happen immediately:

. 1. All of your personal profile information will be deleted in our database. This includes your name, username, email, profile photo, description, and any connections to 3rd party social networks (that you use to sign in to lambdahackers).
All existing memberships you have in any group will be disabled.
You will be logged out and returned to the lambdahackers home page.
The posts and comments that you have posted will not be deleted, in order to preserve the integrity of the public nature of discussions on lambdahackers. Any posts or comments that remain undeleted will not be identifiable as yours.

. 2. If you wish to delete any posts or comments on lambdahackers, please do this prior to deleting your account. You can view your profile.

. 3. All backups containing personal information are deleted after 30 days."
       {:body-format :asciidoc}))]))

(rum/defc user < rum/reactive
  (mixins/query :user)
  [params]
  (let [screen-name (:screen_name params)
        posts-path [:posts :by-screen-name screen-name :newest]
        user (citrus/react [:user :by-screen-name screen-name])
        posts (citrus/react posts-path)]
    (if user
      (let [{:keys [id name screen_name bio website]} user
            avatar (util/cdn-image screen_name)]
        [:div.column.center-area {:class "user-posts"
                                  :style {:margin-bottom 48}}
         (widgets/user-card user)

         (widgets/posts-comments-header screen_name)

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
        (let [{:keys [id name screen_name bio website]} user
             avatar (util/cdn-image screen_name)]
          [:div.column.center-area {:class "user-posts"
                                    :style {:margin-bottom 48}}
          (widgets/user-card user)

          (widgets/posts-comments-header screen_name)

          ;; posts
          [:div
           (query/query
             (post/user-post-list id posts nil))]]))
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])
    [:h1 "Signin first"]
    ))

(rum/defc comments < rum/reactive
  (mixins/query :comments)
  [params]
  (let [screen-name (:screen_name params)
        comments-path [:comments :by-screen-name screen-name]
        user (citrus/react [:user :by-screen-name screen-name])
        comments (citrus/react comments-path)]
    (if user
      (let [{:keys [id name screen_name bio website]} user
            avatar (util/cdn-image screen_name)]
        [:div.column.center-area {:class "user-posts"
                                  :style {:margin-bottom 48}}
         (widgets/user-card user)

         (widgets/posts-comments-header screen_name)

         (query/query
           (comment/user-comments-list id comments))])
      [:div.row {:style {:justify-content "center"}}
       (ui/donut)])))

(rum/defc votes < rum/reactive
  (mixins/query :votes)
  [params]
  (let [post-filter :voted
        path [:posts :current-user post-filter]
        user (citrus/react [:user :current])
        posts (citrus/react path)
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))]
    [:div.column.center-area {:class "user-posts"
                              :style {:margin-bottom 48}}
     ;; posts
     [:h2 {:style (cond-> {:margin "0 0 24px 0"}
                    mobile?
                    (assoc :padding-left 12))}
      (str (t :votes) ":")]
     (query/query
       (post/post-list posts {:filter post-filter
                              :merge-path path}
                       :show-avatar? true
                       :show-group? false))]))

(rum/defc bookmarks < rum/reactive
  (mixins/query :bookmarks)
  [params]
  (let [post-filter :bookmarked
        path [:posts :current-user post-filter]
        user (citrus/react [:user :current])
        posts (citrus/react path)
        mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))]
    [:div.column.center-area {:style {:margin-bottom 48}}
     ;; posts
     [:h2 {:style (cond-> {:margin "0 0 24px 0"}
                    mobile?
                    (assoc :padding-left 12))}
      (str (t :bookmarks) ":")]
     (query/query
       (post/post-list posts {:filter post-filter
                              :merge-path path}
                       :show-avatar? true
                       :show-group? false
                       :empty-widget [:h5.auto-padding
                                      (t :no-bookmarks-yet)]))]))
