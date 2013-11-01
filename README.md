# clj-sub-command

clj-sub-command is a simple sub-command parser for Clojure.

## Installation

clj-sub-command is available as a Maven artifact from [Clojars][1].

Latest stable release is version 0.1.0.

For using with leiningen, add the following dependency:

```
[clj-sub-command "0.1.0"]
```

## Usage

clj-sub-command provides two convenient macros: `do-sub-command` and `with-sub-command`.

### do-sub-command

`do-sub-command` parses command-line arguments and calls a corresponded function with the rest of the arguments.

```clojure
(ns calc.core
  (:require [clj-sub-command.core :refer :all]))

(defn plus [& args]
  (reduce + (map #(Integer/parseInt %) args)))

(defn prod [& args]
  (reduce * (map #(Integer/parseInt %) args)))

(defn -main [& args]
  (do-sub-command args
    "Usage: calc [-h] {plus,prod} ..."
    [:plus calc.core/plus "Plus all arguments"] ; [:sub-cmd-name f desc]
    [:prod calc.core/prod "Multiply all arguments"]))
```

### with-sub-command

`with-sub-command` is more flexible.
This binds local options in the same way as [`clojure.contrib.command-line/with-command-line`][2], and this binds a sub-command and the rest arguments.

```clojure
(defn -main [& args]
  (with-sub-command args
    "Usage: calc [-h] [-v] {plus,prod} ..."
    [[version? v? "Print version"]    ; Binds options in the same way as with-command-line.
     [[sub args] [[:plus "Plus args"] ; [:sub-cmd-name desc]
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
>   plus  Plus all arguments
>   prod  Multiply all arguments
```

### Use with another command-line parser

I recommend using clj-sub-command with another command-line parser for parsing the rest arguments.
(e.g. [tools.cli][3])

```clojure
(ns foo.core
  (:require [clj-sub-command.core :refer [do-sub-command]]
            [clojure.tools.cli :refer [cli]]))

(defn f1 [& args]
  (let [[opt _] (cli args
                     "Usage: foo cmd1 [-v] file [file ...]"
                     ["-v" "--[no-]verbose" :default true])]
    (if (:verbose opt)
      ...)))

(defn f2 [& args]
    ...)

(defn -main [& args]
  (do-sub-command args
    "Usage: foo [-h] {cmd1,cmd2} ..."
    [:cmd1 foo.core/f1 "Desc for f1"]
    [:cmd2 foo.core/f2 "Desc for f2"]))
```

## License

Copyright © 2013 Toshiki Takeuchi

Distributed under the Eclipse Public License, the same as Clojure.

[1]: https://clojars.org/clj-sub-command
[2]: http://clojuredocs.org/clojure_contrib/clojure.contrib.command-line/with-command-line
[3]: https://github.com/clojure/tools.cli
