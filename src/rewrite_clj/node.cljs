(ns rewrite-clj.node
  (:require [rewrite-clj.node.coercer]
            [rewrite-clj.node.protocols :as prot]
            [rewrite-clj.node.keyword :as kw-node]
            [rewrite-clj.node.seq :as seq-node]
            [rewrite-clj.node.whitespace :as ws-node]
            [rewrite-clj.node.token :as tok-node]
            [rewrite-clj.node.comment :as cmt-node]
            [rewrite-clj.node.forms :as fm-node]
            [rewrite-clj.node.meta :as mt-node]
            [rewrite-clj.node.stringz :as s-node]
            [rewrite-clj.node.reader-macro :as rm-node]
            [rewrite-clj.node.quote :as q-node]
            [rewrite-clj.node.uneval :as ue-node]
            [rewrite-clj.node.fn :as f-node]))





; *******************************
; see rewrite-clj.node.protocols
; *******************************
(def tag prot/tag)
(def sexpr prot/sexpr)
(def string prot/string)
(def children prot/children)
(def child-sexprs prot/child-sexprs)
(def replace-children prot/replace-children)
(def inner? prot/inner?)
(def printable-only? prot/printable-only?)
(def coerce prot/coerce)


; *******************************
; see rewrite-clj.node.forms
; *******************************
(def forms-node fm-node/forms-node)
(def keyword-node kw-node/keyword-node)


; *******************************
; see rewrite-clj.node.seq
; *******************************
(def list-node seq-node/list-node)
(def vector-node seq-node/vector-node)
(def set-node seq-node/set-node)
(def map-node seq-node/map-node)


; *******************************
; see rewrite-clj.node.string
; *******************************
(def string-node s-node/string-node)



; *******************************
; see rewrite-clj.node.comment
; *******************************
(def comment-node cmt-node/comment-node)
(def comment? cmt-node/comment?)



; *******************************
; see rewrite-clj.node.whitespace
; *******************************
(def whitespace-node ws-node/whitespace-node)
(def newline-node ws-node/newline-node)
(def spaces ws-node/spaces)
(def newlines ws-node/newlines)
(def whitespace? ws-node/whitespace?)
(def linebreak? ws-node/linebreak?)

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))


; *******************************
; see rewrite-clj.node.token
; *******************************
(def token-node tok-node/token-node)


; *******************************
; see rewrite-clj.node.reader-macro
; *******************************
(def var-node rm-node/var-node)
(def eval-node rm-node/eval-node)
(def reader-macro-node rm-node/reader-macro-node)
(def deref-node rm-node/deref-node)


; *******************************
; see rewrite-clj.node.quote
; *******************************
(def quote-node q-node/quote-node)
(def syntax-quote-node q-node/syntax-quote-node)
(def unquote-node q-node/unquote-node)
(def unquote-splicing-node q-node/unquote-splicing-node)


; *******************************
; see rewrite-clj.node.uneval
; *******************************
(def uneval-node ue-node/uneval-node)


; *******************************
; see rewrite-clj.node.meta
; *******************************
(def meta-node mt-node/meta-node)

; *******************************
; see rewrite-clj.node.fn
; *******************************
(def fn-node f-node/fn-node)



