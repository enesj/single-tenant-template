(ns app.template.backend.crud-meta-test
  (:require
    [app.domain.backend.auto-test-data :as auto-data]
    [app.domain.backend.fixtures :as fixtures]
    [app.shared.field-casting :as field-casting]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as honey.sql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

(use-fixtures :once fixtures/start-test-system)

(use-fixtures :each fixtures/with-clean-transaction)

;; Registry to store created records for foreign key resolution
(def ^:dynamic *created-records* (atom {}))

;; Removed get-records-with-ids - no longer needed for direct DB testing

(defn get-models-edn []
  (-> (slurp "resources/db/models.edn")
    (edn/read-string)))

(defn normalize-field-names [data]
  (into {} (map (fn [[k v]]
                  [(-> k name (str/replace "-" "_") keyword) v])
             data)))

(defn update-data-context-with-created-record
  "Update the data context with a created record for foreign key resolution"
  [entity-keyword created-record]
  (when created-record
    (swap! auto-data/*data-context* update entity-keyword (fnil conj []) created-record)
    (swap! *created-records* update entity-keyword (fnil conj []) created-record)))

(defn get-valid-foreign-key-id
  "Get a valid foreign key ID from previously created records"
  [fk-entity]
  (let [created-records (get @*created-records* fk-entity)
        context-records (get @auto-data/*data-context* fk-entity)]
    (when-let [record (or (first created-records) (first context-records))]
      (str (:id record)))))

;; Forward declare inject-valid-foreign-keys-impl
(declare inject-valid-foreign-keys-impl)

(defn create-missing-parent-record
  "Create a missing parent record when needed for FK resolution"
  [fk-entity models db creating-entities]
  (if (contains? creating-entities fk-entity)
    nil  ; Prevent infinite recursion
    (let [parent-data (auto-data/generate-entity-data fk-entity :valid models)
          clean-data (dissoc parent-data :id :created_at :updated_at)
          fk-injected-data (inject-valid-foreign-keys-impl fk-entity clean-data models db (conj creating-entities fk-entity))
          prepared-data (field-casting/prepare-insert-data models fk-entity fk-injected-data)
          table-name (str/replace (name fk-entity) "-" "_")
          insert-sql (honey.sql/format {:insert-into (keyword table-name)
                                        :values [prepared-data]})]

      (jdbc/execute! db insert-sql)

      (let [created-record (jdbc/execute-one! db [(str "SELECT * FROM " table-name " ORDER BY created_at DESC LIMIT 1")])]
        (update-data-context-with-created-record fk-entity created-record)
        (:id created-record)))))

(defn inject-valid-foreign-keys-impl
  "Internal implementation of FK injection with recursion tracking"
  [entity-keyword data models db creating-entities]
  (let [entity-def (get models entity-keyword)
        fields (:fields entity-def)
        ;; Get FK field names to preserve them even if they have nil values
        fk-field-names (set (keep (fn [field-def]
                                    (let [field-name (auto-data/get-field-name field-def)
                                          constraints (auto-data/get-field-constraints field-def)]
                                      (when (:foreign-key constraints)
                                        field-name)))
                              fields))
        ;; Start with data but filter out non-FK fields that have nil or empty values
        ;; Keep FK fields even if they're nil so we can inject them
        clean-data (into {} (filter (fn [[k v]]
                                      (or (contains? fk-field-names k)  ; Always keep FK fields
                                        (and (some? v)                ; Filter non-FK fields
                                          (not= v "")
                                          (not= v nil))))
                              data))]

    (reduce (fn [acc field-def]
              (let [field-name (auto-data/get-field-name field-def)
                    constraints (auto-data/get-field-constraints field-def)
                    fk-ref (:foreign-key constraints)]

                (if fk-ref  ; If this field has a foreign key, try to inject
                  (let [fk-entity (-> fk-ref namespace keyword)
                        valid-id (get-valid-foreign-key-id fk-entity)]

                    (if (and valid-id (not= valid-id "") (not= valid-id nil))
                      (assoc acc field-name valid-id)
                      ;; Create parent record if required
                      (let [is-required (not (:null constraints true))]
                        (if is-required
                          (let [created-id (create-missing-parent-record fk-entity models db creating-entities)]
                            (if created-id
                              (assoc acc field-name (str created-id))
                              (dissoc acc field-name)))
                          (dissoc acc field-name)))))
                  acc)))
      clean-data
      fields)))

(defn inject-valid-foreign-keys
  "Inject valid foreign key IDs into test data, creating parent records when needed"
  ([entity-keyword data models]
   (inject-valid-foreign-keys entity-keyword data models fixtures/*current-tx*))
  ([entity-keyword data models db]
   (inject-valid-foreign-keys-impl entity-keyword data models db #{})))

;; Removed create-test-entity-debug - no longer needed for direct DB testing

(defn test-entity-crud-meta
  "Test CRUD operations for an entity using fully auto-generated data from models.edn"
  [entity-keyword]
  (let [entity-name         (name entity-keyword)
        models              (get-models-edn)
        db                  fixtures/*current-tx*
        raw-create-data     (auto-data/generate-entity-data entity-keyword :valid models)
        valid-create-data   (inject-valid-foreign-keys entity-keyword raw-create-data models db)
        invalid-create-data (auto-data/generate-entity-data entity-keyword :invalid models)
        raw-update-data     (auto-data/generate-entity-data entity-keyword :valid models)
        valid-update-data   (inject-valid-foreign-keys entity-keyword raw-update-data models db)]

    (testing (str "Direct database CRUD operations for " entity-name)
      ;; Test CREATE with valid data
      (testing "Create with valid data"
        (let [table-name (str/replace entity-name "-" "_")
              data-without-id (dissoc valid-create-data :id)
              prepared-data (field-casting/prepare-insert-data models entity-keyword data-without-id)
              count-before (jdbc/execute-one! db [(str "SELECT COUNT(*) as count FROM " table-name)])
              insert-sql (honey.sql/format {:insert-into (keyword table-name)
                                            :values [prepared-data]})]

          (try
            (jdbc/execute! db insert-sql)
            (catch Exception e
              (log/error "INSERT failed for" entity-name ":" (.getMessage e))
              (throw e)))

          (let [count-after (jdbc/execute-one! db [(str "SELECT COUNT(*) as count FROM " table-name)])
                insert-successful? (> (:count count-after) (:count count-before))
                sample-record-query (if (contains? (set (map #(auto-data/get-field-name %)
                                                          (:fields (get models entity-keyword))))
                                          :created_at)
                                      (str "SELECT * FROM " table-name " ORDER BY created_at DESC LIMIT 1")
                                      (str "SELECT * FROM " table-name " LIMIT 1"))
                sample-record (jdbc/execute-one! db [sample-record-query])
                all-records-debug (when (nil? sample-record)
                                    (jdbc/execute! db [(str "SELECT * FROM " table-name)]))]

            (is insert-successful? "Should successfully insert record")
            (let [final-sample-record (or sample-record (first all-records-debug))]
              (is (some? final-sample-record) "Should have at least one record in table")
              (when final-sample-record
                (is (some? (:id final-sample-record)) "Sample record should have an ID")
                (update-data-context-with-created-record entity-keyword final-sample-record))))))

      ;; Test CREATE with invalid data
      (testing "Create with invalid data should fail"
        (let [table-name (str/replace entity-name "-" "_")
              data-without-id (dissoc invalid-create-data :id)
              prepared-data (field-casting/prepare-insert-data models entity-keyword data-without-id)]
          (is (thrown? Exception
                (jdbc/execute! db
                  (honey.sql/format {:insert-into (keyword table-name)
                                     :values [prepared-data]}))))))

      ;; Test READ all
      (testing "Read all records"
        (let [table-name (str/replace entity-name "-" "_")
              all-records (jdbc/execute! db [(str "SELECT * FROM " table-name)])]
          (is (>= (count all-records) 1) "Should have at least one record")))

      ;; Test READ single record
      (testing "Read single record"
        (let [table-name (str/replace entity-name "-" "_")
              first-record (jdbc/execute-one! db [(str "SELECT * FROM " table-name " LIMIT 1")])]
          (is (some? first-record) "Should find at least one record")
          (is (some? (:id first-record)) "Record should have an ID")))

      ;; Test UPDATE
      (testing "Update with valid data"
        (let [table-name (str/replace entity-name "-" "_")
              record-to-update (jdbc/execute-one! db [(str "SELECT * FROM " table-name " LIMIT 1")])
              update-id (:id record-to-update)
              data-without-id (dissoc valid-update-data :id)
              prepared-update-data (field-casting/prepare-insert-data models entity-keyword data-without-id)
              update-sql (honey.sql/format {:update (keyword table-name)
                                            :set prepared-update-data
                                            :where [:= :id update-id]})
              _ (jdbc/execute! db update-sql)
              updated-record (jdbc/execute-one! db [(str "SELECT * FROM " table-name " WHERE id = ?") update-id])]
          (is (some? updated-record) "Should return updated record")))

      ;; Test DELETE
      (testing "Delete record"
        (let [table-name (str/replace entity-name "-" "_")
              record-to-delete (jdbc/execute-one! db [(str "SELECT * FROM " table-name " LIMIT 1")])
              delete-id (:id record-to-delete)
              delete-sql (honey.sql/format {:delete-from (keyword table-name)
                                            :where [:= :id delete-id]})
              _ (jdbc/execute! db delete-sql)
              deleted-check (jdbc/execute-one! db [(str "SELECT * FROM " table-name " WHERE id = ?") delete-id])]
          (is (nil? deleted-check) "Record should be deleted"))))))

(deftest test-tenants-only
  (testing "Test FK injection for tenant_usage specifically"
    (binding [*created-records* (atom {})
              auto-data/*data-context* (atom {})]
      (let [models (get-models-edn)
            db fixtures/*current-tx*
            ;; Test FK injection for tenant_usage (should create missing tenant)
            raw-tenant-usage-data (auto-data/generate-entity-data :tenant_usage :valid models)
            injected-data (inject-valid-foreign-keys :tenant_usage raw-tenant-usage-data models db)]
        (is (contains? injected-data :tenant_id) "Should have tenant_id after injection")
        (is (some? (:tenant_id injected-data)) "tenant_id should not be nil")))))

(deftest test-all-entities-meta-driven
  (testing "Test CRUD operations for all entities defined in models.edn using fully auto-generated data"
    (binding [*created-records* (atom {})
              auto-data/*data-context* (atom {})]
      (let [models           (get-models-edn)
            entity-keywords  (keys models)
            dependency-graph (auto-data/build-dependency-graph models)
            sorted-entities  (auto-data/topological-sort dependency-graph entity-keywords)]

        ;; Test entities in dependency order
        (doseq [entity-keyword sorted-entities]
          (test-entity-crud-meta entity-keyword))))))
