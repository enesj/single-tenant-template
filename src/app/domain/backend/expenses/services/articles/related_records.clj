(ns app.domain.backend.expenses.services.articles.related-records
  "Related-record queries for articles."
  (:require
    [app.domain.backend.expenses.services.related-records :as shared-related]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn- article-item-linkage-where
  [article-id]
  [:or
   [:= :ei.article_id article-id]
   [:= :aa.article_id article-id]])

(defn- list-related-expenses
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:e.id :id]
                                   [:e.purchased_at :purchased_at]
                                   [:e.total_amount :total_amount]
                                   [:e.currency :currency]
                                   [:e.notes :notes]
                                   [:e.receipt_id :receipt_id]
                                   [:e.created_at :created_at]
                                   [:s.display_name :supplier_display_name]
                                   [:st.display_name :store_display_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:suppliers :s] [:= :s.id :e.supplier_id]
                             [:stores :st] [:= :st.id :e.store_id]]
                 :where (article-item-linkage-where article-id)
                 :order-by [[:e.purchased_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts-expense-linked
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:r.id :id]
                                   [:r.original_filename :original_filename]
                                   [:r.status :status]
                                   [:r.supplier_guess :supplier_guess]
                                   [:r.total_amount_guess :total_amount_guess]
                                   [:r.currency_guess :currency_guess]
                                   [:r.created_at :created_at]
                                   [:r.updated_at :updated_at]
                                   [:e.id :expense_id]
                                   [:s.display_name :supplier_display_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:receipts :r] [:= :r.id :e.receipt_id]]
                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:suppliers :s] [:= :s.id :e.supplier_id]]
                 :where (article-item-linkage-where article-id)
                 :order-by [[:r.created_at :desc]
                            [:r.id :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts-alias-linked
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:r.id :id]
                                   [:r.original_filename :original_filename]
                                   [:r.status :status]
                                   [:r.supplier_guess :supplier_guess]
                                   [:r.total_amount_guess :total_amount_guess]
                                   [:r.currency_guess :currency_guess]
                                   [:r.created_at :created_at]
                                   [:r.updated_at :updated_at]
                                   [:r.expense_id :expense_id]
                                   [[:coalesce :store_supplier.display_name :supplier_alias_supplier.display_name]
                                    :supplier_display_name]]
                 :from [[:receipts :r]]
                 :left-join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                             [:stores :st] [:= :st.id :sta.store_id]
                             [:suppliers :store_supplier] [:= :store_supplier.id :st.supplier_id]
                             [:supplier_aliases :sa] [:= :sa.id :r.supplier_alias_id]
                             [:suppliers :supplier_alias_supplier] [:= :supplier_alias_supplier.id :sa.supplier_id]]
                 :where [:exists {:select [1]
                                  :from [[:article_aliases :aa]]
                                  :where [:and
                                          [:= :aa.article_id article-id]
                                          [:or
                                           [:= :aa.supplier_id :sa.supplier_id]
                                           [:= :aa.supplier_id :st.supplier_id]]]}]
                 :order-by [[:r.created_at :desc]
                            [:r.id :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts
  "List receipts where this article actually appears in expense items.
  Intentionally excludes the broad alias-linked query (which over-counts by
  including all receipts from any supplier that sells this article)."
  [db article-id limit]
  (list-related-receipts-expense-linked db article-id limit))

(defn- list-related-providers
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:s.id :id]
                          [:s.display_name :display_name]
                          [:s.normalized_key :normalized_key]
                          [:s.address :address]
                          [:s.created_at :created_at]]
                 :from [[:suppliers :s]]
                 :where [:or
                         [:exists {:select [1]
                                   :from [[:article_aliases :aa]]
                                   :where [:and
                                           [:= :aa.article_id article-id]
                                           [:= :aa.supplier_id :s.id]]}]
                         [:exists {:select [1]
                                   :from [[:expense_items :ei]]
                                   :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                   :where [:and
                                           [:= :ei.article_id article-id]
                                           [:= :e.supplier_id :s.id]]}]]
                 :order-by [[:s.display_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-stores-expense-linked
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:st.id :id]
                                   [:st.display_name :display_name]
                                   [:st.normalized_key :normalized_key]
                                   [:st.address :address]

                                   [:st.created_at :created_at]
                                   [:s.display_name :supplier_display_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:stores :st] [:= :st.id :e.store_id]]
                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:suppliers :s] [:= :s.id :st.supplier_id]]
                 :where (article-item-linkage-where article-id)
                 :order-by [[:st.display_name :asc]
                            [:st.id :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-stores-alias-linked
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:st.id :id]
                                   [:st.display_name :display_name]
                                   [:st.normalized_key :normalized_key]
                                   [:st.address :address]

                                   [:st.created_at :created_at]
                                   [:s.display_name :supplier_display_name]]
                 :from [[:stores :st]]
                 :left-join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                 :where [:exists {:select [1]
                                  :from [[:article_aliases :aa]]
                                  :where [:and
                                          [:= :aa.article_id article-id]
                                          [:= :aa.supplier_id :st.supplier_id]]}]
                 :order-by [[:st.display_name :asc]
                            [:st.id :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-stores
  "List stores where this article actually appears in expenses.
  Intentionally excludes the broad alias-linked query (which over-counts by
  including all stores of a supplier that sells this article)."
  [db article-id limit]
  (list-related-stores-expense-linked db article-id limit))

(defn- list-related-manufacturers
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:m.id :id]
                                   [:m.display_name :display_name]
                                   [:m.normalized_key :normalized_key]
                                   [:m.created_at :created_at]
                                   [:m.updated_at :updated_at]]
                 :from [[:articles :a]]
                 :join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                 :where [:= :a.id article-id]
                 :order-by [[:m.display_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-subcategories
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:s.id :id]
                                   [:s.name :name]
                                   [:s.description :description]
                                   [:s.category_id :category_id]
                                   [:s.created_at :created_at]
                                   [:s.updated_at :updated_at]
                                   [:c.name :category_name]]
                 :from [[:articles :a]]
                 :join [[:subcategories :s] [:= :s.id :a.subcategory_id]]
                 :left-join [[:categories :c] [:= :c.id :s.category_id]]
                 :where [:= :a.id article-id]
                 :order-by [[:s.name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-related-records
  "List records related to an article by type.

  Supported types: expenses, receipts, providers (suppliers), stores,
  manufacturers, subcategories."
  [db article-id {:keys [type limit]}]
  (when-not article-id
    (throw (ex-info "article-id is required" {:status 400})))
  (let [related-type (shared-related/normalize-related-type type)
        related-limit (shared-related/clamp-related-limit limit)]
    (case related-type
      :expenses (list-related-expenses db article-id related-limit)
      :receipts (list-related-receipts db article-id related-limit)
      :providers (list-related-providers db article-id related-limit)
      :stores (list-related-stores db article-id related-limit)
      :manufacturers (list-related-manufacturers db article-id related-limit)
      :subcategories (list-related-subcategories db article-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: expenses, receipts, providers, stores, manufacturers, subcategories."
               {:status 400 :type type})))))

