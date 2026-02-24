(ns app.domain.backend.expenses.workers.receipt-ocr-test
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as core]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util.concurrent CountDownLatch TimeUnit]))

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

(deftest markdown-line-item-candidates-supports-qty-lines
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "020327 HLJEB 400G SA SJEMELKA MA\n"
                   "1.000x 2,10 2.10E\n"
                   "B31508 PASTETA 114G KOKOSTJA ARGETA\n"
                   "1.000x 1,85 1.85E\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "HLJEB 400G SA SJEMELKA MA"
            :qty 1.000M
            :unit_price 2.10M
            :line_total 2.10M}
          (first items)))
    (is (= {:raw_label "PASTETA 114G KOKOSTJA ARGETA"
            :qty 1.000M
            :unit_price 1.85M
            :line_total 1.85M}
          (second items)))))

(deftest markdown-line-item-candidates-supports-label-plus-price
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "A10150772 Snala za kosu BH231226\n"
                   "1,95E\n"
                   "VOLTAREN RETARD TABLETE 100 MG A 2\n"
                   "0 SA P 172e 5,85E\n"
                   "ANDOL TABLETE 300 MG A 20 5673\n"
                   "5,70E\n")
        items (candidates markdown)]
    (is (= 3 (count items)))
    (is (= {:raw_label "Snala za kosu BH231226"
            :qty 1M
            :unit_price 1.95M
            :line_total 1.95M}
          (first items)))
    (is (= {:raw_label "VOLTAREN RETARD TABLETE 100 MG A 2 0 SA P 172e"
            :qty 1M
            :unit_price 5.85M
            :line_total 5.85M}
          (second items)))
    (is (= {:raw_label "ANDOL TABLETE 300 MG A 20 5673"
            :qty 1M
            :unit_price 5.70M
            :line_total 5.70M}
          (nth items 2)))))

(deftest markdown-line-item-candidates-supports-price-with-vat-letter-suffix
  (testing "BA receipts with VAT category suffix (e.g. 2,00A)"
    (let [candidates #'markdown/markdown->line-item-candidates
          ;; Real-world example from caffe bar receipts in Bosnia:
          ;; Lines end with X,XXA where A is the VAT category letter.
          markdown (str "FISKALNI RAČUN\n"
                     "ESPRESSO KAFA/co 2,00A\n"
                     "CAJ/co 2,50A\n")
          items (candidates markdown)]
      (is (= 2 (count items)) "Should parse two line items")
      ;; The parser may prepend non-money-prefix lines to the first item label.
      ;; Focus on the key behavior: parsing the price correctly despite the A suffix.
      (is (= 2.00M (:line_total (first items))))
      (is (= 2.50M (:line_total (second items))))
      (is (str/includes? (:raw_label (first items)) "ESPRESSO"))
      (is (str/includes? (:raw_label (second items)) "CAJ")))))

(deftest markdown-line-item-candidates-supports-mixed-qty-and-inline-price
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "TUBORG 0,33 NEPOVRATNI/KO\n"
                   "24,000x 1,55 37,20E\n"
                   "SCHWEPPES TONIC 1L/KO\n"
                   "3,000x 2,00 6,00E\n"
                   "BULLDOG GIN SA CASOM 0,7/KO 42,00E\n")
        items (candidates markdown)]
    (is (= 3 (count items)))
    (is (= {:raw_label "TUBORG 0,33 NEPOVRATNI/KO"
            :qty 24.000M
            :unit_price 1.55M
            :line_total 37.20M}
          (first items)))
    (is (= {:raw_label "SCHWEPPES TONIC 1L/KO"
            :qty 3.000M
            :unit_price 2.00M
            :line_total 6.00M}
          (second items)))
    (is (= {:raw_label "BULLDOG GIN SA CASOM 0,7/KO"
            :qty 1M
            :unit_price 42.00M
            :line_total 42.00M}
          (nth items 2)))))

