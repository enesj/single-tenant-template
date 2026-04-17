(ns app.domain.frontend.expenses.events.receipts-test
  (:require
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-test-state!
  []
  (setup/reset-db!)
  (setup/install-http-stub!)
  (swap! rf-db/app-db assoc :current-route {:data {:name :admin/receipts}})
  (swap! rf-db/app-db assoc-in (paths/list-pagination-mode :receipts) :server)
  (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
  (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 3)
  nil)

(deftest load-list-serializes-single-select-status-filter
  (testing "single select values are unwrapped into scalar backend params"
    (reset-test-state!)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts)
      {:status {:value "review_required"
                :label "Review required"}})

    (rf/dispatch-sync [::receipts-events/load-list {}])

    (let [req (setup/last-http-request)
          params (:params req)]
      (is (= "/admin/api/expenses/receipts" (:uri req)))
      (is (= 25 (:limit params)))
      (is (= 50 (:offset params)))
      (is (= "review_required" (:status params))))))

(deftest load-list-serializes-multi-select-and-range-filters-with-sort
  (testing "multi-select, text, date range, and sort params survive the same request"
    (reset-test-state!)
    (let [created-at-from (js/Date. "2026-04-01T00:00:00.000Z")
          created-at-to (js/Date. "2026-04-30T12:34:56.000Z")]
      (swap! rf-db/app-db assoc-in (paths/list-filters :receipts)
        {:status [{:value "uploaded" :label "Uploaded"}
                  {:value "review_required" :label "Review required"}]
         :original-filename "IMG_3885"
         :supplier-guess "SAMON"
         :created-at {:from created-at-from :to created-at-to}})
      (swap! rf-db/app-db assoc-in (paths/list-sorts :receipts)
        [{:field :status :direction :asc}])

      (rf/dispatch-sync [::receipts-events/load-list {}])

      (let [req (setup/last-http-request)
            params (:params req)]
        (is (= "/admin/api/expenses/receipts" (:uri req)))
        (is (= 25 (:limit params)))
        (is (= 50 (:offset params)))
        (is (= "uploaded,review_required" (:status params)))
        (is (= "IMG_3885" (:original-filename params)))
        (is (= "SAMON" (:supplier-guess params)))
        (is (= (.toISOString created-at-from) (:created-at-from params)))
        (is (= (.toISOString created-at-to) (:created-at-to params)))
        (is (= "status:asc" (:sort params)))))))