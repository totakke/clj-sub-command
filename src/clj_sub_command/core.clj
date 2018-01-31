(ns ^{:author "Toshiki Takeuchi",
      :doc "A simple subcommand parser for Clojure."}
  clj-sub-command.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :as s :refer [blank? join replace]]
            [clojure.pprint :as pp :refer [pprint cl-format]]
            [clojure.tools.cli :as cli]))

(def ^:dynamic *max-normalized-levenshtein-distance*
  "Max value of normalized levenshtein distance used in searching candidate
  commands. The default value is 0.5."
  0.5)

(defn- build-option-doc [{:keys [switches docs default]}]
  [(apply str (interpose ", " switches))
   (or (str default) "")
   (or docs "")])

(defn- build-command-doc [{:keys [command docs]}]
  [(name command)
   (or docs "")])

(defn- banner-for-options
  [options]
  (let [docs (into (map build-option-doc options)
                   [["-------" "-------" "----"]
                    ["Options" "Default" "Desc"]])
        max-cols (->> (for [d docs] (map count d))
                      (apply map (fn [& c] (apply vector c)))
                      (map #(apply max %)))
        vs (for [d docs]
             (mapcat (fn [& x] (apply vector x)) max-cols d))]
    (doseq [v vs]
      (cl-format true "~{ ~vA  ~vA  ~vA ~}" v)
      (prn))))

(defn- banner-for-commands
  [commands]
  (let [docs (into (map build-command-doc commands)
                   [["-------" "----"]
                    ["Command" "Desc"]])
        max-cols (->> (for [d docs] (map count d))
                      (apply map (fn [& c] (apply vector c)))
                      (map #(apply max %)))
        vs (for [d docs]
             (mapcat (fn [& x] (apply vector x)) max-cols d))]
    (doseq [v vs]
      (cl-format true "~{ ~vA  ~vA ~}" v)
      (prn))))

(defn- banner-for [desc options commands]
  (when desc
    (println desc)
    (println))
  (banner-for-options options)
  (println)
  (banner-for-commands commands)
)

(defn- name-for [k]
  (replace k #"^--no-|^--\[no-\]|^--|^-" ""))

(defn- flag-for [^String v]
  (not (.startsWith v "--no-")))

(defn- opt? [^String x]
  (.startsWith x "-"))

(defn- flag? [^String x]
  (.startsWith x "--[no-]"))

(defn- end-of-args? [x]
  (= "--" x))

(defn- option-for
  [arg options]
  (->> options
       (filter (fn [s]
                 (let [switches (set (s :switches))]
                   (contains? switches arg))))
       first))

(defn- default-values-for
  [specs]
  (reduce (fn [m s]
            (if (contains? s :default)
              ((:assoc-fn s) m (:name s) (:default s))
              m))
          {} specs))

(defn- apply-options
  [specs args]
  (loop [options    (default-values-for specs)
         extra-args []
         args       args]
    (if-not (seq args)
      [options extra-args]
      (let [opt  (first args)
            spec (option-for opt specs)]
        (cond
         (end-of-args? opt)
         (recur options (into extra-args (vec (rest args))) nil)

         (and (opt? opt) (nil? spec))
         (throw (Exception. (str "'" opt "' is not a valid argument")))

         (and (opt? opt) (spec :flag))
         (recur ((spec :assoc-fn) options (spec :name) (flag-for opt))
                extra-args
                (rest args))

         (opt? opt)
         (recur ((spec :assoc-fn) options (spec :name) ((spec :parse-fn) (second args)))
                extra-args
                (drop 2 args))

         :default
         (recur options (conj extra-args (first args)) (rest args)))))))

(defn- switches-for
  [switches flag]
  (-> (for [^String s switches]
        (cond
         (and flag (flag? s))            [(replace s #"\[no-\]" "no-") (replace s #"\[no-\]" "")]
         (and flag (.startsWith s "--")) [(replace s #"--" "--no-") s]
         :default                        [s]))
      flatten))

(defn- generate-option
  [raw-opt]
  (let [[switches raw-opt] (split-with #(and (string? %) (opt? %)) raw-opt)
        [docs raw-opt]     (split-with string? raw-opt)
        options            (apply hash-map raw-opt)
        aliases            (map name-for switches)
        flag               (or (flag? (last switches)) (options :flag))]
    (merge {:switches (switches-for switches flag)
            :docs     (first docs)
            :aliases  (set aliases)
            :name     (keyword (last aliases))
            :parse-fn identity
            :assoc-fn assoc
            :flag     flag}
           (when flag {:default false})
           options)))

(defn- command-for
  [cmdarg commands]
  (->> (map :command commands)
       (filter (partial = (keyword cmdarg)))
       (first)))

(defn- generate-command
  [raw-cmd]
  (let [[name doc] raw-cmd]
   {:command (keyword name)
    :docs doc}))

(defn- group-by-optargs
  [args options]
  (loop [optargs []
         args args]
    (if-let [option (option-for (first args) options)]
      (if (:flag option)
        (recur (conj optargs (first args)) (rest args))
        (recur (apply conj optargs (take 2 args)) (drop 2 args)))
      [optargs (vec args)])))

(defn- deep-merge-with
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn- levenshtein-distance
  [a b]
  (let [m (count a)
        n (count b)
        init (apply deep-merge-with (fn [a b] b)
                    (concat
                     (for [i (range 0 (+ 1 m))]
                       {i {0 i}})
                     (for [j (range 0 (+ 1 n))]
                       {0 {j j}})))
        table (reduce
               (fn [d [i j]]
                 (deep-merge-with
                  (fn [a b] b)
                  d
                  {i {j (if (= (nth a (- i 1))
                               (nth b (- j 1)))
                          ((d (- i 1)) (- j 1))
                          (min
                           (+ ((d (- i 1))
                               j) 1)
                           (+ ((d i)
                               (- j 1)) 1)
                           (+ ((d (- i 1))
                               (- j 1)) 1)))
                      }}))
               init
               (for [j (range 1 (+ 1 n))
                     i (range 1 (+ 1 m))] [i j]))]
    ((table m) n)))

(defn- normalized-levenshtein-distance
  [a b]
  (/ (levenshtein-distance a b) (max (count a) (count b))))

(defn- candidates
  "Returns candidate commands as a vector. The candidates are aligned in
  similarity order using levenshtein distance. If no similar commands exist,
  returns an empty vector not `nil`. See also
  `*max-normalized-levenshtein-distance*`.

  e.g.

      => (candidate \"sttatus\" #{\"status\" \"commit\" \"push\"})
      [\"status\"]"
  [s command-set]
  (->> command-set
       (map #(vector % (normalized-levenshtein-distance s %)))
       (filter #(<= (second %) *max-normalized-levenshtein-distance*))
       (sort-by second)
       (mapv first)))

(defn candidate-message
  "Returns message for telling a user candidate commands. Returns nil if
  candidates is empty or nil."
  [candidates]
  (if (seq candidates)
    (->> candidates
         (map (partial str "        "))
         (cons (if (= (count candidates) 1)
                 "The most similar command is"
                 "The most similar commands are"))
         (join \newline))))

(defn sub-command
  "THIS IS A LEGACY FUNCTION and may be deprecated in the future. Please use
  clj-sub-command.core/parse-cmds in new applications."
  [args & specs]
  (let [[desc {:keys [options commands]}] (if (string? (first specs))
                                            [(first specs) (rest specs)]
                                            [nil specs])
        options (map generate-option options)
        commands (map generate-command commands)
        [optargs [cmdarg & cmdspecs]] (group-by-optargs args options)
        banner (with-out-str (banner-for desc options commands))
        [options _] (apply-options options optargs)
        command (command-for cmdarg commands)
        candidates (candidates cmdarg (->> commands
                                           (map (comp name :command))
                                           (set)))]
    [options command (vec cmdspecs) banner candidates]))

(defn summarize-cmds
  "Reduces subcommands specs into a subcommands summary for printing at a
  terminal."
  [command-specs]
  (if (seq command-specs)
    (let [lens (apply map (fn [& cols]
                            (apply max (map count cols))) command-specs)]
      (->> command-specs
           (map #(s/trimr (pp/cl-format nil "~{  ~vA  ~vA~}" (interleave lens %))))
           (s/join \newline)))
    ""))

(defn parse-cmds
  "Parses arguments sequence according to given option and subcommand
  specifications.

  parse-cmds returns a map with seven entries:

    {:options          The options map, keyed by :id, mapped to the parsed value
     :command          The keyword of the detected subcommand
     :arguments        A vector of unprocessed arguments
     :options-summary  A string containing a minimal options summary
     :commands-summary A string containing a minimal subcommands summary
     :errors           A possible vector of error message strings generated
                       during parsing; nil when no errors exist
     :candidates       A vector of candidate commands}"
  [args option-specs command-specs]
  (let [m (cli/parse-opts args option-specs :in-order true)
        cmd (first (:arguments m))
        scmds (set (map first command-specs))
        cands (candidates cmd scmds)
        error (when-not (scmds cmd)
                (str "Unknown command: " (pr-str (or cmd ""))
                     (when (seq cands)
                       (str "\n\n" (candidate-message cands)))))
        errors (if error
                 (conj (or (:errors m) []) error)
                 (:errors m))]
    {:options (:options m)
     :command (keyword (scmds cmd))
     :arguments (vec (drop 1 (:arguments m)))
     :options-summary (:summary m)
     :commands-summary (summarize-cmds command-specs)
     :errors (when (seq errors) errors)
     :candidates cands}))
