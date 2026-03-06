(ns app.admin.frontend.events.reports
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]))

(def ^:private base-uri "/admin/api/expenses/reports/analytics")

(defn- months-back->from
  [months-back]
  (let [now (js/Date.)
        y (.getFullYear now)
        m (.getMonth now)
        from-date (js/Date. y (- m (or months-back 6)) 1)]
    (.toISOString from-date)))

(defn- build-params
  [{:keys [months-back tenant-id supplier-id expense-category-id currency]}]
  (let [from (months-back->from (or months-back 6))]
    (cond-> {:from from}
      tenant-id (assoc :tenant_id tenant-id)
      supplier-id (assoc :supplier_id supplier-id)
      expense-category-id (assoc :expense_category_id expense-category-id)
      currency (assoc :currency currency))))

;; --- Filter management ---

(rf/reg-event-db
  :admin/reports-set-filter
  (fn [db [_ k v]]
    (assoc-in db [:admin/reports :filters k] v)))

(rf/reg-event-db
  :admin/reports-clear-filters
  (fn [db _]
    (assoc-in db [:admin/reports :filters] {:months-back 6})))

;; --- Generic report fetch ---

(defn- fetch-report
  [db report-key uri extra-params]
  (let [filters (get-in db [:admin/reports :filters])
        params (merge (build-params filters) extra-params)]
    {:db (-> db
           (assoc-in [:admin/reports report-key :loading?] true)
           (assoc-in [:admin/reports report-key :error] nil))
     :http-xhrio (admin-http/admin-get
                   {:uri (str base-uri uri)
                    :params params
                    :on-success [:admin/report-loaded report-key]
                    :on-failure [:admin/report-failed report-key]})}))

(rf/reg-event-db
  :admin/report-loaded
  (fn [db [_ report-key response]]
    (-> db
      (assoc-in [:admin/reports report-key :data] (:data response))
      (assoc-in [:admin/reports report-key :loading?] false))))

(rf/reg-event-db
  :admin/report-failed
  (fn [db [_ report-key _error]]
    (-> db
      (assoc-in [:admin/reports report-key :loading?] false)
      (assoc-in [:admin/reports report-key :error]
        (str "Failed to load " (name report-key))))))

;; --- Individual report fetchers ---

(rf/reg-event-fx
  :admin/fetch-top-suppliers
  (fn [{:keys [db]} _]
    (fetch-report db :top-suppliers "/top-suppliers" {:limit 20})))

(rf/reg-event-fx
  :admin/fetch-top-items
  (fn [{:keys [db]} _]
    (fetch-report db :top-items "/top-items" {:limit 20})))

(rf/reg-event-fx
  :admin/fetch-category-allocation
  (fn [{:keys [db]} _]
    (fetch-report db :category-allocation "/category-allocation" {})))

(rf/reg-event-fx
  :admin/fetch-report-filter-options
  (fn [{:keys [db]} _]
    (fetch-report db :filter-options "/filter-options" {})))

(rf/reg-event-fx
  :admin/fetch-supplier-monthly-trends
  (fn [{:keys [db]} _]
    (fetch-report db :supplier-monthly-trends "/supplier-monthly-trends" {:limit 10})))

;; --- Load all / refresh ---

(rf/reg-event-fx
  :admin/load-reports
  (fn [{:keys [db]} _]
    (when (:admin/authenticated? db)
      {:dispatch-n [[:admin/fetch-top-suppliers]
                    [:admin/fetch-top-items]
                    [:admin/fetch-category-allocation]
                    [:admin/fetch-supplier-monthly-trends]
                    [:admin/fetch-report-filter-options]]})))

(rf/reg-event-fx
  :admin/reports-refresh
  (fn [_ _]
    {:dispatch [:admin/load-reports]}))
