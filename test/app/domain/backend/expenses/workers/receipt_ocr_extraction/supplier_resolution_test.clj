(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.test :refer [deftest is testing]]))

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

(deftest persist-extract-result-promotes-brand-over-legal-merchant-name-when-branch-present
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:supplier-alias-raw-label nil
                     :store-merchant nil})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  supplier-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    (swap! calls assoc :supplier-alias-raw-label raw-label)
                    {:id supplier-alias-id
                     :supplier_id mapped-supplier-id})
                  suppliers/resolve-or-create-supplier-with-places! (fn [& _]
                                                                      (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id (java.util.UUID/randomUUID)
                     :store_id nil})
                  stores/resolve-store-from-merchant
                  (fn [_db _supplier-id merchant _opts]
                    (swap! calls assoc :store-merchant merchant)
                    {:store-id nil
                     :store-alias-label nil})

                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown (str "Ljekarnička zdraustvena ustanova\n"
                                               "LUPRIV PLUS Mostar\n"
                                               "Ogranak Sarajevo 1\n"
                                               "Milana Preloga 2 S\n"
                                               "71120 Novo Sarajevo\n")
                            :extraction {:merchant {:name "Ljekarnička zdraustvena ustanova"
                                                    :store_name "LUPRIV PLUS Mostar"
                                                    :address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"}
                                         :totals {:total 31.65}
                                         :items [{:raw_label "ITEM" :line_total 31.65}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            stored-merchant (get-in @stored [:raw_extract_json :extraction :merchant])]
        (is (= "LUPRIV PLUS Mostar" (:name stored-merchant)))
        (is (= "Ljekarnička zdraustvena ustanova" (:legal_name stored-merchant)))
        (is (= "Ogranak Sarajevo 1" (:store_name stored-merchant)))
        (is (= "Milana Preloga 2 S, 71120 Novo Sarajevo" (:address stored-merchant)))
        (is (= "LUPRIV PLUS Mostar" (:supplier_guess @stored)))
        (is (= "Ogranak Sarajevo 1" (:store_guess @stored)))
        (is (= "LUPRIV PLUS Mostar" (:supplier-alias-raw-label @calls)))
        (is (= {:name "LUPRIV PLUS Mostar"
                :store_name "Ogranak Sarajevo 1"
                :address "Milana Preloga 2 S, 71120 Novo Sarajevo"}
              (select-keys (:store-merchant @calls) [:name :store_name :address])))))))

(deftest persist-extract-result-prefers-promoted-brand-over-store-alias-inference
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-store-id (java.util.UUID/randomUUID)
        legal-supplier-id (java.util.UUID/randomUUID)
        brand-supplier-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:places-calls 0
                     :mapped-supplier-id nil})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  supplier-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    {:id supplier-alias-id
                     :supplier_id nil
                     :raw_label_normalized (suppliers/normalize-supplier-key raw-label)})
                  supplier-aliases/map-alias-to-supplier-if-unmapped!
                  (fn [_db _alias-id supplier-id _confidence]
                    (swap! calls assoc :mapped-supplier-id supplier-id)
                    {:id supplier-alias-id
                     :supplier_id supplier-id})

                  store-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id (java.util.UUID/randomUUID)
                     :store_id mapped-store-id})
                  stores/get-store
                  (fn [_db store-id]
                    (if (= mapped-store-id store-id)
                      {:id store-id
                       :supplier_id legal-supplier-id
                       :display_name "Ljekarnička zdraustvena ustanova"}
                      nil))
                  stores/update-store!
                  (fn [_db store-id data _opts]
                    {:id store-id
                     :display_name (:display_name data)
                     :address (:address data)})

                  suppliers/service
                  {:get (fn [_db supplier-id]
                          (cond
                            (= legal-supplier-id supplier-id)
                            {:id supplier-id
                             :display_name "Ljekarnička zdraustvena ustanova"
                             :normalized_key "ljekarnicka-zdraustvena-ustanova"}

                            (= brand-supplier-id supplier-id)
                            {:id supplier-id
                             :display_name "LUPRIV PLUS Mostar"
                             :normalized_key "lupriv-plus-mostar"}

                            :else nil))}
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [_db supplier-guess _opts]
                    (swap! calls update :places-calls inc)
                    {:supplier {:id brand-supplier-id
                                :display_name supplier-guess
                                :normalized_key (suppliers/normalize-supplier-key supplier-guess)}
                     :source :ocr-fallback})

                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Ljekarnička zdraustvena ustanova"
                                                    :store_name "LUPRIV PLUS Mostar"
                                                    :address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"}
                                         :totals {:total 31.65}
                                         :items [{:raw_label "ITEM" :line_total 31.65}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            supplier-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :supplier])]
        (is (= 1 (:places-calls @calls)))
        (is (= brand-supplier-id (:mapped-supplier-id @calls)))
        (is (= brand-supplier-id (:supplier_id supplier-snapshot)))
        (is (= "LUPRIV PLUS Mostar" (:supplier_guess @stored)))))))

