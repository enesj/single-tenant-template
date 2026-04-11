(ns app.domain.backend.expenses.routes.blocked-resources-test
  "Unit tests for the impersonation blocking middleware.
   No database required — tests middleware behavior directly."
  (:require
    [app.domain.backend.expenses.routes.middleware :as impersonation-mw]
    [app.domain.backend.expenses.routes.route-configs :as route-configs]
    [clojure.test :refer [deftest is testing]]))

(def ^:private pass-through-handler
  "A handler that returns 200 with the request map."
  (fn [request]
    {:status 200 :body {:passed true :uri (:uri request)}}))

(def ^:private wrapped-handler
  (impersonation-mw/wrap-require-impersonation pass-through-handler))

(deftest wrap-require-impersonation-blocks-without-context
  (testing "returns 403 when :impersonation is absent from request"
    (let [request {:uri "/admin/api/expenses/payers"
                   :admin {:id "admin-1" :role "admin"}}
          response (wrapped-handler request)]
      (is (= 403 (:status response))
        "Should return 403 Forbidden")
      (is (re-find #"impersonation" (str (:body response)))
        "Error message should mention impersonation"))))

(deftest wrap-require-impersonation-allows-with-context
  (testing "passes through when :impersonation is present on request"
    (let [context {:tenant-id "t-1" :role "viewer" :grant-id "g-1"}
          request {:uri "/admin/api/expenses/payers"
                   :admin {:id "admin-1" :role "admin"}
                   :impersonation context}
          response (wrapped-handler request)]
      (is (= 200 (:status response))
        "Should pass through to handler")
      (is (true? (get-in response [:body :passed]))
        "Handler should receive the request"))))

(deftest wrap-require-impersonation-blocks-with-nil-context
  (testing "returns 403 when :impersonation is explicitly nil"
    (let [request {:uri "/admin/api/expenses/payers"
                   :admin {:id "admin-1" :role "admin"}
                   :impersonation nil}
          response (wrapped-handler request)]
      (is (= 403 (:status response))
        "Nil impersonation context should be treated as absent"))))

(deftest admin-expense-routes-no-longer-require-impersonation
  (testing "expense admin routes are no longer wrapped with impersonation middleware"
    (is (nil? (:route-middleware route-configs/expense-config))
      "Admin expenses list/detail routes should be directly available to platform admins")))

(deftest admin-expense-routes-include-total-counts
  (testing "expense admin routes keep count enabled so server pagination can show total records"
    (is (true? (:has-count? route-configs/expense-config))
      "Admin expenses route should include :total in list responses")))