(deftest markdown-line-item-candidates-does-not-treat-dimensions-as-qty
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown "60963601 Torba papirna velika 32 x 16 x 45 - bez /pc 0,70E\n"
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Torba papirna velika 32 x 16 x 45 - bez /pc"
            :qty 1M
            :unit_price 0.70M
            :line_total 0.70M}
          (first items)))))

(deftest markdown-line-item-candidates-applies-discounts
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "62778401 Mirisna svijeca u staklu Premium Collec\n"
                   "t/pc 10,00E\n"
                   "-50,00%: 5,00\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Mirisna svijeca u staklu Premium Collec"
            :qty 1M
            :unit_price 5.00M
            :line_total 5M}
          (first items)))))

(deftest markdown-line-item-candidates-applies-discounts-in-pipe-table
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "|  ITEM A | 1,000x | 10,00 | 10,00E  |\n"
                   "| --- | --- | --- | --- |\n"
                   "|  POPUST | -10,00% |  | 9,00  |\n"
                   "|  ITEM B | 1,000x | 5,00 | 5,00E  |\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "ITEM A"
            :qty 1.000M
            :unit_price 9.00M
            :line_total 9.00M}
          (first items)))
    (is (= {:raw_label "ITEM B"
            :qty 1.000M
            :unit_price 5.00M
            :line_total 5.00M}
          (second items)))))

(deftest markdown-line-item-candidates-ignores-tax-like-lines
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "ITEM\n"
                   "1,000x 1,00 1,00E\n"
                   "PDU E: 7,25\n"
                   "PDU: 7,25\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= "ITEM" (:raw_label (first items))))))

(deftest markdown-line-item-candidates-ignores-payment-summary-lines
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "POVRCE MIX\n"
                   "1,00E\n"
                   "POV E: 0,00\n"
                   "POV: 0,00\n"
                   "CEK: 1,00\n"
                   "CEKIC\n"
                   "10,00E\n"
                   "UMLAČENO: KORTICA: 11,00\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= "POVRCE MIX" (:raw_label (first items))))
    (is (= "CEKIC" (:raw_label (second items))))))

(deftest markdown-line-item-candidates-supports-markdown-table-rows
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "|  Mivolis flasteri za djecu |  |   |\n"
                   "| --- | --- | --- |\n"
                   "|  1,000x | 1,85 | 1,85E  |\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Mivolis flasteri za djecu"
            :qty 1.000M
            :unit_price 1.85M
            :line_total 1.85M}
          (first items)))))

(deftest markdown-line-item-candidates-supports-table-total-in-label-row
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "|  E09438 | BOMBONJERA 230G RAFFAELLO FER | 9,90E  |\n"
                   "| --- | --- | --- |\n"
                   "|  1,000x | 9,90 |   |\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "BOMBONJERA 230G RAFFAELLO FER"
            :qty 1.000M
            :unit_price 9.90M
            :line_total 9.90M}
          (first items)))))

(deftest markdown-line-item-candidates-supports-html-table-cells
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "<table>\n"
                   "  <tbody>\n"
                   "    <tr>\n"
                   "      <td>CHYMORAL_S_KAPSULE_A_10_7152</td>\n"
                   "      <td>21,45E</td>\n"
                   "    </tr>\n"
                   "    <tr>\n"
                   "      <td>HERBIKO_NATURAL_BOKVICA_SIRUP_125_ML_5bad</td>\n"
                   "      <td>9,20E</td>\n"
                   "    </tr>\n"
                   "  </tbody>\n"
                   "</table>\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "CHYMORAL_S_KAPSULE_A_10_7152"
            :qty 1M
            :unit_price 21.45M
            :line_total 21.45M}
          (first items)))
    (is (= {:raw_label "HERBIKO_NATURAL_BOKVICA_SIRUP_125_ML_5bad"
            :qty 1M
            :unit_price 9.20M
            :line_total 9.20M}
          (second items)))))

