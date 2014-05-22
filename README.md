# clj-sub-command

clj-sub-command is a simple sub-command parser for Clojure.

## Installation

clj-sub-command is available as a Maven artifact from [Clojars][1].

Latest stable release is version 0.2.0.

For using with leiningen, add the following dependency:

```
[clj-sub-command "0.2.0"]
```

## Usage

```clojure
(require '[clj-sub-command.core :refer [sub-command]])

(sub-command args
             "Usage: cmd [-v] {cmd1,cmd2} ..."
             :options  [["-p" "--port" "Listen on this port" :parse-fn #(Integer. %)]
                        ["--host" "The hostname" :default "localhost"]
                        ["-v" "--[no-]verbose" :default true]]
             :commands [["cmd1" "Description for cmd1"]
                        ["cmd2" "Description for cmd2"]])
```

with args of:

```clojure
["-p" 8080
 "--no-verbose"
 "cmd1" "--log-directory" "/tmp" "some-file"]
```

will return a vector containing four elements:

1) a map with the option names picked out for you as keywords:

```clojure
{:port    8080
 :host    "localhost"
 :verbose false}
```

2) a keyword to indicate the selected sub command:

```clojure
:cmd1
```

3) a vector of the rest arguments:

```clojure
["--log-directory" "/tmp" "some-file"]
```

4) a documentation string to use to provide help:

```clojure
Usage: cmd [-v] {cmd1,cmd2} ...

 Options                      Default    Desc
 -------                      -------    ----
 -p, --port                              Listen on this port
 --host                       localhost  The hostname
 -v, --no-verbose, --verbose  true

 Command   Desc
 -------   ----
 cmd1      Description for cmd1
 cmd2      Description for cmd2

```

5) a vector of candidate commands in similarity order:

```clojure
["cmd1" "cmd2"]
```

### Help document

The fourth item in the resulting vector is a banner useful for providing help to the user:

```clojure
(let [[opts cmd args help cands]
      (sub-command args
                   :options  [["-h" "--help" "Show help" :default false :flag true]]
                   :commands [["cmd1"] ["cmd2"]])]
  (when (:help opts)
    (println help)
    (System/exit 0))
  ...)
```

### Example

I recommend using clj-sub-command with another command-line parser for parsing the rest arguments.
(e.g. [tools.cli][2])

```clojure
(ns foo.core
  (:require [clojure.string :as str]
            [clj-sub-command.core :refer [sub-command]]
            [clojure.tools.cli :refer [cli]]))

(defn f1 [args]
  (let [[opt _] (cli args
                     "Usage: foo cmd1 [-v] file [file ...]"
                     ["-v" "--[no-]verbose" :default true])]
    (if (:verbose opt)
      ...)))

(defn f2 [args]
  ...)

(defn -main [& args]
  (let [[opts cmd args help cands]
        (sub-command args
                     "Usage: foo [-h] {cmd1,cmd2} ..."
                     :options  [["-h" "--help" "Show help" :default false :flag true]]
                     :commands [["cmd1" "Description for cmd1"]
                                ["cmd2" "Description for cmd2"]])]
    (when (:help opts)
      (println help)
      (System/exit 0))
    (case cmd
      :cmd1 (f1 args)
      :cmd2 (f2 args)
      (do (println (str "Invalid command. See 'foo --help'."))
          (when (seq cands)
            (newline)
            (println "Did you mean one of these?")
            (doseq [c cands]
              (println "       " c)))))))
```

## License

Copyright © 2013 Toshiki Takeuchi

Distributed under the Eclipse Public License, the same as Clojure.

[1]: https://clojars.org/clj-sub-command
[2]: https://github.com/clojure/tools.cli
