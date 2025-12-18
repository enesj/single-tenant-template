(ns app.template.frontend.events.user-expenses.settings
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

;; ---------------------------------------------------------------------------
;; Settings
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-settings
  common-interceptors
  (fn [{:keys [db]} _]
    ;; TODO: Implement actual settings fetch
    {:db (-> db
           (assoc-in [:user-expenses :settings :loading?] true)
           (assoc-in [:user-expenses :settings :data] {:default_currency "BAM"
                                                       :notifications_enabled true})
           (assoc-in [:user-expenses :settings :loading?] false))}))

(rf/reg-event-fx
  :user-expenses/save-settings
  common-interceptors
  (fn [{:keys [db]} [settings-data]]
    ;; TODO: Implement actual settings save
    {:db (-> db
           (assoc-in [:user-expenses :settings :saving?] true)
           (assoc-in [:user-expenses :settings :data] settings-data))
     :dispatch-later [{:ms 500
                       :dispatch [:user-expenses/save-settings-success settings-data]}]}))

(rf/reg-event-db
  :user-expenses/save-settings-success
  common-interceptors
  (fn [db [_settings]]
    (assoc-in db [:user-expenses :settings :saving?] false)))

