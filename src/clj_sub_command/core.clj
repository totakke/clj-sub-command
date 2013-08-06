(ns
    #^{:author "Toshiki Takeuchi",
       :doc "A simple sub-command parser for Clojure."}
  clj-sub-command.core
  (:use [clojure.string :only [blank? join]]))

(defn- align
  "Align strings given as vectors of columns, with first vector
   specifying right or left alignment (:r or :l) for each column."
  [spec & rows]
  (let [maxes (vec (for [n (range (count (first rows)))]
                     (apply max (map (comp count #(nth % n)) rows))))
        fmt (join " "
                  (for [n (range (count maxes))]
                    (str "%"
                         (when-not (zero? (maxes n))
                           (str (when (= (spec n) :l) "-") (maxes n)))
                         "s")))]
    (join "\n"
          (for [row rows]
            (apply format fmt row)))))

(defn- rmv-q
  "Remove ?"
  [#^String s]
  (if (.endsWith s "?")
    (.substring s 0 (dec (count s)))
    s))

(defn- print-options
  "Prints options' names and descriptions."
  [optspec]
  (println "Options")
  (println
   (apply align [:l :l :l]
          (for [spec optspec]
            (let [[argnames [text default]] (split-with symbol? spec)
                  [_ opt q] (re-find #"^(.*[^?])(\??)$"
                                     (str (first argnames)))
                  argnames  (map (comp rmv-q str) argnames)
                  argnames
                  (join ", "
                        (for [arg argnames]
                          (if (= 1 (count arg))
                            (str "-" arg)
                            (str "--" arg))))]
              [(str "  " argnames (when (= "" q) " <arg>") " ")
               text
               (if-not default
                 ""
                 (str " [default " default "]"))])))))

(defn- print-sub-commands
  "Prints sub-commands' names and descriptions."
  [cmdspec]
  (println "Sub-commands")
  (println
   (apply align [:l :l]
          (for [spec cmdspec]
            (let [[cmdkeys rest] (split-with keyword? (if (vector? spec) spec (vector spec)))
                  [_ [text]] (split-with symbol? rest)
                  cmdnames (join ", " (map name cmdkeys))]
              [(str "  " cmdnames " ")
               (if (blank? text) "" text)])))))

(defn print-help
  "Prints help for sub-commands."
  ([desc cmdinfo]
     (if-not (blank? desc) (println desc))
     (println "Sub-commands")
     (doseq [cmd (seq cmdinfo)]
       (if (< 1 (count (first cmd)))
         (println (str "  " (join \, (map name (first cmd))) "  " (second cmd)))
         (println (str "  " (name (ffirst cmd)) "  " (second cmd))))))
  ([desc optmap cmdmap]
     (if-not (blank? desc) (println desc))
     (let [optspec (:optspec optmap), cmdspec (:cmdspec cmdmap)]
      (if-not (empty? optspec) (print-options optspec))
      (if-not (empty? cmdspec) (print-sub-commands cmdspec)))))

(defn group-by-optargs [args optspec]
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
  (let [key-data (into {} (for [[syms [_ default]] (map #(split-with symbol? %)
                                                        (conj optspec '[help? h?]))
                                sym syms]
                            [(re-find #"^.*[^?]" (str sym))
                             {:sym (str (first syms)) :default default}]))
        defaults (into {} (for [[_ {:keys [default sym]}] key-data
                                :when default]
                            [sym default]))]
    (loop [[argkey & [argval :as r]] args
           optmap (assoc defaults :optspec optspec)]
      (if argkey
        (let [[_ & [keybase]] (re-find #"^--?(.*)" argkey)]
          (cond
           (= keybase nil) (recur r optmap)
           (= keybase "") optmap
           :else (if-let [found (key-data keybase)]
                   (if (= \? (last (:sym found)))
                     (recur r (assoc optmap (:sym found) true))
                     (recur (next r) (assoc optmap (:sym found)
                                            (if (or (nil? r) (= \- (ffirst r)))
                                              (:default found)
                                              (first r)))))
                   (throw (Exception. (str "Unknown option " argkey))))))
        optmap))))

(defn make-cmdmap [subcmd cmdspec]
  (let [key-data (into {} (for [[syms [fn _]] (map #(split-with keyword? (if (vector? %) % (vector %))) cmdspec)
                                sym syms]
                            [(name sym) {:sym (first syms), :fn fn}]))]
    (let [found (key-data subcmd)]
      {:cmd (:sym found), :do (:fn found), :cmdspec cmdspec})))

(defmacro do-sub-command
  "Binds the first argument to a sub-command and calls a specified function with
  the rest arguments."
  [args desc & cmdspec]
  `(let [{optargs# true, [subcmd# & subargs#] false} (group-by-optargs ~args [])
         optmap# (make-optmap optargs# [])
         {fn# :do :as cmdmap#} (make-cmdmap subcmd# '~cmdspec)]
     (if (optmap# "help?")
       (print-help ~desc optmap# cmdmap#)
       (if (nil? fn#)
         (throw (Exception. (str "Unknown sub-command " subcmd#)))
         (apply (resolve fn#) subargs#)))))

(comment

  ;; Usage of do-sub-command:

  (defn fn1 [& args] ...)

  (defn fn2 [& args] ...)

  (defn fn34 [& args] ...)

  (do-sub-command *command-line-args*
    "Usage: cmd [-h] {sub1,sub2,sub3,sub4} ..."
    [:sub1 fn1 "Desc about fn1"]          ; [:sub-command-name function description]
    [:sub2 fn2]                           ; Description can be ommited
    [:sub3 :sub4 fn34 "Desc aboud fn34"]) ; Be able to bind multi-sub-commands to a function

  )

(defmacro with-sub-command
  "Binds local options and a sub-command and sub-args to command-line args."
  [args desc spec & body]
  (let [optspec (vec (drop-last  spec))
        cmdspec (last spec)
        optlocals (vec (map first optspec))
        [[subcmd subargs] cmdspec] cmdspec]
    `(let [{optargs# true, [subcmd# & subargs#] false} (group-by-optargs ~args '~optspec)
           {:strs ~optlocals :as optmap#} (make-optmap optargs# '~optspec)
           {~subcmd :cmd :as cmdmap#} (make-cmdmap subcmd# '~cmdspec)
           ~subargs subargs#]
       (if (optmap# "help?")
         (print-help ~desc optmap# cmdmap#)
         (if (nil? ~subcmd)
           (throw (Exception. (str "Unknown sub-command " subcmd#)))
           (do ~@body))))))

(comment

  ;; Usage of with-sub-command:

  (defn fn1 [& args] ...)

  (defn fn2 [& args] ...)

  (defn fn34 [& args] ...)

  (with-sub-command *command-line-args*
    "Usage: cmd [-h] [-v] [--version] {sub1,sub2,sub3,sub4} ..."
    [[verbose? v?]                                   ; Binds options, the same way as clojure.contrib.command-line/with-command-line
     [version? "Print version" false]
     [[sub args] [[:sub1 "Desc about fn1"]           ; [:sub-command-name description]
                  :sub2                              ; Description can be ommited
                  [:sub3 :sub4 "Desc aboud fn34"]]]] ; sub is binded to the first symbol, :sub3 in this case
    (binding [*debug-comments* verbose?]
      (condp = sub
        :sub1 (apply fn1 args)
        :sub2 (apply fn2 args)
        :sub3 (apply fn34 args))))

  )