;; ============================================================================
;; Count helpers (lightweight COUNT queries for the counts endpoint)
;; ============================================================================

(defn- count-related-expenses [db article-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:raw "COUNT(DISTINCT e.id)"] :cnt]]
                       :from [[:expense_items :ei]]
                       :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                       :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                       :where (article-item-linkage-where article-id)})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-receipts
  "Count receipts where this article actually appears in expense items.
  Intentionally excludes the broad alias-linked query (which over-counts by
  including all receipts from any supplier that sells this article)."
  [db article-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:raw "COUNT(DISTINCT r.id)"] :cnt]]
                       :from [[:expense_items :ei]]
                       :join [[:expenses :e] [:= :e.id :ei.expense_id]
                              [:receipts :r] [:= :r.id :e.receipt_id]]
                       :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                       :where (article-item-linkage-where article-id)})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-providers [db article-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:count :*] :cnt]]
                       :from [[:suppliers :s]]
                       :where [:or
                               [:exists {:select [1]
                                         :from [[:article_aliases :aa]]
                                         :where [:and
                                                 [:= :aa.article_id article-id]
                                                 [:= :aa.supplier_id :s.id]]}]
                               [:exists {:select [1]
                                         :from [[:expense_items :ei]]
                                         :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                         :where [:and
                                                 [:= :ei.article_id article-id]
                                                 [:= :e.supplier_id :s.id]]}]]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-stores
  "Count stores where this article actually appears in expenses.
  Excludes broad alias-linked stores (all stores of a supplier that sells
  this article) to avoid over-counting."
  [db article-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:raw "COUNT(DISTINCT st.id)"] :cnt]]
                       :from [[:expense_items :ei]]
                       :join [[:expenses :e] [:= :e.id :ei.expense_id]
                              [:stores :st] [:= :st.id :e.store_id]]
                       :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]]
                       :where (article-item-linkage-where article-id)})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-manufacturers [db article-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:raw "COUNT(DISTINCT m.id)"] :cnt]]
                       :from [[:articles :a]]
                       :join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                       :where [:= :a.id article-id]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-subcategories [db article-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:raw "COUNT(DISTINCT s.id)"] :cnt]]
                       :from [[:articles :a]]
                       :join [[:subcategories :s] [:= :s.id :a.subcategory_id]]
                       :where [:= :a.id article-id]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-all-related
  "Count related records for all types for an article.
  Returns a map of type string to count."
  [db article-id]
  (when-not article-id
    (throw (ex-info "article-id is required" {:status 400})))
  {"expenses" (or (count-related-expenses db article-id) 0)
   "receipts" (or (count-related-receipts db article-id) 0)
   "providers" (or (count-related-providers db article-id) 0)
   "stores" (or (count-related-stores db article-id) 0)
   "manufacturers" (or (count-related-manufacturers db article-id) 0)
   "subcategories" (or (count-related-subcategories db article-id) 0)})
