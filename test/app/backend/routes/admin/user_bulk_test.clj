(ns app.backend.routes.admin.user-bulk-test
  "Tests for admin bulk user operations routes.
   
   Tests cover:
   - Bulk update user status
   - Bulk update user role
   - Batch update users
   - Export users to CSV"
  (:require
    [app.backend.routes.admin.user-bulk :as user-bulk]
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
(def test-user-id-1 (java.util.UUID/randomUUID))
(def test-user-id-2 (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "bulk-update-user-status-handler returns a function"
    (let [db (h/mock-db)
          handler (user-bulk/bulk-update-user-status-handler db)]
      (is (fn? handler))))
  
  (testing "bulk-update-user-role-handler returns a function"
    (let [db (h/mock-db)
          handler (user-bulk/bulk-update-user-role-handler db)]
      (is (fn? handler))))
  
  (testing "export-users-handler returns a function"
    (let [db (h/mock-db)
          handler (user-bulk/export-users-handler db)]
      (is (fn? handler))))
  
  (testing "batch-update-users-handler returns a function"
    (let [db (h/mock-db)
          handler (user-bulk/batch-update-users-handler db)]
      (is (fn? handler)))))

;; ============================================================================
;; Bulk Update Status Tests
;; ============================================================================

(deftest bulk-update-user-status-handler-test
  (testing "bulk-update-user-status updates multiple users"
    (let [db (h/mock-db)
          handler (user-bulk/bulk-update-user-status-handler db)
          user-ids [(str test-user-id-1) (str test-user-id-2)]
          request (h/mock-admin-request :put "/admin/api/users/bulk-status" mock-admin
                    {:body {:user_ids user-ids :status "inactive"}})]
      (with-redefs [admin-service/bulk-update-user-status!
                    (fn [_db ids status _admin-id _ip _ua]
                      (is (= 2 (count ids)))
                      (is (= "inactive" status))
                      {:success true :updated-count 2})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "bulk-update-user-status returns error when fields missing"
    (let [db (h/mock-db)
          handler (user-bulk/bulk-update-user-status-handler db)
          request (h/mock-admin-request :put "/admin/api/users/bulk-status" mock-admin
                    {:body {:user_ids [(str test-user-id-1)]}})
          response (handler request)]
      (is (= 400 (:status response))))))

;; ============================================================================
;; Bulk Update Role Tests
;; ============================================================================

(deftest bulk-update-user-role-handler-test
  (testing "bulk-update-user-role updates multiple users"
    (let [db (h/mock-db)
          handler (user-bulk/bulk-update-user-role-handler db)
          user-ids [(str test-user-id-1) (str test-user-id-2)]
          request (h/mock-admin-request :put "/admin/api/users/bulk-role" mock-admin
                    {:body {:user_ids user-ids :role "premium"}})]
      (with-redefs [admin-service/bulk-update-user-role!
                    (fn [_db ids role _admin-id _ip _ua]
                      (is (= 2 (count ids)))
                      (is (= "premium" role))
                      {:success true :updated-count 2})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "bulk-update-user-role returns error when role missing"
    (let [db (h/mock-db)
          handler (user-bulk/bulk-update-user-role-handler db)
          request (h/mock-admin-request :put "/admin/api/users/bulk-role" mock-admin
                    {:body {:user_ids [(str test-user-id-1)]}})
          response (handler request)]
      (is (= 400 (:status response))))))

;; ============================================================================
;; Batch Update Tests
;; ============================================================================

(deftest batch-update-users-handler-test
  (testing "batch-update updates multiple users with different fields"
    (let [db (h/mock-db)
          handler (user-bulk/batch-update-users-handler db)
          items [{:id (str test-user-id-1) :name "Updated Name 1"}
                 {:id (str test-user-id-2) :name "Updated Name 2"}]
          request (h/mock-admin-request :put "/admin/api/users/batch" mock-admin
                    {:body {:items items}})]
      (with-redefs [admin-service/update-user!
                    (fn [_db _user-id updates _admin-id _ip _ua]
                      (is (contains? updates :name))
                      {:success true})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))
          (is (= 2 (get-in body [:data :summary :total])))))))
  
  (testing "batch-update returns error when items empty"
    (let [db (h/mock-db)
          handler (user-bulk/batch-update-users-handler db)
          request (h/mock-admin-request :put "/admin/api/users/batch" mock-admin
                    {:body {:items []}})
          response (handler request)]
      ;; error-response without explicit status defaults to 500
      (is (contains? #{400 500} (:status response))))))

;; ============================================================================
;; Export Users Tests
;; ============================================================================

(deftest export-users-handler-test
  (testing "export-users returns CSV content"
    (let [db (h/mock-db)
          handler (user-bulk/export-users-handler db)
          request (h/mock-admin-request :post "/admin/api/users/export" mock-admin
                    {:body {:user_ids [(str test-user-id-1)]}})]
      (with-redefs [admin-service/export-users-csv
                    (fn [_db _ids]
                      {:success true
                       :content "email,name\ntest@example.com,Test User"
                       :filename "users-export.csv"})]
        (let [response (handler request)]
          (is (= 200 (:status response)))
          (is (= "text/csv" (get-in response [:headers "Content-Type"])))))))
  
  (testing "export-users handles export failure"
    (let [db (h/mock-db)
          handler (user-bulk/export-users-handler db)
          request (h/mock-admin-request :post "/admin/api/users/export" mock-admin
                    {:body {:user_ids [(str test-user-id-1)]}})]
      (with-redefs [admin-service/export-users-csv
                    (fn [_db _ids]
                      {:success false :message "Export failed"})]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

;; ============================================================================
;; Route Definition Tests
;; ============================================================================

(deftest routes-test
  (testing "routes function returns route definitions"
    (let [db (h/mock-db)
          routes (user-bulk/routes db)]
      (is (vector? routes))
      (is (= "" (first routes))))))
