(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-descriptor-test
  (:require
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.test :refer [deftest is]]))

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
              :alias_action :reused
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
              :alias_action :reused
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
              :alias_action :reused
              :source :alias_descriptor_repaired}
            (resolve nil "Zavod za biomedicinsku dijagnostiku" {:merchant nil} {})))
      (is (= 1 (:map-override @calls))))))
