(ns app.domain.backend.expenses.handlers.search-test
  (:require
    [app.domain.backend.expenses.handlers.search :as search]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
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