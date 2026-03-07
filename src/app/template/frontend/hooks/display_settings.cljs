(ns app.template.frontend.hooks.display-settings
  "Unified hook for display settings that provides a single source of truth.

   This hook encapsulates all reactive display-setting reads used by list cells.
   It also applies any runtime selection gate configured in view-options list-config,
   so reactive selection cells stay aligned with the list-view container."
  (:require
    [app.domain.frontend.expenses.authz :as expenses-authz]
    [app.template.frontend.subs.ui :as ui-subs]
    [uix.re-frame :refer [use-subscribe]]))

(defn- normalize-gate-id
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else nil))

(defn- gate-allows-action?
  [gate-id {:keys [expenses-role can-write? power-user?]}]
  (let [gate-id (normalize-gate-id gate-id)]
    (cond
      (nil? gate-id) true
      (= gate-id :expenses/can-write) (boolean can-write?)
      (= gate-id :expenses/power-user) (boolean power-user?)
      (expenses-authz/can? expenses-role gate-id) true
      :else false)))

(defn use-display-settings
  "Returns effective display settings for an entity.

   Selection visibility is additionally constrained by any configured :select
   action gate from list-config. Other action gates are applied by list-view
   itself because they interact with hide/disable action-mode semantics."
  [entity-name]
  (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
        settings (use-subscribe [::ui-subs/entity-display-settings entity-key])
        list-config (use-subscribe [::ui-subs/entity-list-config entity-key])
        expenses-role (use-subscribe [:expenses/user-role])
        can-write? (use-subscribe [:expenses/can-write?])
        power-user? (use-subscribe [:expenses/power-user?])
        gate-ctx {:expenses-role expenses-role
                  :can-write? can-write?
                  :power-user? power-user?}
        select-allowed? (gate-allows-action?
                          (get-in list-config [:action-gates :select])
                          gate-ctx)]
    (cond-> settings
      (false? select-allowed?) (assoc :show-select? false))))
