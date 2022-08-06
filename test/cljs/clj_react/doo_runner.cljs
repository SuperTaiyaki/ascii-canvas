(ns clj-react.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [clj-react.graphics-test]))

(doo-tests 'clj-react.graphics-test)
