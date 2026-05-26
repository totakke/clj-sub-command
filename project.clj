(defproject clj-sub-command "1.0.0-SNAPSHOT"
  :description "A simple subcommand parser for Clojure"
  :url "https://github.com/totakke/clj-sub-command"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.5" :scope "provided"]
                 [org.clojure/clojurescript "1.12.145" :scope "provided"]
                 [org.clojure/tools.cli "1.4.256"]]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.11"]]
  :profiles {:1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.4"]]}}
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/testable.js"
                                   :output-dir "target"
                                   :main clj-sub-command.runner
                                   :optimizations :none
                                   :target :nodejs}}]})
