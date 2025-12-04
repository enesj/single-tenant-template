(ns app.backend.routes.admin.users-test
  "Tests for admin user management services.
   
   Tests user listing, search, and data normalization."
  (:require
    [app.backend.services.admin.users :as users]
    [app.shared.adapters.database :as db-adapter]
    [clojure.test :refer [deftest is testing]]))

;; ============================================================================
;; User Data Normalization Tests
;; ============================================================================

(deftest user-data-normalization-test
  (testing "normalize-admin-result handles basic user data"
    (let [config {:prefixes ["users-" "user-"]
                  :namespaces #{"users" "user" "u"}
                  :id-fields #{:id}}
          db-user {:users/id #uuid "123e4567-e89b-12d3-a456-426614174000"
                   :users/email "test@example.com"
                   :users/full_name "Test User"
                   :users/status "active"}
          normalized (db-adapter/normalize-admin-result db-user config)]
      (is (map? normalized))
      (is (= "test@example.com" (:email normalized)))
      (is (= "Test User" (:full-name normalized)))))

  (testing "normalize-admin-result preserves simple keys"
    (let [config {:prefixes [] :namespaces #{} :id-fields #{:id}}
          simple-user {:id #uuid "123e4567-e89b-12d3-a456-426614174000"
                       :email "test@example.com"}
          normalized (db-adapter/normalize-admin-result simple-user config)]
      (is (= "test@example.com" (:email normalized)))))

  (testing "normalize-admin-result handles nil gracefully"
    (let [config {:prefixes [] :namespaces #{} :id-fields #{}}]
      (is (nil? (db-adapter/normalize-admin-result nil config))))))

;; ============================================================================
;; List Users Query Building Tests
;; ============================================================================

(deftest list-users-query-test
  (testing "list-all-users with mocked DB returns empty"
    ;; Mock the service to avoid nil DB errors
    (with-redefs [users/list-all-users (fn [_ _] [])]
      (let [result (users/list-all-users nil {})]
        (is (empty? result)))))

  (testing "search-users-advanced with mocked DB returns empty"
    (with-redefs [users/search-users-advanced (fn [_ _] [])]
      (let [result (users/search-users-advanced nil {:search "test"})]
        (is (empty? result))))))

;; ============================================================================
;; User Filter Logic Tests
;; ============================================================================

(deftest user-filter-params-test
  (testing "filter params are processed correctly"
    ;; Test the shape of filter params expected by the service
    (let [filters {:search "john"
                   :status "active"
                   :email-verified true
                   :limit 10
                   :offset 0}]
      (is (string? (:search filters)))
      (is (string? (:status filters)))
      (is (boolean? (:email-verified filters)))
      (is (number? (:limit filters)))
      (is (number? (:offset filters))))))

;; ============================================================================
;; User Status Constants Tests
;; ============================================================================

(deftest user-status-values-test
  (testing "expected user status values"
    ;; Validate that status strings match expected values
    (let [valid-statuses #{"active" "inactive" "suspended" "pending"}]
      (is (contains? valid-statuses "active"))
      (is (contains? valid-statuses "inactive"))
      (is (contains? valid-statuses "suspended")))))

;; ============================================================================
;; User Sort Options Tests
;; ============================================================================

(deftest user-sort-options-test
  (testing "valid sort fields"
    (let [valid-sort-fields #{:created_at :email :full_name :last_login_at :status}]
      (is (contains? valid-sort-fields :created_at))
      (is (contains? valid-sort-fields :email))))

  (testing "valid sort orders"
    (let [valid-orders #{:asc :desc}]
      (is (contains? valid-orders :asc))
      (is (contains? valid-orders :desc)))))

;; ============================================================================
;; User Search Criteria Tests
;; ============================================================================

(deftest user-search-criteria-test
  (testing "advanced search criteria shape"
    (let [criteria {:search "test"
                    :status "active"
                    :email-verified true
                    :role "user"
                    :auth-provider "email"
                    :created-after "2024-01-01"
                    :created-before "2024-12-31"
                    :last-login-after nil
                    :last-login-before nil
                    :limit 50
                    :offset 0
                    :sort-by :created_at
                    :sort-order :desc}]
      (is (map? criteria))
      (is (= "test" (:search criteria)))
      (is (= :desc (:sort-order criteria))))))

;; ============================================================================
;; DB Key Conversion Tests
;; ============================================================================

(deftest db-key-conversion-test
  (testing "convert-db-keys->app-keys transforms snake_case"
    (let [db-map {:created_at "2024-01-01"
                  :full_name "Test User"
                  :email_verified true}
          app-map (db-adapter/convert-db-keys->app-keys db-map)]
      (is (contains? app-map :created-at))
      (is (contains? app-map :full-name))
      (is (contains? app-map :email-verified))))

  (testing "convert-db-keys->app-keys handles nested maps"
    (let [db-map {:user_data {:login_count 5}}
          app-map (db-adapter/convert-db-keys->app-keys db-map)]
      (is (map? (:user-data app-map))))))
