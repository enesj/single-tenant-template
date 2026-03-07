(ns app.admin.frontend.events.reports
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]))

(def ^:private base-uri "/admin/api/expenses/reports/analytics")
(def ^:private explorer-limit 500)
(def ^:private detail-limit 200)

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

(defn- detail-path
  [detail-key detail-id]
  [:admin/reports :details detail-key (str detail-id)])

(defn- start-detail-load
  [db detail-key detail-id]
  (-> db
    (assoc-in (conj (detail-path detail-key detail-id) :loading?) true)
    (assoc-in (conj (detail-path detail-key detail-id) :error) nil)))

(defn- finish-detail-load
  [db detail-key detail-id data]
  (-> db
    (assoc-in (conj (detail-path detail-key detail-id) :loading?) false)
    (assoc-in (conj (detail-path detail-key detail-id) :error) nil)
    (assoc-in (conj (detail-path detail-key detail-id) :data) data)))

(defn- fail-detail-load
  [db detail-key detail-id]
  (-> db
    (assoc-in (conj (detail-path detail-key detail-id) :loading?) false)
    (assoc-in (conj (detail-path detail-key detail-id) :error)
      (str "Failed to load " (name detail-key)))))

;; --- Filter management ---

(rf/reg-event-db
  :admin/reports-set-filter
  (fn [db [_ k v]]
    (assoc-in db [:admin/reports :filters k] v)))

(rf/reg-event-db
  :admin/reports-clear-filters
  (fn [db _]
    (-> db
      (assoc-in [:admin/reports :filters] {:months-back 6})
      (assoc-in [:admin/reports :details] {}))))

(rf/reg-event-db
  :admin/report-toggle-sort
  (fn [db [_ report-key column]]
    (let [current (get-in db [:admin/reports report-key :sort])
          new-sort (cond
                     (not= (:column current) column)
                     {:column column :direction :asc}

                     (= (:direction current) :asc)
                     {:column column :direction :desc}

                     :else nil)]
      (assoc-in db [:admin/reports report-key :sort] new-sort))))

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

(defn- fetch-report-detail
  [db detail-key detail-id uri extra-params]
  (let [filters (get-in db [:admin/reports :filters])
        params (merge (build-params filters) extra-params)]
    {:db (start-detail-load db detail-key detail-id)
     :http-xhrio (admin-http/admin-get
                   {:uri (str base-uri uri)
                    :params params
                    :on-success [:admin/report-detail-loaded detail-key (str detail-id)]
                    :on-failure [:admin/report-detail-failed detail-key (str detail-id)]})}))

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

(rf/reg-event-db
  :admin/report-detail-loaded
  (fn [db [_ detail-key detail-id response]]
    (finish-detail-load db detail-key detail-id (:data response))))

(rf/reg-event-db
  :admin/report-detail-failed
  (fn [db [_ detail-key detail-id _error]]
    (fail-detail-load db detail-key detail-id)))

;; --- Individual report fetchers ---

(rf/reg-event-fx
  :admin/fetch-top-suppliers
  (fn [{:keys [db]} _]
    (fetch-report db :top-suppliers "/top-suppliers" {:limit explorer-limit})))

(rf/reg-event-fx
  :admin/fetch-top-items
  (fn [{:keys [db]} _]
    (fetch-report db :top-items "/top-items" {:limit explorer-limit})))

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

(rf/reg-event-fx
  :admin/fetch-supplier-stores-detail
  (fn [{:keys [db]} [_ detail-id {:keys [supplier-id currency]}]]
    (fetch-report-detail db :supplier-stores detail-id "/supplier-stores"
      {:supplier_id supplier-id
       :currency currency
       :limit detail-limit})))

(rf/reg-event-fx
  :admin/fetch-supplier-articles-detail
  (fn [{:keys [db]} [_ detail-id {:keys [supplier-id currency]}]]
    (fetch-report-detail db :supplier-articles detail-id "/supplier-deep-dive"
      {:supplier_id supplier-id
       :currency currency
       :alias_limit detail-limit})))

(rf/reg-event-fx
  :admin/fetch-article-breakdown-detail
  (fn [{:keys [db]} [_ detail-id {:keys [alias-id currency]}]]
    (fetch-report-detail db :article-breakdown detail-id (str "/top-items/" alias-id "/breakdown")
      {:currency currency
       :limit detail-limit})))

;; --- Load all / refresh ---

(rf/reg-event-fx
  :admin/load-reports
  (fn [{:keys [db]} _]
    (when (:admin/authenticated? db)
      {:db (assoc-in db [:admin/reports :details] {})
       :dispatch-n [[:admin/fetch-top-suppliers]
                    [:admin/fetch-top-items]
                    [:admin/fetch-category-allocation]
                    [:admin/fetch-supplier-monthly-trends]
                    [:admin/fetch-report-filter-options]]})))

(rf/reg-event-fx
  :admin/reports-refresh
  (fn [_ _]
    {:dispatch [:admin/load-reports]}))
