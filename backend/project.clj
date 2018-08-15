(defproject backend "0.1.0-SNAPSHOT"
  :description "lambdahackers backend."
  :url "https://github.com/tiensonqin/lambdahackers"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [hiccup "2.0.0-alpha1"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [pinyin4clj "0.2.0"]
                 ;; [aleph "0.4.4"]
                 [primitive-math "0.1.6"]
                 [org.clojure/data.xml "0.0.8"]
                 [com.taoensso/encore "2.87.0"]
                 [ring/ring-defaults "0.1.4"]
                 [ring/ring-devel "1.6.3"]
                 [ring-middleware-format "0.7.2"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [honeysql "0.9.1"]
                 [hikari-cp "1.8.2"]
                 [com.taoensso/carmine "2.16.0"]
                 [buddy/buddy-sign "2.2.0"]
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [com.taoensso/timbre "4.10.0"]
                 [ring-cors "0.1.7"]
                 [clj-social "0.1.1"]
                 [com.cemerick/url "0.1.1"]
                 [amazonica "0.3.115"]
                 [tick "0.3.5"]
                 [clucie "0.3.0"]
                 [org.clojure/core.async "0.3.465"]
                 [bk/ring-gzip "0.2.1"]
                 [bidi "2.1.2"]
                 [org.flatland/ordered "1.5.6"]
                 [frankiesardo/linked "1.3.0"]
                 [tiensonqin/appkit "0.1.7"]

                 ;; server side rendering
                 [org.clojure/clojurescript "1.9.946"]
                 [org.asciidoctor/asciidoctorj "1.5.6"]
                 [markdown-clj "1.0.2"]
                 [compojure "1.6.0"]
                 [tongue "0.2.4"]
                 [http-kit "2.2.0"]
                 [selmer "1.11.6"]
                 ]

  :source-paths ["src" "../share"]
  :jvm-opts ["-Duser.timezone=UTC" "-Dclojure.spec.check-asserts=true"]

  :plugins [[lein-environ "1.1.0"]]

  :min-lein-version "2.6.1"

  :main backend.application
  ;; :aot :all

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (go) and (reset)
  ;; live.
  :repl-options {:init-ns user}

  :bikeshed {:max-line-length 200}

  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.13"]
                                  [reloaded.repl "0.2.3"]
                                  [enlive "1.1.6"]
                                  [expound "0.6.0"]]
                   :source-paths ["dev"]}
             ;; :uberjar {:main backend.application
             ;;           :aot [backend.application com.stuartsierra.component com.stuartsierra.dependency]}
             })
