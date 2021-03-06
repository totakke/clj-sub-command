(ns clj-sub-command.core
  "A simple subcommand parser for Clojure."
  (:require [clojure.string :as s]
            [clojure.tools.cli :as cli]
            #?(:cljs goog.string.format)))

(def ^:dynamic *max-normalized-levenshtein-distance*
  "Max value of normalized levenshtein distance used in searching candidate
  commands. The default value is 0.5."
  0.5)

(defn- make-format
  [lens]
  (s/join (map #(str "  %" (if-not (zero? %) (str "-" %)) "s") lens)))

(defn- build-option-doc [{:keys [switches docs default]}]
  [(apply str (interpose ", " switches))
   (or (str default) "")
   (or docs "")])

(defn- build-command-doc [{:keys [command docs]}]
  [(name command)
   (or docs "")])

#?(:cljs
   (defn format
     [fmt & args]
     (apply goog.string.format fmt args)))

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
      (let [fmt (make-format (take-nth 2 v))]
        (print (apply format fmt (take-nth 2 (rest v)))))
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
      (let [fmt (make-format (take-nth 2 v))]
        (print (apply format fmt (take-nth 2 (rest v)))))
      (prn))))

(defn- banner-for [desc options commands]
  (when desc
    (println desc)
    (println))
  (banner-for-options options)
  (println)
  (banner-for-commands commands))

(defn- name-for [k]
  (s/replace k #"^--no-|^--\[no-\]|^--|^-" ""))

(defn- flag-for [^String v]
  (not (s/starts-with? v "--no-")))

(defn- opt? [^String x]
  (s/starts-with? x "-"))

(defn- flag? [^String x]
  (s/starts-with? x "--[no-]"))

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
          (throw #?(:clj (Exception. (str "'" opt "' is not a valid argument"))
                    :cljs (js/Error. (str "'" opt "' is not a valid argument"))))

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
          (and flag (flag? s))
          [(s/replace s #"\[no-\]" "no-") (s/replace s #"\[no-\]" "")]

          (and flag (s/starts-with? s "--"))
          [(s/replace s #"--" "--no-") s]

          :default
          [s]))
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
                               (- j 1)) 1)))}}))
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
         (s/join \newline))))

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

(defn- compile-command-spec
  [command-spec]
  (let [[cmd desc & {:keys [id]}] command-spec]
    {:id (or id (keyword cmd))
     :cmd cmd
     :desc desc}))

(defn- compile-command-specs
  [command-specs]
  (map compile-command-spec command-specs))

(defn summarize-cmds
  "Reduces subcommands specs into a subcommands summary for printing at a
  terminal."
  [command-specs]
  (if (seq command-specs)
    (let [parts (map (juxt :cmd :desc) command-specs)
          lens (apply map (fn [& cols]
                            (apply max (map count cols))) parts)
          fmt (make-format lens)]
      (->> parts
           (map #(s/trimr (apply format fmt %)))
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
     :candidates       A vector of candidate commands}

  A few function options may be specified to influence the behavior of
  parse-cmds:

    :strict               Parse required option arguments strictly: if a
                          required argument value matches any other option, it
                          is considered to be missing (and you have a parse
                          error).

    :allow-empty-command  Allow an empty command if true. By default, the empty
                          command causes \"Unknown command\" error.

    :options-summary-fn   A function that receives the sequence of compiled
                          option specs, and returns a custom option summary
                          string.

    :commands-summary-fn  A function that receives the sequence of compiled
                          command specs, and returns a custom command summary
                          string."
  [args option-specs command-specs & options]
  (let [{:keys [strict allow-empty-command options-summary-fn
                commands-summary-fn]} (apply hash-map options)
        m (cli/parse-opts args option-specs
                          :in-order true
                          :strict strict
                          :summary-fn options-summary-fn)
        cmd (first (:arguments m))
        command-specs (compile-command-specs command-specs)
        scmds (set (map :cmd command-specs))
        scmd (scmds cmd)
        cands (candidates cmd scmds)
        error (when-not (or scmd (and allow-empty-command (empty? cmd)))
                (str "Unknown command: " (pr-str (or cmd ""))
                     (when (seq cands)
                       (str "\n\n" (candidate-message cands)))))
        errors (if error
                 (conj (or (:errors m) []) error)
                 (:errors m))]
    {:options (:options m)
     :command (:id (first (filter #(= (:cmd %) scmd) command-specs)))
     :arguments (vec (drop 1 (:arguments m)))
     :options-summary (:summary m)
     :commands-summary ((or commands-summary-fn summarize-cmds) command-specs)
     :errors (when (seq errors) errors)
     :candidates cands}))
