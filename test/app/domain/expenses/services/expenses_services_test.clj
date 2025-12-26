(ns app.domain.expenses.services.expenses-services-test
  "Integration tests for Home Expenses domain services."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc])
  (:import
    (java.nio.file Files)
    (java.util UUID)))

;; Use transactional fixture so each test rolls back changes.
(use-fixtures :each fixtures/with-transaction-rollback)

(defn- count-table [db table]
  (:count (jdbc/execute-one! db
            (hsql/format {:select [[[:count :*] :count]]
                          :from [table]}))))

(defn- now [] (java.time.Instant/now))

(defn- create-test-user!
  "Insert a minimal user row and return its UUID id.

  Keeps integration tests independent from auth/user services."
  [db role]
  (let [user-id (UUID/randomUUID)
        t (now)
        email (str "user-" (UUID/randomUUID) "@test.com")]
    (jdbc/execute!
      db
      (hsql/format {:insert-into :users
                    :values [{:id user-id
                              :email email
                              :full_name "Test User"
                              :password_hash "test-hash-not-real"
                              :role [:cast (or role "member") :user_role]
                              :status [:cast "active" :user_status]
                              :email_verified false
                              :auth_provider "password"
                              :created_at t
                              :updated_at t}]}))
    user-id))

(deftest suppliers-normalization-and-dedupe-test
  (testing "normalize-supplier-key trims, lowercases, strips punctuation"
    (is (= "dm-drogerie" (suppliers/normalize-supplier-key "  DM Drogerie!  "))))
  (testing "find-or-create-supplier! dedupes by normalized key"
    (when-let [db fixtures/*test-db*]
      (let [first (suppliers/find-or-create-supplier! db "Bingo Centar" {:address "Main"})
            second (suppliers/find-or-create-supplier! db "  bingo-centar " {})]
        (is (= (:id (:supplier first)) (:id (:supplier second))))))))

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
    (let [supplier-result (suppliers/find-or-create-supplier! db "Konzum" {})
          supplier (:supplier supplier-result)
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

(deftest receipts-approve-moves-local-receipt-file-to-exported
  (when-let [db fixtures/*test-db*]
    (let [base-dir (io/file "upload" "stripes")
          _ (.mkdirs base-dir)
          storage-key (str (UUID/randomUUID) ".png")
          bytes (.getBytes (str "receipt-" (UUID/randomUUID)))
          src-file (io/file base-dir storage-key)
          dest-file (io/file base-dir "exported" storage-key)]
      (try
        (Files/write (.toPath src-file) bytes (into-array java.nio.file.OpenOption []))
        (let [supplier (:supplier (suppliers/find-or-create-supplier! db "Konzum" {}))
              payer (payers/create-payer! db {:type "card" :label "Visa" :last4 "1234"})
              upload (receipts/upload-receipt! db {:storage_key storage-key
                                                   :bytes bytes})
              receipt-id (:id (:receipt upload))
              _ (receipts/update-status! db receipt-id "extracted")
              review {:supplier_id (:id supplier)
                      :payer_id (:id payer)
                      :purchased_at (now)
                      :total_amount (bigdec "12.34")
                      :currency "BAM"
                      :items [{:raw_label "Milk" :line_total (bigdec "12.34")}]}
              _expense (receipts/approve-and-post! db receipt-id review)
              stored (receipts/get-receipt db receipt-id)]
          (is (= (str "exported/" storage-key) (:storage_key stored)))
          (is (false? (.exists src-file)))
          (is (true? (.exists dest-file))))
        (finally
          (try
            (Files/deleteIfExists (.toPath src-file))
            (catch Exception _ nil))
          (try
            (Files/deleteIfExists (.toPath dest-file))
            (catch Exception _ nil)))))))

(deftest receipts-approve-for-user-sets-expense-user-id-and-scopes
  (when-let [db fixtures/*test-db*]
    (let [user-1 (create-test-user! db "member")
          user-2 (create-test-user! db "member")
          supplier-result (suppliers/find-or-create-supplier! db "UserReceipt Supplier" {})
          supplier (:supplier supplier-result)
          payer (payers/create-payer! db {:type "cash" :label "Cash"})

          upload-owned (receipts/upload-receipt! db {:user_id user-1
                                                     :storage_key (str "s3://bucket/u1-" (UUID/randomUUID) ".jpg")
                                                     :bytes (.getBytes (str "u1-" (UUID/randomUUID)))})
          receipt-owned (:id (:receipt upload-owned))
          _ (receipts/update-status! db receipt-owned "extracted")

          upload-unassigned (receipts/upload-receipt! db {:storage_key (str "s3://bucket/unassigned-" (UUID/randomUUID) ".jpg")
                                                          :bytes (.getBytes (str "unassigned-" (UUID/randomUUID)))})
          receipt-unassigned (:id (:receipt upload-unassigned))
          _ (receipts/update-status! db receipt-unassigned "extracted")

          upload-other (receipts/upload-receipt! db {:user_id user-2
                                                     :storage_key (str "s3://bucket/u2-" (UUID/randomUUID) ".jpg")
                                                     :bytes (.getBytes (str "u2-" (UUID/randomUUID)))})
          receipt-other (:id (:receipt upload-other))

          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "7.77")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "7.77")}]}

          expense-owned (receipts/approve-and-post-for-user! db user-1 receipt-owned review)
          stored-owned (receipts/get-receipt db receipt-owned)

          expense-unassigned (receipts/approve-and-post-for-user! db user-1 receipt-unassigned review)
          stored-unassigned (receipts/get-receipt db receipt-unassigned)

          scoped (receipts/list-user-receipts db user-1 {:limit 200})
          scoped-ids (set (map :id scoped))]
      (is (= user-1 (:user_id expense-owned)))
      (is (= "posted" (:status stored-owned)))
      (is (= (:id expense-owned) (:expense_id stored-owned)))

      (is (= user-1 (:user_id expense-unassigned)))
      (is (= "posted" (:status stored-unassigned)))
      (is (= user-1 (:user_id stored-unassigned)))
      (is (= (:id expense-unassigned) (:expense_id stored-unassigned)))

      ;; list-user-receipts returns owned + unassigned
      (is (contains? scoped-ids receipt-owned))
      (is (contains? scoped-ids receipt-unassigned))
      (is (not (contains? scoped-ids receipt-other)))

      ;; Cannot approve a receipt owned by a different user
      (try
        (receipts/approve-and-post-for-user! db user-1 receipt-other review)
        (is false "Expected non-owned receipt approval to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= 404 (:status (ex-data e)))))))))

(deftest receipts-approve-for-admin-user-can-approve-any-receipt
  (when-let [db fixtures/*test-db*]
    (let [admin-user (create-test-user! db "admin")
          member-user (create-test-user! db "member")
          supplier-result (suppliers/find-or-create-supplier! db "AdminReceipt Supplier" {})
          supplier (:supplier supplier-result)
          payer (payers/create-payer! db {:type "cash" :label "Cash"})

          upload (receipts/upload-receipt! db {:user_id member-user
                                               :storage_key (str "s3://bucket/member-" (UUID/randomUUID) ".jpg")
                                               :bytes (.getBytes (str "member-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipts/update-status! db receipt-id "extracted")

          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "3.33")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "3.33")}]}
          expense (receipts/approve-and-post-for-user-any! db admin-user receipt-id review)
          stored (receipts/get-receipt db receipt-id)]
      (is (= admin-user (:user_id expense)))
      (is (= "posted" (:status stored)))
      (is (= (:id expense) (:expense_id stored)))
      (is (= member-user (:user_id stored))))))

(deftest expenses-price-observation-recorded-when-article-present
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "DM" {})
          supplier (:supplier supplier-result)
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

(deftest expenses-create-accepts-body-with-items-two-arity
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "TwoArity Supplier" {})
          supplier (:supplier supplier-result)
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          article-name (str "CoerceArticle-" (UUID/randomUUID))
          article (articles/create-article! db {:canonical_name article-name})
          expense (expenses/create-expense! db
                    {:supplier_id (str (:id supplier))
                     :payer_id (str (:id payer))
                     ;; HTML `datetime-local` format (no timezone)
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
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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

(deftest expenses-soft-delete-excluded-from-list
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "Pharmacy" {})
          supplier (:supplier supplier-result)
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
