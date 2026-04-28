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
    [app.template.backend.routes.admin.admins :as admins]
    [app.admin.backend.services.admin.admins :as admin-admins]
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
      (with-redefs [admin-admins/list-all-admins (constantly mock-admin-list)
                    admin-admins/get-admin-count (constantly 2)]
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
      (with-redefs [admin-admins/list-all-admins
                    (fn [_db opts]
                      (is (= 10 (:limit opts)))
                      (is (= 0 (:offset opts)))
                      mock-admin-list)
                    admin-admins/get-admin-count (constantly 2)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))

  (testing "list-admins handles search filter"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin
                    {:params {:search "admin1"}})]
      (with-redefs [admin-admins/list-all-admins
                    (fn [_db opts]
                      (is (= "admin1" (:search opts)))
                      [(first mock-admin-list)])
                    admin-admins/get-admin-count (constantly 1)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:admins body))))))))

  (testing "list-admins forwards email and full-name filters"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin
                    {:params {:email "admin1@example.com"
                              :full-name "Admin One"}})]
      (with-redefs [admin-admins/list-all-admins
                    (fn [_db opts]
                      (is (= "admin1@example.com" (:email opts)))
                      (is (= "Admin One" (:full-name opts)))
                      [(first mock-admin-list)])
                    admin-admins/get-admin-count (constantly 1)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))

  (testing "list-admins forwards last-login-at date range"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin
                    {:params {:last-login-at-from "2026-01-01T00:00:00Z"
                              :last-login-at-to "2026-03-31T23:59:59Z"}})]
      (with-redefs [admin-admins/list-all-admins
                    (fn [_db opts]
                      (is (instance? java.time.Instant (:last-login-at-from opts))
                        "last-login-at-from should be parsed to Instant")
                      (is (instance? java.time.Instant (:last-login-at-to opts))
                        "last-login-at-to should be parsed to Instant")
                      mock-admin-list)
                    admin-admins/get-admin-count (constantly 2)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))

  (testing "list-admins ignores invalid date params gracefully"
    (let [db (h/mock-db)
          handler (admins/list-admins-handler db)
          request (h/mock-admin-request :get "/admin/api/admins" mock-admin
                    {:params {:last-login-at-from "not-a-date"}})]
      (with-redefs [admin-admins/list-all-admins
                    (fn [_db opts]
                      (is (nil? (:last-login-at-from opts))
                        "invalid date should parse to nil")
                      mock-admin-list)
                    admin-admins/get-admin-count (constantly 2)]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Get Admin Details Tests
;; ============================================================================

(deftest get-admin-details-handler-test
  (testing "get-admin-details returns admin when found"
    (let [db (h/mock-db)
          handler (admins/get-admin-details-handler db)
          request (h/mock-admin-request :get (str "/admin/api/admins/" test-admin-id) mock-admin
                    {:path-params {:id (str test-admin-id)}})]
      (with-redefs [admin-admins/get-admin-details (constantly mock-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= (str test-admin-id) (str (get-in body [:admin :id]))))))))

  (testing "get-admin-details returns 404 when not found"
    (let [db (h/mock-db)
          handler (admins/get-admin-details-handler db)
          request (h/mock-admin-request :get "/admin/api/admins/nonexistent" mock-admin
                    {:path-params {:id (str (h/random-uuid))}})]
      (with-redefs [admin-admins/get-admin-details (constantly nil)]
        (let [response (handler request)]
          (is (= 404 (:status response))))))))

(deftest reveal-admin-email-route-requires-owner-role-test
  (testing "reveal-email is owner-only, not routine support access"
    (let [reveal-route (some #(when (= "/:id/reveal-email" (first %)) %)
                         (rest (admins/routes :db)))
          role-middleware (-> reveal-route second :post :middleware first)
          protected-handler (role-middleware (fn [_request] {:status 200}))]
      (is (= 403 (:status (protected-handler {:admin {:role "support"}}))))
      (is (= 200 (:status (protected-handler {:admin {:role "owner"}})))))))

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
      (with-redefs [admin-admins/create-admin! (constantly created-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 201 (:status response)))
          (is (some? (get-in body [:admin :id])))))))

  (testing "create-admin fails without email"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body {:password "secure123"}})
          response (handler request)]
      (is (= 400 (:status response)))))

  (testing "create-admin fails without password"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body {:email "new@example.com"}})
          response (handler request)]
      (is (= 400 (:status response)))))

  (testing "create-admin fails with invalid role"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body {:email "new@example.com"
                            :password "secure123"
                            :role "superuser"}})
          response (handler request)]
      (is (= 400 (:status response)))))

  (testing "create-admin surfaces duplicate-owner protection"
    (let [db (h/mock-db)
          handler (admins/create-admin-handler db)
          request (h/mock-admin-request :post "/admin/api/admins" mock-admin
                    {:body {:email "owner2@example.com"
                            :password "secure123"
                            :role "owner"}})]
      (with-redefs [admin-admins/create-admin!
                    (fn [& _]
                      (throw (ex-info "Cannot assign the owner role because an active global owner already exists"
                               {:status 400
                                :field :role
                                :reason :owner-already-exists})))]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

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
      (with-redefs [admin-admins/update-admin! (constantly updated-admin)]
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
      (with-redefs [admin-admins/delete-admin! (constantly {:success true :message "Admin deleted"})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))

  (testing "delete-admin fails when admin not found or protected"
    (let [db (h/mock-db)
          handler (admins/delete-admin-handler db)
          request (h/mock-admin-request :delete "/admin/api/admins/123" mock-admin
                    {:path-params {:id (str (h/random-uuid))}})]
      (with-redefs [admin-admins/delete-admin! (constantly {:success false :message "Cannot delete admin"})]
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
      (with-redefs [admin-admins/update-admin-role! (constantly updated-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (some? (:admin body)))))))

  (testing "update-admin-role fails with invalid role"
    (let [db (h/mock-db)
          handler (admins/update-admin-role-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/role") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:role "invalid"}})
          response (handler request)]
      (is (= 400 (:status response)))))

  (testing "update-admin-role fails without role"
    (let [db (h/mock-db)
          handler (admins/update-admin-role-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/role") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {}})
          response (handler request)]
      (is (= 400 (:status response)))))

  (testing "update-admin-role surfaces duplicate-owner protection"
    (let [db (h/mock-db)
          handler (admins/update-admin-role-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/role") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:role "owner"}})]
      (with-redefs [admin-admins/update-admin-role!
                    (fn [& _]
                      (throw (ex-info "Cannot assign the owner role because an active global owner already exists"
                               {:status 400
                                :field :role
                                :reason :owner-already-exists})))]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

(deftest transfer-admin-ownership-handler-test
  (testing "transfer-admin-ownership succeeds for a valid active admin target"
    (let [db (h/mock-db)
          handler (admins/transfer-admin-ownership-handler db)
          updated-admin (assoc mock-admin :id another-admin-id :email "new-owner@example.com" :role "owner")
          request (h/mock-admin-request :post (str "/admin/api/admins/" another-admin-id "/transfer-ownership") mock-admin
                    {:path-params {:id (str another-admin-id)}})]
      (with-redefs [admin-admins/transfer-admin-ownership! (constantly updated-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= "owner" (get-in body [:admin :role])))))))

  (testing "transfer-admin-ownership fails with invalid admin id"
    (let [db (h/mock-db)
          handler (admins/transfer-admin-ownership-handler db)
          request (h/mock-admin-request :post "/admin/api/admins/not-a-uuid/transfer-ownership" mock-admin
                    {:path-params {:id "not-a-uuid"}})
          response (handler request)]
      (is (= 400 (:status response)))))

  (testing "transfer-admin-ownership surfaces target validation errors"
    (let [db (h/mock-db)
          handler (admins/transfer-admin-ownership-handler db)
          request (h/mock-admin-request :post (str "/admin/api/admins/" another-admin-id "/transfer-ownership") mock-admin
                    {:path-params {:id (str another-admin-id)}})]
      (with-redefs [admin-admins/transfer-admin-ownership!
                    (fn [& _]
                      (throw (ex-info "Ownership can only be transferred to an active admin"
                               {:status 400
                                :field :role
                                :reason :target-must-be-admin})))]
        (let [response (handler request)]
          (is (= 400 (:status response)))))))

  (testing "transfer-admin-ownership surfaces permission errors"
    (let [db (h/mock-db)
          handler (admins/transfer-admin-ownership-handler db)
          request (h/mock-admin-request :post (str "/admin/api/admins/" another-admin-id "/transfer-ownership") mock-admin
                    {:path-params {:id (str another-admin-id)}})]
      (with-redefs [admin-admins/transfer-admin-ownership!
                    (fn [& _]
                      (throw (ex-info "Only the current owner can transfer ownership"
                               {:status 403
                                :field :role
                                :reason :insufficient-permissions})))]
        (let [response (handler request)]
          (is (= 403 (:status response))))))))

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
      (with-redefs [admin-admins/update-admin-status! (constantly updated-admin)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (some? (:admin body)))))))

  (testing "update-admin-status fails with invalid status"
    (let [db (h/mock-db)
          handler (admins/update-admin-status-handler db)
          request (h/mock-admin-request :put (str "/admin/api/admins/" test-admin-id "/status") mock-admin
                    {:path-params {:id (str test-admin-id)}
                     :body {:status "invalid"}})
          response (handler request)]
      (is (= 400 (:status response))))))
