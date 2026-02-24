(ns app.shared.specs.view-options
  "Malli specs for view-options configuration files.
   
   Used by:
   - src/app/admin/frontend/config/view-options.edn (admin list pages)
   - src/app/domain/frontend/expenses/config/view-options.edn (user-facing pages)
   
   Both files share the same schema structure."
  (:require
    [malli.core :as m]
    [malli.error :as me]))

;; =============================================================================
;; Display Toggle Keys
;; =============================================================================

(def DisplayTogglesMap
  "Schema for a map of display toggles (all optional booleans, plus per-page integer)."
  [:map {:closed false}
   [:show-timestamps? {:optional true} :boolean]
   [:show-edit? {:optional true} :boolean]
   [:show-delete? {:optional true} :boolean]
   [:show-highlights? {:optional true} :boolean]
   [:show-select? {:optional true} :boolean]
   [:show-filtering? {:optional true} :boolean]
   [:show-pagination? {:optional true} :boolean]
   [:show-add-button? {:optional true} :boolean]
   [:show-batch-edit? {:optional true} :boolean]
   [:show-batch-delete? {:optional true} :boolean]
   [:show-selected-rows? {:optional true} :boolean]
   [:show-unselected-rows? {:optional true} :boolean]
   [:per-page {:optional true} [:int {:min 1 :max 1000}]]])

;; =============================================================================
;; Column Visibility Policy (defaults/locks)
;; =============================================================================

(def ColumnVisibilityMap
  "Schema for a map of column-key → boolean visibility.

   Used for policy defaults and locks:
   - :column-defaults — per-column default visibility (users can override)
   - :column-locks    — per-column locked visibility (users cannot override)"
  [:map-of :keyword :boolean])

;; =============================================================================
;; Filter Configuration
;; =============================================================================

(def FilterConfig
  "Schema for a single filter configuration."
  [:map {:closed false}
   [:type [:enum "select" "text" "boolean" "date-range"]]
   [:options {:optional true} [:vector :string]]])

(def FiltersMap
  "Schema for filters configuration (field-name → filter-config)."
  [:map-of :keyword FilterConfig])

;; =============================================================================
;; Pagination Configuration
;; =============================================================================

(def PaginationConfig
  "Schema for pagination configuration."
  [:map {:closed false}
   [:default-page-size {:optional true} [:int {:min 1 :max 1000}]]
   [:available-page-sizes {:optional true} [:vector [:int {:min 1 :max 1000}]]]])

;; =============================================================================
;; Sort Configuration
;; =============================================================================

(def SortConfig
  "Schema for default sort configuration."
  [:map {:closed false}
   [:field :string]
   [:direction [:enum "asc" "desc"]]])

;; =============================================================================
;; Entity View Options
;; =============================================================================

(def EntityViewOptions
  "Schema for a single entity's view-options configuration.
   
   Supports two schemas:
   1. New explicit schema (preferred):
      - :display-defaults - map of default toggle values
      - :display-locks - map of locked toggle values (users cannot override)
   
   2. Legacy schema (deprecated):
      - flat :show-*? keys at top level (presence = locked)"
  [:map {:closed false}
   ;; New explicit schema (Phase 2)
   [:display-defaults {:optional true} DisplayTogglesMap]
   [:display-locks {:optional true} DisplayTogglesMap]

    ;; Column visibility policy (defaults + locks)
   [:column-defaults {:optional true} ColumnVisibilityMap]
   [:column-locks {:optional true} ColumnVisibilityMap]

   ;; Legacy schema - flat display toggles at top level
   ;; These are treated as locks when present
   [:show-timestamps? {:optional true} :boolean]
   [:show-edit? {:optional true} :boolean]
   [:show-delete? {:optional true} :boolean]
   [:show-highlights? {:optional true} :boolean]
   [:show-select? {:optional true} :boolean]
   [:show-filtering? {:optional true} :boolean]
   [:show-pagination? {:optional true} :boolean]
   [:show-add-button? {:optional true} :boolean]
   [:show-batch-edit? {:optional true} :boolean]
   [:show-batch-delete? {:optional true} :boolean]
   [:show-selected-rows? {:optional true} :boolean]
   [:show-unselected-rows? {:optional true} :boolean]

   ;; Non-display view options
   [:search-fields {:optional true} [:vector :string]]
   [:filters {:optional true} FiltersMap]
   [:export-formats {:optional true} [:vector :string]]
   [:bulk-actions {:optional true} [:vector :string]]
   [:pagination {:optional true} PaginationConfig]
   [:default-sort {:optional true} SortConfig]
   [:per-page {:optional true} [:int {:min 1 :max 1000}]]])

;; =============================================================================
;; View Options File Schema
;; =============================================================================

(def ViewOptionsFile
  "Schema for the entire view-options.edn file.
   A map of entity-keyword → entity view options."
  [:map-of :keyword EntityViewOptions])

;; =============================================================================
;; Validation Functions
;; =============================================================================

(defn validate-view-options
  "Validate view-options data against the schema.
   
   Returns:
   - {:valid? true :data data} on success
   - {:valid? false :errors [...]} on failure"
  [data]
  (if (m/validate ViewOptionsFile data)
    {:valid? true :data data}
    {:valid? false
     :errors (me/humanize (m/explain ViewOptionsFile data))}))

;; =============================================================================
;; Schema Compliance Checks
;; =============================================================================

(defn check-no-nested-display-locks
  "Check that :display-locks is not nested inside :display-defaults.
   This is a common mistake that breaks the resolver.
   
   Returns nil if OK, otherwise returns a list of problematic entities."
  [data]
  (let [problems (for [[entity-key entity-opts] data
                       :when (get-in entity-opts [:display-defaults :display-locks])]
                   {:entity entity-key
                    :problem ":display-locks is nested inside :display-defaults"
                    :fix "Move :display-locks to be a sibling of :display-defaults"})]
    (when (seq problems)
      problems)))

(defn check-schema-consistency
  "Check for common schema issues.
   Returns nil if OK, otherwise returns a list of warnings/errors."
  [data]
  (let [nested-locks (check-no-nested-display-locks data)]
    (when (seq nested-locks)
      {:warnings nested-locks})))

(defn validate-view-options-strict
  "Validate view-options with additional consistency checks.
   
   Returns:
   - {:valid? true :data data} on success
   - {:valid? false :errors [...] :warnings [...]} on failure"
  [data]
  (let [schema-result (validate-view-options data)
        consistency-result (check-schema-consistency data)]
    (cond
      ;; Schema validation failed
      (not (:valid? schema-result))
      schema-result

      ;; Consistency check found issues
      consistency-result
      {:valid? false
       :errors [(str "Schema consistency issues: " (:warnings consistency-result))]
       :warnings (:warnings consistency-result)
       :data data}

      ;; All good
      :else
      schema-result)))
