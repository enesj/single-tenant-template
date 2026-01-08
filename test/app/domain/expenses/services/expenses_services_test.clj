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

(deftest receipts-soft-delete-expense-reverts-receipt
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "DeleteExpense Supplier" {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          upload (receipts/upload-receipt! db {:storage_key (str "s3://bucket/r-del-" (UUID/randomUUID) ".jpg")
                                               :bytes (.getBytes (str "del-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipts/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "10.00")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "10.00")}]}
          expense (receipts/approve-and-post! db receipt-id review)
          posted (receipts/get-receipt db receipt-id)]
      (is (= "posted" (:status posted)))
      (is (= (:id expense) (:expense_id posted)))

      (expenses/soft-delete-expense! db (:id expense))

      (let [after (receipts/get-receipt db receipt-id)]
        (is (= "extracted" (:status after)))
        (is (nil? (:expense_id after)))))))

(deftest receipts-soft-delete-expense-clears-link-even-if-receipt-already-reverted
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "DeleteExpense Supplier (reverted receipt)" {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          upload (receipts/upload-receipt! db {:storage_key (str "s3://bucket/r-del-" (UUID/randomUUID) ".jpg")
                                               :bytes (.getBytes (str "del-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipts/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "10.00")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "10.00")}]}
          expense (receipts/approve-and-post! db receipt-id review)]
      ;; Simulate an inconsistent-but-realistic state: receipt status reverted/reset while
      ;; still retaining :expense_id (e.g. via a manual reset action).
      (receipts/update-status! db receipt-id "extracted")

      (let [before (receipts/get-receipt db receipt-id)]
        (is (= "extracted" (:status before)))
        (is (= (:id expense) (:expense_id before))))

      (expenses/soft-delete-expense! db (:id expense))

      (let [after (receipts/get-receipt db receipt-id)]
        (is (= "extracted" (:status after)))
        (is (nil? (:expense_id after))))

      ;; With the link cleared, the receipt should now be deletable.
      (is (= receipt-id (:id (receipts/delete-receipt! db receipt-id))))
      (is (nil? (receipts/get-receipt db receipt-id))))))

(deftest receipts-delete-allows-stale-expense-link-when-expense-soft-deleted
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "DeleteReceipt stale expense link " (UUID/randomUUID)) {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          upload (receipts/upload-receipt! db {:storage_key (str "s3://bucket/r-del-stale-" (UUID/randomUUID) ".jpg")
                                               :bytes (.getBytes (str "del-stale-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipts/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "10.00")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "10.00")}]}
          expense (receipts/approve-and-post! db receipt-id review)]
      ;; Put receipt into deletable status.
      (receipts/update-status! db receipt-id "extracted")

      ;; Soft-delete expense (expected to clear link normally).
      (expenses/soft-delete-expense! db (:id expense))

      ;; Re-introduce a stale link to a deleted expense (simulates old data).
      (jdbc/execute! db
        (hsql/format {:update :receipts
                      :set {:expense_id (:id expense)}
                      :where [:= :id receipt-id]}))

      (let [stale (receipts/get-receipt db receipt-id)]
        (is (= (:id expense) (:expense_id stale))))

      ;; Should still be deletable because the linked expense is soft-deleted.
      (is (= receipt-id (:id (receipts/delete-receipt! db receipt-id))))
      (is (nil? (receipts/get-receipt db receipt-id))))))

(deftest receipts-approve-does-not-move-local-receipt-file
  (when-let [db fixtures/*test-db*]
    (let [base-dir (io/file "upload" "stripes")
          _ (.mkdirs base-dir)
          storage-key (str (UUID/randomUUID) ".png")
          bytes (.getBytes (str "receipt-" (UUID/randomUUID)))
          src-file (io/file base-dir storage-key)
          dest-file (io/file base-dir "exported" storage-key)]
      (try
        ;; Create a local file to ensure approve-and-post doesn't relocate it.
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
          ;; File move functionality was removed; receipt files stay in upload/stripes/.
          (is (= storage-key (:storage_key stored)))
          (is (true? (.exists src-file)))
          (is (false? (.exists dest-file))))
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

(deftest expenses-auto-links-article-when-alias-exists
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "Walmart" {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          article (articles/create-article! db {:canonical_name (str "ShouldNotMatch-" (UUID/randomUUID))})
          ;; Even if an alias exists with a bad/blank-ish label, we intentionally skip matching.
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

(deftest articles-batch-create-aliases-dedupes-and-skips-invalid
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "AliasBatch Supplier " (UUID/randomUUID)) {}))
          article (articles/create-article! db {:canonical_name (str "AliasBatch Article " (UUID/randomUUID))})
          result (articles/batch-create-aliases!
                   db
                   {:supplier-id (:id supplier)
                    :article-id (:id article)
                    :raw-labels [" Milk " "MILK" "" "##" "A"]})]
      (is (= 1 (count (:created result))))
      (is (= "milk" (:raw_label_normalized (first (:created result)))))
      (is (some #(= :duplicate (:reason %)) (:skipped result)))
      (is (some #(= :blank (:reason %)) (:skipped result)))
      (is (some #(= :normalizes-to-blank (:reason %)) (:skipped result)))
      (is (some #(= :too-short (:reason %)) (:skipped result))))))

(deftest articles-batch-create-aliases-conflict-and-reassign
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "AliasConflict Supplier " (UUID/randomUUID)) {}))
          article-a (articles/create-article! db {:canonical_name (str "AliasConflict A " (UUID/randomUUID))})
          article-b (articles/create-article! db {:canonical_name (str "AliasConflict B " (UUID/randomUUID))})
          _ (articles/batch-create-aliases!
              db
              {:supplier-id (:id supplier)
               :article-id (:id article-a)
               :raw-labels ["MILK"]})
          conflict (articles/batch-create-aliases!
                     db
                     {:supplier-id (:id supplier)
                      :article-id (:id article-b)
                      :raw-labels ["MILK"]})
          reassigned (articles/batch-create-aliases!
                       db
                       {:supplier-id (:id supplier)
                        :article-id (:id article-b)
                        :raw-labels ["MILK"]
                        :allow-reassign? true})]
      (is (= 1 (count (:conflicts conflict))))
      (is (empty? (:reassigned conflict)))
      (is (= 1 (count (:reassigned reassigned))))
      (is (empty? (:conflicts reassigned))))))

(deftest articles-map-item-to-article-creates-alias-when-enabled
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "MapItem Supplier " (UUID/randomUUID)) {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          article (articles/create-article! db {:canonical_name (str "MapItem Article " (UUID/randomUUID))})
          expense (expenses/create-expense!
                    db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "1.00")
                     :currency "BAM"}
                    [{:raw_label "MILK" :line_total (bigdec "1.00")}])
          item-id (-> expense :items first :id)
          result (articles/map-item-to-article!
                   db
                   item-id
                   (:id article)
                   {:create-alias? true
                    :allow-alias-reassign? false})]
      (is (= (:id article) (get-in result [:expense-item :article_id])))
      (is (= 1 (count (get-in result [:alias-result :created]))))
      (is (= (:id article)
            (:id (articles/find-article-by-alias db (:id supplier) "MILK")))))))

(deftest articles-list-unmapped-items-filters-by-supplier
  (when-let [db fixtures/*test-db*]
    (let [supplier-a (:supplier (suppliers/find-or-create-supplier! db (str "Unmapped Supplier A " (UUID/randomUUID)) {}))
          supplier-b (:supplier (suppliers/find-or-create-supplier! db (str "Unmapped Supplier B " (UUID/randomUUID)) {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          exp-a (expenses/create-expense!
                  db
                  {:supplier_id (:id supplier-a)
                   :payer_id (:id payer)
                   :purchased_at (now)
                   :total_amount (bigdec "1.00")
                   :currency "BAM"}
                  [{:raw_label "A1" :line_total (bigdec "1.00")}])
          _exp-b (expenses/create-expense!
                   db
                   {:supplier_id (:id supplier-b)
                    :payer_id (:id payer)
                    :purchased_at (now)
                    :total_amount (bigdec "1.00")
                    :currency "BAM"}
                   [{:raw_label "B1" :line_total (bigdec "1.00")}])
          rows (articles/list-unmapped-items db {:supplier-id (:id supplier-a) :limit 50 :offset 0})]
      (is (= 1 (count rows)))
      (is (= (-> exp-a :items first :id) (:id (first rows))))
      (is (= (:id supplier-a) (:supplier_id (first rows))))
      (is (string? (:supplier_display_name (first rows)))))))

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

(deftest expenses-update-auto-links-only-newly-inserted-items
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "UpdateAutoLink Supplier " (UUID/randomUUID)) {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})

          ;; Create an expense with an item that will match later, but with NO alias yet.
          ;; This ensures the existing item remains article_id=nil after update.
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

          ;; Update expense with:
          ;; - the existing item (by id) unchanged (should stay nil)
          ;; - a NEW inserted item without :article_id (should be auto-linked)
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

(deftest expenses-soft-delete-soft-deletes-expense-items
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "DeleteExpenseItems Supplier " (UUID/randomUUID)) {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
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

