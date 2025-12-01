(ns app.template.frontend.db.paths)

;; Navigation & Routing
(defn current-route
  "Returns [:current-route] path vector for the current route in the application state."
  []
  [:current-route])

(defn current-page
  "Returns [:ui :current-page] path vector for the current page in the UI state."
  []
  [:ui :current-page])

;; Entity paths
(defn entity-data
  "Returns [:entities entity-type :data] path vector for entity data of a specific entity type."
  [entity-type]
  [:entities entity-type :data])

(defn entity-ids
  "Returns [:entities entity-type :ids] path vector for entity IDs of a specific entity type."
  [entity-type]
  [:entities entity-type :ids])

(defn entity-metadata
  "Returns [:entities entity-type :metadata] path vector for entity metadata of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata])

(defn entity-loading?
  "Returns [:entities entity-type :metadata :loading?] path vector for the loading state of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :loading?])

(defn entity-error
  "Returns [:entities entity-type :metadata :error] path vector for error state of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :error])

(defn entity-last-updated
  "Returns [:entities entity-type :metadata :last-updated] path vector for the last updated timestamp of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :last-updated])

(defn entity-success
  "Returns [:entities entity-type :metadata :success] path vector for success state of a specific entity type."
  [entity-type]
  [:entities entity-type :metadata :success])

;; Form paths
(defn form-data
  "Returns [:forms entity-type :data] path vector for form data of a specific entity type."
  [entity-type]
  [:forms entity-type :data])

(defn form-field
  "Returns [:forms entity-type :data field] path vector for a specific field in the form data of an entity type."
  [entity-type field]
  [:forms entity-type :data field])

(defn form-errors
  "Returns [:forms entity-type :errors] path vector for form validation errors of a specific entity type."
  [entity-type]
  [:forms entity-type :errors])

(defn form-field-error
  "Returns [:forms entity-type :errors field] path vector for validation error of a specific field in an entity type form."
  [entity-type field]
  [:forms entity-type :errors field])

(defn form-submitting?
  "Returns [:forms entity-type :submitting?] path vector for the form submission state of a specific entity type."
  [entity-type]
  [:forms entity-type :submitting?])

(defn form-submitted?
  "Returns [:forms entity-type :submitted?] path vector for tracking if a form has been submitted."
  [entity-type]
  [:forms entity-type :submitted?])

(defn form-dirty-fields
  "Returns [:forms entity-type :dirty-fields] path vector for tracking modified fields in a form of a specific entity type."
  [entity-type]
  [:forms entity-type :dirty-fields])

(defn form-server-errors-all
  "Returns [:forms entity-type :server-errors] path vector for all server-side errors for a specific entity type form."
  [entity-type]
  [:forms entity-type :server-errors])

(defn form-server-errors
  "Returns [:forms entity-type :server-errors field] path vector for server-side errors of a specific field in an entity type form."
  [entity-type field]
  [:forms entity-type :server-errors field])

(defn form-success-all
  "Returns [:forms entity-type :success] path vector for all success states in a specific entity type form."
  [entity-type]
  [:forms entity-type :success])

(defn form-success
  "Returns [:forms entity-type :success field] path vector for success state of a specific field in an entity type form."
  [entity-type field]
  [:forms entity-type :success field])

(defn form-waiting
  "Returns [:forms entity-type :waiting] path vector for the waiting state of a specific entity type form."
  [entity-type]
  [:forms entity-type :waiting])

;; List UI paths
(defn list-ui-state
  "Returns [:ui :lists entity-type] path vector for UI state of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type])

(defn list-sort-config
  "Returns [:ui :lists entity-type :sort] path vector for sort configuration of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :sort])

(defn list-current-page
  "Returns [:ui :lists entity-type :pagination :current-page] path vector for current page number of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :pagination :current-page])

(defn list-total-items
  "Returns [:ui :lists entity-type :total-items] path vector for total items count in a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :total-items])

(defn list-per-page
  "Returns [:ui :lists entity-type :per-page] path vector for items per page setting of a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :per-page])

(defn entity-selected-ids
  "Returns [:ui :lists entity-type :selected-ids] path vector for the set of selected IDs in a list for a specific entity type."
  [entity-type]
  [:ui :lists entity-type :selected-ids])

(defn entity-display-settings
  "Returns [:ui :entity-configs entity-name] path vector for the display settings of a specific entity."
  [entity-name]
  [:ui :entity-configs entity-name])
