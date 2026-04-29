(ns app.admin.frontend.subs.config
  "Simplified subscriptions for vector-based column configuration"
  (:require
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.settings.resolver :as resolver]
    [re-frame.core :as rf]))

;; =============================================================================
;; Core Column Configuration (Vector-based with Order Preservation)
;; =============================================================================

;; Get the resolved table-columns config for an entity (route-aware).
(rf/reg-sub
  ::entity-config
  (fn [db [_ entity-name]]
    (let [entity-kw (model-naming/ensure-app-keyword entity-name)
          admin-route? (paths/admin-route? db)]
      (when entity-kw
        (resolver/resolve-config-source
          admin-route?
          (get-in db [:admin :config :table-columns entity-kw])
          (get-in db [:domain :config :table-columns entity-kw]))))))

;; Admin entity metadata comes from the entity registry (preloaded from entities.edn)
(rf/reg-sub
  :admin/all-entity-configs
  (fn [_ _]
    @entity-registry/registered-entities))

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

(rf/reg-sub
  :admin/navigation
  (fn [db _]
    (get-in db [:admin :config :navigation] {})))

;; =============================================================================
;; Advanced Configuration
;; =============================================================================

(rf/reg-sub
  :admin/sortable-columns
  (fn [db [_ entity-name]]
    (let [entity-kw (model-naming/ensure-app-keyword entity-name)
          admin-route? (paths/admin-route? db)
          table-config (when entity-kw
                         (resolver/resolve-config-source
                           admin-route?
                           (get-in db [:admin :config :table-columns entity-kw])
                           (get-in db [:domain :config :table-columns entity-kw])))]
      (:sortable-columns table-config []))))

;; =============================================================================
;; View Options / Hardcoded Display Settings
;; =============================================================================


