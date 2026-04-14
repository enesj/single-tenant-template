(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-inference-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.test :refer [deftest is]]))

(deftest persist-extract-result-skips-places-when-supplier-alias-mapped
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        alias-id (java.util.UUID/randomUUID)
        calls (atom {:resolve-supplier 0
                     :map-alias 0
                     :article-aliases 0})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id alias-id
                                                            :supplier_id mapped-supplier-id})
                  supplier-aliases/map-alias-to-supplier-if-unmapped!
                  (fn [& _]
                    (swap! calls update :map-alias inc)
                    nil)
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (swap! calls update :resolve-supplier inc)
                    {:supplier {:id (java.util.UUID/randomUUID)}
                     :source :places-api})
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    (swap! calls update :article-aliases inc)
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "AMKO KOMERC"}
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        ;; When the alias is already mapped, we should not call supplier resolution
        ;; (which can trigger Places API requests).
        (is (= 0 (:resolve-supplier @calls)))
        ;; No mapping update should be attempted either.
        (is (= 0 (:map-alias @calls)))
        ;; Still creates article aliases for line items.
        (is (= 1 (:article-aliases @calls)))))))

(deftest persist-extract-result-infers-supplier-from-mapped-store-alias
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        mapped-store-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:places 0
                     :store-alias-raw-labels []
                     :map-supplier-alias 0})]
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
                    {:id supplier-alias-id
                     :supplier_id nil})
                  supplier-aliases/map-alias-to-supplier-if-unmapped!
                  (fn [_db alias-id supplier-id confidence]
                    (swap! calls update :map-supplier-alias inc)
                    (is (= supplier-alias-id alias-id))
                    (is (= mapped-supplier-id supplier-id))
                    (is (= 25 confidence))
                    nil)
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (swap! calls update :places inc)
                    (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    (swap! calls update :store-alias-raw-labels conj raw-label)
                    {:id (java.util.UUID/randomUUID)
                     :store_id mapped-store-id})
                  stores/get-store
                  (fn [_db store-id]
                    (is (= mapped-store-id store-id))
                    {:id store-id
                     :supplier_id mapped-supplier-id})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [raw-address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"
            extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "???"
                                                    ;; NOTE: we preserve a merged provider string here for stable alias keying.
                                                    :raw_address raw-address
                                                    ;; Post-processed address (used for display/review only).
                                                    :address "Milana Preloga 2 S, 71120 Novo Sarajevo"
                                                    :store_name "Ogranak Sarajevo 1"}
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            supplier-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :supplier])]
        ;; The bug: supplier inference used :address (creating a new alias) instead of :raw_address.
        ;; Assert we always key by the merged `raw_address`.
        (is (every? #(= raw-address %) (:store-alias-raw-labels @calls)))

        (is (= 0 (:places @calls)))
        (is (= mapped-supplier-id (:supplier_id supplier-snapshot)))
        (is (= supplier-alias-id (:supplier_alias_id supplier-snapshot)))
        (is (= :store_alias (:source supplier-snapshot)))))))

(deftest persist-extract-result-infers-existing-supplier-from-store-name
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:places 0
                     :find-supplier 0
                     :map-supplier-alias 0})]
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
                    {:id supplier-alias-id
                     :supplier_id nil})
                  supplier-aliases/map-alias-to-supplier-if-unmapped!
                  (fn [_db alias-id supplier-id confidence]
                    (swap! calls update :map-supplier-alias inc)
                    (is (= supplier-alias-id alias-id))
                    (is (= mapped-supplier-id supplier-id))
                    (is (= 10 confidence))
                    nil)

                  store-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                        {:id (java.util.UUID/randomUUID)
                                                         :store_id nil})
                  stores/resolve-store-from-merchant (fn [& _]
                                                       {:store-id nil
                                                        :store-alias-label nil})

                  suppliers/find-by-normalized-key
                  (fn [_db normalized-key]
                    (swap! calls update :find-supplier inc)
                    (when (= normalized-key (suppliers/normalize-supplier-key "Bingo"))
                      {:id mapped-supplier-id}))
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (swap! calls update :places inc)
                    (throw (ex-info "Should not be called" {})))

                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "NOISY SUPPLIER"
                                                    :store_name "Bingo Ilidza"}
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            supplier-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :supplier])]
        (is (= 0 (:places @calls)))
        (is (>= (:find-supplier @calls) 1))
        (is (= mapped-supplier-id (:supplier_id supplier-snapshot)))
        (is (= supplier-alias-id (:supplier_alias_id supplier-snapshot)))
        (is (= :store_name_db (:source supplier-snapshot)))))))

(deftest persist-extract-result-splits-merged-store-address
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:store-alias-raw-label nil
                     :resolved-merchant nil})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  ;; Supplier already resolved via alias mapping.
                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id supplier-alias-id
                                                            :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    (swap! calls assoc :store-alias-raw-label raw-label)
                    {:id (java.util.UUID/randomUUID)
                     :store_id nil})
                  stores/resolve-store-from-merchant
                  (fn [_db _supplier-id merchant _opts]
                    (swap! calls assoc :resolved-merchant merchant)
                    {:store-id nil
                     :store-alias-label nil})

                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [markdown (str "LUPRIV PLUS Mostar\n"
                       "Ogranak Sarajevo 1\n"
                       "Milana Preloga 2 S\n"
                       "71120 Novo Sarajevo\n"
                       "JIB: 123\n")
            extract-result {:parsed-markdown markdown
                            :extraction {:merchant {:name "LUPRIV PLUS Mostar"
                                                    ;; Provider merged store + address into one string.
                                                    :address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"}
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            stored-merchant (get-in @stored [:raw_extract_json :extraction :merchant])]
        (is (= "Ogranak Sarajevo 1" (:store_name stored-merchant)))
        (is (= "Milana Preloga 2 S, 71120 Novo Sarajevo" (:address stored-merchant)))
        (is (= "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"
              (:store-alias-raw-label @calls)))
        (is (= {:name "LUPRIV PLUS Mostar"
                :store_name "Ogranak Sarajevo 1"
                :address "Milana Preloga 2 S, 71120 Novo Sarajevo"}
              (select-keys (:resolved-merchant @calls) [:name :store_name :address])))))))

