#!/usr/bin/env bb

(ns railway-frontend-config
  "Run frontend-config tasks against Railway production using the public DB proxy when needed."
  (:require
    [app.shared.frontend-config.railway :as railway]
    [babashka.process :as process]
    [clojure.java.shell :as shell]
    [clojure.string :as str]))

(defn- die!
  ([msg] (die! msg 2))
  ([msg code]
   (binding [*out* *err*]
     (println msg))
   (System/exit code)))

(defn- usage!
  []
  (die!
    (str "Usage: bb railway-frontend-config <task> [args...]\n\n"
      "Supported tasks:\n  - "
      (str/join "\n  - " (railway/supported-tasks-list))
      "\n\nExamples:\n"
      "  bb railway-frontend-config export-frontend-config-from-db --only expenses\n"
      "  bb railway-frontend-config migrate-and-sync-frontend-config --only expenses\n"
      "  bb railway-frontend-config validate-frontend-config --schema resources/db")))

(defn- run-cmd!
  [opts cmd]
  (let [proc (process/process cmd (merge {:out :inherit :err :inherit} opts))
        result @proc]
    (when-not (zero? (:exit result))
      (die! (str "Command failed: " (str/join " " cmd)) (:exit result)))))

(defn- fetch-railway-public-db-url!
  []
  (let [internal-url (:out (shell/sh "railway" "run" "printenv" "DATABASE_URL"))
        public-url (railway/railway-public-database-url internal-url)]
    (when (str/blank? public-url)
      (die! "Could not get DATABASE_URL from Railway. Run: railway login && railway link"))
    public-url))

(defn -main
  [& args]
  (let [[task & task-args] args]
    (when-not task
      (usage!))
    (when-not (railway/supported-task? task)
      (die! (str "Unsupported frontend-config task for Railway: " task
              "\nSupported: " (str/join ", " (railway/supported-tasks-list)))))
    (let [cmd (railway/build-bb-command task task-args)
          extra-env (when (railway/railway-db-task? task)
                      (let [public-url (fetch-railway-public-db-url!)]
                        (println "Running Railway frontend-config task against prod DB via public proxy...")
                        {"DATABASE_URL" public-url
                         "DATABASE_PUBLIC_URL" public-url}))]
      (run-cmd! {:extra-env extra-env} cmd))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
