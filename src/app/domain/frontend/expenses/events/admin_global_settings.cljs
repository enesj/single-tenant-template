(ns app.domain.frontend.expenses.events.admin-global-settings
  "Admin events and subscriptions for expenses global settings."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:admin :expenses :global-settings])
(def ^:private settings-path (conj base-path :settings))
(def ^:private currencies-path (conj base-path :currencies))
(def ^:private rates-path (conj base-path :rates))
(def ^:private alerts-path (conj base-path :alerts))

(def ^:private global-settings-uri "/admin/api/expenses/global-settings")
(def ^:private enabled-currencies-uri "/admin/api/expenses/enabled-currencies")
(def ^:private daily-rates-uri "/admin/api/expenses/daily-exchange-rates")
(def ^:private fetch-rates-uri "/admin/api/expenses/daily-exchange-rates/fetch")
(def ^:private alerts-uri "/admin/api/expenses/exchange-rate-alerts")

(defn- with-loading [db path loading?]
  (assoc-in db (conj path :loading?) loading?))

(defn- with-error [db path value]
  (assoc-in db (conj path :error) value))

(rf/reg-event-fx
  :admin/expenses-settings-load
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in base-path {})
           (assoc-in (conj settings-path :loading?) true)
           (assoc-in (conj currencies-path :loading?) true)
           (assoc-in (conj rates-path :loading?) true)
           (assoc-in (conj alerts-path :loading?) true))
     :dispatch-n [[:admin/expenses-settings-fetch-settings]
                  [:admin/expenses-settings-fetch-currencies]
                  [:admin/expenses-settings-fetch-rates]
                  [:admin/expenses-settings-fetch-alerts]]}))

(rf/reg-event-fx
  :admin/expenses-settings-fetch-settings
  (fn [{:keys [db]} _]
    {:db (-> db
           (with-loading settings-path true)
           (with-error settings-path nil))
     :http-xhrio (admin-http/admin-get
                   {:uri global-settings-uri
                    :on-success [:admin/expenses-settings-fetch-settings-success]
                    :on-failure [:admin/expenses-settings-fetch-settings-failure]})}))

(rf/reg-event-db
  :admin/expenses-settings-fetch-settings-success
  (fn [db [_ response]]
    (-> db
      (assoc-in (conj settings-path :data) (:settings response))
      (with-loading settings-path false)
      (with-error settings-path nil))))