(deftest persist-extract-result-splits-merged-store-address-when-provider-store-name-duplicates-merchant-name
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:store-alias-raw-label nil
                     :resolved-merchant nil})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  ;; Supplier already resolved via alias mapping.
                  supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id supplier-alias-id
                                                            :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    (swap! calls assoc :store-alias-raw-label raw-label)
                    {:id (java.util.UUID/randomUUID)
                     :store_id nil})
                  stores/resolve-store-from-merchant
                  (fn [_db _supplier-id merchant _opts]
                    (swap! calls assoc :resolved-merchant merchant)
                    {:store-id nil
                     :store-alias-label nil})

                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [markdown (str "LUPRIV PLUS Mostar\n"
                       "Ogranak Sarajevo 1\n"
                       "Milana Preloga 2 S\n"
                       "71120 Novo Sarajevo\n"
                       "JIB: 123\n")
            extract-result {:parsed-markdown markdown
                            :extraction {:merchant {:name "LUPRIV PLUS Mostar"
                                                    ;; Provider duplicated merchant.name into store_name.
                                                    :store_name "LUPRIV PLUS Mostar"
                                                    ;; Provider merged store + address into one string.
                                                    :address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"}
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            stored-merchant (get-in @stored [:raw_extract_json :extraction :merchant])]
        (is (= "Ogranak Sarajevo 1" (:store_name stored-merchant)))
        (is (= "Milana Preloga 2 S, 71120 Novo Sarajevo" (:address stored-merchant)))
        (is (= "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"
              (:store-alias-raw-label @calls)))
        (is (= {:name "LUPRIV PLUS Mostar"
                :store_name "Ogranak Sarajevo 1"
                :address "Milana Preloga 2 S, 71120 Novo Sarajevo"}
              (select-keys (:resolved-merchant @calls) [:name :store_name :address])))))))

(deftest persist-extract-result-infers-supplier-level-store-when-no-store-evidence
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        inferred-store-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:resolve-store 0})]
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
                    {:id supplier-alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  stores/resolve-store-from-merchant
                  (fn [_db supplier-id merchant opts]
                    (swap! calls update :resolve-store inc)
                    (is (= mapped-supplier-id supplier-id))
                    (is (= {:name "JADRANKA"}
                          (select-keys merchant [:name])))
                    (is (= "JADRANKA" (:supplier-display-name opts)))
                    {:store-id inferred-store-id
                     :store-alias-label nil})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [_res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   {:parsed-markdown ""
                    :extraction {:merchant {:name "JADRANKA"}
                                 :totals {:total 1.00}
                                 :items [{:raw_label "ITEM" :line_total 1.00}]}}
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            store-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :store])]
        (is (= 1 (:resolve-store @calls)))
        (is (= inferred-store-id (:store_id store-snapshot)))
        (is (= :supplier_only (:source store-snapshot)))
        (is (= "JADRANKA" (:store_guess @stored)))))))

(deftest persist-extract-result-ignores-mismatched-store-alias-and-resolves-current-supplier-store
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        other-supplier-id (java.util.UUID/randomUUID)
        stale-store-id (java.util.UUID/randomUUID)
        resolved-store-id (java.util.UUID/randomUUID)
        store-alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:resolve-store 0
                     :map-store-alias 0})]
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
                    {:id supplier-alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id store-alias-id
                     :raw_label "71000 SARAJEVO"
                     :raw_label_normalized "71000-sarajevo"
                     :store_id stale-store-id})
                  store-aliases/map-alias-to-store-if-unmapped!
                  (fn [& _]
                    (swap! calls update :map-store-alias inc)
                    nil)
                  stores/get-store
                  (fn [_db store-id]
                    (when (= stale-store-id store-id)
                      {:id store-id
                       :supplier_id other-supplier-id
                       :display_name "Hippy klupa 71000 Sarajevo"}))
                  stores/resolve-store-from-merchant
                  (fn [_db supplier-id merchant opts]
                    (swap! calls update :resolve-store inc)
                    (is (= mapped-supplier-id supplier-id))
                    (is (= {:name "JADRANKA"
                            :address "71000 Sarajevo"}
                          (select-keys merchant [:name :address])))
                    (is (= "71000 SARAJEVO" (:store-alias-raw-label opts)))
                    {:store-id resolved-store-id
                     :store-alias-label "71000 Sarajevo"})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [_res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   {:parsed-markdown ""
                    :extraction {:merchant {:name "JADRANKA"
                                            :address "71000 Sarajevo"}
                                 :totals {:total 1.00}
                                 :items [{:raw_label "ITEM" :line_total 1.00}]}}
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            store-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :store])]
        (is (= 1 (:resolve-store @calls)))
        (is (= 0 (:map-store-alias @calls)))
        (is (= resolved-store-id (:store_id store-snapshot)))
        (is (nil? (:store_alias_id store-snapshot)))
        (is (= :resolved (:source store-snapshot)))
        (is (nil? (:store_alias_id @stored)))))))
