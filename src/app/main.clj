(ns app.main
  "Production entry point. Launches the backend system.
   This namespace exists solely to provide (:gen-class) so the uberjar
   can be executed with: java -jar app.jar"
  (:gen-class)
  (:require [app.template.backend.core :as core]))

(defn -main [& _args]
  (core/main))
