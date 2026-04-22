(ns app.admin.frontend.specs.generic-form-entity-specs-test
  (:require
    ;; Ensure subscriptions are registered
    [app.admin.frontend.specs.generic]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db! [db]
  (reset! rf-db/app-db db)
  ;; Ensure we don't reuse cached subscription computations between tests.
  (rf/clear-subscription-cache!))

(deftest form-entity-specs-coerce-jsonish-form-fields
  (testing "Admin form-fields config delivered via JSON (string field ids + string :type) still produces select + boolean specs"
    (reset-db!
      {:admin {:config {:form-fields
                        {:payers {:create-fields ["type" "label" "is-default"]
                                  :edit-fields ["type" "label" "is-default"]
                                  :required-fields ["type" "label"]
                                  ;; Simulate JSON keywordization off: map keys as strings.
                                  :field-config {"type" {"type" "select"
                                                         "options" ["cash" "card" "account" "person"]}
                                                 "label" {"type" "text"}

                                                 "is-default" {"type" "boolean"}}}}}}})

    (let [spec @(rf/subscribe [:form-entity-specs/by-name :payers])
          by-id (into {} (map (juxt :id identity)) spec)]
      (is (= :select (:type (get by-id :type)))
        "Type should be rendered as a select")
      (is (= :boolean (:type (get by-id :is-default)))
        "Is-default should be rendered as a boolean/checkbox")
      (is (= [{:value "cash" :label "Cash"}
              {:value "card" :label "Card"}
              {:value "account" :label "Account"}
              {:value "person" :label "Person"}]
            (:options (get by-id :type)))
        "Select options should be normalized to [{:value :label}] maps")
      (is (true? (:required (get by-id :type)))
        "Required fields should remain required after normalization")
      (is (true? (:required (get by-id :label)))
        "Required fields should remain required after normalization"))))

(deftest form-entity-specs-normalize-snake-case-field-ids
  (testing "Snake_case field ids in form-fields config are normalized to kebab-case app keywords"
    (reset-db!
      {:admin {:config {:form-fields
                        {:articles {:edit-fields ["manufacturer_id" "canonical_name" "unit" "normalized_key"]
                                    :field-config {"manufacturer_id" {"type" "select"
                                                                      "options" ["manufacturers" "display_name"]}
                                                   "canonical_name" {"type" "text"}
                                                   "unit" {"type" "select"
                                                           "options" ["kom" "kg"]}
                                                   "normalized_key" {"type" "text"}}}}}}})

    (let [spec @(rf/subscribe [:form-entity-specs/by-name :articles true])
          ids (set (map :id spec))
          by-id (into {} (map (juxt :id identity)) spec)]
      (is (contains? ids :manufacturer-id))
      (is (contains? ids :canonical-name))
      (is (contains? ids :unit))
      (is (contains? ids :normalized-key))
      (is (= [:manufacturers :display-name]
            (:options (get by-id :manufacturer-id)))
        "Foreign-key options should be normalized to kebab-case keywords")
      (is (= [:kom :kg]
            (:options (get by-id :unit)))
        "Static select options should be normalized to kebab-case keywords"))))

(deftest form-entity-specs-support-batch-edit-mode
  (testing "Batch edit mode prefers :batch-edit-fields and falls back to :edit-fields when absent"
    (reset-db!
      {:admin {:config {:form-fields
                        {:expenses {:create-fields ["total_amount"]
                                    :edit-fields ["supplier_id" "payer_id" "expense_category_id" "notes"]
                                    :batch-edit-fields ["supplier_id" "payer_id" "notes"]
                                    :field-config {"supplier_id" {"type" "select"
                                                                   "options" ["suppliers" "display_name"]}
                                                   "payer_id" {"type" "select"
                                                               "options" ["payers" "label"]}
                                                   "expense_category_id" {"type" "select"
                                                                          "options" ["expense_categories" "name"]}
                                                   "notes" {"type" "textarea"}}}
                         :suppliers {:edit-fields ["display_name"]
                                     :field-config {"display_name" {"type" "text"}}}}}}})

    (let [expense-spec @(rf/subscribe [:form-entity-specs/by-name :expenses :batch-edit])
          supplier-spec @(rf/subscribe [:form-entity-specs/by-name :suppliers :batch-edit])]
      (is (= [:supplier-id :payer-id :notes]
            (mapv :id expense-spec))
        "Explicit batch-edit fields should drive batch edit mode")
      (is (= [:display-name]
            (mapv :id supplier-spec))
        "Entities without :batch-edit-fields should fall back to :edit-fields"))))
