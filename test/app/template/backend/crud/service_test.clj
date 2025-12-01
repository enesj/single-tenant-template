(ns app.template.backend.crud.service-test
  {:clj-kondo/config '{:linters {:redundant-let {:level :off}}}}
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.crud.service :as crud-service]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.metadata.service :as metadata-service]
    [clojure.test :refer [deftest is run-tests testing]]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Test Configuration and Setup
;; ============================================================================

(def test-models-data
  {:properties
   {:fields [[:id :uuid {:null false :primary-key true}]
             [:tenant_id :uuid {:null false}]
             [:name :varchar {:null false :max-length 255}]
             [:address :text {:null true}]
             [:property_type :varchar {:null true :max-length 50}]
             [:settings :jsonb {:null true}]
             [:created_at :timestamptz {:null false}]
             [:updated_at :timestamptz {:null false}]]
    :constraints {:unique [[:tenant_id :name]]}}

   :users
   {:fields [[:id :uuid {:null false :primary-key true}]
             [:tenant_id :uuid {:null false}]
             [:email :varchar {:null false :max-length 255}]
             [:full_name :varchar {:null false :max-length 255}]
             [:role :varchar {:null false :max-length 50}]
             [:status :varchar {:null false :max-length 50}]
             [:created_at :timestamptz {:null false}]
             [:updated_at :timestamptz {:null false}]]
    :constraints {:unique [[:tenant_id :email]]}}})

(def test-tenant-id #uuid "123e4567-e89b-12d3-a456-426614174000")
(def test-property-id #uuid "123e4567-e89b-12d3-a456-426614174002")

 ;; ============================================================================
;; Mock Database Helpers
;; ============================================================================

(defn resolve-cast-expressions
  "Resolve HoneySQL cast expressions for mock database testing.

   This function converts HoneySQL cast expressions like [:cast value :uuid]
   back to their actual values for use in mock database testing."
  [value]
  (cond
    ;; Handle HoneySQL cast expressions
    (and (vector? value) (= (first value) :cast))
    (let [[_ val type] value]
      (case type
        :uuid (if (string? val) (java.util.UUID/fromString val) val)
        :timestamptz (if (instance? java.time.LocalDateTime val) val val)
        :integer (if (string? val) (Integer/parseInt val) val)
        :decimal (if (string? val) (Double/parseDouble val) val)
        :boolean (if (string? val) (Boolean/parseBoolean val) val)
        ;; For other types, just return the value
        val))

    ;; Handle nested maps recursively
    (map? value)
    (reduce-kv (fn [acc k v]
                 (assoc acc k (resolve-cast-expressions v)))
      {}
      value)

    ;; Handle vectors recursively
    (vector? value)
    (mapv resolve-cast-expressions value)

    ;; Default case - return as-is
    :else value))

(defn resolve-data-for-mock
  "Resolve all HoneySQL cast expressions in a data map for mock database storage."
  [data]
  (reduce-kv (fn [acc k v]
               (assoc acc k (resolve-cast-expressions v)))
    {}
    data))

;; Mock database adapter for testing
(defrecord MockDatabaseAdapter [state]
  db-protocols/DatabaseAdapter
  (find-by-id [this table id]
    (get-in @state [table id]))

  (find-by-field [this table field value]
    (->> (vals (get @state table {}))
      (filter #(= (get % field) value))
      first))

  (find-all [this table]
    (vals (get @state table {})))

  (list-with-filters [this table filters]
    (->> (vals (get @state table {}))
      (filter (fn [record]
                (every? (fn [[field value]]
                          (= (get record field) value))
                  filters)))))

  (create [this _metadata table data]
    (let [resolved-data (resolve-data-for-mock data)
          id (or (:id resolved-data) (java.util.UUID/randomUUID))
          record (assoc resolved-data :id id)]
      (swap! state assoc-in [table id] record)
      record))

  (update-record [this _metadata table id data]
    (let [existing (get-in @state [table id])]
      (when existing
        (let [resolved-data (resolve-data-for-mock data)
              updated (merge existing resolved-data)]
          (swap! state assoc-in [table id] updated)
          updated))))

  (delete [this table id]
    (let [existed? (contains? (get @state table {}) id)]
      (swap! state update table dissoc id)
      {:success? existed?}))

  (delete [this table id _opts]
    (db-protocols/delete this table id))

  (exists? [this table field value]
    (some #(= (get % field) value)
      (vals (get @state table {}))))
  (execute! [_this _sql _params]
    []))

(defn create-test-service-container []
  (let [db-state (atom {})
        db-adapter (->MockDatabaseAdapter db-state)
        app-models (model-naming/convert-models test-models-data)
        metadata-service (metadata-service/create-metadata-service app-models)
        type-casting-service (metadata-service/create-type-casting-service app-models)
        validation-service (metadata-service/create-validation-service app-models db-adapter)
        query-builder (metadata-service/create-query-builder app-models)]

    {:db-adapter db-adapter
     :metadata-service metadata-service
     :type-casting-service type-casting-service
     :validation-service validation-service
     :query-builder query-builder
     :crud-service (crud-service/create-crud-service
                     db-adapter
                     metadata-service
                     validation-service
                     type-casting-service
                     query-builder)}))

;; ============================================================================
;; CRUD Service Tests
;; ============================================================================

(deftest test-crud-service-creation
  (testing "CRUD service creation with all dependencies"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      (is (not (nil? crud-service)))
      (is (satisfies? crud-protocols/CrudService crud-service)))))

(deftest test-get-items-operation
  (testing "Get items operation"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          db-adapter (:db-adapter container)]

      ;; Create test data
      (db-protocols/create db-adapter nil :properties
        {:id test-property-id
         :tenant_id test-tenant-id
         :name "Test Property"
         :address "123 Test St"
         :property_type "apartment"})

      ;; Test get items
      (let [items (crud-protocols/get-items crud-service test-tenant-id :properties {})]
        (is (= 1 (count items)))
        (is (= "Test Property" (:name (first items))))
        (is (= test-tenant-id (:tenant-id (first items))))))))