(deftest persist-extract-result-keeps-store-inferred-supplier-for-close-ocr-variant
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-store-id (java.util.UUID/randomUUID)
        inferred-supplier-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:places-calls 0
                     :mapped-supplier-id nil})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results!
                  (fn [_db _rid payload]
                    (reset! stored payload)
                    nil)
                  receipt-status/update-status! (fn [& _] nil)

                  supplier-aliases/find-or-create-alias!
                  (fn [_db raw-label]
                    {:id supplier-alias-id
                     :supplier_id nil
                     :raw_label_normalized (suppliers/normalize-supplier-key raw-label)})
                  supplier-aliases/map-alias-to-supplier-if-unmapped!
                  (fn [_db _alias-id supplier-id _confidence]
                    (swap! calls assoc :mapped-supplier-id supplier-id)
                    {:id supplier-alias-id
                     :supplier_id supplier-id})

                  store-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id (java.util.UUID/randomUUID)
                     :store_id mapped-store-id})
                  stores/get-store
                  (fn [_db store-id]
                    (if (= mapped-store-id store-id)
                      {:id store-id
                       :supplier_id inferred-supplier-id
                       :display_name "HOŠE-KOMERC"}
                      nil))
                  stores/update-store!
                  (fn [_db store-id data _opts]
                    {:id store-id
                     :display_name (:display_name data)
                     :address (:address data)})

                  suppliers/service
                  {:get (fn [_db supplier-id]
                          (when (= inferred-supplier-id supplier-id)
                            {:id supplier-id
                             :display_name "HOŠE-KOMERC"
                             :normalized_key "hose-komerc"}))}
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [_db supplier-guess _opts]
                    (swap! calls update :places-calls inc)
                    {:supplier {:id (java.util.UUID/randomUUID)
                                :display_name supplier-guess
                                :normalized_key (suppliers/normalize-supplier-key supplier-guess)}
                     :source :ocr-fallback})

                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Ljekarnička zdraustvena ustanova"
                                                    :store_name "HESE-KEMERC"
                                                    :address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"}
                                         :totals {:total 31.65}
                                         :items [{:raw_label "ITEM" :line_total 31.65}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            supplier-snapshot (get-in @stored [:raw_extract_json :resolution_snapshot :supplier])]
        (is (= 0 (:places-calls @calls)))
        (is (= inferred-supplier-id (:mapped-supplier-id @calls)))
        (is (= inferred-supplier-id (:supplier_id supplier-snapshot)))
        (is (= :store_alias (:source supplier-snapshot)))
        (is (= "HESE-KEMERC" (:supplier_guess @stored)))))))

(deftest persist-extract-result-repairs-auto-mapped-supplier-to-brand-and-promotes-store-name
  (let [receipt-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        legal-supplier-id (java.util.UUID/randomUUID)
        brand-supplier-id (java.util.UUID/randomUUID)
        mapped-store-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:repair-alias 0
                     :store-update nil})]
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
                     :supplier_id legal-supplier-id
                     :confidence 25
                     :raw_label_normalized "lupriv-plus-mostar"})
                  suppliers/service
                  {:get (fn [_db sid]
                          (when (= legal-supplier-id sid)
                            {:id sid
                             :display_name "Ljekarnička zdravstvena ustanova Biopharm Neum"
                             :normalized_key "ljekarnicka-zdravstvena-ustanova-biopharm-neum"}))}
                  suppliers/find-or-create-supplier!
                  (fn [_db display-name _opts]
                    (is (= "LUPRIV PLUS Mostar" display-name))
                    {:existing? false
                     :supplier {:id brand-supplier-id
                                :display_name display-name
                                :normalized_key "lupriv-plus-mostar"}})
                  supplier-aliases/map-alias-to-supplier!
                  (fn [_db alias-id supplier-id confidence]
                    (swap! calls update :repair-alias inc)
                    (is (= supplier-alias-id alias-id))
                    (is (= brand-supplier-id supplier-id))
                    (is (= 25 confidence))
                    {:id alias-id
                     :supplier_id supplier-id})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))

                  store-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id (java.util.UUID/randomUUID)
                     :store_id mapped-store-id})
                  stores/get-store
                  (fn [_db store-id]
                    (is (= mapped-store-id store-id))
                    {:id store-id
                     :display_name "LUPRIV PLUS Mostar"})
                  stores/update-store!
                  (fn [_db store-id data _opts]
                    (is (= mapped-store-id store-id))
                    (swap! calls assoc :store-update data)
                    {:id store-id
                     :display_name (:display_name data)
                     :address (:address data)})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "LUPRIV PLUS Mostar"
                                                    :store_name "Ogranak Sarajevo 1"
                                                    :address "Milana Preloga 2 S, 71120 Novo Sarajevo"
                                                    :raw_address "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"}
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
        (is (= 1 (:repair-alias @calls)))
        (is (= :alias_repaired (:source supplier-snapshot)))
        (is (= brand-supplier-id (:supplier_id supplier-snapshot)))
        (is (= "LUPRIV PLUS Mostar" (:supplier_guess @stored)))
        (is (= "Ogranak Sarajevo 1" (:store_guess @stored)))
        (is (= {:display_name "Ogranak Sarajevo 1"
                :address "Milana Preloga 2 S, 71120 Novo Sarajevo"}
              (:store-update @calls)))))))

