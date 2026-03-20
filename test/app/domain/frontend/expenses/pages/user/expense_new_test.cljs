(ns app.domain.frontend.expenses.pages.user.expense-new-test
  (:require
    [app.domain.frontend.expenses.pages.user.expense-new :as expense-new]
    [cljs.test :refer [deftest is testing]]))

(deftest apply-store-selection-auto-fills-supplier
  (testing "selecting a store sets store context and infers supplier"
    (let [context {:supplier nil :store nil :expense-category nil}
          result (expense-new/apply-store-selection
                   context
                   {:id "store-1"
                    :label "Main Store"
                    :supplier_id "sup-1"
                    :supplier_display_name "Supplier One"})]
      (is (= "store-1" (get-in result [:store :id])))
      (is (= "Main Store" (get-in result [:store :label])))
      (is (= "sup-1" (get-in result [:supplier :id])))
      (is (= "Supplier One" (get-in result [:supplier :label]))))))

(deftest apply-supplier-selection-clears-incompatible-store
  (testing "changing supplier clears a previously selected store from another supplier"
    (let [context {:supplier {:id "sup-1" :label "Supplier One"}
                   :store {:id "store-1" :label "Main Store" :supplier_id "sup-1"}
                   :expense-category nil}
          result (expense-new/apply-supplier-selection context {:id "sup-2" :label "Supplier Two"})]
      (is (= "sup-2" (get-in result [:supplier :id])))
      (is (nil? (:store result))))))

(deftest add-article-line-item-fills-blank-row-first
  (testing "selecting an article fills the first blank line item, sets qty to 1, suggests last price, and auto-calculates total"
    (let [items [(expense-new/new-line-item)]
          result (expense-new/add-article-line-item items {:id "article-1"
                                                           :label "Coffee"
                                                           :last_price 4.25})
          item (first result)]
      (is (= 1 (count result)))
      (is (= "Coffee" (:raw_label item)))
      (is (= "1" (:qty item)))
      (is (= "article-1" (:article_id item)))
      (is (= "4.25" (:unit_price item)))
      (is (= "4.25" (:line_total item))))))

(deftest add-article-line-item-appends-after-partially-filled-row
  (testing "selecting an article after a partially filled row appends a fresh row with an initial total"
    (let [items [{:id "line-1"
                  :raw_label ""
                  :qty "2"
                  :unit_price ""
                  :line_total ""}]
          result (expense-new/add-article-line-item items {:id "article-1"
                                                           :label "Coffee"
                                                           :last_price 4.25})
          original-item (first result)
          inserted-item (second result)]
      (is (= 2 (count result)))
      (is (= "2" (:qty original-item)))
      (is (= "" (:raw_label original-item)))
      (is (= "Coffee" (:raw_label inserted-item)))
      (is (= "1" (:qty inserted-item)))
      (is (= "4.25" (:unit_price inserted-item)))
      (is (= "4.25" (:line_total inserted-item))))))

(deftest add-article-line-item-appends-when-current-row-is-used
  (testing "selecting an article appends a new line item when existing rows are already filled"
    (let [items [{:id "line-1"
                  :raw_label "Milk"
                  :qty "1"
                  :unit_price "2.00"
                  :line_total "2.00"}]
          result (expense-new/add-article-line-item items {:id "article-2"
                                                           :label "Paper Towels"
                                                           :last_price 8.15})]
      (is (= 2 (count result)))
      (is (= "Milk" (:raw_label (first result))))
      (is (= "Paper Towels" (:raw_label (second result))))
      (is (= "1" (:qty (second result))))
      (is (= "8.15" (:unit_price (second result)))))))
