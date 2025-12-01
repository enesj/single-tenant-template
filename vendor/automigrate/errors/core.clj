(ns automigrate.errors.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

;; Constants and templates (extracted from lines 8-15)
(def ^:private ERROR-TEMPLATE
  (str "-- %s -------------------------------------\n\n%s\n"))

(def ^:private INDEX-FIELD-NAME-IN-SPEC 2)

;; Error hierarchy definition (extracted from lines 112-118)
(def ^:private error-hierarchy
  (-> (make-hierarchy)
    (derive :automigrate.fields/field-with-type :automigrate.fields/field)
    (derive :automigrate.core/make-args :automigrate.errors.commands/common-command-args-errors)
    (derive :automigrate.core/migrate-args :automigrate.core/make-args)
    (derive :automigrate.core/explain-args :automigrate.core/make-args)
    (derive :automigrate.core/list-args :automigrate.core/make-args)))

;; Helper function for multimethod implementations
(defn last-spec
  [problem]
  (-> problem :via peek))

;; Multimethod declarations (extracted from lines 120-142)
(defmulti ->error-title :main-spec
  :hierarchy #'error-hierarchy)

(defmethod ->error-title :default
  [_]
  "ERROR")

(defmulti ->error-message last-spec
  :hierarchy #'error-hierarchy)

(defn get-model-name-by-default
  [data]
  (if-let [spec-val (seq (:val data))]
    spec-val
    (-> data :in first)))

(defn add-error-value
  "Add error value after the error message."
  [message value]
  (if (and (list? value) (empty? value))
    message
    (str message "\n\n  " (pr-str value))))

(defmethod ->error-message :default
  [data]
  (case (:main-spec data)
    :automigrate.models/->internal-models
    (add-error-value "Schema failed for model." (get-model-name-by-default data))

    :automigrate.actions/->migrations
    (add-error-value "Schema failed for migration." (:val data))

    (add-error-value "Schema failed." (:val data))))

;; Public API functions (extracted from lines 801-826)
(defn explain-data->error-report
  "Convert spec explain-data output to errors' report."
  [explain-data]
  (let [problems (::s/problems explain-data)
        main-spec (::s/spec explain-data)
        origin-value (::s/value explain-data)
        reports (for [problem problems
                      :let [problem* (assoc problem
                                       :origin-value origin-value
                                       :main-spec main-spec)]]
                  {:title (->error-title problem*)
                   :message (->error-message problem*)
                   :problem problem*})
        messages (->> reports
                   (map #(format ERROR-TEMPLATE (:title %) (:message %)))
                   (str/join "\n"))]
    {:reports reports
     :formatted messages}))

(defn custom-error->error-report
  "Convert custom error data output to errors' report."
  [error-data]
  (let [title (or (:title error-data) (->error-title {}))
        formatted-error #(format ERROR-TEMPLATE title %)]
    (update error-data :message formatted-error)))
