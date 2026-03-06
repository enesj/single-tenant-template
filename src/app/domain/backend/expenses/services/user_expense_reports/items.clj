(ns app.domain.backend.expenses.services.user-expense-reports.items
  (:require
    [app.domain.backend.expenses.services.user-expense-reports.shared :as shared]))

(def ^:private default-item-report-limit 20)
(def ^:private default-breakdown-limit 50)

(defn top-spending
  "Top product/item spending grouped by canonical article (merging aliases)."
  [db user-id {:keys [limit] :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        limit* (-> (or limit default-item-report-limit) long (max 1) (min 100))
        resolved-store-id-sql [:raw "COALESCE(e.store_id, sa_receipt.store_id)"]
        resolved-item-id-sql [:raw "COALESCE(aa.article_id, ei.alias_id)"]
        item-label-sql [:raw "COALESCE(a.canonical_name, aa.raw_label, 'Unmapped item')"]]
    (shared/query-many
      db
      {:select [[resolved-item-id-sql :alias_id]
                [item-label-sql :alias_label]
                [item-label-sql :article_canonical_name]
                :e.currency
                [[:sum :ei.line_total] :total_amount]
                [[:sum :ei.qty] :qty_total]
                [[:count [:distinct :e.id]] :line_count]
                [[:count [:distinct resolved-store-id-sql]] :store_count]
                [[:count [:distinct :e.supplier_id]] :supplier_count]]
       :from [[:expense_items :ei]]
       :join [[:expenses :e] [:= :e.id :ei.expense_id]]
       :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                   [:articles :a] [:= :a.id :aa.article_id]
                   [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                   [:categories :c] [:= :c.id :sc.category_id]
                   [:manufacturers :m] [:= :m.id :a.manufacturer_id]
                   [:receipts :r] [:= :r.id :e.receipt_id]
                   [:store_aliases :sa_receipt] [:= :sa_receipt.id :r.store_alias_id]
                   [:stores :st] [:= :st.id resolved-store-id-sql]]
       :where (shared/item-base-where user-id opts)
       :group-by [resolved-item-id-sql
                  item-label-sql
                  :e.currency]
       :order-by [[[:sum :ei.line_total] :desc]
                  [item-label-sql :asc]]
       :limit limit*})))

(defn top-breakdown
  "Break down a top item (article alias) by suppliers and stores."
  [db user-id alias-id {:keys [limit] :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        alias-id (shared/ensure-uuid alias-id)
        limit* (-> (or limit default-breakdown-limit) long (max 1) (min 200))
        resolved-store-id-sql [:raw "COALESCE(e.store_id, sa_receipt.store_id)"]
        store-name-sql [:raw "COALESCE(st.display_name, 'Unmapped store')"]
        where-clause (conj (shared/item-base-where user-id opts)
                       [:or
                        [:= :aa.article_id alias-id]
                        [:= :ei.alias_id alias-id]])]
    (when-not alias-id
      (throw (ex-info "alias-id is required" {:status 400})))
    {:suppliers
     (shared/query-many
       db
       {:select [:e.supplier_id
                 [:s.display_name :supplier_name]
                 :e.currency
                 [[:sum :ei.line_total] :total_amount]
                 [[:sum :ei.qty] :qty_total]
                 [[:count [:distinct :e.id]] :line_count]]
        :from [[:expense_items :ei]]
        :join [[:expenses :e] [:= :e.id :ei.expense_id]
               [:suppliers :s] [:= :s.id :e.supplier_id]]
        :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                    [:articles :a] [:= :a.id :aa.article_id]
                    [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                    [:categories :c] [:= :c.id :sc.category_id]
                    [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
        :where where-clause
        :group-by [:e.supplier_id :s.display_name :e.currency]
        :order-by [[[:sum :ei.line_total] :desc]
                   [:s.display_name :asc]
                   [:e.currency :asc]]
        :limit limit*})

     :stores
     (shared/query-many
       db
       {:select [[resolved-store-id-sql :store_id]
                 [store-name-sql :store_name]
                 [:s.display_name :supplier_name]
                 :e.currency
                 [[:sum :ei.line_total] :total_amount]
                 [[:sum :ei.qty] :qty_total]
                 [[:count [:distinct :e.id]] :line_count]]
        :from [[:expense_items :ei]]
        :join [[:expenses :e] [:= :e.id :ei.expense_id]
               [:suppliers :s] [:= :s.id :e.supplier_id]]
        :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                    [:articles :a] [:= :a.id :aa.article_id]
                    [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                    [:categories :c] [:= :c.id :sc.category_id]
                    [:manufacturers :m] [:= :m.id :a.manufacturer_id]
                    [:receipts :r] [:= :r.id :e.receipt_id]
                    [:store_aliases :sa_receipt] [:= :sa_receipt.id :r.store_alias_id]
                    [:stores :st] [:= :st.id resolved-store-id-sql]]
        :where where-clause
        :group-by [resolved-store-id-sql store-name-sql :s.display_name :e.currency]
        :order-by [[[:sum :ei.line_total] :desc]
                   [store-name-sql :asc]
                   [:s.display_name :asc]
                   [:e.currency :asc]]
        :limit limit*})}))