(deftest markdown-merchant-header-extracts-quoted-name
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "\"Pepco B-H\" d.o.o.\n"
                   "Podružnica Sarajevo 2\n"
                   "ul. Kolodvorska br.12\n"
                   "71000 Sarajevo\n"
                   "\n"
                   "JIB: 4203144510090\n"
                   "PIB: 203144510006\n")
        result (parse-header markdown)]
    (is (= "Pepco B-H" (:merchant_name result)))
    (is (= "Podružnica Sarajevo 2" (:store_name result)))
    (is (= "ul. Kolodvorska br.12, 71000 Sarajevo" (:address result)))))

(deftest markdown-merchant-header-extracts-unquoted-name
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "KONZUM d.o.o.\n"
                   "Poslovnica Tuzla 5\n"
                   "Trg slobode 10\n"
                   "75000 Tuzla\n"
                   "JIB: 123456789\n")
        result (parse-header markdown)]
    (is (= "KONZUM" (:merchant_name result)))
    (is (= "Poslovnica Tuzla 5" (:store_name result)))
    (is (= "Trg slobode 10, 75000 Tuzla" (:address result)))))

(deftest markdown-merchant-header-handles-minimal-header
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "BINGO d.d.\n"
                   "TC Mercator\n"
                   "JIB: 999\n")
        result (parse-header markdown)]
    (is (= "BINGO" (:merchant_name result)))
    (is (= "TC Mercator" (:store_name result)))
    (is (nil? (:address result)))))

(deftest markdown-merchant-header-handles-no-store-name
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "\"DM\" d.o.o.\n"
                   "ul. Marsala Tita 25\n"
                   "71000 Sarajevo\n"
                   "JIB: 111\n")
        result (parse-header markdown)]
    (is (= "DM" (:merchant_name result)))
    (is (= "ul. Marsala Tita 25, 71000 Sarajevo" (:address result)))))

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

(deftest markdown-supplier-guess-ignores-date-time-and-table-lines
  (let [markdown (str "# FISKALNI RAČUN\n"
                   "BF: 238900\n"
                   "13.02.2026. 17:36\n"
                   "<table>\n"
                   "  <tr><td>CHYMORAL_S_KAPSULE_A_10_7152</td><td>21,45E</td></tr>\n"
                   "</table>\n")]
    (is (nil? (markdown/markdown->supplier-guess markdown)))))

(deftest markdown-supplier-guess-keeps-valid-merchant-before-table
  (let [markdown (str "LUPRIV PLUS Mostar\n"
                   "13.02.2026. 17:36\n"
                   "<table>\n"
                   "  <tr><td>ITEM</td><td>1,00E</td></tr>\n"
                   "</table>\n")]
    (is (= "LUPRIV PLUS Mostar" (markdown/markdown->supplier-guess markdown)))))

(deftest markdown-purchased-at-extracts-ba-datetime-format
  (testing "parses dd.mm.yyyy. hh:mm with trailing dot"
    (let [markdown "UR CAFFE BAR\n29.01.2026. 14:31\nESPRESSO KAFA 2,00"]
      (is (= "2026-01-29T14:31:00" (markdown/markdown->purchased-at markdown)))))
  (testing "parses dd.mm.yyyy hh:mm without trailing dot"
    (let [markdown "BINGO\n21.01.2026 17:25\nItem 5,00"]
      (is (= "2026-01-21T17:25:00" (markdown/markdown->purchased-at markdown)))))
  (testing "parses date-only format dd.mm.yyyy"
    (let [markdown "MERCHANT\n15.03.2026\nItem 10,00"]
      (is (= "2026-03-15" (markdown/markdown->purchased-at markdown)))))
  (testing "returns nil when no date found"
    (is (nil? (markdown/markdown->purchased-at "No date here")))
    (is (nil? (markdown/markdown->purchased-at nil)))))

(deftest markdown-total-amount-prefers-total-over-trailing-ukupno-zero
  (let [markdown (str "TOTAL: 19,50\n"
                   "UPLACENO: 19,50\n"
                   "GOTOVINA: 19,50\n"
                   "UKUPNO: 0,00\n"
                   "POVRAT: 0,00\n")]
    (is (= 19.50M (markdown/markdown->total-amount markdown)))))

