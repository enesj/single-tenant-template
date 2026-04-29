(ns app.admin.frontend.events.settings.subs
  (:require
    [app.admin.frontend.events.settings.utils :as utils]
    [re-frame.core :as rf]))

;; =============================================================================
;; Derived View Options State
;; =============================================================================

(rf/reg-sub
  :app.admin.frontend.events.settings/view-options-dirty?
  (fn [db _]
    (or
      (not= (utils/safe-map (get-in db [:admin :settings :view-options]))
        (utils/safe-map (get-in db [:admin :settings :view-options-saved])))
      (not= (utils/safe-map (get-in db [:admin :settings :navigation]))
        (utils/safe-map (get-in db [:admin :settings :navigation-saved]))))))

;; =============================================================================
;; Subscriptions
;; =============================================================================

(rf/reg-sub
  :app.admin.frontend.events.settings/loading?
  (fn [db _]
    (get-in db [:admin :settings :loading?] false)))

(rf/reg-sub
  :app.admin.frontend.events.settings/saving?
  (fn [db _]
    (get-in db [:admin :settings :saving?] false)))

(rf/reg-sub
  :app.admin.frontend.events.settings/error
  (fn [db _]
    (get-in db [:admin :settings :error])))

(rf/reg-sub
  :app.admin.frontend.events.settings/form-fields
  (fn [db _]
    (get-in db [:admin :settings :form-fields] {})))

(rf/reg-sub
  :app.admin.frontend.events.settings/table-columns
  (fn [db _]
    (get-in db [:admin :settings :table-columns] {})))

(rf/reg-sub
  :app.admin.frontend.events.settings/navigation
  (fn [db _]
    (get-in db [:admin :settings :navigation] {})))

(rf/reg-sub
  :app.admin.frontend.events.settings/config-tab
  (fn [db _]
    (get-in db [:admin :settings :config-tab] "view-options")))



