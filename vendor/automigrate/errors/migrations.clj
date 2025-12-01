(ns automigrate.errors.migrations
  (:require
   [automigrate.errors.core :refer [->error-message ->error-title
                                    add-error-value]]))

;; Error title implementations for migrations domain
(defmethod ->error-title :automigrate.actions/->migrations
  [_]
  "MIGRATION ERROR")

;; Helper function for migration errors
(defn- get-fq-type-from-action-error
  [data]
  (let [model-name (-> data :val :model-name)
        type-name (-> data :val :type-name)]
    (keyword (name model-name) (name type-name))))

;; Error message implementations for migrations domain
(defmethod ->error-message :automigrate.actions/->migrations
  [data]
  (let [reason (or (:reason data) (:pred data))]
    (condp = reason
      `coll? (add-error-value
               (format "Migration actions should be vector.")
               (:val data))

      "Migrations' schema error.")))

(defmethod ->error-message :automigrate.actions/->migration
  [data]
  (let [reason (or (:reason data) (:pred data))]
    (condp = reason
      "no method" (add-error-value
                    (format "Invalid action type.")
                    (:val data))

      '(clojure.core/fn [%]
         (clojure.core/contains? % :fields))
      (add-error-value (format "Missing :fields key in action.") (:val data))

      '(clojure.core/fn [%]
         (clojure.core/contains? % :model-name))
      (add-error-value (format "Missing :model-name key in action.") (:val data))

      "Migrations' schema error.")))

(defmethod ->error-message :automigrate.actions/model-name
  [data]
  (add-error-value
    (format "Action has invalid model name.")
    (:val data)))

(defmethod ->error-message :automigrate.actions/validate-type-choices-not-allow-to-remove
  [data]
  (let [fq-type-name (get-fq-type-from-action-error data)]
    (add-error-value
      (format "It is not possible to remove existing choices of enum type %s."
        fq-type-name)
      (-> data :val :changes))))

(defmethod ->error-message :automigrate.actions/validate-type-choices-not-allow-to-re-order
  [data]
  (let [fq-type-name (get-fq-type-from-action-error data)]
    (add-error-value
      (format "It is not possible to re-order existing choices of enum type %s."
        fq-type-name)
      (-> data :val :changes))))
