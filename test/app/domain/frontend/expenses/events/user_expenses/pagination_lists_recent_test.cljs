(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists-recent-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest recent-go-to-page-fetches-next-server-page
  (testing "recent-go-to-page computes offset from page + limit"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 2 :limit 25}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses" (sup/req-uri req)))
      (is (= {:limit 25 :offset 25}
            (sup/req-params req))))
    (is (= 2 (get-in @rf-db/app-db [:user-expenses :recent :page])))
    (is (= 25 (get-in @rf-db/app-db [:user-expenses :recent :limit])))
    (is (= 25 (get-in @rf-db/app-db [:user-expenses :recent :offset])))))