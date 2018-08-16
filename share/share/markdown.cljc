(ns share.markdown
  (:require #?(:clj [clojure.java.io :as io])
            #?(:cljs [marked :as marked])
            [clojure.string :as string])
  #?(:clj
     (:import com.vladsch.flexmark.ext.toc.TocExtension
                   com.vladsch.flexmark.ext.autolink.AutolinkExtension
                   com.vladsch.flexmark.ext.footnotes.FootnoteExtension
                   com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
                   com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
                   com.vladsch.flexmark.ext.tables.TablesExtension
                   com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension
                   com.vladsch.flexmark.html.HtmlRenderer
                   com.vladsch.flexmark.parser.Parser
                   com.vladsch.flexmark.util.options.MutableDataSet
                   [com.vladsch.flexmark.ext.yaml.front.matter
                    AbstractYamlFrontMatterVisitor
                    YamlFrontMatterExtension]
                   java.util.ArrayList)))

#?(:clj
   (defn build-options
     []
     (doto (MutableDataSet.)
       (.set Parser/EXTENSIONS
             (ArrayList. [(AnchorLinkExtension/create)
                          (AutolinkExtension/create)
                          (FootnoteExtension/create)
                          (StrikethroughExtension/create)
                          (TocExtension/create)
                          (TablesExtension/create)
                          (YouTubeLinkExtension/create)
                          (YamlFrontMatterExtension/create)])))))

#?(:clj
   (defn parser-builder
     [^MutableDataSet options]
     (.build (Parser/builder options))))

#?(:clj
   (defn renderer
     [^MutableDataSet options]
     (.build (HtmlRenderer/builder options))))

#?(:clj
   (defonce markdown-parser ^Parser$Builder
     (parser-builder (build-options))))

#?(:clj
   (defonce markdown-html-renderer ^HtmlRenderer$Builder
     (renderer (build-options))))

(defn render [str]
  #?(:clj
     (let [doc (.parse markdown-parser str)]
            (into {:content (.render markdown-html-renderer doc)}
                  (map (fn [[key [value]]]
                         [(keyword key)
                          value]))
                  (.getData (doto (AbstractYamlFrontMatterVisitor.)
                              (.visit doc)))))
     :cljs
     ;; TODO: add toc extension
     (marked str)
     ))
