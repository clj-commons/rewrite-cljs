## rewrite-cljs

This library is a ClojureScript port of [rewrite-clj](https://github.com/xsc/rewrite-clj).
It provides features to traverse and rewrite Clojure/ClojureScript/EDN documents in a whitespace and comment-aware manner replicating
the behavior of its Clojure counterpart as closely as possible.

Created by @rundis in 2015, rewrite-cljs was originally used for Clojure/ClojureScript refactoring support in [Light Table](https://github.com/LightTable/LightTable). In January of 2019, @rundis graciously transferred rewrite-cljs to clj-commons.

rewrite-cljs includes:
- An EDN parser
- An EDN aware zipper (using clojure.zip for ClojureScript)
- A customized cljs.reader (based on [clojurescript-in-clojurescript](https://github.com/kanaka/clojurescript/blob/cljs_in_cljs/src/cljs/cljs/reader.cljs) that mimics more of clojure.tools.reader

[![CircleCI](https://circleci.com/gh/clj-commons/rewrite-cljs.svg?style=svg)](https://circleci.com/gh/clj-commons/rewrite-cljs)
[![cljdoc badge](https://cljdoc.org/badge/rewrite-cljs)](https://cljdoc.org/d/rewrite-cljs)
[![Clojars Project](https://img.shields.io/clojars/v/rewrite-cljs.svg)](https://clojars.org/rewrite-cljs)

## Quick start
Here's a little teaser on the sort of things you can do with the zipper features.

```clojure
(ns rewrite-clj.zip-test
  (:require-macros [cemerick.cljs.test :refer (is deftest )])
  (:require [cemerick.cljs.test :as t]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]))

(deftest manipulate-sexpr
  (let [sexpr "
 ^{:dynamic true} (+ 1 1
   (+ 2 2)
   (reduce + [1 3 4]))"
        expected "
 ^{:dynamic true} (+ 1 1
   (+ 2 2)
   (reduce + [6 7 [1 2]]))"]
    (is (= expected (-> sexpr
                        z/of-string
                        (z/find-tag-by-pos {:row 4 :col 19} :vector)
                        (z/replace [5 6 7])
                        (z/append-child [1 2])
                        z/down
                        z/remove
                        z/root-string)))))
```

## Limitations and ommissions

- rewrite-cljs has fallen quite far behind rewrite-clj - with some love from the community, we can bring it up to date.
- There is no support for parsing files (duh)
- cljs.extended.reader which is used for reading edn/clojure/clojurescript, has lot of limitations. Please don't be surprised
when encountering errors during reading of perhaps legal but hopefully infrequently used language constructs.
- Some features in rewrite-clj are heavily based on macros, these features have been omitted for now
  - Nice printing of nodes - Not implemented
  - [zip subedit support](https://github.com/xsc/rewrite-clj/blob/master/src/rewrite_clj/zip/subedit.clj) is not implemented (YET!)
- The reader captures positional metadata {:row :col :end-row :end-col} for all nodes. As long as you are only traversing the nodes you should be fine using the meta data and functions that depend on them (example zip/find-last-by-pos). However if you perform any form of rewriting the meta-data can't be trusted any longer. Not sure how to address that tbh. Pull requests are more than welcome!

## Rationale
Why a separate project? Why not incorporate ClojureScript support directly into rewrite-clj?

This might have not been terribly viable when this project was first created, but certainly is an option to consider today.

## Licenses

### License for rewrite-cljs
```
The MIT License (MIT)

Copyright (c) 2015 Magnus Rundberget

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### License for rewrite-clj
```
The MIT License (MIT)

Copyright (c) 2013-2015 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
