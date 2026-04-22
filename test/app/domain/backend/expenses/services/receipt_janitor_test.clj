(ns app.domain.backend.expenses.services.receipt-janitor-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.receipts.janitor :as janitor]
    [app.domain.expenses.test-helpers :as th]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- random-file-hash
  []
  (let [raw (str (UUID/randomUUID) (UUID/randomUUID))]
    (-> raw (str/replace "-" "") (subs 0 64))))

(defn- delete-tree!
  [path]
  (let [root (io/file path)]
    (when (.exists root)
      (doseq [f (reverse (file-seq root))]
        (.delete ^java.io.File f)))))

(defn- ensure-parent-dir!
  [path]
  (let [parent (.getParentFile (io/file path))]
    (when parent
      (.mkdirs parent))))

(defn- write-file!
  [base-dir storage-key content]
  (let [path (str (io/file base-dir storage-key))]
    (ensure-parent-dir! path)
    (spit path content)
    path))

(defn- insert-receipt!
  [db {:keys [id tenant-id user-id storage-key status updated-at expense-id file-purged-at]}]
  (jdbc/execute-one!
    db
    ["insert into receipts (id, tenant_id, user_id, created_by, storage_key, file_hash, status, updated_at, expense_id, file_purged_at)
      values (?, ?, ?, ?, ?, ?, ?::receipt_status, ?, ?, ?)
      returning *"
     id
     tenant-id
     user-id
     user-id
     storage-key
     (random-file-hash)
     status
     updated-at
     expense-id
     file-purged-at]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- insert-expense!
  [db {:keys [id tenant-id user-id payer-id receipt-id purchased-at total-amount currency]}]
  (jdbc/execute-one!
    db
    ["insert into expenses (id, tenant_id, user_id, created_by, receipt_id, payer_id, purchased_at, total_amount, currency)
      values (?, ?, ?, ?, ?, ?, ?, ?, ?::currency)
      returning *"
     id
     tenant-id
     user-id
     user-id
     receipt-id
     payer-id
     purchased-at
     total-amount
     currency]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- update-receipt-expense-id!
  [db receipt-id expense-id]
  (jdbc/execute-one!
    db
    ["update receipts set expense_id = ? where id = ? returning *"
     expense-id
     receipt-id]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- fetch-receipt
  [db receipt-id]
  (jdbc/execute-one!
    db
    ["select * from receipts where id = ?" receipt-id]
    {:builder-fn rs/as-unqualified-lower-maps}))

(deftest list-purge-candidates-only-includes-finalized-aged-receipts
  (testing "candidate selection requires posted status, real expense row, age, and not-yet-purged"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email (str "janitor-candidates-" (UUID/randomUUID) "@example.com")})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          payer (th/create-payer! db {:tenant_id tenant-id :type "cash" :label "Cash"})
          now (Instant/parse "2026-03-24T12:00:00Z")
          old-ts (.minusSeconds now (* 90 86400))
          recent-ts (.minusSeconds now (* 10 86400))
          old-good-id (UUID/randomUUID)
          recent-id (UUID/randomUUID)
          no-expense-id (UUID/randomUUID)
          purged-id (UUID/randomUUID)
          wrong-status-id (UUID/randomUUID)
          old-expense-id (UUID/randomUUID)
          recent-expense-id (UUID/randomUUID)
          purged-expense-id (UUID/randomUUID)]
      (insert-receipt! db {:id old-good-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :storage-key (str old-good-id ".jpg")
                           :status "posted"
                           :updated-at old-ts})
      (insert-expense! db {:id old-expense-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :payer-id (:id payer)
                           :receipt-id old-good-id
                           :purchased-at old-ts
                           :total-amount 12.50M
                           :currency "BAM"})
      (update-receipt-expense-id! db old-good-id old-expense-id)

      (insert-receipt! db {:id recent-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :storage-key (str recent-id ".jpg")
                           :status "posted"
                           :updated-at recent-ts})
      (insert-expense! db {:id recent-expense-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :payer-id (:id payer)
                           :receipt-id recent-id
                           :purchased-at recent-ts
                           :total-amount 5.00M
                           :currency "BAM"})
      (update-receipt-expense-id! db recent-id recent-expense-id)

      (insert-receipt! db {:id no-expense-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :storage-key (str no-expense-id ".jpg")
                           :status "posted"
                           :updated-at old-ts
                           :expense-id (UUID/randomUUID)})

      (insert-receipt! db {:id purged-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :storage-key (str purged-id ".jpg")
                           :status "posted"
                           :updated-at old-ts
                           :file-purged-at now})
      (insert-expense! db {:id purged-expense-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :payer-id (:id payer)
                           :receipt-id purged-id
                           :purchased-at old-ts
                           :total-amount 8.00M
                           :currency "BAM"})
      (update-receipt-expense-id! db purged-id purged-expense-id)

      (insert-receipt! db {:id wrong-status-id
                           :tenant-id tenant-id
                           :user-id (:id user)
                           :storage-key (str wrong-status-id ".jpg")
                           :status "extracted"
                           :updated-at old-ts})

      (let [candidates (janitor/list-purge-candidates db {:older-than-days 60
                                                          :limit 20
                                                          :now now})]
        (is (= [old-good-id] (mapv :id candidates)))))))

