(ns app.domain.backend.expenses.handlers.search-test
  (:require
    [app.domain.backend.expenses.handlers.search :as search]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]))

(deftest user-search-handler-passes-tenant-scope-to-all-searches
  (let [db :mock-db
        user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        seen (atom {})
        handler (search/user-search-handler db)
        record-scope! (fn [k expected]
                        (fn [_db _term _limit tenant-id*]
                          (swap! seen assoc k tenant-id*)
                          (is (= expected tenant-id*))
                          []))]
    (with-redefs [h/get-user-id (constantly user-id)
                  h/ensure-role (constantly nil)
                  h/get-tenant-id (constantly tenant-id)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})
                  search/search-payers (record-scope! :payers tenant-id)
                  search/search-expense-cats (record-scope! :expense-cats tenant-id)
                  search/search-suppliers (record-scope! :suppliers tenant-id)
                  search/search-stores (record-scope! :stores tenant-id)
                  search/search-articles (record-scope! :articles tenant-id)
                  search/search-categories (record-scope! :categories tenant-id)
                  search/search-subcategories (record-scope! :subcategories tenant-id)
                  search/search-manufacturers (record-scope! :manufacturers tenant-id)
                  search/search-cities (record-scope! :cities tenant-id)]
      (let [response (handler {:query-params {"q" "biblio"}})]
        (is (= 200 (:status response)))
        (is (= #{:payers
                 :expense-cats
                 :suppliers
                 :stores
                 :articles
                 :categories
                 :subcategories
                 :manufacturers
                 :cities}
              (set (keys @seen))))))))

(deftest admin-search-handler-keeps-global-scope
  (let [db :mock-db
        seen (atom {})
        handler (search/admin-search-handler db)
        record-scope! (fn [k]
                        (fn [_db _term _limit tenant-id]
                          (swap! seen assoc k tenant-id)
                          (is (nil? tenant-id))
                          []))]
    (with-redefs [h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})
                  search/search-payers (record-scope! :payers)
                  search/search-expense-cats (record-scope! :expense-cats)
                  search/search-suppliers (record-scope! :suppliers)
                  search/search-stores (record-scope! :stores)
                  search/search-articles (record-scope! :articles)
                  search/search-categories (record-scope! :categories)
                  search/search-subcategories (record-scope! :subcategories)
                  search/search-manufacturers (record-scope! :manufacturers)
                  search/search-cities (record-scope! :cities)]
      (let [response (handler {:query-params {"q" "biblio"}})]
        (is (= 200 (:status response)))
        (is (= #{:payers
                 :expense-cats
                 :suppliers
                 :stores
                 :articles
                 :categories
                 :subcategories
                 :manufacturers
                 :cities}
              (set (keys @seen))))))))

