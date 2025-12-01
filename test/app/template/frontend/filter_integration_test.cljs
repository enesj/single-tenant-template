(ns app.template.frontend.filter-integration-test
  "Tests for end-to-end filtering workflows and integration"
  (:require
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.components.filter.logic :as filter-logic]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.state.normalize :as normalize]
    [app.template.frontend.subs.list :as list-subs]
    [cljs.test :refer [deftest is run-tests testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Register test events only once to avoid re-frame warnings
(defonce filter-test-events-registered
  (do
    ;; Initialize test database
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))

    ;; Set entity data for testing
    (rf/reg-event-db
      ::test-set-entity-data
      (fn [db [_ entity-type entities]]
        (let [normalized (normalize/normalize-entities entities)]
          (-> db
            (assoc-in (paths/entity-data entity-type) (:data normalized))
            (assoc-in (paths/entity-ids entity-type) (:ids normalized))))))

    ;; Apply filter directly to state
    (rf/reg-event-db
      ::test-apply-filter
      (fn [db [_ entity-type field-id filter-value]]
        (assoc-in db (conj (paths/list-ui-state entity-type) :filters field-id) filter-value)))

    ;; Clear filter
    (rf/reg-event-db
      ::test-clear-filter
      (fn [db [_ entity-type field-id]]
        (if field-id
          (update-in db (conj (paths/list-ui-state entity-type) :filters) dissoc field-id)
          (assoc-in db (conj (paths/list-ui-state entity-type) :filters) {}))))

    ;; Set multiple filters at once
    (rf/reg-event-db
      ::test-set-filters
      (fn [db [_ entity-type filters]]
        (assoc-in db (conj (paths/list-ui-state entity-type) :filters) filters)))

    true))

(deftest filter-type-detection-test
  (testing "Filter type determination from field specs"
    ;; Test text field
    (let [text-field {:id "description" :input-type "text"}
          filter-type (filter-helpers/get-filter-type {:field-spec text-field})]
      (is (= :text filter-type) "Should detect text filter type"))

    ;; Test number field
    (let [number-field {:id "amount" :input-type "number"}
          filter-type (filter-helpers/get-filter-type {:field-spec number-field})]
      (is (= :number-range filter-type) "Should detect number-range filter type"))

    ;; Test date field
    (let [date-field {:id "created_at" :input-type "date"}
          filter-type (filter-helpers/get-filter-type {:field-spec date-field})]
      (is (= :date-range filter-type) "Should detect date-range filter type"))

    ;; Test select field with options
    (let [select-field {:id "status" :input-type "select" :options [{:value "active" :label "Active"}]}
          filter-type (filter-helpers/get-filter-type {:field-spec select-field})]
      (is (= :select filter-type) "Should detect select filter type"))

    ;; Test enum field
    (let [enum-field {:id "flow" :options [{:value "income" :label "Income"} {:value "expense" :label "Expense"}]}
          filter-type (filter-helpers/get-filter-type {:field-spec enum-field})]
      (is (= :select filter-type) "Should detect select filter type for enum"))))

(deftest text-filter-test
  (testing "Text filtering functionality"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-items [{:id 1 :description "Apple Product" :amount 100}
                      {:id 2 :description "Banana Snack" :amount 50}
                      {:id 3 :description "Cherry Dessert" :amount 75}
                      {:id 4 :description "apple juice" :amount 25}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-items])

      ;; Test case-insensitive text filter
      (rf/dispatch-sync [::test-apply-filter :items :description "apple"])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 2 (count filtered-items)) "Should match 2 items containing 'apple' (case-insensitive)")
        (is (every? #(re-find #"(?i)apple" (:description %)) filtered-items)
          "All filtered items should contain 'apple'"))

      ;; Test partial match
      (rf/dispatch-sync [::test-apply-filter :items :description "dess"])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 1 (count filtered-items)) "Should match 1 item containing 'dess'")
        (is (= "Cherry Dessert" (:description (first filtered-items)))
          "Should match Cherry Dessert"))

      ;; Test no matches
      (rf/dispatch-sync [::test-apply-filter :items :description "xyz"])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 0 (count filtered-items)) "Should match 0 items for non-existent text"))

      ;; Test clearing filter
      (rf/dispatch-sync [::test-clear-filter :items :description])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 4 (count filtered-items)) "Should show all items when filter is cleared")))))

