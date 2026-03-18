(ns app.domain.backend.expenses.services.suppliers.related-records
  (:require
    [app.domain.backend.expenses.services.related-records :as rr]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn- list-related-expenses
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:e.id :id]
                          [:e.purchased_at :purchased_at]
                          [:e.total_amount :total_amount]
                          [:e.currency :currency]
                          [:e.notes :notes]
                          [:e.created_at :created_at]
                          [:st.display_name :store_display_name]
                          [[:raw "json_agg(json_build_object('raw_label', aa.raw_label, 'canonical_name', a.canonical_name, 'qty', ei.qty, 'unit_price', ei.unit_price, 'line_total', ei.line_total) ORDER BY ei.created_at) FILTER (WHERE ei.id IS NOT NULL)"]
                           :items]]
                 :from [[:expenses :e]]
                 :left-join [[:stores :st] [:= :st.id :e.store_id]
                             [:expense_items :ei] [:= :ei.expense_id :e.id]
                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:articles :a] [:= :a.id :ei.article_id]]
                 :where [:= :e.supplier_id supplier-id]
                 :group-by [:e.id :st.display_name]
                 :order-by [[:e.purchased_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts
  [db supplier-id limit]
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
                                   [:e.id :expense_id]]
                 :from [[:expenses :e]]
                 :join [[:receipts :r] [:= :r.id :e.receipt_id]]
                 :where [:= :e.supplier_id supplier-id]
                 :order-by [[:r.created_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles-via-aliases
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:a.id :id]
                                   [:a.canonical_name :canonical_name]
                                   [:a.normalized_key :normalized_key]
                                   [:a.link :link]
                                   [:a.created_at :created_at]
                                   [:m.display_name :manufacturer_display_name]
                                   [:s.name :subcategory_name]]
                 :from [[:article_aliases :aa]]
                 :join [[:articles :a] [:= :a.id :aa.article_id]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :s] [:= :s.id :a.subcategory_id]]
                 :where [:and
                         [:= :aa.supplier_id supplier-id]
                         [:is-not :aa.article_id nil]]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles-via-expenses
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:a.id :id]
                                   [:a.canonical_name :canonical_name]
                                   [:a.normalized_key :normalized_key]
                                   [:a.link :link]
                                   [:a.created_at :created_at]
                                   [:m.display_name :manufacturer_display_name]
                                   [:s.name :subcategory_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:articles :a] [:= :a.id :ei.article_id]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :s] [:= :s.id :a.subcategory_id]]
                 :where [:and
                         [:= :e.supplier_id supplier-id]
                         [:is-not :ei.article_id nil]]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles
  [db supplier-id limit]
  (rr/merge-related-rows
    limit
    (list-related-articles-via-aliases db supplier-id limit)
    (list-related-articles-via-expenses db supplier-id limit)))

(defn- list-related-stores
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:st.id :id]
                          [:st.display_name :display_name]
                          [:st.normalized_key :normalized_key]
                          [:st.address :address]
                          [:st.created_at :created_at]
                          [:c.name :city]]
                 :from [[:stores :st]]
                 :left-join [[:cities :c] [:= :c.id :st.city_id]]
                 :where [:= :st.supplier_id supplier-id]
                 :order-by [[:st.display_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-related-records
  [db supplier-id {:keys [type limit]}]
  (when-not supplier-id
    (throw (ex-info "supplier-id is required" {:status 400})))
  (let [related-type (rr/normalize-related-type type)
        related-limit (rr/clamp-related-limit limit)]
    (case related-type
      :expenses (list-related-expenses db supplier-id related-limit)
      :receipts (list-related-receipts db supplier-id related-limit)
      :articles (list-related-articles db supplier-id related-limit)
      :stores (list-related-stores db supplier-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: expenses, receipts, articles, stores."
               {:status 400 :type type})))))

;; ============================================================================
;; Count helpers
;; ============================================================================

(defn- count-related-expenses [db supplier-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:count :*] :cnt]]
                       :from [[:expenses :e]]
                       :where [:= :e.supplier_id supplier-id]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-receipts [db supplier-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:raw "COUNT(DISTINCT r.id)"] :cnt]]
                       :from [[:expenses :e]]
                       :join [[:receipts :r] [:= :r.id :e.receipt_id]]
                       :where [:= :e.supplier_id supplier-id]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-articles [db supplier-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:count :*] :cnt]]
                       :from [[{:union [{:select [[:a.id :id]]
                                         :from [[:article_aliases :aa]]
                                         :join [[:articles :a] [:= :a.id :aa.article_id]]
                                         :where [:and
                                                 [:= :aa.supplier_id supplier-id]
                                                 [:is-not :aa.article_id nil]]}
                                        {:select [[:a.id :id]]
                                         :from [[:expense_items :ei]]
                                         :join [[:expenses :e] [:= :e.id :ei.expense_id]
                                                [:articles :a] [:= :a.id :ei.article_id]]
                                         :where [:and
                                                 [:= :e.supplier_id supplier-id]
                                                 [:is-not :ei.article_id nil]]}]}
                               :sub]]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn- count-related-stores [db supplier-id]
  (:cnt (jdbc/execute-one! db
          (sql/format {:select [[[:count :*] :cnt]]
                       :from [[:stores :st]]
                       :where [:= :st.supplier_id supplier-id]})
          {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-all-related
  "Count related records for all types for a supplier.
  Returns a map of type string to count."
  [db supplier-id]
  (when-not supplier-id
    (throw (ex-info "supplier-id is required" {:status 400})))
  {"expenses" (or (count-related-expenses db supplier-id) 0)
   "receipts" (or (count-related-receipts db supplier-id) 0)
   "articles" (or (count-related-articles db supplier-id) 0)
   "stores" (or (count-related-stores db supplier-id) 0)})
