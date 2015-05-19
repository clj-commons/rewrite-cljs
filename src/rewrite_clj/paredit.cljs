(ns rewrite-clj.paredit
  (:require [rewrite-clj.zip :as z]
            [clojure.zip :as zz]
            [rewrite-clj.zip.whitespace :as ws]
            [rewrite-clj.zip.utils :as u]
            [rewrite-clj.node :as nd]
            [rewrite-clj.node.stringz :as sn :refer [StringNode] ]))


;;*****************************
;; Helpers
;;*****************************

(defn- empty-seq? [zloc]
  (and (z/seq? zloc) (not (seq (z/sexpr zloc)))))

;; helper
(defn move-n [loc f n]
  (if (= 0 n)
    loc
    (->> loc (iterate f) (take (inc n)) last)))

(defn- top
  [zloc]
  (->> zloc
       (iterate z/up)
       (take-while identity)
       last))

;; TODO : not very inefficent ...
(defn- global-find-by-node
  [zloc n]
  (-> zloc
      top
      (z/find zz/next #(= (meta (z/node %)) (meta n)))))



(defn- nodes-by-dir
  ([zloc f] (nodes-by-dir zloc f constantly))
  ([zloc f p?]
   (->> zloc
        (iterate f)
        (take-while identity)
        (take-while p?)
        (map z/node))))

(defn- remove-first-if-ws [nodes]
  (when (seq nodes)
    (if (nd/whitespace? (first nodes))
      (rest nodes)
      nodes)))


(defn- remove-ws-or-comment [zloc]
  (if-not (ws/whitespace-or-comment? zloc)
    zloc
    (recur (zz/remove zloc))))

;;*****************************
;; Paredit functions
;;*****************************




(defn kill [zloc]
  (let [left (zz/left zloc)]
     (-> zloc
         (u/remove-right-while (constantly true))
         zz/remove
         (#(if left
            (global-find-by-node % (z/node left))
            %)))))

(defn- string-node? [zloc]
  (= (some-> zloc z/node type) (type (nd/string-node " "))))


(defn kill-in-string-node [zloc pos]
  (let [bounds (-> zloc z/node meta)
        row-idx (- (:row pos) (:row bounds))
        sub-length (if-not (= (:row pos) (:row bounds))
                      (dec (:col pos))
                      (- (:col pos) (inc (:col bounds))))]

    (-> (take (inc row-idx) (-> zloc z/node :lines))
        vec
        (update-in [row-idx] #(.substring % 0 sub-length))
        (#(z/replace zloc (nd/string-node %))))))


(defn kill-at-pos
  "String aware kill"
  [zloc pos]
  (if-let [candidate (z/find-last-by-pos zloc pos)]
    (if (string-node? candidate)
      (if (= (z/string candidate) "\"\"")
        (z/remove candidate)
        (kill-in-string-node candidate pos))
      (if (and (empty-seq? candidate)
               (> (:col pos) (-> candidate z/node meta :col)))
        (z/remove candidate)
        (kill candidate)))
    zloc))


(defn- find-slurpee [zloc f]
  (loop [l (z/up zloc)
         n 1]
    (cond
     (nil? l) nil
     (not (nil? (f l))) [n (f l)]
     (nil? (z/up l)) nil
     :else (recur (z/up l) (inc n)))))


(defn slurp-forward
  [zloc]
  (let [[slurpee-loc n-ups] (or (when (empty-seq? zloc)
                                   [(z/right zloc) 0])
                                 (let [[n l] (find-slurpee zloc z/right)]
                                   (when l
                                     [l n])))]
    (if-not slurpee-loc
      zloc
      (-> zloc
          (move-n z/up n-ups)
          (u/remove-right-while ws/whitespace?)
          u/remove-right
          (z/append-child (z/node slurpee-loc))
          (#(if (empty-seq? zloc)
              (z/down %)
              (global-find-by-node % (z/node zloc))))))))


(defn slurp-backward
  [zloc]
  (let [slurpee-loc (or (when (empty-seq? zloc) (z/left zloc))
                        (let [[n l] (find-slurpee zloc z/left)]
                          l))]

    (if-not slurpee-loc
      zloc
      (-> slurpee-loc
          z/remove
          z/next
          (z/insert-child (z/node slurpee-loc))
          (#(if (empty-seq? zloc)
              (z/down %)
              (global-find-by-node % (z/node zloc))))))))

(defn barf-forward
  [zloc]
  (let [barfee-loc (z/rightmost zloc)]
    (if-not (z/up zloc)
      zloc
      (-> barfee-loc
          z/remove
          (#(if-not (= (z/leftmost zloc) barfee-loc) (z/up %) %))
          (z/insert-right (z/node barfee-loc))
          (#(or (global-find-by-node % (z/node zloc))
                (global-find-by-node % (z/node barfee-loc))))))))

(defn barf-backward
  [zloc]
  (let [barfee-loc (z/leftmost zloc)]
    (if-not (z/up zloc)
      zloc
      (-> barfee-loc
          z/remove
          (z/insert-left (z/node barfee-loc))
          (#(or (global-find-by-node % (z/node zloc))
                (global-find-by-node % (z/node barfee-loc))))))))

(defn create-seq-node [t v]
  (case t
    :list (nd/list-node v)
    :vector (nd/vector-node v)
    :map (nd/map-node v)
    :set (nd/set-node v)
    (throw (js/Error. (str "Unsupported wrap type: " t)))))

(defn wrap-around
  [zloc t]
  (-> zloc
      (z/insert-left (create-seq-node t nil))
      z/left
      (u/remove-right-while ws/whitespace?)
      u/remove-right
      (zz/append-child (z/node zloc))
      z/down))


(def splice z/splice)


(defn split
  [zloc]
  (let [parent-loc (z/up zloc)]
    (if-not parent-loc
      zloc
      (let [t (z/tag parent-loc)
            lefts (reverse (remove-first-if-ws (rest (nodes-by-dir (z/right zloc) zz/left))))
            rights (remove-first-if-ws (nodes-by-dir (z/right zloc) zz/right))]

        (if-not (and (seq lefts) (seq rights))
          zloc
          (-> parent-loc
              (z/insert-left (create-seq-node t lefts))
              (z/insert-left (create-seq-node t rights))
              z/remove
              (#(or (global-find-by-node % (z/node zloc))
                    (global-find-by-node % (last lefts))))))))))


(defn join
  [zloc]
  (let [left (some-> zloc z/left)
        right (if (some-> zloc z/node nd/whitespace?) (z/right zloc) zloc)]

    (if-not (and (z/seq? left) (z/seq? right))
      zloc
      (let [lefts (-> left z/node nd/children)
            ws-nodes (-> (zz/right left) (nodes-by-dir zz/right ws/whitespace-or-comment?))
            rights (-> right z/node nd/children)]

        (-> right
            zz/remove
            remove-ws-or-comment
            z/up
            (z/insert-left (create-seq-node :vector
                                            (concat lefts
                                                    ws-nodes
                                                    rights)))
            z/remove
            (global-find-by-node (first rights)))))))



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
