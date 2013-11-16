(ns ^{:author "Toshiki Takeuchi",
      :doc "A simple sub-command parser for Clojure."}
  clj-sub-command.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? join replace]]
            [clojure.pprint :refer [pprint cl-format]]))

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
                   [["--------" "-------" "----"]
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
                   [["--------" "----"]
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

(defn sub-command
  [args & specs]
  (let [[desc {:keys [options commands]}] (if (string? (first specs))
                                            [(first specs) (rest specs)]
                                            [nil specs])
        options (map generate-option options)
        commands (map generate-command commands)
        [optargs [cmdarg & cmdspecs]] (group-by-optargs args options)
        banner (with-out-str (banner-for desc options commands))
        [options _] (apply-options options optargs)
        command (command-for cmdarg commands)]
    [options command banner]))
