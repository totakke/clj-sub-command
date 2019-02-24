# clj-sub-command

[![Clojars Project](https://img.shields.io/clojars/v/clj-sub-command.svg)](https://clojars.org/clj-sub-command)
[![cljdoc](https://cljdoc.xyz/badge/clj-sub-command)](https://cljdoc.xyz/jump/release/clj-sub-command)
[![Build Status](https://travis-ci.org/totakke/clj-sub-command.svg?branch=master)](https://travis-ci.org/totakke/clj-sub-command)

A simple subcommand parser for Clojure.

## Installation

clj-sub-command is available as a Maven artifact from [Clojars](https://clojars.org/clj-sub-command).

Clojure CLI/deps.edn:

```clojure
clj-sub-command {:mvn/version "0.5.1"}
```

Leiningen/Boot:

```clojure
[clj-sub-command "0.5.1"]
```

## Usage

```clojure
(ns my.program
  (:require [clj-sub-command.core :refer [parse-cmds]])
  (:gen-class))

;; Options before a subcommand
(def options
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help"]])

;; Subcommands and descriptions
(def commands
  [["up" "Start server"]
   ["down" "Stop server"
    :id :stop]])

(defn -main [& args]
  (parse-cmds args options commands))
```

Execute the command line:

```console
$ my-program -p8080 --help up --log-directory /tmp some-file
```

to produce the map:

```clojure
{:options          {:port 8080
                    :help true}
 :command          :up
 :arguments        ["--log-directory" "/tmp" "some-file"]
 :options-summary  "  -p, --port PORT  80  Port number
                      -h, --help"
 :commands-summary "  up    Start server
                      down  Stop server"
 :errors           nil
 :candidates       ["up"]}
```

### Option Specifications

`parse-cmds` uses [tools.cli](https://github.com/clojure/tools.cli) internally
for parsing the options. See tools.cli document for option specifications.

### Options/Commands Summary

`:options-summary` and `:commands-summary` are minimal summary strings of the
options and commands.

Options:

```
  -p, --port PORT  80  Port number
  -h, --help
```

Commands:

```
  up    Start server
  down  Stop server
```

`:options-summary-fn` and `:commands-summary-fn` may be supplied to `parse-cmds`
if the default formatting is unsatisfactory.

### Candidate Commands

`:candidates` vector has near commands in the specifications to the given
command. These candidates are also contained in `:errors` vector as an error
message when the given command is incorrect.

```
Unknown command: "upp"

The most similar command is
        up
```

## Example

Using clj-sub-command with another command-line parser, such as tools.cli, is
recommended for parsing the rest arguments.

```clojure
(ns example.core
  (:require [clojure.string :as string]
            [clj-sub-command.core :refer [parse-cmds]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

;; "up" subcommand

(def up-options
  [["-v" "--verbose"]
   ["-h" "--help"]])

(defn up-usage [options-summary]
  (->> ["Usage: program-name up [options] file"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn up [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args up-options)]
    (cond
      (:help options) (exit 0 (up-usage summary))
      (not= (count arguments) 1) (exit 1 (up-usage summary))
      errors (exit 1 (error-msg errors)))
    (if (:verbose options)
      ...)))

;; "down" subcommand

(def down-options ...)
(defn down-usage [options-summary] ...)
(defn down [args] ...)

;; main

(def options
  [["-h" "--help"]])

(def commands
  [["up" "Start server"]
   ["down" "Stop server"]])

(defn usage [options-summary commands-summary]
  (->> ["Usage: program-name [--help] <command> [<args>]"
        ""
        "Options:"
        options-summary
        ""
        "Commands:"
        commands-summary]
       (string/join \newline)))

(defn -main [& args]
  (let [{:keys [options command arguments errors options-summary commands-summary]}
        (parse-cmds args options commands)]
    (cond
      (:help options) (exit 0 (usage options-summary commands-summary))
      errors (exit 1 (error-msg errors)))
    (case command
      :up   (up arguments)
      :down (down arguments))))
```

## License

Copyright © 2013-2019 Toshiki Takeuchi

Distributed under the [Eclipse Public License](LICENSE), the same as Clojure.
