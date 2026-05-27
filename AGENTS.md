# clj-sub-command Development Guide for AI Agents

This file provides guidance to AI agents when working with code in this repository.

## Commands

Build tool is Leiningen.

- Run Clojure tests (default profile): `lein test`
- Run the full Clojure version matrix used in CI: `lein with-profile +dev:+1.8:+1.9:+1.10:+1.11 test`
- Run a single Clojure test namespace: `lein test clj-sub-command.core-test`
- Run a single test var: `lein test :only clj-sub-command.core-test/<test-name>`
- Run ClojureScript tests (Node, single run): `lein doo node test once`
- Run cljs tests in watch mode: `lein doo node test`
- REPL: `lein repl`

CI runs both `lein with-profile +dev:+1.8:+1.9:+1.10:+1.11 test` and
`lein doo node test once` on every push (`.github/workflows/build.yml`).

## Architecture

This is a small Clojure/ClojureScript library that adds subcommand parsing on
top of `clojure.tools.cli`. The entire implementation lives in a single `.cljc`
file:

- `src/clj_sub_command/core.cljc` — public API and all internal helpers, with `#?(:clj ...)` / `:cljs` reader conditionals where the host differs (notably `format`, which uses `goog.string.format` on cljs).
- `test/clj_sub_command/core_test.cljc` — shared test suite for both runtimes.
- `test/clj_sub_command/runner.cljs` — lein-doo entry point that loads `core-test` for the cljs run.

Two public entry points coexist:

- `parse-cmds` (current). Delegates global option parsing to `clojure.tools.cli/parse-opts` with `:in-order true`, then treats the first remaining argument as the subcommand. Returns a map with `:options`, `:command`, `:arguments`, `:options-summary`, `:commands-summary`, `:errors`, `:candidates`. The `:command` value is the `:id` from the command spec (defaults to the keyword of the command name).
- `sub-command` (legacy, kept for backward compatibility — marked for possible future deprecation in its docstring). Has its own option/command compilers (`generate-option`, `generate-command`, `apply-options`, `banner-for-*`) that do not go through tools.cli. New code should use `parse-cmds`.

When an unknown subcommand is given, `candidates` ranks the known commands by
normalized Levenshtein distance and keeps those within
`*max-normalized-levenshtein-distance*` (dynamic var, default 0.5).
`candidate-message` formats the "The most similar command(s) is/are…" suggestion
that gets appended to the `:errors` entry.

The library targets both Clojure (1.8 through current) and ClojureScript; keep
`core.cljc` host-agnostic and add reader conditionals only where the JVM/JS
split is unavoidable.
