(ns app.domain.expenses.services.expenses-test
  "Integration tests for expenses service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.expenses :as expenses]

    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is use-fixtures]]
    [next.jdbc :as jdbc])
  (:import
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (java.time.Instant/now))

(deftest expenses-auto-links-article-when-alias-exists
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "Walmart" {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article-name (str "Gala Apples-" (UUID/randomUUID))
          article (articles/create-article! db {:canonical_name article-name})
          _ (articles/create-alias! db (:id supplier) "APPLES G" (:id article))
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "1.00")
                     :currency "BAM"}
                    [{:raw_label "APPLES G"
                      :line_total (bigdec "1.00")}])
          item (-> expense :items first)]
      (is (= "APPLES G" (:raw_label item)))
      (is (= article-name (:article_canonical_name item))))))

(deftest expenses-auto-linking-is-supplier-scoped
  (when-let [db fixtures/*test-db*]
    (let [supplier-a (:supplier (suppliers/find-or-create-supplier! db "Supplier A" {}))
          supplier-b (:supplier (suppliers/find-or-create-supplier! db "Supplier B" {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article-a (articles/create-article! db {:canonical_name (str "ArticleA-" (UUID/randomUUID))})
          _ (articles/create-alias! db (:id supplier-a) "MILK" (:id article-a))
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier-b)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "2.00")
                     :currency "BAM"}
                    [{:raw_label "MILK"
                      :line_total (bigdec "2.00")}])
          item (-> expense :items first)]
      (is (= "MILK" (:raw_label item)))
      (is (nil? (:article_canonical_name item))))))

(deftest expenses-alias-mapping-wins-over-explicit-article-id
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "Override Supplier" {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article-aliased (articles/create-article! db {:canonical_name (str "Aliased-" (UUID/randomUUID))})
          article-explicit (articles/create-article! db {:canonical_name (str "Explicit-" (UUID/randomUUID))})
          _ (articles/create-alias! db (:id supplier) "TP" (:id article-aliased))
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "3.00")
                     :currency "BAM"}
                    [{:raw_label "TP"
                      :article_id (:id article-explicit)
                      :line_total (bigdec "3.00")}])
          item (-> expense :items first)]
      (is (= (:canonical_name article-aliased) (:article_canonical_name item)))
      (is (not= (:canonical_name article-explicit) (:article_canonical_name item))))))

(deftest expenses-auto-linking-skips-blank-or-short-labels
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "BlankLabel Supplier" {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article (articles/create-article! db {:canonical_name (str "ShouldNotMatch-" (UUID/randomUUID))})
          _ (articles/create-alias! db (:id supplier) "  " (:id article))

          exp-blank (expenses/create-expense! db
                      {:supplier_id (:id supplier)
                       :payer_id (:id payer)
                       :purchased_at (now)
                       :total_amount (bigdec "1.00")
                       :currency "BAM"}
                      [{:raw_label "  " :line_total (bigdec "1.00")}])
          exp-short (expenses/create-expense! db
                      {:supplier_id (:id supplier)
                       :payer_id (:id payer)
                       :purchased_at (now)
                       :total_amount (bigdec "1.00")
                       :currency "BAM"}
                      [{:raw_label "A" :line_total (bigdec "1.00")}])
          exp-punct (expenses/create-expense! db
                      {:supplier_id (:id supplier)
                       :payer_id (:id payer)
                       :purchased_at (now)
                       :total_amount (bigdec "1.00")
                       :currency "BAM"}
                      [{:raw_label "##" :line_total (bigdec "1.00")}])]
      (is (nil? (-> exp-blank :items first :alias_id)))
      (is (nil? (-> exp-short :items first :alias_id)))
      (is (nil? (-> exp-punct :items first :alias_id))))))

(deftest expenses-create-accepts-body-with-items-two-arity
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "TwoArity Supplier" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "1.23")
                     :currency "BAM"
                     :notes "two arity"
                     :items [{:raw_label "Test" :line_total (bigdec "1.23")}]})]
      (is (:id expense))
      (is (= 1 (count (:items expense))))
      (is (= "Test" (-> expense :items first :raw_label))))))

(deftest expenses-create-coerces-api-string-types
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "Coerce Supplier" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article-name (str "CoerceArticle-" (UUID/randomUUID))
          article (articles/create-article! db {:canonical_name article-name})
          expense (expenses/create-expense! db
                    {:supplier_id (str (:id supplier))
                     :payer_id (str (:id payer))
                     :purchased_at "2025-01-02T03:04"
                     :total_amount "2.34"
                     :currency "BAM"
                     :is_posted "false"
                     :items [{:raw_label "Item"
                              :article_id (str (:id article))
                              :qty "1"
                              :unit_price "2.34"
                              :line_total "2.34"}]})]
      (is (:id expense))
      (is (= false (:is_posted expense)))
      (is (= 1 (count (:items expense))))
      (is (= "Item" (-> expense :items first :raw_label))))))

(deftest expenses-create-repairs-inconsistent-unit-price-from-line-total
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "Repair Supplier" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "19.80")
                     :currency "BAM"}
                    [{:raw_label "CIG DUNHILL ESSENCE BRONZE"
                      :qty "1"
                      :unit_price "3.00"
                      :line_total "19.80"}])
          item (-> expense :items first)]
      (is (= 1M (bigdec (:qty item))))
      (is (= 19.80M (bigdec (:unit_price item))))
      (is (= 19.80M (bigdec (:line_total item)))))))

(deftest expenses-update-repairs-inconsistent-unit-price-from-line-total
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "Update Repair Supplier " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          created (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "3.00")
                     :currency "BAM"}
                    [{:raw_label "CIG DUNHILL ESSENCE BRONZE"
                      :qty (bigdec "1")
                      :unit_price (bigdec "3.00")
                      :line_total (bigdec "3.00")}])
          item-id (-> created :items first :id)
          updated (expenses/update-expense! db
                    (:id created)
                    {:supplier_id (str (:id supplier))
                     :payer_id (str (:id payer))
                     :purchased_at "2026-01-10T12:01"
                     :total_amount 19.8
                     :currency "BAM"
                     :items [{:id (str item-id)
                              :raw_label "CIG DUNHILL ESSENCE BRONZE"
                              :qty 1
                              :unit_price 3.0
                              :line_total 19.8}]})
          item (-> updated :items first)]
      (is (= item-id (:id item)))
      (is (= 1M (bigdec (:qty item))))
      (is (= 19.80M (bigdec (:unit_price item))))
      (is (= 19.80M (bigdec (:line_total item)))))))

(deftest expenses-update-keeps-items-with-valid-not-in-sql
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "Update Expense Supplier " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          created (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "10.00")
                     :currency "EUR"}
                    [{:raw_label "A" :line_total (bigdec "6.00") :qty (bigdec "1") :unit_price (bigdec "6.00")}
                     {:raw_label "B" :line_total (bigdec "4.00") :qty (bigdec "2") :unit_price (bigdec "2.00")}])
          expense-id (:id created)
          item-ids (mapv :id (:items created))

          ;; Mimic the user UI update payload shape (JSON numbers, ids as strings, datetime-local string).
          body {:supplier_id (str (:id supplier))
                :payer_id (str (:id payer))
                :purchased_at "2026-01-10T12:01"
                :currency "EUR"
                :notes "updated"
                :total_amount 10.0
                :items [{:id (str (nth item-ids 0))
                         :raw_label "A"
                         :qty 1
                         :unit_price 6.0
                         :line_total 6.0}
                        {:id (str (nth item-ids 1))
                         :raw_label "B"
                         :qty 2
                         :unit_price 2.0
                         :line_total 4.0}]}
          updated (expenses/update-expense! db expense-id body)]
      (is updated)
      (is (= expense-id (:id updated)))
      (is (= 2 (count (:items updated))))
      ;; Ensure existing item ids are preserved and nothing is deleted.
      (is (= (set item-ids) (set (map :id (:items updated))))))))

(deftest expenses-alias-mapping-affects-existing-and-new-items
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "UpdateAutoLink Supplier " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})

          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "2.00")
                     :currency "BAM"}
                    [{:raw_label "MILK"
                      :line_total (bigdec "2.00")}])
          item-before (-> expense :items first)
          article (articles/create-article! db {:canonical_name (str "Milk-" (UUID/randomUUID))})
          _ (articles/create-alias! db (:id supplier) "MILK" (:id article))
          expense-after (expenses/get-expense-with-items db (:id expense))
          item-after (-> expense-after :items first)
          expense-new (expenses/create-expense! db
                        {:supplier_id (:id supplier)
                         :payer_id (:id payer)
                         :purchased_at (now)
                         :total_amount (bigdec "0.00")
                         :currency "BAM"}
                        [{:raw_label "MILK" :line_total (bigdec "0.00")}])
          item-new (-> expense-new :items first)]
      (is (nil? (:article_canonical_name item-before)))
      (is (= (:canonical_name article) (:article_canonical_name item-after)))
      (is (= (:canonical_name article) (:article_canonical_name item-new))))))

(deftest expenses-delete-removes-from-list
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "Pharmacy" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "account" :label "Bank"})
          exp (expenses/create-expense! db
                {:supplier_id (:id supplier)
                 :payer_id (:id payer)
                 :purchased_at (now)
                 :total_amount (bigdec "9.99")
                 :currency "BAM"}
                [{:raw_label "Meds" :line_total (bigdec "9.99")}])]
      (expenses/delete-expense! db (:id exp))
      (let [listed (expenses/list-expenses db {:limit 100})]
        (is (empty? (filter #(= (:id exp) (:id %)) listed)))))))

(deftest expenses-delete-cascades-expense-items
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "DeleteExpenseItems Supplier " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          exp (expenses/create-expense! db
                {:supplier_id (:id supplier)
                 :payer_id (:id payer)
                 :purchased_at (now)
                 :total_amount (bigdec "3.00")
                 :currency "BAM"}
                [{:raw_label "Item 1" :line_total (bigdec "1.00")}
                 {:raw_label "Item 2" :line_total (bigdec "2.00")}])
          expense-id (:id exp)
          count-items (fn []
                        (:count
                         (jdbc/execute-one! db
                           ["select count(*) as count from expense_items where expense_id = ?" expense-id])))]
      (is (= 2 (count (:items exp))) "Sanity: expense created with 2 items")
      (is (= 2 (count-items)) "Sanity: 2 expense_items rows exist")

      (expenses/delete-expense! db expense-id)

      (is (= 0 (count-items)) "Deleting an expense should delete its expense_items"))))
