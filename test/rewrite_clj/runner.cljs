(ns rewrite-clj.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [rewrite-clj.zip-test]
            [rewrite-clj.paredit-test]
            [rewrite-clj.node-test]
            [rewrite-clj.seqz-test]
            [rewrite-clj.findz-test]
            [rewrite-clj.editz-test]))

(doo-tests 'rewrite-clj.zip-test
           'rewrite-clj.paredit-test
           'rewrite-clj.node-test
           'rewrite-clj.seqz-test
           'rewrite-clj.findz-test
           'rewrite-clj.editz-test)