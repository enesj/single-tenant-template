(ns app.backend.test-repl
  "Test REPL utilities for interactive test development.
   
   This namespace is loaded by the :test alias to provide
   convenient REPL-based test running."
  (:require
    [app.backend.fixtures :as fixtures]
    [kaocha.repl :as k]))

(defn run-all
  "Run all backend tests"
  []
  (k/run :app.backend))

(defn run-smoke
  "Run smoke tests only"
  []
  (k/run 'app.backend.routes-smoke-test))

(defn run-ns
  "Run tests in a specific namespace"
  [ns-sym]
  (k/run ns-sym))

(defn status
  "Check test system status"
  []
  (fixtures/test-system-status))

(defn start-system!
  "Manually start the test system"
  []
  (fixtures/start-test-system nil nil))

(defn stop-system!
  "Manually stop the test system"
  []
  (fixtures/reset-test-system! nil nil))

(println "")
(println "🧪 Backend Test REPL loaded")
(println "   (run-all)        - Run all tests")
(println "   (run-smoke)      - Run smoke tests")
(println "   (run-ns 'ns)     - Run specific namespace")
(println "   (status)         - Check test system status")
(println "   (start-system!)  - Start test system manually")
(println "   (stop-system!)   - Stop test system manually")
(println "")
