(ns app.template.backend.crud.integration-test
  {:clj-kondo/config '{:linters {:redundant-let {:level :off}}}}
  (:require
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.crud.service :as crud-service]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.metadata.service :as metadata-service]
    [app.template.backend.routes.crud :as crud-routes]
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
             [:owner_id :uuid {:null false :foreign-key "users/id"}]
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
    :constraints {:unique [[:tenant_id :email]]}}

   :transactions
   {:fields [[:id :uuid {:null false :primary-key true}]
             [:tenant_id :uuid {:null false}]
             [:property_id :uuid {:null false :foreign-key "properties/id"}]
             [:amount :decimal {:null false :precision 10 :scale 2}]
             [:description :text {:null true}]
             [:transaction_date :date {:null false}]
             [:created_at :timestamptz {:null false}]
             [:updated_at :timestamptz {:null false}]]
    :constraints {}}})

(def test-tenant-id #uuid "123e4567-e89b-12d3-a456-426614174000")

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
        :decimal (if (string? val) (bigdec val) (bigdec val))
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

  (list-with-filters [_this table filters]
    (->> (vals (get @state table {}))
      (filter (fn [record]
                (every? (fn [[field value]]
                          (= (get record field) value))
                  filters)))))

  (create [_this _metadata table data]
    (let [resolved-data (resolve-data-for-mock data)
          id (or (:id resolved-data) (java.util.UUID/randomUUID))
          record (assoc resolved-data :id id)]
      (swap! state assoc-in [table id] record)
      record))

  (update-record [_this _metadata table id data]
    (let [existing (get-in @state [table id])]
      (when existing
        (let [resolved-data (resolve-data-for-mock data)
              updated (merge existing resolved-data)]
          (swap! state assoc-in [table id] updated)
          updated))))

  (delete [_this table id]
    (let [existed? (contains? (get @state table {}) id)]
      (swap! state update table dissoc id)
      {:success? existed?}))

  (exists? [_this table field value]
    (some #(= (get % field) value)
      (vals (get @state table {}))))

  (execute! [_this _sql _params]
    []))

