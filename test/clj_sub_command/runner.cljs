(ns clj-sub-command.runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [clj-sub-command.core-test]))

(doo-tests 'clj-sub-command.core-test)
