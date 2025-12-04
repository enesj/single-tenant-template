(ns app.template.backend.crud.service-test
  "Tests for the Template CRUD service protocols validation.
   
   These tests verify:
   - Protocol method existence
   - Basic contract validation
   - Mock implementations work correctly"
  (:require
    [app.template.backend.crud.protocols :as crud-protocols]
    [clojure.test :refer [deftest is testing]]))

;; ============================================================================
;; Protocol Tests - Verify protocols are properly defined
;; ============================================================================

(deftest crud-service-protocol-test
  (testing "CrudService protocol exists with expected methods"
    (is (some? crud-protocols/CrudService)
      "CrudService protocol should be defined")
    ;; Check protocol functions exist
    (is (fn? crud-protocols/get-items) "get-items should be a function")
    (is (fn? crud-protocols/get-item) "get-item should be a function")
    (is (fn? crud-protocols/create-item) "create-item should be a function")
    (is (fn? crud-protocols/update-item) "update-item should be a function")
    (is (fn? crud-protocols/delete-item) "delete-item should be a function")
    (is (fn? crud-protocols/batch-update-items) "batch-update-items should be a function")
    (is (fn? crud-protocols/batch-delete-items) "batch-delete-items should be a function")))

(deftest metadata-service-protocol-test
  (testing "MetadataService protocol exists with expected methods"
    (is (some? crud-protocols/MetadataService)
      "MetadataService protocol should be defined")
    (is (fn? crud-protocols/get-entity-metadata) "get-entity-metadata should be a function")
    (is (fn? crud-protocols/validate-entity-exists) "validate-entity-exists should be a function")
    (is (fn? crud-protocols/get-foreign-keys) "get-foreign-keys should be a function")))

(deftest validation-service-protocol-test
  (testing "ValidationService protocol exists with expected methods"
    (is (some? crud-protocols/ValidationService)
      "ValidationService protocol should be defined")
    (is (fn? crud-protocols/validate-entity) "validate-entity should be a function")
    (is (fn? crud-protocols/validate-field) "validate-field should be a function")
    (is (fn? crud-protocols/validate-required-fields) "validate-required-fields should be a function")))

(deftest type-casting-service-protocol-test
  (testing "TypeCastingService protocol exists with expected methods"
    (is (some? crud-protocols/TypeCastingService)
      "TypeCastingService protocol should be defined")
    (is (fn? crud-protocols/cast-for-insert) "cast-for-insert should be a function")
    (is (fn? crud-protocols/cast-for-update) "cast-for-update should be a function")
    (is (fn? crud-protocols/cast-field-value) "cast-field-value should be a function")))

;; ============================================================================
;; Mock Implementation Tests
;; ============================================================================

(def test-entity-metadata
  {:entity :users
   :db/entity :users
   :fields [{:name :id :type :uuid :primary-key true}
            {:name :email :type :string}
            {:name :full-name :type :string}]})

