(ns share.markdown
  (:require #?(:clj [clojure.java.io :as io])
            #?(:cljs [marked :as marked])
            #?(:cljs [share.front-matter :as fm])
            [clojure.string :as string])
  #?(:clj
     (:import com.vladsch.flexmark.ext.toc.TocExtension
              com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
              com.vladsch.flexmark.ext.typographic.TypographicExtension
              com.vladsch.flexmark.ext.autolink.AutolinkExtension
              com.vladsch.flexmark.ext.footnotes.FootnoteExtension
              com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
              [com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughExtension StrikethroughSubscriptExtension]
              com.vladsch.flexmark.ext.tables.TablesExtension
              com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
              com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension
              com.vladsch.flexmark.html.HtmlRenderer
              com.vladsch.flexmark.parser.Parser
              com.vladsch.flexmark.util.options.MutableDataSet
              com.vladsch.flexmark.ext.definition.DefinitionExtension
              com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension
              com.vladsch.flexmark.superscript.SuperscriptExtension
              ;; com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension
              java.util.ArrayList
              [com.vladsch.flexmark.profiles.pegdown Extensions PegdownOptionsAdapter])))


#?(:clj
   (defn build-options
     []
     (doto (MutableDataSet.)
       (.set HtmlRenderer/SOFT_BREAK "<br />\n")
       (.set HtmlRenderer/HARD_BREAK "<br />\n")
       (.set Parser/EXTENSIONS
             (ArrayList. [(AnchorLinkExtension/create)
                          (AutolinkExtension/create)
                          (FootnoteExtension/create)
                          (StrikethroughExtension/create)
                          (TocExtension/create)
                          (TablesExtension/create)
                          (YouTubeLinkExtension/create)
                          (TaskListExtension/create)
                          (DefinitionExtension/create)
                          (AbbreviationExtension/create)
                          (SuperscriptExtension/create)
                          (TypographicExtension/create)
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
   (def markdown-parser ^Parser$Builder
     (parser-builder (build-options))))

#?(:clj
   (def markdown-html-renderer ^HtmlRenderer$Builder
     (renderer (build-options))))

(defn render [str]
  #?(:clj
     (let [doc (.parse markdown-parser str)]

       (.render markdown-html-renderer doc))
     :cljs
     (marked str)))

;; AST
#?(:clj
   (defn node-children [n]
     (loop [c (.getFirstChild n)
            acc []]
       (if (some? c)
         (recur (.getNext c) (conj acc c))
         acc))))
