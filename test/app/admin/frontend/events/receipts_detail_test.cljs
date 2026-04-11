(ns app.admin.frontend.events.receipts-detail-test
  (:require
    [app.admin.frontend.events.receipts-detail]
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest fetch-receipt-detail-uses-admin-expenses-endpoint
  (testing ":admin/fetch-receipt-detail targets the admin expenses receipt detail endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (rf/dispatch-sync [:admin/fetch-receipt-detail "receipt-123"])

    (let [req (setup/last-http-request)]
      (is (= :get (:method req)))
      (is (= "/admin/api/expenses/receipts/receipt-123" (:uri req)))
      (is (= "test-token" (get-in req [:headers "x-admin-token"])))
      (is (true? (get-in @rf-db/app-db [:admin :receipts :detail :loading?]))))))

(deftest fetch-receipt-detail-success-caches-receipt
  (testing ":admin/fetch-receipt-detail-success stores the fetched receipt under its string id"
    (setup/reset-db!)
    (swap! rf-db/app-db assoc-in [:admin :receipts :detail :loading?] true)

    (rf/dispatch-sync [:admin/fetch-receipt-detail-success
                       "receipt-123"
                       {:receipt {:id "receipt-123"
                                  :original-filename "sample.pdf"}}])

    (let [db @rf-db/app-db]
      (is (false? (get-in db [:admin :receipts :detail :loading?])))
      (is (nil? (get-in db [:admin :receipts :detail :error])))
      (is (= "sample.pdf"
            (get-in db [:admin :receipts :detail :by-id "receipt-123" :original-filename]))))))