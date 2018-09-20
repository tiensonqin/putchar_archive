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
  [:div.auto-padding
   (widgets/transform-content
    (t :about-text)
    {})])