(defn create-test-service-container []
  (let [db-state (atom {})
        db-adapter (->MockDatabaseAdapter db-state)
        metadata-service (metadata-service/create-metadata-service test-models-data)
        type-casting-service (metadata-service/create-type-casting-service test-models-data)
        validation-service (metadata-service/create-validation-service test-models-data db-adapter)
        query-builder (metadata-service/create-query-builder test-models-data)]

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
;; Full CRUD Pipeline Integration Tests
;; ============================================================================

(deftest test-complete-crud-pipeline
  (testing "Complete CRUD pipeline integration"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; 1. Create a user first (for foreign key reference)
      (let [user-data {:email "test@example.com"
                       :full_name "Test User"
                       :role "owner"
                       :status "active"}
            created-user (crud-protocols/create-item crud-service test-tenant-id :users user-data {})]

        (is (not (nil? created-user)))
        (is (= "test@example.com" (:email created-user)))
        (is (= test-tenant-id (:tenant_id created-user)))

        ;; 2. Create a property referencing the user
        (let [property-data {:name "Test Property"
                             :address "123 Test St"
                             :property_type "apartment"
                             :owner_id (:id created-user)}
              created-property (crud-protocols/create-item crud-service test-tenant-id :properties property-data {})]

          (is (not (nil? created-property)))
          (is (= "Test Property" (:name created-property)))
          (is (= test-tenant-id (:tenant_id created-property)))
          (is (= (:id created-user) (:owner_id created-property)))

          ;; 3. Retrieve the property
          (let [retrieved-property (crud-protocols/get-item crud-service test-tenant-id :properties
                                     (:id created-property) {})]
            (is (not (nil? retrieved-property)))
            (is (= (:id created-property) (:id retrieved-property)))
            (is (= "Test Property" (:name retrieved-property))))

          ;; 4. Update the property
          (let [update-data {:name "Updated Property"
                             :address "456 Updated St"}
                updated-property (crud-protocols/update-item crud-service test-tenant-id :properties
                                   (:id created-property) update-data {})]

            (is (not (nil? updated-property)))
            (is (= "Updated Property" (:name updated-property)))
            (is (= "456 Updated St" (:address updated-property)))
            (is (= (:id created-property) (:id updated-property))))

          ;; 5. List all properties for the tenant
          (let [all-properties (crud-protocols/get-items crud-service test-tenant-id :properties {})]
            (is (= 1 (count all-properties)))
            (is (= "Updated Property" (:name (first all-properties)))))

          ;; 6. Create a transaction referencing the property
          (let [transaction-data {:property_id (:id created-property)
                                  :amount 1000.50
                                  :description "Monthly rent"
                                  :transaction_date "2024-01-15"}
                created-transaction (crud-protocols/create-item crud-service test-tenant-id :transactions
                                      transaction-data {})]

            (is (not (nil? created-transaction)))
            (is (= 1000.50M (:amount created-transaction)))
            (is (= (:id created-property) (:property_id created-transaction)))

            ;; 7. List all transactions for the tenant
            (let [all-transactions (crud-protocols/get-items crud-service test-tenant-id :transactions {})]
              (is (= 1 (count all-transactions)))
              (is (= 1000.50M (:amount (first all-transactions)))))

            ;; 8. Delete the transaction
            (let [delete-result (crud-protocols/delete-item crud-service test-tenant-id :transactions
                                  (:id created-transaction) {})]
              (is (:success? delete-result))

              ;; Verify transaction is deleted
              (let [deleted-transaction (crud-protocols/get-item crud-service test-tenant-id :transactions
                                          (:id created-transaction) {})]
                (is (nil? deleted-transaction)))))

          ;; 9. Delete the property
          (let [delete-result (crud-protocols/delete-item crud-service test-tenant-id :properties
                                (:id created-property) {})]
            (is (:success? delete-result))

            ;; Verify property is deleted
            (let [deleted-property (crud-protocols/get-item crud-service test-tenant-id :properties
                                     (:id created-property) {})]
              (is (nil? deleted-property))))

          ;; 10. Delete the user
          (let [delete-result (crud-protocols/delete-item crud-service test-tenant-id :users
                                (:id created-user) {})]
            (is (:success? delete-result))))))))

(deftest test-multi-tenant-isolation-integration
  (testing "Multi-tenant isolation in complete pipeline"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)
          tenant1-id test-tenant-id
          tenant2-id #uuid "223e4567-e89b-12d3-a456-426614174000"]

      ;; Create data for tenant 1
      (let [_tenant1-user (crud-protocols/create-item crud-service tenant1-id :users
                            {:email "user1@tenant1.com"
                             :full_name "Tenant 1 User"
                             :role "owner"
                             :status "active"} {})]

        ;; Create data for tenant 2
        (let [tenant2-user (crud-protocols/create-item crud-service tenant2-id :users
                             {:email "user1@tenant2.com"
                              :full_name "Tenant 2 User"
                              :role "owner"
                              :status "active"} {})
              tenant2-property (crud-protocols/create-item crud-service tenant2-id :properties
                                 {:name "Tenant 2 Property"
                                  :address "Tenant 2 Address"
                                  :owner_id (:id tenant2-user)} {})]

          ;; Test tenant 1 can only see their data
          (let [tenant1-users (crud-protocols/get-items crud-service tenant1-id :users {})
                tenant1-properties (crud-protocols/get-items crud-service tenant1-id :properties {})]

            (is (= 1 (count tenant1-users)))
            (is (= "user1@tenant1.com" (:email (first tenant1-users))))

            (is (= 1 (count tenant1-properties)))
            (is (= "Tenant 1 Property" (:name (first tenant1-properties)))))

          ;; Test tenant 2 can only see their data
          (let [tenant2-users (crud-protocols/get-items crud-service tenant2-id :users {})
                tenant2-properties (crud-protocols/get-items crud-service tenant2-id :properties {})]

            (is (= 1 (count tenant2-users)))
            (is (= "user1@tenant2.com" (:email (first tenant2-users))))

            (is (= 1 (count tenant2-properties)))
            (is (= "Tenant 2 Property" (:name (first tenant2-properties)))))

          ;; Test cross-tenant access is blocked
          (let [cross-tenant-user (crud-protocols/get-item crud-service tenant1-id :users (:id tenant2-user) {})
                cross-tenant-property (crud-protocols/get-item crud-service tenant1-id :properties (:id tenant2-property) {})]

            (is (nil? cross-tenant-user))
            (is (nil? cross-tenant-property))))))))

