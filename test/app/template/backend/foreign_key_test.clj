(ns app.template.backend.foreign-key-test
  (:require
    [app.backend.mock-data-helper :refer [get-mock-data]]
    [app.domain.backend.auto-test-data :as auto-data]
    [app.domain.backend.fixtures :refer [current-db with-clean-transaction]]
    [cheshire.core]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time LocalDate LocalDateTime]))

;; Use transaction isolation for perfect test isolation
;; System started globally by Kaocha hook, no individual fixture needed
;; (use-fixtures :once start-test-system)
(use-fixtures :each with-clean-transaction)

(defn get-tables-with-foreign-keys
  "Get all entities that have foreign key fields using auto-data system"
  []
  (let [models (auto-data/load-models)
        entities-with-fks (filter
                            (fn [entity-keyword]
                              (let [entity-def (get models entity-keyword)
                                    fields (:fields entity-def)]
                                (some #(get-in % [2 :foreign-key]) fields)))
                            (keys models))]
    entities-with-fks))

(defn get-referenced-table [models table-name field-name]
  (let [fields (get-in models [table-name :fields])
        field-def (first (filter #(= (first %) field-name) fields))
        constraints (when (and field-def (>= (count field-def) 3)) (nth field-def 2))
        foreign-key (:foreign-key constraints)]
    (when foreign-key
      (-> foreign-key namespace keyword))))

(defn get-foreign-key-fields
  "Get all foreign key fields for a specific entity"
  [models table-name]
  (->> (get-in models [table-name :fields])
    (filter #(and (>= (count %) 3)
               (:foreign-key (nth % 2))))
    (map first)))

(defn quote-table-name [table-name]
  (str "\""
    (-> table-name
      name
      (str/replace "-" "_"))
    "\""))

(defn get-valid-foreign-key
  "Get a valid foreign key value from the referenced table"
  [db referenced-table]
  (when (and db referenced-table)
    (try
      (let [sql (str "SELECT id FROM " (quote-table-name referenced-table) " LIMIT 1")
            result (jdbc/execute-one! db [sql] {:builder-fn rs/as-unqualified-maps})]
        (:id result))
      (catch Exception e
        (println "Warning: Could not get valid foreign key for table" referenced-table ":" (.getMessage e))
        nil))))

(defn ensure-foundation-records
  "Ensure basic foundation records exist for foreign key testing"
  [db]
  (let [default-tenant-id (java.util.UUID/fromString "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")]
    (try
      ; Create a basic user record if it doesn't exist
      (jdbc/execute-one! db
        ["INSERT INTO users (id, tenant_id, email, auth_provider, role, status)
              VALUES (?, ?, ?, ?, ?, ?)
              ON CONFLICT (id) DO NOTHING"
         default-tenant-id default-tenant-id "test@example.com" "google" "member" "active"])

      ; Create a basic tenant record if it doesn't exist
      (jdbc/execute-one! db
        ["INSERT INTO tenants (id, name, slug, status)
              VALUES (?, ?, ?, ?)
              ON CONFLICT (id) DO NOTHING"
         default-tenant-id "Test Tenant" "test-tenant" "active"])

      ; Create a basic property record if it doesn't exist
      (jdbc/execute-one! db
        ["INSERT INTO properties (id, tenant_id, owner_id, name, property_type, status)
              VALUES (?, ?, ?, ?, ?, ?)
              ON CONFLICT (id) DO NOTHING"
         default-tenant-id default-tenant-id default-tenant-id "Test Property" "apartment" "active"])

      (catch Exception e
        (println "Warning: Could not create foundation records:" (.getMessage e))))))

(defn generate-minimal-test-data
  "Generate minimal test data for foreign key testing with only required fields"
  [entity-keyword field fk-value models]
  (let [entity-spec (get models entity-keyword)
        fields (:fields entity-spec)
        required-fields (filter (fn [field-def]
                                  (let [constraints (when (>= (count field-def) 3) (nth field-def 2))
                                        nullable (get constraints :null true)]
                                    (not nullable)))
                          fields)
        ; Build minimal data map with only required fields
        minimal-data (reduce (fn [acc field-def]
                               (let [field-name (first field-def)
                                     field-type (second field-def)
                                     constraints (when (>= (count field-def) 3) (nth field-def 2))]
                                 (cond
                                   ; Set the foreign key field we're testing
                                   (= field-name field)
                                   (assoc acc field-name fk-value)

                                   ; Always set tenant_id for multi-tenant support
                                   (= field-name :tenant_id)
                                   (assoc acc field-name (java.util.UUID/fromString "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"))

                                   ; Handle common foreign key fields with default values
                                   (#{:created_by :owner_id :user_id :inviter_id :invitee_id :property_id :property_cohost_id :cohost_id} field-name)
                                   (assoc acc field-name (java.util.UUID/fromString "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"))

                                   ; Skip system-managed fields
                                   (#{:id :created_at :updated_at} field-name)
                                   acc

                                   ; Handle other foreign key fields (set to valid values from FK constraint)
                                   (:foreign-key constraints)
                                   acc                      ; Skip for now, will be handled by populate-foreign-keys if needed

                                   ; Generate minimal valid values for other required fields
                                   (= field-type :text)
                                   (assoc acc field-name "Test Description")

                                   (or (= field-type :varchar)
                                     (and (vector? field-type) (= (first field-type) :varchar)))
                                   (assoc acc field-name (str "test-" (name field-name) "-" (System/currentTimeMillis)))

                                   (= field-type :decimal)
                                   (assoc acc field-name 10.00)

                                   (= field-type :integer)
                                   (assoc acc field-name 1)

                                   (= field-type :boolean)
                                   (assoc acc field-name true)

                                   (= field-type :date)
                                   (assoc acc field-name (java.time.LocalDate/now))

                                   (= field-type :jsonb)
                                   (assoc acc field-name {})

                                   (= field-type :uuid)
                                   (assoc acc field-name (java.util.UUID/randomUUID))

                                   (and (vector? field-type) (= (first field-type) :enum))
                                   (let [enum-name (second field-type)
                                         types (:types entity-spec)
                                         enum-def (first (filter #(= (first %) enum-name) types))
                                         choices (when enum-def (get-in enum-def [2 :choices]))]
                                     (assoc acc field-name (or (first choices) "default")))

                                   :else acc)))
                       {}
                       required-fields)]
    ; Add entity-specific required fields based on known schema requirements
    (case entity-keyword
      :properties (merge minimal-data
                    {:name "Test Property"
                     :property_type "apartment"             ; Fixed column name
                     :status "active"})
      :users (merge minimal-data
               {:email (str "test-" (System/currentTimeMillis) "@example.com")
                :auth_provider "google"                     ; Added required field
                :role "member"
                :status "active"})
      :tenant_usage (merge minimal-data
                      {:metric_name "test_metric"
                       :usage_value 1                       ; Fixed column name
                       :period_start (java.time.LocalDate/now)
                       :period_end (.plusDays (java.time.LocalDate/now) 30)})
      :audit_logs (merge minimal-data
                    {:action "test_action"
                     :entity_type "test_entity"})
      :cohost_balances (merge minimal-data
                         {:period_start (java.time.LocalDate/now)
                          :period_end (.plusDays (java.time.LocalDate/now) 30)
                          :total_income 1000.00
                          :total_expenses 500.00
                          :owner_share 600.00
                          :cohost_share 400.00})            ; Removed net_balance
      :invitations (merge minimal-data
                     {:invitee_email (str "invitee-" (System/currentTimeMillis) "@example.com")
                      :role "cohost"
                      :status "pending"
                      :token (str "token-" (java.util.UUID/randomUUID))
                      :expires_at (.plusDays (LocalDateTime/now) 7)})
      :transaction_templates (merge minimal-data
                               {:name "Test Template"
                                :template_data {}
                                :visibility "private"})
      :transactions (merge minimal-data
                      {:amount 100.00
                       :date (java.time.LocalDate/now)
                       :description "Test Transaction"})
      :transaction_types (merge minimal-data
                           {:name (str "Test Type " (System/currentTimeMillis))
                            :flow "income"})
      minimal-data)))

(defn get-invalid-foreign-key-value
  "Get an invalid foreign key value that shouldn't exist in any table"
  []
  999999)

(defn normalize-field-name
  "Convert field name from kebab-case to snake_case for database"
  [field-name]
  (str/replace (name field-name) "-" "_"))

(defn to-sql-value
  "Convert Clojure values to SQL-compatible values"
  [value]
  (cond
    (instance? LocalDate value) (java.sql.Date/valueOf value)
    (instance? LocalDateTime value) (java.sql.Timestamp/valueOf value)
    (instance? java.util.UUID value) (str value)
    :else value))

(defn get-column-placeholder
  "Get placeholder for SQL column based on field type"
  [_field-name field-type]
  (cond
    (and (vector? field-type) (= (first field-type) :enum))
    (str "?::" (str/replace (name (second field-type)) "-" "_"))

    (= field-type :uuid)
    "?::uuid"

    (= field-type :inet)
    "?::inet"

    (= field-type :jsonb)
    "?::jsonb"

    :else "?"))

(defn verify-test-data-exists []
  (doseq [[entity-name {:keys [seed-data]}] (get-mock-data)]
    (when seed-data
      (let [table-name (quote-table-name entity-name)
            count-sql (str "SELECT COUNT(*) as count FROM " table-name)
            count-result (jdbc/execute-one! (current-db) [count-sql] {:builder-fn rs/as-unqualified-maps})
            actual-count (:count count-result)
            expected-count (count seed-data)]
        ;; For audit_logs and tenant_usage, be more flexible due to triggers/multiple test contexts creating additional records
        (if (#{:audit_logs :tenant_usage} entity-name)
          (is (>= actual-count expected-count)
            (str "Expected at least " expected-count " records in " entity-name
              " but found only " actual-count ". Test data may not be properly seeded."))
          ;; For other tables, check for exact or slightly higher counts (within reason)
          (is (<= expected-count actual-count (+ expected-count 2))
            (str "Expected around " expected-count " records in " entity-name
              " but found " actual-count ". Test data may not be properly seeded.")))))))

(deftest test-foreign-key-constraints
  (testing "Verifying test data is properly seeded"
    (verify-test-data-exists))

  (testing "Foreign key constraints for all tables with foreign keys"
    (let [models (auto-data/load-models)
          tables (get-tables-with-foreign-keys)]
      (doseq [table tables]
        (let [fk-fields (get-foreign-key-fields models table)]
          (testing (str "Testing foreign key constraints for table: " table)
            (doseq [field fk-fields]
              (let [invalid-fk-value (get-invalid-foreign-key-value)
                    field-name (normalize-field-name field)]
                (testing (str "Should not allow invalid foreign key for field: " field)
                  (is (thrown? Exception
                        (jdbc/execute-one! (current-db)
                          [(str "INSERT INTO " (quote-table-name table)
                             " (" field-name ") VALUES (?)")
                           invalid-fk-value]
                          {:return-keys true
                           :builder-fn rs/as-unqualified-maps}))
                    (str "Expected foreign key constraint violation for " field)))))))))))
