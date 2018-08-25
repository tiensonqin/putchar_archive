(ns share.kit.modal
  (:require [rum.core :as rum]
            [share.util :as util]))

;; copy from re-com
(rum/defc modal
  [{:keys [child wrap-nicely? backdrop-color backdrop-opacity on-close class style attr]
    :or   {wrap-nicely? true backdrop-color "black" backdrop-opacity 0.6}
    :as   args}]
  [:div    ;; Containing div
   (merge {:class  (str "rc-modal-panel display-flex " class)
           :style (merge {:position "fixed"
                          :left     "0px"
                          :top      "0px"
                          :width    "100%"
                          :height   "100%"
                          :z-index  1020}
                         style)}
          attr)
   [:div    ;; Backdrop
    {:class    "rc-modal-panel-backdrop"
     :style    {:position         "fixed"
                :width            "100%"
                :height           "100%"
                :background-color backdrop-color
                :opacity          backdrop-opacity
                :z-index          1}
     :on-click (fn [e]
                 (when on-close
                   (on-close))
                 (util/stop e))}]
   [:div    ;; Child container
    {:class    "rc-modal-panel-container"
     :style (merge {:margin  "auto"
                    :z-index 2}
                   (when wrap-nicely? {:background-color "white"
                                       :padding          "16px"
                                       :border-radius    "6px"}))}
    child]])

(rum/defc dialog
  [{:keys [visible on-close style
           animation maskAnimation footer]}
   child]
  (let [child [:div.column1 {:style (merge {:width (min 600 (- (:width (util/get-layout)) 48))}
                                           style)}
               child
               footer]]
    (if visible
      (modal {:child child
              :on-close on-close}))))
