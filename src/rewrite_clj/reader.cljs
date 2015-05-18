(ns rewrite-clj.reader
  (:refer-clojure :exclude [peek next])
  (:require [cljs.extended.reader :as r]
            [goog.string :as gstring]
            [rewrite-clj.node.protocols :as nd]))




;; TODO: try to get goog.string.format up and running !
(defn throw-reader
  "Throw reader exception, including line/column."
  [reader fmt & data]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw
      (js/Error.
        (str data fmt
             " [at line " l ", column " c "]")))))

(defn boundary?
  [c]
  "Check whether a given char is a token boundary."
  (contains?
    #{\" \: \; \' \@ \^ \` \~
      \( \) \[ \] \{ \} \\ nil}
    c))


(defn whitespace?
  [c]
  (and c
       (or (r/whitespace? c)
           (= c \,))))

(defn linebreak?
  [c]
  (contains? #{\newline \return} c))

(defn space?
  [c]
  (and (whitespace? c)
       (not (linebreak? c))))

(defn whitespace-or-boundary?
  [c]
  (or (whitespace? c) (boundary? c)))


(defn read-while
  "Read while the chars fulfill the given condition. Ignores
   the unmatching char."
  [reader p? & [eof?]]
  (let [buf (gstring/StringBuffer. "")
        eof? (if (nil? eof?)
               (not (p? nil))
               eof?)]
    (loop []
      (if-let [c (r/read-char reader)]
        (if (p? c)
          (do
            (.append buf c)
            (recur))
          (do
            (r/unread reader c)
            (.toString buf)))
        (if eof?
          (.toString buf)
          (throw-reader reader "Unexpected EOF."))))))

(defn read-until
  "Read until a char fulfills the given condition. Ignores the
   matching char."
  [reader p?]
  (read-while
    reader
    (complement p?)
    (p? nil)))

(defn read-include-linebreak
  "Read until linebreak and include it."
  [reader]
  (str
    (read-until
      reader
      #(or (nil? %) (linebreak? %)))
    (r/read-char reader)))

(defn string->edn
  "Convert string to EDN value."
  [s]
  (r/read-string s))

(defn ignore
  "Ignore the next character."
  [reader]
  (r/read-char reader)
  nil)


(defn next
  "Read next char."
  [reader]
  (r/read-char reader))

(defn peek
  "Peek next char."
  [reader]
  (r/peek-char reader))


(defn read-with-meta
  "Use the given function to read value, then attach row/col metadata."
  [reader read-fn]
  (let [row (r/get-line-number reader)
        col (r/get-column-number reader)
        entry (read-fn reader)]
    (when entry
      (let [end-row (r/get-line-number reader)
            end-col (r/get-column-number reader)
            end-col (if (= 0 end-col)
                      (+ col (count (nd/string entry)))
                      end-col)] ; TODO: Figure out why numbers are sometimes whacky
        (with-meta
          entry
          {:row row
           :col col
           :end-row end-row
           :end-col end-col})))))

(defn read-repeatedly
  "Call the given function on the given reader until it returns
   a non-truthy value."
  [reader read-fn]
  (->> (repeatedly #(read-fn reader))
       (take-while identity)
       (doall)))


(defn read-n
  "Call the given function on the given reader until `n` values matching `p?` have been
   collected."
  [reader node-tag read-fn p? n]
  {:pre [(pos? n)]}
  (loop [c 0
         vs []]
    (if (< c n)
      (if-let [v (read-fn reader)]
        (recur
          (if (p? v) (inc c) c)
          (conj vs v))
        (throw-reader
          reader
          "%s node expects %d value%s."
          node-tag
          n
          (if (= n 1) "" "s")))
      vs)))

(defn string-reader
  "Create reader for strings."
  [s]
  (r/indexing-push-back-reader s))




;; (let [form-rdr (r/indexing-push-back-reader "(+ 1 1)")]
;;   (read-include-linebreak form-rdr))

