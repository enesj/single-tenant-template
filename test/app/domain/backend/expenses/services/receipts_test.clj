(ns app.domain.backend.expenses.services.receipts-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- insert-receipt!
  [db {:keys [id status total-amount-guess tenant-id]}]
  (let [file-hash (str (UUID/randomUUID) (UUID/randomUUID))
        file-hash (-> file-hash (str/replace "-" "") (subs 0 64))]
    (jdbc/execute-one!
      db
      ["insert into receipts (id, storage_key, file_hash, status, total_amount_guess, tenant_id)
        values (?, ?, ?, ?::receipt_status, ?::numeric, ?)"
       id
       (str "test/" id ".png")
       file-hash
       status
       total-amount-guess
       tenant-id]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- parse-jsonish
  [raw]
  (cond
    (map? raw) raw
    (string? raw) (json/parse-string raw true)
    (instance? org.postgresql.util.PGobject raw)
    (json/parse-string (.getValue ^org.postgresql.util.PGobject raw) true)
    :else nil))

(deftest build-receipt-order-clauses-supports-purchased-at-guess
  (testing "receipt sorting allowlist includes purchased-at-guess"
    (is (= [[:purchased_at_guess :asc]
            [:receipts.id :asc]]
          (#'receipt-queries/build-receipt-order-clauses
           [{:field :purchased-at-guess :direction :asc}]
           nil
           nil
           "receipts.status::text")))))

(deftest store-extraction-results-patch-style-and-casts
  (testing "does not wipe absent fields and uses jsonb/currency casts"
    (let [captured (atom nil)
          receipt-id (java.util.UUID/randomUUID)]
      (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured sql-params)
                                        {:ok true})]
        ;; Only raw_extract_json should be set.
        (receipt-status/store-extraction-results! :db receipt-id {:raw_extract_json {:a 1}})
        (let [[sql & _] @captured
              sql-lc (str/lower-case sql)]
          (is (str/includes? sql-lc "raw_extract_json"))
          (is (not (str/includes? sql-lc "raw_parse_json")))
          (is (str/includes? sql-lc "jsonb")))

        ;; Currency should be cast to enum.
        (receipt-status/store-extraction-results! :db receipt-id {:currency_guess "USD"})
        (let [[sql & _] @captured
              sql-lc (str/lower-case sql)]
          (is (str/includes? sql-lc "currency_guess"))
          (is (str/includes? sql-lc "cast"))
          (is (str/includes? sql-lc "currency")))))))

(deftest claim-status-includes-lease-interval
  (let [captured (atom nil)
        receipt-id (java.util.UUID/randomUUID)]
    (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                      (reset! captured sql-params)
                                      {:ok true})]
      (receipt-status/claim-for-parsing! :db receipt-id {:lease-seconds 60})
      (let [[sql & _] @captured]
        (is (str/includes? sql "NOW() - INTERVAL '60 seconds'"))))))

(deftest save-review-parses-datetime-local-validates-currency-and-persists-reviewed-total
  (testing "datetime-local purchased_at is parsed; invalid currency yields 400; saving a review persists the reviewed total_amount_guess"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db)
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          create-supplier! (:create! suppliers/service)
          supplier (create-supplier! db {:display_name (str "Test Supplier " (UUID/randomUUID))})
          receipt-id (UUID/randomUUID)
          _receipt (insert-receipt! db {:id receipt-id
                                        :status "review_required"
                                        :total-amount-guess "10.00"
                                        :tenant-id tenant-id})
          items [{:raw_label "Line 1" :line-total "12.00"}]]

      (testing "valid review saves reviewed items and updates total_amount_guess"
        (let [updated (receipt-approval/save-review!
                        db
                        receipt-id
                        {:supplier_id (:id supplier)
                         :purchased_at "2026-01-07T12:34"
                         :total_amount "12.00"
                         :currency "eur"
                         :items items})]
          (is (= "extracted" (str (:status updated)))
            "status should be promoted to extracted once the reviewed receipt has all required fields")
          (is (= 12.00M (:total_amount_guess updated))
            "total_amount_guess should reflect the reviewed total")
          (is (= 12.0 (get-in (parse-jsonish (:raw_extract_json updated))
                        [:extraction :totals :total]))
            "raw_extract_json should reflect the reviewed total")
          (is (some? (:purchased_at_guess updated))
            "purchased_at_guess should be persisted (parsed from datetime-local)")))

      (testing "invalid currency returns a 400 (no JDBC cast exception/500)"
        (try
          (receipt-approval/save-review!
            db
            receipt-id
            {:supplier_id (:id supplier)
             :purchased_at "2026-01-07T12:34"
             :total_amount "10.00"
             :currency "ZZZ"
             :items items})
          (is false "Expected ExceptionInfo")
          (catch clojure.lang.ExceptionInfo e
            (is (= 400 (:status (ex-data e))))
            (is (= :currency (:field (ex-data e))))))))))

