(ns app.template.di.container-test
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.email.service :as email-service]
    [app.template.di.config :as di-config]
    [app.template.protocols :as core-protocols]
    [clojure.test :refer [deftest is run-tests testing]]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Test Configuration and Setup
;; ============================================================================

;; Raw models in DB (snake_case) format - mimics what's in models.edn
(def raw-test-models
  {:properties
   {:fields [[:id :uuid {:null false :primary-key true}]
             [:tenant_id :uuid {:null false}]
             [:name :varchar {:null false :max-length 255}]
             [:address :text {:null true}]
             [:created_at :timestamptz {:null false}]
             [:updated_at :timestamptz {:null false}]]
    :constraints {:unique [[:tenant_id :name]]}}

   :users
   {:fields [[:id :uuid {:null false :primary-key true}]
             [:tenant_id :uuid {:null false}]
             [:email :varchar {:null false :max-length 255}]
             [:full_name :varchar {:null false :max-length 255}]
             [:role :varchar {:null false :max-length 50}]
             [:created_at :timestamptz {:null false}]
             [:updated_at :timestamptz {:null false}]]
    :constraints {:unique [[:tenant_id :email]]}}})

;; Converted models with :app/entity and field aliases - what services expect
(def test-models-data
  (model-naming/convert-models raw-test-models))

(def test-config
  {:google-client-id "test-client-id"
   :google-client-secret "test-client-secret"
   :github-client-id "test-github-id"
   :github-client-secret "test-github-secret"})

;; Mock database connection for testing
(defrecord MockDatabaseConnection [state])
  ; Implement minimal database connection interface for testing

(defn create-mock-db-connection []
  (->MockDatabaseConnection (atom {})))

;; ============================================================================
;; Dependency Injection Container Tests
;; ============================================================================

(deftest test-create-database-adapter
  (testing "Database adapter creation"
    (let [mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)]

      (is (not (nil? db-adapter)))
      (is (satisfies? db-protocols/DatabaseAdapter db-adapter)))))

(deftest test-create-metadata-service
  (testing "Metadata service creation"
    (let [metadata-service (di-config/create-metadata-service test-models-data)]

      (is (not (nil? metadata-service)))
      (is (satisfies? crud-protocols/MetadataService metadata-service)))))

(deftest test-create-type-casting-service
  (testing "Type casting service creation"
    (let [type-casting-service (di-config/create-type-casting-service test-models-data)]

      (is (not (nil? type-casting-service)))
      (is (satisfies? crud-protocols/TypeCastingService type-casting-service)))))

(deftest test-create-validation-service
  (testing "Validation service creation"
    (let [mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          validation-service (di-config/create-validation-service test-models-data db-adapter)]

      (is (not (nil? validation-service)))
      (is (satisfies? crud-protocols/ValidationService validation-service)))))

(deftest test-create-query-builder
  (testing "Query builder creation"
    (let [query-builder (di-config/create-query-builder test-models-data)]

      (is (not (nil? query-builder)))
      (is (satisfies? crud-protocols/QueryBuilder query-builder)))))

(deftest test-create-crud-service
  (testing "CRUD service creation with all dependencies"
    (let [mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          crud-service (di-config/create-crud-service db-adapter test-models-data)]

      (is (not (nil? crud-service)))
      (is (satisfies? crud-protocols/CrudService crud-service)))))

(deftest test-create-authentication-service
  (testing "Authentication service creation"
    (let [mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          email-service (email-service/create-email-service {:type :postmark :postmark {:api-key "test-key" :from-email "test@example.com"}})
          auth-service (di-config/create-authentication-service test-config db-adapter test-models-data email-service)]

      (is (not (nil? auth-service)))
      (is (satisfies? core-protocols/BusinessService auth-service)))))

(deftest test-crud-routes-creation
  (testing "CRUD routes creation"
    (let [mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          crud-service (di-config/create-crud-service db-adapter test-models-data)
          crud-routes (di-config/create-crud-routes crud-service)]

      (is (not (nil? crud-routes)))
      (is (vector? crud-routes))
      (is (seq crud-routes)))))

  ;; ============================================================================
  ;; Service Integration Tests
  ;; ============================================================================

