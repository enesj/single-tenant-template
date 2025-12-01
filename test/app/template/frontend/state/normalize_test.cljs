(ns app.template.frontend.state.normalize-test
  "Comprehensive tests for state normalization functions - critical business logic"
  (:require
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.state.normalize :as normalize]
    [cljs.test :refer [are deftest is run-tests testing]]))

;; Test normalize-entity function
(deftest normalize-entity-test

  (testing "normalizes single entity correctly"
    (let [entity {:id 1 :name "Test Entity" :description "A test"}
          result (normalize/normalize-entity entity)]
      (is (= {1 {:id 1 :name "Test Entity" :description "A test"}} result)
        "Should create id->entity map")))

  (testing "handles entity with different id types"
    (are [entity expected] (= expected (normalize/normalize-entity entity))
      {:id "string-id" :name "String ID"} {"string-id" {:id "string-id" :name "String ID"}}
      {:id 42 :name "Number ID"} {42 {:id 42 :name "Number ID"}}
      {:id :keyword-id :name "Keyword"} {:keyword-id {:id :keyword-id :name "Keyword"}})))

;; Test normalize-entities function
(deftest normalize-entities-test

  (testing "normalizes collection of entities"
    (let [entities [{:id 1 :name "First"} {:id 2 :name "Second"} {:id 3 :name "Third"}]
          result (normalize/normalize-entities entities)]

      (helpers/assert-normalized-structure result)

      (is (= {1 {:id 1 :name "First"}
              2 {:id 2 :name "Second"}
              3 {:id 3 :name "Third"}} (:data result))
        "Should create correct data map")

      (is (= [1 2 3] (:ids result))
        "Should preserve entity order in ids vector")))

  (testing "handles empty collection"
    (let [result (normalize/normalize-entities [])]
      (helpers/assert-normalized-structure result)
      (is (= {} (:data result)) "Data should be empty map")
      (is (= [] (:ids result)) "IDs should be empty vector")))

  (testing "preserves original entity order"
    (let [entities [{:id 5 :name "Five"} {:id 1 :name "One"} {:id 3 :name "Three"}]
          result (normalize/normalize-entities entities)]
      (is (= [5 1 3] (:ids result))
        "Should maintain original order, not sort by ID")))

  (testing "handles entities with complex data"
    (let [entities [{:id 1
                     :name "Complex Entity"
                     :metadata {:tags ["tag1" "tag2"] :score 95}
                     :relationships {:parent-id 0 :children [2 3]}}]
          result (normalize/normalize-entities entities)]
      (is (= entities (normalize/denormalize-entities result))
        "Should preserve all entity data during normalization"))))

;; Test denormalize-entities function
(deftest denormalize-entities-test

  (testing "converts normalized data back to entity vector"
    (let [normalized {:data {1 {:id 1 :name "First"}
                             2 {:id 2 :name "Second"}}
                      :ids [1 2]}
          result (normalize/denormalize-entities normalized)]
      (is (= [{:id 1 :name "First"} {:id 2 :name "Second"}] result)
        "Should return entities in ID order")))

  (testing "maintains order from ids vector"
    (let [normalized {:data {1 {:id 1 :name "First"}
                             2 {:id 2 :name "Second"}
                             3 {:id 3 :name "Third"}}
                      :ids [3 1 2]}                         ; Different order
          result (normalize/denormalize-entities normalized)]
      (is (= [{:id 3 :name "Third"} {:id 1 :name "First"} {:id 2 :name "Second"}] result)
        "Should follow ids vector order, not data map order")))

  (testing "handles empty normalized data"
    (let [normalized {:data {} :ids []}
          result (normalize/denormalize-entities normalized)]
      (is (= [] result) "Should return empty vector")))

  (testing "round-trip normalization preserves data"
    (let [original-entities [{:id 3 :name "Third" :value 300}
                             {:id 1 :name "First" :value 100}
                             {:id 2 :name "Second" :value 200}]
          normalized (normalize/normalize-entities original-entities)
          denormalized (normalize/denormalize-entities normalized)]
      (is (= original-entities denormalized)
        "Round-trip should preserve exact data and order"))))