(deftest number-range-filter-test
  (testing "Number range filtering functionality"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-items [{:id 1 :description "Item 1" :amount 10}
                      {:id 2 :description "Item 2" :amount 25}
                      {:id 3 :description "Item 3" :amount 50}
                      {:id 4 :description "Item 4" :amount 75}
                      {:id 5 :description "Item 5" :amount 100}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-items])

      ;; Test minimum value filter
      (rf/dispatch-sync [::test-apply-filter :items :amount {:min 50}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 3 (count filtered-items)) "Should match 3 items with amount >= 50")
        (is (every? #(>= (:amount %) 50) filtered-items)
          "All filtered items should have amount >= 50"))

      ;; Test maximum value filter
      (rf/dispatch-sync [::test-apply-filter :items :amount {:max 50}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 3 (count filtered-items)) "Should match 3 items with amount <= 50")
        (is (every? #(<= (:amount %) 50) filtered-items)
          "All filtered items should have amount <= 50"))

      ;; Test range filter (min and max)
      (rf/dispatch-sync [::test-apply-filter :items :amount {:min 25 :max 75}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 3 (count filtered-items)) "Should match 3 items with amount between 25 and 75")
        (is (every? #(and (>= (:amount %) 25) (<= (:amount %) 75)) filtered-items)
          "All filtered items should have amount between 25 and 75"))

      ;; Test exact value (min = max)
      (rf/dispatch-sync [::test-apply-filter :items :amount {:min 50 :max 50}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 1 (count filtered-items)) "Should match 1 item with amount = 50")
        (is (= 50 (:amount (first filtered-items))) "Should match item with amount exactly 50")))))

(deftest date-range-filter-test
  (testing "Date range filtering functionality"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data with dates
    (let [date1 (js/Date. "2024-01-15")
          date2 (js/Date. "2024-02-15")
          date3 (js/Date. "2024-03-15")
          date4 (js/Date. "2024-04-15")
          test-items [{:id 1 :description "Item 1" :created_at date1}
                      {:id 2 :description "Item 2" :created_at date2}
                      {:id 3 :description "Item 3" :created_at date3}
                      {:id 4 :description "Item 4" :created_at date4}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-items])

      ;; Test from date filter
      (rf/dispatch-sync [::test-apply-filter :items :created_at {:from date2}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 3 (count filtered-items)) "Should match 3 items with date >= Feb 15")
        (is (every? #(>= (.getTime (:created_at %)) (.getTime date2)) filtered-items)
          "All filtered items should have date >= Feb 15"))

      ;; Test to date filter
      (rf/dispatch-sync [::test-apply-filter :items :created_at {:to date3}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 3 (count filtered-items)) "Should match 3 items with date <= Mar 15")
        (is (every? #(<= (.getTime (:created_at %)) (.getTime date3)) filtered-items)
          "All filtered items should have date <= Mar 15"))

      ;; Test date range filter
      (rf/dispatch-sync [::test-apply-filter :items :created_at {:from date2 :to date3}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 2 (count filtered-items)) "Should match 2 items between Feb 15 and Mar 15")
        (is (every? #(and (>= (.getTime (:created_at %)) (.getTime date2))
                       (<= (.getTime (:created_at %)) (.getTime date3))) filtered-items)
          "All filtered items should have date between Feb 15 and Mar 15")))))

(deftest select-filter-test
  (testing "Select/multi-select filtering functionality"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-items [{:id 1 :description "Item 1" :status "active" :category "A"}
                      {:id 2 :description "Item 2" :status "inactive" :category "B"}
                      {:id 3 :description "Item 3" :status "active" :category "A"}
                      {:id 4 :description "Item 4" :status "pending" :category "C"}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-items])

      ;; Test single value select filter
      (rf/dispatch-sync [::test-apply-filter :items :status [{:value "active" :label "Active"}]])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 2 (count filtered-items)) "Should match 2 items with status 'active'")
        (is (every? #(= "active" (:status %)) filtered-items)
          "All filtered items should have status 'active'"))

      ;; Test multiple value select filter
      (rf/dispatch-sync [::test-apply-filter :items :status
                         [{:value "active" :label "Active"}
                          {:value "pending" :label "Pending"}]])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 3 (count filtered-items)) "Should match 3 items with status 'active' or 'pending'")
        (is (every? #(contains? #{"active" "pending"} (:status %)) filtered-items)
          "All filtered items should have status 'active' or 'pending'"))

      ;; Test empty selection (should show all items)
      (rf/dispatch-sync [::test-apply-filter :items :status []])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 4 (count filtered-items)) "Should show all items when no options selected")))))

(deftest multiple-filters-test
  (testing "Multiple filters working together (AND logic)"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-items [{:id 1 :description "Apple Product" :amount 100 :status "active"}
                      {:id 2 :description "Apple Juice" :amount 25 :status "inactive"}
                      {:id 3 :description "Orange Product" :amount 150 :status "active"}
                      {:id 4 :description "Apple Sauce" :amount 75 :status "active"}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-items])

      ;; Apply multiple filters: text + status
      (rf/dispatch-sync [::test-set-filters :items
                         {:description "apple"
                          :status [{:value "active" :label "Active"}]}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 2 (count filtered-items)) "Should match 2 items with 'apple' AND status 'active'")
        (is (every? #(and (re-find #"(?i)apple" (:description %))
                       (= "active" (:status %))) filtered-items)
          "All items should match both text and status filters"))

      ;; Add amount filter to existing filters
      (rf/dispatch-sync [::test-set-filters :items
                         {:description "apple"
                          :status [{:value "active" :label "Active"}]
                          :amount {:min 75}}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 2 (count filtered-items)) "Should match items with all three filters")
        (is (every? #(and (re-find #"(?i)apple" (:description %))
                       (= "active" (:status %))
                       (>= (:amount %) 75)) filtered-items)
          "All items should match text, status, and amount filters"))

      ;; Make amount filter more restrictive
      (rf/dispatch-sync [::test-set-filters :items
                         {:description "apple"
                          :status [{:value "active" :label "Active"}]
                          :amount {:min 100}}])
      (let [filtered-items @(rf/subscribe [::list-subs/filtered-items :items])]
        (is (= 1 (count filtered-items)) "Should match 1 item with all restrictive filters")
        (is (= "Apple Product" (:description (first filtered-items)))
          "Should match only Apple Product")))))

