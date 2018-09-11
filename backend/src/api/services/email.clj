(ns api.services.email
  (:require [clojure.java.io :as io]
            [api.config :refer [config]]
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
     (let [config# (:aws config)
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
                     :source "Putchar <no-reply@putchar.org>"
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
        title (t :welcome-to-putchar)]
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
  [db to {:keys [who]
          :as data}]
  (when (seq to)
    (let [token (:token (invite/create db))
          link (str "https://putchar.org" (if token (str "?token=" token)))
          title (format "%s invited you to join Putchar.org!" who)]
      (send-email to title
                  (selmer/render (get-template "invite.txt")
                    (merge data
                           {:invite-link link}))
                  (template/template
                   title
                   {:body [:div
                           [:p "Hi there,
"]
                           [:p [:a {:href (str "https://putchar.org/@" who)
                                    :target "_blank"}
                                [:img {:src (str (:img-cdn config) "/" who ".jpg")
                                       :style  "border-radius: 6px;margin-right:6px;"}]
                                [:span who]]
                            " invited you to join Putchar.org!"]]
                    :invite-link link
                    :action-text (str "Join Putchar.org")})))))

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
