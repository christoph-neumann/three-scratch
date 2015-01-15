(defproject threes "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2665"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; repl
                 [com.cemerick/piggieback "0.1.5"]
                 [weasel "0.5.0"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-ancient "0.5.5" :exclusions [org.clojure/clojure]]
            [cider/cider-nrepl "0.8.2"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["out/threes" "threes.js" "threes.min.js"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "threes.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "threes.min.js"
                :pretty-print false
                :optimizations :advanced}}]})
