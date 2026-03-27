(ns app.domain.expenses.services.receipts-test
  "Integration tests for receipts service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is use-fixtures]])
  (:import
    (java.nio.file Files)
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (java.time.Instant/now))

(defn- create-test-context!
  "Create a minimal user plus tenant context for receipt tests."
  ([db] (create-test-context! db "receipt-user"))
  ([db label]
   (let [user (th/ensure-test-user! db {:email (str label "-" (UUID/randomUUID) "@test.com")
                                        :name "Receipt Test User"})
         {:keys [tenant-id]} (th/ensure-test-tenant! db user)]
     {:user-id (:id user)
      :tenant-id tenant-id})))

(deftest receipts-approve-creates-expense-and-links
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-test-context! db "approve-links")
          supplier-result (suppliers/find-or-create-supplier! db "Konzum" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "card"
                                      :label "Visa"
                                      :tenant_id tenant-id})
          upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                      :storage_key "s3://bucket/r1.jpg"
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

(deftest receipts-delete-expense-reverts-receipt
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-test-context! db "delete-reverts")
          supplier (:supplier (suppliers/find-or-create-supplier! db "DeleteExpense Supplier" {}))
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                      :storage_key (str "s3://bucket/r-del-" (UUID/randomUUID) ".jpg")
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

      (expenses/delete-expense! db (:id expense))

      (let [after (receipt-queries/get-receipt db receipt-id)]
        (is (= "extracted" (:status after)))
        (is (nil? (:expense_id after)))))))

(deftest receipts-approve-rejects-second-post-until-explicitly-unlinked
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-test-context! db "no-duplicate-post")
          supplier (:supplier (suppliers/find-or-create-supplier! db "Duplicate Guard Supplier" {}))
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                      :storage_key (str "s3://bucket/r-dup-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "dup-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "11.11")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "11.11")}]}
          expense (receipt-approval/approve-and-post! db receipt-id review)]
      ;; Simulate OCR/reset bringing the receipt back to an approvable-looking status
      ;; while it is still linked to an expense.
      (receipt-status/update-status! db receipt-id "extracted")

      (try
        (receipt-approval/approve-and-post! db receipt-id review)
        (is false "Expected duplicate receipt approval to throw")
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (is (= 409 (:status data)))
            (is (= receipt-id (:receipt-id data)))
            (is (= (:id expense) (:expense-id data))))))

      (let [stored (receipt-queries/get-receipt db receipt-id)]
        (is (= (:id expense) (:expense_id stored)))
        (is (= (:id expense) (receipt-status/linked-expense-id db receipt-id)))))))

(deftest receipts-reset-for-ocr-rejects-linked-receipt
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-test-context! db "reset-linked-receipt")
          supplier (:supplier (suppliers/find-or-create-supplier! db "Reset Guard Supplier" {}))
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                      :storage_key (str "s3://bucket/r-reset-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "reset-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "9.99")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "9.99")}]}
          expense (receipt-approval/approve-and-post! db receipt-id review)]
      (try
        (receipt-status/reset-for-ocr! db receipt-id)
        (is false "Expected OCR reset on linked receipt to throw")
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (is (= 409 (:status data)))
            (is (= receipt-id (:receipt-id data)))
            (is (= (:id expense) (:expense-id data)))))))))

(deftest receipts-approve-allows-repost-after-explicit-unlink
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-test-context! db "repost-after-unlink")
          supplier (:supplier (suppliers/find-or-create-supplier! db "Repost Supplier" {}))
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                      :storage_key (str "s3://bucket/r-repost-" (UUID/randomUUID) ".jpg")
                                                      :bytes (.getBytes (str "repost-" (UUID/randomUUID)))})
          receipt-id (:id (:receipt upload))
          _ (receipt-status/update-status! db receipt-id "extracted")
          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "13.13")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "13.13")}]}
          first-expense (receipt-approval/approve-and-post! db receipt-id review)]
      (expenses/delete-expense! db (:id first-expense))

      (let [after-unlink (receipt-queries/get-receipt db receipt-id)
            second-expense (receipt-approval/approve-and-post! db receipt-id review)
            stored (receipt-queries/get-receipt db receipt-id)]
        (is (= "extracted" (:status after-unlink)))
        (is (nil? (:expense_id after-unlink)))
        (is (not= (:id first-expense) (:id second-expense)))
        (is (= "posted" (:status stored)))
        (is (= (:id second-expense) (:expense_id stored)))))))

