(ns app.backend.routes.admin.settings
  "Admin settings API - read/write view-options.edn"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [taoensso.timbre :as log]))

(def ^:private view-options-path "resources/public/admin/ui-config/view-options.edn")
(def ^:private form-fields-path "resources/public/admin/ui-config/form-fields.edn")
(def ^:private table-columns-path "resources/public/admin/ui-config/table-columns.edn")

(defn- read-view-options
  "Read view-options.edn file and parse it"
  []
  (try
    (let [file (io/file view-options-path)]
      (if (.exists file)
        (edn/read-string (slurp file))
        {}))
    (catch Exception e
      (log/error e "Failed to read view-options.edn")
      (throw (ex-info "Failed to read settings file" {:status 500})))))

(defn- write-view-options!
  "Write view-options map to EDN file with pretty printing"
  [view-options]
  (try
    (let [file (io/file view-options-path)]
      ;; Ensure parent directory exists
      (io/make-parents file)
      ;; Write with pretty printing for readability
      (spit file (with-out-str (pprint/pprint view-options))))
    (catch Exception e
      (log/error e "Failed to write view-options.edn")
      (throw (ex-info "Failed to write settings file" {:status 500})))))

;; =============================================================================
;; Form Fields Config Read/Write
;; =============================================================================

(defn- read-form-fields
  "Read form-fields.edn file and parse it"
  []
  (try
    (let [file (io/file form-fields-path)]
      (if (.exists file)
        (edn/read-string (slurp file))
        {}))
    (catch Exception e
      (log/error e "Failed to read form-fields.edn")
      (throw (ex-info "Failed to read form fields file" {:status 500})))))

(defn- write-form-fields!
  "Write form-fields map to EDN file with pretty printing"
  [form-fields]
  (try
    (let [file (io/file form-fields-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint form-fields))))
    (catch Exception e
      (log/error e "Failed to write form-fields.edn")
      (throw (ex-info "Failed to write form fields file" {:status 500})))))

;; =============================================================================
;; Table Columns Config Read/Write
;; =============================================================================

(defn- read-table-columns
  "Read table-columns.edn file and parse it"
  []
  (try
    (let [file (io/file table-columns-path)]
      (if (.exists file)
        (edn/read-string (slurp file))
        {}))
    (catch Exception e
      (log/error e "Failed to read table-columns.edn")
      (throw (ex-info "Failed to read table columns file" {:status 500})))))

(defn- write-table-columns!
  "Write table-columns map to EDN file with pretty printing"
  [table-columns]
  (try
    (let [file (io/file table-columns-path)]
      (io/make-parents file)
      (spit file (with-out-str (pprint/pprint table-columns))))
    (catch Exception e
      (log/error e "Failed to write table-columns.edn")
      (throw (ex-info "Failed to write table columns file" {:status 500})))))

(defn get-view-options-handler
  "GET handler - return all view options"
  [_db]
  (utils/with-error-handling
    (fn [_request]
      (let [view-options (read-view-options)]
        (utils/json-response {:view-options view-options})))
    "Failed to read view options"))

(defn update-view-options-handler
  "PUT handler - update all view options"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            new-view-options (:view-options body)
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if new-view-options
          (do
            ;; Log the action
            (utils/log-admin-action-with-context
              "update-view-options"
              admin-id
              "settings"
              nil
              {:changes new-view-options}
              (:ip-address context)
              (:user-agent context))
            ;; Write to file
            (write-view-options! new-view-options)
            (utils/success-response {:message "View options updated successfully"
                                     :view-options new-view-options}))
          (utils/error-response "Missing view-options in request body" :status 400))))
    "Failed to update view options"))

(defn update-entity-setting-handler
  "PATCH handler - update a single entity's setting"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            _ (log/info "PATCH request body:" body)
            ;; Support both dash and underscore versions for flexibility
            entity-name (keyword (or (:entity-name body) (:entity_name body)))
            setting-key (keyword (or (:setting-key body) (:setting_key body)))
            setting-value (if (contains? body :setting-value)
                            (:setting-value body)
                            (if (contains? body :setting_value)
                              (:setting_value body)
                              ::not-found))
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)
            _ (log/info "Parsed values:" {:entity-name entity-name
                                          :setting-key setting-key
                                          :setting-value setting-value})]
        (if (and entity-name setting-key (not= setting-value ::not-found))
          (let [current-options (read-view-options)
                updated-options (assoc-in current-options [entity-name setting-key] setting-value)]
            ;; Log the action
            (utils/log-admin-action-with-context
              "update-entity-setting"
              admin-id
              "settings"
              nil
              {:entity entity-name
               :setting setting-key
               :old-value (get-in current-options [entity-name setting-key])
               :new-value setting-value}
              (:ip-address context)
              (:user-agent context))
            ;; Write to file
            (write-view-options! updated-options)
            (utils/success-response {:message "Setting updated successfully"
                                     :entity entity-name
                                     :setting setting-key
                                     :value setting-value}))
          (utils/error-response "Missing required fields: entity-name, setting-key, setting-value" :status 400))))
    "Failed to update entity setting"))

