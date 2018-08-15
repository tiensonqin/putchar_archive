(ns share.kit.query
  (:require [rum.core :as rum]
            [appkit.citrus :as citrus]
            #?(:cljs [appkit.db :as db])
            [share.kit.ui :as ui]
            [share.kit.mixins :as mixins]
            [share.util :as util]
            [share.query :refer [queries]]
            [share.dommy :as dommy]
            #?(:cljs ["prop-types" :as prop-types])))

(defn get-context
  "Extract context from component."
  [component]
  #?(:cljs (-> component .-context (js->clj :keywordize-keys true))))

(rum/defcc query
  < rum/reactive
  {:static-properties {:contextTypes {:route-handler #?(:cljs prop-types/string
                                                        :clj nil)
                                      :args #?(:cljs prop-types/string
                                               :clj nil)}}}
  [comp content & {:keys [loading]}]

  ;; queried, => show result
  ;; else, start to query => loading

  #?(:clj
     content
     :cljs

     (let [{:keys [route-handler args]} (get-context comp)
           args (if (contains? #{:groups} route-handler)
                  nil
                  (util/read-string args))
           route-handler (if (= :comment route-handler)
                           :post
                           route-handler)
           q-fn (get queries (keyword route-handler))
           q    (q-fn @db/state args)]
       (if (nil? q)
         content

         (let [qid  (util/md5-query q)
               result (citrus/react [:query :cache qid])]
           (cond
             result
             content

             :else
             (if loading
               loading
               [:div.row {:style {:justify-content "center"}}
                (ui/donut)]))))))
  )
