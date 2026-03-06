(ns app.domain.backend.expenses.services.user-expense-reports.categories
  (:require
    [app.domain.backend.expenses.services.user-expense-reports.shared :as shared]))

(defn allocation
  "Category allocation based on expense items with uncategorized bucket support."
  [db user-id opts]
  (let [user-id (shared/ensure-uuid user-id)
        category-key-sql [:raw "COALESCE(c.id::text, 'uncategorized')"]
        category-name-sql [:raw "COALESCE(c.name, 'Uncategorized')"]
        subcategory-key-sql [:raw "COALESCE(sc.id::text, 'uncategorized')"]
        subcategory-name-sql [:raw "COALESCE(sc.name, 'Uncategorized')"]
        joins [[:expenses :e] [:= :e.id :ei.expense_id]]
        left-joins [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                    [:articles :a] [:= :a.id :aa.article_id]
                    [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                    [:categories :c] [:= :c.id :sc.category_id]
                    [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
        where-clause (shared/item-base-where user-id opts)
        category-totals-query {:select [[category-key-sql :category_key]
                                        [category-name-sql :category_name]
                                        :e.currency
                                        [[:sum :ei.line_total] :total_amount]
                                        [[:count [:distinct :e.id]] :line_count]]
                               :from [[:expense_items :ei]]
                               :join joins
                               :left-join left-joins
                               :where where-clause
                               :group-by [category-key-sql
                                          category-name-sql
                                          :e.currency]}
        subcategory-totals-query {:select [[category-key-sql :category_key]
                                           :e.currency
                                           [subcategory-key-sql :subcategory_key]
                                           [subcategory-name-sql :subcategory_name]
                                           [[:sum :ei.line_total] :subcategory_total_amount]]
                                  :from [[:expense_items :ei]]
                                  :join joins
                                  :left-join left-joins
                                  :where where-clause
                                  :group-by [category-key-sql
                                             :e.currency
                                             subcategory-key-sql
                                             subcategory-name-sql]}
        rows (shared/query-many
               db
               {:with [[:category_totals category-totals-query]
                       [:subcategory_totals subcategory-totals-query]]
                :select [[:ct.category_key :category_key]
                         [:ct.category_name :category_name]
                         [:ct.currency :currency]
                         [:ct.total_amount :total_amount]
                         [:ct.line_count :line_count]
                         [:st.subcategory_key :subcategory_key]
                         [:st.subcategory_name :subcategory_name]
                         [[:coalesce :st.subcategory_total_amount 0M] :subcategory_total_amount]]
                :from [[:category_totals :ct]]
                :left-join [[:subcategory_totals :st]
                            [:and
                             [:= :st.category_key :ct.category_key]
                             [:= :st.currency :ct.currency]]]
                :order-by [[:ct.total_amount :desc]
                           [:ct.category_name :asc]
                           [:st.subcategory_total_amount :desc]
                           [:st.subcategory_name :asc]]})
        rows* (shared/ensure-uncategorized rows (:currency opts))
        category-rows (->> rows*
                        (reduce (fn [acc row]
                                  (assoc acc
                                    [(:category_key row) (:currency row)]
                                    {:category_key (:category_key row)
                                     :currency (:currency row)
                                     :total_amount (or (:total_amount row) 0M)}))
                          {})
                        vals
                        shared/allocation-with-percentages)
        allocation-by-category (into {}
                                 (map (fn [row]
                                        [[(:category_key row) (:currency row)]
                                         (:allocation_pct row)]))
                                 category-rows)]
    (->> rows*
      (mapv (fn [row]
              (assoc row
                :allocation_pct (or (get allocation-by-category
                                      [(:category_key row) (:currency row)])
                                  0.0)))))))
