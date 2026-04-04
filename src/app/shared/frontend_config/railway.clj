(ns app.shared.frontend-config.railway
  "Helpers for running frontend-config tasks against Railway production."
  (:require
    [clojure.string :as str]))

(def ^:private internal-db-host "postgres.railway.internal:5432")
(def ^:private public-db-host "gondola.proxy.rlwy.net:12386")

(def ^:private supported-tasks
  #{"validate-frontend-config"
    "sync-frontend-config"
    "export-frontend-config-from-db"
    "migrate-and-sync-frontend-config"})

(def ^:private tasks-needing-railway-db-env
  #{"export-frontend-config-from-db"
    "migrate-and-sync-frontend-config"})

(def ^:private tasks-needing-prod-profile
  #{"export-frontend-config-from-db"
    "migrate-and-sync-frontend-config"})

(defn supported-task?
  [task]
  (contains? supported-tasks task))

(defn supported-tasks-list
  []
  (sort supported-tasks))

(defn railway-db-task?
  [task]
  (contains? tasks-needing-railway-db-env task))

(defn railway-public-database-url
  [internal-url]
  (let [internal-url (some-> internal-url str/trim not-empty)]
    (when internal-url
      (str/replace internal-url internal-db-host public-db-host))))

(defn ensure-prod-profile-args
  [task args]
  (if (and (contains? tasks-needing-prod-profile task)
        (not-any? #{"--profile"} args))
    (vec (concat ["--profile" "prod"] args))
    (vec args)))

(defn build-bb-command
  [task args]
  (vec (concat ["bb" task]
         (ensure-prod-profile-args task args))))