(defn remove-entity-setting-handler
  "DELETE handler - remove a setting from an entity (makes it user-configurable)"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            ;; Support both dash and underscore versions for flexibility
            entity-name (keyword (or (:entity-name body) (:entity_name body)))
            setting-key (keyword (or (:setting-key body) (:setting_key body)))
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if (and entity-name setting-key)
          (let [current-options (read-view-options)
                entity-settings (get current-options entity-name {})
                updated-entity-settings (dissoc entity-settings setting-key)
                updated-options (assoc current-options entity-name updated-entity-settings)]
            ;; Log the action
            (utils/log-admin-action-with-context
              "remove-entity-setting"
              admin-id
              "settings"
              nil
              {:entity entity-name
               :setting setting-key
               :old-value (get-in current-options [entity-name setting-key])}
              (:ip-address context)
              (:user-agent context))
            ;; Write to file
            (write-view-options! updated-options)
            (utils/success-response {:message "Setting removed successfully"
                                     :entity entity-name
                                     :setting setting-key}))
          (utils/error-response "Missing required fields: entity-name, setting-key" :status 400))))
    "Failed to remove entity setting"))

;; =============================================================================
;; Form Fields Handlers
;; =============================================================================

(defn get-form-fields-handler
  "GET handler - return all form fields config"
  [_db]
  (utils/with-error-handling
    (fn [_request]
      (let [form-fields (read-form-fields)]
        (utils/json-response {:form-fields form-fields})))
    "Failed to read form fields"))

(defn update-form-fields-entity-handler
  "PATCH handler - update a single entity's form fields config"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            entity-name (keyword (or (:entity-name body) (:entity_name body)))
            ;; Accept full entity config or individual field updates
            entity-config (or (:entity-config body) (:entity_config body))
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if (and entity-name entity-config)
          (let [current-config (read-form-fields)
                ;; Merge with existing config or replace entirely
                updated-config (assoc current-config entity-name entity-config)]
            (utils/log-admin-action-with-context
              "update-form-fields"
              admin-id
              "settings"
              nil
              {:entity entity-name
               :old-config (get current-config entity-name)
               :new-config entity-config}
              (:ip-address context)
              (:user-agent context))
            (write-form-fields! updated-config)
            (utils/success-response {:message "Form fields updated successfully"
                                     :entity entity-name
                                     :config entity-config}))
          (utils/error-response "Missing required fields: entity-name, entity-config" :status 400))))
    "Failed to update form fields"))

;; =============================================================================
;; Table Columns Handlers
;; =============================================================================

(defn get-table-columns-handler
  "GET handler - return all table columns config"
  [_db]
  (utils/with-error-handling
    (fn [_request]
      (let [table-columns (read-table-columns)]
        (utils/json-response {:table-columns table-columns})))
    "Failed to read table columns"))

(defn update-table-columns-entity-handler
  "PATCH handler - update a single entity's table columns config"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            entity-name (keyword (or (:entity-name body) (:entity_name body)))
            ;; Accept full entity config
            entity-config (or (:entity-config body) (:entity_config body))
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if (and entity-name entity-config)
          (let [current-config (read-table-columns)
                updated-config (assoc current-config entity-name entity-config)]
            (utils/log-admin-action-with-context
              "update-table-columns"
              admin-id
              "settings"
              nil
              {:entity entity-name
               :old-config (get current-config entity-name)
               :new-config entity-config}
              (:ip-address context)
              (:user-agent context))
            (write-table-columns! updated-config)
            (utils/success-response {:message "Table columns updated successfully"
                                     :entity entity-name
                                     :config entity-config}))
          (utils/error-response "Missing required fields: entity-name, entity-config" :status 400))))
    "Failed to update table columns"))

;; Route definitions
(defn routes
  "Settings route definitions"
  [db]
  ["/settings"
   ;; View options (existing)
   ["" {:get (get-view-options-handler db)
        :put (update-view-options-handler db)}]
   ["/entity" {:patch (update-entity-setting-handler db)
               :delete (remove-entity-setting-handler db)}]
   ;; Form fields config
   ["/form-fields" {:get (get-form-fields-handler db)}]
   ["/form-fields/entity" {:patch (update-form-fields-entity-handler db)}]
   ;; Table columns config
   ["/table-columns" {:get (get-table-columns-handler db)}]
   ["/table-columns/entity" {:patch (update-table-columns-entity-handler db)}]])
