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
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:s.id :id]
                    [:s.display_name :display_name]
                    [:s.normalized_key :normalized_key]
                    [:s.address :address]]
           :from [[:suppliers :s]]
           :where [:and
                   [:or
                    [:ilike :s.display_name p]
                    [:ilike :s.address p]]
                   [:or
                    [:exists {:select [1]
                              :from [[:expenses :e]]
                              :where [:and
                                      [:= :e.tenant_id tenant-id]
                                      [:= :e.supplier_id :s.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:supplier_aliases :sa] [:= :sa.id :r.supplier_alias_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :sa.supplier_id :s.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                                     [:stores :st_receipt] [:= :st_receipt.id :sta.store_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :st_receipt.supplier_id :s.id]]}]]]
           :order-by [[:s.display_name :asc]]
           :limit limit}
          {:select [:id :display_name :normalized_key :address]
           :from [:suppliers]
           :where [:or
                   [:ilike :display_name p]
                   [:ilike :address p]]
           :order-by [[:display_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-stores
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:st.id :id]
                    [:st.display_name :display_name]
                    [:st.normalized_key :normalized_key]
                    [:st.address :address]]
           :from [[:stores :st]]
           :where [:and
                   [:or
                    [:ilike :st.display_name p]
                    [:ilike :st.address p]]
                   [:or
                    [:exists {:select [1]
                              :from [[:expenses :e]]
                              :where [:and
                                      [:= :e.tenant_id tenant-id]
                                      [:= :e.store_id :st.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :sta.store_id :st.id]]}]]]
           :order-by [[:st.display_name :asc]]
           :limit limit}
          {:select [:id :display_name :normalized_key :address]
           :from [:stores]
           :where [:or
                   [:ilike :display_name p]
                   [:ilike :address p]]
           :order-by [[:display_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-articles
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:a.id :id]
                    [:a.canonical_name :canonical_name]]
           :from [[:articles :a]]
           :where [:and
                   [:ilike :a.canonical_name p]
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :ei.article_id :a.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :aa.article_id :a.id]]}]]]
           :order-by [[:a.canonical_name :asc]]
           :limit limit}
          {:select [:id :canonical_name]
           :from [:articles]
           :where [:ilike :canonical_name p]
           :order-by [[:canonical_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-categories
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:c.id :id]
                    [:c.name :name]
                    [:c.description :description]]
           :from [[:categories :c]]
           :where [:and
                   [:ilike :c.name p]
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:articles :a_item] [:= :a_item.id :ei.article_id]
                                     [:subcategories :sub_item] [:= :sub_item.id :a_item.subcategory_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :sub_item.category_id :c.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a_alias] [:= :a_alias.id :aa.article_id]
                                     [:subcategories :sub_alias] [:= :sub_alias.id :a_alias.subcategory_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :sub_alias.category_id :c.id]]}]]]
           :order-by [[:c.name :asc]]
           :limit limit}
          {:select [:id :name :description]
           :from [:categories]
           :where [:ilike :name p]
           :order-by [[:name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-subcategories
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:sub.id :id]
                    [:sub.name :name]
                    [:c.name :category_name]]
           :from [[:subcategories :sub]]
           :join [[:categories :c] [:= :c.id :sub.category_id]]
           :where [:and
                   [:ilike :sub.name p]
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:articles :a_item] [:= :a_item.id :ei.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_item.subcategory_id :sub.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a_alias] [:= :a_alias.id :aa.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_alias.subcategory_id :sub.id]]}]]]
           :order-by [[:c.name :asc] [:sub.name :asc]]
           :limit limit}
          {:select [[:s.id :id] [:s.name :name] [:c.name :category_name]]
           :from [[:subcategories :s]]
           :join [[:categories :c] [:= :c.id :s.category_id]]
           :where [:ilike :s.name p]
           :order-by [[:c.name :asc] [:s.name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-manufacturers
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:m.id :id]
                    [:m.display_name :display_name]
                    [:m.normalized_key :normalized_key]]
           :from [[:manufacturers :m]]
           :where [:and
                   [:or
                    [:ilike :m.display_name p]
                    [:ilike :m.normalized_key p]]
                   [:or
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:articles :a_item] [:= :a_item.id :ei.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_item.manufacturer_id :m.id]]}]
                    [:exists {:select [1]
                              :from [[:expense_items :ei]]
                              :join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a_alias] [:= :a_alias.id :aa.article_id]]
                              :where [:and
                                      [:= :ei.tenant_id tenant-id]
                                      [:= :a_alias.manufacturer_id :m.id]]}]]]
           :order-by [[:m.display_name :asc]]
           :limit limit}
          {:select [:id :display_name :normalized_key]
           :from [:manufacturers]
           :where [:or
                   [:ilike :display_name p]
                   [:ilike :normalized_key p]]
           :order-by [[:display_name :asc]]
           :limit limit}))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- search-cities
  [db term limit tenant-id]
  (let [p (pattern term)]
    (jdbc/execute!
      db
      (sql/format
        (if tenant-id
          {:select [[:city.id :id]
                    [:city.name :name]
                    [:city.zip :zip]
                    [:city.country :country]]
           :from [[:cities :city]]
           :where [:and
                   [:or
                    [:ilike :city.name p]
                    [:ilike :city.zip p]]
                   [:or
                    [:exists {:select [1]
                              :from [[:expenses :e]]
                              :join [[:stores :st_exp] [:= :st_exp.id :e.store_id]]
                              :where [:and
                                      [:= :e.tenant_id tenant-id]
                                      [:= :st_exp.city_id :city.id]]}]
                    [:exists {:select [1]
                              :from [[:receipts :r]]
                              :join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                                     [:stores :st_receipt] [:= :st_receipt.id :sta.store_id]]
                              :where [:and
                                      [:= :r.tenant_id tenant-id]
                                      [:= :st_receipt.city_id :city.id]]}]]]
           :order-by [[:city.name :asc]]
           :limit limit}
          {:select-distinct [[:c.id :id] [:c.name :name] [:c.zip :zip] [:c.country :country]]
           :from [[:cities :c]]
           :join [[:stores :st] [:= :st.city_id :c.id]
                  [:expenses :e] [:= :e.store_id :st.id]]
           :where [:or
                   [:ilike :c.name p]
                   [:ilike :c.zip p]]
           :order-by [[:c.name :asc]]
           :limit limit}))
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
                                {:payers         #(search-payers db q limit tenant-id)
                                 :expense-cats   #(search-expense-cats db q limit tenant-id)
                                 :suppliers      #(search-suppliers db q limit tenant-id)
                                 :stores         #(search-stores db q limit tenant-id)
                                 :articles       #(search-articles db q limit tenant-id)
                                 :categories     #(search-categories db q limit tenant-id)
                                 :subcategories  #(search-subcategories db q limit tenant-id)
                                 :manufacturers  #(search-manufacturers db q limit tenant-id)
                                 :cities         #(search-cities db q limit tenant-id)}))]
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
                            {:payers         #(search-payers db q limit nil)
                             :expense-cats   #(search-expense-cats db q limit nil)
                             :suppliers      #(search-suppliers db q limit nil)
                             :stores         #(search-stores db q limit nil)
                             :articles       #(search-articles db q limit nil)
                             :categories     #(search-categories db q limit nil)
                             :subcategories  #(search-subcategories db q limit nil)
                             :manufacturers  #(search-manufacturers db q limit nil)
                             :cities         #(search-cities db q limit nil)}))]
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
  "Supplier detail: stores with total spendings + articles purchased from this supplier."
  [db supplier-id _limit tenant-id]
  (let [;; Common WHERE for supplier-scoped queries
        sup-cond [:= :e.supplier_id supplier-id]
        where    (if tenant-id [:and sup-cond [:= :e.tenant_id tenant-id]] sup-cond)

        ;; 1. Stores belonging to this supplier with total spendings
        stores (jdbc/execute!
                 db
                 (sql/format {:select [[:st.id :id]
                                       [:st.display_name :display_name]
                                       [:st.address :address]
                                       [[:coalesce [:sum :e.total_amount] [:inline 0]] :total_spendings]]
                              :from [[:expenses :e]]
                              :join [[:stores :st] [:= :st.id :e.store_id]]
                              :where where
                              :group-by [:st.id :st.display_name :st.address]
                              :order-by [[[:sum :e.total_amount] :desc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; 2. Articles purchased from this supplier (aggregates)
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Last price per article (most recent expense from this supplier)
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        ;; Merge last prices into article rows
        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:stores stores
     :articles articles}))

(defn- related-for-store
  "Store detail (supplier, city, address) + articles purchased there."
  [db store-id _limit tenant-id]
  (let [;; 1. Store detail with supplier and city names
        detail (first
                 (jdbc/execute!
                   db
                   (sql/format {:select [[:s.display_name :display_name]
                                         [:s.address :address]
                                         [:sup.display_name :supplier_display_name]
                                         [:c.name :city_name]]
                                :from [[:stores :s]]
                                :left-join [[:suppliers :sup] [:= :sup.id :s.supplier_id]
                                            [:cities :c] [:= :c.id :s.city_id]]
                                :where [:= :s.id store-id]})
                   {:builder-fn rs/as-unqualified-lower-maps}))

        ;; 2. Articles purchased at this store (aggregates)
        store-cond [:= :e.store_id store-id]
        art-where  (if tenant-id
                     [:and store-cond [:= :e.tenant_id tenant-id]]
                     store-cond)

        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where art-where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Last price per article (most recent expense at this store)
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                                              :where art-where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        ;; Merge last prices into article rows
        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:detail detail
     :articles articles}))

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

(defn- related-for-manufacturer
  "Manufacturer detail: suppliers + articles purchased (aggregates + last prices).
   Articles include supplier_id so the frontend can filter by supplier."
  [db manufacturer-id _limit tenant-id]
  (let [;; Common WHERE: articles by this manufacturer, joined via expense_items
        mfr-cond [:= :a.manufacturer_id manufacturer-id]
        where    (if tenant-id
                   [:and mfr-cond [:= :e.tenant_id tenant-id]]
                   mfr-cond)

        ;; 1. Suppliers that carry this manufacturer's articles (via expenses)
        suppliers (jdbc/execute!
                    db
                    (sql/format {:select-distinct [[:sup.id :id]
                                                   [:sup.display_name :display_name]
                                                   [:sup.address :address]]
                                 :from [[:expense_items :ei]]
                                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                        [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                        [:articles :a] [:= :a.id :aa.article_id]
                                        [:suppliers :sup] [:= :sup.id :e.supplier_id]]
                                 :where where
                                 :order-by [[:sup.display_name :asc]]})
                    {:builder-fn rs/as-unqualified-lower-maps})

        ;; 2. Articles aggregated globally (all suppliers)
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Per-supplier article aggregates (for filtering)
        supplier-articles (jdbc/execute!
                            db
                            (sql/format {:select [[:a.id :article_id]
                                                  [:e.supplier_id :supplier_id]
                                                  [[:sum :ei.qty] :total_qty]
                                                  [[:sum :ei.line_total] :total_bam]]
                                         :from [[:expense_items :ei]]
                                         :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                [:articles :a] [:= :a.id :aa.article_id]]
                                         :where where
                                         :group-by [:a.id :e.supplier_id]})
                            {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Last price per article
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                     [:articles :a] [:= :a.id :aa.article_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:suppliers suppliers
     :articles articles
     :supplier-articles supplier-articles}))

(defn- related-for-category
  "Category detail: subcategories, stores, + articles (with subcategory/store filtering)."
  [db category-id _limit tenant-id]
  (let [;; 1. Subcategories
        subcategories (jdbc/execute!
                        db
                        (sql/format {:select [:id :name]
                                     :from [:subcategories]
                                     :where [:= :category_id category-id]
                                     :order-by [[:name :asc]]})
                        {:builder-fn rs/as-unqualified-lower-maps})

        ;; Common WHERE: articles in this category (via subcategories)
        cat-cond [:= :sub.category_id category-id]
        where    (if tenant-id
                   [:and cat-cond [:= :e.tenant_id tenant-id]]
                   cat-cond)

        ;; 2. Stores where articles from this category were purchased
        stores (jdbc/execute!
                 db
                 (sql/format {:select-distinct [[:st.id :id]
                                                [:st.display_name :display_name]
                                                [:st.address :address]]
                              :from [[:expense_items :ei]]
                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a] [:= :a.id :aa.article_id]
                                     [:subcategories :sub] [:= :sub.id :a.subcategory_id]
                                     [:stores :st] [:= :st.id :e.store_id]]
                              :where where
                              :order-by [[:st.display_name :asc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Articles aggregated globally
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]
                                           [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Per-subcategory article aggregates (for filtering)
        subcat-articles (jdbc/execute!
                          db
                          (sql/format {:select [[:a.id :article_id]
                                                [:a.subcategory_id :subcategory_id]
                                                [[:sum :ei.qty] :total_qty]
                                                [[:sum :ei.line_total] :total_bam]]
                                       :from [[:expense_items :ei]]
                                       :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                              [:articles :a] [:= :a.id :aa.article_id]
                                              [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                       :where where
                                       :group-by [:a.id :a.subcategory_id]})
                          {:builder-fn rs/as-unqualified-lower-maps})

        ;; 5. Per-store article aggregates (for filtering)
        store-articles (jdbc/execute!
                         db
                         (sql/format {:select [[:a.id :article_id]
                                               [:e.store_id :store_id]
                                               [[:sum :ei.qty] :total_qty]
                                               [[:sum :ei.line_total] :total_bam]]
                                      :from [[:expense_items :ei]]
                                      :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                             [:articles :a] [:= :a.id :aa.article_id]
                                             [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                      :where where
                                      :group-by [:a.id :e.store_id]})
                         {:builder-fn rs/as-unqualified-lower-maps})

        ;; 6. Last price per article
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                     [:articles :a] [:= :a.id :aa.article_id]
                                                     [:subcategories :sub] [:= :sub.id :a.subcategory_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:subcategories subcategories
     :stores stores
     :articles articles
     :subcat-articles subcat-articles
     :store-articles store-articles}))

(defn- related-for-subcategory
  "Subcategory detail: stores + articles in this subcategory."
  [db subcategory-id _limit tenant-id]
  (let [sub-cond [:= :a.subcategory_id subcategory-id]
        where    (if tenant-id
                   [:and sub-cond [:= :e.tenant_id tenant-id]]
                   sub-cond)

        ;; 1. Stores
        stores (jdbc/execute!
                 db
                 (sql/format {:select-distinct [[:st.id :id]
                                                [:st.display_name :display_name]
                                                [:st.address :address]]
                              :from [[:expense_items :ei]]
                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a] [:= :a.id :aa.article_id]
                                     [:stores :st] [:= :st.id :e.store_id]]
                              :where where
                              :order-by [[:st.display_name :asc]]})
                 {:builder-fn rs/as-unqualified-lower-maps})

        ;; 2. Articles aggregated
        article-aggs (jdbc/execute!
                       db
                       (sql/format {:select [[:a.id :id]
                                             [:a.canonical_name :canonical_name]
                                             [[:sum :ei.qty] :total_qty]
                                             [[:sum :ei.line_total] :total_bam]]
                                    :from [[:expense_items :ei]]
                                    :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                           [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                           [:articles :a] [:= :a.id :aa.article_id]]
                                    :where where
                                    :group-by [:a.id :a.canonical_name]
                                    :order-by [[[:sum :ei.line_total] :desc]]})
                       {:builder-fn rs/as-unqualified-lower-maps})

        ;; 3. Per-store article aggregates
        store-articles (jdbc/execute!
                         db
                         (sql/format {:select [[:a.id :article_id]
                                               [:e.store_id :store_id]
                                               [[:sum :ei.qty] :total_qty]
                                               [[:sum :ei.line_total] :total_bam]]
                                      :from [[:expense_items :ei]]
                                      :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                             [:articles :a] [:= :a.id :aa.article_id]]
                                      :where where
                                      :group-by [:a.id :e.store_id]})
                         {:builder-fn rs/as-unqualified-lower-maps})

        ;; 4. Last price per article
        last-prices (when (seq article-aggs)
                      (jdbc/execute!
                        db
                        (sql/format {:select [:article_id :unit_price]
                                     :from [[{:select [[:aa.article_id :article_id]
                                                       [:ei.unit_price :unit_price]
                                                       [[:over [[:row_number]
                                                                {:partition-by :aa.article_id
                                                                 :order-by [[:e.purchased_at :desc]]}]]
                                                        :rn]]
                                              :from [[:expense_items :ei]]
                                              :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                     [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                                     [:articles :a] [:= :a.id :aa.article_id]]
                                              :where where}
                                             :sub]]
                                     :where [:= :rn 1]})
                        {:builder-fn rs/as-unqualified-lower-maps}))

        price-map (reduce (fn [acc {:keys [article_id unit_price]}]
                            (assoc acc (str article_id) unit_price))
                    {} last-prices)
        articles  (mapv (fn [art]
                          (assoc art :last_price (get price-map (str (:id art)))))
                    article-aggs)]
    {:stores stores
     :articles articles
     :store-articles store-articles}))

(defn- related-results
  [db entity-type entity-id limit tenant-id]
  (case entity-type
    "articles" (related-for-article db entity-id limit tenant-id)
    "suppliers" (related-for-supplier db entity-id limit tenant-id)
    "stores" (related-for-store db entity-id limit tenant-id)
    "payers" (related-for-payer db entity-id limit tenant-id)
    "expense-cats" (related-for-expense-cat db entity-id limit tenant-id)
    "manufacturers" (related-for-manufacturer db entity-id limit tenant-id)
    "categories" (related-for-category db entity-id limit tenant-id)
    "subcategories" (related-for-subcategory db entity-id limit tenant-id)
    {}))

(defn- related-response
  [db request tenant-id]
  (let [params (:query-params request)
        entity-type (get params "type")
        entity-id (h/try-parse-uuid (get params "id"))
        limit 8]
    (if-not entity-id
      (h/json-response {:error "Missing or invalid id"} 400)
      (try
        (let [related (related-results db entity-type entity-id limit tenant-id)]
          (h/json-response {:related related
                            :type entity-type
                            :id (str entity-id)}))
        (catch Exception e
          (log/error e "Error fetching related records"
            {:type entity-type
             :id entity-id
             :tenant-id tenant-id})
          (h/json-response {:error "Failed to fetch related records"} 500))))))

(defn user-related-handler
  "Fetch related records for a selected search result.
   Query params: type (string), id (uuid string)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (related-response db request (h/get-tenant-id request)))
      (h/unauthorized-response))))

(defn admin-related-handler
  "Fetch related records for the admin search page using global scope."
  [db]
  (fn [request]
    (related-response db request nil)))

(comment
  ;; REPL test: load handler and call with mock request
  ;; (require '[app.domain.backend.expenses.handlers.search :as search] :reload)
  ;; ((search/user-search-handler db) {:query-params {"q" "coffee"} :session {:auth-session {:user {:id ...} :membership {:role "member"} :tenant {:id ...}}}})
  :rcf)
