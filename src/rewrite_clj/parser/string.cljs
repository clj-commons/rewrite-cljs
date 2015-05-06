(ns rewrite-clj.parser.string
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.reader :as u]
            [cljs.extended.reader :as r]
            [goog.string :as gstring]))

(defn- flush-into
  "Flush buffer and add string to the given vector."
  [lines buf]
  (let [s (.toString buf)]
    (.set buf "")
    (conj lines s)))

(defn- read-string-data
  [reader]
  (u/ignore reader)
  (let [buf (gstring/StringBuffer.)]
    (loop [escape? false
           lines []]
      (if-let [c (r/read-char reader)]
        (cond (and (not escape?) (= c \"))
              (flush-into lines buf)

              (= c \newline)
              (recur escape? (flush-into lines buf))

              :else
              (do
                (.append buf c)
                (recur (and (not escape?) (= c \\)) lines)))
        (u/throw-reader reader "Unexpected EOF while reading string.")))))

(defn parse-string
  [reader]
  (node/string-node (read-string-data reader)))

(defn parse-regex
  [reader]
  (let [[h & _] (read-string-data reader)]
    (node/token-node (re-pattern h))))
