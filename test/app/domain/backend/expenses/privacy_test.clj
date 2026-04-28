(ns app.domain.backend.expenses.privacy-test
  (:require
    [app.domain.backend.expenses.privacy :as privacy]
    [clojure.test :refer [deftest is testing]]))

(deftest admin-receipt-view-scrubs-user-linkage-and-raw-content
  (testing "routine admin receipt projection hides identity, raw OCR, and storage metadata"
    (let [receipt {:id #uuid "00000000-0000-0000-0000-000000000001"
                   :user_id #uuid "00000000-0000-0000-0000-000000000002"
                   :created_by #uuid "00000000-0000-0000-0000-000000000003"
                   :created_by_name "Private User"
                   :created_by_email_ciphertext "encrypted-email"
                   :raw_extract_json {:extraction {:items [{:raw_label "secret line"}]}}
                   :parsed_markdown "# raw receipt markdown"
                   :storage_key "tenant/private/receipt.jpg"
                   :original_filename "private-receipt.jpg"
                   :file_hash "abcd"
                   :content_type "image/jpeg"
                   :download_url "/admin/api/expenses/receipts/1/download"
                   :status "extracted"}
          projected (privacy/admin-receipt-view receipt)]
      (is (= (:id receipt) (:id projected)))
      (is (= "extracted" (:status projected)))
      (is (= "image/jpeg" (:content-type projected)))
      (is (= "/admin/api/expenses/receipts/1/download" (:download-url projected)))
      (is (nil? (:user-id projected)))
      (is (nil? (:created-by projected)))
      (is (nil? (:created-by-name projected)))
      (is (nil? (:created-by-email-ciphertext projected)))
      (is (nil? (:raw-extract-json projected)))
      (is (nil? (:parsed-markdown projected)))
      (is (nil? (:storage-key projected)))
      (is (nil? (:original-filename projected)))
      (is (nil? (:file-hash projected))))))

(deftest admin-receipts-view-scrubs-nested-raw-content
  (testing "raw receipt fields are removed recursively from collection payloads"
    (let [projected (privacy/admin-receipts-view
                      [{:id 1
                        :raw_extract_json {:secret true}
                        :linked_expense {:id 2
                                         :raw_extract_json {:nested true}}}])]
      (is (= 1 (-> projected first :id)))
      (is (nil? (-> projected first :raw-extract-json)))
      (is (nil? (-> projected first :linked-expense :raw-extract-json))))))
