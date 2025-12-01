(ns automigrate.errors.commands
  (:require
   [automigrate.errors.core :refer [->error-message ->error-title
                                    add-error-value]]
   [automigrate.errors.extraction :as extract]))

;; Error title implementations for commands domain
(defmethod ->error-title :automigrate.errors.commands/common-command-args-errors
  [_]
  "COMMAND ERROR")

;; Error message implementations for commands domain
(defmethod ->error-message :automigrate.errors.commands/common-command-args-errors
  [data]
  (let [reason (extract/problem-reason data)]
    (condp = reason
      '(clojure.core/fn [%] (clojure.core/contains? % :models-file))
      (add-error-value "Missing model file path." (:val data))

      '(clojure.core/fn [%] (clojure.core/contains? % :migrations-dir))
      (add-error-value "Missing migrations dir path." (:val data))

      '(clojure.core/fn [%] (clojure.core/contains? % :jdbc-url))
      (add-error-value "Missing db connection URL." (:val data))

      '(clojure.core/fn [%] (clojure.core/contains? % :number))
      (add-error-value "Missing migration number." (:val data))

      "Invalid command arguments.")))

(defmethod ->error-message :automigrate.core/jdbc-url
  [data]
  (add-error-value "Missing database connection URL." (:val data)))

(defmethod ->error-message :automigrate.core/type
  [data]
  (add-error-value "Invalid migration type." (:val data)))

(defmethod ->error-message :automigrate.core/direction
  [data]
  (add-error-value "Invalid direction of migration." (:val data)))
