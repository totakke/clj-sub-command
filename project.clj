(defproject clj-sub-command "0.4.1-SNAPSHOT"
  :description "A simple subcommand parser for Clojure"
  :url "https://github.com/totakke/clj-sub-command"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/tools.cli "0.3.7"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}})
