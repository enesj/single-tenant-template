(ns app.domain.backend.expenses.workers.receipt-ocr-extraction-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
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

(deftest non-item-reason-keeps-legitimate-item-label-with-br-fino
  (let [non-item-reason #'extraction/non-item-reason
        ctx {:items-count 10
             :grand-total 41.94M}
        item {:raw_label "MILERAM BR&FINO 400G"
              :qty 1
              :unit_price 3.30
              :line_total 3.30}]
    (is (nil? (non-item-reason ctx item)))))

(deftest non-item-reason-keeps-item-with-leading-header-token
  (let [non-item-reason #'extraction/non-item-reason
        ctx {:items-count 10
             :grand-total 49.92M}
        item {:raw_label "Artikal BOMBONJERA 230G RAFFAELLO FER"
              :qty 1
              :unit_price 9.90
              :line_total 9.90}]
    (is (nil? (non-item-reason ctx item)))))

(deftest non-item-reason-filters-br-colon-reference-as-metadata
  (let [non-item-reason #'extraction/non-item-reason
        ctx {:items-count 10
             :grand-total 41.94M}
        item {:raw_label "br: 12345/AB"
              :qty 1
              :unit_price 1.00
              :line_total 1.00}]
    (is (= :metadata (non-item-reason ctx item)))))

(deftest persist-extract-result-filters-cyrillic-summary-rows-from-items
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
                            :extraction {:merchant {:name "TROPIC MALOPRODAJA"}
                                         :totals {:total 6.78}
                                         :items [{:raw_label "ITEM A" :qty 1 :unit_price 2.99 :line_total 2.99}
                                                 {:raw_label "ITEM B" :qty 1 :unit_price 3.79 :line_total 3.79}
                                                 {:raw_label "Примљено средстава" :qty 1 :unit_price 6.78 :line_total 6.78}
                                                 {:raw_label "Платна картица" :qty 1 :unit_price 6.78 :line_total 6.78}
                                                 {:raw_label "Укупан износ без пореза" :qty 1 :unit_price 5.79 :line_total 5.79}
                                                 {:raw_label "Укупан износ пореза" :qty 1 :unit_price 0.99 :line_total 0.99}
                                                 {:raw_label "Укупан промет (Е)" :qty 1 :unit_price 6.78 :line_total 6.78}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})
            stored-items (get-in @stored [:raw_extract_json :extraction :items])
            post-processing (get-in @stored [:raw_extract_json :post_processing])]
        (is (= receipt-id (:receipt-id res)))
        (is (= "extracted" (:status res)))
        (is (= "extracted" (:effective-status res)))
        (is (= 2 (count stored-items)))
        (is (= #{"ITEM A" "ITEM B"}
              (set (map :raw_label stored-items))))
        (is (= 2 (:article-aliases @calls)))
        (is (= #{"ITEM A" "ITEM B"}
              (set (:labels @calls))))
        (is (= 5 (:dropped-count post-processing)))))))

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

(deftest persist-extract-result-does-not-create-article-aliases-when-supplier-unknown
  (let [receipt-id (java.util.UUID/randomUUID)
        unknown-supplier-id (java.util.UUID/randomUUID)
        calls (atom {:resolve-supplier 0
                     :supplier-aliases 0
                     :article-aliases 0})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias!
                  (fn [& _]
                    (swap! calls update :supplier-aliases inc)
                    {:id (java.util.UUID/randomUUID)
                     :supplier_id nil})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (swap! calls update :resolve-supplier inc)
                    {:supplier {:id (java.util.UUID/randomUUID)}
                     :source :places-api})
                  article-aliases/get-unknown-supplier-id (fn [& _] unknown-supplier-id)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    (swap! calls update :article-aliases inc)
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            ;; No merchant name -> supplier_guess nil -> :unknown source.
                            :extraction {:totals {:total 1.00}
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
        ;; No supplier guess -> no alias lookup or Places resolution.
        (is (= 0 (:supplier-aliases @calls)))
        (is (= 0 (:resolve-supplier @calls)))
        ;; Critically: don't create article aliases under "Unknown Supplier" during extraction.
        (is (= 0 (:article-aliases @calls)))))))

(deftest persist-extract-result-marks-review-required-when-supplier-is-undefined
  (let [receipt-id (java.util.UUID/randomUUID)
        unknown-supplier-id (java.util.UUID/randomUUID)
        persisted-status (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [_db _rid status _extra]
                                                  (reset! persisted-status status)
                                                  nil)
                  article-aliases/get-unknown-supplier-id (fn [& _] unknown-supplier-id)
                  supplier-aliases/find-or-create-alias! (fn [& _]
                                                           (throw (ex-info "supplier resolution failed" {})))
                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Known Label But Unresolved Supplier"}
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
        (is (= "review_required" (:status res)))
        (is (= "review_required" @persisted-status))))))

(deftest parse-money-handles-common-formats
  (let [parse-money #'common/parse-money]
    (is (= 10.26M (parse-money "10.26")))
    (is (= 10.26M (parse-money "$10.26")))
    (is (= 10.26M (parse-money "10,26")))
    (is (= 1234.56M (parse-money "1,234.56")))
    (is (nil? (parse-money "abc")))))

(deftest normalize-currency-applies-default
  (let [normalize-currency #'common/normalize-currency]
    (is (= "USD" (normalize-currency "usd" "BAM")))
    (is (= "BAM" (normalize-currency nil "BAM")))
    (is (= "EUR" (normalize-currency "GBP" "EUR")))
    (is (nil? (normalize-currency "GBP" "GBP")))))

(deftest review-required-heuristic
  (let [review-required? #'extraction/review-required?]
    (testing "missing critical fields"
      (is (true? (review-required? {:supplier_guess nil :total_amount_guess 1M :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess nil :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess nil :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 0}))))
    (testing "looks good"
      (is (false? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 2}))))))

(deftest lines-total-mismatch-detects-absolute-difference
  (let [mismatch? #'extraction/lines-total-mismatch?]
    (testing "overage mismatch"
      (is (true? (mismatch? [{:raw_label "A" :line_total 12.00M}] 10.00M))))
    (testing "underage mismatch"
      (is (true? (mismatch? [{:raw_label "A" :line_total 8.00M}] 10.00M))))
    (testing "exact total"
      (is (false? (mismatch? [{:raw_label "A" :line_total 10.00M}] 10.00M))))))

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
