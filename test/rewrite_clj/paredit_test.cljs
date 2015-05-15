(ns rewrite-clj.paredit-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [rewrite-clj.zip :as z]
            [clojure.zip :as zz]
            [rewrite-clj.paredit :as pe]))

;; helper
(defn move-n [loc f n]
  (->> loc (iterate f) (take n) last))


(deftest kill-to-end-of-sexpr
  (let [res (-> "[1 2 3 4]"
                z/of-string
                z/down zz/right
                pe/kill)]
    (is (= "[1]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))

(deftest kill-to-end-of-line
  (let [res (-> "[1 2] ; useless comment"
                z/of-string
                zz/right
                pe/kill)]
    (is (= "[1 2]" (-> res z/root-string)))
    (is (= "[1 2]" (-> res z/string)))))

(deftest kill-to-wipe-all-sexpr-contents
  (let [res (-> "[1 2 3 4]"
                z/of-string
                z/down
                pe/kill)]
    (is (= "[]" (-> res z/root-string)))
    (is (= "[]" (-> res z/string)))))

(deftest kill-to-wipe-all-sexpr-contents-in-nested-seq
  (let [res (-> "[[1 2 3 4]]"
                z/of-string
                z/down
                pe/kill)]
    (is (= "[]" (-> res z/root-string)))
    (is (= "[]" (-> res z/string)))))

(deftest kill-when-left-is-sexpr
  (let [res (-> "[1 2 3 4] 2"
                z/of-string
                zz/right
                pe/kill)]
    (is (= "[1 2 3 4]" (-> res z/root-string)))
    (is (= "[1 2 3 4]" (-> res z/string)))))

(deftest kill-it-all
  (let [res (-> "[1 2 3 4] 5"
                z/of-string
                pe/kill)]
    (.log js/console res)
    (is (= "" (-> res z/root-string)))
    (is (= "" (-> res z/string)))))



(deftest slurp-forward-and-keep-loc-rightmost
  (let [res (-> "[[1 2] 3 4]"
                z/of-string
                z/down z/down z/right
                pe/slurp-forward)]
    (is (= "[[1 2 3] 4]" (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))

(deftest slurp-forward-and-keep-loc-leftmost
  (let [res (-> "[[1 2] 3 4]"
                z/of-string
                z/down z/down
                pe/slurp-forward)]
    (is (= "[[1 2 3] 4]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))

(deftest slurp-forward-from-empty-sexpr
  (let [res (-> "[[] 1 2 3]"
                z/of-string
                z/down
                pe/slurp-forward)]
    (is (= "[[1] 2 3]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))

(deftest slurp-forward-from-whitespace-node
  (let [res (-> "[[1 2] 3 4]"
                z/of-string
                z/down z/down zz/right
                pe/slurp-forward)]
    (is (= "[[1 2 3] 4]" (-> res z/root-string)))
    (is (= " " (-> res z/string)))))

(deftest slurp-forward-nested
  (let [res (-> "[[[1 2]] 3 4]"
                z/of-string
                z/down z/down z/down
                pe/slurp-forward)]
    (is (= "[[[1 2] 3] 4]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))

(deftest slurp-forward-nested-silly
  (let [res (-> "[[[[[1 2]]]] 3 4]"
                z/of-string
                z/down z/down z/down z/down z/down
                pe/slurp-forward)]
    (is (= "[[[[[1 2]]] 3] 4]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))




(deftest slurp-backward-and-keep-loc-leftmost
  (let [res (-> "[1 2 [3 4]]"
                z/of-string
                z/down z/rightmost z/down
                pe/slurp-backward)]
    (is (= "[1 [2 3 4]]" (-> res z/root-string)))
    (is (= "3" (-> res z/string)))))

(deftest slurp-backward-and-keep-loc-rightmost
  (let [res (-> "[1 2 [3 4]]"
                z/of-string
                z/down z/rightmost z/down z/rightmost
                pe/slurp-backward)]
    (is (= "[1 [2 3 4]]" (-> res z/root-string)))
    (is (= "4" (-> res z/string)))))

(deftest slurp-backward-from-empty-sexpr
  (let [res (-> "[1 2 3 4 []]"
                z/of-string
                z/down z/rightmost
                pe/slurp-backward)]
    (is (= "[1 2 3 [4]]" (-> res z/root-string)))
    (is (= "4" (-> res z/string)))))

(deftest slurp-backward-nested
  (let [res (-> "[1 2 [[3 4]]]"
                z/of-string
                z/down z/rightmost z/down z/down z/rightmost
                pe/slurp-backward)]
    (is (= "[1 [2 [3 4]]]" (-> res z/root-string)))
    (is (= "4" (-> res z/string)))))

(deftest slurp-backward-nested-silly
  (let [res (-> "[1 2 [[[3 4]]]]"
                z/of-string
                z/down z/rightmost z/down z/down z/down z/rightmost
                pe/slurp-backward)]
    (is (= "[1 [2 [[3 4]]]]" (-> res z/root-string)))
    (is (= "4" (-> res z/string)))))


(deftest barf-forward-and-keep-loc
  (let [res (-> "[[1 2 3] 4]"
                z/of-string
                z/down z/down z/right; position at 2
                pe/barf-forward)]
    (is (= "[[1 2] 3 4]" (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))

(deftest barf-forward-at-rightmost-moves-out-of-sexrp
  (let [res (-> "[[1 2 3] 4]"
                z/of-string
                z/down z/down z/rightmost; position at 3
                pe/barf-forward)]

    (is (= "[[1 2] 3 4]" (-> res z/root-string)))
    (is (= "3" (-> res z/string)))))

(deftest barf-forward-at-rightmost-which-is-a-whitespace-haha
  (let [res (-> "[[1 2 3 ] 4]"
                z/of-string
                z/down z/down zz/rightmost; position at space at the end
                pe/barf-forward)]

    (is (= "[[1 2] 3 4]" (-> res z/root-string)))
    (is (= "3" (-> res z/string)))))


(deftest barf-forward-at-when-only-one
  (let [res (-> "[[1] 2]"
                z/of-string
                z/down z/down
                pe/barf-forward)]

    (is (= "[[] 1 2]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))



(deftest barf-backward-and-keep-current-loc
  (let [res (-> "[1 [2 3 4]]"
                z/of-string
                z/down z/rightmost z/down z/rightmost ; position at 4
                pe/barf-backward)]
    (is (= "[1 2 [3 4]]" (-> res z/root-string)))
    (is (= "4" (-> res z/string)))))

(deftest barf-backward-at-leftmost-moves-out-of-sexpr
  (let [res (-> "[1 [2 3 4]]"
                z/of-string
                z/down z/rightmost z/down ; position at 2
                pe/barf-backward)]
    (is (= "[1 2 [3 4]]" (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))


(deftest wrap-around
  (is (= "(1)" (-> (z/of-string "1") (pe/wrap-around :list) z/root-string)))
  (is (= "[1]" (-> (z/of-string "1") (pe/wrap-around :vector) z/root-string)))
  (is (= "{1}" (-> (z/of-string "1") (pe/wrap-around :map) z/root-string)))
  (is (= "#{1}" (-> (z/of-string "1") (pe/wrap-around :set) z/root-string))))

(deftest wrap-around-keeps-loc
  (let [res (-> "1"
                z/of-string
                (pe/wrap-around :list))]
    (is (= "1" (-> res z/string)))))


(deftest wrap-around-fn
  (is (= "(-> (#(+ 1 1)))" (-> (z/of-string "(-> #(+ 1 1))")
                               z/down z/right
                                (pe/wrap-around :list)
                                z/root-string))))


(map #(+ 1 1) [1 2])


(deftest split
  (let [res (-> "[1 2]"
                z/of-string
                z/down
                pe/split)]
    (is (= "[1] [2]" (-> res z/root-string)))
    (is (= "1" (-> res z/string)))))

(deftest split-includes-node-at-loc-as-left
  (let [res (-> "[1 2 3 4]"
                z/of-string
                z/down z/right
                pe/split)]
    (is (= "[1 2] [3 4]" (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))


(deftest split-at-whitespace
  (let [res (-> "[1 2 3 4]"
                z/of-string
                z/down z/right zz/right
                pe/split)]
    (is (= "[1 2] [3 4]" (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))




(deftest split-includes-comments-and-newlines
  (let [sexpr "
[1 ;dill
 2 ;dall
 3 ;jalla
]"
        expected "
[1 ;dill
 2 ;dall
] [3 ;jalla
]"
        res (-> sexpr
                z/of-string
                z/down z/right
                pe/split)]
    (is (= expected (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))

(deftest split-when-only-one-returns-self
  (is (= "[1]" (-> (z/of-string "[1]")
                   z/down
                   pe/split
                   z/root-string)))
  (is (= "[1 ;dill\n]" (-> (z/of-string "[1 ;dill\n]")
                           z/down
                           pe/split
                           z/root-string))))


(deftest join-simple
  (let [res (-> "[1 2] [3 4]"
                z/of-string
                ;z/down
                zz/right
                pe/join)]
    (is (= "[1 2 3 4]" (-> res z/root-string)))
    (is (= "3" (-> res z/string)))))

(deftest join-with-comments
  (let [sexpr "
[[1 2] ; the first stuff
 [3 4] ; the second stuff
]"      expected "
[[1 2 ; the first stuff
 3 4]; the second stuff
]"
        res (-> sexpr
                z/of-string
                z/down zz/right
                pe/join)]
    (is (= expected (-> res z/root-string)))))



(deftest move-to-prev-flat
  (is (= "(+ 2 1)" (-> "(+ 1 2)"
                       z/of-string
                       z/down
                       z/rightmost
                       pe/move-to-prev
                       z/root-string))))

(deftest move-to-prev-when-prev-is-seq
  (is (= "(+ 1 (+ 2 3 4))" (-> "(+ 1 (+ 2 3) 4)"
                              z/of-string
                              z/down
                              z/rightmost
                              pe/move-to-prev
                              z/root-string))))

(deftest move-to-prev-out-of-seq
  (is (= "(+ 1 4 (+ 2 3))" (-> "(+ 1 (+ 2 3) 4)"
                              z/of-string
                              z/down
                              z/rightmost
                              (move-n pe/move-to-prev 6)
                              z/root-string))))
