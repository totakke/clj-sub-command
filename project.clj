(defproject clj-sub-command "0.2.4-SNAPSHOT"
  :description "A simple sub-command parser for Clojure."
  :url "https://github.com/totakke/clj-sub-command"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:plugins [[lein-midje "3.2"]]
                   :dependencies [[midje "1.8.3"]]}
             :1.7 [:dev {:dependencies [[org.clojure/clojure "1.7.0"]]}]
             :1.6 [:dev {:dependencies [[org.clojure/clojure "1.6.0"]]}]}
  :signing {:gpg-key "roimisia@gmail.com"})
