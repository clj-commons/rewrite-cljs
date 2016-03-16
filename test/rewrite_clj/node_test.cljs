(ns rewrite-clj.node-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [rewrite-clj.node :as n]
            [rewrite-clj.parser :as p]))


(deftest namespaced-keyword
  (is (= ":dill/dall"
         (n/string (n/keyword-node :dill/dall)))))

(deftest funky-keywords
  (is (= ":%dummy.*"
         (n/string (n/keyword-node :%dummy.*)))))

(deftest regex-node
  (let [sample "(re-find #\"(?i)RUN\" s)"
        sample2 "(re-find #\"(?m)^rss\\s+(\\d+)$\")"
        sample3 "(->> (str/split container-name #\"/\"))"]
    (is (= sample (-> sample p/parse-string n/string)))
    (is (= sample2 (-> sample2 p/parse-string n/string)))
    (is (= sample3 (-> sample3 p/parse-string n/string)))))
