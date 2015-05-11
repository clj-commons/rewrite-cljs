(ns rewrite-clj.editz-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip.editz :as e]))



(deftest splice
  (is (= "[1 2 [3 4]]" (-> "[[1 2] [3 4]]"
                           z/of-string
                           z/down
                           e/splice
                           z/root-string))))
