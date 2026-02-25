(ns app.domain.frontend.expenses.events.user-expenses.reports.fetch
  "Report data fetch events."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.reports.helpers :as h]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- start-load
  [db report-key]
  (-> db
    (assoc-in (conj h/reports-path report-key :loading?) true)
    (assoc-in (conj h/reports-path report-key :error) nil)))

(defn- finish-success
  [db report-key value]
  (-> db
    (assoc-in (conj h/reports-path report-key :loading?) false)
    (assoc-in (conj h/reports-path report-key :error) nil)
    (assoc-in (conj h/reports-path report-key :data) value)))

(defn- finish-failure
  [db report-key error]
  (-> db
    (assoc-in (conj h/reports-path report-key :loading?) false)
    (assoc-in (conj h/reports-path report-key :error) (h/finish-failure-message error))))

(defn- fetch-fx
  [db report-key uri params on-success on-failure]
  {:db (start-load db report-key)
   :http-xhrio (x/xhrio db
                 {:method :get
                  :uri uri
                  :params params
                  :on-success on-success
                  :on-failure on-failure})})

;; ---------------------------------------------------------------------------
;; Filter options
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-filter-options
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :filter-options
      endpoints/reports-filter-options-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-filter-options-success]
      [:user-expenses/fetch-report-filter-options-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-filter-options-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :filter-options (or (:data response) {}))))

(rf/reg-event-db
  :user-expenses/fetch-report-filter-options-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch report filter options" {:error error})
    (finish-failure db :filter-options error)))

;; ---------------------------------------------------------------------------
;; Supplier deep-dive
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-supplier-deep-dive
  common-interceptors
  (fn [{:keys [db]} _]
    (let [supplier-id (-> (get-in db (conj h/reports-path :filters :supplier-id))
                        h/normalize-id-values
                        first)]
      (if-not supplier-id
        {:db (finish-success db :supplier-deep-dive nil)}
        (fetch-fx db
          :supplier-deep-dive
          endpoints/reports-supplier-deep-dive-endpoint
          (assoc (h/common-report-params db) :supplier_id supplier-id)
          [:user-expenses/fetch-report-supplier-deep-dive-success]
          [:user-expenses/fetch-report-supplier-deep-dive-failure])))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-deep-dive-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :supplier-deep-dive (:data response))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-deep-dive-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch supplier deep-dive report" {:error error})
    (finish-failure db :supplier-deep-dive error)))

;; ---------------------------------------------------------------------------
;; Day of week
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-day-of-week
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :day-of-week
      endpoints/reports-day-of-week-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-day-of-week-success]
      [:user-expenses/fetch-report-day-of-week-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-day-of-week-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :day-of-week (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-day-of-week-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch day-of-week report" {:error error})
    (finish-failure db :day-of-week error)))

;; ---------------------------------------------------------------------------
;; Top items
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-top-items
  common-interceptors
  (fn [{:keys [db]} _]
    (let [limit* (h/->positive-int (get-in db (conj h/reports-path :filters :top-items-limit)) 20)]
      (fetch-fx db
        :top-items
        endpoints/reports-top-items-endpoint
        (assoc (h/common-report-params db) :limit limit*)
        [:user-expenses/fetch-report-top-items-success]
        [:user-expenses/fetch-report-top-items-failure]))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-items-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :top-items (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-items-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch top-items report" {:error error})
    (finish-failure db :top-items error)))

;; ---------------------------------------------------------------------------
;; Top item breakdown
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-top-item-breakdown
  common-interceptors
  (fn [{:keys [db]} _]
    (let [expanded-top-item-alias-id (-> (get-in db (conj h/reports-path :filters :expanded-top-item-alias-id))
                                       h/normalize-id)]
      (if-not expanded-top-item-alias-id
        {:db (finish-success db :top-item-breakdown {:suppliers [] :stores []})}
        (fetch-fx db
          :top-item-breakdown
          (endpoints/reports-top-item-breakdown-endpoint expanded-top-item-alias-id)
          (assoc (h/common-report-params db) :limit 50)
          [:user-expenses/fetch-report-top-item-breakdown-success]
          [:user-expenses/fetch-report-top-item-breakdown-failure])))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-item-breakdown-success
  common-interceptors
  (fn [db [response]]
    (finish-success db
      :top-item-breakdown
      (or (:data response) {:suppliers [] :stores []}))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-item-breakdown-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch top-item-breakdown report" {:error error})
    (finish-failure db :top-item-breakdown error)))

