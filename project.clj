(defproject clj-sub-command "0.5.0-SNAPSHOT"
  :description "A simple subcommand parser for Clojure"
  :url "https://github.com/totakke/clj-sub-command"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/tools.cli "0.4.1"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}})
