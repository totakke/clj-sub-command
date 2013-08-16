(defproject clj-sub-command "0.1.1-SNAPSHOT"
  :description "A simple sub-command parser for Clojure."
  :url "https://github.com/totakke/clj-sub-command"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
