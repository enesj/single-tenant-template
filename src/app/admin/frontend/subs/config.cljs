(ns app.admin.frontend.subs.config
  "Simplified subscriptions for vector-based column configuration"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [re-frame.core :as rf]))

;; =============================================================================
;; Core Column Configuration (Vector-based with Order Preservation)
;; =============================================================================

;; Get the entire config for an entity
(rf/reg-sub
  ::entity-config
  (fn [db [_ entity-name]]
    (get-in db [:admin :config :table-columns entity-name])))

;; Get visible columns as a vector (maintains order!)
(rf/reg-sub
  ::visible-columns
  (fn [db [_ entity-name]]
    (or (get-in db [:admin :config :table-columns entity-name :visible-columns])
       ;; Fallback to default if not set
      (get-in db [:admin :config :table-columns entity-name :default-visible-columns])
      [])))

;; Get all columns for an entity (ordered)
(rf/reg-sub
  ::all-columns
  (fn [db [_ entity-name]]
    (vec (get-in db [:admin :config :table-columns entity-name :available-columns] []))))

;; Get default visible columns (as vector)
(rf/reg-sub
  ::default-visible-columns
  (fn [db [_ entity-name]]
    (get-in db [:admin :config :table-columns entity-name :default-visible-columns] [])))

;; =============================================================================
;; Column Metadata and Labels
;; =============================================================================

;; Get column metadata (labels, types, etc.)
(rf/reg-sub
  ::column-metadata
  (fn [db [_ entity-name column-name]]
    (get-in db [:admin :config :table-columns entity-name :column-metadata column-name])))

;; Admin entity metadata comes from the entity registry (preloaded from entities.edn)
(rf/reg-sub
  :admin/all-entity-configs
  (fn [_ _]
    (try
      @entity-registry/registered-entities
      (catch :default _
        {}))))

(rf/reg-sub
  :admin/entity-config
  (fn [[_ _entity-keyword]]
    (rf/subscribe [:admin/all-entity-configs]))
  (fn [all-configs [_ entity-keyword]]
    (get all-configs entity-keyword)))

;; =============================================================================
;; Column Statistics and Utilities
;; =============================================================================

;; =============================================================================
;; Configuration Loading State
;; =============================================================================

(rf/reg-sub
  :admin/config-loaded?
  (fn [db _]
    (boolean (:admin/config-loaded? db))))

(rf/reg-sub
  :admin/config-loading?
  (fn [db _]
    (boolean (:admin/config-loading? db))))

;; =============================================================================
;; Entity Specs (Simplified)
;; =============================================================================

;; Removed legacy fixed proxies (:entity-specs/<entity>, :form-entity-specs/<entity>).
;; Admin pages should use [:admin/entity-specs-by-name <entity>] which is generated
;; from the vector-config and kept in sync with settings/toggles.

;; =============================================================================
;; Advanced Configuration
;; =============================================================================

(rf/reg-sub
  ::filterable-columns
  (fn [[_ entity-name]]
    (rf/subscribe [::entity-config entity-name]))
  (fn [config _]
    (:filterable-columns config [])))

(rf/reg-sub
  ::sortable-columns
  (fn [[_ entity-name]]
    (rf/subscribe [::entity-config entity-name]))
  (fn [config _]
    (:sortable-columns config [])))

(rf/reg-sub
  :admin/sortable-columns
  (fn [[_ entity-keyword]]
    (rf/subscribe [::sortable-columns entity-keyword]))
  (fn [sortable-columns _]
    sortable-columns))

;; =============================================================================
;; View Options / Hardcoded Display Settings
;; =============================================================================

(rf/reg-sub
  :admin/all-view-options
  (fn [_ _]
    ;; View options are stored in config-cache atom, not re-frame db
    (config-loader/get-all-view-options)))

(rf/reg-sub
  :admin/view-options
  (fn [[_ _entity-keyword]]
    (rf/subscribe [:admin/all-view-options]))
  (fn [all-view-options [_ entity-keyword]]
    (get all-view-options entity-keyword)))
