(ns app.domain.expenses.services.expenses-test
  "Integration tests for expenses service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc])
  (:import
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- count-table [db table]
  (:count (jdbc/execute-one! db
            (hsql/format {:select [[[:count :*] :count]]
                          :from [table]}))))

(defn- now [] (java.time.Instant/now))

(deftest expenses-price-observation-recorded-when-article-present
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "DM" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article-name (str "Toothpaste-" (UUID/randomUUID))
          article (articles/create-article! db {:canonical_name article-name})
          before (count-table db :price_observations)
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "5.50")
                     :currency "BAM"}
                    [{:raw_label "TP"
                      :article_id (:id article)
                      :line_total (bigdec "5.50")}])
          after (count-table db :price_observations)]
      (is (:id expense))
      (is (= 1 (count (:items expense))))
      (is (= (inc before) after)))))

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
      (is (= (:id article) (:article_id item))))))

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
      (is (nil? (:article_id item))))))

(deftest expenses-auto-linking-does-not-override-explicit-article-id
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
      (is (= (:id article-explicit) (:article_id item))))))

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
      (is (nil? (-> exp-blank :items first :article_id)))
      (is (nil? (-> exp-short :items first :article_id)))
      (is (nil? (-> exp-punct :items first :article_id))))))

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

(deftest expenses-update-upserts-items
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "UpdateItems Supplier" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article-name (str "UpdateItemsArticle-" (UUID/randomUUID))
          article (articles/create-article! db {:canonical_name article-name})
          expense (expenses/create-expense! db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "5.00")
                     :currency "BAM"}
                    [{:raw_label "Old-1" :qty (bigdec "1") :unit_price (bigdec "2.00") :line_total (bigdec "2.00")}
                     {:raw_label "Old-2" :line_total (bigdec "3.00")}])
          [item-1 item-2] (:items expense)
          before (count-table db :price_observations)
          updated (expenses/update-expense! db
                    (:id expense)
                    {:total_amount (bigdec "7.00")
                     :items [{:id (:id item-1)
                              :raw_label "Updated"
                              :qty (bigdec "2")
                              :unit_price (bigdec "2.50")
                              :line_total (bigdec "5.00")}
                             {:raw_label "New"
                              :article_id (:id article)
                              :qty (bigdec "1")
                              :unit_price (bigdec "2.00")
                              :line_total (bigdec "2.00")}]})
          after (count-table db :price_observations)
          items (:items updated)
          updated-item (first (filter #(= (:id item-1) (:id %)) items))
          new-item (first (filter #(= "New" (:raw_label %)) items))]
      (is updated)
      (is (= 2 (count items)))
      (is (nil? (some #(= (:id item-2) (:id %)) items)))
      (is (= "Updated" (:raw_label updated-item)))
      (is (== (bigdec "2") (:qty updated-item)))
      (is (== (bigdec "2.50") (:unit_price updated-item)))
      (is (== (bigdec "5.00") (:line_total updated-item)))
      (is (some? new-item))
      (is (= (:id article) (:article_id new-item)))
      (is (== (bigdec "2.00") (:line_total new-item)))
      (is (= (inc before) after)))))

(deftest expenses-update-auto-links-only-newly-inserted-items
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
          existing-item (-> expense :items first)

          article (articles/create-article! db {:canonical_name (str "Milk-" (UUID/randomUUID))})
          _ (articles/create-alias! db (:id supplier) "MILK" (:id article))

          updated (expenses/update-expense! db
                    (:id expense)
                    {:items [{:id (:id existing-item)
                              :raw_label (:raw_label existing-item)
                              :line_total (:line_total existing-item)}
                             {:raw_label "MILK"
                              :line_total (bigdec "0.00")}]})
          items (:items updated)
          existing-after (first (filter #(= (:id existing-item) (:id %)) items))
          inserted-after (first (filter #(and (= "MILK" (:raw_label %))
                                           (not= (:id existing-item) (:id %)))
                                  items))]
      (is (some? existing-after))
      (is (nil? (:article_id existing-after))
        "Existing items are not retroactively auto-linked on update (follow-up behavior)")

      (is (some? inserted-after))
      (is (= (:id article) (:article_id inserted-after))
        "Newly inserted items are auto-linked when an alias exists"))))

(deftest expenses-soft-delete-excluded-from-list
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
      (expenses/soft-delete-expense! db (:id exp))
      (let [listed (expenses/list-expenses db {:limit 100})]
        (is (empty? (filter #(= (:id exp) (:id %)) listed)))))))

(deftest expenses-soft-delete-soft-deletes-expense-items
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
          count-active (fn []
                         (:count
                          (jdbc/execute-one! db
                            ["select count(*) as count from expense_items where expense_id = ? and deleted_at is null" expense-id])))
          count-deleted (fn []
                          (:count
                           (jdbc/execute-one! db
                             ["select count(*) as count from expense_items where expense_id = ? and deleted_at is not null" expense-id])))
          count-all (fn []
                      (:count
                       (jdbc/execute-one! db
                         ["select count(*) as count from expense_items where expense_id = ?" expense-id])))]
      (is (= 2 (count (:items exp))) "Sanity: expense created with 2 items")
      (is (= 2 (count-active)) "Sanity: 2 active expense_items rows exist")
      (is (= 0 (count-deleted)) "Sanity: no deleted rows before delete")

      (expenses/soft-delete-expense! db expense-id)

      (is (= 0 (count-active)) "Soft-deleting an expense should soft-delete its expense_items")
      (is (= 2 (count-deleted)) "Soft-deleted expense_items remain, but have deleted_at set")
      (is (= 2 (count-all)) "Soft delete should not physically remove expense_items rows"))))
