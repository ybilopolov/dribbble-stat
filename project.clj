(defproject dribbble-stat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.4.1"]]
  :main ^:skip-aot dribbble-stat.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
