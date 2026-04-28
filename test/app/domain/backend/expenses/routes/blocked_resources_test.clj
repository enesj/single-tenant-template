(ns app.domain.backend.expenses.routes.blocked-resources-test
  "Unit tests for admin-private resource blocking middleware.
   No database required — tests middleware behavior directly."
  (:require
    [app.domain.backend.expenses.routes.middleware :as private-resource-mw]
    [app.domain.backend.expenses.routes.route-configs :as route-configs]
    [clojure.test :refer [deftest is testing]]))

(def ^:private pass-through-handler
  "A handler that would return 200 if private-resource middleware allowed it."
  (fn [request]
    {:status 200 :body {:passed true :uri (:uri request)}}))

(def ^:private wrapped-handler
  (private-resource-mw/wrap-block-private-admin-resource pass-through-handler))

(deftest wrap-block-private-admin-resource-always-blocks
  (testing "returns 403 even if a stale impersonation context is present"
    (let [request {:uri "/admin/api/expenses/payers"
                   :admin {:id "admin-1" :role "admin"}
                   :impersonation {:tenant-id "t-1" :role "viewer" :grant-id "g-1"}}
          response (wrapped-handler request)]
      (is (= 403 (:status response))
        "Private tenant-scoped admin resources should remain unavailable")
      (is (re-find #"disabled" (str (:body response)))
        "Error message should explain that admin access is disabled"))))

(deftest payer-and-item-routes-block-private-admin-access
  (testing "payer and expense-item admin routes keep the private-resource blocker"
    (is (seq (:route-middleware route-configs/payer-config))
      "Payer routes should keep blocking middleware")
    (is (seq (:route-middleware route-configs/expense-item-config))
      "Expense item routes should keep blocking middleware")))

(deftest admin-expense-routes-no-longer-require-impersonation
  (testing "expense admin routes are privacy-scrubbed instead of impersonation-gated"
    (is (nil? (:route-middleware route-configs/expense-config))
      "Admin expenses list/detail routes should be directly available to platform admins")))

(deftest admin-expense-routes-include-total-counts
  (testing "expense admin routes keep count enabled so server pagination can show total records"
    (is (true? (:has-count? route-configs/expense-config))
      "Admin expenses route should include :total in list responses")))
