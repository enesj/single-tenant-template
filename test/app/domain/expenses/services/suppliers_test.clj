(ns app.domain.expenses.services.suppliers-test
  "Integration tests for suppliers service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest suppliers-normalization-and-dedupe-test
  (testing "normalize-supplier-key trims, lowercases, strips punctuation"
    (is (= "dm-drogerie" (suppliers/normalize-supplier-key "  DM Drogerie!  "))))
  (testing "normalize-supplier-key folds diacritics"
    (is (= "samon" (suppliers/normalize-supplier-key "Šamon"))))
  (testing "normalize-supplier-key strips legal suffix + trailing location"
    (is (= "hose-komerc"
          (suppliers/normalize-supplier-key "HOŠE-KOMERC d.o.o. Sarajevo")))
    (is (= "hose-komerc"
          (suppliers/normalize-supplier-key "Hoše komerc DOO Sarajevo"))))
  (testing "normalize-supplier-key joins spaced letters"
    (is (= "bingo" (suppliers/normalize-supplier-key "B I N G O")))
    (is (= "hose-komerc" (suppliers/normalize-supplier-key "H O S E-KOMERC"))))
  (testing "find-or-create-supplier! dedupes by normalized key"
    (when-let [db fixtures/*test-db*]
      (let [first (suppliers/find-or-create-supplier! db "Bingo Centar" {:address "Main"})
            second (suppliers/find-or-create-supplier! db "  bingo-centar " {})]
        (is (= (:id (:supplier first)) (:id (:supplier second))))))))

(deftest resolve-or-create-supplier-with-places-test
  (when-let [db fixtures/*test-db*]
    (testing "DB hit skips Places"
      (let [supplier-name (str "Places DB Hit " (java.util.UUID/randomUUID))
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            called? (atom false)
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           (reset! called? true)
                                                           {:places [] :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places!
                       db
                       (str " " supplier-name " ")
                       opts))]
        (is (= (:id supplier) (get-in result [:supplier :id])))
        (is (= :db (:source result)))
        (is (false? @called?))))

    (testing "DB hit via legacy diacritics-stripped key skips Places"
      (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
            display (str "Hoše komerc " suffix)
            legacy-id (java.util.UUID/randomUUID)
            legacy-normalized (str "hoe-komerc-" suffix)
            _ (jdbc/execute-one!
                db
                ["insert into suppliers (id, display_name, normalized_key) values (?, ?, ?)"
                 legacy-id display legacy-normalized]
                {:builder-fn rs/as-unqualified-lower-maps})
            called? (atom false)
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           (reset! called? true)
                                                           {:places [] :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places! db display opts))]
        (is (= legacy-id (get-in result [:supplier :id])))
        (is (= :db (:source result)))
        (is (false? @called?))))

    (testing "Places hit resolves to existing supplier when OCR misses"
      (let [supplier-name "Bingo"
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           {:places [{:name supplier-name
                                                                      :raw {:displayName {:text supplier-name}}}]
                                                            :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places! db "B I N G O O" opts))]
        (is (= (:id supplier) (get-in result [:supplier :id])))
        (is (= :places-api (:source result)))))

    (testing "Places error falls back to OCR create"
      (let [opts {:places-cfg {:api-key "test" :region-code "BA"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           {:places [] :error {:type :places/timeout}})]
                     (suppliers/resolve-or-create-supplier-with-places! db "Fallback Shop" opts))]
        (is (= "Fallback Shop" (get-in result [:supplier :display_name])))
        (is (= :ocr-fallback (:source result)))))

    (testing "Unique violation reselects existing supplier"
      (let [supplier-name (str "Places Unique " (java.util.UUID/randomUUID))
            ;; NOTE: Don't insert a real supplier row here. We want to simulate
            ;; the 23505 path without the legacy-lookup fallback finding the
            ;; row directly from the DB.
            supplier {:id (java.util.UUID/randomUUID)
                      :display_name supplier-name
                      :normalized_key (suppliers/normalize-supplier-key supplier-name)}
            opts {:places-cfg {:api-key "test"}}
            create-throws (fn [_db _data]
                            (throw (java.sql.SQLException. "duplicate" "23505")))
            calls (atom 0)
            lookup (fn [_db _normalized]
                     (swap! calls inc)
                     (if (= 1 @calls) nil supplier))]
        (with-redefs [suppliers/find-by-normalized-key lookup
                      suppliers/service {:create! create-throws}
                      places-api/search-text! (fn [& _]
                                                {:places [] :error {:type :places/unavailable}})]
          (let [result (suppliers/resolve-or-create-supplier-with-places! db supplier-name opts)]
            (is (= (:id supplier) (get-in result [:supplier :id])))
            (is (= :ocr-fallback (:source result)))))))

    (testing "Places hit resolves to legacy supplier with legal suffix key"
      (let [legacy-id (java.util.UUID/randomUUID)
            legacy-display "HOŠE-KOMERC d.o.o. Sarajevo"
            legacy-normalized "hose-komerc-doo-sarajevo"
            _ (jdbc/execute-one!
                db
                ["insert into suppliers (id, display_name, normalized_key) values (?, ?, ?)"
                 legacy-id legacy-display legacy-normalized]
                {:builder-fn rs/as-unqualified-lower-maps})
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           {:places [{:name "Hoše komerc" :raw {}}]
                                                            :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places!
                       db
                       "HUŠE KEMERC d.o.o. Sarajevo"
                       opts))]
        (is (= legacy-id (get-in result [:supplier :id])))
        (is (= legacy-normalized (get-in result [:supplier :normalized_key])))
        (is (= :places-api (:source result)))))

    (testing "Places hit resolves to legacy supplier with diacritics-stripped + legal suffix key"
      (let [legacy-id (java.util.UUID/randomUUID)
            legacy-display "Šamon d.o.o. Sarajevo"
        ;; Simulate the pre-diacritic-folding bug:
        ;; "Š" is dropped by the legacy ASCII whitelist.
            legacy-normalized "amon-doo-sarajevo"
            _ (jdbc/execute-one!
                db
                ["insert into suppliers (id, display_name, normalized_key) values (?, ?, ?)"
                 legacy-id legacy-display legacy-normalized]
                {:builder-fn rs/as-unqualified-lower-maps})
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           {:places [{:name legacy-display :raw {}}]
                                                            :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places!
                       db
                       "SAMON d.o.o. Sarajevo"
                       opts))]
        (is (= legacy-id (get-in result [:supplier :id])))
        (is (= legacy-normalized (get-in result [:supplier :normalized_key])))
        (is (= :places-api (:source result)))))

    (testing "Places hit resolves to existing supplier when OCR includes legal suffix + location"
      (let [{:keys [supplier]} (suppliers/find-or-create-supplier! db "Hoše komerc" {})
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           {:places [{:name "Hoše komerc" :raw {}}]
                                                            :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places!
                       db
                       "HUŠE KEMERC d.o.o. Sarajevo"
                       opts))]
        (is (= (:id supplier) (get-in result [:supplier :id])))
        (is (= :places-api (:source result)))))))
(deftest delete-supplier-blocked-when-expenses-exist
  (testing "delete is blocked when supplier has expenses (FK RESTRICT)"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (str "Delete Supplier Blocked " (java.util.UUID/randomUUID))
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
            payer (th/create-payer! db {:type "cash" :label "Cash"})]
        (expenses/create-expense!
          db
          {:supplier_id supplier-id
           :payer_id (:id payer)
           :purchased_at (java.time.Instant/now)
           :total_amount 10M
           :currency "BAM"
           :items [{:raw_label "Milk" :line_total 10M}]})

        (try
          (suppliers/delete-supplier! db supplier-id)
          (is false "Expected delete to fail due to FK restrict")
          (catch org.postgresql.util.PSQLException e
            (is (= "23503" (.getSQLState e)))))))))

(deftest delete-supplier-succeeds-without-expenses
  (testing "delete succeeds when supplier has no expenses"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (str "Delete Supplier OK " (java.util.UUID/randomUUID))
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
            deleted (suppliers/delete-supplier! db supplier-id)]
        (is deleted)
        (is (nil?
              (jdbc/execute-one!
                db
                ["select id from suppliers where id = ?" supplier-id]
                {:builder-fn rs/as-unqualified-lower-maps})))))))
