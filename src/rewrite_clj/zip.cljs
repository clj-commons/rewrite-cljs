(ns rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.zip.base :as base]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.findz :as f]
            [rewrite-clj.zip.editz :as ed]
            [rewrite-clj.zip.insert :as ins]
            [rewrite-clj.zip.removez :as rm]
            [rewrite-clj.zip.seqz :as sz]
            [clojure.zip :as z]))



(def node z/node)
(def root z/root)


(def of-string base/of-string)
(def root-string base/root-string)
(def string base/string)
(def tag base/tag)
(def sexpr base/sexpr)




;; **********************************
;; Originally in rewrite-clj.zip.move
;; **********************************
(def right m/right)
(def left m/left)
(def down m/down)
(def up m/up)
(def next m/next)
(def end? m/end?)
(def rightmost? m/rightmost?)
(def leftmost? m/leftmost?)
(def prev m/prev)
(def leftmost m/leftmost)
(def rightmost m/rightmost)




;; **********************************
;; Originally in rewrite-clj.zip.findz
;; **********************************
(def find f/find)
(def find-last-by-pos f/find-last-by-pos)
(def find-depth-first f/find-depth-first)
(def find-next f/find-next)
(def find-next-depth-first f/find-next-depth-first)
(def find-tag f/find-tag)
(def find-next-tag f/find-next-tag)
(def find-tag-by-pos f/find-tag-by-pos)
(def find-token f/find-token)
(def find-next-token f/find-next-token)
(def find-value f/find-value)
(def find-next-value f/find-next-value)



;; **********************************
;; Originally in rewrite-clj.zip.editz
;; **********************************
(def replace ed/replace)
(def edit ed/edit)
(def splice ed/splice)
(def prefix ed/prefix)
(def suffix ed/suffix)

;; **********************************
;; Originally in rewrite-clj.zip.removez
;; **********************************
(def remove rm/remove)


;; **********************************
;; Originally in rewrite-clj.zip.insert
;; **********************************
(def insert-right ins/insert-right)
(def insert-left ins/insert-left)
(def insert-child ins/insert-child)
(def append-child ins/append-child)


;; **********************************
;; Originally in rewrite-clj.zip.seqz
;; **********************************
(def seq? sz/seq?)
(def list? sz/list?)
(def vector? sz/vector?)
(def set? sz/set?)
(def map? sz/map?)
(def map-vals sz/map-vals)
(def map-keys sz/map-keys)
(def map sz/map)
(def get sz/get)
(def assoc sz/assoc)
