(ns app.backend.routes.admin.settings-test
  "Tests for admin settings API routes.
   
   Tests cover:
   - Getting view options
   - Updating all view options
   - Updating single entity settings
   - Removing entity settings"
  (:require
    [app.backend.routes.admin.settings :as settings]
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

(def mock-view-options
  {:users {:default-sort :email
           :columns-visible [:email :name :status]}
   :admins {:default-sort :created-at
            :columns-visible [:email :role]}})

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "get-view-options-handler returns a function"
    (let [db (h/mock-db)
          handler (settings/get-view-options-handler db)]
      (is (fn? handler))))
  
  (testing "update-view-options-handler returns a function"
    (let [db (h/mock-db)
          handler (settings/update-view-options-handler db)]
      (is (fn? handler))))
  
  (testing "update-entity-setting-handler returns a function"
    (let [db (h/mock-db)
          handler (settings/update-entity-setting-handler db)]
      (is (fn? handler))))
  
  (testing "remove-entity-setting-handler returns a function"
    (let [db (h/mock-db)
          handler (settings/remove-entity-setting-handler db)]
      (is (fn? handler)))))

;; ============================================================================
;; Get View Options Tests
;; ============================================================================

(deftest get-view-options-handler-test
  (testing "get-view-options returns current settings"
    (let [db (h/mock-db)
          handler (settings/get-view-options-handler db)
          request (h/mock-admin-request :get "/admin/api/settings/view-options" mock-admin {})]
      (with-redefs [settings/read-view-options (constantly mock-view-options)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (map? (:view-options body)))))))
  
  (testing "get-view-options returns empty map when no file exists"
    (let [db (h/mock-db)
          handler (settings/get-view-options-handler db)
          request (h/mock-admin-request :get "/admin/api/settings/view-options" mock-admin {})]
      (with-redefs [settings/read-view-options (constantly {})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= {} (:view-options body))))))))

;; ============================================================================
;; Update View Options Tests
;; ============================================================================

(deftest update-view-options-handler-test
  (testing "update-view-options updates all settings"
    (let [db (h/mock-db)
          handler (settings/update-view-options-handler db)
          new-options {:users {:default-sort :name}}
          request (h/mock-admin-request :put "/admin/api/settings/view-options" mock-admin
                    {:body {:view-options new-options}})]
      (with-redefs [settings/write-view-options! (fn [opts] 
                                                    (is (= new-options opts))
                                                    nil)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "update-view-options returns error when view-options missing"
    (let [db (h/mock-db)
          handler (settings/update-view-options-handler db)
          request (h/mock-admin-request :put "/admin/api/settings/view-options" mock-admin
                    {:body {}})
          response (handler request)]
      (is (= 400 (:status response))))))

;; ============================================================================
;; Update Entity Setting Tests
;; ============================================================================

(deftest update-entity-setting-handler-test
  (testing "update-entity-setting updates a single setting"
    (let [db (h/mock-db)
          handler (settings/update-entity-setting-handler db)
          request (h/mock-admin-request :patch "/admin/api/settings/entity" mock-admin
                    {:body {:entity-name "users"
                            :setting-key "default-sort"
                            :setting-value :email}})]
      (with-redefs [settings/read-view-options (constantly mock-view-options)
                    settings/write-view-options! (fn [opts]
                                                    (is (= :email (get-in opts [:users :default-sort])))
                                                    nil)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))
          (is (= "users" (name (:entity body))))))))
  
  (testing "update-entity-setting supports underscore field names"
    (let [db (h/mock-db)
          handler (settings/update-entity-setting-handler db)
          request (h/mock-admin-request :patch "/admin/api/settings/entity" mock-admin
                    {:body {:entity_name "admins"
                            :setting_key "columns-visible"
                            :setting_value [:email :role :status]}})]
      (with-redefs [settings/read-view-options (constantly mock-view-options)
                    settings/write-view-options! (fn [_] nil)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))
  
  (testing "update-entity-setting returns error when fields missing"
    (let [db (h/mock-db)
          handler (settings/update-entity-setting-handler db)
          request (h/mock-admin-request :patch "/admin/api/settings/entity" mock-admin
                    {:body {:entity-name "users"}})
          response (handler request)]
      (is (= 400 (:status response))))))

;; ============================================================================
;; Remove Entity Setting Tests
;; ============================================================================

(deftest remove-entity-setting-handler-test
  (testing "remove-entity-setting removes a setting"
    (let [db (h/mock-db)
          handler (settings/remove-entity-setting-handler db)
          request (h/mock-admin-request :delete "/admin/api/settings/entity" mock-admin
                    {:body {:entity-name "users"
                            :setting-key "default-sort"}})]
      (with-redefs [settings/read-view-options (constantly mock-view-options)
                    settings/write-view-options! (fn [opts]
                                                    (is (nil? (get-in opts [:users :default-sort])))
                                                    nil)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "remove-entity-setting returns error when fields missing"
    (let [db (h/mock-db)
          handler (settings/remove-entity-setting-handler db)
          request (h/mock-admin-request :delete "/admin/api/settings/entity" mock-admin
                    {:body {:entity-name "users"}})
          response (handler request)]
      (is (= 400 (:status response))))))
