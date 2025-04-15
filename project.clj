(defproject asteroids-new "0.1.0-SNAPSHOT"
  :description "The original figwheel asteroidsbird demo redone for figwheel.main"

  :url "http://rigsomelight.com/2014/05/01/interactive-programming-asteroids-bird-clojurescript.html"

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/clojurescript "1.11.132"]
                 [org.clojure/core.async "1.7.701"]
                 [cljsjs/react-dom "18.0.0-rc.0-0"]
                 [cljsjs/react "18.0.0-rc.0-0"]
                 [sablono/sablono "0.8.6"]]

  :resource-paths ["resources" "target"]
  
  :clean-targets ^{:protect false} ["target/public"]

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.20"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]
                                  [org.slf4j/slf4j-nop "2.0.3"]]}}

  :aliases {"dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "asteroids" "-r"]
            ;; https://figwheel.org/docs/advanced_compile.html
            "build" ["trampoline" "run" "-m" "figwheel.main" "-bo" "asteroids"]})