(deftest test-validation-integration-pipeline
  (testing "Validation integration throughout the pipeline"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; Test validation failure on create
      (try
        (crud-protocols/create-item crud-service test-tenant-id :users
          {:email ""  ; Invalid: empty email
           :full_name "Test User"
           :role "owner"
           :status "active"} {})
        (is false "Should have thrown validation error")
        (catch Exception e
          (is (= :validation-error (:type (ex-data e))))))

      ;; Test successful validation and creation
      (let [valid-user (crud-protocols/create-item crud-service test-tenant-id :users
                         {:email "valid@example.com"
                          :full_name "Valid User"
                          :role "owner"
                          :status "active"} {})]

        (is (not (nil? valid-user)))
        (is (= "valid@example.com" (:email valid-user)))

        ;; Test validation on update
        (try
          (crud-protocols/update-item crud-service test-tenant-id :users (:id valid-user)
            {:email ""} {})  ; Invalid: empty email
          (is false "Should have thrown validation error")
          (catch Exception e
            (is (= :validation-error (:type (ex-data e))))))

        ;; Test successful update with validation
        (let [updated-user (crud-protocols/update-item crud-service test-tenant-id :users (:id valid-user)
                             {:full_name "Updated User"} {})]
          (is (not (nil? updated-user)))
          (is (= "Updated User" (:full_name updated-user)))
          (is (= "valid@example.com" (:email updated-user))))))))

(deftest test-modern-crud-handlers-integration
  (testing "Modern CRUD handlers integration"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; Test that modern handler functions are available and return functions
      (is (fn? (crud-routes/get-items-handler crud-service)))
      (is (fn? (crud-routes/get-item-handler crud-service)))
      (is (fn? (crud-routes/create-item-handler crud-service)))
      (is (fn? (crud-routes/update-item-handler crud-service)))
      (is (fn? (crud-routes/delete-item-handler crud-service)))
      (is (fn? (crud-routes/batch-update-handler crud-service)))
      (is (fn? (crud-routes/batch-delete-handler crud-service)))
      (is (fn? (crud-routes/validate-field-handler crud-service)))

      ;; Test that crud-routes function returns proper route structure
      (let [routes (crud-routes/crud-routes crud-service)]
        (is (vector? routes))
        (is (> (count routes) 0))

        ;; Verify route structure contains expected paths
        (let [route-paths (map first routes)]
          (is (some #(= "/entities/{entity}" %) route-paths))
          (is (some #(= "/entities/{entity}/items/{id}" %) route-paths))
          (is (some #(= "/entities/{entity}/validate" %) route-paths))
          (is (some #(= "/entities/{entity}/batch" %) route-paths)))))))

;; ============================================================================
;; Performance and Load Tests
;; ============================================================================

(deftest test-batch-operations-performance
  (testing "Batch operations performance"
    (let [container (create-test-service-container)
          crud-service (:crud-service container)]

      ;; Create multiple users in batch
      (let [user-ids (atom [])]
        (dotimes [i 10]
          (let [user-data {:email (str "user" i "@example.com")
                           :full_name (str "User " i)
                           :role "member"
                           :status "active"}
                created-user (crud-protocols/create-item crud-service test-tenant-id :users user-data {})]
            (swap! user-ids conj (:id created-user))))

        ;; Verify all users were created
        (let [all-users (crud-protocols/get-items crud-service test-tenant-id :users {})]
          (is (= 10 (count all-users))))

        ;; Test batch delete
        (let [batch-delete-result (crud-protocols/batch-delete-items crud-service test-tenant-id :users @user-ids {})]
          (is (= 10 (:deleted batch-delete-result)))
          (is (= @user-ids (:deleted-ids batch-delete-result)))

          ;; Verify all users are deleted
          (let [remaining-users (crud-protocols/get-items crud-service test-tenant-id :users {})]
            (is (empty? remaining-users))))))))

;; ============================================================================
;; Test Runner
;; ============================================================================

(defn run-integration-tests []
  (log/info "Running CRUD integration tests...")
  (run-tests 'app.template.backend.crud.integration-test))

(comment
  (run-integration-tests))
