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
;; Advanced Configuration
;; =============================================================================

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


