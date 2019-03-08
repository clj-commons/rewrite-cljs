(ns rewrite-clj.zip.editz-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip.editz :as e]))



(deftest splice
  (is (= "[1 2 [3 4]]" (-> "[[1 2] [3 4]]"
                           z/of-string
                           z/down
                           e/splice
                           z/root-string))))