(deftest receipts-delete-expense-clears-link-even-if-receipt-already-reverted
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-test-context! db "delete-clears-link")
          supplier (:supplier (suppliers/find-or-create-supplier! db "DeleteExpense Supplier (reverted receipt)" {}))
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                      :storage_key (str "s3://bucket/r-del-" (UUID/randomUUID) ".jpg")
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

      (expenses/delete-expense! db (:id expense))

      (let [after (receipt-queries/get-receipt db receipt-id)]
        (is (= "extracted" (:status after)))
        (is (nil? (:expense_id after))))

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
        (let [{:keys [tenant-id]} (create-test-context! db "local-file")
              supplier (:supplier (suppliers/find-or-create-supplier! db "Konzum" {}))
              payer (th/create-payer! db {:type "card"
                                          :label "Visa"
                                          :tenant_id tenant-id})
              upload (receipt-storage/upload-receipt! db {:tenant_id tenant-id
                                                          :storage_key storage-key
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
    (let [{user-1 :user-id tenant-1 :tenant-id} (create-test-context! db "member-one")
          {user-2 :user-id tenant-2 :tenant-id} (create-test-context! db "member-two")
          supplier-result (suppliers/find-or-create-supplier! db "UserReceipt Supplier" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-1})

          upload-owned (receipt-storage/upload-receipt! db {:user_id user-1
                                                            :tenant_id tenant-1
                                                            :storage_key (str "s3://bucket/u1-" (UUID/randomUUID) ".jpg")
                                                            :bytes (.getBytes (str "u1-" (UUID/randomUUID)))})
          receipt-owned (:id (:receipt upload-owned))
          _ (receipt-status/update-status! db receipt-owned "extracted")

          upload-unassigned (receipt-storage/upload-receipt! db {:tenant_id tenant-1
                                                                 :storage_key (str "s3://bucket/unassigned-" (UUID/randomUUID) ".jpg")
                                                                 :bytes (.getBytes (str "unassigned-" (UUID/randomUUID)))})
          receipt-unassigned (:id (:receipt upload-unassigned))
          _ (receipt-status/update-status! db receipt-unassigned "extracted")

          upload-other (receipt-storage/upload-receipt! db {:user_id user-2
                                                            :tenant_id tenant-2
                                                            :storage_key (str "s3://bucket/u2-" (UUID/randomUUID) ".jpg")
                                                            :bytes (.getBytes (str "u2-" (UUID/randomUUID)))})
          receipt-other (:id (:receipt upload-other))

          review {:supplier_id (:id supplier)
                  :payer_id (:id payer)
                  :purchased_at (now)
                  :total_amount (bigdec "7.77")
                  :currency "BAM"
                  :items [{:raw_label "Item" :line_total (bigdec "7.77")}]}

          expense-owned (receipt-approval/approve-and-post-for-user! db user-1 receipt-owned review :tenant-id tenant-1)
          stored-owned (receipt-queries/get-receipt db receipt-owned tenant-1)

          expense-unassigned (receipt-approval/approve-and-post-for-user! db user-1 receipt-unassigned review :tenant-id tenant-1)
          stored-unassigned (receipt-queries/get-receipt db receipt-unassigned tenant-1)

          scoped (receipt-queries/list-user-receipts db user-1 {:limit 200 :tenant-id tenant-1})
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
        (receipt-approval/approve-and-post-for-user! db user-1 receipt-other review :tenant-id tenant-1)
        (is false "Expected non-owned receipt approval to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= 404 (:status (ex-data e)))))))))

(deftest receipts-approve-for-admin-user-can-approve-any-receipt
  (when-let [db fixtures/*test-db*]
    (let [{admin-user :user-id} (create-test-context! db "admin-user")
          {member-user :user-id member-tenant-id :tenant-id} (create-test-context! db "member-user")
          supplier-result (suppliers/find-or-create-supplier! db "AdminReceipt Supplier" {})
          supplier (:supplier supplier-result)
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id member-tenant-id})

          upload (receipt-storage/upload-receipt! db {:user_id member-user
                                                      :tenant_id member-tenant-id
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
          expense (receipt-approval/approve-and-post-for-user-any! db admin-user receipt-id review :tenant-id member-tenant-id)
          stored (receipt-queries/get-receipt db receipt-id member-tenant-id)]
      (is (= admin-user (:user_id expense)))
      (is (= "posted" (:status stored)))
      (is (= (:id expense) (:expense_id stored)))
      (is (= member-user (:user_id stored))))))
