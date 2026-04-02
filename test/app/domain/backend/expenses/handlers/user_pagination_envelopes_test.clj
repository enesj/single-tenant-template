(ns app.domain.backend.expenses.handlers.user-pagination-envelopes-test
  (:require
    [app.domain.backend.expenses.handlers.user-articles :as user-articles]
    [app.domain.backend.expenses.handlers.user-categories :as user-categories]
    [app.domain.backend.expenses.handlers.user-cities :as user-cities]
    [app.domain.backend.expenses.handlers.user-expense-categories :as user-expense-categories]
    [app.domain.backend.expenses.handlers.user-expenses.crud :as user-expenses-crud]
    [app.domain.backend.expenses.handlers.user-expenses.expense-items :as expense-items]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.handlers.user-expenses.reference-data :as reference-data]
    [app.domain.backend.expenses.handlers.user-expenses.supplier-aliases :as supplier-aliases-handler]
    [app.domain.backend.expenses.handlers.user-expenses.supplier-detail :as supplier-detail]
    [app.domain.backend.expenses.handlers.user-manufacturers :as user-manufacturers]
    [app.domain.backend.expenses.handlers.user-store-aliases :as user-store-aliases]
    [app.domain.backend.expenses.handlers.user-stores :as user-stores]
    [app.domain.backend.expenses.handlers.user-subcategories :as user-subcategories]
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.categories :as categories]
    [app.domain.backend.expenses.services.cities :as cities]
    [app.domain.backend.expenses.services.expense-categories :as expense-categories]
    [app.domain.backend.expenses.services.manufacturers :as manufacturers]
    [app.domain.backend.expenses.services.payer-types :as payer-types]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores.service :as stores-service]
    [app.domain.backend.expenses.services.subcategories :as subcategories]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.time Instant]
    [java.util UUID]))

(def db :test-db)

(defn- req
  [role query-params]
  {:identity {:id (UUID/randomUUID)
              :role role}
   :query-params query-params})