(rf/reg-event-db
  :admin/expenses-settings-fetch-settings-failure
  (fn [db [_ error]]
    (log/warn "Failed to fetch expenses global settings" {:error error})
    (-> db
      (with-loading settings-path false)
      (with-error settings-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-update-settings
  (fn [{:keys [db]} [_ params]]
    {:db (-> db
           (assoc-in (conj settings-path :saving?) true)
           (with-error settings-path nil))
     :http-xhrio (admin-http/admin-put
                   {:uri global-settings-uri
                    :params params
                    :on-success [:admin/expenses-settings-update-settings-success]
                    :on-failure [:admin/expenses-settings-update-settings-failure]})}))

(rf/reg-event-fx
  :admin/expenses-settings-update-settings-success
  (fn [{:keys [db]} [_ response]]
    {:db (-> db
           (assoc-in (conj settings-path :data) (:settings response))
           (assoc-in (conj settings-path :saving?) false)
           (with-error settings-path nil))
     :dispatch [:toast {:type :success :message "Expenses settings updated"}]}))

(rf/reg-event-db
  :admin/expenses-settings-update-settings-failure
  (fn [db [_ error]]
    (log/warn "Failed to update expenses global settings" {:error error})
    (-> db
      (assoc-in (conj settings-path :saving?) false)
      (with-error settings-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-fetch-currencies
  (fn [{:keys [db]} _]
    {:db (-> db
           (with-loading currencies-path true)
           (with-error currencies-path nil))
     :http-xhrio (admin-http/admin-get
                   {:uri enabled-currencies-uri
                    :on-success [:admin/expenses-settings-fetch-currencies-success]
                    :on-failure [:admin/expenses-settings-fetch-currencies-failure]})}))

(rf/reg-event-db
  :admin/expenses-settings-fetch-currencies-success
  (fn [db [_ response]]
    (-> db
      (assoc-in (conj currencies-path :data) (:currencies response))
      (with-loading currencies-path false)
      (with-error currencies-path nil))))

(rf/reg-event-db
  :admin/expenses-settings-fetch-currencies-failure
  (fn [db [_ error]]
    (log/warn "Failed to fetch enabled currencies" {:error error})
    (-> db
      (with-loading currencies-path false)
      (with-error currencies-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-add-currency
  (fn [{:keys [db]} [_ params]]
    {:db (-> db
           (assoc-in (conj currencies-path :saving?) true)
           (with-error currencies-path nil))
     :http-xhrio (admin-http/admin-post
                   {:uri enabled-currencies-uri
                    :params params
                    :on-success [:admin/expenses-settings-add-currency-success]
                    :on-failure [:admin/expenses-settings-add-currency-failure]})}))

(rf/reg-event-fx
  :admin/expenses-settings-add-currency-success
  (fn [{:keys [db]} _]
    {:db (assoc-in db (conj currencies-path :saving?) false)
     :dispatch-n [[:toast {:type :success :message "Currency added"}]
                  [:admin/expenses-settings-fetch-currencies]
                  [:admin/expenses-settings-fetch-settings]]}))

(rf/reg-event-db
  :admin/expenses-settings-add-currency-failure
  (fn [db [_ error]]
    (log/warn "Failed to add currency" {:error error})
    (-> db
      (assoc-in (conj currencies-path :saving?) false)
      (with-error currencies-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-remove-currency
  (fn [{:keys [db]} [_ code]]
    {:db (-> db
           (assoc-in (conj currencies-path :saving?) true)
           (with-error currencies-path nil))
     :http-xhrio (admin-http/admin-delete
                   {:uri (str enabled-currencies-uri "/" code)
                    :on-success [:admin/expenses-settings-remove-currency-success]
                    :on-failure [:admin/expenses-settings-remove-currency-failure]})}))

(rf/reg-event-fx
  :admin/expenses-settings-remove-currency-success
  (fn [{:keys [db]} _]
    {:db (assoc-in db (conj currencies-path :saving?) false)
     :dispatch-n [[:toast {:type :success :message "Currency removed"}]
                  [:admin/expenses-settings-fetch-currencies]
                  [:admin/expenses-settings-fetch-settings]]}))

(rf/reg-event-db
  :admin/expenses-settings-remove-currency-failure
  (fn [db [_ error]]
    (log/warn "Failed to remove currency" {:error error})
    (-> db
      (assoc-in (conj currencies-path :saving?) false)
      (with-error currencies-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-fetch-rates
  (fn [{:keys [db]} _]
    {:db (-> db
           (with-loading rates-path true)
           (with-error rates-path nil))
     :http-xhrio (admin-http/admin-get
                   {:uri daily-rates-uri
                    :on-success [:admin/expenses-settings-fetch-rates-success]
                    :on-failure [:admin/expenses-settings-fetch-rates-failure]})}))

(rf/reg-event-db
  :admin/expenses-settings-fetch-rates-success
  (fn [db [_ response]]
    (-> db
      (assoc-in (conj rates-path :data) (:rates response))
      (assoc-in (conj rates-path :date) (:date response))
      (with-loading rates-path false)
      (with-error rates-path nil))))

(rf/reg-event-db
  :admin/expenses-settings-fetch-rates-failure
  (fn [db [_ error]]
    (log/warn "Failed to fetch daily exchange rates" {:error error})
    (-> db
      (with-loading rates-path false)
      (with-error rates-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-fetch-rates-now
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (conj rates-path :fetching?) true)
           (with-error rates-path nil))
     :http-xhrio (admin-http/admin-post
                   {:uri fetch-rates-uri
                    :on-success [:admin/expenses-settings-fetch-rates-now-success]
                    :on-failure [:admin/expenses-settings-fetch-rates-now-failure]})}))

(rf/reg-event-fx
  :admin/expenses-settings-fetch-rates-now-success
  (fn [{:keys [db]} [_ response]]
    {:db (-> db
           (assoc-in (conj rates-path :fetching?) false)
           (assoc-in (conj rates-path :last-result) response))
     :dispatch-n [[:toast {:type :success :message "Exchange rates refreshed"}]
                  [:admin/expenses-settings-fetch-rates]
                  [:admin/expenses-settings-fetch-alerts]]}))

(rf/reg-event-db
  :admin/expenses-settings-fetch-rates-now-failure
  (fn [db [_ error]]
    (log/warn "Failed to refresh exchange rates" {:error error})
    (-> db
      (assoc-in (conj rates-path :fetching?) false)
      (with-error rates-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-fetch-alerts
  (fn [{:keys [db]} _]
    {:db (-> db
           (with-loading alerts-path true)
           (with-error alerts-path nil))
     :http-xhrio (admin-http/admin-get
                   {:uri alerts-uri
                    :on-success [:admin/expenses-settings-fetch-alerts-success]
                    :on-failure [:admin/expenses-settings-fetch-alerts-failure]})}))

(rf/reg-event-db
  :admin/expenses-settings-fetch-alerts-success
  (fn [db [_ response]]
    (-> db
      (assoc-in (conj alerts-path :data) (:alerts response))
      (with-loading alerts-path false)
      (with-error alerts-path nil))))

(rf/reg-event-db
  :admin/expenses-settings-fetch-alerts-failure
  (fn [db [_ error]]
    (log/warn "Failed to fetch exchange rate alerts" {:error error})
    (-> db
      (with-loading alerts-path false)
      (with-error alerts-path (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  :admin/expenses-settings-ack-alert
  (fn [{:keys [db]} [_ alert-id]]
    {:db (-> db
           (assoc-in (conj alerts-path :saving?) true)
           (with-error alerts-path nil))
     :http-xhrio (admin-http/admin-put
                   {:uri (str alerts-uri "/" alert-id "/acknowledge")
                    :on-success [:admin/expenses-settings-ack-alert-success]
                    :on-failure [:admin/expenses-settings-ack-alert-failure]})}))

(rf/reg-event-fx
  :admin/expenses-settings-ack-alert-success
  (fn [{:keys [db]} _]
    {:db (assoc-in db (conj alerts-path :saving?) false)
     :dispatch-n [[:toast {:type :success :message "Alert acknowledged"}]
                  [:admin/expenses-settings-fetch-alerts]]}))

(rf/reg-event-db
  :admin/expenses-settings-ack-alert-failure
  (fn [db [_ error]]
    (log/warn "Failed to acknowledge alert" {:error error})
    (-> db
      (assoc-in (conj alerts-path :saving?) false)
      (with-error alerts-path (admin-http/extract-error-message error)))))

(rf/reg-sub
  :admin/expenses-settings-settings
  (fn [db _]
    (get-in db (conj settings-path :data) {})))

(rf/reg-sub
  :admin/expenses-settings-settings-loading?
  (fn [db _]
    (boolean (get-in db (conj settings-path :loading?) false))))

(rf/reg-sub
  :admin/expenses-settings-settings-saving?
  (fn [db _]
    (boolean (get-in db (conj settings-path :saving?) false))))

(rf/reg-sub
  :admin/expenses-settings-settings-error
  (fn [db _]
    (get-in db (conj settings-path :error))))

(rf/reg-sub
  :admin/expenses-settings-currencies
  (fn [db _]
    (get-in db (conj currencies-path :data) [])))

(rf/reg-sub
  :admin/expenses-settings-currencies-loading?
  (fn [db _]
    (boolean (get-in db (conj currencies-path :loading?) false))))

(rf/reg-sub
  :admin/expenses-settings-currencies-saving?
  (fn [db _]
    (boolean (get-in db (conj currencies-path :saving?) false))))

(rf/reg-sub
  :admin/expenses-settings-currencies-error
  (fn [db _]
    (get-in db (conj currencies-path :error))))

(rf/reg-sub
  :admin/expenses-settings-rates
  (fn [db _]
    (get-in db (conj rates-path :data) [])))

(rf/reg-sub
  :admin/expenses-settings-rates-date
  (fn [db _]
    (get-in db (conj rates-path :date))))

(rf/reg-sub
  :admin/expenses-settings-rates-loading?
  (fn [db _]
    (boolean (get-in db (conj rates-path :loading?) false))))

(rf/reg-sub
  :admin/expenses-settings-rates-fetching?
  (fn [db _]
    (boolean (get-in db (conj rates-path :fetching?) false))))

(rf/reg-sub
  :admin/expenses-settings-rates-error
  (fn [db _]
    (get-in db (conj rates-path :error))))

(rf/reg-sub
  :admin/expenses-settings-rates-last-result
  (fn [db _]
    (get-in db (conj rates-path :last-result))))

(rf/reg-sub
  :admin/expenses-settings-alerts
  (fn [db _]
    (get-in db (conj alerts-path :data) [])))

(rf/reg-sub
  :admin/expenses-settings-alerts-loading?
  (fn [db _]
    (boolean (get-in db (conj alerts-path :loading?) false))))

(rf/reg-sub
  :admin/expenses-settings-alerts-saving?
  (fn [db _]
    (boolean (get-in db (conj alerts-path :saving?) false))))

(rf/reg-sub
  :admin/expenses-settings-alerts-error
  (fn [db _]
    (get-in db (conj alerts-path :error))))
