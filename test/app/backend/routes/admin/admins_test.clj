(ns app.backend.routes.admin.admins-test
  "Tests for admin management API routes.
   
   Tests cover:
   - Listing admins with pagination and filters
   - Getting admin details
   - Creating new admins
   - Updating admin info
   - Deleting admins
   - Role and status management"
  (:require
    [app.backend.routes.admin.admins :as admins]
    [app.backend.services.admin :as admin-service]
    [app.backend.services.admin.admins :as admin-admins-service]
    [app.backend.test-helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

;; ============================================================================
;; Test Data
;; ============================================================================

(def test-admin-id (h/random-uuid))
(def another-admin-id (h/random-uuid))

(def mock-admin
  {:id test-admin-id
   :email "test@example.com"
   :full_name "Test Admin"
   :role "owner"
   :status "active"})

(def mock-admin-list
  [{:id test-admin-id
    :email "admin1@example.com"
    :full_name "Admin One"
    :role "owner"
    :status "active"}
   {:id another-admin-id
    :email "admin2@example.com"
    :full_name "Admin Two"
    :role "admin"
    :status "active"}])

;; ============================================================================
;; List Admins Tests
;; ============================================================================

(deftest list-admins-handler-test
  (testing "list-admins returns all admins"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin {})]
      (with-redefs [admin-service/list-all-admins (constantly mock-admin-list)
                    admin-service/get-admin-count (constantly 2)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= 2 (:total body)))
          (is (= 2 (count (:admins body))))))))
  
  (testing "list-admins respects pagination"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin
                    {:params {:limit "10" :offset "0"}})]
      (with-redefs [admin-service/list-all-admins 
                    (fn [_db opts]
                      (is (= 10 (:limit opts)))
                      (is (= 0 (:offset opts)))
                      mock-admin-list)
                    admin-service/get-admin-count (constantly 2)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))
  
  (testing "list-admins handles search filter"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin
                    {:params {:search "admin1"}})]
      (with-redefs [admin-service/list-all-admins 
                    (fn [_db opts]
                      (is (= "admin1" (:search opts)))
                      [(first mock-admin-list)])
                    admin-service/get-admin-count (constantly 1)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:admins body)))))))))

;; ============================================================================
;; Get Admin Details Tests
;; ============================================================================

(deftest get-admin-details-handler-test
  (testing "get-admin-details returns admin when found"
    (let [db (h/mock-db)
          handler (admins/get-admin-details-handler db)
          request (h/mock-admin-request :get (str "/admin/api/admins/" test-admin-id) mock-admin
                    {:path-params {:id (str test-admin-id)}})]
      (with-redefs [admin-service/get-admin-details (constantly mock-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= (str test-admin-id) (str (get-in body [:admin :id]))))))))
  
  (testing "get-admin-details returns 404 when not found"
    (let [db (h/mock-db)
          handler (admins/get-admin-details-handler db)
          request (h/mock-admin-request :get "/admin/api/admins/nonexistent" mock-admin
                    {:path-params {:id (str (h/random-uuid))}})]
      (with-redefs [admin-service/get-admin-details (constantly nil)]
        (let [response (handler request)]
          (is (= 404 (:status response))))))))

;; ============================================================================
;; Create Admin Tests
;; ============================================================================

(deftest create-admin-handler-test
  (testing "create-admin creates new admin with valid data"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          new-admin-data {:email "new@example.com"
                          :password "secure123"
                          :full_name "New Admin"
                          :role "admin"}
          created-admin (assoc new-admin-data :id (h/random-uuid))
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body new-admin-data})]
      (with-redefs [admin-service/create-admin-with-audit! (constantly created-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 201 (:status response)))
          (is (some? (get-in body [:admin :id])))))))
  
  (testing "create-admin fails without email"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body {:password "secure123"}})]
      (let [response (handler request)]
        (is (= 400 (:status response))))))
  
  (testing "create-admin fails without password"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body {:email "new@example.com"}})]
      (let [response (handler request)]
        (is (= 400 (:status response)))))))

;; ============================================================================
;; Update Admin Tests
;; ============================================================================

(deftest update-admin-handler-test
  (testing "update-admin updates admin successfully"
    (let [db (h/mock-db)
          handler (admins/update-admin-handler db)
          updated-admin (assoc mock-admin :full_name "Updated Name")
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id) mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:full_name "Updated Name"}})]
      (with-redefs [admin-service/update-admin! (constantly updated-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (some? (:admin body))))))))

;; ============================================================================
;; Delete Admin Tests
;; ============================================================================

(deftest delete-admin-handler-test
  (testing "delete-admin deletes admin successfully"
    (let [db (h/mock-db)
          handler (admins/delete-admin-handler db)
          request (h/mock-admin-request :delete (str "/admin/api/admins/" test-admin-id) mock-admin
                    {:path-params {:id (str test-admin-id)}})]
      (with-redefs [admin-service/delete-admin! (constantly {:success true :message "Admin deleted"})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "delete-admin fails when admin not found or protected"
    (let [db (h/mock-db)
          handler (admins/delete-admin-handler db)
          request (h/mock-admin-request :delete "/admin/api/admins/123" mock-admin
                    {:path-params {:id (str (h/random-uuid))}})]
      (with-redefs [admin-service/delete-admin! (constantly {:success false :message "Cannot delete admin"})]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

;; ============================================================================
;; Update Admin Role Tests
;; ============================================================================

(deftest update-admin-role-handler-test
  (testing "update-admin-role changes role successfully"
    (let [db (h/mock-db)
          handler (admins/update-admin-role-handler db)
          updated-admin (assoc mock-admin :role "support")
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/role") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:role "support"}})]
      (with-redefs [admin-service/update-admin-role! (constantly updated-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (some? (:admin body)))))))
  
  (testing "update-admin-role fails with invalid role"
    (let [db (h/mock-db)
          handler (admins/update-admin-role-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/role") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:role "invalid"}})]
      (let [response (handler request)]
        (is (= 400 (:status response))))))
  
  (testing "update-admin-role fails without role"
    (let [db (h/mock-db)
          handler (admins/update-admin-role-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/role") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {}})]
      (let [response (handler request)]
        (is (= 400 (:status response)))))))

;; ============================================================================
;; Update Admin Status Tests
;; ============================================================================

(deftest update-admin-status-handler-test
  (testing "update-admin-status changes status successfully"
    (let [db (h/mock-db)
          handler (admins/update-admin-status-handler db)
          updated-admin (assoc mock-admin :status "suspended")
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/status") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:status "suspended"}})]
      (with-redefs [admin-service/update-admin-status! (constantly updated-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (some? (:admin body)))))))
  
  (testing "update-admin-status fails with invalid status"
    (let [db (h/mock-db)
          handler (admins/update-admin-status-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/status") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:status "invalid"}})]
      (let [response (handler request)]
        (is (= 400 (:status response)))))))
