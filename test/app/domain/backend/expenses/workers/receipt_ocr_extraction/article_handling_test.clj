(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.article-handling-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.test :refer [deftest is]]))

(deftest persist-extract-result-auto-creates-articles-when-enabled
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        article-alias-id (java.util.UUID/randomUUID)
        article-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:create-article 0
                     :map-alias 0})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  ;; Supplier already resolved via alias mapping, so Places is skipped.
                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id supplier-alias-id
                                                            :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))

                  ;; Article alias is created but starts unmapped.
                  article-aliases/find-or-create-alias! (fn [_db _supplier-id _raw-label]
                                                          {:id article-alias-id
                                                           :article_id nil})
                  articles/find-or-create-article-by-canonical-name!
                  (fn [_db canonical-name]
                    (swap! calls update :create-article inc)
                    (is (= "ITEM" canonical-name))
                    {:id article-id})
                  article-aliases/map-alias-to-article!
                  (fn [_db alias-id mapped-article-id]
                    (swap! calls update :map-alias inc)
                    (is (= article-alias-id alias-id))
                    (is (= article-id mapped-article-id))
                    {:id alias-id
                     :article_id mapped-article-id})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "AMKO KOMERC"}
                                         :currency "BAM"
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true
                    :auto-create-articles? true})
            resolution-items (get-in @stored [:raw_extract_json :resolution_snapshot :items])]
        (is (= 1 (:create-article @calls)))
        (is (= 1 (:map-alias @calls)))
        (is (= 1 (count resolution-items)))
        (is (= article-alias-id (get-in resolution-items [0 :article_alias_id])))
        (is (= article-id (get-in resolution-items [0 :article_id])))))))

(deftest persist-extract-result-does-not-auto-create-article-when-alias-mapped
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        article-alias-id (java.util.UUID/randomUUID)
        existing-article-id (java.util.UUID/randomUUID)
        stored (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id supplier-alias-id
                                                            :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))

                  ;; Alias is already mapped -> skip article creation + mapping.
                  article-aliases/find-or-create-alias! (fn [_db _supplier-id _raw-label]
                                                          {:id article-alias-id
                                                           :article_id existing-article-id})
                  articles/find-or-create-article-by-canonical-name!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  article-aliases/map-alias-to-article!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "AMKO KOMERC"}
                                         :currency "BAM"
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true
                    :auto-create-articles? true})
            resolution-items (get-in @stored [:raw_extract_json :resolution_snapshot :items])]
        (is (= 1 (count resolution-items)))
        (is (= existing-article-id (get-in resolution-items [0 :article_id])))))))

(deftest persist-extract-result-filters-non-item-rows-before-alias-creation
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        calls (atom {:article-aliases 0
                     :labels []})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id alias-id
                                                            :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))
                  article-aliases/find-or-create-alias!
                  (fn [_db _supplier-id raw-label]
                    (swap! calls (fn [m]
                                   (-> m
                                     (update :article-aliases inc)
                                     (update :labels conj raw-label))))
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "HOŠE-KOMERC"}
                                         :totals {:total 20.00}
                                         :items [{:raw_label "ITEM A" :qty 1 :unit_price 10.00 :line_total 10.00}
                                                 {:raw_label "POPUST -10,00%:" :qty 1 :unit_price 9.00 :line_total 9.00}
                                                 {:raw_label "ITEM B" :qty 1 :unit_price 5.00 :line_total 5.00}
                                                 {:raw_label "ITEM B" :qty 1 :unit_price 5.00 :line_total 5.00}
                                                 {:raw_label "V.: 17,00%" :qty 1 :unit_price 2.38 :line_total 2.38}
                                                 {:raw_label "KARTICA" :qty 1 :unit_price 20.00 :line_total 20.00}
                                                 {:raw_label "UKUPNO" :qty 1 :unit_price 20.00 :line_total 20.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            labels (set (:labels @calls))]
        ;; Only the real purchased items should result in alias creation:
        ;; - ITEM A
        ;; - ITEM B (twice - duplicate items are preserved as separate purchases)
        (is (= 3 (:article-aliases @calls)))
        (is (= #{"ITEM A" "ITEM B"} labels))
        ;; Raw extraction status is still "extracted", but the effective status is
        ;; review_required because line totals and receipt total differ.
        (is (= "extracted" (:status _res)))
        (is (= "review_required" (:effective-status _res)))))))

(deftest persist-extract-result-keeps-legitimate-item-label-ending-with-br
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:article-aliases 0
                     :labels []})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id alias-id
                                                            :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))
                  article-aliases/find-or-create-alias!
                  (fn [_db _supplier-id raw-label]
                    (swap! calls (fn [m]
                                   (-> m
                                     (update :article-aliases inc)
                                     (update :labels conj raw-label))))
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "HOŠE-KOMERC"}
                                         :totals {:total 12.50}
                                         :items [{:raw_label "CIG DUNHIL ESSEN BR"
                                                  :qty 1
                                                  :unit_price 12.50
                                                  :line_total 12.50}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})
            stored-items (get-in @stored [:raw_extract_json :extraction :items])
            resolution-items (get-in @stored [:raw_extract_json :resolution_snapshot :items])
            post-processing (get-in @stored [:raw_extract_json :post_processing])]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= ["CIG DUNHIL ESSEN BR"] (mapv :raw_label stored-items)))
        (is (= ["CIG DUNHIL ESSEN BR"] (mapv :raw_label resolution-items)))
        (is (= 1 (:article-aliases @calls)))
        (is (= ["CIG DUNHIL ESSEN BR"] (:labels @calls)))
        ;; Regression: this line-item must never be misclassified as metadata.
        (is (not (contains? (set (get-in post-processing [:dropped-labels-sample :metadata]))
                   "CIG DUNHIL ESSEN BR")))))))