(deftest run-janitor-dry-run-is-side-effect-free
  (testing "dry-run reports candidate and orphan work without deleting or marking rows"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email (str "janitor-dry-run-" (UUID/randomUUID) "@example.com")})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          payer (th/create-payer! db {:tenant_id tenant-id :type "cash" :label "Cash"})
          now (Instant/parse "2026-03-24T12:00:00Z")
          old-ts (.minusSeconds now (* 90 86400))
          receipt-id (UUID/randomUUID)
          expense-id (UUID/randomUUID)
          storage-dir (str (io/file "tmp" (str "receipt-janitor-dry-run-" (UUID/randomUUID))))]
      (.mkdirs (io/file storage-dir))
      (try
        (write-file! storage-dir (str receipt-id ".jpg") "keep me during dry run")
        (write-file! storage-dir "orphan.jpg" "orphan but dry-run")
        (insert-receipt! db {:id receipt-id
                             :tenant-id tenant-id
                             :user-id (:id user)
                             :storage-key (str receipt-id ".jpg")
                             :status "posted"
                             :updated-at old-ts})
        (insert-expense! db {:id expense-id
                             :tenant-id tenant-id
                             :user-id (:id user)
                             :payer-id (:id payer)
                             :receipt-id receipt-id
                             :purchased-at old-ts
                             :total-amount 19.99M
                             :currency "BAM"})
        (update-receipt-expense-id! db receipt-id expense-id)

        (let [result (janitor/run-janitor! db {:storage-base-dir storage-dir
                                               :older-than-days 60
                                               :limit 10
                                               :orphan-limit 10
                                               :dry-run? true
                                               :now now
                                               :purge-at now})]
          (is (= 1 (get-in result [:summary :purge-candidates])))
          (is (= 1 (get-in result [:summary :orphan-candidates])))
          (is (= :would-purge (get-in result [:purge-results 0 :result])))
          (is (= :would-delete-orphan (get-in result [:orphan-results 0 :result])))
          (is (.exists (io/file storage-dir (str receipt-id ".jpg"))))
          (is (.exists (io/file storage-dir "orphan.jpg")))
          (is (nil? (:file_purged_at (fetch-receipt db receipt-id)))))
        (finally
          (delete-tree! storage-dir))))))

(deftest run-janitor-purges-old-files-and-deletes-orphans
  (testing "one janitor pass deletes eligible receipt files, marks rows, and sweeps orphan files"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email (str "janitor-live-" (UUID/randomUUID) "@example.com")})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          payer (th/create-payer! db {:tenant_id tenant-id :type "cash" :label "Cash"})
          now (Instant/parse "2026-03-24T12:00:00Z")
          old-ts (.minusSeconds now (* 90 86400))
          receipt-id (UUID/randomUUID)
          expense-id (UUID/randomUUID)
          storage-dir (str (io/file "tmp" (str "receipt-janitor-live-" (UUID/randomUUID))))]
      (.mkdirs (io/file storage-dir))
      (try
        (write-file! storage-dir (str receipt-id ".jpg") "purge me")
        (write-file! storage-dir "nested/orphan.jpg" "nested orphan")
        (insert-receipt! db {:id receipt-id
                             :tenant-id tenant-id
                             :user-id (:id user)
                             :storage-key (str receipt-id ".jpg")
                             :status "posted"
                             :updated-at old-ts})
        (insert-expense! db {:id expense-id
                             :tenant-id tenant-id
                             :user-id (:id user)
                             :payer-id (:id payer)
                             :receipt-id receipt-id
                             :purchased-at old-ts
                             :total-amount 22.00M
                             :currency "BAM"})
        (update-receipt-expense-id! db receipt-id expense-id)

        (let [result (janitor/run-janitor! db {:storage-base-dir storage-dir
                                               :older-than-days 60
                                               :limit 10
                                               :orphan-limit 10
                                               :dry-run? false
                                               :now now
                                               :purge-at now})
              receipt (fetch-receipt db receipt-id)]
          (is (= 1 (get-in result [:summary :purge-candidates])))
          (is (= 1 (get-in result [:summary :orphan-candidates])))
          (is (= :purged (get-in result [:purge-results 0 :result])))
          (is (= :deleted-orphan (get-in result [:orphan-results 0 :result])))
          (is (false? (.exists (io/file storage-dir (str receipt-id ".jpg")))))
          (is (false? (.exists (io/file storage-dir "nested/orphan.jpg"))))
          (is (= now (:file_purged_at receipt))))
        (finally
          (delete-tree! storage-dir))))))