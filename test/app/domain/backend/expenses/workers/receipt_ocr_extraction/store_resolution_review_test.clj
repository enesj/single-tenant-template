(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.store-resolution-review-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.item-aliases :as item-aliases]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.supplier-store :as supplier-store]
    [clojure.test :refer [deftest is]]))

(defn- sample-extract-result
  [merchant]
  {:parsed-markdown ""
   :extraction {:merchant merchant
                :totals {:total 7.00M}
                :items [{:raw_label "ITEM"
                         :line_total 7.00M}]}})

(deftest persist-extract-result-keeps-extracted-when-store-evidence-is-absent
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        persisted-status (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [_db _rid status _extra]
                                                  (reset! persisted-status status)
                                                  nil)
                  article-aliases/find-unknown-supplier-id (fn [_db]
                                                             (java.util.UUID/randomUUID))
                  supplier-store/resolve-supplier-and-alias (fn [& _]
                                                              {:supplier-id supplier-id
                                                               :supplier-alias-id (java.util.UUID/randomUUID)
                                                               :source :alias})
                  supplier-store/resolve-store-and-alias (fn [& _]
                                                           {:store-id nil
                                                            :store-alias-id nil
                                                            :store-guess nil
                                                            :source :unknown})
                  item-aliases/auto-create-aliases! (fn [& _] nil)]
      (let [res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  (sample-extract-result {:name "BOR MEDIA"})
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= "extracted" @persisted-status))))))

(deftest persist-extract-result-marks-review-required-when-store-evidence-has-no-canonical-store
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        persisted-status (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [_db _rid status _extra]
                                                  (reset! persisted-status status)
                                                  nil)
                  article-aliases/find-unknown-supplier-id (fn [_db]
                                                             (java.util.UUID/randomUUID))
                  supplier-store/resolve-supplier-and-alias (fn [& _]
                                                              {:supplier-id supplier-id
                                                               :supplier-alias-id (java.util.UUID/randomUUID)
                                                               :source :alias})
                  supplier-store/resolve-store-and-alias (fn [& _]
                                                           {:store-id nil
                                                            :store-alias-id (java.util.UUID/randomUUID)
                                                            :store-guess "Milana Preloga 2 S, 71120 Novo Sarajevo"
                                                            :source :unknown})
                  item-aliases/auto-create-aliases! (fn [& _] nil)]
      (let [res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  (sample-extract-result {:name "BOR MEDIA"
                                          :address "Milana Preloga 2 S, 71120 Novo Sarajevo"})
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        (is (= "review_required" (:status res)))
        (is (= "review_required" @persisted-status))))))

(deftest persist-extract-result-marks-review-required-when-resolved-store-belongs-to-other-supplier
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        other-supplier-id (java.util.UUID/randomUUID)
        store-id (java.util.UUID/randomUUID)
        persisted-status (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [_db _rid status _extra]
                                                  (reset! persisted-status status)
                                                  nil)
                  article-aliases/find-unknown-supplier-id (fn [_db]
                                                             (java.util.UUID/randomUUID))
                  supplier-store/resolve-supplier-and-alias (fn [& _]
                                                              {:supplier-id supplier-id
                                                               :supplier-alias-id (java.util.UUID/randomUUID)
                                                               :source :alias_repaired})
                  supplier-store/resolve-store-and-alias (fn [& _]
                                                           {:store-id store-id
                                                            :store-alias-id (java.util.UUID/randomUUID)
                                                            :store-guess "Podružnica Restoran Anatolia"
                                                            :source :alias})
                  stores/get-store (fn [_db sid]
                                     (when (= store-id sid)
                                       {:id sid
                                        :supplier_id other-supplier-id
                                        :display_name "Podružnica Restoran Anatolia"}))
                  item-aliases/auto-create-aliases! (fn [& _] nil)]
      (let [res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  (sample-extract-result {:name "BOR MEDIA"
                                          :store_name "Podružnica Restoran Anatolia"
                                          :raw_address "Milana Preloga 2 S, 71120 Novo Sarajevo"})
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        (is (= "review_required" (:status res)))
        (is (= "review_required" @persisted-status))))))

(deftest persist-extract-result-clears-store-fields-when-store-resolution-is-missing
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        persisted-payload (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [_db _rid payload]
                                                             (reset! persisted-payload payload)
                                                             nil)
                  receipt-status/update-status! (fn [& _] nil)
                  article-aliases/find-unknown-supplier-id (fn [_db]
                                                             (java.util.UUID/randomUUID))
                  supplier-store/resolve-supplier-and-alias (fn [& _]
                                                              {:supplier-id supplier-id
                                                               :supplier-alias-id (java.util.UUID/randomUUID)
                                                               :source :alias})
                  supplier-store/resolve-store-and-alias (fn [& _]
                                                           {:store-id nil
                                                            :store-alias-id nil
                                                            :store-guess nil
                                                            :source :unknown})
                  item-aliases/auto-create-aliases! (fn [& _] nil)]
      (extraction/persist-extract-result!
        ::db
        receipt-id
        (sample-extract-result {:name "JADRANKA"})
        {:default-currency "BAM"
         :places-cfg {}
         :user-region "BA"
         :defer-refine? true})
      (is (contains? @persisted-payload :store_guess))
      (is (contains? @persisted-payload :store_alias_id))
      (is (nil? (:store_guess @persisted-payload)))
      (is (nil? (:store_alias_id @persisted-payload))))))
