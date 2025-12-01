(ns app.template.backend.metadata.service-test
  {:clj-kondo/config '{:linters {:redundant-let {:level :off}}}}
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.metadata.service :as metadata-service]
    [clojure.test :refer [deftest is run-tests testing]]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Test Configuration and Setup
;; ============================================================================

(def test-models-data
  {:properties   {:fields      [[:id :uuid {:null        false
                                            :primary-key true}]
                                [:tenant_id :uuid {:null false}]
                                [:name :varchar {:null       false
                                                 :max-length 255}]
                                [:address :text {:null true}]
                                [:property_type :varchar {:null       true
                                                          :max-length 50}]
                                [:owner_id :uuid {:null        false
                                                  :foreign-key "users/id"}]
                                [:settings :jsonb {:null true}]
                                [:created_at :timestamptz {:null false}]
                                [:updated_at :timestamptz {:null false}]]
                  :constraints {:unique [[:tenant_id :name]]}}

   :users        {:fields      [[:id :uuid {:null        false
                                            :primary-key true}]
                                [:tenant_id :uuid {:null false}]
                                [:email :varchar {:null       false
                                                  :max-length 255}]
                                [:full_name :varchar {:null       false
                                                      :max-length 255}]
                                [:role :varchar {:null       false
                                                 :max-length 50}]
                                [:status :varchar {:null       false
                                                   :max-length 50}]
                                [:created_at :timestamptz {:null false}]
                                [:updated_at :timestamptz {:null false}]]
                  :constraints {:unique [[:tenant_id :email]]}}

   :transactions {:fields      [[:id :uuid {:null        false
                                            :primary-key true}]
                                [:tenant_id :uuid {:null false}]
                                [:property_id :uuid {:null        false
                                                     :foreign-key "properties/id"}]
                                [:amount :decimal {:null      false
                                                   :precision 10
                                                   :scale     2}]
                                [:description :text {:null true}]
                                [:transaction_date :date {:null false}]
                                [:created_at :timestamptz {:null false}]
                                [:updated_at :timestamptz {:null false}]]
                  :constraints {}}})

(def test-app-models (model-naming/convert-models test-models-data))

;; ============================================================================
;; Metadata Service Tests
;; ============================================================================

(deftest test-metadata-service-creation
  (testing "Metadata service creation"
    (let [metadata-service (metadata-service/create-metadata-service test-app-models)]
      (is (not (nil? metadata-service)))
      (is (satisfies? crud-protocols/MetadataService metadata-service)))))

(deftest test-get-entity-metadata
  (testing "Get entity metadata"
    (let [metadata-service (metadata-service/create-metadata-service test-app-models)]

      ;; Test valid entity
      (let [properties-metadata (crud-protocols/get-entity-metadata metadata-service :properties)]
        (is (not (nil? properties-metadata)))
        (is (= :properties (:entity properties-metadata)))
        (is (= :properties (:db/entity properties-metadata)))
        (is (seq (:fields properties-metadata)))
        (is (= [[:tenant-id :name]] (get-in properties-metadata [:constraints :unique]))))

      ;; Test invalid entity
      (let [invalid-metadata (crud-protocols/get-entity-metadata metadata-service :nonexistent)]
        (is (nil? invalid-metadata))))))

(deftest test-get-field-metadata
  (testing "Get field metadata"
    (let [metadata-service (metadata-service/create-metadata-service test-app-models)]

      ;; Test valid field
      (let [name-field-metadata (crud-protocols/get-field-metadata metadata-service :properties :name)]
        (is (not (nil? name-field-metadata)))
        (is (= :name (:field-name name-field-metadata)))
        (is (= :name (:db/field-name name-field-metadata)))
        (is (= :varchar (:field-type name-field-metadata)))
        (is (= false (get-in name-field-metadata [:constraints :null])))
        (is (= 255 (get-in name-field-metadata [:constraints :max-length]))))

      ;; Test invalid field
      (let [invalid-field-metadata (crud-protocols/get-field-metadata metadata-service :properties :nonexistent)]
        (is (nil? invalid-field-metadata)))

      ;; Test invalid entity
      (let [invalid-entity-field (crud-protocols/get-field-metadata metadata-service :nonexistent :name)]
        (is (nil? invalid-entity-field))))))

(deftest test-get-foreign-keys
  (testing "Get foreign keys"
    (let [metadata-service (metadata-service/create-metadata-service test-app-models)]

      ;; Test entity with foreign keys
      (let [properties-fks (crud-protocols/get-foreign-keys metadata-service :properties)
            owner-fk (first properties-fks)]
        (is (= 1 (count properties-fks)))
        (is (= :owner-id (:field owner-fk)))
        (is (= :owner_id (:db/field owner-fk)))
        (is (= :users (:foreign-table owner-fk)))
        (is (= :users (:db/foreign-table owner-fk)))
        (is (= :id (:foreign-field owner-fk)))
        (is (= :id (:db/foreign-field owner-fk))))

      ;; Test entity with multiple foreign keys
      (let [transactions-fks (crud-protocols/get-foreign-keys metadata-service :transactions)
            property-fk (first transactions-fks)]
        (is (= 1 (count transactions-fks)))
        (is (= :property-id (:field property-fk)))
        (is (= :property_id (:db/field property-fk)))
        (is (= :properties (:foreign-table property-fk)))
        (is (= :properties (:db/foreign-table property-fk)))
        (is (= :id (:foreign-field property-fk)))
        (is (= :id (:db/foreign-field property-fk))))

      ;; Test entity with no foreign keys
      (let [users-fks (crud-protocols/get-foreign-keys metadata-service :users)]
        (is (empty? users-fks))))))

(deftest test-validate-entity-exists
  (testing "Validate entity exists"
    (let [metadata-service (metadata-service/create-metadata-service test-app-models)]

      ;; Test existing entities
      (is (true? (crud-protocols/validate-entity-exists metadata-service :properties)))
      (is (true? (crud-protocols/validate-entity-exists metadata-service :users)))
      (is (true? (crud-protocols/validate-entity-exists metadata-service :transactions)))

      ;; Test non-existing entity
      (is (false? (crud-protocols/validate-entity-exists metadata-service :nonexistent))))))

;; ============================================================================
;; Type Casting Service Tests
;; ============================================================================

(deftest test-type-casting-service-creation
  (testing "Type casting service creation"
    (let [type-casting-service (metadata-service/create-type-casting-service test-app-models)]
      (is (not (nil? type-casting-service)))
      (is (satisfies? crud-protocols/TypeCastingService type-casting-service)))))

(deftest test-cast-for-insert
  (testing "Cast data for insert"
    (let [type-casting-service (metadata-service/create-type-casting-service test-app-models)
          input-data           {:name         "Test Property"
                                :address      "123 Test St"
                                :property-type "apartment"
                                :owner-id     "123e4567-e89b-12d3-a456-426614174000"}
          cast-data            (crud-protocols/cast-for-insert type-casting-service :properties input-data)]

      (let [name-value (:name cast-data)
            property-value (:property-type cast-data)]
        (is (= :cast (first name-value)))
        (is (= "Test Property" (second name-value)))
        (is (= "123 Test St" (:address cast-data)))
        (is (= :cast (first property-value)))
        (is (= "apartment" (second property-value))))
      ;; UUID should be cast properly (implementation depends on type-conversion)
      (is (contains? cast-data :owner-id)))))

(deftest test-cast-for-update
  (testing "Cast data for update"
    (let [type-casting-service (metadata-service/create-type-casting-service test-app-models)
          input-data           {:name      "Updated Property"
                                :address   "456 Updated St"
                                :tenant-id "should-be-removed"}       ; Should be removed as immutable
          cast-data            (crud-protocols/cast-for-update type-casting-service :properties input-data)]

      (let [name-value (:name cast-data)]
        (is (= :cast (first name-value)))
        (is (= "Updated Property" (second name-value))))
      (is (= "456 Updated St" (:address cast-data)))
      (is (not (contains? cast-data :tenant-id)))           ; Should be removed
      (is (contains? cast-data :updated-at)))))             ; Should be added

(deftest test-cast-field-value
  (testing "Cast individual field value"
    (let [type-casting-service (metadata-service/create-type-casting-service test-app-models)
          cast-name            (crud-protocols/cast-field-value type-casting-service :properties :name "Test Property")
          uuid-string          "123e4567-e89b-12d3-a456-426614174000"
          cast-uuid            (crud-protocols/cast-field-value type-casting-service :properties :owner-id uuid-string)]

      ;; Test string field
      (is (= :cast (first cast-name)))
      (is (= "Test Property" (second cast-name)))

      ;; Test UUID field
      (is (= :cast (first cast-uuid)))
      (is (not (nil? (second cast-uuid)))))))

;; ============================================================================
;; Validation Service Tests
;; ============================================================================

(deftest test-validation-service-creation
  (testing "Validation service creation"
    (let [validation-service (metadata-service/create-validation-service test-app-models nil)]
      (is (not (nil? validation-service)))
      (is (satisfies? crud-protocols/ValidationService validation-service)))))

(deftest test-validate-field
  (testing "Validate individual field"
    (let [validation-service (metadata-service/create-validation-service test-app-models nil)]

      ;; Test valid string field
      (let [result (crud-protocols/validate-field validation-service :properties :name "Valid Property")]
        (is (:valid? result))
        (is (nil? (:message result))))

      ;; Test invalid required field (empty string)
      (let [result (crud-protocols/validate-field validation-service :properties :name "")]
        (is (not (:valid? result)))
        (is (not (nil? (:message result)))))

      ;; Test invalid required field (nil)
      (let [result (crud-protocols/validate-field validation-service :properties :name nil)]
        (is (not (:valid? result)))
        (is (not (nil? (:message result)))))

      ;; Test optional field (can be nil)
      (let [result (crud-protocols/validate-field validation-service :properties :address nil)]
        (is (:valid? result))))))

(deftest test-validate-entity
  (testing "Validate entire entity"
    (let [validation-service (metadata-service/create-validation-service test-app-models nil)]

      ;; Test valid entity data
      (let [valid-data {:name          "Valid Property"
                        :address       "123 Valid St"
                        :property-type "apartment"}
            result     (crud-protocols/validate-entity validation-service :properties valid-data)]
        (is (:valid? result))
        (is (empty? (:errors result))))

      ;; Test invalid entity data
      (let [invalid-data {:name    ""                          ; Required field is empty
                          :address "123 Test St"}
            result       (crud-protocols/validate-entity validation-service :properties invalid-data)]
        (is (not (:valid? result)))
        (is (seq (:errors result)))))))

(deftest test-validate-required-fields
  (testing "Validate required fields"
    (let [validation-service (metadata-service/create-validation-service test-app-models nil)]

      ;; Test with all required fields present
      (let [complete-data {:name      "Test Property"
                           :tenant-id #uuid "123e4567-e89b-12d3-a456-426614174000"
                           :owner-id  #uuid "123e4567-e89b-12d3-a456-426614174001"}
            result        (crud-protocols/validate-required-fields validation-service :properties complete-data)]
        (is (:valid? result))
        (is (empty? (:missing-fields result))))

      ;; Test with missing required fields
      (let [incomplete-data {:name "Test Property"}
            result          (crud-protocols/validate-required-fields validation-service :properties incomplete-data)]
        (is (not (:valid? result)))
        (is (seq (:missing-fields result)))
        (is (every? #{:tenant-id :owner-id} (:missing-fields result)))))))

;; ============================================================================
;; Query Builder Service Tests
;; ============================================================================

(deftest test-query-builder-service-creation
  (testing "Query builder service creation"
    (let [query-builder (metadata-service/create-query-builder test-app-models)]
      (is (not (nil? query-builder)))
      (is (satisfies? crud-protocols/QueryBuilder query-builder)))))

(deftest test-build-select-query
  (testing "Build SELECT query"
    (let [query-builder (metadata-service/create-query-builder test-app-models)]

      ;; Test basic select query
      (let [basic-query (crud-protocols/build-select-query query-builder :properties {})]
        (is (= [:*] (:select basic-query)))
        (is (= [:properties] (:from basic-query))))

      ;; Test select query with filters
      (let [filtered-query (crud-protocols/build-select-query query-builder :properties
                             {:filters {:tenant-id     #uuid "123e4567-e89b-12d3-a456-426614174000"
                                        :property-type "apartment"}})]
        (is (= [:*] (:select filtered-query)))
        (is (= [:properties] (:from filtered-query)))
        (is (= [:and [:= :tenant_id #uuid "123e4567-e89b-12d3-a456-426614174000"]
                [:= :property_type "apartment"]] (:where filtered-query))))

      ;; Test select query with ordering
      (let [ordered-query (crud-protocols/build-select-query query-builder :properties
                            {:order-by :name})]
        (is (= [:name] (:order-by ordered-query))))

      ;; Test select query with pagination
      (let [paginated-query (crud-protocols/build-select-query query-builder :properties
                              {:limit  10
                               :offset 20})]
        (is (= 10 (:limit paginated-query)))
        (is (= 20 (:offset paginated-query)))))))

(deftest test-build-insert-query
  (testing "Build INSERT query"
    (let [query-builder (metadata-service/create-query-builder test-app-models)]

      (let [insert-data  {:name      "New Property"
                          :address   "123 New St"
                          :tenant-id #uuid "123e4567-e89b-12d3-a456-426614174000"}
            insert-query (crud-protocols/build-insert-query query-builder :properties insert-data)]

        (is (= [:properties] (:insert-into insert-query)))
        (is (= [{:name "New Property"
                 :address "123 New St"
                 :tenant_id #uuid "123e4567-e89b-12d3-a456-426614174000"}]
              (:values insert-query)))
        (is (= [:*] (:returning insert-query)))))))

(deftest test-build-update-query
  (testing "Build UPDATE query"
    (let [query-builder (metadata-service/create-query-builder test-app-models)]

      (let [update-data  {:name    "Updated Property"
                          :address "123 Updated St"}
            item-id      #uuid "123e4567-e89b-12d3-a456-426614174002"
            update-query (crud-protocols/build-update-query query-builder :properties item-id update-data)]

        (is (= [:properties] (:update update-query)))
        (is (= {:name "Updated Property"
                :address "123 Updated St"}
              (:set update-query)))
        (is (= [:= :id [:cast item-id :uuid]] (:where update-query)))
        (is (= [:*] (:returning update-query)))))))

(deftest test-build-delete-query
  (testing "Build DELETE query"
    (let [query-builder (metadata-service/create-query-builder test-app-models)]

      (let [item-id      #uuid "123e4567-e89b-12d3-a456-426614174002"
            delete-query (crud-protocols/build-delete-query query-builder :properties item-id)]

        (is (= [:properties] (:delete-from delete-query)))
        (is (= [:= :id [:cast item-id :uuid]] (:where delete-query)))
        (is (= [:id] (:returning delete-query)))))))

;; ============================================================================
;; Test Runner
;; ============================================================================

(defn run-metadata-service-tests []
  (log/info "Running metadata service tests...")
  (run-tests 'app.template.backend.metadata.service-test))

(comment
  (run-metadata-service-tests))
