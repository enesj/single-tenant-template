(ns app.template.frontend.components.filter.helpers-test
  "Tests for filter helper functions"
  (:require
    [app.template.frontend.components.filter.helpers :as helpers]
    [app.template.frontend.helpers-test :as test-helpers]
    [cljs.test :refer [are deftest is run-tests testing]]))

(deftest get-field-type-from-spec-test
  (testing "Extracting field type from various spec formats"
    ;; Direct input-type
    (is (= "number" (helpers/get-field-type-from-spec
                      {:field-spec {:input-type "number"}})))

    ;; Type info with input-type
    (is (= "date" (helpers/get-field-type-from-spec
                    {:field-spec {:type "input" :input-type "date"}})))

    ;; Type info without input-type
    (is (= "select" (helpers/get-field-type-from-spec
                      {:field-spec {:type "select"}})))

    ;; Default to text
    (is (= "text" (helpers/get-field-type-from-spec
                    {:field-spec {}})))

    ;; Empty field-spec
    (is (= "text" (helpers/get-field-type-from-spec {})))

    ;; Various input types
    (are [input-type expected] (= expected (helpers/get-field-type-from-spec
                                             {:field-spec {:input-type input-type}}))
      "email" "email"
      "tel" "tel"
      "url" "url"
      "decimal" "decimal"
      "integer" "integer")))

(deftest get-filter-type-test
  (testing "Determining filter type based on field specification"
    ;; Select type - has options
    (is (= :select (helpers/get-filter-type
                     {:field-spec {:input-type "text" :options ["A" "B" "C"]}})))

    ;; Select type - explicit select input
    (is (= :select (helpers/get-filter-type
                     {:field-spec {:input-type "select"}})))

    ;; Number range types
    (are [input-type] (= :number-range (helpers/get-filter-type
                                         {:field-spec {:input-type input-type}}))
      "number"
      "decimal"
      "integer")

    ;; Date range types
    (are [input-type] (= :date-range (helpers/get-filter-type
                                       {:field-spec {:input-type input-type}}))
      "date"
      "datetime"
      "datetime-local")

    ;; Text types (default)
    (are [input-type] (= :text (helpers/get-filter-type
                                 {:field-spec {:input-type input-type}}))
      "text"
      "email"
      "tel"
      "url"
      "search")

    ;; Default to text when no input-type
    (is (= :text (helpers/get-filter-type {:field-spec {}})))))

(deftest get-foreign-key-entity-test
  (testing "Extracting foreign key entity from field options"
    ;; Valid foreign key spec
    (is (= :users (helpers/get-foreign-key-entity
                    {:field-spec {:options [:users :name]}})))

    ;; Another valid foreign key spec
    (is (= :transaction-types (helpers/get-foreign-key-entity
                                {:field-spec {:options [:transaction-types :description]}})))

    ;; Not a foreign key spec - regular options array
    (is (nil? (helpers/get-foreign-key-entity
                {:field-spec {:options ["Option 1" "Option 2"]}})))

    ;; Not a foreign key spec - wrong length
    (is (nil? (helpers/get-foreign-key-entity
                {:field-spec {:options [:users]}})))

    ;; Not a foreign key spec - first element not keyword
    (is (nil? (helpers/get-foreign-key-entity
                {:field-spec {:options ["users" :name]}})))

    ;; No options
    (is (nil? (helpers/get-foreign-key-entity
                {:field-spec {}})))

    ;; Empty field-spec
    (is (nil? (helpers/get-foreign-key-entity {})))))

(deftest parse-field-value-test
  (testing "Parsing field values based on type"
    ;; Number range parsing
    (is (= 123.45 (helpers/parse-field-value
                    {:value "123.45" :field-type :number-range})))
    (is (= 42.0 (helpers/parse-field-value
                  {:value 42 :field-type :number-range})))
    (is (js/isNaN (helpers/parse-field-value
                    {:value "not-a-number" :field-type :number-range})))
    (is (nil? (helpers/parse-field-value
                {:value nil :field-type :number-range})))

    ;; Date range parsing
    (let [date-str "2024-01-15"
          parsed (helpers/parse-field-value
                   {:value date-str :field-type :date-range})]
      (is (instance? js/Date parsed))
      (is (= "2024-01-15" (.slice (.toISOString parsed) 0 10))))

    ;; Date object passthrough
    (let [date-obj (js/Date. "2024-01-15")]
      (is (= date-obj (helpers/parse-field-value
                        {:value date-obj :field-type :date-range}))))

    ;; Invalid date string
    (is (nil? (helpers/parse-field-value
                {:value "invalid-date" :field-type :date-range})))

    ;; Default - return as is
    (is (= "test value" (helpers/parse-field-value
                          {:value "test value" :field-type :text})))
    (is (= {:key "value"} (helpers/parse-field-value
                            {:value {:key "value"} :field-type :select})))))