(deftest test-get-item-operation
  (testing "Get single item operation"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          db-adapter (:db-adapter container)]

      ;; Create test data
      (db-protocols/create db-adapter nil :properties
        {:id test-property-id
         :tenant_id test-tenant-id
         :name "Test Property"
         :address "123 Test St"})

      ;; Test get item
      (let [item (crud-protocols/get-item crud-service test-tenant-id :properties test-property-id {})]
        (is (not (nil? item)))
        (is (= "Test Property" (:name item)))
        (is (= test-tenant-id (:tenant-id item))))

      ;; Test get non-existent item
      (let [missing-item (crud-protocols/get-item crud-service test-tenant-id :properties
                           #uuid "999e4567-e89b-12d3-a456-426614174999" {})]
        (is (nil? missing-item))))))

(deftest test-create-item-operation
  (testing "Create item operation"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; Test create item
      (let [new-property-data {:name "New Property"
                               :address "456 New St"
                               :property-type "house"}
            created-item (crud-protocols/create-item crud-service test-tenant-id :properties new-property-data {})]

        (is (not (nil? created-item)))
        (is (not (nil? (:id created-item))))
        (is (= "New Property" (:name created-item)))
        (is (= test-tenant-id (:tenant-id created-item)))
        (is (= "456 New St" (:address created-item)))))))

(deftest test-update-item-operation
  (testing "Update item operation"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          db-adapter (:db-adapter container)]

      ;; Create test data
      (db-protocols/create db-adapter nil :properties
        {:id test-property-id
         :tenant_id test-tenant-id
         :name "Original Property"
         :address "123 Original St"})

      ;; Test update item
      (let [update-data {:name "Updated Property"
                         :address "123 Updated St"}
            updated-item (crud-protocols/update-item crud-service test-tenant-id :properties
                           test-property-id update-data {})]

        (is (not (nil? updated-item)))
        (is (= "Updated Property" (:name updated-item)))
        (is (= "123 Updated St" (:address updated-item)))
        (is (= test-tenant-id (:tenant-id updated-item)))))))

(deftest test-delete-item-operation
  (testing "Delete item operation"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          db-adapter (:db-adapter container)]

      ;; Create test data
      (db-protocols/create db-adapter nil :properties
        {:id test-property-id
         :tenant_id test-tenant-id
         :name "To Be Deleted"})

      ;; Verify item exists
      (let [existing-item (crud-protocols/get-item crud-service test-tenant-id :properties test-property-id {})]
        (is (not (nil? existing-item))))

      ;; Test delete item
      (let [delete-result (crud-protocols/delete-item crud-service test-tenant-id :properties test-property-id {})]
        (is (:success? delete-result)))

      ;; Verify item is deleted
      (let [deleted-item (crud-protocols/get-item crud-service test-tenant-id :properties test-property-id {})]
        (is (nil? deleted-item))))))

