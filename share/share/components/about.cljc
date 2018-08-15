(ns share.components.about
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.kit.colors :as colors]
            [share.components.widgets :as widgets]
            [appkit.citrus :as citrus]
            [share.dicts :refer [t]]
            [share.util :as util]
            [clojure.string :as str]))

(rum/defc about
  [params]
  [:div.ubuntu.auto-padding
   [:h1.heading-1 {:style {:margin-top 0
                           :margin-bottom "16px"}}
    "Lambdahackers"]

   [:p {:style {:font-size "1.125em"}}
    (t :about-description)]

   (widgets/transform-content
    (t :about-text)
    {:style {:padding-bottom 100}})])
