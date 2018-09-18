(ns share.components.moderation-logs
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.widgets :as w]
            [share.util :as util]
            [share.kit.mixins :as mixins]
            [share.kit.query :as query]
            [share.kit.colors :as colors]
            [share.dicts :refer [t] :as dicts]))

(rum/defc logs < rum/reactive
  (mixins/query :moderation-logs)
  [params]
  [:div.auto-padding.column.moderation-logs
   [:h1 {:style {:margin-top 0}}
    "Moderation Logs"]
   [:div.column {:style {:margin-left 3}}
    (query/query
     (let [logs (citrus/react [:moderation-logs])
           reasons (dicts/reasons)]
       (if (seq logs)
         (for [{:keys [id moderator post_permalink comment_idx type data reason created_at] :as log} logs]
           [:div.col-item {:key id}
            [:div.space-between {:style {:align-items "center"
                                         :margin-bottom "1.5em"}}
             [:h4 {:style {:margin-top 0
                           :margin-bottom 0}}
              type]
             [:span {:style {:font-size 14
                             :color colors/shadow}}
              (util/date-format created_at "yyyy-MM-dd HH:mm:ss")]]
            (when moderator
              [:p "Moderator: " [:a.control {:href (str "/@" moderator)}
                                 (str "@" moderator)]])
            (when post_permalink
              [:p "Post: " [:a.control {:href (str "/" post_permalink)}
                            post_permalink]])
            (when data
              [:p "Data: " (pr-str data)])
            (when reason
              [:p "reason: " (nth reasons reason)])])
         [:div "No logs yet."])))]
   ])
