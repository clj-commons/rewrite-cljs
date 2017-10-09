(defproject rewrite-cljs "0.4.4"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/rundis/rewrite-cljs"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"
                  :exclusions [org.apache.ant/ant]]
                 [org.clojure/tools.reader "1.0.5"]]
  :doo {:build "test"}
  :profiles {:dev
             {:plugins [[lein-cljsbuild "1.1.2"]
                        [lein-doo "0.1.6"]]

              :cljsbuild {
                          :builds {:test
                                   {:source-paths ["src" "test"]
                                    ;:notify-command ["phantomjs" :cljs.test/runner "target/out/unit-test.js"]
                                    :compiler {:output-to "target/out/unit-test.js"
                                               :main 'rewrite-clj.runner
                                               :optimizations :whitespace
                                               :pretty-print true}}}}}

           :doc {:plugins  [[funcool/codeina "0.1.0"
                             :exclusions [org.clojure/clojure]]]
                 :codeina {:sources ["src"]
                           :language :clojurescript
                           :src-dir-uri "https://github.com/rundis/rewrite-cljs/blob/master/"
                           :src-linenum-anchor-prefix "L"}}}

  :aliases {"auto-test" ["with-profile" "dev" "do" "clean," "cljsbuild" "auto" "test"]})
