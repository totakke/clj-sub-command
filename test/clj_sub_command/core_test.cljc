(ns clj-sub-command.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as s]
            [clj-sub-command.core :refer [candidate-message parse-cmds
                                          sub-command summarize-cmds]]))

(defn- parse-int [x]
  #?(:clj (Integer/parseInt x)
     :cljs (do (assert (re-seq #"^\d" x))
               (js/parseInt x))))

(deftest candidates-test
  (let [candidates' #'clj-sub-command.core/candidates]
    (is (= (candidates' "sttatus" #{"status" "commit" "push"}) ["status"]))
    (is (= (candidates' "unknown" #{"status" "commit" "push"}) []))))

(deftest candidate-message-test
  (is (= (candidate-message ["pull"]) "The most similar command is\n        pull"))
  (is (= (candidate-message ["pull" "push"]) "The most similar commands are\n        pull\n        push"))
  (is (nil? (candidate-message [])))
  (is (nil? (candidate-message nil))))

(deftest sub-command-test
  (let [[opts cmd args _ cands]
        (sub-command ["-p" "8080" "--no-verbose" "command1" "filename"]
                     :options  [["-p" "--port" :parse-fn parse-int]
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

(deftest compile-command-specs-test
  (is (= (#'clj-sub-command.core/compile-command-specs [["command1" "desc for command1"]
                                                        ["command2" "desc for command2"
                                                         :id :cmd2]
                                                        ["command3" "desc for command3"
                                                         :group "Group A"]])
         [{:id :command1, :desc "desc for command1", :cmd "command1",
           :group nil}
          {:id :cmd2, :desc "desc for command2", :cmd "command2", :group nil}
          {:id :command3, :desc "desc for command3", :cmd "command3",
           :group "Group A"}])))

(deftest summarize-cmds-test
  (let [compile-specs #'clj-sub-command.core/compile-command-specs]
    (testing "empty specs"
      (is (= (summarize-cmds []) "")))

    (testing "ungrouped specs render flat (no headings, backward compatible)"
      (is (= (summarize-cmds (compile-specs [["up" "Start server"]
                                             ["down" "Stop server"]]))
             (str "  up    Start server\n"
                  "  down  Stop server"))))

    (testing "all-grouped specs render with headings"
      (is (= (summarize-cmds
              (compile-specs [["create" "Create a resource" :group "Basic"]
                              ["expose" "Expose a service" :group "Basic"]
                              ["rollout" "Manage rollout" :group "Deploy"]]))
             (str "Basic:\n"
                  "  create   Create a resource\n"
                  "  expose   Expose a service\n"
                  "\n"
                  "Deploy:\n"
                  "  rollout  Manage rollout"))))

    (testing "groups appear in first-appearance order, not alphabetical"
      (let [out (summarize-cmds
                 (compile-specs [["zoo" "z" :group "Zeta"]
                                 ["alpha" "a" :group "Alpha"]]))]
        (is (s/starts-with? out "Zeta:"))
        (is (< (s/index-of out "Zeta:") (s/index-of out "Alpha:")))))

    (testing "ungrouped specs in a mixed list fall under \"Other Commands:\""
      (is (= (summarize-cmds (compile-specs [["create" "Create" :group "Basic"]
                                             ["help" "Show help"]]))
             (str "Basic:\n"
                  "  create  Create\n"
                  "\n"
                  "Other Commands:\n"
                  "  help    Show help"))))

    (testing "\"Other Commands\" is pinned to the end, regardless of input order"
      (is (= (summarize-cmds (compile-specs [["help"   "Show help"]
                                             ["create" "Create" :group "Basic"]]))
             (str "Basic:\n"
                  "  create  Create\n"
                  "\n"
                  "Other Commands:\n"
                  "  help    Show help"))))

    (testing "single group renders with a heading"
      (is (= (summarize-cmds (compile-specs [["create" "Create" :group "Basic"]]))
             (str "Basic:\n"
                  "  create  Create"))))

    (testing "command column aligns across groups"
      (let [out (summarize-cmds (compile-specs [["a"          "short" :group "G1"]
                                                ["very-long"  "long"  :group "G2"]]))]
        (is (re-find #"  a          short" out))
        (is (re-find #"  very-long  long" out))))))

(deftest parse-cmds-test
  (testing "w/o options"
    (let [m (parse-cmds ["command1" "file1" "file2"]
                        []
                        [["command1" "desc for command1"]
                         ["command2" "desc for command2"]])]
      (is (empty? (:options m)))
      (is (= (:command m) :command1))
      (is (= (:arguments m) ["file1" "file2"]))
      (is (s/blank? (:options-summary m)))
      (is (not (s/blank? (:commands-summary m))))
      (is (nil? (:errors m)))
      (is (= (:candidates m) ["command1" "command2"]))))
  (testing "with options"
    (let [m (parse-cmds ["-p" "8080" "--verbose" "command1" "file"]
                        [["-p" "--port PORT" "Port number"
                          :parse-fn parse-int]
                         ["-v" "--verbose"]]
                        [["command1" "desc for command1"]
                         ["command2" "desc for command2"]])]
      (is (= (:options m) {:port 8080, :verbose true}))
      (is (= (:command m) :command1))
      (is (= (:arguments m) ["file"]))
      (is (not (s/blank? (:options-summary m))))
      (is (not (s/blank? (:commands-summary m))))
      (is (nil? (:errors m)))
      (is (= (:candidates m) ["command1" "command2"]))))
  (testing "error"
    (let [m (parse-cmds ["--vervose" "sstatus"]
                        [["-v" "--verbose"]]
                        [["status" "Status"]
                         ["commit" "Commit"]
                         ["push" "Push"]])]
      (is (= (count (:errors m)) 2))
      (is (= (:candidates m) ["status"])))
    (let [m (parse-cmds []
                        [["-v" "--verbose"]]
                        [["status" "Status"]
                         ["commit" "Commit"]
                         ["push" "Push"]])]
      (is (= (:errors m) ["Unknown command: \"\""]))))
  (testing "function options"
    (let [m (parse-cmds []
                        [["-a" "--alpha"] ["-b" "--beta"]]
                        [["command1"] ["command2"]]
                        :allow-empty-command true
                        :options-summary-fn (fn [specs]
                                              (str "Options: " (s/join \| (map :long-opt specs))))
                        :commands-summary-fn (fn [specs]
                                               (str "Commands: " (s/join \, (map :cmd specs)))))]
      (is (nil? (:errors m)))
      (is (= (:options-summary m) "Options: --alpha|--beta"))
      (is (= (:commands-summary m) "Commands: command1,command2")))
    (let [m (parse-cmds ["command3"]
                        []
                        [["command1"] ["command2"]]
                        :allow-empty-command true)]
      (is (= (count (:errors m)) 1))))
  (testing "grouped commands"
    (let [m (parse-cmds ["create" "file"]
                        []
                        [["create"  "Create a resource" :group "Basic"]
                         ["rollout" "Manage rollout"    :group "Deploy"]])]
      (is (= (:command m) :create))
      (is (s/includes? (:commands-summary m) "Basic:"))
      (is (s/includes? (:commands-summary m) "Deploy:"))
      (is (s/includes? (:commands-summary m) "  create   Create a resource")))))