(deftest resolve-supplier-and-alias-strips-legal-suffix-before-creating-supplier
  (let [resolve #'extraction/resolve-supplier-and-alias
        created-name (atom nil)
        alias-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)]
    (with-redefs [supplier-aliases/find-or-create-alias! (fn [_db _raw-label]
                                                           {:id alias-id
                                                            :supplier_id nil})
                  suppliers/resolve-or-create-supplier-with-places! (fn [_db supplier-guess _opts]
                                                                      (reset! created-name supplier-guess)
                                                                      {:supplier {:id supplier-id}
                                                                       :source :ocr-fallback})
                  supplier-aliases/map-alias-to-supplier-if-unmapped! (fn [& _] nil)]
      (is (= {:supplier-id supplier-id
              :supplier-alias-id alias-id
              :source :ocr-fallback}
            (resolve nil "HESE-KEMERC d.o.o. Sarajevo" {:merchant nil} {})))
      (is (= "HESE-KEMERC" @created-name)))))

(deftest resolve-supplier-and-alias-prefers-unique-descriptor-tail-supplier-for-unmapped-alias
  (let [resolve #'extraction/resolve-supplier-and-alias
        alias-id (java.util.UUID/randomUUID)
        descriptor-supplier-id (java.util.UUID/randomUUID)
        calls (atom {:resolve-or-create 0
                     :map-unmapped 0})]
    (with-redefs [supplier-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id alias-id
                     :supplier_id nil
                     :raw_label_normalized "zavod-za-biomedicinsku-dijagnostiku"})
                  suppliers/find-unique-descriptor-suffix-supplier
                  (fn [_db normalized-key]
                    (is (= "zavod-za-biomedicinsku-dijagnostiku" normalized-key))
                    {:id descriptor-supplier-id
                     :normalized_key "zavod-za-biomedicinsku-dijagnostiku-i-ispitivanje-medicover-bh"})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (swap! calls update :resolve-or-create inc)
                    (throw (ex-info "Should not be called" {})))
                  supplier-aliases/map-alias-to-supplier-if-unmapped!
                  (fn [_db passed-alias-id passed-supplier-id confidence]
                    (swap! calls update :map-unmapped inc)
                    (is (= alias-id passed-alias-id))
                    (is (= descriptor-supplier-id passed-supplier-id))
                    (is (= 25 confidence))
                    nil)]
      (is (= {:supplier-id descriptor-supplier-id
              :supplier-alias-id alias-id
              :source :alias_descriptor}
            (resolve nil "Zavod za biomedicinsku dijagnostiku" {:merchant nil} {})))
      (is (= 0 (:resolve-or-create @calls)))
      (is (= 1 (:map-unmapped @calls))))))

(deftest resolve-supplier-and-alias-repairs-low-confidence-mapping-to-descriptor-tail-supplier
  (let [resolve #'extraction/resolve-supplier-and-alias
        alias-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        descriptor-supplier-id (java.util.UUID/randomUUID)
        calls (atom {:map-override 0})]
    (with-redefs [supplier-aliases/find-or-create-alias!
                  (fn [_db _raw-label]
                    {:id alias-id
                     :supplier_id mapped-supplier-id
                     :confidence 25
                     :raw_label_normalized "zavod-za-biomedicinsku-dijagnostiku"})
                  suppliers/service
                  {:get (fn [_db sid]
                          (when (= mapped-supplier-id sid)
                            {:id sid
                             :normalized_key "zavod-za-biomedicinsku-dijagnostiku"}))}
                  suppliers/find-unique-descriptor-suffix-supplier
                  (fn [_db _normalized-key]
                    {:id descriptor-supplier-id
                     :normalized_key "zavod-za-biomedicinsku-dijagnostiku-i-ispitivanje-medicover-bh"})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (throw (ex-info "Should not be called" {})))
                  supplier-aliases/map-alias-to-supplier!
                  (fn [_db passed-alias-id passed-supplier-id confidence]
                    (swap! calls update :map-override inc)
                    (is (= alias-id passed-alias-id))
                    (is (= descriptor-supplier-id passed-supplier-id))
                    (is (= 25 confidence))
                    {:id passed-alias-id
                     :supplier_id passed-supplier-id})]
      (is (= {:supplier-id descriptor-supplier-id
              :supplier-alias-id alias-id
              :source :alias_descriptor_repaired}
            (resolve nil "Zavod za biomedicinsku dijagnostiku" {:merchant nil} {})))
      (is (= 1 (:map-override @calls))))))