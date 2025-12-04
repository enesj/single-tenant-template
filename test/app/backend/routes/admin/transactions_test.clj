(ns app.backend.routes.admin.transactions-test
  "Tests for admin transaction management routes.
   
   Tests cover:
   - Transaction overview
   - Transaction trends
   - Suspicious transactions detection"
  (:require
    [app.backend.routes.admin.transactions :as transactions]
    [app.backend.services.admin :as admin-service]
    [app.backend.test-helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

;; ============================================================================
;; Test Data
;; ============================================================================

(def test-admin-id (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

(def mock-transaction-overview
  {:total-transactions 1500
   :total-volume 125000.00
   :average-transaction 83.33
   :transactions-today 45
   :volume-today 3750.00})

(def mock-transaction-trends
  [{:period "2025-01" :count 500 :volume 41000}
   {:period "2025-02" :count 520 :volume 43000}
   {:period "2025-03" :count 480 :volume 41000}])

(def mock-suspicious-transactions
  [{:id (java.util.UUID/randomUUID)
    :amount 9999.99
    :reason "Near threshold"
    :created_at "2025-01-15T10:30:00Z"}])

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "get-transaction-overview-handler returns a function"
    (let [db (h/mock-db)
          handler (transactions/get-transaction-overview-handler db)]
      (is (fn? handler))))
  
  (testing "get-transaction-trends-handler returns a function"
    (let [db (h/mock-db)
          handler (transactions/get-transaction-trends-handler db)]
      (is (fn? handler))))
  
  (testing "get-suspicious-transactions-handler returns a function"
    (let [db (h/mock-db)
          handler (transactions/get-suspicious-transactions-handler db)]
      (is (fn? handler)))))

;; ============================================================================
;; Transaction Overview Tests
;; ============================================================================

(deftest get-transaction-overview-handler-test
  (testing "get-transaction-overview returns overview data"
    (let [db (h/mock-db)
          handler (transactions/get-transaction-overview-handler db)
          request (h/mock-admin-request :get "/admin/api/transactions/overview" mock-admin {})]
      (with-redefs [admin-service/get-transaction-overview
                    (constantly mock-transaction-overview)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (number? (or (:total-transactions body) (:totalTransactions body)))))))))

;; ============================================================================
;; Transaction Trends Tests
;; ============================================================================

(deftest get-transaction-trends-handler-test
  (testing "get-transaction-trends returns trend data"
    (let [db (h/mock-db)
          handler (transactions/get-transaction-trends-handler db)
          request (h/mock-admin-request :get "/admin/api/transactions/trends" mock-admin
                    {:params {:period "month" :months "12"}})]
      (with-redefs [admin-service/get-transaction-trends
                    (fn [_db opts]
                      (is (= :month (:period opts)))
                      (is (= 12 (:months opts)))
                      mock-transaction-trends)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))
  
  (testing "get-transaction-trends uses default period"
    (let [db (h/mock-db)
          handler (transactions/get-transaction-trends-handler db)
          request (h/mock-admin-request :get "/admin/api/transactions/trends" mock-admin {})]
      (with-redefs [admin-service/get-transaction-trends
                    (fn [_db opts]
                      (is (= :month (:period opts)))
                      mock-transaction-trends)]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Suspicious Transactions Tests
;; ============================================================================

(deftest get-suspicious-transactions-handler-test
  (testing "get-suspicious-transactions returns flagged transactions"
    (let [db (h/mock-db)
          handler (transactions/get-suspicious-transactions-handler db)
          request (h/mock-admin-request :get "/admin/api/transactions/suspicious" mock-admin
                    {:params {:limit "50" :offset "0"}})]
      (with-redefs [admin-service/get-suspicious-transactions
                    (fn [_db pagination]
                      (is (= 50 (:limit pagination)))
                      mock-suspicious-transactions)]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Route Definition Tests
;; ============================================================================

(deftest routes-test
  (testing "routes function returns route definitions"
    (let [db (h/mock-db)
          routes (transactions/routes db)]
      (is (vector? routes))
      (is (= "" (first routes))))))