(deftest test-complete-service-container
  (testing "Complete service container integration"
    (let [mock-db (create-mock-db-connection)

          ;; Create all services step by step
          db-adapter (di-config/create-database-adapter mock-db)
          metadata-service (di-config/create-metadata-service test-models-data)
          type-casting-service (di-config/create-type-casting-service test-models-data)
          validation-service (di-config/create-validation-service test-models-data db-adapter)
          query-builder (di-config/create-query-builder test-models-data)
          crud-service (di-config/create-crud-service db-adapter test-models-data)
          email-service (email-service/create-email-service {:type :postmark :postmark {:api-key "test-key" :from-email "test@example.com"}})
          auth-service (di-config/create-authentication-service test-config db-adapter test-models-data email-service)
          crud-routes (di-config/create-crud-routes crud-service)]

      ;; Verify all services are created
      (is (not (nil? db-adapter)))
      (is (not (nil? metadata-service)))
      (is (not (nil? type-casting-service)))
      (is (not (nil? validation-service)))
      (is (not (nil? query-builder)))
      (is (not (nil? crud-service)))
      (is (not (nil? auth-service)))
      (is (not (nil? crud-routes)))

      ;; Verify services implement correct protocols
      (is (satisfies? db-protocols/DatabaseAdapter db-adapter))
      (is (satisfies? crud-protocols/MetadataService metadata-service))
      (is (satisfies? crud-protocols/TypeCastingService type-casting-service))
      (is (satisfies? crud-protocols/ValidationService validation-service))
      (is (satisfies? crud-protocols/QueryBuilder query-builder))
      (is (satisfies? crud-protocols/CrudService crud-service))
      (is (satisfies? core-protocols/BusinessService auth-service)))))

(deftest test-service-dependencies
  (testing "Service dependencies are correctly wired"
    (let [mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          metadata-service (di-config/create-metadata-service test-models-data)
          type-casting-service (di-config/create-type-casting-service test-models-data)
          validation-service (di-config/create-validation-service test-models-data db-adapter)
          query-builder (di-config/create-query-builder test-models-data)]
          ;;crud-service (di-config/create-crud-service db-adapter test-models-data)]

      ;; Test that CRUD service can use its dependencies
      ;; (This is mostly a smoke test to ensure wiring works)

      ;; Test metadata service integration
      (is (crud-protocols/validate-entity-exists metadata-service :properties))
      (is (not (crud-protocols/validate-entity-exists metadata-service :nonexistent)))

      ;; Test type casting service integration
      (let [cast-data (crud-protocols/cast-for-insert type-casting-service :properties
                        {:name "Test Property"
                         :address "123 Test St"})]
        (is (contains? cast-data :name))
        (is (contains? cast-data :address)))

      ;; Test validation service integration
      (let [validation-result (crud-protocols/validate-field validation-service :properties :name "Valid Name")]
        (is (:valid? validation-result)))

      ;; Test query builder integration
      (let [select-query (crud-protocols/build-select-query query-builder :properties {})]
        (is (= [:*] (:select select-query)))
        (is (= [:properties] (:from select-query)))))))

(deftest test-configuration-handling
  (testing "Configuration handling in service creation"
    ;; Test with minimal configuration
    (let [minimal-config {:google-client-id "test"
                          :google-client-secret "test"}
          mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          email-service (email-service/create-email-service {:type :postmark :postmark {:api-key "test-key" :from-email "test@example.com"}})
          auth-service (di-config/create-authentication-service minimal-config db-adapter test-models-data email-service)]

      (is (not (nil? auth-service))))

    ;; Test with full configuration
    (let [full-config {:google-client-id "google-test"
                       :google-client-secret "google-secret"
                       :github-client-id "github-test"
                       :github-client-secret "github-secret"}
          mock-db (create-mock-db-connection)
          db-adapter (di-config/create-database-adapter mock-db)
          email-service (email-service/create-email-service {:type :postmark :postmark {:api-key "test-key" :from-email "test@example.com"}})
          auth-service (di-config/create-authentication-service full-config db-adapter test-models-data email-service)]

      (is (not (nil? auth-service))))))

(deftest test-error-handling-in-service-creation
  (testing "Error handling in service creation"
    ;; Test creating services with nil dependencies
    (try
      (di-config/create-validation-service test-models-data nil)
      (is true "Should handle nil db-adapter gracefully")
      (catch Exception e
        (is false (str "Should not throw exception: " (.getMessage e)))))

    ;; Test creating services with empty models data
    (try
      (let [empty-models {}
            metadata-service (di-config/create-metadata-service empty-models)]
        (is (not (nil? metadata-service))))
      (catch Exception e
        (is false (str "Should handle empty models gracefully: " (.getMessage e)))))))

  ;; ============================================================================
  ;; Test Runner
  ;; ============================================================================

(defn run-container-tests []
  (log/info "Running dependency injection container tests...")
  (run-tests 'app.template.di.container-test))

(comment
  (run-container-tests))
