(ns app.domain.expenses.services.articles-test
  "Integration tests for articles service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is use-fixtures]]
    [next.jdbc :as jdbc])
  (:import
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (java.time.Instant/now))

(defn- insert-receipt!
  [db {:keys [original-filename supplier-alias-id store-alias-id created-at]}]
  (let [receipt-id (UUID/randomUUID)
        file-hash (str (UUID/randomUUID))
        storage-key (str "test/receipts/" receipt-id)]
    (jdbc/execute-one!
      db
      ["insert into receipts (id, storage_key, file_hash, original_filename, supplier_alias_id, store_alias_id, created_at) values (?, ?, ?, ?, ?, ?, coalesce(?, now()))"
       receipt-id
       storage-key
       file-hash
       original-filename
       supplier-alias-id
       store-alias-id
       created-at])
    receipt-id))

(defn- insert-category!
  [db name]
  (let [category-id (UUID/randomUUID)]
    (jdbc/execute-one! db ["insert into categories (id, name) values (?, ?)" category-id name])
    category-id))

(defn- insert-subcategory!
  [db category-id name]
  (let [subcategory-id (UUID/randomUUID)]
    (jdbc/execute-one!
      db
      ["insert into subcategories (id, category_id, name) values (?, ?, ?)"
       subcategory-id category-id name])
    subcategory-id))

(defn- insert-manufacturer!
  [db display-name normalized-key]
  (let [manufacturer-id (UUID/randomUUID)]
    (jdbc/execute-one!
      db
      ["insert into manufacturers (id, display_name, normalized_key) values (?, ?, ?)"
       manufacturer-id display-name normalized-key])
    manufacturer-id))

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

(deftest articles-list-related-records-resolves-alias-linked-expense-items
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "Related Alias Supplier " (UUID/randomUUID)) {}))
          payer (th/create-payer! db {:type "cash" :label "Cash"})
          store (stores/find-or-create-store!
                  db
                  {:supplier_id (:id supplier)
                   :display_name (str "Related Alias Store " (UUID/randomUUID))})
          receipt-id (insert-receipt! db {:original-filename "related-alias.pdf"})
          article (articles/create-article! db {:canonical_name (str "Related Alias Article " (UUID/randomUUID))})
          expense (expenses/create-expense!
                    db
                    {:supplier_id (:id supplier)
                     :store_id (:id store)
                     :receipt_id receipt-id
                     :payer_id (:id payer)
                     :purchased_at (now)
                     :total_amount (bigdec "4.20")
                     :currency "BAM"}
                    [{:raw_label "MILK" :line_total (bigdec "4.20")}])
          expense-item (-> expense :items first)
          alias-id (:alias_id expense-item)]
      (is (uuid? alias-id))
      (is (nil? (:article_id expense-item)))

      (aliases/map-alias-to-article! db alias-id (:id article))

      (let [related-expenses (articles/list-related-records db (:id article) {:type "expenses"})
            related-receipts (articles/list-related-records db (:id article) {:type "receipts"})
            related-stores (articles/list-related-records db (:id article) {:type "stores"})]
        (is (= [(:id expense)] (mapv :id related-expenses)))
        (is (= [receipt-id] (mapv :id related-receipts)))
        (is (= [(:id store)] (mapv :id related-stores)))))))

(deftest articles-list-related-records-resolves-receipts-and-stores-via-alias-supplier-links
  (when-let [db fixtures/*test-db*]
    (let [supplier (:supplier (suppliers/find-or-create-supplier! db (str "Related Alias Path Supplier " (UUID/randomUUID)) {}))
          store-a (stores/find-or-create-store!
                    db
                    {:supplier_id (:id supplier)
                     :display_name (str "Alias Path Store A " (UUID/randomUUID))})
          store-b (stores/find-or-create-store!
                    db
                    {:supplier_id (:id supplier)
                     :display_name (str "Alias Path Store B " (UUID/randomUUID))})
          article (articles/create-article! db {:canonical_name (str "Related Alias Path Article " (UUID/randomUUID))})
          _ (articles/create-alias! db (:id supplier) (str "ALIAS-PATH-" (UUID/randomUUID)) (:id article))
          supplier-alias (supplier-aliases/find-or-create-alias! db (str "Receipt Supplier Alias " (UUID/randomUUID)))
          _ (supplier-aliases/map-alias-to-supplier! db (:id supplier-alias) (:id supplier))
          store-alias (store-aliases/find-or-create-alias! db (str "Receipt Store Alias " (UUID/randomUUID)))
          _ (store-aliases/map-alias-to-store! db (:id store-alias) (:id store-b))
          receipt-via-supplier (insert-receipt!
                                 db
                                 {:original-filename "via-supplier-alias.pdf"
                                  :supplier-alias-id (:id supplier-alias)
                                  :created-at (java.time.Instant/parse "2026-01-01T10:00:00Z")})
          receipt-via-store (insert-receipt!
                              db
                              {:original-filename "via-store-alias.pdf"
                               :store-alias-id (:id store-alias)
                               :created-at (java.time.Instant/parse "2026-01-02T10:00:00Z")})
          related-expenses (articles/list-related-records db (:id article) {:type "expenses"})
          related-receipts (articles/list-related-records db (:id article) {:type "receipts"})
          related-stores (articles/list-related-records db (:id article) {:type "stores"})]
      (is (empty? related-expenses))
      (is (= [receipt-via-store receipt-via-supplier] (mapv :id related-receipts)))
      (is (every? #(= (:display_name supplier) %) (mapv :supplier_display_name related-receipts)))
      (is (= [(:id store-a) (:id store-b)] (mapv :id related-stores))))))

(deftest articles-list-related-records-supports-manufacturers-and-subcategories
  (when-let [db fixtures/*test-db*]
    (let [category-id (insert-category! db (str "Category " (UUID/randomUUID)))
          subcategory-id (insert-subcategory! db category-id (str "Subcategory " (UUID/randomUUID)))
          manufacturer-name (str "Manufacturer " (UUID/randomUUID))
          manufacturer-id (insert-manufacturer! db manufacturer-name (articles/normalize-article-key manufacturer-name))
          article (articles/create-article! db {:canonical_name (str "Article " (UUID/randomUUID))
                                                :manufacturer_id manufacturer-id
                                                :subcategory_id subcategory-id})
          related-manufacturers (articles/list-related-records db (:id article) {:type "manufacturers"})
          related-subcategories (articles/list-related-records db (:id article) {:type "subcategories"})]
      (is (= [manufacturer-id] (mapv :id related-manufacturers)))
      (is (= [subcategory-id] (mapv :id related-subcategories)))
      (is (= manufacturer-name (:display_name (first related-manufacturers))))
      (is (= category-id (:category_id (first related-subcategories)))))))

(deftest articles-list-related-records-invalid-type-includes-new-options
  (when-let [db fixtures/*test-db*]
    (let [article (articles/create-article! db {:canonical_name (str "Invalid Type Article " (UUID/randomUUID))})]
      (try
        (articles/list-related-records db (:id article) {:type "invalid"})
        (is false "Expected invalid type error")
        (catch clojure.lang.ExceptionInfo e
          (is (= 400 (:status (ex-data e))))
          (is (re-find #"manufacturers, subcategories" (.getMessage e))))))))
