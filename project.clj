(defproject clj-sub-command "0.3.0"
  :description "A simple sub-command parser for Clojure"
  :url "https://github.com/totakke/clj-sub-command"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0-alpha13"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}})
