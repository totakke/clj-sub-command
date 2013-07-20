(ns
    #^{:author "Toshiki Takeuchi",
       :doc "Process a command line sub-command."}
  clj-sub-command.core
  (:use [clojure.string :only [blank? join]]))

(defn print-help
  "Print help for sub-commands."
  [desc cmdinfo]
  (if-not (blank? desc) (println desc))
  (println "Sub-commands")
  (doseq [cmd (seq cmdinfo)]
    (if (< 1 (count (first cmd)))
      (println (str "  " (join \, (map name (first cmd))) "  " (second cmd)))
      (println (str "  " (name (ffirst cmd)) "  " (second cmd))))))

(defn contains-command?
  "Returns true if cmdvec includes cmd, false if not."
  [cmdvec cmd]
  (not= -1 (.indexOf cmdvec cmd)))

(defmacro do-sub-command
  "Binds the first argument to a sub-command and calls a specified function with
  the rest arguments."
  [args desc & cmdspec]
  (let [cmdinfo (vec (for [spec cmdspec]
                       (vector (vec (filter keyword? spec))
                               (first (filter string? spec))
                               (last spec))))]
    `(let [[cmd# & args#] ~args]
       (if (or (= cmd# "-h") (= cmd# "--help"))
         (print-help ~desc ~cmdinfo)
         (if-let [func# (some #(when (contains-command? (first %) (keyword cmd#)) (last %)) ~cmdinfo)]
           (apply func# args#)
           (apply (some #(when (contains-command? (first %) :else) (last %)) ~cmdinfo) args#))))))

(comment

  ;; Example of usage:

  (defn plus [& args]
    (->> (map #(Integer/parseInt %) args)
         (apply +)))

  (defn prod [& args]
    (->> (map #(Integer/parseInt %) args)
         (apply *)))

  (defn hello [& args] "hello")

  (do-sub-command *command-line-args*
    "Usage: cmd {plus,prod} ..."
    [:plus "Plus args" plus]
    [:prod "Multiply args" prod]
    [:else "Hello" hello])

  )
