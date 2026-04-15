(ns app.domain.frontend.expenses.shared.manual-entry.core-test
  (:require
    [app.domain.frontend.expenses.shared.manual-entry.core :as sut]
    [cljs.test :refer [deftest is testing]]))

(deftest default-category-chip-preselection-only-runs-when-eligible
  (let [expense-categories [{:id "cat-1" :name "Household" :is-default true}
                            {:id "cat-2" :name "Food"}]]
    (testing "returns the default category chip when enabled and nothing is selected"
      (is (= {:id "cat-1" :label "Household"}
            (sut/default-category-chip-to-preselect expense-categories nil true nil))))
    (testing "stops preselecting after the user has already chosen a category"
      (is (nil? (sut/default-category-chip-to-preselect
                  expense-categories
                  nil
                  true
                  {:id "cat-2" :label "Food"}))))
    (testing "does not preselect when the one-shot flag has been disabled"
      (is (nil? (sut/default-category-chip-to-preselect expense-categories nil false nil))))))

(deftest prepare-submit-values-normalizes-items-context-and-notes
  (let [payload (sut/prepare-submit-values
                  {:items [{:label "Milk" :qty "2" :unit-price "3.50"}
                           {:label "" :qty "1" :unit-price "9.99"}
                           {:label "Free sample" :qty "1" :unit-price "0"}]
                   :context {:supplier {:id "sup-1" :label "Bingo"}
                             :category {:id "cat-1" :label "Household"}}
                   :currency "BAM"
                   :purchased-at "2026-04-14T09:30"
                   :payer-id "payer-1"
                   :notes "  "})]
    (testing "keeps only valid line items and computes the total"
      (is (= [{:raw_label "Milk"
               :qty 2
               :unit_price 3.5
               :line_total 7.0}]
            (:items payload)))
      (is (= 7.0 (:total_amount payload))))
    (testing "serializes selected context ids and omits blank notes"
      (is (= "sup-1" (:supplier_id payload)))
      (is (= "cat-1" (:expense_category_id payload)))
      (is (nil? (:notes payload))))))

(deftest ensure-unknown-context-fills-missing-supplier-and-store
  (testing "injects shared unknown entities only where context is missing"
    (is (= {:supplier {:id sut/unknown-supplier-id :label "Unknown supplier"}
            :store {:id sut/unknown-store-id :label "Unknown store"}}
          (sut/ensure-unknown-context {})))
    (is (= {:category {:id "cat-1" :label "Household"}
            :supplier {:id sut/unknown-supplier-id :label "Unknown supplier"}
            :store {:id sut/unknown-store-id :label "Unknown store"}}
          (sut/ensure-unknown-context {:category {:id "cat-1" :label "Household"}})))
    (is (= {:supplier {:id "sup-1" :label "Known supplier"}
            :store {:id sut/unknown-store-id :label "Fallback store"}}
          (sut/ensure-unknown-context
            {:supplier {:id "sup-1" :label "Known supplier"}}
            {:store-label "Fallback store"})))))

(deftest payer-default-id-prefers-explicit-default-and-falls-back-to-first
  (testing "prefers the explicit default payer flag when present"
    (is (= "payer-2"
          (sut/payer-default-id [{:id "payer-1"}
                                 {:id "payer-2" :is-default true}]))))
  (testing "falls back to the first payer when no default is flagged"
    (is (= "payer-1"
          (sut/payer-default-id [{:id "payer-1"}
                                 {:id "payer-2"}])))))