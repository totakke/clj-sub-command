# clj-sub-command

[![Clojars Project](https://img.shields.io/clojars/v/clj-sub-command.svg)](https://clojars.org/clj-sub-command)
[![Build Status](https://travis-ci.org/totakke/clj-sub-command.svg?branch=master)](https://travis-ci.org/totakke/clj-sub-command)
[![Dependency Status](https://www.versioneye.com/user/projects/55e18a52c6d8f2001500037f/badge.svg?style=flat)](https://www.versioneye.com/user/projects/55e18a52c6d8f2001500037f)

A simple sub-command parser for Clojure.

## Installation

clj-sub-command is available as a Maven artifact from [Clojars](https://clojars.org/clj-sub-command).

With Leiningen/Boot:

```clojure
[clj-sub-command "0.3.0"]
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

will return a vector containing five elements:

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

5) and a vector of candidate commands in similarity order:

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

### Candidate commands

The fifth item in the resulting vector is useful for suggesting candidate commands to the user. `candidate-message` generates message of the suggestion.

```clojure
(require '[clj-sub-command.core :refer [sub-command candidate-message]])

(let [[opts cmd args help cands]
      (sub-command args :commands [["cmd1"] ["cmd2"]])]
  (case cmd
    :cmd1 (println "cmd1!")
    :cmd2 (println "cmd2!")
    (println (str "Invalid command. See 'foo --help'.\n\n"
                  (candidate-message cands)))))
```

with args of `["cmd3"]` will print

```
Invalid command. See 'foo --help'.

Did you mean one of these?
        cmd1
        cmd2
```

## Example

I recommend using clj-sub-command with another command-line parser for parsing the rest arguments.
(e.g. [tools.cli](https://github.com/clojure/tools.cli))

```clojure
(ns foo.core
  (:require [clojure.string :as string]
            [clj-sub-command.core :refer [sub-command candidate-message]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

;; "cmd1" command

(def cmd1-options
  [["-v" "--verbose"]
   ["-h" "--help"]])

(defn cmd1-usage [options-summary]
  (->> ["Usage: foo cmd1 [options] file"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn cmd1 [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cmd1-options)]
    (cond
      (:help options) (exit 0 (cmd1-usage summary))
      (not= (count arguments) 1) (exit 1 (cmd1-usage summary))
      errors (exit 1 (error-msg errors)))
    (if (:verbose opt)
      ...)))

;; "cmd2" command

(def cmd2-options ...)
(defn cmd2-usage [options-summary] ...)
(defn cmd2 [args] ...)

;; main

(defn -main [& args]
  (let [[opts cmd args help cands]
        (sub-command args
                     "Usage: foo [-h] {cmd1,cmd2} ..."
                     :options  [["-h" "--help" "Show help" :default false :flag true]]
                     :commands [["cmd1" "Description for cmd1"]
                                ["cmd2" "Description for cmd2"]])]
    (when (:help opts)
      (exit 0 help))
    (case cmd
      :cmd1 (cmd1 args)
      :cmd2 (cmd2 args)
      (exit 1 (str "Invalid command. See 'foo --help'.\n\n"
                   (candidate-message cands))))))
```

## License

Copyright © 2013-2017 Toshiki Takeuchi

Distributed under the Eclipse Public License, the same as Clojure.