;; Test add-entity function
(deftest add-entity-test

  (testing "adds new entity to normalized structure"
    (let [initial (helpers/empty-normalized-state)
          new-entity {:id 1 :name "New Entity"}
          result (normalize/add-entity initial new-entity)]

      (helpers/assert-normalized-structure result)
      (helpers/assert-entity-in-normalized result 1)

      (is (= new-entity (get-in result [:data 1]))
        "Should add entity to data map")
      (is (= [1] (:ids result))
        "Should add ID to ids vector")))

  (testing "adds entity to existing normalized structure"
    (let [initial {:data {1 {:id 1 :name "First"}} :ids [1]}
          new-entity {:id 2 :name "Second"}
          result (normalize/add-entity initial new-entity)]

      (is (= 2 (count (:data result))) "Should have 2 entities")
      (is (= [1 2] (:ids result)) "Should append new ID to vector")))

  (testing "does not add duplicate entity"
    (let [initial {:data {1 {:id 1 :name "Original"}} :ids [1]}
          duplicate-entity {:id 1 :name "Duplicate"}
          result (normalize/add-entity initial duplicate-entity)]

      (is (= initial result) "Should not modify structure when ID exists")
      (is (= "Original" (get-in result [:data 1 :name]))
        "Should preserve original entity data")))

  (testing "maintains order when adding multiple entities"
    (let [initial (helpers/empty-normalized-state)
          entities [{:id 3 :name "Third"} {:id 1 :name "First"} {:id 2 :name "Second"}]
          result (reduce normalize/add-entity initial entities)]

      (is (= [3 1 2] (:ids result))
        "Should maintain addition order, not sort by ID"))))

;; Test update-entity function
(deftest update-entity-test

  (testing "updates existing entity"
    (let [initial {:data {1 {:id 1 :name "Original" :value 100}} :ids [1]}
          updated-entity {:id 1 :name "Updated" :value 200}
          result (normalize/update-entity initial updated-entity)]

      (is (= updated-entity (get-in result [:data 1]))
        "Should replace entity data")
      (is (= [1] (:ids result))
        "Should preserve ids vector")))

  (testing "does not modify structure when entity doesn't exist"
    (let [initial {:data {1 {:id 1 :name "Existing"}} :ids [1]}
          non-existent-entity {:id 999 :name "Does Not Exist"}
          result (normalize/update-entity initial non-existent-entity)]

      (is (= initial result)
        "Should not modify structure when ID doesn't exist")))

  (testing "updates preserve other entities"
    (let [initial {:data {1 {:id 1 :name "First"}
                          2 {:id 2 :name "Second"}
                          3 {:id 3 :name "Third"}}
                   :ids [1 2 3]}
          updated-entity {:id 2 :name "Updated Second" :new-field "added"}
          result (normalize/update-entity initial updated-entity)]

      (is (= {:id 1 :name "First"} (get-in result [:data 1]))
        "Should preserve other entities")
      (is (= {:id 2 :name "Updated Second" :new-field "added"} (get-in result [:data 2]))
        "Should update target entity")
      (is (= [1 2 3] (:ids result))
        "Should preserve ids order"))))

;; Test remove-entity function
(deftest remove-entity-test

  (testing "removes entity from normalized structure"
    (let [initial {:data {1 {:id 1 :name "First"}
                          2 {:id 2 :name "Second"}}
                   :ids [1 2]}
          result (normalize/remove-entity initial 1)]

      (helpers/assert-entity-not-in-normalized result 1)
      (helpers/assert-entity-in-normalized result 2)

      (is (= [2] (:ids result))
        "Should remove ID from ids vector")))

  (testing "handles removal of non-existent entity"
    (let [initial {:data {1 {:id 1 :name "Only"}} :ids [1]}
          result (normalize/remove-entity initial 999)]

      (is (= initial result)
        "Should not modify structure when ID doesn't exist")))

  (testing "removes from middle of collection"
    (let [initial {:data {1 {:id 1 :name "First"}
                          2 {:id 2 :name "Second"}
                          3 {:id 3 :name "Third"}}
                   :ids [1 2 3]}
          result (normalize/remove-entity initial 2)]

      (is (= [1 3] (:ids result))
        "Should preserve order when removing from middle")
      (is (= 2 (count (:data result)))
        "Should have correct data count")))

  (testing "handles removing all entities"
    (let [initial {:data {1 {:id 1 :name "Only"}} :ids [1]}
          result (normalize/remove-entity initial 1)]

      (is (= {:data {} :ids []} result)
        "Should result in empty normalized structure"))))

