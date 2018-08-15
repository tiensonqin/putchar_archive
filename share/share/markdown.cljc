(ns share.markdown
  (:require [markdown.core :as markdown]))

(defn md->html [md-string]
  (let [options {:reference-links? true
                 :footnotes? true
                 :heading-anchors true}]
    #?(:cljs (markdown/md->html md-string options)
       :clj (apply markdown/md-to-html-string md-string (interleave (keys options) (vals options))))))
