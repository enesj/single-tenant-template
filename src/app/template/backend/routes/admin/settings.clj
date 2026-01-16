(ns app.template.backend.routes.admin.settings
  "Admin settings API - HTTP handlers"
  (:require
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [app.template.backend.routes.admin.utils :as utils]
    [taoensso.timbre :as log]))

(defn- display-setting-key?
  "True when the key represents one of the list-view display toggles.

  In the new schema these are stored under :display-locks (and optionally :display-defaults).
  This keeps admin settings and user settings aligned."
  [k]
  (and (keyword? k)
    (re-matches #"show-.*\?" (name k))))

(defn get-view-options-handler
  "GET handler - return all view options"
  [_db]
  (utils/with-error-handling
    (fn [_request]
      (let [view-options (settings-io/read-view-options)]
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
            (settings-io/write-view-options! new-view-options)
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
            entity-name (keyword (:entity-name body))
            setting-key (keyword (:setting-key body))
            setting-value (if (contains? body :setting-value)
                            (:setting-value body)
                            ::not-found)
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)
            _ (log/info "Parsed values:" {:entity-name entity-name
                                          :setting-key setting-key
                                          :setting-value setting-value})]
        (if (and entity-name setting-key (not= setting-value ::not-found))
          (let [current-options (settings-io/read-view-options)
                updated-options (if (display-setting-key? setting-key)
                                  (assoc-in current-options [entity-name :display-locks setting-key] setting-value)
                                  (assoc-in current-options [entity-name setting-key] setting-value))]
            ;; Log the action
            (utils/log-admin-action-with-context
              "update-entity-setting"
              admin-id
              "settings"
              nil
              {:entity entity-name
               :setting setting-key
               :old-value (if (display-setting-key? setting-key)
                             (get-in current-options [entity-name :display-locks setting-key])
                             (get-in current-options [entity-name setting-key]))
               :new-value setting-value}
              (:ip-address context)
              (:user-agent context))
            ;; Write to file
            (settings-io/write-view-options! updated-options)
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
            entity-name (keyword (:entity-name body))
            setting-key (keyword (:setting-key body))
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if (and entity-name setting-key)
          (let [current-options (settings-io/read-view-options)
                display? (display-setting-key? setting-key)
                old-value (if display?
                            (get-in current-options [entity-name :display-locks setting-key])
                            (get-in current-options [entity-name setting-key]))
                updated-options (if display?
                                  (update-in current-options [entity-name :display-locks] dissoc setting-key)
                                  (update current-options entity-name dissoc setting-key))]
            ;; Log the action
            (utils/log-admin-action-with-context
              "remove-entity-setting"
              admin-id
              "settings"
              nil
              {:entity entity-name
               :setting setting-key
               :old-value old-value}
              (:ip-address context)
              (:user-agent context))
            ;; Write to file
            (settings-io/write-view-options! updated-options)
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
      (let [form-fields (settings-io/read-form-fields)]
        (utils/json-response {:form-fields form-fields})))
    "Failed to read form fields"))

(defn update-form-fields-entity-handler
  "PATCH handler - update a single entity's form fields config"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            entity-name (keyword (:entity-name body))
            ;; Accept full entity config or individual field updates
            entity-config (:entity-config body)
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if (and entity-name entity-config)
          (let [current-config (settings-io/read-form-fields)
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
            (settings-io/write-form-fields! updated-config)
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
      (let [table-columns (settings-io/read-table-columns)]
        (utils/json-response {:table-columns table-columns})))
    "Failed to read table columns"))

(defn update-table-columns-entity-handler
  "PATCH handler - update a single entity's table columns config"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            entity-name (keyword (:entity-name body))
            ;; Accept full entity config
            entity-config (:entity-config body)
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (if (and entity-name entity-config)
          (let [current-config (settings-io/read-table-columns)
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
            (settings-io/write-table-columns! updated-config)
            (utils/success-response {:message "Table columns updated successfully"
                                     :entity entity-name
                                     :config entity-config}))
          (utils/error-response "Missing required fields: entity-name, entity-config" :status 400))))
    "Failed to update table columns"))

;; =============================================================================
;; User UI Config (Domain-Owned) Handlers
;; =============================================================================

(defn get-user-ui-config-handler
  "GET handler - return all user-facing (domain-owned) UI config"
  [_db]
  (utils/with-error-handling
    (fn [_request]
      (utils/json-response
        {:entities (settings-io/read-user-entities)
         :view-options (settings-io/read-user-view-options)
         :form-fields (settings-io/read-user-form-fields)
         :table-columns (settings-io/read-user-table-columns)}))
    "Failed to read user UI config"))

(defn update-user-ui-config-handler
  "PUT handler - update user-facing (domain-owned) UI config.

  Accepts any subset of:
  - :entities
  - :view-options
  - :form-fields
  - :table-columns"
  [_db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            entities (:entities body)
            view-options (:view-options body)
            form-fields (:form-fields body)
            table-columns (:table-columns body)
            admin-id (utils/get-admin-id request)
            context (utils/extract-request-context request)]
        (when-not (or entities view-options form-fields table-columns)
          (throw (ex-info "No config provided" {:status 400})))

        (utils/log-admin-action-with-context
          "update-user-ui-config"
          admin-id
          "user-ui-settings"
          nil
          {:updated-keys (->> {:entities (boolean entities)
                               :view-options (boolean view-options)
                               :form-fields (boolean form-fields)
                               :table-columns (boolean table-columns)}
                           (filter (comp true? val))
                           (map key)
                           vec)}
          (:ip-address context)
          (:user-agent context))

        (when entities (settings-io/write-user-entities! entities))
        (when view-options (settings-io/write-user-view-options! view-options))
        (when form-fields (settings-io/write-user-form-fields! form-fields))
        (when table-columns (settings-io/write-user-table-columns! table-columns))

        (utils/success-response
          {:message "User UI config updated successfully"
           :entities (or entities (settings-io/read-user-entities))
           :view-options (or view-options (settings-io/read-user-view-options))
           :form-fields (or form-fields (settings-io/read-user-form-fields))
           :table-columns (or table-columns (settings-io/read-user-table-columns))})))
    "Failed to update user UI config"))

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
   ["/table-columns/entity" {:patch (update-table-columns-entity-handler db)}]

   ;; User UI (domain-owned)
   ["/user-ui-config" {:get (get-user-ui-config-handler db)
                       :put (update-user-ui-config-handler db)}]])