;; Test sort-normalized function
(deftest sort-normalized-test

  (testing "sorts by field in ascending order"
    (let [initial {:data {1 {:id 1 :name "Charlie" :value 300}
                          2 {:id 2 :name "Alice" :value 100}
                          3 {:id 3 :name "Bob" :value 200}}
                   :ids [1 2 3]}
          result (normalize/sort-normalized initial [:name :asc])]

      (is (= [2 3 1] (:ids result))
        "Should sort by name: Alice, Bob, Charlie")
      (is (= (:data initial) (:data result))
        "Should preserve data map unchanged")))

  (testing "sorts by field in descending order"
    (let [initial {:data {1 {:id 1 :value 100}
                          2 {:id 2 :value 300}
                          3 {:id 3 :value 200}}
                   :ids [1 2 3]}
          result (normalize/sort-normalized initial [:value :desc])]

      (is (= [2 3 1] (:ids result))
        "Should sort by value desc: 300, 200, 100")))

  (testing "handles missing sort parameters gracefully"
    (let [initial {:data {1 {:id 1 :name "Test"}} :ids [1]}]

      (is (= initial (normalize/sort-normalized initial [nil :asc]))
        "Should return unchanged when sort-field is nil")

      (is (= initial (normalize/sort-normalized initial [:name nil]))
        "Should return unchanged when sort-direction is nil")

      (is (= initial (normalize/sort-normalized initial nil))
        "Should return unchanged when sort params are nil")))

  (testing "handles empty normalized data"
    (let [empty-data {:data {} :ids []}
          result (normalize/sort-normalized empty-data [:name :asc])]

      (is (= empty-data result)
        "Should handle empty data gracefully")))

  (testing "sorts numeric values correctly"
    (let [initial {:data {1 {:id 1 :score 85}
                          2 {:id 2 :score 92}
                          3 {:id 3 :score 78}}
                   :ids [1 2 3]}
          result (normalize/sort-normalized initial [:score :asc])]

      (is (= [3 1 2] (:ids result))
        "Should sort numerically: 78, 85, 92"))))

;; Integration test combining multiple operations
(deftest normalization-integration-test

  (testing "complex workflow with multiple operations"

    (let [entities (helpers/generate-test-entities :properties 3)
          normalized (normalize/normalize-entities entities)
          new-entity (helpers/generate-test-entity :properties {:name "Added Entity"})
          with-added (normalize/add-entity normalized new-entity)]

      (is (= 4 (count (:data with-added))) "Should have 4 entities after add")

      ;; Use the actual ID from the first generated entity
      (let [first-entity-id (first (:ids normalized))
            second-entity-id (second (:ids normalized))
            updated-entity (assoc (get-in with-added [:data second-entity-id])
                             :name "Updated Entity" :description "Modified")
            with-updated (normalize/update-entity with-added updated-entity)]

        (is (= "Updated Entity" (get-in with-updated [:data second-entity-id :name]))
          "Should have updated entity")

        (let [with-removed (normalize/remove-entity with-updated first-entity-id)]

          (is (= 3 (count (:data with-removed))) "Should have 3 entities after remove")
          (helpers/assert-entity-not-in-normalized with-removed first-entity-id)

          (let [sorted (normalize/sort-normalized with-removed [:name :asc])
                final-entities (normalize/denormalize-entities sorted)]

            (is (= 3 (count final-entities)) "Should maintain entity count")
            (is (every? #(contains? % :id) final-entities)
              "All entities should have IDs")))))))

;; Performance test for large datasets
(deftest normalization-performance-test

  (testing "handles large dataset efficiently"

    (let [large-dataset (helpers/generate-test-entities :properties 1000)
          start-time (.now js/Date)
          normalized (normalize/normalize-entities large-dataset)
          normalize-time (- (.now js/Date) start-time)
          denorm-start (.now js/Date)
          denormalized (normalize/denormalize-entities normalized)
          denorm-time (- (.now js/Date) denorm-start)]

      (is (= 1000 (count (:data normalized))) "Should normalize all entities")
      (is (= 1000 (count (:ids normalized))) "Should have all IDs")

      (is (= 1000 (count denormalized)) "Should denormalize all entities")

      ;; Performance assertions (generous limits for CI)
      (is (< normalize-time 100) "Normalization should be fast")
      (is (< denorm-time 50) "Denormalization should be fast"))))

;; Run all tests when this file is loaded
(defn run-all-tests []
  (println "Running frontend state normalization tests...")
  (run-tests 'app.template.frontend.state.normalize-test))

;; Export for easy running
(set! js/window.runNormalizeTests run-all-tests)
