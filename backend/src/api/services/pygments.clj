(ns api.services.pygments
  (:require [net.cgrand.enlive-html :as html :refer [attr=]]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [api.services.slack :as slack])
  (:import (java.io StringReader)))

(defn h
  [content]
  (html/html-resource (StringReader. content)))

(defn pygmentize
  ([lexer content]
   (pygmentize "html" lexer content))
  ([formatter lexer content]
   (if lexer
     (let [{:keys [exit out err]}
          (shell/sh "/usr/bin/pygmentize"
                    "-f" formatter
                    "-l" lexer
                    :in content)]
      (if (not (zero? exit))
        (do
          (slack/error "pygmentize error:" err lexer content)
          content)
        (first (h out))))
     content)))

;; Replaces the content of the element. Values can be nodes or collection of nodes.
(defn transform!
  [nodes selector]
  (html/transform nodes selector (fn [{:keys [attrs content] :as node}]
                                   (let [lang (if-let [lang (or (:data-lang attrs)
                                                                (:class attrs))]
                                                  (str/replace lang "language-" ""))]
                                     {:tag :code
                                      :attrs {:data-lang lang}
                                      :content (map (partial pygmentize lang) content)}))))

(defn highlight!
  [content]
  (let [result (when (and (string? content)
                          (not (str/blank? content)))
                 (some->> (transform! (h content) [:pre :code])
                          html/emit*
                          (apply str)))]
    result))

(comment
  (def content "<div class=\"sect2\">\n<h3 id=\"_test_highlight\">Test highlight</h3>\n<div class=\"listingblock\">\n<div class=\"content\">\n<pre class=\"pygments highlight\"><code data-lang=\"clojure\">(defn hello\n  [x]\n  (prn x))</code></pre>\n</div>\n</div>\n<div class=\"listingblock\">\n<div class=\"content\">\n<pre class=\"pygments highlight\"><code data-lang=\"ocaml\">let with_document opts source f =</code></pre>\n</div>\n</div>\n<div class=\"listingblock\">\n<div class=\"content\">\n<pre class=\"pygments highlight\"><code data-lang=\"ruby\">def output x\nend</code></pre>\n</div>\n</div>\n<div class=\"paragraph\">\n<p>Some long sentence. [^footnote]</p>\n</div>\n<div class=\"paragraph\">\n<p>[^footnote]: Test, [Link](<a href=\"https://google.com\" class=\"bare\">https://google.com</a>).</p>\n</div>\n</div>")

  (highlight! content))
