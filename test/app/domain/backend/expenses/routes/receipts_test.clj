(ns app.domain.backend.expenses.routes.receipts-test
  (:require
    [app.domain.backend.expenses.routes.receipts :as receipts-routes]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(deftest lines-total-amount-guess-sums-line-totals
  (let [lines-total-amount-guess #'receipts-routes/lines-total-amount-guess]
    (testing "returns nil when no items"
      (is (nil? (lines-total-amount-guess {})))
      (is (nil? (lines-total-amount-guess {:raw-extract-json {:extraction {:items []}}}))))

    (testing "sums parseable line totals"
      (is (= 19.95M
            (lines-total-amount-guess
              {:raw-extract-json
               {:extraction {:items [{:line-total "10.00"}
                                     {:line-total "$9.95"}]}}}))))

    (testing "ignores non-parseable values"
      (is (= 10M
            (lines-total-amount-guess
              {:raw-extract-json
               {:extraction {:items [{:line-total "abc"}
                                     {:line-total "10.00"}]}}}))))))

(deftest enrich-receipt-for-detail-adds-supplier-match-and-total-check
  (let [enrich #'receipts-routes/enrich-receipt-for-detail
        supplier-id (java.util.UUID/randomUUID)]
    (with-redefs [suppliers/normalize-supplier-key (fn [_] "samon-promet")
                  suppliers/find-by-normalized-key (fn [_db _key]
                                                     {:id supplier-id
                                                      :display_name "SAMON PROMET"
                                                      :normalized_key "samon-promet"})]
      (let [receipt {:supplier-guess "SAMON PROMET"
                     :total-amount-guess 19.95M
                     :currency-guess "BAM"
                     :raw-extract-json
                     {:extraction
                      {:items [{:line-total "10.00"}
                               {:line-total "9.95"}]}}}
            enriched (enrich :db receipt)]
        (is (true? (:supplier-guess-has-supplier? enriched)))
        (is (= {:id supplier-id
                :display-name "SAMON PROMET"
                :normalized-key "samon-promet"}
              (:supplier-guess-supplier enriched)))
        (is (= 19.95M (:lines-total-amount-guess enriched)))
        (is (true? (:total-guess-equals-lines-total-guess? enriched)))))

    (with-redefs [suppliers/normalize-supplier-key (fn [_] "no-match")
                  suppliers/find-by-normalized-key (fn [_db _key] nil)]
      (let [receipt {:supplier-guess "UNKNOWN"
                     :total-amount-guess 19.95M
                     :raw-extract-json {:extraction {:items [{:line-total "10"}]}}}
            enriched (enrich :db receipt)]
        (is (false? (:supplier-guess-has-supplier? enriched)))
        (is (nil? (:supplier-guess-supplier enriched)))
        (is (= 10M (:lines-total-amount-guess enriched)))
        (is (false? (:total-guess-equals-lines-total-guess? enriched)))))))

(deftest get-receipt-handler-includes-linked-expense-for-posted-receipts
  (let [receipt-id (java.util.UUID/randomUUID)
        expense-id (java.util.UUID/randomUUID)
        user-id (java.util.UUID/randomUUID)
        handler (receipts-routes/get-receipt-handler :db)]
    (with-redefs [receipt-queries/get-receipt
                  (fn [_db rid]
                    (is (= receipt-id rid))
                    {:id rid
                     :status "posted"
                     :expense_id expense-id
                     :user_id user-id
                     :created_by user-id
                     :created_by_user_id user-id
                     :created_by_name "Private User"
                     :created_by_email_ciphertext "encrypted-email"
                     :original_filename "receipt.jpg"
                     :content_type "image/jpeg"})

                  receipt-storage/resolve-local-receipt-file
                  (fn [_storage-key]
                    nil)

                  expenses/get-expense-with-items
                  (fn [_db eid]
                    (is (= expense-id eid))
                    {:id eid
                     :receipt_id receipt-id
                     :user_id user-id
                     :created_by user-id
                     :created_by_name "Private User"
                     :email_ciphertext "encrypted-email"
                     :items []})]
      (let [response (handler {:path-params {:id (str receipt-id)}})
            body (json/parse-string (:body response) true)]
        (is (= 200 (:status response)))
        (is (= true (:success body)))
        (is (= (str receipt-id) (get-in body [:receipt :id])))
        (is (= (str expense-id) (get-in body [:receipt :linked-expense :id])))
        (is (= [] (get-in body [:receipt :linked-expense :items])))
        (is (nil? (get-in body [:receipt :user-id])) "receipt user id is hidden")
        (is (nil? (get-in body [:receipt :created-by])) "receipt creator id is hidden")
        (is (nil? (get-in body [:receipt :created-by-user-id])) "receipt creator user id is hidden")
        (is (nil? (get-in body [:receipt :created-by-name])) "receipt creator name is hidden")
        (is (nil? (get-in body [:receipt :created-by-email-ciphertext])) "receipt encrypted email is hidden")
        (is (nil? (get-in body [:receipt :linked-expense :user-id])) "linked expense user id is hidden")
        (is (nil? (get-in body [:receipt :linked-expense :created-by])) "linked expense creator id is hidden")
        (is (nil? (get-in body [:receipt :linked-expense :created-by-name])) "linked expense creator name is hidden")
        (is (nil? (get-in body [:receipt :linked-expense :email-ciphertext])) "linked expense encrypted email is hidden")))))

(deftest get-receipt-handler-scrubs-raw-receipt-content
  (testing "routine admin receipt detail response hides raw OCR and storage metadata"
    (let [receipt-id (java.util.UUID/randomUUID)
          handler (receipts-routes/get-receipt-handler :db)]
      (with-redefs [receipt-queries/get-receipt
                    (fn [_db rid]
                      (is (= receipt-id rid))
                      {:id rid
                       :status "extracted"
                       :raw_extract_json {:extraction {:items [{:raw_label "secret"}]}}
                       :parsed_markdown "# raw markdown"
                       :storage_key "tenant/private/receipt.jpg"
                       :original_filename "private-receipt.jpg"
                       :file_hash "abcd"
                       :content_type "image/jpeg"})

                    receipt-storage/resolve-local-receipt-file
                    (fn [_storage-key]
                      nil)]
        (let [response (handler {:path-params {:id (str receipt-id)}})
              body (json/parse-string (:body response) true)
              receipt (:receipt body)]
          (is (= 200 (:status response)))
          (is (= true (:success body)))
          (is (= (str receipt-id) (:id receipt)))
          (is (= "image/jpeg" (:content-type receipt)))
          (is (nil? (:raw-extract-json receipt)))
          (is (nil? (:parsed-markdown receipt)))
          (is (nil? (:storage-key receipt)))
          (is (nil? (:original-filename receipt)))
          (is (nil? (:file-hash receipt))))))))

(deftest update-posted-handler-hides-user-linkage-while-returning-edit-results
  (let [receipt-id (java.util.UUID/randomUUID)
        expense-id (java.util.UUID/randomUUID)
        user-id (java.util.UUID/randomUUID)
        handler (receipts-routes/update-posted-handler :db)]
    (with-redefs [receipt-approval/update-posted-receipt!
                  (fn [_db rid body]
                    (is (= receipt-id rid))
                    (is (= {:notes "corrected"} body))
                    {:expense {:id expense-id
                               :receipt_id receipt-id
                               :user_id user-id
                               :created_by user-id
                               :created_by_name "Private User"
                               :total_amount 12.34M}
                     :receipt {:id receipt-id
                               :expense_id expense-id
                               :user_id user-id
                               :created_by user-id
                               :created_by_user_id user-id
                               :created_by_name "Private User"
                               :status "posted"}})]
      (let [response (handler {:path-params {:id (str receipt-id)}
                               :body {:notes "corrected"}})
            body (json/parse-string (:body response) true)]
        (is (= 200 (:status response)))
        (is (= true (:success body)))
        (is (= (str expense-id) (get-in body [:expense :id])))
        (is (= (str receipt-id) (get-in body [:receipt :id])))
        (is (= 12.34 (get-in body [:expense :total-amount])))
        (is (nil? (get-in body [:expense :user-id])) "updated expense user id is hidden")
        (is (nil? (get-in body [:expense :created-by])) "updated expense creator id is hidden")
        (is (nil? (get-in body [:expense :created-by-name])) "updated expense creator name is hidden")
        (is (nil? (get-in body [:receipt :user-id])) "updated receipt user id is hidden")
        (is (nil? (get-in body [:receipt :created-by])) "updated receipt creator id is hidden")
        (is (nil? (get-in body [:receipt :created-by-user-id])) "updated receipt creator user id is hidden")
        (is (nil? (get-in body [:receipt :created-by-name])) "updated receipt creator name is hidden")))))

(deftest download-receipt-handler-converts-heic-to-jpeg-when-requested
  (let [id (java.util.UUID/randomUUID)
        handler (receipts-routes/download-receipt-handler :db)
        tmp (java.io.File/createTempFile "receipt-test-" ".heic")]
    (spit tmp "not-a-real-heic")
    (with-redefs [receipt-queries/get-receipt
                  (fn [_db rid]
                    (is (= id rid))
                    {:id rid
                     :storage_key "test/receipt.heic"
                     :original_filename "IMG_3885.HEIC"
                     :content_type "image/heic"})

                  receipt-storage/resolve-local-receipt-file
                  (fn [_storage-key] tmp)

                  image-preprocess/prepare-for-preview
                  (fn [{:keys [path content-type filename]}]
                    (is (string? path))
                    (is (= "image/heic" content-type))
                    (is (= "IMG_3885.HEIC" filename))
                    {:bytes (byte-array [1 2 3])
                     :content-type "image/jpeg"
                     :preprocessed? true})]
      (let [resp (handler {:path-params {:id (str id)}
                           :query-params {"format" "jpeg"}})]
        (is (= 200 (:status resp)))
        (is (= "image/jpeg" (get-in resp [:headers "Content-Type"])))
        (is (= 3 (alength ^bytes (:body resp))))))))

(deftest download-receipt-handler-generates-jpeg-preview-when-requested
  (let [id (java.util.UUID/randomUUID)
        handler (receipts-routes/download-receipt-handler :db)
        tmp (java.io.File/createTempFile "receipt-test-" ".jpg")]
    (spit tmp "not-a-real-jpg")
    (with-redefs [receipt-queries/get-receipt
                  (fn [_db rid]
                    (is (= id rid))
                    {:id rid
                     :storage_key "test/receipt.jpg"
                     :original_filename "receipt.jpg"
                     :content_type "image/jpeg"})

                  receipt-storage/resolve-local-receipt-file
                  (fn [_storage-key] tmp)

                  image-preprocess/prepare-for-preview
                  (fn [{:keys [path content-type filename]}]
                    (is (string? path))
                    (is (= "image/jpeg" content-type))
                    (is (= "receipt.jpg" filename))
                    {:bytes (byte-array [4 5 6 7])
                     :content-type "image/jpeg"
                     :preprocessed? true})]
      (let [resp (handler {:path-params {:id (str id)}
                           :query-params {"preview" "1"}})]
        (is (= 200 (:status resp)))
        (is (= "image/jpeg" (get-in resp [:headers "Content-Type"])))
        (is (= 4 (alength ^bytes (:body resp))))))))

(deftest list-receipts-handler-includes-pagination-totals
  (let [handler (receipts-routes/list-receipts-handler :db)
        user-id (java.util.UUID/randomUUID)
        receipt-id (java.util.UUID/randomUUID)]
    (with-redefs [receipt-queries/list-receipts-page
                  (fn [_db opts]
                    (is (= {:status "uploaded"
                            :show-purged? false
                            :limit 25
                            :offset 50
                            :sorts [{:field :created-at :direction :asc}]
                            :order-dir :asc
                            :order-by :created-at}
                          opts))
                    {:rows [{:id receipt-id
                             :user_id user-id
                             :created_by user-id
                             :created_by_user_id user-id
                             :created_by_name "Private User"
                             :created_by_email_ciphertext "encrypted-email"
                             :original_filename "receipt-1.jpg"}]
                     :total 192
                     :purged-total 7})]
      (let [response (handler {:query-params {:status "uploaded"
                                              :limit "25"
                                              :offset "50"
                                              :sort "created-at:asc"}})
            body (json/parse-string (if (string? (:body response))
                                      (:body response)
                                      (slurp (:body response)))
                   true)
            receipt (first (:receipts body))]
        (is (= 200 (:status response)))
        (is (= true (:success body)))
        (is (= 192 (:total body)))
        (is (= 7 (:purged-total body)))
        (is (= 1 (count (:receipts body))))
        (is (= (str receipt-id) (:id receipt)))
        (is (nil? (:user-id receipt)) "list receipt user id is hidden")
        (is (nil? (:created-by receipt)) "list receipt creator id is hidden")
        (is (nil? (:created-by-user-id receipt)) "list receipt creator user id is hidden")
        (is (nil? (:created-by-name receipt)) "list receipt creator name is hidden")
        (is (nil? (:created-by-email-ciphertext receipt)) "list encrypted email is hidden")))))

(deftest list-receipts-handler-forwards-text-date-status-and-show-purged-filters
  (let [handler (receipts-routes/list-receipts-handler :db)
        purchased-at-from (java.time.Instant/parse "2026-03-01T10:15:30Z")
        created-at-from (java.time.Instant/parse "2026-04-01T00:00:00Z")
        created-at-to (java.time.Instant/parse "2026-04-30T00:00:00Z")
        updated-at-to (java.time.Instant/parse "2026-04-11T23:59:59Z")]
    (with-redefs [receipt-queries/list-receipts-page
                  (fn [_db opts]
                    (is (= {:status ["uploaded" "review_required"]
                            :show-purged? true
                            :limit 25
                            :offset 50
                            :sorts [{:field :status :direction :asc}]
                            :order-dir :asc
                            :order-by :status
                            :original-filename "IMG_3885"
                            :supplier-guess "SAMON"
                            :total-amount-guess-min 2M
                            :total-amount-guess-max 10.50M
                            :purchased-at-guess-from purchased-at-from
                            :created-at-from created-at-from
                            :created-at-to created-at-to
                            :updated-at-to updated-at-to}
                          opts))
                    {:rows []
                     :total 0
                     :purged-total 4})]
      (let [response (handler {:query-params {"status" "uploaded,review_required"
                                              "show-purged" "true"
                                              "limit" "25"
                                              "offset" "50"
                                              "sort" "status:asc"
                                              "original-filename" "IMG_3885"
                                              "supplier-guess" "SAMON"
                                              "total-display-min" "2"
                                              "total-amount-guess-max" "10.50"
                                              "purchased-at-guess-from" "2026-03-01T10:15:30Z"
                                              "created-at-from" "2026-04-01"
                                              "created-at-to" "2026-04-30"
                                              "updated-at-to" "2026-04-11T23:59:59Z"}})
            body (json/parse-string (if (string? (:body response))
                                      (:body response)
                                      (slurp (:body response)))
                   true)]
        (is (= 200 (:status response)))
        (is (= true (:success body)))
        (is (= 4 (:purged-total body)))))))

(deftest list-receipts-handler-supports-canonical-sort-param
  (let [handler (receipts-routes/list-receipts-handler :db)]
    (with-redefs [receipt-queries/list-receipts-page
                  (fn [_db opts]
                    (is (= {:status nil
                            :show-purged? false
                            :limit 50
                            :offset 0
                            :sorts [{:field :purchased-at-guess :direction :asc}
                                    {:field :status :direction :desc}]
                            :order-by :purchased-at-guess
                            :order-dir :asc}
                          opts))
                    {:rows []
                     :total 0
                     :purged-total 0})]
      (let [response (handler {:query-params {"sort" "purchased-at-guess:asc,status:desc"}})
            body (json/parse-string (if (string? (:body response))
                                      (:body response)
                                      (slurp (:body response)))
                   true)]
        (is (= 200 (:status response)))
        (is (= true (:success body)))
        (is (= 0 (:purged-total body)))))))

(deftest list-receipts-handler-defaults-to-desc-order-dir
  (let [handler (receipts-routes/list-receipts-handler :db)]
    (with-redefs [receipt-queries/list-receipts-page
                  (fn [_db opts]
                    (is (= {:status nil
                            :show-purged? false
                            :limit 50
                            :offset 0}
                          opts))
                    {:rows []
                     :total 0
                     :purged-total 0})]
      (let [response (handler {:query-params {}})
            body (json/parse-string (if (string? (:body response))
                                      (:body response)
                                      (slurp (:body response)))
                   true)]
        (is (= 200 (:status response)))
        (is (= true (:success body)))
        (is (= 0 (:purged-total body)))))))

(deftest admin-receipts-routes-no-longer-require-impersonation
  (testing "admin receipts routes expose child handlers directly without root impersonation middleware"
    (let [routes (receipts-routes/routes :db)
          route-options (when (map? (second routes)) (second routes))
          children (if route-options (nnext routes) (rest routes))]
      (is (= "/receipts" (first routes)))
      (is (nil? (:middleware route-options))
        "Root admin receipts route should not define impersonation middleware")
      (is (= #{"" "/ocr" "/:id/download" "/:id" "/:id/review" "/:id/approve" "/:id/update-posted"}
            (set (map first children)))))))