(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-brand-repair-test
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

(deftest persist-extract-result-replaces-store-name-when-it-duplicates-address
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:store-merchant nil})]
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
                    {:id (java.util.UUID/randomUUID)
                     :store_id nil})
                  stores/resolve-store-from-merchant
                  (fn [_db _supplier-id merchant _opts]
                    (swap! calls assoc :store-merchant merchant)
                    {:store-id nil
                     :store-alias-label nil})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Hippy klupa"
                                                    :store_name "71000 Sarajevo"
                                                    :address "71000 Sarajevo"}
                                         :totals {:total 14.0}
                                         :items [{:raw_label "ITEM" :line_total 14.0}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            stored-merchant (get-in @stored [:raw_extract_json :extraction :merchant])]
        (is (= "Hippy klupa 71000 Sarajevo" (:store_name stored-merchant)))
        (is (= "71000 Sarajevo" (:address stored-merchant)))
        (is (= "Hippy klupa 71000 Sarajevo" (:store_guess @stored)))
        (is (= {:name "Hippy klupa"
                :store_name "Hippy klupa 71000 Sarajevo"
                :address "71000 Sarajevo"}
              (select-keys (:store-merchant @calls) [:name :store_name :address])))))))

(deftest persist-extract-result-replaces-store-name-when-store-name-is-absent
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:store-merchant nil})]
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
                    {:id (java.util.UUID/randomUUID)
                     :store_id nil})
                  stores/resolve-store-from-merchant
                  (fn [_db _supplier-id merchant _opts]
                    (swap! calls assoc :store-merchant merchant)
                    {:store-id nil
                     :store-alias-label nil})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Hippy klupa"
                                                    :address "71000 Sarajevo"}
                                         :totals {:total 14.0}
                                         :items [{:raw_label "ITEM" :line_total 14.0}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})
            stored-merchant (get-in @stored [:raw_extract_json :extraction :merchant])]
        (is (= "Hippy klupa 71000 Sarajevo" (:store_name stored-merchant)))
        (is (= "71000 Sarajevo" (:address stored-merchant)))
        (is (= "Hippy klupa 71000 Sarajevo" (:store_guess @stored)))
        (is (= {:name "Hippy klupa"
                :store_name "Hippy klupa 71000 Sarajevo"
                :address "71000 Sarajevo"}
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

(deftest persist-extract-result-fixes-existing-store-with-address-only-display-name
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-store-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:store-update nil})]
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
                    {:id (java.util.UUID/randomUUID)
                     :store_id mapped-store-id})
                  stores/get-store
                  (fn [_db store-id]
                    (when (= mapped-store-id store-id)
                      {:id store-id
                       :display_name "71000 Sarajevo"}))
                  stores/update-store!
                  (fn [_db store-id data _opts]
                    (swap! calls assoc :store-update data)
                    {:id store-id
                     :display_name (:display_name data)})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Hippy klupa"
                                                    :store_name "71000 Sarajevo"
                                                    :address "71000 Sarajevo"}
                                         :totals {:total 14.0}
                                         :items [{:raw_label "ITEM" :line_total 14.0}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})]
        (is (= {:display_name "Hippy klupa 71000 Sarajevo"
                :address "71000 Sarajevo"}
              (:store-update @calls))
          "existing store with address-only display_name should be updated to supplier + address")))))

(deftest persist-extract-result-fixes-existing-store-when-store-name-is-absent
  (let [receipt-id (java.util.UUID/randomUUID)
        mapped-supplier-id (java.util.UUID/randomUUID)
        supplier-alias-id (java.util.UUID/randomUUID)
        mapped-store-id (java.util.UUID/randomUUID)
        stored (atom nil)
        calls (atom {:store-update nil})]
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
                    {:id (java.util.UUID/randomUUID)
                     :store_id mapped-store-id})
                  stores/get-store
                  (fn [_db store-id]
                    (when (= mapped-store-id store-id)
                      {:id store-id
                       :display_name "71000 Sarajevo"}))
                  stores/update-store!
                  (fn [_db store-id data _opts]
                    (swap! calls assoc :store-update data)
                    {:id store-id
                     :display_name (:display_name data)})

                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Hippy klupa"
                                                    :address "71000 Sarajevo"}
                                         :totals {:total 14.0}
                                         :items [{:raw_label "ITEM" :line_total 14.0}]}}
            _res (extraction/persist-extract-result!
                   ::db
                   receipt-id
                   extract-result
                   {:default-currency "BAM"
                    :places-cfg {}
                    :user-region "BA"
                    :defer-refine? true})]
        (is (= {:display_name "Hippy klupa 71000 Sarajevo"
                :address "71000 Sarajevo"}
              (:store-update @calls))
          "existing store with address-only display_name should be updated even when store_name is absent")))))
