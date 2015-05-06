(defproject rewrite-cljs "0.4.13-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/rundis/rewrite-cljs"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"
                  :exclusions [org.apache.ant/ant]]]

  :hooks [leiningen.cljsbuild]
  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.2.3"]]

  :cljsbuild {
              :builds {:dev
                       {:source-paths ["src"]
                        :compiler {;:output-dir "target/out"
                                   :output-to "target/main.js"
                                   :source-map "target/main.js.map"
                                   ;:optimizations :whitespace
                                   :pretty-print true}}
                       :test
                       {:source-paths ["src" "test"]
                        :notify-command ["phantomjs" :cljs.test/runner "target/out/unit-test.js"]
                        :compiler {:output-to "target/out/unit-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}}}
  :aliases {"auto-test" ["do" "clean," "cljsbuild" "auto" "test"]})
