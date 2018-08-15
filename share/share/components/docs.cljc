(ns share.components.docs
  (:require [share.components.widgets :as widgets]
            [rum.core :as rum]
            #?(:clj [clojure.java.io :as io])
            [appkit.citrus :as citrus]
            [share.components.layout :as layout]
            [share.util :as util]))


(defonce docs (atom nil))

(defn- get-doc
  [doc-name locale]
  #?(:clj
     (if-let [doc (get @docs [doc-name locale])]
       doc
       (let [doc (slurp (io/resource (str "public/docs/" (name doc-name) "/" (name locale) ".adoc")))]
         (swap! docs assoc [doc-name locale] doc)
         doc))
     :cljs nil))

(rum/defc privacy < rum/reactive
  []
  (let [locale (citrus/react [:locale])
        doc (get-doc "privacy" locale)]
    [:div.column.auto-padding {:style {:margin-bottom 64}}
     (widgets/transform-content doc {:body-format :asciidoc})]))

(rum/defc terms < rum/reactive
  []
  (let [locale (citrus/react [:locale])
        doc (get-doc "terms" locale)]
    [:div.column.auto-padding {:style {:margin-bottom 64}}
     (widgets/transform-content doc {:body-format :asciidoc})
     ]))

(rum/defc code-of-conduct < rum/reactive
  []
  (let [locale (citrus/react [:locale])
        doc (get-doc "code_of_conduct" locale)]
    [:div.column.auto-padding {:style {:margin-bottom 64}}
     (widgets/transform-content doc {:body-format :asciidoc})]))
