(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.reconciliation-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.test :refer [deftest is]]))

(deftest persist-extract-result-applies-discount-override-for-popost-ocr-misread
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:article-aliases 0})]
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
                  stores/resolve-store-from-merchant (fn [& _] nil)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    (swap! calls update :article-aliases inc)
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "HOŠE-KOMERC"}
                                         :totals {:total 14.00}
                                         :items [{:raw_label "ITEM A" :qty 1 :unit_price 10.00 :line_total 10.00}
                                                 ;; OCR sometimes misreads "POPUST" as "POPOST"; we should still apply the override.
                                                 {:raw_label "POPOST -10,00%:" :qty 1 :unit_price 9.00 :line_total 9.00}
                                                 {:raw_label "ITEM B" :qty 1 :unit_price 5.00 :line_total 5.00}]}}
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
        (is (= 2 (count stored-items)))
        (let [normalize-item (fn [m]
                               (-> m
                                 (select-keys [:raw_label :qty :unit_price :line_total])
                                 (update :qty (comp double common/parse-money))
                                 (update :unit_price (comp double common/parse-money))
                                 (update :line_total (comp double common/parse-money))))]
          (is (= #{{:raw_label "ITEM A" :qty 1.0 :unit_price 9.0 :line_total 9.0}
                   {:raw_label "ITEM B" :qty 1.0 :unit_price 5.0 :line_total 5.0}}
                (set (map normalize-item stored-items)))))
        (is (= 2 (count resolution-items)))
        (is (= #{"ITEM A" "ITEM B"}
              (set (map :raw_label resolution-items))))
        (is (every? uuid? (map :article_alias_id resolution-items)))
        (is (= 1 (:discount-overrides post-processing)))
        (is (= 2 (:article-aliases @calls)))))))

(deftest persist-extract-result-prefers-markdown-discounted-items-when-provider-items-are-pre-discount
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  stores/resolve-store-from-merchant (fn [& _] nil)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown (str "|  ITEM A | 1,000x | 10,00 | 10,00E  |\n"
                                               "| --- | --- | --- | --- |\n"
                                               "|  POPUST | -10,00% |  | 9,00  |\n"
                                               "|  ITEM B | 1,000x | 5,00 | 5,00E  |\n"
                                               "TOTAL: 14,00\n")
                            :extraction {:merchant {:name "HOŠE-KOMERC"}
                                         :totals {:total 14.00}
                                         ;; Provider rows sometimes carry pre-discount totals only.
                                         :items [{:raw_label "ITEM A" :qty 1 :unit_price 10.00 :line_total 10.00}
                                                 {:raw_label "ITEM B" :qty 1 :unit_price 5.00 :line_total 5.00}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})
            stored-items (get-in @stored [:raw_extract_json :extraction :items])
            normalize-item (fn [m]
                             (-> m
                               (select-keys [:raw_label :qty :unit_price :line_total])
                               (update :qty (comp double common/parse-money))
                               (update :unit_price (comp double common/parse-money))
                               (update :line_total (comp double common/parse-money))))]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= [{:raw_label "ITEM A" :qty 1.0 :unit_price 9.0 :line_total 9.0}
                {:raw_label "ITEM B" :qty 1.0 :unit_price 5.0 :line_total 5.0}]
              (mapv normalize-item stored-items)))))))

(deftest persist-extract-result-overlays-structured-response-items-onto-bad-provider-items
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  stores/resolve-store-from-merchant (fn [& _] nil)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown nil
                            :raw {:items {:pages [{:items [{:type "table"
                                                            :rows [["CIG DUNHILL ESSENCE BRONZE" "3,000x" "6,60" "19,80E"]
                                                                   ["CHIPSY XCUT SALTED 140G" "" "" "3,60E"]]}]}]}}
                            :extraction {:merchant {:name "AMKO KOMERC"}
                                         :totals {:total 23.40}
                                         :items [{:raw_label "CIG DUNHILL ESSENCE BRONZE" :qty 1 :unit_price 3.00 :line_total 19.80}
                                                 {:raw_label "CHIPSY XCUT SALTED 140G" :qty 1 :unit_price 3.60 :line_total 3.60}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})
            stored-items (get-in @stored [:raw_extract_json :extraction :items])
            structured-merge (get-in @stored [:raw_extract_json :structured_response_merge])
            normalize-item (fn [m]
                             (-> m
                               (select-keys [:raw_label :qty :unit_price :line_total])
                               (update :qty common/parse-money)
                               (update :unit_price common/parse-money)
                               (update :line_total common/parse-money)))
            normalized-items (mapv normalize-item stored-items)]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= [{:raw_label "CIG DUNHILL ESSENCE BRONZE"
                 :qty 3.000M
                 :unit_price 6.60M
                 :line_total 19.80M}
                {:raw_label "CHIPSY XCUT SALTED 140G"
                 :qty 1M
                 :unit_price 3.60M
                 :line_total 3.60M}]
              normalized-items))
        (is (= 1 (:repaired-count structured-merge)))
        (is (= 2 (:matched-count structured-merge)))))))

