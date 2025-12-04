(ns app.backend.routes.admin.dashboard-test
  "Tests for admin dashboard statistics service.
   
   Tests dashboard stats structure and fallback behavior."
  (:require
    [app.backend.services.admin.auth :as admin-auth]
    [app.backend.services.admin.dashboard :as dashboard]
    [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================================
;; Dashboard Stats Structure Tests
;; ============================================================================

(deftest dashboard-stats-structure-test
  (testing "get-dashboard-stats returns expected keys when mocked"
    (with-redefs [dashboard/get-dashboard-stats 
                  (fn [_] {:total-admins 5
                           :active-sessions 3
                           :recent-activity 10
                           :recent-events [{:action "login" :count 5}]})]
      (let [stats (dashboard/get-dashboard-stats nil)]
        (is (contains? stats :total-admins))
        (is (contains? stats :active-sessions))
        (is (contains? stats :recent-activity))
        (is (contains? stats :recent-events))
        (is (= 5 (:total-admins stats)))
        (is (vector? (:recent-events stats)))))))

(deftest dashboard-stats-fallback-test
  (testing "get-dashboard-stats returns zeros on error"
    ;; When DB is nil, the function should return fallback values
    (let [stats (dashboard/get-dashboard-stats nil)]
      (is (map? stats))
      (is (= 0 (:total-admins stats)))
      (is (>= (:active-sessions stats) 0))
      (is (= 0 (:recent-activity stats)))
      (is (vector? (:recent-events stats))))))

;; ============================================================================
;; Advanced Dashboard Tests
;; ============================================================================

(deftest advanced-dashboard-structure-test
  (testing "get-advanced-dashboard-data returns expected structure"
    (let [data (dashboard/get-advanced-dashboard-data nil)]
      (is (map? data))
      (is (contains? data :tenant-metrics))
      (is (contains? data :user-metrics))
      (is (contains? data :risk-alerts))
      (is (contains? data :tenant-spec))
      (is (contains? data :user-spec))))

  (testing "tenant-metrics contains expected fields"
    (let [data (dashboard/get-advanced-dashboard-data nil)
          metrics (:tenant-metrics data)]
      (is (map? metrics))
      (is (contains? metrics :active-tenants))
      (is (contains? metrics :tenant-growth-rate))
      (is (contains? metrics :avg-health-score))
      (is (contains? metrics :monthly-revenue))))

  (testing "user-metrics contains expected fields"
    (let [data (dashboard/get-advanced-dashboard-data nil)
          metrics (:user-metrics data)]
      (is (map? metrics))
      (is (contains? metrics :high-risk-users))
      (is (contains? metrics :risk-change-rate))))

  (testing "risk-alerts is a collection"
    (let [data (dashboard/get-advanced-dashboard-data nil)
          alerts (:risk-alerts data)]
      (is (coll? alerts))
      (when (seq alerts)
        (is (every? #(contains? % :id) alerts))
        (is (every? #(contains? % :title) alerts))))))

;; ============================================================================
;; Session Store Integration Tests
;; ============================================================================

(deftest session-store-integration-test
  (testing "active-sessions counts in-memory sessions"
    ;; Save original session store state
    (let [original-sessions @admin-auth/session-store]
      (try
        ;; Add some test sessions
        (reset! admin-auth/session-store 
                {"token1" {:admin-id 1}
                 "token2" {:admin-id 2}})
        
        (let [stats (dashboard/get-dashboard-stats nil)]
          (is (= 2 (:active-sessions stats))))
        
        (finally
          ;; Restore original state
          (reset! admin-auth/session-store original-sessions)))))

  (testing "active-sessions handles empty session store"
    (let [original-sessions @admin-auth/session-store]
      (try
        (reset! admin-auth/session-store {})
        (let [stats (dashboard/get-dashboard-stats nil)]
          (is (= 0 (:active-sessions stats))))
        (finally
          (reset! admin-auth/session-store original-sessions))))))
