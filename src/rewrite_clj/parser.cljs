(ns rewrite-clj.parser
  (:require [rewrite-clj.parser.core :as p]
            [rewrite-clj.node :as node]
            [rewrite-clj.reader :as reader]
            [clojure.walk :as w]))

;; ## Parser Core

(defn parse
  "Parse next form from the given reader."
  [^not-native reader]
  (p/parse-next reader))

(defn parse-all
  "Parse all forms from the given reader."
  [^not-native reader]
  (let [nodes (->> (repeatedly #(parse reader))
                   (take-while identity)
                   (doall))]
    (with-meta
      (node/forms-node nodes)
      (meta (first nodes)))))

;; ## Specialized Parsers

(defn parse-string
  "Parse first form in the given string."
  [s]
  (parse (reader/string-reader s)))

(defn parse-string-all
  "Parse all forms in the given string."
  [s]
  (parse-all (reader/string-reader s)))


