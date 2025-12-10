(ns app.domain.expenses.services.expenses-services-test
  "Integration tests for Home Expenses domain services."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.expenses.services.articles :as articles]
    [app.domain.expenses.services.expenses :as expenses]
    [app.domain.expenses.services.payers :as payers]
    [app.domain.expenses.services.receipts :as receipts]
    [app.domain.expenses.services.suppliers :as suppliers]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc])
  (:import
    (java.util UUID)))

;; Use transactional fixture so each test rolls back changes.
(use-fixtures :each fixtures/with-transaction-rollback)

(defn- count-table [db table]
  (:count (jdbc/execute-one! db
            (hsql/format {:select [[[:count :*] :count]]
                          :from [table]}))))

(defn- now [] (java.time.Instant/now))

(deftest suppliers-normalization-and-dedupe-test
  (testing "normalize-supplier-key trims, lowercases, strips punctuation"
    (is (= "dm-drogerie" (suppliers/normalize-supplier-key "  DM Drogerie!  "))))
  (testing "find-or-create-supplier! dedupes by normalized key"
    (when-let [db fixtures/*test-db*]
      (let [first (suppliers/find-or-create-supplier! db "Bingo Centar" {:address "Main"})
            second (suppliers/find-or-create-supplier! db "  bingo-centar " {})]
        (is (= (:id first) (:id second)))))))

(deftest payers-default-per-type-test
  (when-let [db fixtures/*test-db*]
    (let [p1 (payers/create-payer! db {:type "cash" :label "Cash Wallet" :is_default true})
          p2 (payers/create-payer! db {:type "cash" :label "Cash Jar" :is_default false})
          _ (payers/set-default-payer! db (:id p2))
          p1* (payers/get-payer db (:id p1))
          p2* (payers/get-payer db (:id p2))
          default (payers/get-default-payer db)]
      (is (false? (:is_default p1*)))
      (is (true? (:is_default p2*)))
      (is (= (:id p2) (:id default))))))

(deftest receipts-approve-creates-expense-and-links
  (when-let [db fixtures/*test-db*]
    (let [supplier (suppliers/find-or-create-supplier! db "Konzum" {})
          payer (payers/create-payer! db {:type "card" :label "Visa" :last4 "1234"})
          upload (receipts/upload-receipt! db {:storage_key "s3://bucket/r1.jpg"
                                               :bytes (.getBytes "hello world")})
          receipt-id (:id (:receipt upload))
          _ (receipts/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "12.34")
                  :currency "BAM"
                  :items [{:raw_label "Milk" :line_total (bigdec "12.34")}]}
          expense (receipts/approve-and-post! db receipt-id review)
          stored (receipts/get-receipt db receipt-id)]
      (is (:id expense))
      (is (= "posted" (:status stored)))
      (is (= (:id expense) (:expense_id stored)))
      (is (= 1 (count (:items expense)))))))

(deftest expenses-price-observation-recorded-when-article-present
  (when-let [db fixtures/*test-db*]
    (let [supplier (suppliers/find-or-create-supplier! db "DM" {})
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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

(deftest expenses-soft-delete-excluded-from-list
  (when-let [db fixtures/*test-db*]
    (let [supplier (suppliers/find-or-create-supplier! db "Pharmacy" {})
          payer (payers/create-payer! db {:type "account" :label "Bank"})
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
