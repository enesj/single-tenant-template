(ns app.backend.routes.admin.entities-test
  "Tests for admin entity CRUD routes.
   
   Tests cover:
   - Delete entity handler (with dry-run, force-delete)
   - Create entity handler
   - Error handling for unsupported entities"
  (:require
    [app.backend.routes.admin.entities :as entities]
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
(def test-user-id (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

(def mock-crud-service nil)

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "delete-entity-handler returns a function"
    (let [db (h/mock-db)
          handler (entities/delete-entity-handler db mock-crud-service)]
      (is (fn? handler))))
  
  (testing "create-entity-handler returns a function"
    (let [db (h/mock-db)
          handler (entities/create-entity-handler db mock-crud-service)]
      (is (fn? handler)))))

;; ============================================================================
;; Delete Entity Tests
;; ============================================================================

(deftest delete-entity-handler-test
  (testing "delete-entity performs dry-run successfully"
    (let [db (h/mock-db)
          handler (entities/delete-entity-handler db mock-crud-service)
          request (h/mock-admin-request :delete (str "/admin/api/entities/users/" test-user-id) mock-admin
                    {:path-params {:entity "users" :id (str test-user-id)}
                     :body {:dry-run true}})]
      (with-redefs [admin-service/delete-user!
                    (fn [_db user-id _admin-id _ip _ua & {:keys [dry-run]}]
                      (is (= test-user-id user-id))
                      (is (true? dry-run))
                      {:dry-run true
                       :message "Dry-run complete"
                       :impact-summary {:related-records 5}})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))
          (is (true? (or (:dry-run body) (:dryRun body))))))))
  
  (testing "delete-entity deletes user successfully"
    (let [db (h/mock-db)
          handler (entities/delete-entity-handler db mock-crud-service)
          request (h/mock-admin-request :delete (str "/admin/api/entities/users/" test-user-id) mock-admin
                    {:path-params {:entity "users" :id (str test-user-id)}
                     :body {}})]
      (with-redefs [admin-service/delete-user!
                    (fn [_db user-id _admin-id _ip _ua & _opts]
                      (is (= test-user-id user-id))
                      {:success true
                       :message "User deleted"
                       :deleted-at (java.time.Instant/now)})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "delete-entity with force-delete flag"
    (let [db (h/mock-db)
          handler (entities/delete-entity-handler db mock-crud-service)
          request (h/mock-admin-request :delete (str "/admin/api/entities/users/" test-user-id) mock-admin
                    {:path-params {:entity "users" :id (str test-user-id)}
                     :body {:force-delete true}})]
      (with-redefs [admin-service/delete-user!
                    (fn [_db _user-id _admin-id _ip _ua & {:keys [force-delete]}]
                      (is (true? force-delete))
                      {:success true :message "User force deleted"})]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))
  
  (testing "delete-entity returns 501 for unsupported entity"
    (let [db (h/mock-db)
          handler (entities/delete-entity-handler db mock-crud-service)
          request (h/mock-admin-request :delete "/admin/api/entities/unknown/123" mock-admin
                    {:path-params {:entity "unknown" :id "123"}})]
      (let [response (handler request)]
        (is (= 501 (:status response))))))
  
  (testing "delete-entity handles constraint violations"
    (let [db (h/mock-db)
          handler (entities/delete-entity-handler db mock-crud-service)
          request (h/mock-admin-request :delete (str "/admin/api/entities/users/" test-user-id) mock-admin
                    {:path-params {:entity "users" :id (str test-user-id)}})]
      (with-redefs [admin-service/delete-user!
                    (fn [_db _user-id _admin-id _ip _ua & _opts]
                      (throw (ex-info "Cannot delete user with active subscriptions"
                               {:status 400
                                :reason :has-active-subscriptions})))]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

;; ============================================================================
;; Create Entity Tests
;; ============================================================================

(deftest create-entity-handler-test
  (testing "create-entity creates user successfully"
    (let [db (h/mock-db)
          handler (entities/create-entity-handler db mock-crud-service)
          user-data {:email "newuser@example.com" :name "New User" :password "SecurePass123!"}
          request (h/mock-admin-request :post "/admin/api/entities/users" mock-admin
                    {:path-params {:entity "users"}
                     :body user-data})]
      (with-redefs [admin-service/create-user!
                    (fn [_db data _admin-id _ip _ua]
                      (is (= "newuser@example.com" (:email data)))
                      {:id (java.util.UUID/randomUUID)
                       :email (:email data)
                       :name (:name data)})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (or (:id body) (:success body)))))))
  
  (testing "create-entity handles validation errors"
    (let [db (h/mock-db)
          handler (entities/create-entity-handler db mock-crud-service)
          request (h/mock-admin-request :post "/admin/api/entities/users" mock-admin
                    {:path-params {:entity "users"}
                     :body {:email "invalid-email"}})]
      (with-redefs [admin-service/create-user!
                    (fn [_db _data _admin-id _ip _ua]
                      (throw (ex-info "Invalid email format"
                               {:status 400
                                :validation-errors [{:field :email :message "Invalid format"}]})))]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))