(deftest persist-extract-result-prefers-structured-qty-and-unit-price-when-provider-collapses-line-total
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  stores/resolve-store-from-merchant (fn [& _] nil)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown nil
                            :raw {:items {:pages [{:items [{:type "table"
                                                            :rows [["100640 CIGARETE DUNHILL DISTINCT BLE" "4,000x 6,90" "27,60E"]
                                                                   ["000004 VRECICA SA RUCKOM" "1,000x 0,20" "0,20E"]]}]}]}}
                            :extraction {:merchant {:name "BINGO"}
                                         :totals {:total 27.80}
                                         :items [{:raw_label "CIGARETE DUNHILL DISTINCT BLE" :qty 1 :unit_price 27.60 :line_total 27.60}
                                                 {:raw_label "VRECICA SA RUCKOM" :qty 1 :unit_price 0.20 :line_total 0.20}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})
            stored-items (get-in @stored [:raw_extract_json :extraction :items])
            structured-merge (get-in @stored [:raw_extract_json :structured_response_merge])
            normalize-item (fn [m]
                             (-> m
                               (select-keys [:raw_label :qty :unit_price :line_total])
                               (update :qty common/parse-money)
                               (update :unit_price common/parse-money)
                               (update :line_total common/parse-money)))
            normalized-items (mapv normalize-item stored-items)]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= [{:raw_label "CIGARETE DUNHILL DISTINCT BLE"
                 :qty 4.000M
                 :unit_price 6.90M
                 :line_total 27.60M}
                {:raw_label "VRECICA SA RUCKOM"
                 :qty 1.000M
                 :unit_price 0.20M
                 :line_total 0.20M}]
              normalized-items))
        (is (= 1 (:repaired-count structured-merge)))
        (is (= 2 (:matched-count structured-merge)))))))

(deftest persist-extract-result-does-not-replace-refined-total-with-markdown-payment-total
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  stores/resolve-store-from-merchant (fn [& _] nil)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown (str "New Yorker BH\n"
                                               "10.02.2026. 17:25\n"
                                               "| Label | Qty | Unit | Total |\n"
                                               "| --- | --- | --- | --- |\n"
                                               "| Amisu Dzemper/Pullove | 1.000 | 9.95 | 9.95 |\n"
                                               "TOTAL: 20.00\n")
                            :extraction {:merchant {:name "New Yorker BH"}
                                         :totals {:subtotal 9.95
                                                  :total 9.95}
                                         :items [{:raw_label "Amisu Dzemper/Pullove"
                                                  :qty 1
                                                  :unit_price 9.95
                                                  :line_total 9.95}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        (is (= 9.95M (common/parse-money (:total_amount_guess @stored))))
        (is (= 9.95M (common/parse-money (get-in @stored [:raw_extract_json :extraction :totals :total]))))))))

