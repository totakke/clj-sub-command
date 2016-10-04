(ns clj-sub-command.core-test
  (:require [clojure.test :refer :all]
            [clj-sub-command.core :refer :all]))

(deftest candidates-test
  (let [candidates' #'clj-sub-command.core/candidates]
    (is (= (candidates' "sttatus" #{"status" "commit" "push"}) ["status"]))
    (is (= (candidates' "unknown" #{"status" "commit" "push"}) []))))

(deftest candidate-message-test
  (is (= (candidate-message ["pull"]) "Did you mean this?\n        pull"))
  (is (= (candidate-message ["pull" "push"]) "Did you mean one of these?\n        pull\n        push"))
  (is (nil? (candidate-message [])))
  (is (nil? (candidate-message nil))))

(deftest sub-command-test
  (let [[opts cmd args _ cands]
        (sub-command ["-p" "8080" "--no-verbose" "command1" "filename"]
                     :options  [["-p" "--port" :parse-fn #(Integer. %)]
                                ["--[no-]verbose" :default true]]
                     :commands [["command1" "desc for command1"]
                                ["command2" "desc for command2"]])]
    (testing "for option parsing"
      (is (= opts {:port 8080, :verbose false})))
    (testing "for command parsing"
      (is (= cmd :command1)))
    (testing "for sub-command arguments"
      (is (= args ["filename"])))
    (testing "for candidates"
      (is (= cands ["command1" "command2"])))))
