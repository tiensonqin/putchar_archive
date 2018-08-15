(ns ssr.resolver
  (:require [api.cookie :as cookie]
            [api.handler.query :as query]
            [share.query :refer [queries]]
            [share.merge :as merge]
            [api.util :as util]
            [api.db.top :as top]
            [api.db.bookmark :as bookmark]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.report :as report]
            [api.db.invite :as invite]
            [api.services.slack :as slack]
            [share.util :as su]
            [clojure.string :as str]
            [share.dicts :as dicts]))

(defn make-resolver [req]
  (let [locale (su/get-locale req)
        _ (reset! dicts/locale locale)
        {:keys [handler route-params]} (:ui/route req)
        handler (if (= handler :comment) :post handler)
        uid (get-in req [:context :uid])
        q-fn (get queries handler)
        _ (reset! su/user-agent (get-in req [:headers "user-agent"]))
        mobile? (su/mobile?)
        db (:datasource (:context req))
        current-user (query/get-current-user (:context req) nil)
        route-params (if (and (= handler :home) uid)
                       (assoc route-params :current-user current-user)
                       route-params)
        hot-groups (if (or (nil? current-user)
                           (= :groups handler))
                     (query/get-hot-groups (:context req) {:limit 12}))
        valid-invite? (and (= handler :group)
                           (get-in req [:params :token])
                           (= (invite/get-group-name db (get-in req [:params :token]))
                              (:group-name route-params)))
        admin-groups (if current-user
                       (group/get-user-managed-ids db (:screen_name current-user))
                       nil)
        state {:search-mode? false
               :router       (:ui/route req)
               :layout       {:show-panel? false
                              :current {:width (if mobile? 400 1024)
                                        :height (if mobile? 400 1024)}}
               :locale       (keyword locale)
               :hide-github-connect? (get-in req [:cookies "hide-github-connect" :value])
               :user         {:current current-user}
               :notification nil
               :image        {}
               :group        (cond->
                               {:loading? false
                                :current nil
                                :hot hot-groups
                                :managed admin-groups}
                               valid-invite?
                               (assoc :invited-group (:group-name route-params)))
               :channel      {:loading? false
                              :current nil}
               :post         {:loading? false
                              :current nil
                              :filter (if (= handler :home)
                                        :hot
                                        :latest-reply)
                              :toped (if uid (top/get-toped-posts uid) nil)
                              :bookmarked (if uid (bookmark/get-bookmarked-posts uid) nil)}
               :comment      nil
               :report       {:new? (if (seq admin-groups)
                                      (report/has-new? admin-groups)
                                      false)}
               :search       nil}
        state (if q-fn
                (let [query (q-fn nil route-params)]
                  (if query
                    (let [{:keys [q args] :as q-opts} query
                          state (assoc state :query
                                       (merge
                                        {:loading? {handler false}}
                                        q-opts))
                          result (let [[query result] (query/query (:context req) q args)]
                                   (if (= query :ok)
                                     result
                                     nil))]
                      (if (contains? (set (vals result)) :not-found)
                        {:not-found true}
                        (let [state (assoc state
                                           :initial-query-result result
                                           )]
                          (reduce (fn [state [k v]]
                                   (merge/mergef
                                    state
                                    handler
                                    q-opts
                                    result
                                    k))
                                 state
                                 (if (:merge q-opts)
                                   (:merge q-opts)
                                   {handler true})))))
                    state))
                (assoc state :query {:loading? nil
                                     :q nil}))]
    state))

(comment
  (def req {:headers {"user-agent" "not-mobile"}
            :ui/route {:handler :post
                       :route-params {:permalink "foo-6-7db9abd828e144379229f376b2d09964"}
                       }
            :context {:datasource user/db
                      :uid #uuid "392dd0c3-4ed2-473f-9e59-339cd16c2e18"}})

  (clojure.pprint/pprint (make-resolver req))
  )