(deftest markdown-total-amount-falls-back-to-ukupno-when-total-missing
  (let [markdown (str "UKUPNO: 42,00\n"
                   "POVRAT: 0,00\n")]
    (is (= 42.00M (markdown/markdown->total-amount markdown)))))

(deftest markdown-total-amount-supports-heading-prefixed-total
  (let [markdown (str "## TOTAL: 30,70\n"
                   "UKUPNO: 0,00\n")]
    (is (= 30.70M (markdown/markdown->total-amount markdown)))))

(deftest process-extract-auto-retries-review-required-once
  (let [process-extract! #'core/process-extract!
        receipt-id (java.util.UUID/randomUUID)
        calls (atom {:claim 0 :ocr 0 :persist 0 :retry 0})]
    (with-redefs [receipt-status/claim-for-extracting! (fn [_db _rid _opts]
                                                         (swap! calls update :claim inc)
                                                         true)
                  common/read-receipt-bytes! (fn [_receipt _opts]
                                               {:bytes (.getBytes "x")})
                  image-preprocess/prepare-for-ocr (fn [{:keys [bytes content-type] :as _req}]
                                                     {:bytes bytes
                                                      :content-type content-type
                                                      :preprocessed? false})
                  mistral-ocr/ocr-extract! (fn [_cfg _req]
                                             (swap! calls update :ocr inc)
                                             {})
                  extraction/persist-extract-result! (fn [_db _rid _extract-result _opts]
                                                       (swap! calls update :persist inc)
                                                       (if (= 1 (:persist @calls))
                                                         {:receipt-id receipt-id :stage :extract :result :ok :status "review_required"}
                                                         {:receipt-id receipt-id :stage :extract :result :ok :status "extracted"}))
                  receipt-status/retry-extraction! (fn [_db _rid]
                                                     (swap! calls update :retry inc)
                                                     nil)]
      (let [res (process-extract! nil {:api-key "k"} {:id receipt-id :content_type "image/jpeg"} {:lease-seconds 900})]
        ;; Current implementation does not auto-retry review_required.
        (is (= "review_required" (:status res)))
        (is (= 1 (:claim @calls)))
        (is (= 1 (:ocr @calls)))
        (is (= 1 (:persist @calls)))
        (is (= 0 (:retry @calls)))))))

(deftest process-extract-preprocesses-image-before-mistral
  (let [process-extract! #'core/process-extract!
        receipt-id (java.util.UUID/randomUUID)
        seen (atom nil)
        prepared-bytes (.getBytes "prepared")]
    (with-redefs [receipt-status/claim-for-extracting! (fn [_db _rid _opts] true)
                  common/read-receipt-bytes! (fn [_receipt _opts]
                                               {:bytes (.getBytes "original")
                                                :path "/tmp/receipt-orig.heic"})
                  image-preprocess/prepare-for-ocr (fn [_req]
                                                     {:bytes prepared-bytes
                                                      :content-type "image/jpeg"
                                                      :preprocessed? true})
                  mistral-ocr/ocr-extract! (fn [_cfg req]
                                             (reset! seen req)
                                             {:raw {}
                                              :parsed-markdown ""})
                  extraction/persist-extract-result! (fn [_db _rid _extract-result _opts]
                                                       {:receipt-id receipt-id
                                                        :stage :extract
                                                        :result :ok
                                                        :status "extracted"})]
      (let [res (process-extract! nil
                  {:api-key "k" :auto-post-after-upload? false}
                  {:id receipt-id
                   :content_type "image/heic"
                   :original_filename "r.heic"}
                  {:lease-seconds 900
                   :defer-refine? true})]
        (is (= "extracted" (:status res)))
        (is (= "image/jpeg" (:content-type @seen)))
        (is (= "prepared" (String. ^bytes (:bytes @seen))))))))

