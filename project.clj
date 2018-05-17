(defproject fulcrologic/fulcro-inspect "2.1.0"
  :description "A debugging tool for Fulcro that allows you to inspect state and other aspects of the running application(s)."
  :url "https://github.com/fulcrologic/fulcro-inspect"
  :min-lein-version "2.7.0"
  :license {:name "MIT Public License"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [fulcrologic/fulcro "2.5.2" :scope "provided"]
                 [com.wsscode/pathom "2.0.0-beta4"]

                 [org.clojure/tools.namespace "0.3.0-alpha4" :scope "test"]
                 [lein-doo "0.1.7" :scope "test"]
                 [fulcrologic/fulcro-spec "2.1.0-1" :scope "test" :exclusions [fulcrologic/fulcro]]]

  :source-paths ["src/remote" "src/ui" "src/chrome" "src/electron"]

  :figwheel {:server-port 3389}

  :jar-exclusions [#"public/.*"]

  ;; CI tests: Set up to support karma runner.
  :doo {:build "automated-tests"
        :paths {:karma "node_modules/karma/bin/karma"}}

  :profiles {:dev {:plugins      [[lein-cljsbuild "1.1.7"]
                                  [lein-doo "0.1.8"]]

                   :dependencies [[devcards "0.2.3" :exclusions [cljsjs/react cljsjs/react-dom]]
                                  [figwheel-sidecar "0.5.14" :exclusions [org.clojure/tools.nrepl]]
                                  [binaryage/devtools "0.9.9"]
                                  [org.clojure/test.check "0.9.0"]]}})
