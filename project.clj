(defproject rewrite-cljs "0.1.0-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/rundis/rewrite-cljs"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"
                  :exclusions [org.apache.ant/ant]]]


  :profiles {:dev
             {:plugins [[lein-cljsbuild "1.0.3"]
                        [com.cemerick/clojurescript.test "0.3.3"]]

              :cljsbuild {
                          :builds {:test
                                   {:source-paths ["src" "test"]
                                    :notify-command ["phantomjs" :cljs.test/runner "target/out/unit-test.js"]
                                    :compiler {:output-to "target/out/unit-test.js"
                                               :optimizations :whitespace
                                               :pretty-print true}}}}}

           :doc {:plugins  [[funcool/codeina "0.1.0"
                             :exclusions [org.clojure/clojure]]]
                 :codeina {:sources ["src"]
                           :language :clojurescript
                           :exclude [cljs.extended.reader]
                           :src-dir-uri "https://github.com/rundis/rewrite-cljs/blob/master/"
                           :src-linenum-anchor-prefix "L"}}}

  :aliases {"auto-test" ["with-profile" "dev" "do" "clean," "cljsbuild" "auto" "test"]})
