(ns automigrate.sql.core
  "Core SQL generation infrastructure with multimethod declarations and public API."
  (:require
   [automigrate.util.db :as db-util]
   [clojure.spec.alpha :as s]))

;; Constants
(def ^:private DEFAULT-INDEX :btree)

;; Multimethod declarations
(defmulti action->sql :action)

;; Core specs
(s/def ::->sql (s/multi-spec action->sql :action))

;; Public API
(defn ->sql
  "Convert migration action to sql."
  [action]
  (let [formatted-action (s/conform ::->sql action)]
    (if (sequential? formatted-action)
      (map #(db-util/fmt %) formatted-action)
      (db-util/fmt formatted-action))))
