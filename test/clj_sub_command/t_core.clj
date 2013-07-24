(ns clj-sub-command.t-core
  (:use midje.sweet
        clj-sub-command.core))

(defn plus [& args]
  (->> (map #(Integer/parseInt %) args)
       (apply +)))

(defn prod [& args]
  (->> (map #(Integer/parseInt %) args)
       (apply *)))

(defn hello [& args] "hello")

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
    []
    [[subcmd subargs] [[:sub1 "Plus args"]
                       :sub2
                       [:sub3 :sub4 "Multiply args"]]]
    (condp = subcmd
      :sub1 (apply plus subargs)
      :sub2 (apply prod subargs)
      :sub3 (apply prod subargs))))

(fact "about with-sub-command macro"
  (with-sub-command-test "sub1" "2" "3") => 5
  (with-sub-command-test "sub2" "2" "3") => 6
  (with-sub-command-test "sub3" "3" "4") => 12
  (with-sub-command-test "sub4" "3" "4") => 12
  (with-sub-command-test "sub5" "3" "4") => (throws Exception)
  (with-sub-command-test "-h") => nil?
  (with-sub-command-test "-h" "sub1") => nil?)