(defn create-mock-metadata-service
  "Create a mock MetadataService that returns test data for :users"
  []
  (reify crud-protocols/MetadataService
    (get-entity-metadata [_ entity-key]
      (when (= entity-key :users)
        test-entity-metadata))
    (get-field-metadata [_ entity-key field-name]
      (when (= entity-key :users)
        (first (filter #(= (:name %) field-name) (:fields test-entity-metadata)))))
    (get-foreign-keys [_ _entity-key]
      [])
    (validate-entity-exists [_ entity-key]
      (= entity-key :users))
    (get-entity-field-specs [_ entity-key _opts]
      (when (= entity-key :users)
        (:fields test-entity-metadata)))))

(deftest mock-metadata-service-test
  (testing "Mock MetadataService implements protocol correctly"
    (let [mock-svc (create-mock-metadata-service)]
      (testing "get-entity-metadata returns data for known entity"
        (let [result (crud-protocols/get-entity-metadata mock-svc :users)]
          (is (map? result))
          (is (= :users (:entity result)))))

      (testing "get-entity-metadata returns nil for unknown entity"
        (let [result (crud-protocols/get-entity-metadata mock-svc :unknown)]
          (is (nil? result))))

      (testing "validate-entity-exists returns true for known entity"
        (is (true? (crud-protocols/validate-entity-exists mock-svc :users))))

      (testing "validate-entity-exists returns false for unknown entity"
        (is (false? (crud-protocols/validate-entity-exists mock-svc :unknown))))

      (testing "get-foreign-keys returns empty vector"
        (is (= [] (crud-protocols/get-foreign-keys mock-svc :users))))

      (testing "get-field-metadata returns field data"
        (let [result (crud-protocols/get-field-metadata mock-svc :users :email)]
          (is (= :email (:name result)))
          (is (= :string (:type result))))))))

(defn create-mock-validation-service
  "Create a mock ValidationService that always validates successfully"
  []
  (reify crud-protocols/ValidationService
    (validate-field [_ _entity-key _field-name _value]
      {:valid? true})
    (validate-entity [_ _entity-key _data]
      {:valid? true})
    (validate-required-fields [_ _entity-key _data]
      {:valid? true})
    (validate-foreign-keys [_ _tenant-id _entity-key _data]
      {:valid? true})))

(deftest mock-validation-service-test
  (testing "Mock ValidationService implements protocol correctly"
    (let [mock-svc (create-mock-validation-service)]
      (testing "validate-field returns valid"
        (let [result (crud-protocols/validate-field mock-svc :users :email "test@example.com")]
          (is (:valid? result))))

      (testing "validate-entity returns valid"
        (let [result (crud-protocols/validate-entity mock-svc :users {:email "test@example.com"})]
          (is (:valid? result))))

      (testing "validate-required-fields returns valid"
        (let [result (crud-protocols/validate-required-fields mock-svc :users {:email "test@example.com"})]
          (is (:valid? result))))

      (testing "validate-foreign-keys returns valid"
        (let [tenant-id (java.util.UUID/randomUUID)
              result (crud-protocols/validate-foreign-keys mock-svc tenant-id :users {:tenant-id tenant-id})]
          (is (:valid? result)))))))

(defn create-mock-type-casting-service
  "Create a mock TypeCastingService that returns data unchanged"
  []
  (reify crud-protocols/TypeCastingService
    (cast-for-insert [_ _entity-key data]
      data)
    (cast-for-update [_ _entity-key data]
      data)
    (cast-field-value [_ _entity-key _field-name value]
      value)))

(deftest mock-type-casting-service-test
  (testing "Mock TypeCastingService implements protocol correctly"
    (let [mock-svc (create-mock-type-casting-service)]
      (testing "cast-for-insert returns data unchanged"
        (let [data {:email "test@example.com"}
              result (crud-protocols/cast-for-insert mock-svc :users data)]
          (is (= data result))))

      (testing "cast-for-update returns data unchanged"
        (let [data {:email "updated@example.com"}
              result (crud-protocols/cast-for-update mock-svc :users data)]
          (is (= data result))))

      (testing "cast-field-value returns value unchanged"
        (let [value "test@example.com"
              result (crud-protocols/cast-field-value mock-svc :users :email value)]
          (is (= value result)))))))

;; ============================================================================
;; Protocol Extension Tests
;; ============================================================================

(deftest satisfies-protocol-test
  (testing "Mock implementations satisfy their protocols"
    (is (satisfies? crud-protocols/MetadataService (create-mock-metadata-service))
      "Mock should satisfy MetadataService protocol")
    (is (satisfies? crud-protocols/ValidationService (create-mock-validation-service))
      "Mock should satisfy ValidationService protocol")
    (is (satisfies? crud-protocols/TypeCastingService (create-mock-type-casting-service))
      "Mock should satisfy TypeCastingService protocol")))
