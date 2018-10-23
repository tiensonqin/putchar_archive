(ns ssr.resolver
  (:require [api.cookie :as cookie]
            [api.handler.query :as query]
            [share.query :refer [queries]]
            [share.merge :as merge]
            [api.util :as util]
            [api.db.top :as top]
            [api.db.report :as report]
            [api.db.resource :as resource]
            [api.services.slack :as slack]
            [share.admins :as admins]
            [share.util :as su]
            [share.kit.colors :as colors]
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
        latest-books (when-not current-user
                       (resource/get-resources db "book" {:limit 7} [:object_id :title]))

        state {:search-mode? false
               :router       (:ui/route req)
               :layout       {:show-panel? false
                              :current {:width (if mobile? 400 1024)
                                        :height (if mobile? 400 1024)}}
               :locale       (keyword locale)
               :hide-votes?  (= (get-in req [:cookies "hide-votes" :value]) "true")
               :user         {:current current-user}
               :notification nil
               :image        {}
               :post         {:loading? false
                              :current nil
                              :filter :hot
                              :toped (if uid (top/get-toped-posts uid) nil)}
               :comment      nil
               :report       {:new? (if (and current-user (admins/admin? (:screen_name current-user)))
                                      (report/has-new? db (:screen_name current-user))
                                      false)}
               :search       nil
               :books   {:latest latest-books}}
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
