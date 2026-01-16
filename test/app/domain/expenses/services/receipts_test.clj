(ns app.domain.expenses.services.receipts-test
  "Integration tests for receipts service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc])
  (:import
    (java.nio.file Files)
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (java.time.Instant/now))

(defn- create-test-user!
  "Insert a minimal user row and return its UUID id."
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

(deftest receipts-approve-creates-expense-and-links
  (when-let [db fixtures/*test-db*]
    (let [supplier-result (suppliers/find-or-create-supplier! db "Konzum" {})
          supplier (:supplier supplier-result)
          payer (payers/create-payer! db {:type "card" :label "Visa" :last4 "1234"})
          upload (receipt-storage/upload-receipt! db {:storage_key "s3://bucket/r1.jpg"
                                                      :bytes (.getBytes "hello world")})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "12.34")
                  :currency "BAM"
                  :items [{:raw_label "Milk" :line_total (bigdec "12.34")}]}
          expense (receipt-approval/approve-and-post! db receipt-id review)
          stored (receipt-queries/get-receipt db receipt-id)]
      (is (:id expense))
      (is (= "posted" (:status stored)))
      (is (= (:id expense) (:expense_id stored)))
      (is (= 1 (count (:items expense)))))))

(deftest receipts-soft-delete-expense-reverts-receipt
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "DeleteExpense Supplier" {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          upload (receipt-storage/upload-receipt! db {:storage_key (str "s3://bucket/r-del-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "del-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "10.00")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "10.00")}]}
          expense (receipt-approval/approve-and-post! db receipt-id review)
          posted (receipt-queries/get-receipt db receipt-id)]
      (is (= "posted" (:status posted)))
      (is (= (:id expense) (:expense_id posted)))

      (expenses/soft-delete-expense! db (:id expense))

      (let [after (receipt-queries/get-receipt db receipt-id)]
        (is (= "extracted" (:status after)))
        (is (nil? (:expense_id after)))))))

(deftest receipts-soft-delete-expense-clears-link-even-if-receipt-already-reverted
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db "DeleteExpense Supplier (reverted receipt)" {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          upload (receipt-storage/upload-receipt! db {:storage_key (str "s3://bucket/r-del-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "del-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "10.00")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "10.00")}]}
          expense (receipt-approval/approve-and-post! db receipt-id review)]
      (receipt-status/update-status! db receipt-id "extracted")

      (let [before (receipt-queries/get-receipt db receipt-id)]
        (is (= "extracted" (:status before)))
        (is (= (:id expense) (:expense_id before))))

      (expenses/soft-delete-expense! db (:id expense))

      (let [after (receipt-queries/get-receipt db receipt-id)]
        (is (= "extracted" (:status after)))
        (is (nil? (:expense_id after))))

      (is (= receipt-id (:id (receipt-queries/delete-receipt! db receipt-id))))
      (is (nil? (receipt-queries/get-receipt db receipt-id))))))

(deftest receipts-delete-allows-stale-expense-link-when-expense-soft-deleted
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "DeleteReceipt stale expense link " (UUID/randomUUID)) {}))
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          upload (receipt-storage/upload-receipt! db {:storage_key (str "s3://bucket/r-del-stale-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "del-stale-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "10.00")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "10.00")}]}
          expense (receipt-approval/approve-and-post! db receipt-id review)]
      (receipt-status/update-status! db receipt-id "extracted")

      (expenses/soft-delete-expense! db (:id expense))

      (jdbc/execute! db
        (hsql/format {:update :receipts
                      :set {:expense_id (:id expense)}
                      :where [:= :id receipt-id]}))

      (let [stale (receipt-queries/get-receipt db receipt-id)]
        (is (= (:id expense) (:expense_id stale))))

      (is (= receipt-id (:id (receipt-queries/delete-receipt! db receipt-id))))
      (is (nil? (receipt-queries/get-receipt db receipt-id))))))

(deftest receipts-approve-does-not-move-local-receipt-file
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
            upload (receipt-storage/upload-receipt! db {:storage_key storage-key
                            :bytes bytes})
              receipt-id (:id (:receipt upload))
            _ (receipt-status/update-status! db receipt-id "extracted")
              review {:supplier_id (:id supplier)
                      :payer_id (:id payer)
                      :purchased_at (now)
                      :total_amount (bigdec "12.34")
                      :currency "BAM"
                      :items [{:raw_label "Milk" :line_total (bigdec "12.34")}]}
            _expense (receipt-approval/approve-and-post! db receipt-id review)
            stored (receipt-queries/get-receipt db receipt-id)]
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

          upload-owned (receipt-storage/upload-receipt! db {:user_id user-1
                                                            :storage_key (str "s3://bucket/u1-" (UUID/randomUUID) ".jpg")
                                                            :bytes (.getBytes (str "u1-" (UUID/randomUUID)))})
          receipt-owned (:id (:receipt upload-owned))
          _ (receipt-status/update-status! db receipt-owned "extracted")

          upload-unassigned (receipt-storage/upload-receipt! db {:storage_key (str "s3://bucket/unassigned-" (UUID/randomUUID) ".jpg")
                                                                 :bytes (.getBytes (str "unassigned-" (UUID/randomUUID)))})
          receipt-unassigned (:id (:receipt upload-unassigned))
          _ (receipt-status/update-status! db receipt-unassigned "extracted")

          upload-other (receipt-storage/upload-receipt! db {:user_id user-2
                                                            :storage_key (str "s3://bucket/u2-" (UUID/randomUUID) ".jpg")
                                                            :bytes (.getBytes (str "u2-" (UUID/randomUUID)))})
          receipt-other (:id (:receipt upload-other))

          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "7.77")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "7.77")}]}

          expense-owned (receipt-approval/approve-and-post-for-user! db user-1 receipt-owned review)
          stored-owned (receipt-queries/get-receipt db receipt-owned)

          expense-unassigned (receipt-approval/approve-and-post-for-user! db user-1 receipt-unassigned review)
          stored-unassigned (receipt-queries/get-receipt db receipt-unassigned)

          scoped (receipt-queries/list-user-receipts db user-1 {:limit 200})
          scoped-ids (set (map :id scoped))]
      (is (= user-1 (:user_id expense-owned)))
      (is (= "posted" (:status stored-owned)))
      (is (= (:id expense-owned) (:expense_id stored-owned)))

      (is (= user-1 (:user_id expense-unassigned)))
      (is (= "posted" (:status stored-unassigned)))
      (is (= user-1 (:user_id stored-unassigned)))
      (is (= (:id expense-unassigned) (:expense_id stored-unassigned)))

      (is (contains? scoped-ids receipt-owned))
      (is (contains? scoped-ids receipt-unassigned))
      (is (not (contains? scoped-ids receipt-other)))

      (try
        (receipt-approval/approve-and-post-for-user! db user-1 receipt-other review)
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

          upload (receipt-storage/upload-receipt! db {:user_id member-user
                                                      :storage_key (str "s3://bucket/member-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "member-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")

          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "3.33")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "3.33")}]}
            expense (receipt-approval/approve-and-post-for-user-any! db admin-user receipt-id review)
            stored (receipt-queries/get-receipt db receipt-id)]
      (is (= admin-user (:user_id expense)))
      (is (= "posted" (:status stored)))
      (is (= (:id expense) (:expense_id stored)))
      (is (= member-user (:user_id stored))))))
