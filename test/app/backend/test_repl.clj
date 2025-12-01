(ns app.backend.test-repl
  "Entry point for interactive Kaocha test runs used by the :test alias."
  (:require
    [kaocha.repl :as repl]
    [kaocha.watch :as watch]))

(defn config
  "Return the current Kaocha configuration."
  []
  (repl/config))

(defn run
  "Run Kaocha with optional selectors or config overrides."
  [& args]
  (apply repl/run args))

(defn watch
  "Start Kaocha in watch mode using the active configuration."
  []
  (watch/run (repl/config)))