(deftest search-suppliers-adds-tenant-expense-and-receipt-boundary
  (let [tenant-id (java.util.UUID/randomUUID)]
    (with-redefs [sql/format identity
                  jdbc/execute! (fn [_db query _opts] query)]
      (let [query (#'search/search-suppliers :mock-db "biblio" 5 tenant-id)
            query-str (pr-str query)]
        (is (re-find #":r\.tenant_id" query-str))
        (is (re-find #"supplier_aliases" query-str))
        (is (re-find #"store_aliases" query-str))
        (is (re-find #":e\.supplier_id" query-str))))))

(deftest search-articles-adds-tenant-expense-item-boundary
  (let [tenant-id (java.util.UUID/randomUUID)]
    (with-redefs [sql/format identity
                  jdbc/execute! (fn [_db query _opts] query)]
      (let [query (#'search/search-articles :mock-db "mlijeko" 5 tenant-id)
            query-str (pr-str query)]
        (is (re-find #"expense_items" query-str))
        (is (re-find #"article_aliases" query-str))
        (is (re-find #":ei\.tenant_id" query-str))))))

(deftest quick-add-search-handler-passes-supplier-filter-to-store-search
  (let [db :mock-db
        user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        handler (search/quick-add-search-handler db)]
    (with-redefs-fn {#'h/get-user-id (constantly user-id)
                     #'h/ensure-role (constantly nil)
                     #'h/get-tenant-id (constantly tenant-id)
                     #'h/json-response (fn [body & [status]]
                                         {:status (or status 200)
                                          :body body})
                     #'search/quick-add-search-stores (fn [db* term limit supplier-id*]
                                                        (is (= db db*))
                                                        (is (= "kon" term))
                                                        (is (= 8 limit))
                                                        (is (= supplier-id supplier-id*))
                                                        [{:id 1 :label "Konzum Centar" :supplier_id supplier-id*}])}
      (fn []
        (let [response (handler {:query-params {"type" "store"
                                                "q" "kon"
                                                "supplier_id" (str supplier-id)}})]
          (is (= 200 (:status response)))
          (is (= [{:id 1 :label "Konzum Centar" :supplier_id supplier-id}]
                (get-in response [:body :results]))))))))

(deftest quick-add-search-stores-excludes-supplierless-stores-and-honors-supplier-filter
  (let [supplier-id (java.util.UUID/randomUUID)]
    (with-redefs [sql/format identity
                  jdbc/execute! (fn [_db query _opts] query)]
      (let [query (#'search/quick-add-search-stores :mock-db "kon" 5 supplier-id)
            query-str (pr-str query)]
        (is (re-find #":is-not :st\.supplier_id nil" query-str))
        (is (re-find #":= :st\.supplier_id" query-str))
        (is (re-find #":supplier_id" query-str))
        (is (re-find #":supplier_display_name" query-str))))))

(deftest quick-add-article-last-prices-honors-selected-supplier
  (let [tenant-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        article-id (java.util.UUID/randomUUID)]
    (with-redefs [sql/format identity
                  jdbc/execute! (fn [_db query _opts] query)]
      (let [query (#'search/quick-add-article-last-prices :mock-db [article-id] tenant-id supplier-id)
            query-str (pr-str query)]
        (is (str/includes? query-str ":e.tenant_id"))
        (is (str/includes? query-str ":aa.article_id"))
        (is (str/includes? query-str ":e.supplier_id"))
        (is (str/includes? query-str ":supplier_display_name"))
        (is (str/includes? query-str ":article_id"))
        (is (str/includes? query-str ":unit_price"))))))

(deftest quick-add-search-articles-prefers-supplier-price-and-falls-back-to-global
  (let [tenant-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        article-id-1 (java.util.UUID/randomUUID)
        article-id-2 (java.util.UUID/randomUUID)]
    (with-redefs-fn {#'jdbc/execute! (fn [_db _query _opts]
                                       [{:id article-id-1 :label "Milk"}
                                        {:id article-id-2 :label "Bread"}])
                     #'search/quick-add-article-last-prices (fn [_db article-ids tenant-id* supplier-id*]
                                                              (is (= [article-id-1 article-id-2] article-ids))
                                                              (is (= tenant-id tenant-id*))
                                                              (if supplier-id*
                                                                [{:article_id article-id-1
                                                                  :unit_price 4.25M
                                                                  :supplier_display_name "BINGO"}]
                                                                [{:article_id article-id-1
                                                                  :unit_price 4.10M
                                                                  :supplier_display_name "BINGO"}
                                                                 {:article_id article-id-2
                                                                  :unit_price 3.60M
                                                                  :supplier_display_name "KONZUM"}]))}
      (fn []
        (is (= [{:id article-id-1
                 :label "Milk"
                 :last_price 4.25M
                 :last_price_source "supplier"
                 :last_price_supplier_display_name "BINGO"}
                {:id article-id-2
                 :label "Bread"
                 :last_price 3.60M
                 :last_price_source "global"
                 :last_price_supplier_display_name "KONZUM"}]
              (#'search/quick-add-search-articles :mock-db "mi" 8 tenant-id supplier-id)))))))

(deftest related-for-supplier-includes-all-supplier-stores-even-without-expenses
  (let [supplier-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)]
    (with-redefs [sql/format identity
                  jdbc/execute! (fn [_db query _opts] query)]
      (let [result (#'search/related-for-supplier :mock-db supplier-id 5 tenant-id)
            stores-query-str (pr-str (:stores result))]
        (is (clojure.string/includes? stores-query-str ":st.supplier_id"))
        (is (clojure.string/includes? stores-query-str "[:and [:= :e.store_id :st.id]"))
        (is (clojure.string/includes? stores-query-str ":e.tenant_id"))
        (is (clojure.string/includes? stores-query-str ":supplier_id"))
        (is (clojure.string/includes? stores-query-str ":left-join"))
        (is (clojure.string/includes? stores-query-str ":st.display_name"))
        (is (clojure.string/includes? stores-query-str ":e.total_amount"))))))

(deftest quick-add-search-handler-all-tags-results-and-filters-stores
  (let [db :mock-db
        user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        handler (search/quick-add-search-handler db)]
    (with-redefs-fn {#'h/get-user-id (constantly user-id)
                     #'h/ensure-role (constantly nil)
                     #'h/get-tenant-id (constantly tenant-id)
                     #'h/json-response (fn [body & [status]] {:status (or status 200) :body body})
                     #'search/quick-add-search-suppliers (fn [_db _term _limit] [{:id 1 :label "Bingo"}])
                     #'search/quick-add-search-stores (fn [_db _term _limit supplier-id*]
                                                        (is (= supplier-id supplier-id*))
                                                        [{:id 2 :label "Bingo Store" :supplier_id supplier-id*}])
                     #'search/quick-add-search-expense-categories (fn [_db _term _limit _tenant-id] [])
                     #'search/quick-add-search-articles (fn [_db _term _limit tenant-id* supplier-id*]
                                                          (is (= tenant-id tenant-id*))
                                                          (is (= supplier-id supplier-id*))
                                                          [{:id 3
                                                            :label "Milk"
                                                            :last_price 4.25
                                                            :last_price_source "supplier"
                                                            :last_price_supplier_display_name "BINGO"}])}
      (fn []
        (let [response (handler {:query-params {"type" "all"
                                                "q" "bi"
                                                "supplier_id" (str supplier-id)}})
              results (get-in response [:body :results])]
          (is (= 200 (:status response)))
          (is (= #{{:id 1 :label "Bingo" :entity_type "supplier"}
                   {:id 2 :label "Bingo Store" :supplier_id supplier-id :entity_type "store"}
                   {:id 3
                    :label "Milk"
                    :last_price 4.25
                    :last_price_source "supplier"
                    :last_price_supplier_display_name "BINGO"
                    :entity_type "article"}}
                (set results))))))))

(deftest admin-related-handler-uses-global-scope
  (let [db :mock-db
        entity-id (java.util.UUID/randomUUID)
        handler (search/admin-related-handler db)]
    (with-redefs [h/try-parse-uuid (fn [value]
                                     (is (= (str entity-id) value))
                                     entity-id)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})
                  search/related-for-article (fn [db* id limit tenant-id]
                                               (is (= db db*))
                                               (is (= entity-id id))
                                               (is (= 8 limit))
                                               (is (nil? tenant-id))
                                               {:detail {:canonical_name "Coffee"}})]
      (let [response (handler {:query-params {"type" "articles"
                                              "id" (str entity-id)}})]
        (is (= 200 (:status response)))
        (is (= {:related {:detail {:canonical_name "Coffee"}}
                :type "articles"
                :id (str entity-id)}
              (:body response)))))))

(deftest user-related-handler-passes-tenant-scope
  (let [db :mock-db
        user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        handler (search/user-related-handler db)]
    (with-redefs [h/get-user-id (constantly user-id)
                  h/ensure-role (constantly nil)
                  h/get-tenant-id (constantly tenant-id)
                  h/try-parse-uuid (constantly supplier-id)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})
                  search/related-for-supplier (fn [db* id limit tenant-id*]
                                                (is (= db db*))
                                                (is (= supplier-id id))
                                                (is (= 8 limit))
                                                (is (= tenant-id tenant-id*))
                                                {:stores []
                                                 :articles []})]
      (let [response (handler {:query-params {"type" "suppliers"
                                              "id" (str supplier-id)}})]
        (is (= 200 (:status response)))
        (is (= {:related {:stores []
                          :articles []}
                :type "suppliers"
                :id (str supplier-id)}
              (:body response)))))))

(deftest related-handler-rejects-invalid-id
  (testing "admin related handler returns 400 for invalid ids"
    (let [handler (search/admin-related-handler :mock-db)]
      (with-redefs [h/try-parse-uuid (constantly nil)
                    h/json-response (fn [body & [status]]
                                      {:status (or status 200)
                                       :body body})]
        (is (= {:status 400
                :body {:error "Missing or invalid id"}}
              (handler {:query-params {"type" "articles"
                                       "id" "not-a-uuid"}})))))))