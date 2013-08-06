(ns clj-sub-command.t-core
  (:use midje.sweet
        clj-sub-command.core))

(defn plus [& args]
  (reduce + (map #(Integer/parseInt %) args)))

(defn prod [& args]
  (reduce * (map #(Integer/parseInt %) args)))

(defn do-sub-command-test [& args]
  (do-sub-command args
    "Test for do-sub-command macro"
    [:sub1 plus "Plus args"]
    [:sub2 prod]
    [:sub3 :sub4 prod "Multiply args"]))

(fact "about do-sub-command macro"
  (do-sub-command-test "sub1" "2" "3") => 5
  (do-sub-command-test "sub2" "2" "3") => 6
  (do-sub-command-test "sub3" "3" "4") => 12
  (do-sub-command-test "sub4" "3" "4") => 12
  (do-sub-command-test "sub5" "3" "4") => (throws Exception)
  (do-sub-command-test "-h") => nil?
  (do-sub-command-test "-h" "sub1") => nil?)

(defn with-sub-command-test [& args]
  (with-sub-command args
    "Test for with-sub-command macro"
    [[opt?]
     [[sub args] [[:sub1 "Plus args"]
                  :sub2
                  [:sub3 :sub4 "Multiply args"]]]]
    (if opt?
      "opt"
      (condp = sub
        :sub1 (apply plus args)
        :sub2 (apply prod args)
        :sub3 (apply prod args)))))

(fact "about with-sub-command macro"
  (with-sub-command-test "sub1" "2" "3") => 5
  (with-sub-command-test "sub2" "2" "3") => 6
  (with-sub-command-test "sub3" "3" "4") => 12
  (with-sub-command-test "sub4" "3" "4") => 12
  (with-sub-command-test "sub5" "3" "4") => (throws Exception)
  (with-sub-command-test "--opt" "sub1") => "opt"
  (with-sub-command-test "-h") => nil?
  (with-sub-command-test "-h" "sub1") => nil?)