(deftest matches-filter-test
  (testing "Text filter matching"
    ;; Case insensitive matching
    (is (helpers/matches-filter? {:item {:name "John Doe"}
                                  :field-id :name
                                  :filter-value "john"
                                  :filter-type :text}))

    ;; Partial matching
    (is (helpers/matches-filter? {:item {:description "A long description"}
                                  :field-id :description
                                  :filter-value "long desc"
                                  :filter-type :text}))

    ;; Minimum 3 characters required
    (is (not (helpers/matches-filter? {:item {:name "John"}
                                       :field-id :name
                                       :filter-value "Jo"
                                       :filter-type :text})))

    ;; No match
    (is (not (helpers/matches-filter? {:item {:name "John"}
                                       :field-id :name
                                       :filter-value "Jane"
                                       :filter-type :text})))

    ;; Nil field value
    (is (not (helpers/matches-filter? {:item {:name nil}
                                       :field-id :name
                                       :filter-value "test"
                                       :filter-type :text}))))

  (testing "Number range filter matching"
    ;; Within range
    (is (helpers/matches-filter? {:item {:amount 50}
                                  :field-id :amount
                                  :filter-value {:min 10 :max 100}
                                  :filter-type :number-range}))

    ;; Min only
    (is (helpers/matches-filter? {:item {:amount 50}
                                  :field-id :amount
                                  :filter-value {:min 10}
                                  :filter-type :number-range}))

    ;; Max only
    (is (helpers/matches-filter? {:item {:amount 50}
                                  :field-id :amount
                                  :filter-value {:max 100}
                                  :filter-type :number-range}))

    ;; Outside range
    (is (not (helpers/matches-filter? {:item {:amount 150}
                                       :field-id :amount
                                       :filter-value {:min 10 :max 100}
                                       :filter-type :number-range})))

    ;; String number conversion
    (is (helpers/matches-filter? {:item {:amount "50"}
                                  :field-id :amount
                                  :filter-value {:min 10 :max 100}
                                  :filter-type :number-range})))

  (testing "Date range filter matching"
    (let [date1 (js/Date. "2024-01-15")
          date2 (js/Date. "2024-01-20")
          date3 (js/Date. "2024-01-25")]

      ;; Within range
      (is (helpers/matches-filter? {:item {:date date2}
                                    :field-id :date
                                    :filter-value {:from date1 :to date3}
                                    :filter-type :date-range}))

      ;; From only
      (is (helpers/matches-filter? {:item {:date date2}
                                    :field-id :date
                                    :filter-value {:from date1}
                                    :filter-type :date-range}))

      ;; To only
      (is (helpers/matches-filter? {:item {:date date2}
                                    :field-id :date
                                    :filter-value {:to date3}
                                    :filter-type :date-range}))

      ;; Outside range
      (is (not (helpers/matches-filter? {:item {:date date1}
                                         :field-id :date
                                         :filter-value {:from date2 :to date3}
                                         :filter-type :date-range})))))

  (testing "Select filter matching"
    ;; Simple value in array
    (is (helpers/matches-filter? {:item {:type "income"}
                                  :field-id :type
                                  :filter-value ["income" "expense"]
                                  :filter-type :select}))

    ;; Object format with :value/:label
    (is (helpers/matches-filter? {:item {:type "income"}
                                  :field-id :type
                                  :filter-value [{:value "income" :label "Income"}
                                                 {:value "expense" :label "Expense"}]
                                  :filter-type :select}))

    ;; Not in selection
    (is (not (helpers/matches-filter? {:item {:type "other"}
                                       :field-id :type
                                       :filter-value ["income" "expense"]
                                       :filter-type :select})))

    ;; Nil field value
    (is (not (helpers/matches-filter? {:item {:type nil}
                                       :field-id :type
                                       :filter-value ["income"]
                                       :filter-type :select})))))

