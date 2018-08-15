(ns share.components.home
  (:require [rum.core :as rum]
            [share.kit.query :as query]
            [share.kit.ui :as ui]
            [share.kit.colors :as colors]
            [share.components.group :as group]
            [share.components.login :as login]
            [share.components.post :as post]
            [share.components.widgets :as widgets]
            [share.components.layout :as layout]
            [share.kit.mixins :as mixins]
            [appkit.citrus :as citrus]
            [share.dicts :refer [t]]
            [share.util :as util]
            [share.config :as config]
            [clojure.string :as str]))

(rum/defc home < rum/reactive
  {:will-mount (fn [state]
                 (citrus/dispatch! :citrus/reset-current-group-channel nil)
                 state)
   :did-remount (fn [old-state state]
                   (citrus/dispatch! :citrus/reset-current-group-channel nil)
                   state)}
  (mixins/query :home)
  [params]
  [:div.column {:style {:padding-bottom 48}}
   (widgets/cover-nav nil nil)

   (let [posts (citrus/react [:posts :hot])]
     (query/query
       (post/post-list posts
                       {:merge-path [:posts :hot]}
                       :show-group? true)))])
