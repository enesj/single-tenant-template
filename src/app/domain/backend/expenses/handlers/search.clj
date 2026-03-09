(ns app.domain.backend.expenses.handlers.search
  "Cross-entity search handlers.

  User search is tenant-scoped; admin search is global.
  Both fan out across all relevant tables in parallel via pmap."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Search helpers
;; ---------------------------------------------------------------------------

(defn- pattern [term] (str "%" term "%"))

(defn- search-entity
  "Run `entity-fn` safely, returning [] on any exception."
  [entity-fn]
  (try (entity-fn) (catch Exception e (log/warn "Search entity failed" {:error (.getMessage e)}) [])))

;; ---------------------------------------------------------------------------
;; Per-entity search queries
;; ---------------------------------------------------------------------------

(defn- search-expenses
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where [:or
                    [:ilike :s.display_name p]
                    [:ilike :p.label p]
                    [:ilike :e.notes p]]
        where (if tenant-id
                [:and text-where [:= :e.tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [[:e.id :id]
                            [:e.total_amount :total_amount]
                            [:e.currency :currency]
                            [:e.purchased_at :purchased_at]
                            [:e.is_posted :is_posted]
                            [:e.notes :notes]
                            [:s.display_name :supplier_display_name]
                            [:p.label :payer_label]]
                   :from [[:expenses :e]]
                   :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                               [:payers :p] [:= :p.id :e.payer_id]]
                   :where where
                   :order-by [[:e.purchased_at :desc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-receipts
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where [:or
                    [:ilike :original_filename p]
                    [:ilike :supplier_guess p]
                    [:ilike :store_guess p]]
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :original_filename :supplier_guess :store_guess :status :created_at]
                   :from [:receipts]
                   :where where
                   :order-by [[:created_at :desc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-payers
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where [:ilike :label p]
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :label :payer_type_id]
                   :from [:payers]
                   :where where
                   :order-by [[:label :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-expense-cats
  [db term limit tenant-id]
  (let [p (pattern term)
        text-where [:ilike :name p]
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :name]
                   :from [:expense_categories]
                   :where where
                   :order-by [[:name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-suppliers
  [db term limit]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :display_name :normalized_key :address]
                   :from [:suppliers]
                   :where [:or
                           [:ilike :display_name p]
                           [:ilike :address p]]
                   :order-by [[:display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-stores
  [db term limit]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :display_name :normalized_key :address]
                   :from [:stores]
                   :where [:or
                           [:ilike :display_name p]
                           [:ilike :address p]]
                   :order-by [[:display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-articles
  [db term limit]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id :canonical_name]
                   :from [:articles]
                   :where [:ilike :canonical_name p]
                   :order-by [[:canonical_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ---------------------------------------------------------------------------
;; Handler factories
;; ---------------------------------------------------------------------------

(defn user-search-handler
  "Handler for user search — results are scoped to the user's tenant."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              q (get-in request [:query-params "q"])
              limit 5]
          (if (and q (>= (count q) 2))
            (try
              (let [results (into {}
                              (pmap
                                (fn [[k f]] [k (search-entity f)])
                                {:expenses     #(search-expenses db q limit tenant-id)
                                 :receipts     #(search-receipts db q limit tenant-id)
                                 :payers       #(search-payers db q limit tenant-id)
                                 :expense-cats #(search-expense-cats db q limit tenant-id)
                                 :suppliers    #(search-suppliers db q limit)
                                 :stores       #(search-stores db q limit)
                                 :articles     #(search-articles db q limit)}))]
                (h/json-response {:results results}))
              (catch Exception e
                (log/error e "Error executing user search" {:q q :tenant-id tenant-id})
                (h/json-response {:error "Search failed"} 500)))
            (h/json-response {:results {}}))))
      (h/unauthorized-response))))

(defn admin-search-handler
  "Handler for admin search — results are global (no tenant filter)."
  [db]
  (fn [request]
    (let [q (get-in request [:query-params "q"])
          limit 5]
      (if (and q (>= (count q) 2))
        (try
          (let [results (into {}
                          (pmap
                            (fn [[k f]] [k (search-entity f)])
                            {:expenses     #(search-expenses db q limit nil)
                             :receipts     #(search-receipts db q limit nil)
                             :payers       #(search-payers db q limit nil)
                             :expense-cats #(search-expense-cats db q limit nil)
                             :suppliers    #(search-suppliers db q limit)
                             :stores       #(search-stores db q limit)
                             :articles     #(search-articles db q limit)}))]
            (h/json-response {:results results}))
          (catch Exception e
            (log/error e "Error executing admin search" {:q q})
            (h/json-response {:error "Search failed"} 500)))
        (h/json-response {:results {}})))))

;; ---------------------------------------------------------------------------
;; Related-records queries (called when a search result is selected)
;; ---------------------------------------------------------------------------

(defn- related-for-article
  "Rich article detail: metadata, price history, stores, aggregates."
  [db article-id _limit tenant-id]
  (let [;; 1. Article detail with manufacturer, category, subcategory
        detail (first
                 (jdbc/execute!
                   db
                   (sql/format {:select [[:a.canonical_name :canonical_name]
                                         [:a.link :link]
                                         [:m.display_name :manufacturer_display_name]
                                         [:sub.name :subcategory_name]
                                         [:c.name :category_name]]
                                :from [[:articles :a]]
                                :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                                            [:subcategories :sub] [:= :sub.id :a.subcategory_id]
                                            [:categories :c] [:= :c.id :sub.category_id]]
                                :where [:= :a.id article-id]})
                   {:builder-fn rs/as-unqualified-lower-maps}))

        ;; Common WHERE: expense_items links to articles via article_aliases
        ;; (expense_items.alias_id → article_aliases.article_id = article-id)
        aa-cond [:= :aa.article_id article-id]
        ei-where (if tenant-id
                   [:and aa-cond [:= :e.tenant_id tenant-id]]
                   aa-cond)

        ;; 2. Aggregate stats: total turnover, item count
        stats (first
                (jdbc/execute!
                  db
                  (sql/format {:select [[[:coalesce [:sum :ei.line_total] [:inline 0]] :total_turnover]
                                        [[:count :ei.id] :total_items]]
                               :from [[:expense_items :ei]]
                               :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                      [:expenses :e] [:= :e.id :ei.expense_id]]
                               :where ei-where})
                  {:builder-fn rs/as-unqualified-lower-maps}))

        ;; 3. Price history (most recent 50, one row per expense)
        price-history (jdbc/execute!
                        db
                        (sql/format {:select [[[:min :ei.unit_price] :unit_price]
                                              [[:sum :ei.qty] :qty]
                                              [[:sum :ei.line_total] :line_total]
                                              [:e.id :expense_id]
                                              [:e.purchased_at :purchased_at]
                                              [:st.id :store_id]
                                              [:st.display_name :store_display_name]
                                              [:st.address :store_address]
                                              [:sup.display_name :supplier_display_name]]
                                     :from [[:expense_items :ei]]
                                     :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                            [:expenses :e] [:= :e.id :ei.expense_id]]
                                     :left-join [[:stores :st] [:= :st.id :e.store_id]
                                                 [:suppliers :sup] [:= :sup.id :e.supplier_id]]
                                     :where ei-where
                                     :group-by [:e.id :e.purchased_at
                                                :st.id :st.display_name :st.address
                                                :sup.display_name]
                                     :order-by [[:e.purchased_at :desc]]
                                     :limit 50})
                        {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Distinct stores (all, not limited by price history)
        stores (jdbc/execute!
                 db
                 (sql/format {:select-distinct [[:st.id :id]
                                                [:st.display_name :display_name]
                                                [:st.address :address]]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:expenses :e] [:= :e.id :ei.expense_id]
                                     [:stores :st] [:= :st.id :e.store_id]]
                              :where ei-where
                              :order-by [[:st.display_name :asc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; Last price = unit_price from the most recent entry
        last-price (:unit_price (first price-history))]
    {:detail (assoc detail :last_price last-price)
     :stats stats
     :price_history price-history
     :stores stores}))

(defn- related-for-supplier
  "Recent expenses for this supplier scoped to tenant."
  [db supplier-id limit tenant-id]
  (let [id-cond [:= :e.supplier_id supplier-id]
        where   (if tenant-id [:and id-cond [:= :e.tenant_id tenant-id]] id-cond)]
    {:expenses (jdbc/execute!
                 db
                 (sql/format {:select [[:e.id :id]
                                       [:e.total_amount :total_amount]
                                       [:e.currency :currency]
                                       [:e.purchased_at :purchased_at]
                                       [:p.label :payer_label]]
                              :from [[:expenses :e]]
                              :left-join [[:payers :p] [:= :p.id :e.payer_id]]
                              :where where
                              :order-by [[:e.purchased_at :desc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})}))

(defn- related-for-store
  "Recent expenses at this store."
  [db store-id limit tenant-id]
  (let [id-cond [:= :e.store_id store-id]
        where   (if tenant-id [:and id-cond [:= :e.tenant_id tenant-id]] id-cond)]
    {:expenses (jdbc/execute!
                 db
                 (sql/format {:select [[:e.id :id]
                                       [:e.total_amount :total_amount]
                                       [:e.currency :currency]
                                       [:e.purchased_at :purchased_at]
                                       [:s.display_name :supplier_display_name]]
                              :from [[:expenses :e]]
                              :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                              :where where
                              :order-by [[:e.purchased_at :desc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})}))

(defn- related-for-payer
  "Recent expenses for this payer."
  [db payer-id limit tenant-id]
  (let [id-cond [:= :e.payer_id payer-id]
        where   (if tenant-id [:and id-cond [:= :e.tenant_id tenant-id]] id-cond)]
    {:expenses (jdbc/execute!
                 db
                 (sql/format {:select [[:e.id :id]
                                       [:e.total_amount :total_amount]
                                       [:e.currency :currency]
                                       [:e.purchased_at :purchased_at]
                                       [:s.display_name :supplier_display_name]]
                              :from [[:expenses :e]]
                              :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                              :where where
                              :order-by [[:e.purchased_at :desc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})}))

(defn- related-for-expense-cat
  "Recent expenses in this category."
  [db cat-id limit tenant-id]
  (let [id-cond [:= :e.expense_category_id cat-id]
        where   (if tenant-id [:and id-cond [:= :e.tenant_id tenant-id]] id-cond)]
    {:expenses (jdbc/execute!
                 db
                 (sql/format {:select [[:e.id :id]
                                       [:e.total_amount :total_amount]
                                       [:e.currency :currency]
                                       [:e.purchased_at :purchased_at]
                                       [:s.display_name :supplier_display_name]]
                              :from [[:expenses :e]]
                              :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                              :where where
                              :order-by [[:e.purchased_at :desc]]
                              :limit limit})
                 {:builder-fn rs/as-unqualified-lower-maps})}))

(defn user-related-handler
  "Fetch related records for a selected search result.
   Query params: type (string), id (uuid string)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              params (:query-params request)
              entity-type (get params "type")
              entity-id (h/try-parse-uuid (get params "id"))
              limit 8]
          (if-not entity-id
            (h/json-response {:error "Missing or invalid id"} 400)
            (try
              (let [related (case entity-type
                              "articles"    (related-for-article db entity-id limit tenant-id)
                              "suppliers"   (related-for-supplier db entity-id limit tenant-id)
                              "stores"      (related-for-store db entity-id limit tenant-id)
                              "payers"      (related-for-payer db entity-id limit tenant-id)
                              "expense-cats" (related-for-expense-cat db entity-id limit tenant-id)
                              {})]
                (h/json-response {:related related :type entity-type :id (str entity-id)}))
              (catch Exception e
                (log/error e "Error fetching related records" {:type entity-type :id entity-id})
                (h/json-response {:error "Failed to fetch related records"} 500))))))
      (h/unauthorized-response))))

(comment
  ;; REPL test: load handler and call with mock request
  ;; (require '[app.domain.backend.expenses.handlers.search :as search] :reload)
  ;; ((search/user-search-handler db) {:query-params {"q" "coffee"} :session {:auth-session {:user {:id ...} :membership {:role "member"} :tenant {:id ...}}}})
  :rcf)
