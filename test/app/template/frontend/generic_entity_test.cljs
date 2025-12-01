(ns app.template.frontend.generic-entity-test
  "Example of fully generic entity tests that work with any models.edn structure"
  (:require
    [app.template.frontend.auto-test-data :as auto-test-data]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.state.normalize :as normalize]
    [cljs.test :refer [deftest is run-tests testing]]))

(deftest generic-entity-generation-test
  (testing "Generate entities for all types defined in models"
    (helpers/reset-test-data!)

    ;; Test each entity type without hardcoding names
    (helpers/run-test-for-each-entity
      (fn [entity-type]
        (let [entity (auto-test-data/generate-entity entity-type)]
          (is (map? entity) (str entity-type " should return a map"))

          ;; Get field definitions for this entity type
          (let [entity-def (get auto-test-data/models entity-type)
                fields (:fields entity-def)]

            ;; Check each field exists and has appropriate type
            (doseq [field-def fields]
              (let [field-name (first field-def)
                    field-type (second field-def)]
                ;; Skip system fields
                (when-not (#{:id :created_at :updated_at} field-name)
                  (is (contains? entity field-name)
                    (str entity-type " should have field " field-name))

                  ;; Basic type checking
                  (let [value (get entity field-name)]
                    (cond
                      (#{:text :varchar} (if (vector? field-type)
                                           (first field-type)
                                           field-type))
                      (is (string? value)
                        (str field-name " should be string"))

                      (= field-type :decimal)
                      (is (number? value)
                        (str field-name " should be number"))

                      (= field-type :integer)
                      (is (or (number? value) (nil? value))
                        (str field-name " should be number or nil"))

                      (= field-type :date)
                      (is (string? value)
                        (str field-name " should be date string"))

                      (and (vector? field-type)
                        (= (first field-type) :enum))
                      (is (string? value)
                        (str field-name " should be enum string")))))))))))))

(deftest generic-normalization-test
  (testing "Normalize/denormalize for all entity types"
    (helpers/reset-test-data!)

    (helpers/run-test-for-each-entity
      (fn [entity-type]
        (let [;; Generate entities with indexed names to ensure uniqueness
              ;; This is particularly important for transaction-types which have a unique name constraint
              entities (map-indexed
                         (fn [idx _]
                           (auto-test-data/generate-entity
                             entity-type
                             {:name (str "Test " entity-type " " idx)}))
                         (range 5))
              normalized (normalize/normalize-entities entities)]

          (helpers/assert-normalized-structure normalized)
          (is (= 5 (count (:data normalized)))
            (str entity-type " should normalize all entities"))
          (is (= 5 (count (:ids normalized)))
            (str entity-type " should have all IDs"))

          ;; Test denormalization
          (let [denormalized (normalize/denormalize-entities normalized)]
            (is (= 5 (count denormalized))
              (str entity-type " should denormalize all entities"))

            ;; Verify data integrity
            (doseq [original entities]
              (let [denorm-entity (first (filter #(= (:id %) (:id original))
                                           denormalized))]
                (is (= original denorm-entity)
                  (str entity-type " data should be preserved"))))))))))

(deftest generic-crud-operations-test
  (testing "CRUD operations for all entity types"
    (helpers/reset-test-data!)

    (helpers/run-test-for-each-entity
      (fn [entity-type]
        (let [initial-state (helpers/empty-normalized-state)
              new-entity (auto-test-data/generate-entity entity-type)
              after-add (normalize/add-entity initial-state new-entity)
              _ (helpers/assert-entity-in-normalized after-add (:id new-entity))
              updated-entity (merge new-entity {:updated-field "new-value"})
              after-update (normalize/update-entity after-add updated-entity)]
          ;; Update
          (is (= "new-value"
                (get-in after-update [:data (:id new-entity) :updated-field]))
            (str entity-type " should update correctly"))

          ;; Delete
          (let [after-remove (normalize/remove-entity after-update (:id new-entity))]
            (helpers/assert-entity-not-in-normalized after-remove (:id new-entity))))))))

(deftest generic-invalid-data-test
  (testing "Invalid data generation for all entity types"
    (helpers/reset-test-data!)

    (helpers/run-test-for-each-entity
      (fn [entity-type]
        (let [invalid-entity (auto-test-data/generate-invalid-entity entity-type)]
          (is (map? invalid-entity)
            (str entity-type " invalid data should be a map"))

          ;; Check that at least some fields have invalid values
          (let [entity-def (get auto-test-data/models entity-type)
                fields (:fields entity-def)
                has-invalid-field?
                (some (fn [field-def]
                        (let [field-name (first field-def)
                              constraints (when (>= (count field-def) 3)
                                            (nth field-def 2))
                              value (get invalid-entity field-name)]
                          ;; Check for various invalid conditions
                          (or (and (:null constraints) (= false (:null constraints))
                                (nil? value))
                            (and (= :decimal (second field-def))
                              (< value 0))
                            (and (string? value)
                              (= "" value)))))
                  fields)]
            (is has-invalid-field?
              (str entity-type " should have at least one invalid field"))))))))

(deftest comprehensive-all-entities-test
  (testing "Comprehensive test data for all entities"
    (helpers/reset-test-data!)

    (let [all-data (helpers/generate-test-data-for-all-entities 3)]

      ;; Check we have data for all entity types
      (is (= (set (helpers/get-all-entity-types))
            (set (keys all-data)))
        "Should have data for all entity types")

      ;; Check each entity type has correct count
      (doseq [[entity-type entities] all-data]
        (is (= 3 (count entities))
          (str entity-type " should have 3 entities"))

        ;; Verify all entities are valid
        (doseq [entity entities]
          (is (map? entity) "Entity should be a map"))))))

(defn run-all-tests []
  (println "🧪 Running Generic Entity Tests...")
  (run-tests))

;; Export for browser testing
(set! js/window.runGenericEntityTests run-all-tests)
