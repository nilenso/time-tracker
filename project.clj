(defproject time-tracker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.6.2-alpha3"]
                 [http-kit "2.1.18"]
                 [bidi "2.0.11"]
                 [ring "1.5.0"]
                 [cheshire "5.6.3"]
                 [yesql "0.5.3"]
                 [ragtime "0.6.3"]
                 [postgresql "9.3-1102.jdbc41"]
                 [ring/ring-json "0.4.0"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [environ "1.1.0"]
                 [ring/ring-defaults "0.2.1"]]
  :main ^:skip-aot time-tracker.core
  :target-path "target/%s"
  :plugins [[lein-environ "1.1.0"]]
  :profiles {:uberjar {:aot :all}}

  :aliases {"migrate"  ["run" "-m" "time-tracker.migration/migrate-db"]
            "rollback" ["run" "-m" "time-tracker.migration/rollback-db"]})

