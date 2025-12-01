(ns app.admin.frontend.handlers.generic
  "Generic admin entity handlers and hooks (single-tenant, no deletion constraints)."
  (:require
    [app.admin.frontend.security.wrapper :as security]
    [re-frame.core :as rf :refer [dispatch]]
    [uix.core :refer [use-effect]]))

(def ^:private show-batch-events
  {:audit-logs :admin/show-batch-audit-actions
   :users :admin/show-batch-user-actions})

(def ^:private hide-batch-events
  {:audit-logs :admin/hide-batch-audit-actions
   :users :admin/hide-batch-user-actions})

(rf/reg-event-db
  :admin/show-batch-actions
  (fn [db [_ entity-key]]
    (assoc-in db [:admin :batch-operations entity-key :visible?] true)))

(rf/reg-event-db
  :admin/hide-batch-actions
  (fn [db [_ entity-key]]
    (assoc-in db [:admin :batch-operations entity-key :visible?] false)))

(rf/reg-sub
  :admin/batch-actions-visible?
  (fn [db [_ entity-key]]
    (get-in db [:admin :batch-operations entity-key :visible?] false)))

(defn create-generic-selection-handler
  "Create entity-specific selection change handler."
  [entity-key]
  (fn [selected-ids]
    (if (seq selected-ids)
      (do
        (when-let [legacy-event (get show-batch-events entity-key)]
          (dispatch [legacy-event]))
        (dispatch [:admin/show-batch-actions entity-key]))
      (do
        (when-let [legacy-event (get hide-batch-events entity-key)]
          (dispatch [legacy-event]))
        (dispatch [:admin/hide-batch-actions entity-key])))))

(defn create-generic-additional-effects
  "Create entity-specific additional effects (e.g., security wrapper)."
  [entity-config]
  (let [{:keys [features]} entity-config]
    (fn []
      (when (true? (:security-wrapper? features))
        (security/init-security-wrapper!)))))

(defn use-deletion-constraints
  "Single-tenant has no deletion-constraint checks; keep hook signature for compatibility."
  [_entity-key _entity-ids _enabled?]
  (use-effect (fn [] js/undefined) []))
