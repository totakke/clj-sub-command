(ns clj-sub-command.t-core
  (:use midje.sweet
        clj-sub-command.core))

(let [[opts cmd args _] (sub-command ["-p" "8080" "--no-verbose" "command1" "filename"]
                                     :options  [["-p" "--port" :parse-fn #(Integer. %)]
                                                ["--[no-]verbose" :default true]]
                                     :commands [["command1" "desc for command1"]
                                                ["command2" "desc for command2"]])]
     (fact "Test for option parsing"
       opts => {:port 8080, :verbose false})

     (fact "Test for command parsing"
       cmd => :command1)

     (fact "Test for sub-command arguments"
       args => ["filename"]))
