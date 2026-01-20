(ns app.domain.expenses.services.articles-test
  "Integration tests for articles service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is use-fixtures]])
  (:import
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (java.time.Instant/now))

(deftest articles-batch-create-aliases-dedupes-and-skips-invalid
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "AliasBatch Supplier " (UUID/randomUUID)) {}))
          article (articles/create-article! db {:canonical_name (str "AliasBatch Article " (UUID/randomUUID))})
          result (articles/batch-create-aliases!
                   db
                   {:supplier-id (:id supplier)
                    :article-id (:id article)
                    :raw-labels [" Milk " "MILK" "" "##" "A"]})]
      (is (= 1 (count (:created result))))
      (is (= "milk" (:raw_label_normalized (first (:created result)))))
      (is (some #(= :duplicate (:reason %)) (:skipped result)))
      (is (some #(= :blank (:reason %)) (:skipped result)))
      (is (some #(= :normalizes-to-blank (:reason %)) (:skipped result)))
      (is (some #(= :too-short (:reason %)) (:skipped result))))))

(deftest articles-batch-create-aliases-conflict-and-reassign
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "AliasConflict Supplier " (UUID/randomUUID)) {}))
          article-a (articles/create-article! db {:canonical_name (str "AliasConflict A " (UUID/randomUUID))})
          article-b (articles/create-article! db {:canonical_name (str "AliasConflict B " (UUID/randomUUID))})
          _ (articles/batch-create-aliases!
              db
              {:supplier-id (:id supplier)
               :article-id (:id article-a)
               :raw-labels ["MILK"]})
          conflict (articles/batch-create-aliases!
                     db
                     {:supplier-id (:id supplier)
                      :article-id (:id article-b)
                      :raw-labels ["MILK"]})
          reassigned (articles/batch-create-aliases!
                       db
                       {:supplier-id (:id supplier)
                        :article-id (:id article-b)
                        :raw-labels ["MILK"]
                        :allow-reassign? true})]
      (is (= 1 (count (:conflicts conflict))))
      (is (empty? (:reassigned conflict)))
      (is (= 1 (count (:reassigned reassigned))))
      (is (empty? (:conflicts reassigned))))))

(deftest articles-map-alias-to-article-makes-lookup-work
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "MapItem Supplier " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          article (articles/create-article! db {:canonical_name (str "MapItem Article " (UUID/randomUUID))})
          expense (expenses/create-expense!
                    db
                    {:supplier_id (:id supplier)
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "1.00")
                     :currency "BAM"}
                    [{:raw_label "MILK" :line_total (bigdec "1.00")}])
          alias-id (-> expense :items first :alias_id)]
      (is (uuid? alias-id))

      (aliases/map-alias-to-article! db alias-id (:id article))

      (is (= (:id article)
             (:id (articles/find-article-by-alias db (:id supplier) "MILK")))))))

(deftest articles-list-unmapped-aliases-filters-by-supplier
  (when-let [db fixtures/*test-db*]
    (let [supplier-a (:supplier (suppliers/find-or-create-supplier! db (str "Unmapped Supplier A " (UUID/randomUUID)) {}))
          supplier-b (:supplier (suppliers/find-or-create-supplier! db (str "Unmapped Supplier B " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          _exp-a (expenses/create-expense!
                  db
                  {:supplier_id (:id supplier-a)
                   :payer_id (:id payer)
                   :purchased_at (now)
                   :total_amount (bigdec "1.00")
                   :currency "BAM"}
                  [{:raw_label "A1" :line_total (bigdec "1.00")}])
          _exp-b (expenses/create-expense!
                   db
                   {:supplier_id (:id supplier-b)
                    :payer_id (:id payer)
                    :purchased_at (now)
                    :total_amount (bigdec "1.00")
                    :currency "BAM"}
                   [{:raw_label "B1" :line_total (bigdec "1.00")}])
          rows (aliases/list-unmapped-aliases db {:supplier-id (:id supplier-a) :limit 50 :offset 0})]
      (is (= 1 (count rows)))
      (is (= (:id supplier-a) (:supplier_id (first rows))))
      (is (= "A1" (:raw_label (first rows))))
      (is (string? (:supplier_display_name (first rows)))))))
