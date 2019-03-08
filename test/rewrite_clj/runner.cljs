(ns rewrite-clj.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [rewrite-clj.zip-test]
            [rewrite-clj.paredit-test]
            [rewrite-clj.node-test]
            [rewrite-clj.zip.seqz-test]
            [rewrite-clj.zip.findz-test]
            [rewrite-clj.zip.editz-test]))

(doo-tests 'rewrite-clj.zip-test
           'rewrite-clj.paredit-test
           'rewrite-clj.node-test
           'rewrite-clj.zip.seqz-test
           'rewrite-clj.zip.findz-test
           'rewrite-clj.zip.editz-test)
