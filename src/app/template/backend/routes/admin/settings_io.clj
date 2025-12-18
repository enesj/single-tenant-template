(ns app.template.backend.routes.admin.settings-io
  "Admin settings I/O - reading and writing configuration files."
  (:require
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [taoensso.timbre :as log]))

(def ^:private view-options-path "src/app/admin/frontend/config/view-options.edn")
(def ^:private form-fields-path "src/app/admin/frontend/config/form-fields.edn")
(def ^:private table-columns-path "src/app/admin/frontend/config/table-columns.edn")

;; User-facing (domain-owned) UI config for expenses.
(def ^:private user-entities-path "src/app/domain/frontend/expenses/config/entities.edn")
(def ^:private user-view-options-path "src/app/domain/frontend/expenses/config/view-options.edn")
(def ^:private user-form-fields-path "src/app/domain/frontend/expenses/config/form-fields.edn")
(def ^:private user-table-columns-path "src/app/domain/frontend/expenses/config/table-columns.edn")

(defn read-view-options
  "Read view-options.edn file and parse it.
   Validates the file content against the Malli spec."
  []
  (try
    (let [file (io/file view-options-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              validation (view-options-spec/validate-view-options-strict data)]
          (when-not (:valid? validation)
            (log/warn "view-options.edn validation issues:"
              {:errors (:errors validation)
               :warnings (:warnings validation)}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read view-options.edn")
      (throw (ex-info "Failed to read settings file" {:status 500})))))

(defn write-view-options!
  "Write view-options map to EDN file with pretty printing.
   Validates the data before writing and throws on invalid data."
  [view-options]
  ;; Validate before writing - throw on invalid data
  (let [{:keys [valid? errors nested-locks-errors]}
        (view-options-spec/validate-view-options-strict view-options)]
    (when-not valid?
      (log/error "Attempted to write invalid view-options data"
        {:errors errors :nested-locks-errors nested-locks-errors})
      (throw (ex-info "Invalid view-options data"
               {:status 400
                :errors errors
                :nested-locks-errors nested-locks-errors}))))
  (try
    (let [file (io/file view-options-path)]
      ;; Ensure parent directory exists
      (io/make-parents file)
      ;; Write with pretty printing for readability
      (spit file (with-out-str (pprint/pprint view-options))))
    (catch Exception e
      (log/error e "Failed to write view-options.edn")
      (throw (ex-info "Failed to write settings file" {:status 500})))))

(defn read-form-fields
  "Read form-fields.edn file and parse it"
  []
  (try
    (let [file (io/file form-fields-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              validation (form-fields-spec/validate-form-fields-strict data)]
          (when-not (:valid? validation)
            (log/warn "form-fields.edn validation issues:"
              {:errors (:errors validation)
               :warnings (:warnings validation)}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read form-fields.edn")
      (throw (ex-info "Failed to read form fields file" {:status 500})))))

(defn write-form-fields!
  "Write form-fields map to EDN file with pretty printing"
  [form-fields]
  (let [{:keys [valid? errors warnings]}
        (form-fields-spec/validate-form-fields-strict form-fields)]
    (when-not valid?
      (log/error "Attempted to write invalid form-fields data"
        {:errors errors :warnings warnings})
      (throw (ex-info "Invalid form-fields data"
               {:status 400
                :errors errors
                :warnings warnings}))))
  (try
    (let [file (io/file form-fields-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint form-fields))))
    (catch Exception e
      (log/error e "Failed to write form-fields.edn")
      (throw (ex-info "Failed to write form fields file" {:status 500})))))

(defn read-table-columns
  "Read table-columns.edn file and parse it"
  []
  (try
    (let [file (io/file table-columns-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              validation (table-columns-spec/validate-table-columns-strict data)]
          (when-not (:valid? validation)
            (log/warn "table-columns.edn validation issues:"
              {:errors (:errors validation)
               :warnings (:warnings validation)}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read table-columns.edn")
      (throw (ex-info "Failed to read table columns file" {:status 500})))))

(defn write-table-columns!
  "Write table-columns map to EDN file with pretty printing"
  [table-columns]
  (let [{:keys [valid? errors warnings]}
        (table-columns-spec/validate-table-columns-strict table-columns)]
    (when-not valid?
      (log/error "Attempted to write invalid table-columns data"
        {:errors errors :warnings warnings})
      (throw (ex-info "Invalid table-columns data"
               {:status 400
                :errors errors
                :warnings warnings}))))
  (try
    (let [file (io/file table-columns-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint table-columns))))
    (catch Exception e
      (log/error e "Failed to write table-columns.edn")
      (throw (ex-info "Failed to write table columns file" {:status 500})))))

(defn read-user-entities
  []
  (try
    (let [file (io/file user-entities-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              validation (entities-spec/validate-user-entities data)]
          (when-not (:valid? validation)
            (log/warn "user entities.edn validation issues:"
              {:errors (:errors validation)}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read user entities.edn")
      (throw (ex-info "Failed to read user entities file" {:status 500})))))

(defn write-user-entities!
  [entities]
  (let [{:keys [valid? errors]}
        (entities-spec/validate-user-entities entities)]
    (when-not valid?
      (log/error "Attempted to write invalid user entities data" {:errors errors})
      (throw (ex-info "Invalid user entities data"
               {:status 400
                :errors errors}))))
  (try
    (let [file (io/file user-entities-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint entities))))
    (catch Exception e
      (log/error e "Failed to write user entities.edn")
      (throw (ex-info "Failed to write user entities file" {:status 500})))))

(defn read-user-view-options
  "Read user view-options.edn file and parse it.
   Validates the data and logs warnings if issues found."
  []
  (try
    (let [file (io/file user-view-options-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              {:keys [valid? errors nested-locks-errors]}
              (view-options-spec/validate-view-options-strict data)]
          (when-not valid?
            (log/warn "user view-options.edn validation issues:"
              {:errors errors :nested-locks-errors nested-locks-errors}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read user view-options.edn")
      (throw (ex-info "Failed to read user view options file" {:status 500})))))

(defn write-user-view-options!
  "Write user view-options.edn file with pretty printing.
   Validates the data before writing and throws on invalid data."
  [view-options]
  ;; Validate before writing - throw on invalid data
  (let [{:keys [valid? errors nested-locks-errors]}
        (view-options-spec/validate-view-options-strict view-options)]
    (when-not valid?
      (log/error "Attempted to write invalid user view-options data"
        {:errors errors :nested-locks-errors nested-locks-errors})
      (throw (ex-info "Invalid user view-options data"
               {:status 400
                :errors errors
                :nested-locks-errors nested-locks-errors}))))
  (try
    (let [file (io/file user-view-options-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint view-options))))
    (catch Exception e
      (log/error e "Failed to write user view-options.edn")
      (throw (ex-info "Failed to write user view options file" {:status 500})))))

(defn read-user-form-fields
  []
  (try
    (let [file (io/file user-form-fields-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              validation (form-fields-spec/validate-form-fields-strict data)]
          (when-not (:valid? validation)
            (log/warn "user form-fields.edn validation issues:"
              {:errors (:errors validation)
               :warnings (:warnings validation)}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read user form-fields.edn")
      (throw (ex-info "Failed to read user form fields file" {:status 500})))))

(defn write-user-form-fields!
  [form-fields]
  (let [{:keys [valid? errors warnings]}
        (form-fields-spec/validate-form-fields-strict form-fields)]
    (when-not valid?
      (log/error "Attempted to write invalid user form-fields data"
        {:errors errors :warnings warnings})
      (throw (ex-info "Invalid user form-fields data"
               {:status 400
                :errors errors
                :warnings warnings}))))
  (try
    (let [file (io/file user-form-fields-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint form-fields))))
    (catch Exception e
      (log/error e "Failed to write user form-fields.edn")
      (throw (ex-info "Failed to write user form fields file" {:status 500})))))

(defn read-user-table-columns
  []
  (try
    (let [file (io/file user-table-columns-path)]
      (if (.exists file)
        (let [data (edn/read-string (slurp file))
              validation (table-columns-spec/validate-table-columns-strict data)]
          (when-not (:valid? validation)
            (log/warn "user table-columns.edn validation issues:"
              {:errors (:errors validation)
               :warnings (:warnings validation)}))
          data)
        {}))
    (catch Exception e
      (log/error e "Failed to read user table-columns.edn")
      (throw (ex-info "Failed to read user table columns file" {:status 500})))))

(defn write-user-table-columns!
  [table-columns]
  (let [{:keys [valid? errors warnings]}
        (table-columns-spec/validate-table-columns-strict table-columns)]
    (when-not valid?
      (log/error "Attempted to write invalid user table-columns data"
        {:errors errors :warnings warnings})
      (throw (ex-info "Invalid user table-columns data"
               {:status 400
                :errors errors
                :warnings warnings}))))
  (try
    (let [file (io/file user-table-columns-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint table-columns))))
    (catch Exception e
      (log/error e "Failed to write user table-columns.edn")
      (throw (ex-info "Failed to write user table columns file" {:status 500})))))
