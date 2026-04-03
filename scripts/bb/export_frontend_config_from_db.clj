#!/usr/bin/env bb

(ns export-frontend-config-from-db
  "Babashka wrapper that runs the real Clojure export entrypoint."
  (:require
    [babashka.process :as process]
    [clojure.string :as str]))

(defn- die!
  ([msg] (die! msg 2))
  ([msg code]
   (binding [*out* *err*]
     (println msg))
   (System/exit code)))

(defn- run-cmd!
  [& cmd]
  (let [proc (process/process cmd {:out :inherit :err :inherit})
        result @proc]
    (when-not (zero? (:exit result))
      (die! (str "Command failed: " (str/join " " cmd)) (:exit result)))))

(defn -main
  [& args]
  (apply run-cmd! "clj" "-M" "-m" "app.shared.frontend-config.export-from-db" args))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
