(ns rewrite-clj.node-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [rewrite-clj.node :as n]))


(deftest namespaced-keyword
  (is (= ":dill/dall"
         (n/string (n/keyword-node :dill/dall)))))
