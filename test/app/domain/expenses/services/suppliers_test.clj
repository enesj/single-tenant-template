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

(deftest suppliers-dedupe-location-suffix-and-strip-branch-suffix-test
  (when-let [db fixtures/*test-db*]
    (testing "find-or-create dedupes when OCR appends location suffix to existing supplier"
      (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
            canonical-name (str "Apoteke Sarajevo " suffix)
            {:keys [supplier]} (suppliers/find-or-create-supplier! db canonical-name {})
            variant (str canonical-name " Kosevsko Brdo")
            resolved (suppliers/find-or-create-supplier! db variant {})]
        (is (= (:id supplier) (get-in resolved [:supplier :id])))
        (is (true? (:existing? resolved)))))

    (testing "find-or-create strips explicit branch marker suffix (PJ) before create"
      (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
            with-branch (str "Duhanpromet " suffix " PJ 7")
            expected-display (str "Duhanpromet " suffix)
            {:keys [supplier]} (suppliers/find-or-create-supplier! db with-branch {})]
        (is (= expected-display (:display_name supplier)))
        (is (= (suppliers/normalize-supplier-key expected-display)
              (:normalized_key supplier)))))))

(deftest find-or-create-supplier-unescapes-existing-display-name-test
  (when-let [db fixtures/*test-db*]
    (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
          id (java.util.UUID/randomUUID)
          display (str "B&amp;K " suffix " d.o.o. Sarajevo")
          normalized (suppliers/normalize-supplier-key display)
          _ (jdbc/execute-one!
              db
              ["insert into suppliers (id, display_name, normalized_key) values (?, ?, ?)"
               id display normalized]
              {:builder-fn rs/as-unqualified-lower-maps})
          res (suppliers/find-or-create-supplier!
                db
                (str "B&K " suffix " d.o.o. Sarajevo")
                {})]
      (is (= id (get-in res [:supplier :id])))
      (is (= (str "B&K " suffix " d.o.o. Sarajevo")
            (get-in res [:supplier :display_name]))))))

(deftest suppliers-create-update-test
  (testing "address can be set on update without affecting normalized_key"
    (when-let [db fixtures/*test-db*]
      (let [created ((:create! suppliers/service)
                     db
                     {:display_name "Brand X"})
            id (:id created)
            normalized (:normalized_key created)
            updated ((:update! suppliers/service)
                     db
                     id
                     {:address "Street 123"})]
        (is (= id (:id updated)))
        (is (= "Street 123" (:address updated)))
        (is (= normalized (:normalized_key updated)))))))

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
      (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
            legacy-id (java.util.UUID/randomUUID)
            legacy-display (str "HOŠE-KOMERC " suffix " d.o.o. Sarajevo")
            legacy-normalized (str "hose-komerc-" suffix "-doo-sarajevo")
            _ (jdbc/execute-one!
                db
                ["insert into suppliers (id, display_name, normalized_key) values (?, ?, ?)"
                 legacy-id legacy-display legacy-normalized]
                {:builder-fn rs/as-unqualified-lower-maps})
            opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
            result (with-redefs [places-api/search-text! (fn [& _]
                                                           {:places [{:name (str "Hoše komerc " suffix) :raw {}}]
                                                            :error nil})]
                     (suppliers/resolve-or-create-supplier-with-places!
                       db
                       (str "HUŠE KEMERC " suffix " d.o.o. Sarajevo")
                       opts))]
        (is (= legacy-id (get-in result [:supplier :id])))
        (is (= legacy-normalized (get-in result [:supplier :normalized_key])))
        (is (= :places-api (:source result)))))

    (testing "Places hit resolves to legacy supplier with diacritics-stripped + legal suffix key"
      (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
            legacy-id (java.util.UUID/randomUUID)
            legacy-display (str "Šamon " suffix " d.o.o. Sarajevo")
            ;; Simulate the pre-diacritic-folding bug:
            ;; "Š" is dropped by the legacy ASCII whitelist.
            legacy-normalized (str "amon-" suffix "-doo-sarajevo")
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
                       (str "SAMON " suffix " d.o.o. Sarajevo")
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

(deftest resolve-or-create-supplier-with-places-ignores-prefix-only-match-for-short-brands
  (when-let [db fixtures/*test-db*]
    (let [brand-name "B&K"
          opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
          result (with-redefs [places-api/search-text! (fn [& _]
                                                         {:places [{:name "B&K Family DOO"
                                                                    :raw {:displayName {:text "B&K Family DOO"}}}
                                                                   {:name "B&K Wäge- und Anlagentechnik GmbH"
                                                                    :raw {:displayName {:text "B&K Wäge- und Anlagentechnik GmbH"}}}]
                                                          :error nil})]
                   (suppliers/resolve-or-create-supplier-with-places! db brand-name opts))]
      (is (= :ocr-fallback (:source result)))
      (is (= brand-name (get-in result [:supplier :display_name])))
      (is (= (suppliers/normalize-supplier-key brand-name)
            (get-in result [:supplier :normalized_key]))))))

(deftest resolve-or-create-supplier-with-places-ignores-places-branch-suffix-for-supplier
  (when-let [db fixtures/*test-db*]
    (let [supplier-name "AMKO KOMERC"
          opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
          result (with-redefs [places-api/search-text! (fn [& _]
                                                         {:places [{:name "Amko komerc Humska"
                                                                    :raw {:displayName {:text "Amko komerc Humska"}}}
                                                                   {:name "Amko komerc Grbavička Grbavica"
                                                                    :raw {:displayName {:text "Amko komerc Grbavička Grbavica"}}}]
                                                          :error nil})]
                   (suppliers/resolve-or-create-supplier-with-places! db supplier-name opts))]
      (is (= :ocr-fallback (:source result)))
      (is (= supplier-name (get-in result [:supplier :display_name])))
      (is (= (suppliers/normalize-supplier-key supplier-name)
            (get-in result [:supplier :normalized_key]))))))

(deftest resolve-or-create-supplier-with-places-ignores-shorter-places-root-for-qualified-brand
  (when-let [db fixtures/*test-db*]
    (let [supplier-name "New Yorker BH"
          opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
          result (with-redefs [places-api/search-text! (fn [& _]
                                                         {:places [{:name "New Yorker"
                                                                    :raw {:displayName {:text "New Yorker"}}}]
                                                          :error nil})]
                   (suppliers/resolve-or-create-supplier-with-places! db supplier-name opts))]
      (is (= :ocr-fallback (:source result)))
      (is (= supplier-name (get-in result [:supplier :display_name])))
      (is (= (suppliers/normalize-supplier-key supplier-name)
            (get-in result [:supplier :normalized_key]))))))

(deftest resolve-or-create-supplier-with-places-preserves-ocr-brand-name
  (when-let [db fixtures/*test-db*]
    (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
          brand-name (str "LUPRIV PLUS Mostar " suffix)
          legal-name (str brand-name " d.o.o. Sarajevo")
          opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
          result (with-redefs [places-api/search-text! (fn [& _]
                                                         {:places [{:name legal-name
                                                                    :raw {:displayName {:text legal-name}}}]
                                                          :error nil})]
                   (suppliers/resolve-or-create-supplier-with-places! db brand-name opts))]
      (is (= :places-api (:source result)))
      (is (= brand-name (get-in result [:supplier :display_name])))
      (is (= (suppliers/normalize-supplier-key brand-name)
            (get-in result [:supplier :normalized_key]))))))

(deftest resolve-or-create-supplier-with-places-prefers-places-name-for-ocr-typo
  (when-let [db fixtures/*test-db*]
    (let [suffix (subs (str (java.util.UUID/randomUUID)) 0 8)
          ocr-name (str "HESE-KEMERC " suffix)
          places-name (str "HOŠE-KOMERC " suffix)
          opts {:places-cfg {:api-key "test" :region-code "BA" :language-code "bs"}}
          places-calls (atom 0)]
      (with-redefs [places-api/search-text! (fn [& _]
                                              (swap! places-calls inc)
                                              {:places [{:name places-name
                                                         :raw {:displayName {:text places-name}}}]
                                               :error nil})]
        (let [first-res (suppliers/resolve-or-create-supplier-with-places! db ocr-name opts)
              first-id (get-in first-res [:supplier :id])
              second-res (suppliers/resolve-or-create-supplier-with-places! db places-name opts)]
          (is (= :places-api (:source first-res)))
          (is (= places-name (get-in first-res [:supplier :display_name])))
          (is (= (suppliers/normalize-supplier-key places-name)
                (get-in first-res [:supplier :normalized_key])))
          (is (= first-id (get-in second-res [:supplier :id])))
          (is (= :db (:source second-res)))
          (is (= 1 @places-calls)))))))
(deftest delete-supplier-blocked-when-expenses-exist
  (testing "delete is blocked when supplier has expenses (FK RESTRICT)"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (str "Delete Supplier Blocked " (java.util.UUID/randomUUID))
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
            payer (th/create-payer! db {:type "cash" :label "Cash"})
            expense (expenses/create-expense!
                      db
                      {:supplier_id supplier-id
                       :payer_id (:id payer)
                       :purchased_at (java.time.Instant/now)
                       :total_amount 10M
                       :currency "BAM"
                       :items [{:raw_label "Milk" :line_total 10M}]})
            expense-id (:id expense)]
        (try
          (suppliers/delete-supplier! db supplier-id)
          (is false "Expected delete to fail due to FK restrict")
          (catch org.postgresql.util.PSQLException e
            (is (= "23503" (.getSQLState e))))
          (finally
            ;; Prevent residual rows during standalone REPL runs without rollback fixture.
            ;; When wrapped in `with-transaction-rollback`, the expected FK exception
            ;; marks the tx as aborted, so cleanup must be skipped there.
            (when-not (instance? java.sql.Connection db)
              (when expense-id
                (jdbc/execute! db ["delete from expense_items where expense_id = ?" expense-id])
                (jdbc/execute! db ["delete from expenses where id = ?" expense-id]))
              (jdbc/execute! db ["delete from suppliers where id = ?" supplier-id])
              (jdbc/execute! db ["delete from payers where id = ?" (:id payer)]))))))))

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