(deftest save-review-preserves-ocr-item-unit-when-review-payload-omits-it
  (testing "saving a reviewed receipt keeps OCR-derived unit metadata by item position"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db)
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          create-supplier! (:create! suppliers/service)
          supplier (create-supplier! db {:display_name (str "Unit Preserve Supplier " (UUID/randomUUID))})
          receipt-id (UUID/randomUUID)
          _receipt (insert-receipt! db {:id receipt-id
                                        :status "review_required"
                                        :total-amount-guess "5.25"
                                        :tenant-id tenant-id})
          _stored (receipt-status/store-extraction-results!
                    db
                    receipt-id
                    {:raw_extract_json {:extraction {:items [{:raw_label "JAGODA SVJEZA"
                                                              :qty 0.750M
                                                              :unit "kg"
                                                              :line_total 5.25M}]}}})
          updated (receipt-approval/save-review!
                    db
                    receipt-id
                    {:supplier_id (:id supplier)
                     :purchased_at "2026-01-07T12:34"
                     :total_amount "5.25"
                     :currency "BAM"
                     :items [{:raw_label "JAGODA SVJEZA"
                              :qty 0.750M
                              :line_total 5.25M}]})]
      (is (= "kg" (get-in (parse-jsonish (:raw_extract_json updated)) [:extraction :items 0 :unit]))))))

(deftest update-posted-receipt-keeps-linked-expense-updates-working
  (testing "posted receipt editing can still update the linked expense"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db)
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          create-supplier! (:create! suppliers/service)
          original-supplier (create-supplier! db {:display_name (str "Posted Receipt Supplier " (UUID/randomUUID))})
          updated-supplier (create-supplier! db {:display_name (str "Updated Posted Receipt Supplier " (UUID/randomUUID))})
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          receipt-id (UUID/randomUUID)
          _receipt (insert-receipt! db {:id receipt-id
                                        :status "extracted"
                                        :total-amount-guess "10.00"
                                        :tenant-id tenant-id})
          review {:supplier_id (:id original-supplier)
                  :payer_id (:id payer)
                  :purchased_at "2026-01-07T12:34"
                  :total_amount "10.00"
                  :currency "BAM"
                  :items [{:raw_label "Line 1"
                           :line_total "10.00"}]}
          created-expense (receipt-approval/approve-and-post! db receipt-id review)
          {:keys [expense receipt]} (receipt-approval/update-posted-receipt!
                                      db
                                      receipt-id
                                      {:supplier_id (:id updated-supplier)
                                       :payer_id (:id payer)
                                       :purchased_at "2026-01-08T09:45"
                                       :total_amount "12.00"
                                       :currency "BAM"
                                       :notes "Updated through receipt"
                                       :items [{:raw_label "Line 1"
                                                :line_total "12.00"}]}
                                      :tenant-id tenant-id)]
      (is (= (:id created-expense) (:id expense)))
      (is (= receipt-id (:receipt_id expense)))
      (is (= (:id updated-supplier) (:supplier_id expense)))
      (is (= "Updated through receipt" (:notes expense)))
      (is (= 12.00M (:total_amount expense)))
      (is (= (:id expense) (:expense_id receipt)))
      (is (= "posted" (str (:status receipt)))))))
