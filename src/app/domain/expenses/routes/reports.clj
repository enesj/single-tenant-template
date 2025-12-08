(ns app.domain.expenses.routes.reports
  "Admin API routes for expense reporting."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.reports :as reports]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app [data]
  (-> data
      db-adapter/convert-pg-objects
      db-adapter/convert-db-keys->app-keys))

(defn- range-opts [qp]
  {:from (:from qp)
   :to (:to qp)})

(defn summary-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            summary (reports/get-summary db (range-opts qp))]
        (utils/success-response {:summary (to-app summary)})))
    "Failed to fetch summary"))

(defn payer-breakdown-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            data (reports/get-breakdown-by-payer db (range-opts qp))]
        (utils/success-response {:payers (to-app data)})))
    "Failed to fetch payer breakdown"))

(defn supplier-breakdown-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            data (reports/get-breakdown-by-supplier db (range-opts qp))]
        (utils/success-response {:suppliers (to-app data)})))
    "Failed to fetch supplier breakdown"))

(defn weekly-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            data (reports/get-weekly-totals db (range-opts qp))]
        (utils/success-response {:weeks (to-app data)})))
    "Failed to fetch weekly totals"))

(defn monthly-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            data (reports/get-monthly-totals db (range-opts qp))]
        (utils/success-response {:months (to-app data)})))
    "Failed to fetch monthly totals"))

(defn top-suppliers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            limit (utils/parse-int-param qp :limit 5)
            data (reports/get-top-suppliers db (merge (range-opts qp) {:limit limit}))]
        (utils/success-response {:suppliers (to-app data)})))
    "Failed to fetch top suppliers"))

(defn routes [db]
  ["/reports"
   ["/summary" {:get (summary-handler db)}]
   ["/payers" {:get (payer-breakdown-handler db)}]
   ["/suppliers" {:get (supplier-breakdown-handler db)}]
   ["/weekly" {:get (weekly-handler db)}]
   ["/monthly" {:get (monthly-handler db)}]
   ["/top-suppliers" {:get (top-suppliers-handler db)}]])
