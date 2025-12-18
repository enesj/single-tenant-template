(ns app.domain.frontend.expenses.events.user-expenses.settings
  "User expense settings events."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Fetch user settings
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-settings
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:user-expenses :settings :loading?] true)
           (assoc-in [:user-expenses :settings :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/settings-endpoint
                    :admin-uri endpoints/admin-settings-endpoint
                    :on-success [:user-expenses/fetch-settings-success]
                    :on-failure [:user-expenses/fetch-settings-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-settings-success
  common-interceptors
  (fn [db [response]]
    (let [settings (or (:data response) (:settings response) response)]
      (-> db
        (assoc-in [:user-expenses :settings :data] settings)
        (assoc-in [:user-expenses :settings :loading?] false)
        (assoc-in [:user-expenses :settings :error] nil)))))

(rf/reg-event-db
  :user-expenses/fetch-settings-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch user settings" {:error error})
    (-> db
      (assoc-in [:user-expenses :settings :loading?] false)
      (assoc-in [:user-expenses :settings :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Update user settings
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/update-settings
  common-interceptors
  (fn [{:keys [db]} [settings-data]]
    {:db (-> db
           (assoc-in [:user-expenses :settings :loading?] true)
           (assoc-in [:user-expenses :settings :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri endpoints/settings-endpoint
                    :admin-uri endpoints/admin-settings-endpoint
                    :params settings-data
                    :on-success [:user-expenses/update-settings-success]
                    :on-failure [:user-expenses/update-settings-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-settings-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [settings (or (:data response) (:settings response) response)]
      {:db (-> db
             (assoc-in [:user-expenses :settings :data] settings)
             (assoc-in [:user-expenses :settings :loading?] false)
             (assoc-in [:user-expenses :settings :error] nil))
       :dispatch [:toast {:type :success :message "Settings updated"}]})))

(rf/reg-event-db
  :user-expenses/update-settings-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update user settings" {:error error})
    (-> db
      (assoc-in [:user-expenses :settings :loading?] false)
      (assoc-in [:user-expenses :settings :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Local settings (UI preferences stored in app-db)
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :user-expenses/set-local-setting
  common-interceptors
  (fn [db [setting-key value]]
    (assoc-in db [:user-expenses :local-settings setting-key] value)))

(rf/reg-event-db
  :user-expenses/toggle-local-setting
  common-interceptors
  (fn [db [setting-key]]
    (update-in db [:user-expenses :local-settings setting-key] not)))
