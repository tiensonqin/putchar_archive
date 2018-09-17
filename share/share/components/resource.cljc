(ns share.components.resource
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.helpers.form :as form]
            [share.components.widgets :as widgets]
            [share.config :as config]
            [share.util :as util]
            [share.dicts :refer [t]]
            [share.kit.colors :as colors]
            [clojure.string :as str]))

(rum/defc resources
  [object-type])

(rum/defc resource
  [object-type id])

(rum/defc edit-resource
  [object-type id])