(deftest count-matching-items-test
  (testing "Counting items that match filters"
    (let [items [{:id 1 :name "John Smith" :amount 100 :type "income"}
                 {:id 2 :name "Jane Smith" :amount 200 :type "expense"}
                 {:id 3 :name "Jack Johnson" :amount 150 :type "income"}
                 {:id 4 :name "Jill Anderson" :amount 50 :type "expense"}]]

      ;; Text filter - needs 3+ characters
      ;; "Smith" matches both "John Smith" and "Jane Smith"
      (is (= 2 (helpers/count-matching-items {:items items
                                              :field-id :name
                                              :filter-value "Smith"
                                              :filter-type :text})))

      ;; "John" matches both "John Smith" and "Jack Johnson"
      (is (= 2 (helpers/count-matching-items {:items items
                                              :field-id :name
                                              :filter-value "ohn"
                                              :filter-type :text})))

      ;; Test that filter is case insensitive
      (is (= 2 (helpers/count-matching-items {:items items
                                              :field-id :name
                                              :filter-value "smith"
                                              :filter-type :text})))

      ;; Number range filter
      (is (= 3 (helpers/count-matching-items {:items items
                                              :field-id :amount
                                              :filter-value {:min 100}
                                              :filter-type :number-range})))

      ;; Select filter
      (is (= 2 (helpers/count-matching-items {:items items
                                              :field-id :type
                                              :filter-value ["income"]
                                              :filter-type :select})))

      ;; Select filter with object format
      (is (= 2 (helpers/count-matching-items {:items items
                                              :field-id :type
                                              :filter-value [{:value "income" :label "Income"}]
                                              :filter-type :select})))

      ;; No matches
      (is (= 0 (helpers/count-matching-items {:items items
                                              :field-id :name
                                              :filter-value "xyz"
                                              :filter-type :text})))

      ;; Empty items
      (is (= 0 (helpers/count-matching-items {:items []
                                              :field-id :name
                                              :filter-value "test"
                                              :filter-type :text})))

      ;; Nil parameters
      (is (nil? (helpers/count-matching-items {:items nil
                                               :field-id :name
                                               :filter-value "test"
                                               :filter-type :text})))
      (is (nil? (helpers/count-matching-items {:items items
                                               :field-id nil
                                               :filter-value "test"
                                               :filter-type :text})))
      (is (nil? (helpers/count-matching-items {:items items
                                               :field-id :name
                                               :filter-value nil
                                               :filter-type :text}))))))

(deftest format-filter-value-for-display-test
  (testing "Formatting filter values for display"
    ;; Text filter
    (is (= "search term" (helpers/format-filter-value-for-display
                           {:filter-value "search term"
                            :filter-type :text})))

    ;; Number range filter
    (is (= "≥ 10 and ≤ 100" (helpers/format-filter-value-for-display
                              {:filter-value {:min 10 :max 100}
                               :filter-type :number-range})))
    (is (= "≥ 50" (helpers/format-filter-value-for-display
                    {:filter-value {:min 50}
                     :filter-type :number-range})))
    (is (= "≤ 100" (helpers/format-filter-value-for-display
                     {:filter-value {:max 100}
                      :filter-type :number-range})))

    ;; Date range filter
    (let [from-date (js/Date. "2024-01-15")
          to-date (js/Date. "2024-01-25")]
      (is (string? (helpers/format-filter-value-for-display
                     {:filter-value {:from from-date :to to-date}
                      :filter-type :date-range})))
      (is (string? (helpers/format-filter-value-for-display
                     {:filter-value {:from from-date}
                      :filter-type :date-range})))
      (is (string? (helpers/format-filter-value-for-display
                     {:filter-value {:to to-date}
                      :filter-type :date-range}))))

    ;; Select filter with objects
    (is (= "Income, Expense" (helpers/format-filter-value-for-display
                               {:filter-value [{:value "income" :label "Income"}
                                               {:value "expense" :label "Expense"}]
                                :filter-type :select})))

    ;; Select filter with many items
    (is (= "3 selected" (helpers/format-filter-value-for-display
                          {:filter-value [{:value "a" :label "A"}
                                          {:value "b" :label "B"}
                                          {:value "c" :label "C"}]
                           :filter-type :select})))

    ;; Select filter with simple array
    (is (= "2 selected" (helpers/format-filter-value-for-display
                          {:filter-value ["income" "expense"]
                           :filter-type :select})))

    ;; Default formatting
    (is (= "unknown value" (helpers/format-filter-value-for-display
                             {:filter-value "unknown value"
                              :filter-type :unknown})))))

(deftest field-id-coercion-test
  (testing "Field ID coercion in matches-filter?"
    ;; String field-id converted to keyword
    (is (helpers/matches-filter? {:item {:name "Test"}
                                  :field-id "name"
                                  :filter-value "test"
                                  :filter-type :text}))

    ;; Keyword field-id works as is
    (is (helpers/matches-filter? {:item {:name "Test"}
                                  :field-id :name
                                  :filter-value "test"
                                  :filter-type :text}))))

(defn run-all-tests []
  (test-helpers/log-test-start "Filter Helpers Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runFilterHelpersTests run-all-tests)