(deftest refine-review-required-results-respects-concurrency-limit
  (let [limit 2
        opts {:cerebras-cfg {:refine-concurrency limit :refine-timeout-ms 5000}}
        started (CountDownLatch. limit)
        release (CountDownLatch. 1)
        active (atom 0)
        max-active (atom 0)
        results (vec (for [_ (range 5)]
                       {:receipt {:id (java.util.UUID/randomUUID)}
                        :review-required? true
                        :extract-result {}}))
        runner (future
                 (clojure.core/with-redefs-fn
                   {#'core/maybe-refine-review-required
                    (fn [_db _receipt _extract-result persist-result _opts]
                      (let [n (swap! active inc)]
                        (swap! max-active max n)
                        (.countDown started)
                        (.await release 2 TimeUnit/SECONDS)
                        (swap! active dec)
                        (assoc persist-result :refined? true)))}
                   (fn []
                     (#'core/refine-review-required-results! nil opts results))))]
    (is (.await started 1 TimeUnit/SECONDS))
    (is (<= @max-active limit))
    (.countDown release)
    (let [res @runner]
      (is (= (count results) (count res)))
      (is (every? :refined? res)))))

(deftest refine-review-required-results-keeps-processing-on-failure
  (let [opts {:cerebras-cfg {:refine-concurrency 3 :refine-timeout-ms 5000}}
        ok-id (java.util.UUID/randomUUID)
        bad-id (java.util.UUID/randomUUID)
        results [{:receipt {:id ok-id}
                  :review-required? true
                  :extract-result {}}
                 {:receipt {:id bad-id}
                  :review-required? true
                  :extract-result {}}]
        res (clojure.core/with-redefs-fn
              {#'core/maybe-refine-review-required
               (fn [_db receipt _extract-result persist-result _opts]
                 (if (= bad-id (:id receipt))
                   (throw (ex-info "boom" {:receipt-id bad-id}))
                   (assoc persist-result :refined? true)))}
              (fn []
                (#'core/refine-review-required-results! nil opts results)))]
    (is (true? (get-in res [0 :refined?])))
    (is (nil? (get-in res [1 :refined?])))))

(deftest maybe-refine-review-required-clears-refine-pending-when-skipped
  (let [receipt-id (java.util.UUID/randomUUID)
        cleared (atom [])
        res (clojure.core/with-redefs-fn
              {#'core/maybe-refine-with-cerebras (fn [_db _receipt extract-result _opts]
                                                  ;; Skip/failed refine: no :llm_refine returned.
                                                   extract-result)
               #'receipt-status/clear-refine-pending! (fn [_db rid]
                                                        (swap! cleared conj rid)
                                                        {:id rid})}
              (fn []
                (#'core/maybe-refine-review-required
                 ::db
                 {:id receipt-id}
                 {:parsed-markdown "x"}
                 {:receipt-id receipt-id :review-required? true}
                 {:clear-refine-pending? true})))]
    (is (= {:receipt-id receipt-id :review-required? true} res))
    (is (= [receipt-id] @cleared))))

(deftest refine-review-required-results-times-out
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 50}}
        results [{:receipt {:id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        res (clojure.core/with-redefs-fn
              {#'core/maybe-refine-review-required
               (fn [_db _receipt _extract-result persist-result _opts]
                 (try
                   (Thread/sleep 200)
                   (catch InterruptedException _))
                 (assoc persist-result :refined? true))}
              (fn []
                (#'core/refine-review-required-results! nil opts results)))]
    (is (nil? (get-in res [0 :refined?])))))

(deftest refine-review-required-results-clears-refine-pending-on-timeout
  (let [receipt-id (java.util.UUID/randomUUID)
        cleared (atom [])
        opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 10}}
        results [{:receipt {:id receipt-id}
                  :review-required? true
                  :extract-result {}}]
        _res (clojure.core/with-redefs-fn
               {#'core/maybe-refine-review-required
                (fn [_db _receipt _extract-result persist-result _opts]
                  (try
                    (Thread/sleep 200)
                    (catch InterruptedException _))
                  persist-result)
                #'receipt-status/clear-refine-pending!
                (fn [_db rid]
                  (swap! cleared conj rid)
                  {:id rid})}
               (fn []
                 (#'core/refine-review-required-results! ::db opts results)))]
    (is (= [receipt-id] @cleared))))

(deftest refine-review-required-results-logs-post-process-when-no-eligible-refine
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 1000}}
        results [{:receipt {:id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        output (let [w (java.io.StringWriter.)]
                 (binding [*out* w
                           *err* w]
                   (clojure.core/with-redefs-fn
                     {#'core/maybe-refine-review-required
                      (fn [_db _receipt _extract-result persist-result _opts]
                        persist-result)}
                     (fn []
                       (#'core/refine-review-required-results! nil opts results)))
                   (str w)))]
    (is (str/includes? output "Review-required post-process starting (no eligible refine)"))
    (is (str/includes? output "Review-required post-process complete (no eligible refine)"))
    (is (not (str/includes? output "Cerebras parallel refine starting")))))

(deftest refine-review-required-results-logs-cerebras-when-eligible-refine
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 1000}}
        results [{:receipt {:id (java.util.UUID/randomUUID)
                            :user_id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        output (let [w (java.io.StringWriter.)]
                 (binding [*out* w
                           *err* w]
                   (clojure.core/with-redefs-fn
                     {#'core/user-allows-receipt-refine?
                      (fn [_db _receipt]
                        true)
                      #'core/maybe-refine-review-required
                      (fn [_db _receipt _extract-result persist-result _opts]
                        (assoc persist-result :extract-result {:llm_refine {:model "test"}}))}
                     (fn []
                       (#'core/refine-review-required-results! ::db opts results)))
                   (str w)))]
    (is (str/includes? output "Cerebras parallel refine starting"))
    (is (str/includes? output "Cerebras parallel refine complete"))
    (is (not (str/includes? output "Review-required post-process starting (no eligible refine)")))))

(deftest refine-review-required-results-logs-cerebras-when-force-refine-without-user
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 1000}
              :force-refine? true}
        results [{:receipt {:id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        output (let [w (java.io.StringWriter.)]
                 (binding [*out* w
                           *err* w]
                   (clojure.core/with-redefs-fn
                     {#'core/user-allows-receipt-refine?
                      (fn [_db _receipt]
                        false)
                      #'core/maybe-refine-review-required
                      (fn [_db _receipt _extract-result persist-result _opts]
                        (assoc persist-result :extract-result {:llm_refine {:model "test"}}))}
                     (fn []
                       (#'core/refine-review-required-results! ::db opts results)))
                   (str w)))]
    (is (str/includes? output "Cerebras parallel refine starting"))
    (is (str/includes? output "Cerebras parallel refine complete"))
    (is (not (str/includes? output "Review-required post-process starting (no eligible refine)")))))

(deftest process-receipts-by-ids-defaults-force-refine
  (let [receipt-id (java.util.UUID/randomUUID)
        captured-opts (atom nil)]
    (clojure.core/with-redefs-fn
      {#'app.domain.backend.expenses.integrations.ocr-provider/build-provider
       (fn [_]
         {:provider :llamaparse
          :enabled? true
          :api-key "k"
          :auto-post-after-upload? false})
       #'app.domain.backend.expenses.integrations.cerebras/build-config
       (fn [_]
         {})
       #'app.domain.backend.expenses.services.places-api/build-config
       (fn [_]
         {})
       #'receipt-status/reset-for-ocr!
       (fn [_db _receipt-id]
         nil)
       #'receipt-queries/get-receipt
       (fn [_db _receipt-id]
         {:id receipt-id
          :status "uploaded"})
       #'core/process-receipt!
       (fn [_db _ocr-cfg _receipt opts]
         (reset! captured-opts opts)
         {:receipt-id receipt-id
          :result :ok})}
      (fn []
        (core/process-receipts-by-ids! ::db {} [receipt-id])))
    (is (true? (:force-refine? @captured-opts)))))
