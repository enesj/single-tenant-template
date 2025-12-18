(ns app.admin.frontend.events.settings.ui
  (:require
    [app.admin.frontend.events.settings.utils :as utils]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Toggle Editing Mode
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/toggle-editing
  (fn [{:keys [db]} _]
    (let [current (get-in db [:admin :settings :editing?] false)
          new-val (not current)
          ;; When leaving edit mode, discard any staged view-options changes.
          db' (if (false? new-val)
                (let [saved (utils/safe-map (get-in db [:admin :settings :view-options-saved]))]
                  (assoc-in db [:admin :settings :view-options] saved))
                db)]
      (log/info "Toggle editing" {:current current :new-val new-val})
      {:db (assoc-in db' [:admin :settings :editing?] new-val)})))

;; =============================================================================
;; Active Config Tab
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/set-config-tab
  (fn [{:keys [db]} [_ tab]]
    {:db (assoc-in db [:admin :settings :config-tab] tab)}))

;; =============================================================================
;; Active Domain Tab
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/set-domain-tab
  (fn [{:keys [db]} [_ tab]]
    {:db (assoc-in db [:admin :settings :domain-tab] tab)}))

