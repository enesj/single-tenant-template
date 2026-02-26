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

                    :on-success [:user-expenses/fetch-settings-success]
                    :on-failure [:user-expenses/fetch-settings-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-settings-success
  common-interceptors
  (fn [db [response]]
    (let [settings (:data response)]
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

                     ;; NOTE: day8.re-frame.http-fx + cljs-ajax expects JSON payloads in :params.
                     ;; :body is reserved for raw bodies like FormData or pre-serialized JSON strings.
                    :params settings-data
                    :on-success [:user-expenses/update-settings-success]
                    :on-failure [:user-expenses/update-settings-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-settings-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [settings (:data response)]
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
;; Save settings (alias for update-settings for better UX naming)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/save-settings
  common-interceptors
  ;; NOTE: common-interceptors includes trim-v, so the event vector here is [settings-data]
  (fn [{:keys [db]} [settings-data]]
    (log/info "Saving user expense settings" {:settings settings-data})
    {:db (-> db
           (assoc-in [:user-expenses :settings :loading?] true)
           (assoc-in [:user-expenses :settings :saving?] true)
           (assoc-in [:user-expenses :settings :error] nil))
     :dispatch [:user-expenses/update-settings settings-data]}))

;; Override the success/failure handlers to also clear :saving?
(rf/reg-event-fx
  :user-expenses/update-settings-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [settings (:data response)]
      {:db (-> db
             (assoc-in [:user-expenses :settings :data] settings)
             (assoc-in [:user-expenses :settings :loading?] false)
             (assoc-in [:user-expenses :settings :saving?] false)
             (assoc-in [:user-expenses :settings :error] nil))
       :dispatch [:toast {:type :success :message "Settings updated"}]})))

(rf/reg-event-db
  :user-expenses/update-settings-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update user settings" {:error error})
    (-> db
      (assoc-in [:user-expenses :settings :loading?] false)
      (assoc-in [:user-expenses :settings :saving?] false)
      (assoc-in [:user-expenses :settings :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Subscriptions
;; ---------------------------------------------------------------------------

(rf/reg-sub
  :user-expenses/settings
  (fn [db _]
    (get-in db [:user-expenses :settings :data] {})))

(rf/reg-sub
  :user-expenses/settings-loading?
  (fn [db _]
    (get-in db [:user-expenses :settings :loading?] false)))

(rf/reg-sub
  :user-expenses/settings-saving?
  (fn [db _]
    (get-in db [:user-expenses :settings :saving?] false)))


