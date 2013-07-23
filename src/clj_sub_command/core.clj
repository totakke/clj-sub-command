(ns
    #^{:author "Toshiki Takeuchi",
       :doc "A little library to process a command line sub-command for Clojure."}
  clj-sub-command.core
  (:use [clojure.string :only [blank? join]]))

(defn- make-cmdinfo [cmdspec]
  (vec (for [spec cmdspec]
         (vector (vec (filter keyword? spec))
                 (first (filter string? spec))
                 (last spec)))))

(defn print-help
  "Print help for sub-commands."
  ([desc cmdinfo]
     (if-not (blank? desc) (println desc))
     (println "Sub-commands")
     (doseq [cmd (seq cmdinfo)]
       (if (< 1 (count (first cmd)))
         (println (str "  " (join \, (map name (first cmd))) "  " (second cmd)))
         (println (str "  " (name (ffirst cmd)) "  " (second cmd))))))
  ([desc optmap cmdinfo]
     (if-not (blank? desc) (println desc))
     (println "Options")
     ;; TODO
     (println "Sub-commands")
     (doseq [cmd (seq cmdinfo)]
       (if (< 1 (count (first cmd)))
         (println (str "  " (join \, (map name (first cmd))) "  " (second cmd)))
         (println (str "  " (name (ffirst cmd)) "  " (second cmd)))))))

(defn contains-command?
  "Returns true if cmdvec includes cmd, false if not."
  [cmdvec cmd]
  (not= -1 (.indexOf cmdvec cmd)))

(defmacro do-sub-command
  "Binds the first argument to a sub-command and calls a specified function with
  the rest arguments."
  [args desc & cmdspec]
  (let [cmdinfo (make-cmdinfo cmdspec)]
    `(let [[cmd# & args#] ~args]
       (if (or (= cmd# "-h") (= cmd# "--help"))
         (print-help ~desc ~cmdinfo)
         (if-let [func# (some #(when (contains-command? (first %) (keyword cmd#)) (last %)) ~cmdinfo)]
           (apply func# args#)
           (apply (some #(when (contains-command? (first %) :else) (last %)) ~cmdinfo) args#))))))

(comment

  ;; Usage of do-sub-command:

  (defn fn1 [& args] ...)

  (defn fn2 [& args] ...)

  (defn fn34 [& args] ...)

  (defn fn-else [& args] ...)

  (do-sub-command *command-line-args*
    "Usage: cmd [-h] {sub1,sub2,sub3,sub4} ..."
    [:sub1 "Desc about fn1" fn1]          ; [:sub-command-name description function]
    [:sub2 fn2]                           ; Description can be ommited
    [:sub3 :sub4 "Desc aboud fn34" fn34]  ; Be able to bind multi-sub-commands to a function
    [:else "Desc about fn-else" fn-else]) ; :else is called by a no-binded sub-command

  )

(defn- group-by-opt-args [args optspec]
  (let [key-data (into {} (for [[syms _] (map #(split-with symbol? %)
                                              (conj optspec '[help? h?]))
                                sym syms]
                            [(re-find #"^.*[^?]" (str sym))
                             {:sym (str (first syms))}]))]
    (loop [[argkey & [argval :as r]] args
           ret {true [], false []}]
      (if argkey
        (let [[_ & [keybase]] (re-find #"^--?(.*)" argkey)]
          (condp = keybase
            nil (recur nil (update-in ret [false] #(apply conj % argkey r)))
            "" (recur nil (update-in ret [false] #(apply conj % argkey r)))
            (if-let [found (key-data keybase)]
              (if (= \? (last (:sym found)))
                (recur r (update-in ret [true] conj argkey))
                (recur (next r) (update-in ret [true] conj argkey (first r))))
              (throw (Exception. (str "Unknown option " argkey))))))
        ret))))

(defn make-optmap [args optspec]
  )

(defmacro with-sub-command
  "TODO"
  [args desc optspec cmdspec & body]
  (let [{opt-args true, [cmd & cmd-args] false} (group-by-opt-args args optspec)
        opt-locals (vec (map first optspec))
        cmdinfo (make-cmdinfo cmdspec)]
    `(let [{:strs ~opt-locals :as optmap#} (make-optmap ~opt-args '~optspec)
           (first ~cmdspec) (first ~args)
           (second ~cmdspec) (rest ~args)]
       (if (optmap# "help?")
         (print-help ~desc optmap# ~cmdinfo)
         (do ~@body)))))

(comment

  ;; Usage of with-sub-command:

  (defn fn1 [& args] ...)

  (defn fn2 [& args] ...)

  (defn fn34 [& args] ...)

  (defn fn-else [& args] ...)

  (with-sub-command *command-line-args*
    "Usage: cmd [-h] [-v] {sub1,sub2,sub3,sub4} ..."
    [[verbose? v?]]                            ; Binds options, the same way as clojure.contrib.command-line/with-command-line
    [sub args [[:sub1 "Desc about fn1"]        ; [:sub-command-name description function]
               :sub2                           ; Description can be ommited
               [:sub3 :sub4 "Desc aboud fn34"] ; First symbol is binded to subcmd
               [:else "Desc about fn-else"]]]  ; :else is called by a no-binded sub-command
    (binding [*debug-comments* verbose?]
      (condp = sub
        :sub1 (apply fn1 args)
        :sub2 (apply fn2 args)
        :sub3 (apply fn34 args)
        :else (apply fn-else args))))

  )