(deftest persist-extract-result-prefers-markdown-merchant-over-role-labeled-person-metadata
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        supplier-alias-raw-label (atom nil)
        places-supplier-guess (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)
                  article-aliases/find-unknown-supplier-id (fn [_db] ::unknown-supplier)
                  supplier-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    (reset! supplier-alias-raw-label raw-label)
                    {:id alias-id
                     :supplier_id nil})
                  supplier-aliases/map-alias-to-supplier-if-unmapped! (fn [& _] nil)
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [_db supplier-guess _opts]
                    (reset! places-supplier-guess supplier-guess)
                    {:supplier {:id mapped-supplier-id
                                :display_name supplier-guess
                                :normalized_key (suppliers/normalize-supplier-key supplier-guess)}
                     :source :places-api})
                  stores/resolve-store-from-merchant
                  (fn [& _]
                    {:store-id ::store-id
                     :store-alias-label "Pepco B-H Sarajevo"})
                  store-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id ::store-alias-id
                     :store_id nil})
                  store-aliases/map-alias-to-store-if-unmapped! (fn [& _] nil)
                  stores/get-store
                  (fn [_db store-id]
                    {:id store-id
                     :supplier_id mapped-supplier-id})
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown (str "\"Pepco B-H\" d.o.o.\n"
                                               "Podružnica Sarajevo 2\n"
                                               "ul. Kolodvorska br.12\n"
                                               "71000 Sarajevo\n"
                                               "JIB: 4203144510090\n"
                                               "TOTAL: 10,00\n")
                            :extraction {:merchant {:name "Касир: Alma Halilovic"}
                                         :totals {:total 10.00}
                                         :items [{:raw_label "ITEM" :line_total 10.00}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})
            supplier-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :supplier])]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= "Pepco B-H" @supplier-alias-raw-label))
        (is (= "Pepco B-H" @places-supplier-guess))
        (is (= "Pepco B-H" (:supplier_guess @stored)))
        (is (= "Pepco B-H" (get-in @stored [:raw_extract_json :extraction :merchant :name])))
        (is (= mapped-supplier-id (:supplier_id supplier-snapshot)))
        (is (= :places-api (:source supplier-snapshot)))))))

(deftest reconcile-extraction-prefers-ocr-markdown-label
  (let [reconcile #'extraction/reconcile-extraction-with-markdown
        markdown (str "FISKALNI RACUN\n"
                   "| MLIJEKO MEGGLE 3,2% 657 | 3,000x | 2,25 | 6,75E |\n")
        extraction-in {:items [{:raw_label "NIKE AIR MAX 1"
                                :qty 1
                                :unit_price 6.75
                                :line_total 6.75}]}
        {:keys [extraction changed? changes]} (reconcile extraction-in markdown)]
    (is (true? changed?))
    (is (= [{:from "NIKE AIR MAX 1" :to "MLIJEKO MEGGLE 3,2% 657" :match :ocr-markdown}] changes))
    (is (= "MLIJEKO MEGGLE 3,2% 657" (get-in extraction [:items 0 :raw_label])))
    (is (= 3.000M (get-in extraction [:items 0 :qty])))
    (is (= 2.25M (get-in extraction [:items 0 :unit_price])))
    (is (= 6.75M (get-in extraction [:items 0 :line_total])))))

(deftest reconcile-extraction-noop-when-label-already-present
  (let [reconcile #'extraction/reconcile-extraction-with-markdown
        markdown "| NIKE AIR MAX 1 | 1x | 6,75 | 6,75 |\n"
        extraction-in {:items [{:raw_label "NIKE AIR MAX 1" :line_total 6.75}]}
        {:keys [extraction changed? changes]} (reconcile extraction-in markdown)]
    (is (false? changed?))
    (is (= [] changes))
    (is (= "NIKE AIR MAX 1" (get-in extraction [:items 0 :raw_label])))))

(deftest reconcile-extraction-keeps-descriptive-labels-over-code-only-markdown-summary
  (let [reconcile #'extraction/reconcile-extraction-with-markdown
        markdown (str "BINGO\n"
                   "| Label | Qty | Unit | Total |\n"
                   "| --- | --- | --- | --- |\n"
                   "| E15098 | 1 | 127.00 | 1.60 |\n"
                   "| D19717 | 1 | 270.00 | 5.00 |\n"
                   "TOTAL: 35.60\n")
        extraction-in {:items [{:raw_label "KEKS 127G SOFT LOVITA LJESNJA"
                                :qty 1
                                :unit_price 1.60
                                :line_total 1.60}
                               {:raw_label "HLJEB 270G ZLATNI KORIJEN"
                                :qty 2
                                :unit_price 2.50
                                :line_total 5.00}]}
        {:keys [extraction changed? changes]} (reconcile extraction-in markdown)]
    (is (false? changed?))
    (is (= [] changes))
    (is (= "KEKS 127G SOFT LOVITA LJESNJA" (get-in extraction [:items 0 :raw_label])))
    (is (= 1 (get-in extraction [:items 0 :qty])))
    (is (= 1.60 (get-in extraction [:items 0 :unit_price])))
    (is (= "HLJEB 270G ZLATNI KORIJEN" (get-in extraction [:items 1 :raw_label])))
    (is (= 2 (get-in extraction [:items 1 :qty])))
    (is (= 2.50 (get-in extraction [:items 1 :unit_price])))))