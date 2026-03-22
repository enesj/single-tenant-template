(ns app.domain.frontend.expenses.events.user-expenses.reports.fetch
  "Report data fetch events.

  Only tenant-scoped reports remain here. Reports grouping by global
  entities (suppliers, articles, categories) moved to admin."
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
;; By category
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-report-by-category
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :by-category
      endpoints/reports-by-category-endpoint
      (h/common-report-params db)
      [:user-expenses/fetch-report-by-category-success]
      [:user-expenses/fetch-report-by-category-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-by-category-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :by-category (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-by-category-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch by-category report" {:error error})
    (finish-failure db :by-category error)))
