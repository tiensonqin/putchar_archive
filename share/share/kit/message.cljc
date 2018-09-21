(ns share.kit.message
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.util :as util]))

(rum/defcs message
  [state type body {:keys [duration]
                    :or {duration 2000}}]
  (if (and type body)
    (let [[icon-type color] (case type
                              :info ["info" "#1890ff"]
                              :success ["check_circle" "#52c41a"]
                              :error ["error" "#f5222d"]
                              :warning ["warning" "#faad14"])]
      [:div.ant-message
       [:span
        [:div.ant-message-notice
         [:div.ant-message-notice-content
          [:div.ant-message-custom-content {:class (str "ant-message-" (name type))}
           (ui/icon {:type icon-type
                     :color color})
           [:span {:style {:margin-left 6}}
            body]]]]]])))
