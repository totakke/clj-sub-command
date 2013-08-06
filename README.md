# clj-sub-command

clj-sub-command is a simple sub-command parser for Clojure.

## Installation

clj-sub-command is available from Clojars classic repository.

    :repositories [["clojars classic" "http://clojars.org/repo/"]]
    :dependencies [[clj-sub-command "0.1.0-SNAPSHOT"]]

## Usage

clj-sub-command provides two convenient macros: `do-sub-command` and `with-sub-command`.

### do-sub-command

`do-sub-command` parses command-line arguments and calls a corresponded function with the rest of the arguments.

```clojure
(ns calc.core
  (:use clj-sub-command.core))

(defn plus [& args]
  (reduce + (map #(Integer/parseInt %) args)))

(defn prod [& args]
  (reduce * (map #(Integer/parseInt %) args)))

(defn -main [& args]
  (do-sub-command args
    "Usage: calc [-h] {plus,prod} ..."
    [:plus calc.core/plus "Plus args"]       ; [:sub-command-name function description]
    [:prod calc.core/prod "Multiply args"]))
```

### with-sub-command

`with-sub-command` is more flexible.
This binds local options and a sub-command and arguments of the sub-command to command-line arguments.

```clojure
(defn -main [& args]
  (with-sub-command args
    "Usage: calc [-h] [-v] {plus,prod} ..."
    [[version? v? "Print version"]           ; Binds options in the same way as with-command-line.
     [[sub args] [[:plus "Plus args"]        ; [:sub-command-name description]
                  [:prod "Multiply args"]]]]
    (if version?
      (println "0.1.0-SNAPSHOT")
      (condp = sub
        :plus (apply plus args)
        :prod (apply prod args)))))
```

### Run with leiningen

```bash
$ lein run plus 2 3
> 5
```

Help is showed when adding `-h` or `--help` option.

```bash
$ lein run -- -h
> Usage: calc [-h] [-v] {plus,prod} ...
> Options
>   --version, -v  Print version
> Sub-commands
>   plus  Plus args
>   prod  Multiply args
```

## License

Copyright © 2013 Toshiki Takeuchi

Distributed under the Eclipse Public License, the same as Clojure.
