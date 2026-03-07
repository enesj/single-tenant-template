(ns app.domain.frontend.expenses.events.user-expenses.reports-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- capture-dispatch-n!
  [captured]
  (rf/reg-fx :dispatch-n (fn [events]
                           (reset! captured events))))

(defn- restore-dispatch-n!
  []
  (rf/reg-fx :dispatch-n (fn [events]
                           (doseq [event events]
                             (rf/dispatch event)))))

(deftest reports-fetch-events-hit-current-report-endpoints
  (testing "tenant-scoped report fetch events hit the current /api/v1/expenses/reports/* endpoints"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (reset! sup/captured-http-requests [])

    (rf/dispatch-sync [:user-expenses/fetch-report-day-of-week])
    (rf/dispatch-sync [:user-expenses/fetch-report-size-distribution])
    (rf/dispatch-sync [:user-expenses/fetch-report-daily-heatmap])
    (rf/dispatch-sync [:user-expenses/fetch-report-filter-options])

    (let [uris (set (map sup/req-uri @sup/captured-http-requests))]
      (is (contains? uris "/api/v1/expenses/reports/day-of-week"))
      (is (contains? uris "/api/v1/expenses/reports/size-distribution"))
      (is (contains? uris "/api/v1/expenses/reports/daily-heatmap"))
      (is (contains? uris "/api/v1/expenses/reports/filter-options")))))

(deftest reports-refresh-dispatches-current-fetch-set
  (testing "reports-refresh dispatches the current summary, trend, supplier, and report fetch events"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (let [captured (atom nil)]
      (capture-dispatch-n! captured)
      (try
        (rf/dispatch-sync [:user-expenses/reports-refresh])
        (let [events @captured]
          (is (some #(= :user-expenses/fetch-summary (first %)) events))
          (is (some #(= [:user-expenses/fetch-by-month {:months-back 6}] %) events))
          (is (some #(= [:user-expenses/fetch-by-supplier {:limit 25}] %) events))
          (is (some #(= :user-expenses/fetch-report-filter-options (first %)) events))
          (is (some #(= :user-expenses/fetch-report-day-of-week (first %)) events))
          (is (some #(= :user-expenses/fetch-report-size-distribution (first %)) events))
          (is (some #(= :user-expenses/fetch-report-daily-heatmap (first %)) events)))
        (finally
          (restore-dispatch-n!))))))

(deftest reports-set-filter-refreshes-and-forwards-common-params
  (testing "current report requests forward only the supported common filter params"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :supplier-id "supplier-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :expense-category-id "expense-category-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :months-back 3])

    (reset! sup/captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/fetch-report-day-of-week])
    (rf/dispatch-sync [:user-expenses/fetch-report-filter-options])

    (let [requests @sup/captured-http-requests
          day-req (first (filter #(= "/api/v1/expenses/reports/day-of-week" (sup/req-uri %)) requests))
          filter-req (first (filter #(= "/api/v1/expenses/reports/filter-options" (sup/req-uri %)) requests))
          day-params (sup/req-params day-req)
          filter-params (sup/req-params filter-req)]
      (is (some? day-req))
      (is (= "supplier-42" (:supplier_id day-params)))
      (is (= "expense-category-42" (:expense_category_id day-params)))
      (is (string? (:from day-params)))
      (is (string? (:to day-params)))

      (is (some? filter-req))
      (is (= "supplier-42" (:supplier_id filter-params)))
      (is (= "expense-category-42" (:expense_category_id filter-params)))
      (is (string? (:from filter-params)))
      (is (string? (:to filter-params)))
      (is (nil? (:category_id day-params)))
      (is (nil? (:manufacturer_id day-params))))))

(deftest reports-clear-local-filters-resets-current-local-state
  (testing "clearing local report filters removes supported local filters and triggers refresh"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :supplier-id "supplier-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :expense-category-id "expense-category-42"])
    (rf/dispatch-sync [:user-expenses/reports-toggle-day-of-week 3])
    (rf/dispatch-sync [:user-expenses/reports-toggle-amount-bucket "small"])
    (rf/dispatch-sync [:user-expenses/reports-toggle-selected-day "2026-03-01"])

    (let [captured (atom nil)]
      (rf/reg-fx :dispatch (fn [event]
                             (reset! captured event)))
      (try
        (rf/dispatch-sync [:user-expenses/reports-clear-local-filters])
        (is (= [:user-expenses/reports-refresh] @captured))
        (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :supplier-id])))
        (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :expense-category-id])))
        (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :day-of-week])))
        (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :amount-bucket])))
        (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :selected-day])))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))