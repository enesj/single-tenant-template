(ns app.admin.frontend.events.receipts-approval-test
	(:require
		[app.admin.frontend.events.receipts-approval]
		[app.admin.frontend.test-setup :as setup]
		[cljs.test :refer [deftest is testing]]
		[re-frame.core :as rf]
		[re-frame.db :as rf-db]))

(deftest approve-receipt-uses-admin-expenses-approve-endpoint
	(testing ":admin/approve-receipt targets the admin expenses approve endpoint"
		(setup/reset-db!)
		(setup/install-http-stub!)

		(rf/dispatch-sync [:admin/approve-receipt "receipt-123" {:supplier_id "sup-1"} nil])

		(let [req (setup/last-http-request)]
			(is (= :post (:method req)))
			(is (= "/admin/api/expenses/receipts/receipt-123/approve" (:uri req)))
			(is (= {:supplier_id "sup-1"} (:params req)))
			(is (= "test-token" (get-in req [:headers "x-admin-token"])))
			(is (true? (get-in @rf-db/app-db [:admin :receipts :detail :action-loading?]))))))

(deftest save-receipt-review-uses-admin-expenses-review-endpoint
	(testing ":admin/save-receipt-review targets the admin expenses review endpoint"
		(setup/reset-db!)
		(setup/install-http-stub!)

		(rf/dispatch-sync [:admin/save-receipt-review "receipt-123" {:notes "checked"} nil])

		(let [req (setup/last-http-request)]
			(is (= :post (:method req)))
			(is (= "/admin/api/expenses/receipts/receipt-123/review" (:uri req)))
			(is (= {:notes "checked"} (:params req)))
			(is (= "test-token" (get-in req [:headers "x-admin-token"])))
			(is (true? (get-in @rf-db/app-db [:admin :receipts :detail :action-loading?]))))))

(deftest approve-receipt-success-caches-updated-receipt
	(testing ":admin/approve-receipt-success stores the updated receipt and clears loading/error state"
		(setup/reset-db!)
		(swap! rf-db/app-db assoc-in [:admin :receipts :detail :action-loading?] true)
		(swap! rf-db/app-db assoc-in [:admin :receipts :form :loading?] true)
		(swap! rf-db/app-db assoc-in [:admin :receipts :form :error] "boom")

		(rf/dispatch-sync [:admin/approve-receipt-success
											 "receipt-123"
											 nil
											 {:receipt {:id "receipt-123"
																	:status "posted"}}])

		(let [db @rf-db/app-db]
			(is (false? (get-in db [:admin :receipts :detail :action-loading?])))
			(is (false? (get-in db [:admin :receipts :form :loading?])))
			(is (nil? (get-in db [:admin :receipts :form :error])))
			(is (= "posted"
						(get-in db [:admin :receipts :detail :by-id "receipt-123" :status]))))))