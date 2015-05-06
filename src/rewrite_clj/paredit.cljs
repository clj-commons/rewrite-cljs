(ns rewrite-clj.paredit
  (:require [rewrite-clj.zip :as z]))

(defn move-to-prev
  "Move node at current location to the position of previous location given a depth first traversal
    loc-node: 3 in (+ 1 (+ 2 3) 4) => (+ 1 (+ 3 2) 4)
    loc-node: 4 in (+ 1 (+ 2 3) 4) => (+ 1 (+ 2 3 4))
  returns zloc after move or given zloc if a move isn't possible"
  [zloc]
  (let [n (z/node zloc)
        p (some-> zloc z/left z/node)
        ins-fn (if (or (nil? p) (= (-> zloc z/remove z/node) p))
                 #(-> % (z/insert-left n) z/left)
                 #(-> % (z/insert-right n) z/right))]
    (if-not (-> zloc z/remove z/prev)
      zloc
      (-> zloc
          z/remove
          ins-fn))))
