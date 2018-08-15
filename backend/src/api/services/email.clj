(ns api.services.email
  (:require [clojure.java.io :as io]
            [api.config :refer [config]]
            [api.db.group :as group]
            [api.db.invite :as invite]
            [api.db.util :as du]
            [taoensso.timbre :as t]
            [amazonica.core :as core]
            [amazonica.aws.simpleemail :as ses]
            [clojure.string :as str]
            [cheshire.core :refer [generate-string]]
            [selmer.parser :as selmer]
            [selmer.util]
            [share.util :as util]
            [share.asciidoc :as ascii]
            [api.services.email.template :as template]
            [share.dicts :refer [t]]))

;; TODO: i18n

(defmacro aws-with-credential
  [& body]
  `(try
     (let [config# (:us-east-1 config)
           access-key# (:access-key config#)
           secret-key# (:secret-key config#)
           endpoint# (:endpoint config#)]
       (core/with-credential [access-key# secret-key# endpoint#]
         ~@body))
     (catch Exception e#
       (t/error e#)
       false)))

(defn send-email
  [to-addresses subject text html]
  (when (seq to-addresses)
    (aws-with-credential
     (ses/send-email :destination {:to-addresses to-addresses}
                     :source "lambdahackers <no-reply@lambdahackers.com>"
                     :message {:subject subject, :body {:text text
                                                        :html html}}))))

(defn- get-template
  [path]
  (slurp (io/resource (str "email_templates/" path))))

(defn create-template
  [{:keys [name subject html-path text-path]}]
  (aws-with-credential
   (ses/create-template {:Template
                         {:TemplateName name
                          :SubjectPart subject
                          :HtmlPart (get-template html-path)
                          :TextPart (get-template text-path)
                          }})))

(defn delete-template
  [name]
  (aws-with-credential
   (ses/delete-template {:TemplateName name})))

(defn send-welcome
  [to confirmation-code]
  (let [data {:code confirmation-code}
        title (t :welcome-to-lambdahackers)]
    (send-email [to] title
                (selmer/render (get-template "welcome.txt")
                  data)
                (template/template
                 title
                 {:body [:div
                         [:p (t :root-title)]
                         [:p (t :root-description)]]
                  :action-link (str
                                (:website-uri config)
                                "/email_confirmation?code="
                                confirmation-code)
                  :action-text (t :activate-your-account)}))))

(defn send-invite
  [db to {:keys [who
                 group-name]
          :as data}]
  (when (seq to)
    (when-let [group (group/get db group-name)]
      (let [invite? (= (:privacy group) "invite")
            stared-groups (du/query db {:select [:email :stared_groups]
                                        :from [:users]
                                        :where [:and
                                                [:= :block false]
                                                [:in :email to]]})
            to (remove
                (fn [email]
                  (contains? (set (get-in (util/normalize :email stared-groups) [email :stared_groups]))
                             (:id group)))
                to)]
        (when (seq to)
          (let [name (util/original-name group-name)
                token (if invite? (:token (invite/create db group-name)))
                group-link (str "https://lambdahackers.com/" group-name
                                (if token (str "?token=" token)))
                title (format "Invite to group %s on lambdahackers.com!" name)]
            (send-email to title
                        (selmer/render (get-template "invite.txt")
                          (merge data
                                 {:group-link group-link
                                  :group-name name}))
                        (template/template
                         title
                         {:body [:div
                                 [:p "Hi there,
"]
                                 [:p [:a {:href (str "https://lambdahackers.com/@" who)
                                          :target "_blank"}
                                      [:img {:src (str (:img-cdn config) "/" who ".jpg?w=40&h=40")
                                             :style  "border-radius: 6px;margin-right:6px;"}]
                                      [:span who]]
                                  (format " invited you to join the group of %s on lambdahackers.com!"
                                          name)]]
                          :action-link group-link
                          :action-text (str "Join " name)}))))))))

(defn send-comment
  [to-addresses {:keys [title post-title post_url screen_name body created_at comment_url] :as data
                 :or {title "New comment"}}]
  (send-email to-addresses title
              (selmer/render
                (get-template "comment_notification.txt")
                data)
              (selmer.util/without-escaping
               (selmer/render
                 (get-template "comment_notification.html")
                 data
                 :safe true))))
