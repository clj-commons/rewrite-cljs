;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cljs.extended.reader
  (:require [goog.string :as gstring]
            [clojure.string :as string]))



(def specials '#{if def fn* do let* loop* letfn* throw try* recur new set! ns deftype* defrecord* . js* & quote})

(def symbol-pattern (re-pattern "^[:]?([^0-9/].*/)?([^0-9/][^/]*)$"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reader protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defprotocol Reader
  (read-char [reader] "Returns the next char from the Reader, nil if the end of stream has been reached")
  (peek-char [reader] "Returns the next char from the Reader without removing it from the reader stream"))

(defprotocol IPushbackReader
  (unread [reader ch] "Push back a single character on to the stream"))

(defprotocol IndexingReader
  (get-line-number [reader])
  (get-column-number [reader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reader deftypes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype StringReader
    [s s-len ^:mutable s-pos]
  Reader
  (read-char [reader]
    (when (> s-len s-pos)
      (let [r (aget s s-pos)]
        (set! s-pos (inc s-pos))
        r)))
  (peek-char [reader]
    (when (> s-len s-pos)
      (aget s s-pos))))

(deftype PushbackReader
    [rdr buf buf-len ^:mutable buf-pos]
  Reader
  (read-char [reader]
    (if (< buf-pos buf-len)
      (let [r (aget buf buf-pos)]
        (set! buf-pos (inc buf-pos))
        r)
      (read-char rdr)))
  (peek-char [reader]
    (if (< buf-pos buf-len)
      (aget buf buf-pos)
      (peek-char rdr)))
  IPushbackReader
  (unread [reader ch]
    (when ch
      (if (zero? buf-pos)
        (throw (js/Error. "Pushback buffer is full")))
      (set! buf-pos (dec buf-pos))
      (aset buf buf-pos ch))))

(declare newline?)

(defn- normalize-newline [rdr ch]
  (if (identical? \return ch)
    (let [c (peek-char rdr)]
      (when (identical? \formfeed c)
        (read-char rdr))
      \newline)
    ch))

(deftype IndexingPushbackReader
    [rdr ^:mutable line ^:mutable column
     ^:mutable line-start? ^:mutable prev]
  Reader
  (read-char [reader]
    (when-let [ch (read-char rdr)]
      (let [ch (normalize-newline rdr ch)]
        (set! prev line-start?)
        (set! line-start? (newline? ch))
        (when line-start?
          (set! column 0)
          (set! line (inc line)))
        (set! column (inc column))
        ch)))

  (peek-char [reader]
    (peek-char rdr))

  IPushbackReader
  (unread [reader ch]
    (when line-start? (set! line (dec line)))
    (set! line-start? prev)
    (set! column (dec column))
    (unread rdr ch))

  IndexingReader
  (get-line-number [reader] (inc line))
  (get-column-number [reader]  column))



;;;;;;;;;;;;;;;;;;;;;;;;;
;; Source logging support
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-meta
  "Returns an object of the same type and value as `obj`, with its metadata merged over `m`."
  [obj m]
  (let [orig-meta (meta obj)]
    (with-meta obj (merge m (dissoc orig-meta :source)))))

(defn- peek-source-log
  "Returns a string containing the contents of the top most source logging frame."
  [source-log-frames]
  (let [current-frame @source-log-frames]
    (.substring (.toString (:buffer current-frame)) (:offset current-frame))))


(defn- log-source-char
  "Logs `char` to all currently active source logging frames."
  [source-log-frames char]
  (when-let [buffer (:buffer @source-log-frames)]
    (.append buffer char)))


(defn- drop-last-logged-char
  "Removes the last logged character from all currently active source logging frames. Called when pushing a character back."
  [source-log-frames]
  (when-let [buffer (:buffer @source-log-frames)]
    (let [s (.toString buffer)]
      (.set buffer (.substring s 0 (dec (.getLength buffer)))))))



(deftype SourceLoggingPushbackReader
    [rdr ^:mutable line ^:mutable column
     ^:mutable line-start? ^:mutable prev
    source-log-frames]
  Reader
  (read-char [reader]
    (when-let [ch (read-char rdr)]
      (let [ch (normalize-newline rdr ch)]
        (set! prev line-start?)
        (set! line-start? (newline? ch))
        (when line-start?
          ;(set! prev-column column)
          (set! column 0)
          (set! line (inc line)))
        (set! column (inc column))
        (log-source-char source-log-frames ch)
        ch)))

  (peek-char [reader]
    (peek-char rdr))

  IPushbackReader
  (unread [reader ch]
    (if line-start?
      (do (set! line (dec line))
          ;(set! column prev-column)
        )
      (set! column (dec column)))
    (set! line-start? prev)
    (when ch
      (drop-last-logged-char source-log-frames))
    (unread rdr ch))

  IndexingReader
  (get-line-number [reader] (int line))
  (get-column-number [reader] (int column)))



;; Utility functions for instatiating


(defn string-reader
  "Creates a StringReader from a given string"
  ([s]
     (StringReader. s (count s) 0)))

(defn string-push-back-reader
  "Creates a PushbackReader from a given string"
  ([s]
     (string-push-back-reader s 1))
  ([s buf-len]
     (PushbackReader. (string-reader s) (object-array buf-len) buf-len buf-len)))

(defn indexing-push-back-reader
  "Creates an IndexingPushbackReader from a given string"
  ([s]
     (IndexingPushbackReader.
      (string-push-back-reader s) 0 1 true nil))
  ([s buf-len]
     (IndexingPushbackReader.
      (string-push-back-reader s buf-len) 0 1 true nil)))


(defn source-logging-push-back-reader
  "Creates a SourceLoggingPushbackReader from a given string or PushbackReader"
  ([s-or-rdr]
     (source-logging-push-back-reader s-or-rdr 1))
  ([s-or-rdr buf-len]
     (source-logging-push-back-reader s-or-rdr buf-len nil))
  ([s-or-rdr buf-len file-name]
     (SourceLoggingPushbackReader.
      (if (string? s-or-rdr) (string-push-back-reader s-or-rdr buf-len) s-or-rdr)
      1
      1
      true
      nil
      (atom {:buffer (gstring/StringBuffer.)
             :offset 0})
;;       (doto (make-var)
;;         (alter-var-root (constantly {:buffer (gstring/StringBuffer.)
;;                                      :offset 0})))
      )))








;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def js-whitespaces
  #js [\return \newline \tab \space ","])

(defn- ^boolean whitespace?
  "Checks whether a given character is whitespace"
  [ch]
  ;(or (gstring/isBreakingWhitespace ch) (identical? \, ch))
  (< -1 (.indexOf js-whitespaces ch)))

(defn- ^boolean numeric?
  "Checks whether a given character is numeric"
  [ch]
  (gstring/isNumeric ch))

(defn- ^boolean newline?
  "Checks whether the character is a newline."
  [ch]
  (identical? "\n" ch))

(defn- ^boolean comment-prefix?
  "Checks whether the character begins a comment."
  [ch]
  (identical? \; ch))

(defn- ^boolean number-literal?
  "Checks whether the reader is at the start of a number literal"
  [reader initch]
  (or (numeric? initch)
      (and (or (identical? \+ initch) (identical? \- initch))
           (numeric? (peek-char reader)))))

(declare read macros dispatch-macros)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; read helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: use ex-info
(defn reader-error
  [rdr & msg]
  (let [error-msg (apply str msg)]
    (throw (js/Error. (str error-msg (when (satisfies? cljs.extended.reader.IndexingReader rdr)
                                       (str ", on line: " (get-line-number rdr)
                                            ", on column: " (get-column-number rdr))))))))

(defn ^boolean macro-terminating? [ch]
  (and (not (identical? ch "#"))
       (not (identical? ch \'))
       (not (identical? ch ":"))
       (macros ch)))

(defn read-token
  [rdr initch]
  (loop [sb (gstring/StringBuffer. initch)
         ch (read-char rdr)]
    (if (or (nil? ch)
            (whitespace? ch)
            (macro-terminating? ch))
      (do (unread rdr ch) (. sb (toString)))
      (recur (do (.append sb ch) sb) (read-char rdr)))))

(defn read-line
  "Reads to the end of a line and returns the line."
  [rdr]
  (loop [sb (gstring/StringBuffer.)
         ch (read-char rdr)]
    (cond
      (and (nil? ch) (= 0 (.getLength sb)))
      nil

      (or (identical? ch "\n") (identical? ch "\r") (nil? ch))
      (. sb (toString))

      :else
      (recur (do (.append sb ch) sb) (read-char rdr)))))

(defn line-seq [rdr]
  "Returns the lines of text from rdr as a lazy sequence of strings."
  (when-let [line (read-line rdr)]
    (cons line (lazy-seq (line-seq rdr)))))

(defn skip-line
  "Advances the reader to the end of a line. Returns the reader"
  [reader _]
  (loop []
    (let [ch (read-char reader)]
      (if (or (identical? ch "\n") (identical? ch "\r") (nil? ch))
        reader
        (recur)))))

;; Note: Input begin and end matchers are used in a pattern since otherwise
;; anything begininng with `0` will match just `0` cause it's listed first.
(def int-pattern (re-pattern "^([-+]?)(?:(0)|([1-9][0-9]*)|0[xX]([0-9A-Fa-f]+)|0([0-7]+)|([1-9][0-9]?)[rR]([0-9A-Za-z]+)|0[0-9]+)(N)?$"))
(def ratio-pattern (re-pattern "([-+]?[0-9]+)/([0-9]+)"))
(def float-pattern (re-pattern "([-+]?[0-9]+(\\.[0-9]*)?([eE][-+]?[0-9]+)?)(M)?"))

(defn- re-find*
  [re s]
  (let [matches (.exec re s)]
    (when-not (nil? matches)
      (if (== (alength matches) 1)
        (aget matches 0)
        matches))))

(defn- match-int
  [s]
  (let [groups (re-find* int-pattern s)
        group3 (aget groups 2)]
    (if-not (or (nil? group3)
                (< (alength group3) 1))
      0
      (let [negate (if (identical? "-" (aget groups 1)) -1 1)
            a (cond
               (aget groups 3) (array (aget groups 3) 10)
               (aget groups 4) (array (aget groups 4) 16)
               (aget groups 5) (array (aget groups 5) 8)
               (aget groups 7) (array (aget groups 7) (js/parseInt (aget groups 7)))
               :default (array nil nil))
            n (aget a 0)
            radix (aget a 1)]
        (if (nil? n)
          nil
          (* negate (js/parseInt n radix)))))))


(defn- match-ratio
  [s]
  (let [groups (re-find* ratio-pattern s)
        numinator (aget groups 1)
        denominator (aget groups 2)]
    (/ (js/parseInt numinator) (js/parseInt denominator))))

(defn- match-float
  [s]
  (js/parseFloat s))

(defn- re-matches*
  [re s]
  (let [matches (.exec re s)]
    (when (and (not (nil? matches))
               (identical? (aget matches 0) s))
      (if (== (alength matches) 1)
        (aget matches 0)
        matches))))



(defn- match-number
  [s]
  (cond
   (re-matches* int-pattern s) (match-int s)
   (re-matches* ratio-pattern s) (match-ratio s)
   (re-matches* float-pattern s) (match-float s)))

(defn escape-char-map [c]
  (cond
   (identical? c \t) "\t"
   (identical? c \r) "\r"
   (identical? c \n) "\n"
   (identical? c \\) \\
   (identical? c \") \"
   (identical? c \b) "\b"
   (identical? c \f) "\f"
   :else nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; unicode
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-2-chars [reader]
  (.toString
    (gstring/StringBuffer.
      (read-char reader)
      (read-char reader))))

(defn read-4-chars [reader]
  (.toString
    (gstring/StringBuffer.
      (read-char reader)
      (read-char reader)
      (read-char reader)
      (read-char reader))))

(def unicode-2-pattern (re-pattern "[0-9A-Fa-f]{2}"))
(def octal-pattern (re-pattern "[0-7]{1,3}"))
(def unicode-4-pattern (re-pattern "[0-9A-Fa-f]{4}"))

(defn validate-unicode-escape [unicode-pattern reader escape-char unicode-str]
  (if (re-matches unicode-pattern unicode-str)
    unicode-str
    (reader-error reader "Unexpected unicode escape \\" escape-char unicode-str)))

(defn make-unicode-char [code-str & [base]]
    (let [base (or base 16)
          code (js/parseInt code-str base)]
      (.fromCharCode js/String code)))

(defn escape-char
  [buffer reader]
  (let [ch (read-char reader)
        mapresult (escape-char-map ch)]
    (if mapresult
      mapresult
      (cond
        (identical? ch \x)
        (->> (read-2-chars reader)
          (validate-unicode-escape unicode-2-pattern reader ch)
          (make-unicode-char))

        (identical? ch \u)
        (->> (read-4-chars reader)
          (validate-unicode-escape unicode-4-pattern reader ch)
          (make-unicode-char))

        (numeric? ch)
        (.fromCharCode js/String ch)

        :else
        (reader-error reader "Unexpected unicode escape \\" ch )))))

(defn read-past
  "Read until first character that doesn't match pred, returning
   char."
  [pred rdr]
  (loop [ch (read-char rdr)]
    (if (pred ch)
      (recur (read-char rdr))
      ch)))

(defn read-delimited-list
  [delim rdr recursive?]
  (loop [a (transient [])]
    (let [ch (read-past whitespace? rdr)]
      (when-not ch
        (reader-error rdr "EOF while reading"))
      (if (identical? delim ch)
        (persistent! a)
        (if-let [macrofn (macros ch)]
          (let [mret (macrofn rdr ch)]
            (recur (if (identical? mret rdr) a (conj! a mret))))
          (do
            (unread rdr ch)
            (let [o (read rdr true nil recursive?)]
              (recur (if (identical? o rdr) a (conj! a o))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data structure readers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn not-implemented
  [rdr ch]
  (reader-error rdr "Reader for " ch " not implemented yet"))

(declare maybe-read-tagged-type)

(defn read-dispatch
  [rdr _]
  (let [ch (read-char rdr)
        dm (dispatch-macros ch)]
    (if dm
      (dm rdr _)
      (if-let [obj (maybe-read-tagged-type rdr ch)]
        obj
        (reader-error rdr "No dispatch macro for " ch)))))

(defn read-unmatched-delimiter
  [rdr ch]
  (reader-error rdr "Unmatched delimiter " ch))


(defn maybe-get-pos [rdr]
  (when (satisfies? cljs.extended.reader.IndexingReader rdr)
    {:line (get-line-number rdr)
     :column (dec (get-column-number rdr))}))

(defn prep-meta [rdr pos source]
  (when pos
    (merge pos
           {:end-line (get-line-number rdr)
            :end-column (get-column-number rdr)
            :source source})))

(defn with-source-log [rdr f prefix]
  (if (= cljs.extended.reader/SourceLoggingPushbackReader (type rdr))
    (let [frame (.-source-log-frames rdr)
          new-frame (atom (assoc-in @frame [:offset] (.getLength (:buffer @frame))))
          ret (f)
          source (str prefix (peek-source-log new-frame))]
      [ret source])
    [(f) nil]))


(defn read-list
  [rdr _]
  (let [pos (maybe-get-pos rdr)
        [the-list source] (with-source-log rdr #(read-delimited-list \) rdr true) "(")]
    (with-meta (apply list the-list)
      (prep-meta rdr pos source))))


(def read-comment skip-line)

(defn read-vector
  [rdr _]
  (let [pos (maybe-get-pos rdr)
        [the-vec source] (with-source-log rdr #(read-delimited-list "]" rdr true) "[")]
    (with-meta (identity the-vec)
      (prep-meta rdr pos source))))

(defn read-map
  [rdr _]
  (let [pos (maybe-get-pos rdr)
        [l source] (with-source-log rdr #(read-delimited-list "}" rdr true) "{")]
    (when (odd? (count l))
      (reader-error rdr "Map literal must contain an even number of forms"))
    (with-meta
      (apply hash-map l)
      (prep-meta rdr pos source))))


(defn read-number
  [reader initch]
  (loop [buffer (gstring/StringBuffer. initch)
         ch (read-char reader)]
    (if (or (nil? ch) (whitespace? ch) (macros ch))
      (do
        (unread reader ch)
        (let [s (. buffer (toString))]
          (or (match-number s)
              (reader-error reader "Invalid number format [" s "]"))))
      (recur (do (.append buffer ch) buffer) (read-char reader)))))

(defn read-string*
  [reader _]
  (loop [buffer (gstring/StringBuffer.)
         ch (read-char reader)]
    (cond
     (nil? ch) (reader-error reader "EOF while reading")
     (identical? "\\" ch) (recur (do (.append buffer (escape-char buffer reader)) buffer)
                        (read-char reader))
     (identical? \" ch) (. buffer (toString))
     :default (recur (do (.append buffer ch) buffer) (read-char reader)))))

(defn special-symbols [t not-found]
  (cond
   (identical? t "nil") nil
   (identical? t "true") true
   (identical? t "false") false
   :else not-found))


(defn parse-symbol [token]
  (if (gstring/contains token "/")
      (symbol (subs token 0 (.indexOf token "/"))
              (subs token (inc (.indexOf token "/")) (.-length token)))
      (special-symbols token (symbol token))))

(defn read-symbol
  [reader initch]
  (let [pos (maybe-get-pos reader)
        [token source] (with-source-log reader #(read-token reader initch) initch)]
    (if (satisfies? IWithMeta token)
      (with-meta (parse-symbol token)
        (prep-meta reader pos source))
      (parse-symbol token))))
;;     (if (gstring/contains token "/")
;;       (symbol (subs token 0 (.indexOf token "/"))
;;               (subs token (inc (.indexOf token "/")) (.-length token)))
;;       (special-symbols token (symbol token)))))

;; based on matchSymol in clojure/lang/LispReader.java
(defn read-keyword
  [reader initch]
  (let [tok (read-token reader (read-char reader))
        a (re-matches* symbol-pattern tok)
        token (aget a 0)
        ns (aget a 1)
        name (aget a 2)]
    (if (or (and (not (undefined? ns))
                 (identical? (. ns (substring (- (.-length ns) 2) (.-length ns))) ":/"))
            (identical? (aget name (dec (.-length name))) ":")
            (not (== (.indexOf token "::" 1) -1)))
      (reader-error reader "Invalid token: " token)
      (if (and (not (nil? ns)) (> (.-length ns) 0))
        (keyword (.substring ns 0 (.indexOf ns "/")) name)
        (keyword (.substring token 1))))))


(defn desugar-meta
  [f]
  (cond
   (symbol? f) {:tag f}
   (string? f) {:tag f}
   (keyword? f) {f true}
   :else f))

(defn wrapping-reader
  [sym]
  (fn [rdr _]
    (list sym (read rdr true nil true))))

(defn throwing-reader
  [msg]
  (fn [rdr _]
    (reader-error rdr msg)))

(defn read-meta
  [rdr _]
  (let [[line column] (when (satisfies? cljs.extended.reader.IndexingReader rdr)
                        [(get-line-number rdr) (dec (get-column-number rdr))])
        m (desugar-meta (read rdr true nil true))]
    (when-not (map? m)
      (reader-error rdr "Metadata must be Symbol,Keyword,String or Map"))
    (let [o (read rdr true nil true)]
      (if (satisfies? IWithMeta o)
        (let [m (if (and line
                         (seq? o))
                  (assoc m :line line
                         :column column)
                  m)]
          (with-meta o (merge (meta o) m)))
        (reader-error rdr "Metadata can only be applied to IWithMetas")))))

(def UNQUOTE :__thisInternalKeywordRepresentsUnquoteToTheReader__)
(def UNQUOTE-SPLICING :__thisInternalKeywordRepresentsUnquoteSplicingToTheReader__)

(declare syntaxQuote)
(def ^:dynamic *gensym-env* (atom nil))
(def ^:dynamic *arg-env* (atom nil))

(defn isUnquote? [form]
  (and (satisfies? ISeq form) (= (first form) UNQUOTE)))

(defn isUnquoteSplicing? [form]
  (and (satisfies? ISeq form) (= (first form) UNQUOTE-SPLICING)))

(defn sqExpandList [sq]
  (doall
    (for [item sq]
      (cond
        (isUnquote? item)
        (list 'list (second item))

        (isUnquoteSplicing? item)
        (second item)

        :else
        (list 'list (syntaxQuote item))))))

;; TODO: analyzer deps (specials and resolve-existing-var) should really be moved to cljs.core
(defn syntaxQuote [form]
  (cond
    ;; (Compiler.isSpecial(form))
    (get specials form)
    (list 'quote form)

    ;; (form instanceof Symbol)
    (symbol? form)
    (let [sym form
          name (name sym)
          ns (namespace sym)
          ;var (resolve-existing-var (empty-env) sym)
          ]
      (cond
       ;; no namespace and name ends with #
       (and (not ns) (= "#" (last name)))
       (let [new-name (subs name 0 (- (count name) 1))
             gmap @*gensym-env*]
         (when (not gmap)
           (reader-error nil "Gensym literal not in syntax-quote"))
         (let [gs (or (get gmap sym)
                      (gensym (str new-name "__auto__")))]
           (swap! *gensym-env* assoc sym gs)
           (list 'quote gs)))

       ;; no namespace and name ends with .
       (and (not ns) (= "." (last name)))
       (throw (js/Error. "Syntax quote with no namespace ending with . not supported"))
;;        (let [new-name (subs name 0 (- (count name) 1))
;;              new-var (resolve-existing-var
;;                       (empty-env) (symbol new-name))]
;;          (list 'quote (:name new-var)))

       ;; no namespace and name begins with .
       (and (not ns) (= "." (first name)))
       (list 'quote sym)

       ;; resolve symbol
       :else
       (throw (js/Error. "Syntax quote resolving existing var not supported"))
;;        (list 'quote
;;              (:name
;;               (resolve-existing-var (empty-env) sym)))
       ))

    ;; (isUnquote(form))
    (isUnquote? form)
    (second form)

    ;; (isUnquoteSplicing(form))
    (isUnquoteSplicing? form)
    (throw (js/Error. "Reader ~@ splice not in list"))

    ;; TODO: figure out why nil is mapping to IMap
    (nil? form)
    (list 'quote form)

    ;; (form instanceof IPersistentCollection)
    (satisfies? ICollection form)
    (cond
      (satisfies? IRecord form)
      form

      (satisfies? IMap form)
      (list 'apply 'hash-map (list 'seq (cons 'concat (sqExpandList (apply concat (seq form))))))

      (satisfies? IVector form)
      (list 'apply 'vector (list 'seq (cons 'concat (sqExpandList form))))

      (satisfies? ISet form)
      (list 'apply 'hash-set (list 'seq (cons 'concat (sqExpandList (seq form)))))

      (or (satisfies? ISeq form) (satisfies? IList form))
      (if-let [sq (seq form)]
        (list 'seq (cons 'concat (sqExpandList sq)))
        (cons 'list nil))

      :else
      (throw (js/Error. "Unknown Collection type")))

    ;; (form instanceof Keyword || form instanceof Number ||
    ;;  form instanceof Character || form instanceof String)
    (or (keyword? form) (number? form) (string? form))
    form

    :else
    (list 'quote form)
    ))

(defn read-syntax-quote
  [rdr _]
  (binding [*gensym-env* (atom {})]
    (let [form (read rdr true nil true)]
      (syntaxQuote form))))

(defn read-unquote
  [rdr _]
  (let [ch (read-char rdr)]
    (cond
      (= nil ch)
      (reader-error rdr "EOF while reading unquote character")

      (= "@" ch)
      (let [o (read rdr true nil true)]
        (list UNQUOTE-SPLICING o))

      :else
      (do
        (unread rdr ch)
        (let [o (read rdr true nil true)]
          (list UNQUOTE o))))))

(defn read-character
  [rdr _]
  (let [ch (read-char rdr)]
    (when (= nil ch)
      (reader-error rdr "EOF while reading character constant"))
    (let [token (read-token rdr ch)]
      (cond
        (= 1 (count token))   token
        (= "newline" token)   "\n"
        (= "space" token)     " "
        (= "tab" token)       "\t"
        (= "backspace" token) "\b"
        (= "formfeed" token)  "\f"
        (= "return" token)    "\r"

        (= "u" (first token))
        (let [chars (apply str (rest token))]
          (validate-unicode-escape unicode-4-pattern rdr "u" chars)
          (let [c (make-unicode-char chars 16)
                cval (js/parseInt chars 16)]
            ;; surrogate code unit between \uD800 and \xDFFF?
            (when (and (>= cval 55296) (<= cval 57343))
              (reader-error rdr "Invalid character constant: \\" token))
            c))

        (= "o" (first token))
        (let [chars (apply str (rest token))]
          (validate-unicode-escape octal-pattern rdr "o" chars)
          (let [c (make-unicode-char chars 8)
                cval (js/parseInt chars 8)]
            (when (> cval 0377)
              (reader-error rdr "Octal escape sequence must be in range [0, 377]."))
            c))

        :else (reader-error rdr "Unsupported character: \\" token)))))

(defn garg [n]
  (let [pre (if (= n -1) "rest" (str "p" n))]
    (symbol (str (gensym pre) "#"))))

(defn read-fn
  [rdr _]
  (when @*arg-env*
    (reader-error nil "nested #()s are not allowed"))
  (binding [*arg-env* (atom (sorted-map))]
    (unread rdr "(")  ;) - the wink towards vim paren matching
    (let [pos (maybe-get-pos rdr)
          [form source] (with-source-log rdr #(read rdr true nil true) "#")
          argsyms @*arg-env*
          rargs (rseq argsyms)
          highpair (first rargs)
          higharg (if highpair (key highpair) 0)
          args (if (> higharg 0)
                 (doall (for [i (range 1 (+ 1 higharg))]
                          (or (get argsyms i)
                              (garg i))))
                 rargs)
          restsym (get argsyms -1)
          args (if restsym
                 (concat args ['& restsym])
                 args)]
      (with-meta (list 'fn* (vec args) form)
        (prep-meta rdr pos source)))))

(defn registerArg [n]
  (let [argsyms @*arg-env*]
    (when-not argsyms (reader-error nil "arg literal not in #()"))
    (let [ret (get argsyms n)]
      (if ret
        ret
        (let [ret (garg n)]
          (swap! *arg-env* assoc n ret)
          ret)))))

(defn read-arg
  [rdr pct]
  (if (not @*arg-env*)
    (read-symbol rdr "%")
    (let [ch (peek-char rdr)]
      ;; % alone is first arg
      (if (or (nil? ch)
              (whitespace? ch)
              (macro-terminating? ch))
        (registerArg 1)
        (let [n (read rdr true nil true)]
          (cond
            (= '& n)
            (registerArg -1)

            (not (number? n))
            (reader-error rdr "arg literal must be %, %& or %integer")

            :else
            (registerArg (int n))))))))

(defn read-set
  [rdr _]
  (let [pos (maybe-get-pos rdr)
        [the-set source] (with-source-log rdr #(read-delimited-list "}" rdr true) "#{")]
    (with-meta (identity the-set)
      (prep-meta rdr (update-in pos [:column] dec) source))))

(defn read-regex
  [reader]
  (loop [buffer ""
         ch (read-char reader)]

    (cond
     (nil? ch)
      (reader-error reader "EOF while reading regex")
     (identical? \\ ch)
      (recur (str buffer ch (read-char reader))
             (read-char reader))
     (identical? "\"" ch)
      (re-pattern buffer)
     :default
      (recur (str buffer ch) (read-char reader)))))

(defn read-discard
  [rdr _]
  (read rdr true nil true)
  rdr)

(defn macros [c]
  (cond
   (identical? c \") read-string*
   (identical? c \:) read-keyword
   (identical? c \;) read-comment
   (identical? c \') (wrapping-reader 'quote)
   (identical? c \@) (wrapping-reader 'deref)
   (identical? c \^) read-meta
   (identical? c \`) not-implemented ;read-syntax-quote
   (identical? c \~) read-unquote
   (identical? c \() read-list
   (identical? c \)) read-unmatched-delimiter
   (identical? c \[) read-vector
   (identical? c \]) read-unmatched-delimiter
   (identical? c \{) read-map
   (identical? c \}) read-unmatched-delimiter
   (identical? c \\) read-character
   (identical? c \%) read-arg
   (identical? c \#) read-dispatch
   :else nil))

;; omitted by design: var reader, eval reader
(defn dispatch-macros [s]
  (cond
   (identical? s "{") read-set
   (identical? s "(") read-fn
   (identical? s "<") (throwing-reader "Unreadable form")
   (identical? s "\"") read-regex
   (identical? s"!") read-comment
   (identical? s "_") read-discard
   :else nil))

(defn read
  "Reads the first object from a PushbackReader. Returns the object read.
   If EOF, throws if eof-is-error is true. Otherwise returns sentinel."
  ([reader]
    (read reader true nil))
  ([reader eof-is-error sentinel]
    (read reader eof-is-error sentinel false))
  ([reader eof-is-error sentinel is-recursive]
    (let [ch (read-char reader)]
      (cond
       (nil? ch) (if eof-is-error (reader-error reader "EOF while reading") sentinel)
       (whitespace? ch) (recur reader eof-is-error sentinel is-recursive)
       (comment-prefix? ch) (recur (read-comment reader ch) eof-is-error sentinel is-recursive)
       :else (let [f (macros ch)
                   res
                   (cond
                    f (f reader ch)
                    (number-literal? reader ch) (read-number reader ch)
                    :else (read-symbol reader ch))]
       (if (identical? res reader)
         (recur reader eof-is-error sentinel is-recursive)
         res))))))

(defn read-string
  "Reads one object from the string s"
  [s]
  (let [r (string-push-back-reader s)]
    (read r true nil false)))


(defn read-string-indexed
  "Reads one object indexed from the string s"
  [s]
  (let [r (indexing-push-back-reader s)]
    (read r true nil false)))


(defn read-string-source-logged
  "Reads one object indexed and source-logged for the string s (source and bounds only for colls)"
  [s]
  (let [r (source-logging-push-back-reader s)]
    (read r true nil false)))



;; read instances

(defn ^:private zero-fill-right [s width]
  (cond (= width (count s)) s
        (< width (count s)) (.substring s 0 width)
        :else (loop [b (gstring/StringBuffer. s)]
                (if (< (.getLength b) width)
                  (recur (.append b \0))
                  (.toString b)))))

(defn ^:private divisible?
  [num div]
  (zero? (mod num div)))

(defn ^:private indivisible?
  [num div]
    (not (divisible? num div)))

(defn ^:private leap-year?
  [year]
  (and (divisible? year 4)
       (or (indivisible? year 100)
           (divisible? year 400))))

(def ^:private days-in-month
  (let [dim-norm [nil 31 28 31 30 31 30 31 31 30 31 30 31]
        dim-leap [nil 31 29 31 30 31 30 31 31 30 31 30 31]]
    (fn [month leap-year?]
      (get (if leap-year? dim-leap dim-norm) month))))

(def ^:private parse-and-validate-timestamp
  (let [timestamp #"(\d\d\d\d)(?:-(\d\d)(?:-(\d\d)(?:[T](\d\d)(?::(\d\d)(?::(\d\d)(?:[.](\d+))?)?)?)?)?)?(?:[Z]|([-+])(\d\d):(\d\d))?"
        check (fn [low n high msg]
                (assert (<= low n high) (str msg " Failed:  " low "<=" n "<=" high))
                n)]
    (fn [ts]
      (when-let [[[_ years months days hours minutes seconds milliseconds] [_ _ _] :as V]
                 (->> ts
                      (re-matches timestamp)
                      (split-at 8)
                      (map vec))]
        (let [[[_ y mo d h m s ms] [offset-sign offset-hours offset-minutes]]
              (->> V
                   (map #(update-in %2 [0] %)
                        [(constantly nil) #(if (= % "-") "-1" "1")])
                   (map (fn [v] (map #(js/parseInt % 10) v))))
              offset (* offset-sign (+ (* offset-hours 60) offset-minutes))]
          [(if-not years 1970 y)
           (if-not months 1        (check 1 mo 12 "timestamp month field must be in range 1..12"))
           (if-not days 1          (check 1 d (days-in-month mo (leap-year? y)) "timestamp day field must be in range 1..last day in month"))
           (if-not hours 0         (check 0 h 23 "timestamp hour field must be in range 0..23"))
           (if-not minutes 0       (check 0 m 59 "timestamp minute field must be in range 0..59"))
           (if-not seconds 0       (check 0 s (if (= m 59) 60 59) "timestamp second field must be in range 0..60"))
           (if-not milliseconds 0  (check 0 ms 999 "timestamp millisecond field must be in range 0..999"))
           offset])))))

(defn parse-timestamp
  [ts]
  (if-let [[years months days hours minutes seconds ms offset]
           (parse-and-validate-timestamp ts)]
    (js/Date.
     (- (.UTC js/Date years (dec months) days hours minutes seconds ms)
        (* offset 60 1000)))
    (reader-error nil (str "Unrecognized date/time syntax: " ts))))

(defn ^:private read-date
  [s]
  (if (string? s)
    (parse-timestamp s)
    (reader-error nil "Instance literal expects a string for its timestamp.")))


(defn ^:private read-queue
  [elems]
  (if (vector? elems)
    (into (PersistentQueue. nil 0 nil [] 0) elems)
    (reader-error nil "Queue literal expects a vector for its elements.")))


(defn ^:private read-uuid
  [uuid]
  (if (string? uuid)
    (UUID. uuid)
    (reader-error nil "UUID literal expects a string as its representation.")))

(def *tag-table* (atom {"inst"  read-date
                        "uuid"  read-uuid
                        "queue" read-queue}))

(defn maybe-read-tagged-type
  [rdr initch]
  (let [tag  (read-symbol rdr initch)]
    (if-let [pfn (get @*tag-table* (name tag))]
      (pfn (read rdr true nil false))
      (reader-error rdr
                    "Could not find tag parser for " (name tag)
                    " in " (pr-str (keys @*tag-table*))))))

(defn register-tag-parser!
  [tag f]
  (let [tag (name tag)
        old-parser (get @*tag-table* tag)]
    (swap! *tag-table* assoc tag f)
    old-parser))

(defn deregister-tag-parser!
  [tag]
  (let [tag (name tag)
        old-parser (get @*tag-table* tag)]
    (swap! *tag-table* dissoc tag)
    old-parser))



;(read-string-source-logged "(+ 1 1)")



