(ns app.admin.frontend.events.user-settings.tabs
  (:require
    [re-frame.core :as rf]))

;; =============================================================================
;; Tabs
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-tab
  (fn [db [_ tab]]
    (assoc-in db [:admin :user-settings :tab] tab)))

