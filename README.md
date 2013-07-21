# clj-sub-command

clj-sub-command is a little library to process a command line sub-command for Clojure.

## Installation

clj-sub-command has not been available on Clojars yet.
Download source and install clj-sub-command to your local repository.

    $ git clone https://github.com/totakke/clj-sub-command.git
    $ cd clj-sub-command
    $ lein install

Then add the following dependency to your project.clj.

    [clj-sub-command "0.1.0-SNAPSHOT"]

## Usage

In your source, use `do-sub-command` macro to parse a sub-command.

    (ns foo.bar
      (:use clj-sub-command.core))

    (defn plus [& args]
      (->> (map #(Integer/parseInt %) args)
           (apply +)))

    (defn prod [& args]
      (->> (map #(Integer/parseInt %) args)
           (apply *)))

    (defn hello [& args] "hello")

    (defn -main [& args]
      (do-sub-command args
        "Usage: cmd {plus,prod} ..."
        [:plus "Plus args" plus]
        [:prod "Multiply args" prod]
        [:else "Hello" hello]))

You can use a sub-command in command line tools.

    $ lein run plus 2 3
    > 5

Help is showed when adding `-h` or `--help` option.

    $ lein run -- -h
    > Usage: cmd {plus,prod} ...
    > Sub-commands
    >   plus  Plus args
    >   prod  Multiply args
    >   else  Hello

## License

Copyright © 2013 Toshiki Takeuchi

Distributed under the Eclipse Public License, the same as Clojure.
