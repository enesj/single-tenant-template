(ns app.shared.frontend.utils.db
  "Generic database and configuration utilities for all domains.

  This namespace provides universal utilities for database state management
  and configuration loading patterns."
  (:require [taoensso.timbre :as log]))

(defn assoc-paths
  "Utility to assoc multiple `[path value]` pairs in a db map."
  [db path-value-pairs]
  (reduce (fn [acc [path value]]
            (assoc-in acc path value))
    db
    path-value-pairs))

(defn maybe-fetch-config
  "Return the config fetch dispatch when models data is missing."
  [db]
  (when-not (:models-data db)
    [:app.template.frontend.events.config/fetch-config]))
