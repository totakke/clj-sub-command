(ns clj-sub-command.t-core
  (:use midje.sweet
        [clj-sub-command.core]))

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
    [:cmd1 "Plus args" plus]
    [:cmd2 prod]
    [:cmd3 :cmd4 "Multiply args" prod]
    [:else hello]))

(fact "about do-sub-command macro"
  (do-sub-command-test "cmd1" "2" "3") => 5
  (do-sub-command-test "cmd2" "2" "3") => 6
  (do-sub-command-test "cmd3" "3" "4") => 12
  (do-sub-command-test "cmd4" "3" "4") => 12
  (do-sub-command-test "cmd5") => "hello")
