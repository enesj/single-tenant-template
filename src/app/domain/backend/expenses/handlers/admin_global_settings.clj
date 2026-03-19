(ns app.domain.backend.expenses.handlers.admin-global-settings
  "Admin API handlers for global settings, enabled currencies,
   daily exchange rates, and exchange rate fetch alerts.

   Mounted under /admin/api/expenses/global-settings (and related paths)."
  (:require
    [app.domain.backend.expenses.services.exchange-rates :as exchange-rates]
    [app.domain.backend.expenses.services.global-settings :as global-settings]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.time LocalDate]))

;; ---------------------------------------------------------------------------
;; Global settings handlers
;; ---------------------------------------------------------------------------

(defn get-global-settings-handler [db]
  (utils/with-error-handling
    (fn [_request]
      (let [settings (global-settings/get-global-settings db)]
        (utils/success-response {:settings settings})))
    "Failed to fetch global settings"))

(defn update-global-settings-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (when (or (nil? body) (empty? body))
          (throw (ex-info "No fields provided"
                   {:status 400})))
        (let [updated (global-settings/update-global-settings! db body)]
          (log/info "Updated global settings" {:keys (keys body)})
          (utils/success-response {:settings updated}))))
    "Failed to update global settings"))

;; ---------------------------------------------------------------------------
;; Enabled currencies handlers
;; ---------------------------------------------------------------------------

(defn list-enabled-currencies-handler [db]
  (utils/with-error-handling
    (fn [_request]
      (let [currencies (global-settings/get-enabled-currencies db)]
        (utils/success-response {:currencies currencies})))
    "Failed to list enabled currencies"))

(defn add-enabled-currency-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            code (some-> (or (:code body) (get body "code")) str str/trim str/upper-case)
            currency-name (or (:name body) (get body "name"))]
        (when (or (str/blank? code) (not (re-matches #"[A-Z]{3}" (or code ""))))
          (throw (ex-info "code must be a 3-letter currency code"
                   {:status 400})))
        (when (str/blank? currency-name)
          (throw (ex-info "name is required"
                   {:status 400})))
        (let [currency (global-settings/add-enabled-currency! db {:code code :name currency-name})]
          (log/info "Added enabled currency" {:code code :name currency-name})
          (utils/success-response {:currency currency}))))
    "Failed to add enabled currency"))

(defn remove-enabled-currency-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [code (some-> (get-in request [:path-params :code]) str str/trim str/upper-case)]
        (when (str/blank? code)
          (throw (ex-info "Currency code is required" {:status 400})))
        (global-settings/remove-enabled-currency! db code)
        (log/info "Removed enabled currency" {:code code})
        (utils/success-response {:removed code})))
    "Failed to remove enabled currency"))

;; ---------------------------------------------------------------------------
;; Daily exchange rates handlers
;; ---------------------------------------------------------------------------

(defn get-daily-exchange-rates-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            date-str (or (:date qp) (get qp "date"))
            rate-date (if (str/blank? date-str)
                        (LocalDate/now)
                        (LocalDate/parse date-str))
            rates (exchange-rates/get-daily-rates db rate-date)]
        (utils/success-response {:rates rates
                                 :date (str rate-date)})))
    "Failed to fetch daily exchange rates"))

(defn fetch-daily-rates-handler [db app-config]
  (utils/with-error-handling
    (fn [_request]
      (let [config (exchange-rates/build-config app-config)
            result (exchange-rates/ensure-daily-rates! db config)]
        (utils/success-response result)))
    "Failed to fetch exchange rates"))

;; ---------------------------------------------------------------------------
;; Exchange rate alerts handlers
;; ---------------------------------------------------------------------------

(defn list-exchange-rate-alerts-handler [db]
  (utils/with-error-handling
    (fn [_request]
      (let [alerts (exchange-rates/get-unacknowledged-alerts db)]
        (utils/success-response {:alerts alerts})))
    "Failed to list exchange rate alerts"))

(defn acknowledge-alert-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [alert-id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (do
          (exchange-rates/acknowledge-alert! db alert-id)
          (log/info "Acknowledged exchange rate alert" {:alert-id alert-id})
          (utils/success-response {:acknowledged (str alert-id)}))
        (utils/error-response "Invalid alert id" :status 400)))
    "Failed to acknowledge alert"))