;; ---------------------------------------------------------------------------
;; Monthly comparison
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-monthly-comparison
  common-interceptors
  (fn [{:keys [db]} _]
    (let [{:keys [month-a month-b]} (get-in db (conj h/reports-path :filters))
          month-a* (h/normalize-month month-a)
          month-b* (h/normalize-month month-b)]
      (if (or (not month-a*) (not month-b*))
        {:db (-> db
               (assoc-in (conj h/reports-path :monthly-comparison :loading?) false)
               (assoc-in (conj h/reports-path :monthly-comparison :error) "Select both months to compare.")
               (assoc-in (conj h/reports-path :monthly-comparison :data) nil))}
        (fetch-fx db
          :monthly-comparison
          endpoints/reports-monthly-comparison-endpoint
          (assoc (h/common-report-params db)
            :month_a month-a*
            :month_b month-b*)
          [:user-expenses/fetch-report-monthly-comparison-success]
          [:user-expenses/fetch-report-monthly-comparison-failure])))))

(rf/reg-event-db
  :user-expenses/fetch-report-monthly-comparison-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :monthly-comparison (:data response))))

(rf/reg-event-db
  :user-expenses/fetch-report-monthly-comparison-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch monthly-comparison report" {:error error})
    (finish-failure db :monthly-comparison error)))

;; ---------------------------------------------------------------------------
;; Size distribution
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-size-distribution
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :size-distribution
      endpoints/reports-size-distribution-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-size-distribution-success]
      [:user-expenses/fetch-report-size-distribution-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-size-distribution-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :size-distribution (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-size-distribution-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch size-distribution report" {:error error})
    (finish-failure db :size-distribution error)))

;; ---------------------------------------------------------------------------
;; Daily heatmap
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-daily-heatmap
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :daily-heatmap
      endpoints/reports-daily-heatmap-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-daily-heatmap-success]
      [:user-expenses/fetch-report-daily-heatmap-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-daily-heatmap-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :daily-heatmap (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-daily-heatmap-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch daily-heatmap report" {:error error})
    (finish-failure db :daily-heatmap error)))

;; ---------------------------------------------------------------------------
;; Category allocation
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-category-allocation
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :category-allocation
      endpoints/reports-category-allocation-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-category-allocation-success]
      [:user-expenses/fetch-report-category-allocation-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-category-allocation-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :category-allocation (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-category-allocation-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch category-allocation report" {:error error})
    (finish-failure db :category-allocation error)))

;; ---------------------------------------------------------------------------
;; Top suppliers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-top-suppliers
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :top-suppliers
      endpoints/reports-top-suppliers-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-top-suppliers-success]
      [:user-expenses/fetch-report-top-suppliers-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-top-suppliers-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :top-suppliers (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-suppliers-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch top-suppliers report" {:error error})
    (finish-failure db :top-suppliers error)))

;; ---------------------------------------------------------------------------
;; Supplier stores (expanded drill-down)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-supplier-stores
  common-interceptors
  (fn [{:keys [db]} _]
    (let [expanded-supplier-id (-> (get-in db (conj h/reports-path :filters :expanded-supplier-id))
                                 h/normalize-id)]
      (if-not expanded-supplier-id
        {:db (finish-success db :supplier-stores [])}
        (fetch-fx db
          :supplier-stores
          endpoints/reports-supplier-stores-endpoint
          (assoc (h/common-report-params db)
            :supplier_id expanded-supplier-id
            :limit 20)
          [:user-expenses/fetch-report-supplier-stores-success]
          [:user-expenses/fetch-report-supplier-stores-failure])))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-stores-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :supplier-stores (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-stores-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch supplier-stores report" {:error error})
    (finish-failure db :supplier-stores error)))

;; ---------------------------------------------------------------------------
;; Supplier monthly trends
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-supplier-monthly-trends
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :supplier-monthly-trends
      endpoints/reports-supplier-monthly-trends-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-supplier-monthly-trends-success]
      [:user-expenses/fetch-report-supplier-monthly-trends-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-monthly-trends-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :supplier-monthly-trends (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-monthly-trends-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch supplier-monthly-trends report" {:error error})
    (finish-failure db :supplier-monthly-trends error)))