(deftest reference-data-list-handlers-return-standard-envelope
  (let [suppliers-opts (atom nil)
        suppliers-count-opts (atom nil)
        payers-opts (atom nil)
        payers-count-opts (atom nil)
        payer-types-opts (atom nil)
        payer-types-count-opts (atom nil)]
    (with-redefs [suppliers/list-suppliers
                  (fn [_db opts]
                    (reset! suppliers-opts opts)
                    [{:id (UUID/randomUUID)}])
                  suppliers/count-suppliers
                  (fn [_db opts]
                    (reset! suppliers-count-opts opts)
                    10)
                  payers/service
                  {:list (fn [_db opts]
                           (reset! payers-opts opts)
                           [{:id (UUID/randomUUID)}])
                   :count (fn [_db opts]
                            (reset! payers-count-opts opts)
                            7)}
                  payer-types/service
                  {:list (fn [_db opts]
                           (reset! payer-types-opts opts)
                           [{:id (UUID/randomUUID)}])
                   :count (fn [_db opts]
                            (reset! payer-types-count-opts opts)
                            5)}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (testing "suppliers clamps invalid pagination and includes envelope"
        (let [handler (reference-data/list-suppliers-handler db)
              resp (handler (req "member" {:limit "-5"
                                           :offset "-2"
                                           :search "milk"}))]
          (is (= 200 (:status resp)))
          (is (= 10 (get-in resp [:body :total])))
          (is (= 1 (get-in resp [:body :limit])))
          (is (= 0 (get-in resp [:body :offset])))
          (is (= {:limit 1 :offset 0 :search "milk"} @suppliers-opts))
          (is (= {:search "milk"} @suppliers-count-opts))))

      (testing "payers passes parsed pagination/search and includes envelope"
        (let [handler (reference-data/list-payers-handler db)
              resp (handler (req "member" {:limit "3"
                                           :offset "4"
                                           :search "cash"}))]
          (is (= 200 (:status resp)))
          (is (= 7 (get-in resp [:body :total])))
          (is (= 3 (get-in resp [:body :limit])))
          (is (= 4 (get-in resp [:body :offset])))
          (is (= {:limit 3 :offset 4 :search "cash"} @payers-opts))
          (is (= {:search "cash" :tenant-id nil} @payers-count-opts))))

      (testing "payer types clamps non-positive limit/offset and includes envelope"
        (let [handler (reference-data/list-payer-types-handler db)
              resp (handler (req "member" {:limit "0"
                                           :offset "-1"
                                           :search "type"}))]
          (is (= 200 (:status resp)))
          (is (= 5 (get-in resp [:body :total])))
          (is (= 1 (get-in resp [:body :limit])))
          (is (= 0 (get-in resp [:body :offset])))
          (is (= {:limit 1 :offset 0 :search "type"} @payer-types-opts))
          (is (= {:search "type" :tenant-id nil} @payer-types-count-opts)))))))

(deftest supplier-detail-and-supplier-aliases-list-handlers-return-standard-envelope
  (let [article-aliases-opts (atom nil)
        article-aliases-count-opts (atom nil)
        supplier-aliases-opts (atom nil)
        supplier-aliases-count-opts (atom nil)
        supplier-id (UUID/randomUUID)]
    (with-redefs [article-aliases/list-article-aliases
                  (fn [_db opts]
                    (reset! article-aliases-opts opts)
                    [{:id (UUID/randomUUID)}])
                  article-aliases/count-article-aliases
                  (fn [_db opts]
                    (reset! article-aliases-count-opts opts)
                    4)
                  supplier-aliases/list-supplier-aliases
                  (fn [_db opts]
                    (reset! supplier-aliases-opts opts)
                    [{:id (UUID/randomUUID)}])
                  supplier-aliases/count-supplier-aliases
                  (fn [_db opts]
                    (reset! supplier-aliases-count-opts opts)
                    9)
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (testing "supplier detail article aliases includes pagination envelope"
        (let [handler (supplier-detail/list-article-aliases-handler db)
              resp (handler (req "member" {:limit "7"
                                           :offset "2"
                                           :supplier_id (str supplier-id)}))]
          (is (= 200 (:status resp)))
          (is (= 4 (get-in resp [:body :total])))
          (is (= 7 (get-in resp [:body :limit])))
          (is (= 2 (get-in resp [:body :offset])))
          (is (= {:limit 7 :offset 2 :supplier_id supplier-id} @article-aliases-opts))
          (is (= @article-aliases-opts @article-aliases-count-opts))))

      (testing "supplier aliases list includes pagination envelope and parsed filters"
        (let [handler (supplier-aliases-handler/list-supplier-aliases-handler db)
              resp (handler (req "admin" {:limit "-10"
                                          :offset "-1"
                                          :search "sup"
                                          :supplier_id (str supplier-id)
                                          :unmapped-only "true"}))]
          (is (= 200 (:status resp)))
          (is (= 9 (get-in resp [:body :total])))
          (is (= 1 (get-in resp [:body :limit])))
          (is (= 0 (get-in resp [:body :offset])))
          (is (= {:limit 1
                  :offset 0
                  :search "sup"
                  :supplier_id supplier-id
                  :unmapped-only true}
                @supplier-aliases-opts))
          (is (= @supplier-aliases-opts @supplier-aliases-count-opts)))))))

(deftest supplier-detail-and-subcategories-date-filters-flow-through
  (let [article-aliases-opts (atom nil)
        article-aliases-count-opts (atom nil)
        subcategories-opts (atom nil)
        subcategories-count-opts (atom nil)
        from-ts (Instant/parse "2026-04-01T22:00:00Z")
        to-ts (Instant/parse "2026-04-02T21:59:59.999Z")]
    (with-redefs [article-aliases/list-article-aliases
                  (fn [_db opts]
                    (reset! article-aliases-opts opts)
                    [{:id (UUID/randomUUID)}])
                  article-aliases/count-article-aliases
                  (fn [_db opts]
                    (reset! article-aliases-count-opts opts)
                    0)
                  subcategories/service
                  {:list (fn [_db opts]
                           (reset! subcategories-opts opts)
                           [{:id (UUID/randomUUID)}])
                   :count (fn [_db opts]
                            (reset! subcategories-count-opts opts)
                            0)}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (testing "supplier detail article aliases forwards created-at extra filters"
        (let [handler (supplier-detail/list-article-aliases-handler db)
              resp (handler (req "owner" {:created-at-from "2026-04-01T22:00:00Z"
                                          :created-at-to "2026-04-02T21:59:59.999Z"}))]
          (is (= 200 (:status resp)))
          (is (= [[:>= :aa.created_at from-ts]
                  [:<= :aa.created_at to-ts]]
                (:extra-filters @article-aliases-opts)))
          (is (= @article-aliases-opts @article-aliases-count-opts))))

      (testing "subcategories forwards created-at extra filters using table alias"
        (let [handler (user-subcategories/list-subcategories-handler db)
              resp (handler (req "owner" {:created-at-from "2026-04-01T22:00:00Z"
                                          :created-at-to "2026-04-02T21:59:59.999Z"}))]
          (is (= 200 (:status resp)))
          (is (= [[:>= :sc.created_at from-ts]
                  [:<= :sc.created_at to-ts]]
                (:extra-filters @subcategories-opts)))
          (is (= {:search nil
                  :extra-filters [[:>= :sc.created_at from-ts]
                                  [:<= :sc.created_at to-ts]]}
                @subcategories-count-opts)))))))

(deftest expenses-list-handler-forwards-currency-and-total-range-filters
  (let [list-opts (atom nil)
        count-opts (atom nil)]
    (with-redefs [user-expenses/list-user-expenses
                  (fn [_db _tenant-id _user-id opts]
                    (reset! list-opts opts)
                    [{:id (UUID/randomUUID)}])
                  user-expenses/count-user-expenses
                  (fn [_db _tenant-id _user-id opts]
                    (reset! count-opts opts)
                    1)
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (let [handler (user-expenses-crud/list-expenses-handler db)
            resp (handler (req "owner" {:currency "EUR"
                                        :total-amount-min "10.25"
                                        :total-amount-max "25.50"}))]
        (is (= 200 (:status resp)))
        (is (= 1 (get-in resp [:body :total])))
        (is (= "EUR" (:currency @list-opts)))
        (is (= 10.25M (:total-amount-min @list-opts)))
        (is (= 25.50M (:total-amount-max @list-opts)))
        (is (= @list-opts @count-opts))))))

(deftest expense-items-list-handler-includes-standard-envelope
  (let [list-sql (atom nil)
        count-sql (atom nil)]
    (with-redefs [jdbc/execute! (fn [_db sql _opts]
                                  (reset! list-sql sql)
                                  [{:id (UUID/randomUUID)}])
                  jdbc/execute-one! (fn [_db sql _opts]
                                      (reset! count-sql sql)
                                      {:total 9})
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (let [handler (expense-items/list-expense-items-handler db)
            resp (handler (req "admin" {:limit "-4"
                                        :offset "-9"
                                        :search "milk"
                                        :raw-label "jagoda"
                                        :qty-min "0.35"
                                        :qty-max "0.35"
                                        :expense-purchased-at-from "2026-03-01T00:00:00Z"}))]
        (is (= 200 (:status resp)))
        (is (= 9 (get-in resp [:body :total])))
        (is (= 1 (get-in resp [:body :limit])))
        (is (= 0 (get-in resp [:body :offset])))
        (is (vector? (get-in resp [:body :data])))
        (is (= 2 (count (filter #(= 0.35M %) (rest @list-sql)))))
        (is (some #(= "%jagoda%" %) (rest @list-sql)))
        (is (some #(= "%milk%" %) (rest @list-sql)))
        (is (some #(instance? java.time.Instant %) (rest @list-sql)))
        (is (= 2 (count (filter #(= 0.35M %) (rest @count-sql)))))
        (is (some #(= "%jagoda%" %) (rest @count-sql)))
        (is (some #(= "%milk%" %) (rest @count-sql)))
        (is (some #(instance? java.time.Instant %) (rest @count-sql)))))))

(deftest user-articles-list-handlers-return-standard-envelope
  (let [articles-opts (atom nil)
        articles-search (atom nil)
        unmapped-opts (atom nil)
        unmapped-count-opts (atom nil)
        supplier-id (UUID/randomUUID)
        tenant-id (UUID/randomUUID)]
    (with-redefs [articles/list-articles
                  (fn [_db opts]
                    (reset! articles-opts opts)
                    [{:id (UUID/randomUUID)}])
                  articles/count-articles
                  (fn [_db search-opts]
                    (reset! articles-search search-opts)
                    {:total 6})
                  article-aliases/list-unmapped-aliases
                  (fn [_db opts]
                    (reset! unmapped-opts opts)
                    [{:id (UUID/randomUUID)}])
                  article-aliases/count-unmapped-aliases
                  (fn [_db opts]
                    (reset! unmapped-count-opts opts)
                    4)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})]
      (testing "articles list clamps and includes envelope"
        (let [handler (user-articles/list-articles-handler db)
              resp (handler (req "owner" {:limit "5000"
                                          :offset "-1"
                                          :search "abc"}))]
          (is (= 200 (:status resp)))
          (is (= 6 (get-in resp [:body :total])))
          (is (= 500 (get-in resp [:body :limit])))
          (is (= 0 (get-in resp [:body :offset])))
          (is (= {:limit 500 :offset 0 :search "abc"} @articles-opts))
          (is (= {:search "abc"} @articles-search))))

      (testing "unmapped aliases list includes envelope, supplier filter, and tenant scope"
        (let [handler (user-articles/list-unmapped-aliases-handler db)
              resp (handler (assoc (req "owner" {:limit "2"
                                                 :offset "3"
                                                 :supplier_id (str supplier-id)
                                                 :unit "kg"})
                              :session {:auth-session {:tenant {:id tenant-id}}}))]
          (is (= 200 (:status resp)))
          (is (= 4 (get-in resp [:body :total])))
          (is (= 2 (get-in resp [:body :limit])))
          (is (= 3 (get-in resp [:body :offset])))
          (is (= {:limit 2
                  :offset 3
                  :supplier-id supplier-id
                  :unit "kg"
                  :tenant-id tenant-id}
                @unmapped-opts))
          (is (= @unmapped-opts @unmapped-count-opts)))))))

(deftest catalog-list-handlers-return-standard-envelope
  (let [seen (atom {})
        mk-list (fn [k]
                  (fn [_db opts]
                    (swap! seen assoc k opts)
                    [{:id (UUID/randomUUID)}]))
        mk-count (fn [k total]
                   (fn [_db opts]
                     (swap! seen assoc (keyword (str (name k) "-count")) opts)
                     total))]
    (with-redefs [store-aliases/service {:list (mk-list :store-aliases)
                                         :count (mk-count :store-aliases 11)}
                  stores-service/entity-service {:list (mk-list :stores)
                                                 :count (mk-count :stores 12)}
                  cities/service {:list (mk-list :cities)
                                  :count (mk-count :cities 13)}
                  manufacturers/service {:list (mk-list :manufacturers)
                                         :count (mk-count :manufacturers 14)}
                  categories/service {:list (mk-list :categories)
                                      :count (mk-count :categories 15)}
                  subcategories/service {:list (mk-list :subcategories)
                                         :count (mk-count :subcategories 16)}
                  expense-categories/service {:list (mk-list :expense-categories)
                                              :count (mk-count :expense-categories 17)}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (testing "store aliases envelope"
        (let [handler (user-store-aliases/list-store-aliases-handler db)
              resp (handler (req "admin" {:limit "7" :offset "6" :search "alias"}))]
          (is (= 11 (get-in resp [:body :total])))
          (is (= 7 (get-in resp [:body :limit])))
          (is (= 6 (get-in resp [:body :offset])))
          (is (= {:limit 7 :offset 6 :search "alias"} (get @seen :store-aliases)))
          (is (= {:search "alias"} (get @seen :store-aliases-count)))))

      (testing "stores envelope with clamp"
        (let [handler (user-stores/list-stores-handler db)
              resp (handler (req "admin" {:limit "-1" :offset "-4" :search "s"}))]
          (is (= 12 (get-in resp [:body :total])))
          (is (= 1 (get-in resp [:body :limit])))
          (is (= 0 (get-in resp [:body :offset])))
          (is (= {:limit 1 :offset 0 :search "s"} (get @seen :stores)))
          (is (= {:search "s"} (get @seen :stores-count)))))

      (testing "cities envelope"
        (let [handler (user-cities/list-cities-handler db)
              resp (handler (req "admin" {:limit "2" :offset "1" :search "c"}))]
          (is (= 13 (get-in resp [:body :total])))
          (is (= 2 (get-in resp [:body :limit])))
          (is (= 1 (get-in resp [:body :offset])))
          (is (= {:limit 2 :offset 1 :search "c"} (get @seen :cities)))
          (is (= {:search "c"} (get @seen :cities-count)))))

      (testing "manufacturers envelope"
        (let [handler (user-manufacturers/list-manufacturers-handler db)
              resp (handler (req "admin" {:limit "3" :offset "2" :search "m"}))]
          (is (= 14 (get-in resp [:body :total])))
          (is (= 3 (get-in resp [:body :limit])))
          (is (= 2 (get-in resp [:body :offset])))
          (is (= {:limit 3 :offset 2 :search "m"} (get @seen :manufacturers)))
          (is (= {:search "m"} (get @seen :manufacturers-count)))))

      (testing "categories envelope"
        (let [handler (user-categories/list-categories-handler db)
              resp (handler (req "admin" {:limit "4" :offset "3" :search "cat"}))]
          (is (= 15 (get-in resp [:body :total])))
          (is (= 4 (get-in resp [:body :limit])))
          (is (= 3 (get-in resp [:body :offset])))
          (is (= {:limit 4 :offset 3 :search "cat"} (get @seen :categories)))
          (is (= {:search "cat"} (get @seen :categories-count)))))

      (testing "subcategories envelope"
        (let [handler (user-subcategories/list-subcategories-handler db)
              resp (handler (req "admin" {:limit "5" :offset "4" :search "sub"}))]
          (is (= 16 (get-in resp [:body :total])))
          (is (= 5 (get-in resp [:body :limit])))
          (is (= 4 (get-in resp [:body :offset])))
          (is (= {:limit 5 :offset 4 :search "sub"} (get @seen :subcategories)))
          (is (= {:search "sub"} (get @seen :subcategories-count)))))

      (testing "expense categories envelope"
        (let [handler (user-expense-categories/list-expense-categories-handler db)
              resp (handler (req "admin" {:limit "6" :offset "5" :search "exp"}))]
          (is (= 17 (get-in resp [:body :total])))
          (is (= 6 (get-in resp [:body :limit])))
          (is (= 5 (get-in resp [:body :offset])))
          (is (= {:limit 6 :offset 5 :search "exp"} (get @seen :expense-categories)))
          (is (= {:search "exp"} (get @seen :expense-categories-count))))))))

(deftest catalog-list-handlers-accept-column-filter-aliases
  (let [seen (atom {})
        tenant-id (UUID/randomUUID)
        mk-list (fn [k]
                  (fn [_db opts]
                    (swap! seen assoc k opts)
                    [{:id (UUID/randomUUID)}]))
        mk-count (fn [k total]
                   (fn [_db opts]
                     (swap! seen assoc (keyword (str (name k) "-count")) opts)
                     total))]
    (with-redefs [suppliers/list-suppliers (mk-list :suppliers)
                  suppliers/count-suppliers (mk-count :suppliers 10)
                  stores-service/entity-service {:list (mk-list :stores)
                                                 :count (mk-count :stores 12)}
                  categories/service {:list (mk-list :categories)
                                      :count (mk-count :categories 15)}
                  cities/service {:list (mk-list :cities)
                                  :count (mk-count :cities 13)}
                  manufacturers/service {:list (mk-list :manufacturers)
                                         :count (mk-count :manufacturers 14)}
                  subcategories/service {:list (mk-list :subcategories)
                                         :count (mk-count :subcategories 16)}
                  expense-categories/service {:list (mk-list :expense-categories)
                                              :count (mk-count :expense-categories 17)}
                  h/json-response (fn [body & [status]] {:status (or status 200)
                                                         :body body})]
      (testing "suppliers use display-name as search"
        (let [handler (reference-data/list-suppliers-handler db)
              resp (handler (req "member" {:limit "8"
                                           :display-name "Konzum"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 8 :offset 0 :search "Konzum"}
                (get @seen :suppliers)))
          (is (= {:search "Konzum"}
                (get @seen :suppliers-count)))))

      (testing "stores use display-name as search"
        (let [handler (user-stores/list-stores-handler db)
              resp (handler (req "admin" {:limit "3"
                                          :display-name "Podružnica"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 3 :offset 0 :search "Podružnica"}
                (get @seen :stores)))
          (is (= {:search "Podružnica"}
                (get @seen :stores-count)))))

      (testing "categories use name as search"
        (let [handler (user-categories/list-categories-handler db)
              resp (handler (req "admin" {:limit "4"
                                          :name "Dom i vrt"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 4 :offset 0 :search "Dom i vrt"}
                (get @seen :categories)))
          (is (= {:search "Dom i vrt"}
                (get @seen :categories-count)))))

      (testing "cities use name as search"
        (let [handler (user-cities/list-cities-handler db)
              resp (handler (req "admin" {:limit "2"
                                          :name "Aleksandrovac"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 2 :offset 0 :search "Aleksandrovac"}
                (get @seen :cities)))
          (is (= {:search "Aleksandrovac"}
                (get @seen :cities-count)))))

      (testing "manufacturers use display-name as search"
        (let [handler (user-manufacturers/list-manufacturers-handler db)
              resp (handler (req "admin" {:limit "3"
                                          :display-name "Amisu"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 3 :offset 0 :search "Amisu"}
                (get @seen :manufacturers)))
          (is (= {:search "Amisu"}
                (get @seen :manufacturers-count)))))

      (testing "subcategories use name as search"
        (let [handler (user-subcategories/list-subcategories-handler db)
              resp (handler (req "admin" {:limit "5"
                                          :name "Alati"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 5 :offset 0 :search "Alati"}
                (get @seen :subcategories)))
          (is (= {:search "Alati"}
                (get @seen :subcategories-count)))))

      (testing "expense categories use name as search while keeping tenant scope"
        (let [handler (user-expense-categories/list-expense-categories-handler db)
              resp (handler (assoc (req "admin" {:limit "6"
                                                 :name "Komunalije"})
                              :session {:auth-session {:tenant {:id tenant-id}}}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 6 :offset 0 :search "Komunalije" :tenant-id tenant-id}
                (get @seen :expense-categories)))
          (is (= {:search "Komunalije" :tenant-id tenant-id}
                (get @seen :expense-categories-count))))))))

(deftest article-and-alias-list-handlers-accept-column-filter-aliases
  (let [articles-opts (atom nil)
        articles-count-opts (atom nil)
        supplier-aliases-opts (atom nil)
        supplier-aliases-count-opts (atom nil)
        unmapped-opts (atom nil)
        unmapped-count-opts (atom nil)
        tenant-id (UUID/randomUUID)]
    (with-redefs [articles/list-articles
                  (fn [_db opts]
                    (reset! articles-opts opts)
                    [{:id (UUID/randomUUID)}])
                  articles/count-articles
                  (fn [_db opts]
                    (reset! articles-count-opts opts)
                    {:total 6})
                  supplier-aliases/list-supplier-aliases
                  (fn [_db opts]
                    (reset! supplier-aliases-opts opts)
                    [{:id (UUID/randomUUID)}])
                  supplier-aliases/count-supplier-aliases
                  (fn [_db opts]
                    (reset! supplier-aliases-count-opts opts)
                    9)
                  article-aliases/list-unmapped-aliases
                  (fn [_db opts]
                    (reset! unmapped-opts opts)
                    [{:id (UUID/randomUUID)}])
                  article-aliases/count-unmapped-aliases
                  (fn [_db opts]
                    (reset! unmapped-count-opts opts)
                    4)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})]
      (testing "articles honor canonical and related column filters"
        (let [handler (user-articles/list-articles-handler db)
              resp (handler (req "owner" {:limit "7"
                                          :canonical-name "Ananas CJ"
                                          :manufacturer-display-name "Bayer"
                                          :category-name "Pića"
                                          :subcategory-name "Voće"
                                          :unit "kg"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 7
                  :offset 0
                  :search "Ananas CJ"
                  :unit "kg"
                  :manufacturer-display-name "Bayer"
                  :category-name "Pića"
                  :subcategory-name "Voće"}
                @articles-opts))
          (is (= {:search "Ananas CJ"
                  :unit "kg"
                  :manufacturer-display-name "Bayer"
                  :category-name "Pića"
                  :subcategory-name "Voće"}
                @articles-count-opts))))

      (testing "supplier aliases use raw-label as search"
        (let [handler (supplier-aliases-handler/list-supplier-aliases-handler db)
              resp (handler (req "admin" {:limit "2"
                                          :raw-label "PETROL BH OIL COMPANY"
                                          :supplier-display-name "PETROL BH OIL COMPANY"
                                          :raw-label-normalized "petrol-bh-oil-company"}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 2
                  :offset 0
                  :search "PETROL BH OIL COMPANY"
                  :supplier-display-name "PETROL BH OIL COMPANY"
                  :raw-label-normalized "petrol-bh-oil-company"}
                @supplier-aliases-opts))
          (is (= @supplier-aliases-opts @supplier-aliases-count-opts))))

      (testing "unmapped aliases use supplier-display-name alias"
        (let [handler (user-articles/list-unmapped-aliases-handler db)
              resp (handler (assoc (req "owner" {:limit "2"
                                                 :supplier-display-name "BINGO"
                                                 :raw-label "COKOLADNA BANANICA 25G STARK"})
                              :session {:auth-session {:tenant {:id tenant-id}}}))]
          (is (= 200 (:status resp)))
          (is (= {:limit 2
                  :offset 0
                  :tenant-id tenant-id
                  :supplier-name "BINGO"
                  :raw-label "COKOLADNA BANANICA 25G STARK"}
                @unmapped-opts))
          (is (= @unmapped-opts @unmapped-count-opts)))))))