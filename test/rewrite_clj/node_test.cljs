(ns rewrite-clj.node-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [rewrite-clj.node :as n]))


(deftest namespaced-keyword
  (is (= ":dill/dall"
         (n/string (n/keyword-node :dill/dall)))))

(deftest funky-keywords
  (is (= ":%dummy.*"
         (n/string (n/keyword-node :%dummy.*)))))
