(ns
    #^{:author "Toshiki Takeuchi",
       :doc "Process sub-command and the rest arguments."}
  clj-sub-command.core
  (:use [clojure.string :only [blank? join]]))

(defn print-help [desc cmdmap]
  (if-not (blank? desc) (println desc))
  (println (str "Usage: cmd [-h] {"
                (join \, (map (comp name first) (seq cmdmap)))
                "} ..."))
  (println "Sub-commands")
  (doseq [cmd (seq cmdmap)]
    (println (str "  " (name (first cmd)) "  " (:doc (second cmd))))))

(defmacro with-sub-command
  "Binds the first argument to a sub-command and calls a specified function by
  the rest arguments."
  [args desc & cmdspec]
  (let [cmdmap (apply merge (for [[key info] (partition 2 cmdspec)]
                              {key {:doc (if (string? (first info)) (first info) nil)
                                    :cmd (last info)}}))]
    `(let [[cmd# & args#] ~args]
       (if (or (= cmd# "-h") (= cmd# "--help"))
         (print-help ~desc ~cmdmap)
         (if-let [func# (:cmd ((keyword cmd#) ~cmdmap))]
           (func# args#)
           ((:cmd (:else ~cmdmap)) args#))))))

(comment

  ;; Example of usage:

 (defn bar1 [args]
   nil)

 (defn bar2 [args]
   nil)

 (defn bar3 [args]
   nil)

 (with-sub-command *command-line-args*
   "Usage: cmd [foo1|foo2] & args"
   :foo1 ["Runs bar1" bar1]
   :foo2 ["Runs bar2" bar2]
   :else ["Runs bar3" bar3])

 )