(deftest test-batch-operations
  (testing "Batch operations"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          db-adapter (:db-adapter container)]

      ;; Create test data
      (let [property1-id (java.util.UUID/randomUUID)
            property2-id (java.util.UUID/randomUUID)]

        (db-protocols/create db-adapter nil :properties
          {:id property1-id
           :tenant_id test-tenant-id
           :name "Property 1"})

        (db-protocols/create db-adapter nil :properties
          {:id property2-id
           :tenant_id test-tenant-id
           :name "Property 2"})

        ;; Test batch delete
        (let [batch-delete-result (crud-protocols/batch-delete-items crud-service test-tenant-id :properties
                                    [property1-id property2-id] {})]
          (is (= 2 (:deleted batch-delete-result)))
          (is (= [property1-id property2-id] (:deleted-ids batch-delete-result))))))))

(deftest test-tenant-isolation
  (testing "Tenant isolation in CRUD operations"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          db-adapter (:db-adapter container)
          tenant1-id test-tenant-id
          tenant2-id #uuid "223e4567-e89b-12d3-a456-426614174000"]

      ;; Create data for two different tenants
      (db-protocols/create db-adapter nil :properties
        {:id test-property-id
         :tenant_id tenant1-id
         :name "Tenant 1 Property"})

      (let [tenant2-property-id (java.util.UUID/randomUUID)]
        (db-protocols/create db-adapter nil :properties
          {:id tenant2-property-id
           :tenant_id tenant2-id
           :name "Tenant 2 Property"})

        ;; Test that tenant 1 can only see their own data
        (let [tenant1-items (crud-protocols/get-items crud-service tenant1-id :properties {})]
          (is (= 1 (count tenant1-items)))
          (is (= "Tenant 1 Property" (:name (first tenant1-items)))))

        ;; Test that tenant 2 can only see their own data
        (let [tenant2-items (crud-protocols/get-items crud-service tenant2-id :properties {})]
          (is (= 1 (count tenant2-items)))
          (is (= "Tenant 2 Property" (:name (first tenant2-items)))))

        ;; Test that tenant 1 cannot access tenant 2's data
        (let [cross-tenant-item (crud-protocols/get-item crud-service tenant1-id :properties tenant2-property-id {})]
          (is (nil? cross-tenant-item)))))))

(deftest test-validation-integration
  (testing "Validation integration in CRUD operations"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; Test validation failure on create
      (try
        (crud-protocols/create-item crud-service test-tenant-id :properties
          {:name ""                                         ; Invalid: empty name
           :address "123 Test St"} {})
        (is false "Should have thrown validation error")
        (catch Exception e
          (is (= :validation-error (:type (ex-data e))))))

      ;; Test successful validation
      (let [valid-property (crud-protocols/create-item crud-service test-tenant-id :properties
                             {:name "Valid Property"
                              :address "123 Valid St"} {})]
        (is (not (nil? valid-property)))
        (is (= "Valid Property" (:name valid-property)))))))

(deftest test-error-handling
  (testing "Error handling in CRUD operations"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; Test entity not found error
      (try
        (crud-protocols/get-items crud-service test-tenant-id :nonexistent-entity {})
        (is false "Should have thrown entity not found error")
        (catch Exception e
          (is (= :entity-not-found (:type (ex-data e))))))

      ;; Test item not found for update
      (try
        (crud-protocols/update-item crud-service test-tenant-id :properties
          #uuid "999e4567-e89b-12d3-a456-426614174999"
          {:name "Updated"} {})
        (is false "Should have thrown item not found error")
        (catch Exception e
          (is (= :item-not-found (:type (ex-data e)))))))))

;; ============================================================================
;; Test Runner
;; ============================================================================

(defn run-crud-service-tests []
  (log/info "Running CRUD service tests...")
  (run-tests 'app.template.backend.crud.service-test))

(comment
  (run-crud-service-tests))
