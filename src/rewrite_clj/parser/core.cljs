(ns rewrite-clj.parser.core
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.reader :as reader]
            [rewrite-clj.parser.keyword :refer [parse-keyword]]
            [rewrite-clj.parser.string :refer [parse-string parse-regex]]
            [rewrite-clj.parser.token :refer [parse-token]]
            [rewrite-clj.parser.whitespace :refer [parse-whitespace]]))

;; ## Base Parser

(def ^:dynamic ^:private *delimiter*
  nil)


(declare parse-next)


(defn- parse-delim
  [reader delimiter]
  (reader/ignore reader)
  (->> #(binding [*delimiter* delimiter]
          (parse-next %))
       (reader/read-repeatedly reader)))

(defn- parse-printables
  [reader node-tag n & [ignore?]]
  (when ignore?
    (reader/ignore reader))
  (reader/read-n
    reader
    node-tag
    parse-next
    (complement node/printable-only?)
    n))


(defn- parse-meta
  [reader]
  (reader/ignore reader)
  (node/meta-node (parse-printables reader :meta 2)))


(defn- parse-eof
  [reader]
  (when *delimiter*
    (reader/throw-reader reader "Unexpected EOF.")))

;; ### Seqs

(defn- parse-list
  [reader]
  (node/list-node (parse-delim reader \))))

(defn- parse-vector
  [reader]
  (node/vector-node (parse-delim reader \])))

(defn- parse-map
  [reader]
  (node/map-node (parse-delim reader \})))


;; ### Reader Specialities

(defn- parse-sharp
  [reader]
  (reader/ignore reader)
  (case (reader/peek reader)
    nil (reader/throw-reader reader "Unexpected EOF.")
    \{ (node/set-node (parse-delim reader \}))
    \( (node/fn-node (parse-delim reader \)))
    \" (parse-regex reader)
    \^ (node/raw-meta-node (parse-printables reader :meta 2 true))
    \' (node/var-node (parse-printables reader :var 1 true))
    \= (node/eval-node (parse-printables reader :eval 1 true))
    \_ (node/uneval-node (parse-printables reader :uneval 1 true))
    (node/reader-macro-node (parse-printables reader :reader-macro 2))))

(defn- parse-unmatched
  [reader]
  (reader/throw-reader
    reader
    "Unmatched delimiter: %s"
    (reader/peek reader)))


(defn- parse-deref
  [reader]
  (node/deref-node (parse-printables reader :deref 1 true)))

;; ## Quotes

(defn- parse-quote
  [reader]
  (node/quote-node (parse-printables reader :quote 1 true)))

(defn- parse-syntax-quote
  [reader]
  (node/syntax-quote-node (parse-printables reader :syntax-quote 1 true)))

(defn- parse-unquote
  [reader]
  (reader/ignore reader)
  (let [c (reader/peek reader)]
    (if (= c \@)
      (node/unquote-splicing-node
        (parse-printables reader :unquote 1 true))
      (node/unquote-node
        (parse-printables reader :unquote 1)))))

(defn- parse-comment
  [reader]
  (reader/ignore reader)
  (node/comment-node (reader/read-include-linebreak reader)))


(def dispatch-map
  {\^ parse-meta      \# parse-sharp
   \( parse-list      \[ parse-vector    \{ parse-map
   \} parse-unmatched \] parse-unmatched \) parse-unmatched
   \~ parse-unquote   \' parse-quote     \` parse-syntax-quote
   \; parse-comment   \@ parse-deref     \" parse-string
   \: parse-keyword})

(defn- dispatch
  [c]
  (cond (nil? c)               parse-eof
        (reader/whitespace? c) parse-whitespace
        (= c *delimiter*)      reader/ignore
        :else (get dispatch-map c parse-token)))


(defn parse-next*
  []
  (comp dispatch reader/peek))

(defn parse-next
  [reader]
  (reader/read-with-meta reader ((parse-next*) reader)))
