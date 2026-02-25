(ns app.domain.frontend.expenses.events.user-expenses.reports-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest reports-init-fetches-expanded-report-endpoints
  (testing "expanded report fetch events hit /api/v1/expenses/reports/* endpoints"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (reset! sup/captured-http-requests [])

    (rf/dispatch-sync [:user-expenses/fetch-report-day-of-week])
    (rf/dispatch-sync [:user-expenses/fetch-report-top-items])
    (rf/dispatch-sync [:user-expenses/fetch-report-monthly-comparison])
    (rf/dispatch-sync [:user-expenses/fetch-report-size-distribution])
    (rf/dispatch-sync [:user-expenses/fetch-report-daily-heatmap])
    (rf/dispatch-sync [:user-expenses/fetch-report-category-allocation])

    (let [uris (set (map sup/req-uri @sup/captured-http-requests))]
      (is (contains? uris "/api/v1/expenses/reports/day-of-week"))
      (is (contains? uris "/api/v1/expenses/reports/top-items"))
      (is (contains? uris "/api/v1/expenses/reports/monthly-comparison"))
      (is (contains? uris "/api/v1/expenses/reports/size-distribution"))
      (is (contains? uris "/api/v1/expenses/reports/daily-heatmap"))
      (is (contains? uris "/api/v1/expenses/reports/category-allocation")))))

(deftest reports-set-filter-refreshes-and-passes-supplier-param
  (testing "report requests forward supplier and item filter params"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])

    (reset! sup/captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-deep-dive])
    (is (= 0 (count @sup/captured-http-requests)))

    (rf/dispatch-sync [:user-expenses/reports-set-filter :supplier-id "supplier-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :category-id "category-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :subcategory-id "subcategory-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :expense-category-id "expense-category-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :manufacturer-id "manufacturer-42"])

    (reset! sup/captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-deep-dive])
    (rf/dispatch-sync [:user-expenses/fetch-report-top-items])
    (rf/dispatch-sync [:user-expenses/fetch-report-category-allocation])
    (rf/dispatch-sync [:user-expenses/fetch-report-monthly-comparison])

    (let [requests @sup/captured-http-requests
          deep-dive-req (first (filter #(= "/api/v1/expenses/reports/supplier-deep-dive" (sup/req-uri %)) requests))
          top-items-req (first (filter #(= "/api/v1/expenses/reports/top-items" (sup/req-uri %)) requests))
          category-allocation-req (first (filter #(= "/api/v1/expenses/reports/category-allocation" (sup/req-uri %)) requests))
          monthly-req (first (filter #(= "/api/v1/expenses/reports/monthly-comparison" (sup/req-uri %)) requests))
          deep-dive-params (sup/req-params deep-dive-req)
          top-items-params (sup/req-params top-items-req)
          category-allocation-params (sup/req-params category-allocation-req)
          monthly-params (sup/req-params monthly-req)]
      (is (some? deep-dive-req))
      (is (= "supplier-42" (:supplier_id deep-dive-params)))
      (is (= "category-42" (:category_id deep-dive-params)))
      (is (= "subcategory-42" (:subcategory_id deep-dive-params)))
      (is (= "expense-category-42" (:expense_category_id deep-dive-params)))
      (is (= "manufacturer-42" (:manufacturer_id deep-dive-params)))
      (is (string? (:from deep-dive-params)))
      (is (string? (:to deep-dive-params)))

      (is (some? top-items-req))
      (is (= "supplier-42" (:supplier_id top-items-params)))
      (is (= "category-42" (:category_id top-items-params)))
      (is (= "subcategory-42" (:subcategory_id top-items-params)))
      (is (= "expense-category-42" (:expense_category_id top-items-params)))
      (is (= "manufacturer-42" (:manufacturer_id top-items-params)))

      (is (some? category-allocation-req))
      (is (= "category-42" (:category_id category-allocation-params)))
      (is (= "subcategory-42" (:subcategory_id category-allocation-params)))
      (is (= "manufacturer-42" (:manufacturer_id category-allocation-params)))

      (is (some? monthly-req))
      (is (= "supplier-42" (:supplier_id monthly-params)))
      (is (= "expense-category-42" (:expense_category_id monthly-params)))
      (is (string? (:month_a monthly-params)))
      (is (string? (:month_b monthly-params))))))

(deftest reports-toggle-expanded-supplier-fetches-and-clears-stores
  (testing "expanding a supplier fetches /reports/supplier-stores, toggling same supplier clears drilldown state"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])

    (reset! sup/captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/reports-toggle-expanded-supplier "supplier-99"])
    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-stores])

    (let [req (sup/last-http-request)
          params (sup/req-params req)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/reports/supplier-stores" (sup/req-uri req)))
      (is (= "supplier-99" (:supplier_id params)))
      (is (= 20 (:limit params)))
      (is (string? (:from params)))
      (is (string? (:to params))))

    (is (= "supplier-99"
          (get-in @rf-db/app-db [:user-expenses :reports :filters :expanded-supplier-id])))

    (swap! rf-db/app-db assoc-in [:user-expenses :reports :supplier-stores :data]
      [{:store_name "Store A"}])
    (let [request-count (count @sup/captured-http-requests)]
      (rf/dispatch-sync [:user-expenses/reports-toggle-expanded-supplier "supplier-99"])
      (is (= request-count (count @sup/captured-http-requests)))
      (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :expanded-supplier-id])))
      (is (= [] (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :data])))
      (is (false? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :loading?])))
      (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :error]))))))

(deftest fetch-report-supplier-stores-without-expanded-supplier-is-safe
  (testing "explicit supplier-stores fetch does not fire HTTP when no expanded supplier is selected"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (reset! sup/captured-http-requests [])

    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-stores])

    (is (= 0 (count @sup/captured-http-requests)))
    (is (= [] (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :data])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :loading?])))
    (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :error])))))

(deftest reports-refresh-requests-supplier-stores-only-when-expanded-supplier-exists
  (testing "reports-refresh includes supplier-stores fetch only for expanded supplier state"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])

    (let [captured-dispatch-n (atom nil)]
      (rf/reg-fx :dispatch-n (fn [events]
                               (reset! captured-dispatch-n events)))
      (try
        (rf/dispatch-sync [:user-expenses/reports-refresh])
        (is (not-any? #(= :user-expenses/fetch-report-supplier-stores (first %))
              @captured-dispatch-n))

        (rf/dispatch-sync [:user-expenses/reports-toggle-expanded-supplier "supplier-42"])
        (rf/dispatch-sync [:user-expenses/reports-refresh])
        (is (some #(= :user-expenses/fetch-report-supplier-stores (first %))
              @captured-dispatch-n))
        (finally
          (rf/reg-fx :dispatch-n (fn [events]
                                   (doseq [event events]
                                     (rf/dispatch event)))))))))
