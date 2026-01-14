(ns app.template.backend.routes.admin.settings-io
  "Admin settings I/O - reading and writing configuration files.
   
   Supports both admin-owned config (under admin/frontend/config) and
   domain-owned admin config (under domain/**/admin/config).
   
   Admin runtime settings (view-options, table-columns, form-fields) are
   merged from both admin core files and domain-specific files."
  (:require
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [app.domain.backend.registry :as domain-registry]
    [taoensso.timbre :as log]))

;; Admin-owned UI config paths (hardcoded, template-owned)
(def ^:private view-options-path "src/app/admin/frontend/config/view-options.edn")
(def ^:private form-fields-path "src/app/admin/frontend/config/form-fields.edn")
(def ^:private table-columns-path "src/app/admin/frontend/config/table-columns.edn")

(defn- read-domain-admin-configs
  "Read and merge all domain admin config files of a given type.
   config-key is one of :view-options, :form-fields, :table-columns"
  [config-key]
  (let [domain-admin-config-paths (domain-registry/get-admin-ui-config-paths)]
    (reduce
      (fn [acc domain-paths]
        (merge acc (frontend-config-io/read-edn-or-empty (get domain-paths config-key)
                     {:log-message "Failed to read domain admin config EDN"
                      :log-context {:scope :admin-settings
                                    :config config-key}})))
      {}
      domain-admin-config-paths)))

;; User-facing (domain-owned) UI config - paths come from domain registry.
;; Use domain-registry/primary-user-ui-config-paths for backwards compatibility.
(defn- get-user-config-paths []
  (domain-registry/primary-user-ui-config-paths))

(defn- user-entities-path []
  (get (get-user-config-paths) :entities))

(defn- user-view-options-path []
  (get (get-user-config-paths) :view-options))

(defn- user-form-fields-path []
  (get (get-user-config-paths) :form-fields))

(defn- user-table-columns-path []
  (get (get-user-config-paths) :table-columns))

(defn read-view-options
  "Read view-options from admin core file and merge with domain admin configs.
   Validates the merged content against the Malli spec."
  []
  (try
    (let [admin-data (frontend-config-io/read-edn-or-throw view-options-path
                       {:log-message "Failed to read admin view-options.edn"
                        :log-context {:scope :admin-settings
                                      :config :view-options}})
          domain-data (read-domain-admin-configs :view-options)
          ;; Merge: admin settings override domain defaults (runtime overlay)
          merged-data (merge domain-data admin-data)
          validation (view-options-spec/validate-view-options-strict merged-data)]
      (when-not (:valid? validation)
        (log/warn "merged view-options validation issues:"
          {:errors (:errors validation)
           :warnings (:warnings validation)}))
      merged-data)
    (catch Exception e
      (log/error e "Failed to read view-options")
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
    (frontend-config-io/write-edn-pretty! view-options-path view-options)
    (catch Exception e
      (log/error e "Failed to write view-options.edn")
      (throw (ex-info "Failed to write settings file" {:status 500})))))

(defn read-form-fields
  "Read form-fields from admin core file and merge with domain admin configs.
   Validates the merged content."
  []
  (try
    (let [admin-data (frontend-config-io/read-edn-or-throw form-fields-path
                       {:log-message "Failed to read admin form-fields.edn"
                        :log-context {:scope :admin-settings
                                      :config :form-fields}})
          domain-data (read-domain-admin-configs :form-fields)
          ;; Merge: admin settings override domain defaults (runtime overlay)
          merged-data (merge domain-data admin-data)
          validation (form-fields-spec/validate-form-fields-strict merged-data)]
      (when-not (:valid? validation)
        (log/warn "merged form-fields validation issues:"
          {:errors (:errors validation)
           :warnings (:warnings validation)}))
      merged-data)
    (catch Exception e
      (log/error e "Failed to read form-fields")
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
    (frontend-config-io/write-edn-pretty! form-fields-path form-fields)
    (catch Exception e
      (log/error e "Failed to write form-fields.edn")
      (throw (ex-info "Failed to write form fields file" {:status 500})))))

(defn read-table-columns
  "Read table-columns from admin core file and merge with domain admin configs.
   Validates the merged content."
  []
  (try
    (let [admin-data (frontend-config-io/read-edn-or-throw table-columns-path
                       {:log-message "Failed to read admin table-columns.edn"
                        :log-context {:scope :admin-settings
                                      :config :table-columns}})
          domain-data (read-domain-admin-configs :table-columns)
          ;; Merge: admin settings override domain defaults (runtime overlay)
          merged-data (merge domain-data admin-data)
          validation (table-columns-spec/validate-table-columns-strict merged-data)]
      (when-not (:valid? validation)
        (log/warn "merged table-columns validation issues:"
          {:errors (:errors validation)
           :warnings (:warnings validation)}))
      merged-data)
    (catch Exception e
      (log/error e "Failed to read table-columns")
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
    (frontend-config-io/write-edn-pretty! table-columns-path table-columns)
    (catch Exception e
      (log/error e "Failed to write table-columns.edn")
      (throw (ex-info "Failed to write table columns file" {:status 500})))))

(defn read-user-entities
  []
  (try
    (frontend-config-io/read-edn-or-throw+validate
      {:config-key :entities
       :path (user-entities-path)
       :validate-fn entities-spec/validate-user-entities
       :log-message "Failed to read user entities.edn"
       :log-context {:scope :user-ui-config}})
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
    (frontend-config-io/write-edn-pretty! (user-entities-path) entities)
    (catch Exception e
      (log/error e "Failed to write user entities.edn")
      (throw (ex-info "Failed to write user entities file" {:status 500})))))

(defn read-user-view-options
  "Read user view-options.edn file and parse it.
   Validates the data and logs warnings if issues found."
  []
  (try
    (frontend-config-io/read-edn-or-throw+validate
      {:config-key :view-options
       :path (user-view-options-path)
       :validate-fn view-options-spec/validate-view-options-strict
       :log-message "Failed to read user view-options.edn"
       :log-context {:scope :user-ui-config}})
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
    (frontend-config-io/write-edn-pretty! (user-view-options-path) view-options)
    (catch Exception e
      (log/error e "Failed to write user view-options.edn")
      (throw (ex-info "Failed to write user view options file" {:status 500})))))

(defn read-user-form-fields
  []
  (try
    (frontend-config-io/read-edn-or-throw+validate
      {:config-key :form-fields
       :path (user-form-fields-path)
       :validate-fn form-fields-spec/validate-form-fields-strict
       :log-message "Failed to read user form-fields.edn"
       :log-context {:scope :user-ui-config}})
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
    (frontend-config-io/write-edn-pretty! (user-form-fields-path) form-fields)
    (catch Exception e
      (log/error e "Failed to write user form-fields.edn")
      (throw (ex-info "Failed to write user form fields file" {:status 500})))))

(defn read-user-table-columns
  []
  (try
    (frontend-config-io/read-edn-or-throw+validate
      {:config-key :table-columns
       :path (user-table-columns-path)
       :validate-fn table-columns-spec/validate-table-columns-strict
       :log-message "Failed to read user table-columns.edn"
       :log-context {:scope :user-ui-config}})
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
    (frontend-config-io/write-edn-pretty! (user-table-columns-path) table-columns)
    (catch Exception e
      (log/error e "Failed to write user table-columns.edn")
      (throw (ex-info "Failed to write user table columns file" {:status 500})))))
