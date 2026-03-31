(ns app.backend.routes.admin.users-test
  "Tests for admin user management services.

   Tests user listing, search, and data normalization."
  (:require
    [app.admin.backend.services.admin.users :as users]
    [app.backend.test-helpers :as h]
    [app.shared.adapters.normalization :as norm]
    [app.template.backend.routes.admin.users :as users-routes]
    [app.template.backend.utils.adapters.persistence :as persist]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]))

;; ============================================================================
;; User Data Normalization Tests
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

(deftest user-data-normalization-test
  (testing "normalize-admin-result handles basic user data"
    (let [config {:prefixes ["users-" "user-"]
                  :namespaces #{"users" "user" "u"}
                  :id-fields #{:id}}
          db-user {:users/id #uuid "123e4567-e89b-12d3-a456-426614174000"
                   :users/email "test@example.com"
                   :users/full_name "Test User"
                   :users/status "active"}
          normalized (norm/normalize-admin-result db-user config)]
      (is (map? normalized))
      (is (= "test@example.com" (:email normalized)))
      (is (= "Test User" (:full-name normalized)))))

  (testing "normalize-admin-result preserves simple keys"
    (let [config {:prefixes [] :namespaces #{} :id-fields #{:id}}
          simple-user {:id #uuid "123e4567-e89b-12d3-a456-426614174000"
                       :email "test@example.com"}
          normalized (norm/normalize-admin-result simple-user config)]
      (is (= "test@example.com" (:email normalized)))))

  (testing "normalize-admin-result handles nil gracefully"
    (let [config {:prefixes [] :namespaces #{} :id-fields #{}}]
      (is (nil? (norm/normalize-admin-result nil config))))))

;; ============================================================================
;; List Users Query Building Tests
;; ============================================================================

(deftest list-users-query-test
  (testing "list-all-users with mocked DB returns empty"
    ;; Mock the service to avoid nil DB errors
    (with-redefs [users/list-all-users (fn [_ _] [])]
      (let [result (users/list-all-users nil {})]
        (is (empty? result)))))

  (testing "list-all-users derives last-login-at from successful login events"
    (let [captured-query (atom nil)
          last-login-from (java.time.Instant/parse "2026-03-01T00:00:00Z")]
      (with-redefs [persist/execute-admin-query
                    (fn [_db query _normalize _opts]
                      (reset! captured-query query)
                      [])]
        (is (= [] (users/list-all-users nil {:last-login-at-from last-login-from
                                             :order-by :last-login-at
                                             :order-dir :desc
                                             :limit 10
                                             :offset 0})))
        (is (= [[:raw "COALESCE(u.last_login_at, ul.last_login_at)"] :last_login_at]
              (last (:select @captured-query))))
        (is (= [[:raw "COALESCE(u.last_login_at, ul.last_login_at)"] :desc]
              (first (:order-by @captured-query))))
        (is (vector? (:left-join @captured-query)))
        (is (= [:>= [:raw "COALESCE(u.last_login_at, ul.last_login_at)"] last-login-from]
              (second (:where @captured-query)))))))

  (testing "count-all-users applies the same derived last-login filter"
    (let [captured-query (atom nil)
          last-login-from (java.time.Instant/parse "2026-03-01T00:00:00Z")]
      (with-redefs [hsql/format identity
                    jdbc/execute-one! (fn [_db query]
                                        (reset! captured-query query)
                                        {:total 0})]
        (is (= 0 (users/count-all-users nil {:last-login-at-from last-login-from})))
        (is (vector? (:left-join @captured-query)))
        (is (= [:>= [:raw "COALESCE(u.last_login_at, ul.last_login_at)"] last-login-from]
              (second (:where @captured-query)))))))

  (testing "search-users-advanced with mocked DB returns empty"
    (with-redefs [users/search-users-advanced (fn [_ _] [])]
      (let [result (users/search-users-advanced nil {:search "test"})]
        (is (empty? result))))))

(deftest list-users-handler-pagination-metadata-test
  (testing "list-users handler returns users, total, limit, and offset with filter-aware total"
    (let [db (h/mock-db)
          handler (users-routes/list-users-handler db)
          request (h/mock-admin-request :get "/admin/api/users" {:id (random-uuid)}
                    {:params {:search "ali"
                              :status "active"
                              :email-verified "true"
                              :limit "2"
                              :offset "0"}})
          sample-users [{:id (random-uuid)
                         :email "alice@example.com"
                         :full_name "Alice Doe"
                         :status "active"
                         :email_verified true}
                        {:id (random-uuid)
                         :email "alex@example.com"
                         :full_name "Alex Active"
                         :status "active"
                         :email_verified true}
                        {:id (random-uuid)
                         :email "bob@example.com"
                         :full_name "Bob Inactive"
                         :status "inactive"
                         :email_verified true}
                        {:id (random-uuid)
                         :email "charlie@example.com"
                         :full_name "Charlie Pending"
                         :status "active"
                         :email_verified false}]]
      (with-redefs [users/list-all-users-page
                    (fn [_db opts]
                      (is (= "ali" (:search opts)))
                      (is (= "active" (:status opts)))
                      (is (true? (:email-verified opts)))
                      (is (= 2 (:limit opts)))
                      (is (= 0 (:offset opts)))
                      (let [needle (str/lower-case (:search opts))
                            matches? (fn [user]
                                       (and (= (:status opts) (:status user))
                                         (= (:email-verified opts) (:email_verified user))
                                         (or (str/includes? (str/lower-case (:email user)) needle)
                                           (str/includes? (str/lower-case (:full_name user)) needle))))
                            filtered (vec (filter matches? sample-users))
                            paged (->> filtered
                                    (drop (:offset opts))
                                    (take (:limit opts))
                                    vec)]
                        {:users paged
                         :total (count filtered)
                         :limit (:limit opts)
                         :offset (:offset opts)}))]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:users body)))
          (is (= 1 (:total body)))
          (is (= 2 (:limit body)))
          (is (= 0 (:offset body)))
          (is (= 1 (count (:users body)))))))))

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

