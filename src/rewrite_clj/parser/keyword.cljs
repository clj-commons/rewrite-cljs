(ns rewrite-clj.parser.keyword
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.reader :as u]
            [cljs.extended.reader :as r]))

(defn parse-keyword
  [reader]
  (u/next reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (node/keyword-node
        (r/read-keyword reader ":")
        true)
      (do
        (r/unread reader \:)
        (node/keyword-node (r/read-keyword reader ":"))))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))
