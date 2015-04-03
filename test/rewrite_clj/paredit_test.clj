(ns rewrite-clj.paredit-test
  (:use midje.sweet)
  (:require [rewrite-clj.paredit :as par]
            [rewrite-clj.zip :as z]))

(fact "testing 'open'"
  (-> (z/of-string "(a b c)")
      (z/down)
      (par/open {})
      (z/root-string))
  => "({ } a b c)"
  
  (-> (z/of-string "(a b c)")
      (z/down)
      (par/open {} :a 1)
      (z/root-string))
  => "({:a 1} a b c)"
  
  (-> (z/of-string "(a b c)")
      (z/down)
      (par/open {} :a)
      (z/insert-right 1)
      (z/root-string))
  => "({:a 1} a b c)")

(fact "testing 'wrap'"
  (-> (z/of-string "a b c")
      (par/wrap)
      z/root-string)
  => "(a) b c")


(fact "testing 'splice'"
  (-> (z/of-string "((1 2) 3 4)")
      (z/down)
      (z/down)
      (par/splice)
      (z/root-string))
  => "(1 2 3 4)")


(fact "testing 'split'"
  (-> (z/of-string "(hello world)")
      (z/down)
      (z/right)
      (par/split)
      z/root-string)
  => "(hello) (world)"

  (-> (z/of-string "(hello world)")
      (z/down)
      (z/right)
      (par/split)
      (z/insert-left 'o)
      z/root-string)
  => "(hello) o (world)")

(fact "testing 'join'" ;; NOT PASSING
  (-> (z/of-string "(hello) (world)")
      (z/right)
      (par/join)
      z/root-string)
  => "(hello world)")

(fact "BREAKING TEST FOR REMOVE"
  (-> (z/of-string "(hello) (world)")
      (z/right)
      (z/remove)
      (z/sexpr))
  => '(hello))