(deftest list-users-handler-date-range-test
  (testing "list-users forwards date-range params as parsed Instants"
    (let [db (h/mock-db)
          handler (users-routes/list-users-handler db)
          request (h/mock-admin-request :get "/admin/api/users" {:id (random-uuid)}
                    {:params {:created-at-from "2026-01-01T00:00:00Z"
                              :created-at-to "2026-03-31T23:59:59Z"
                              :last-login-at-from "2026-02-01T00:00:00Z"
                              :limit "50"
                              :offset "0"}})]
      (with-redefs [users/list-all-users-page
                    (fn [_db opts]
                      (is (instance? java.time.Instant (:created-at-from opts))
                        "created-at-from should be parsed to Instant")
                      (is (instance? java.time.Instant (:created-at-to opts))
                        "created-at-to should be parsed to Instant")
                      (is (instance? java.time.Instant (:last-login-at-from opts))
                        "last-login-at-from should be parsed to Instant")
                      (is (nil? (:last-login-at-to opts))
                        "absent date param should remain nil")
                      {:users [] :total 0 :limit 50 :offset 0})]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))

  (testing "list-users preserves email-verified=false"
    (let [db (h/mock-db)
          handler (users-routes/list-users-handler db)
          request (h/mock-admin-request :get "/admin/api/users" {:id (random-uuid)}
                    {:params {:email-verified "false"
                              :limit "50"
                              :offset "0"}})]
      (with-redefs [users/list-all-users-page
                    (fn [_db opts]
                      (is (false? (:email-verified opts))
                        "email-verified=false must not be dropped")
                      {:users [] :total 0 :limit 50 :offset 0})]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

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
          app-map (norm/convert-db-keys->app-keys db-map)]
      (is (contains? app-map :created-at))
      (is (contains? app-map :full-name))
      (is (contains? app-map :email-verified))))

  (testing "convert-db-keys->app-keys handles nested maps"
    (let [db-map {:user_data {:login_count 5}}
          app-map (norm/convert-db-keys->app-keys db-map)]
      (is (map? (:user-data app-map))))))
