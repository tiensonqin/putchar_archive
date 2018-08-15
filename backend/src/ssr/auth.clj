(ns ssr.auth
  (:require [taoensso.timbre :as timbre]
            [clj-social.core :as social]
            [api.config :as config]
            [api.util :as util]
            [api.db.user :as u]
            [api.db.util :as du]
            [api.db.token :as token]
            [api.cookie :as cookie]
            [clojure.java.jdbc :as j]))

(def github-token (atom nil))
(defn github [{:keys [datasource redis]} data]
  (let [{:keys [app-key app-secret redirect-uri]} (get-in config/config [:oauth :github])
        instance (social/make-social :github app-key app-secret redirect-uri :state (str (util/uuid))
                                     :scope "user:email")
        access-token (social/getAccessToken instance (:code data))
        info (social/getUserInfo instance access-token)]
    ;; save github tokens
    ;; id, screen_name, description, name, lang
    (j/with-db-connection [conn datasource]
      ;; save github tokens
      (token/create conn {:github_id (str (:id info))
                          :token (.getAccessToken access-token)})
      (if-let [user (u/oauth-authenticate conn :github (str (:id info)))]
        user
        info))))
