(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest recent-go-to-page-applies-new-limit-and-reuses-it
  (testing "page-size changes fetch page 1 with offset 0, and later page changes reuse stored limit"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 1 :limit 50}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses" (sup/req-uri req)))
      (is (= {:limit 50 :offset 0}
            (sup/req-params req))))
    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 2}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses" (sup/req-uri req)))
      (is (= {:limit 50 :offset 50}
            (sup/req-params req))))
    (is (= 2 (count @sup/captured-http-requests)))
    (is (= 2 (get-in @rf-db/app-db [:user-expenses :recent :page])))
    (is (= 50 (get-in @rf-db/app-db [:user-expenses :recent :limit])))
    (is (= 50 (get-in @rf-db/app-db [:user-expenses :recent :offset])))))