(deftest filter-state-management-test
  (testing "Filter state persistence and management"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test getting active filters
    (rf/dispatch-sync [::test-apply-filter :items :description "test"])
    (let [active-filters @(rf/subscribe [::list-subs/active-filters :items])]
      (is (= {:description "test"} active-filters)
        "Should store and retrieve active filters"))

    ;; Test adding multiple filters
    (rf/dispatch-sync [::test-apply-filter :items :amount {:min 50}])
    (let [active-filters @(rf/subscribe [::list-subs/active-filters :items])]
      (is (= {:description "test" :amount {:min 50}} active-filters)
        "Should accumulate multiple filters"))

    ;; Test clearing specific filter
    (rf/dispatch-sync [::test-clear-filter :items :description])
    (let [active-filters @(rf/subscribe [::list-subs/active-filters :items])]
      (is (= {:amount {:min 50}} active-filters)
        "Should clear specific filter while keeping others"))

    ;; Test clearing all filters
    (rf/dispatch-sync [::test-clear-filter :items nil])
    (let [active-filters @(rf/subscribe [::list-subs/active-filters :items])]
      (is (= {} active-filters)
        "Should clear all filters"))))

(deftest filter-matching-logic-test
  (testing "Filter matching logic edge cases"
    ;; Test matches-filter? function directly for various scenarios

    ;; Text filter - minimum character requirement
    (is (not (filter-helpers/matches-filter?
               {:item {:description "test"} :field-id :description :filter-value "te" :filter-type :text}))
      "Should not match text filter with less than 3 characters")

    (is (filter-helpers/matches-filter?
          {:item {:description "test"} :field-id :description :filter-value "tes" :filter-type :text})
      "Should match text filter with 3 or more characters")

    ;; Number range filter with string values
    (is (filter-helpers/matches-filter?
          {:item {:amount "100"} :field-id :amount :filter-value {:min 50 :max 150} :filter-type :number-range})
      "Should handle string numeric values in number range filter")

    ;; Date filter with string dates
    (let [filter-date (js/Date. "2024-01-01")]
      (is (filter-helpers/matches-filter?
            {:item {:created_at "2024-01-15"} :field-id :created_at :filter-value {:from filter-date} :filter-type :date-range})
        "Should handle string dates in date range filter"))

    ;; Select filter with various value formats
    (is (filter-helpers/matches-filter?
          {:item {:status "active"} :field-id :status :filter-value [{:value "active" :label "Active"}] :filter-type :select})
      "Should match select filter with value/label format")

    ;; Nil/empty value handling
    (is (not (filter-helpers/matches-filter?
               {:item {:description nil} :field-id :description :filter-value "test" :filter-type :text}))
      "Should not match when field value is nil")

    (is (not (filter-helpers/matches-filter?
               {:item {} :field-id :description :filter-value "test" :filter-type :text}))
      "Should not match when field is missing")))

(deftest filter-helpers-test
  (testing "Filter helper functions"
    ;; Test field type extraction
    (is (= "text" (filter-helpers/get-field-type-from-spec {:field-spec {:input-type "text"}}))
      "Should extract field type from spec")

    ;; Test value parsing
    (is (= 123 (filter-helpers/parse-field-value {:value "123" :field-type :number-range}))
      "Should parse string number to number")

    (let [date-obj (filter-helpers/parse-field-value {:value "2024-01-15" :field-type :date-range})]
      (is (instance? js/Date date-obj)
        "Should parse string date to Date object"))))

(deftest filter-initialization-test
  (testing "Filter state initialization logic"
    ;; Test number range initialization
    (let [entities [{:amount 10} {:amount 50} {:amount 100}]
          field-spec {:id "amount"}
          initial-state (filter-logic/initialize-filter-state
                          {:initial-value nil
                           :filter-type :number-range
                           :entity-type :items
                           :field-spec field-spec
                           :all-entities entities})]
      (is (= 10 (:filter-min initial-state))
        "Should set min to minimum value from entities")
      (is (= 100 (:filter-max initial-state))
        "Should set max to maximum value from entities"))

    ;; Test select options initialization
    (let [initial-value [{:value "active" :label "Active"}]
          initial-state (filter-logic/initialize-filter-state
                          {:initial-value initial-value
                           :filter-type :select
                           :entity-type :items
                           :field-spec {}
                           :all-entities []
                           :available-options initial-value})]
      (is (= [{:value "active" :label "Active"}] (:filter-selected-options initial-state))
        "Should initialize select options correctly"))))

(defn run-all-tests []
  (helpers/log-test-start "Filter Integration Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runFilterIntegrationTests run-all-tests)
