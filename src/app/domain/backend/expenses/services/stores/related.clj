(ns app.domain.backend.expenses.services.stores.related
  (:require
    [app.domain.backend.expenses.services.related-records :as rr]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn- list-related-expenses
  [db store-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:e.id :id]
                          [:e.purchased_at :purchased_at]
                          [:e.total_amount :total_amount]
                          [:e.currency :currency]
                          [:e.notes :notes]
                          [:e.created_at :created_at]
                          [:s.display_name :supplier_display_name]
                          [[:raw "json_agg(json_build_object('raw_label', aa.raw_label, 'canonical_name', a.canonical_name, 'qty', ei.qty, 'unit_price', ei.unit_price, 'line_total', ei.line_total) ORDER BY ei.created_at) FILTER (WHERE ei.id IS NOT NULL)"]
                           :items]]
                 :from [[:expenses :e]]
                 :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                             [:expense_items :ei] [:= :ei.expense_id :e.id]
                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:articles :a] [:= :a.id :ei.article_id]]
                 :where [:= :e.store_id store-id]
                 :group-by [:e.id :s.display_name]
                 :order-by [[:e.purchased_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts
  [db store-id limit]
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
                 :from [[:expenses :e]]
                 :join [[:receipts :r] [:= :r.id :e.receipt_id]]
                 :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                 :where [:= :e.store_id store-id]
                 :order-by [[:r.created_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles-direct
  [db store-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:a.id :id]
                                   [:a.canonical_name :canonical_name]
                                   [:a.normalized_key :normalized_key]
                                   [:a.link :link]
                                   [:a.created_at :created_at]
                                   [:m.display_name :manufacturer_display_name]
                                   [:sc.name :subcategory_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:articles :a] [:= :a.id :ei.article_id]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :sc] [:= :sc.id :a.subcategory_id]]
                 :where [:and
                         [:= :e.store_id store-id]
                         [:is-not :ei.article_id nil]]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles-via-aliases
  [db store-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:a.id :id]
                                   [:a.canonical_name :canonical_name]
                                   [:a.normalized_key :normalized_key]
                                   [:a.link :link]
                                   [:a.created_at :created_at]
                                   [:m.display_name :manufacturer_display_name]
                                   [:sc.name :subcategory_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                        [:articles :a] [:= :a.id :aa.article_id]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :sc] [:= :sc.id :a.subcategory_id]]
                 :where [:and
                         [:= :e.store_id store-id]
                         [:is-not :aa.article_id nil]]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles
  [db store-id limit]
  (rr/merge-related-rows
    limit
    (list-related-articles-direct db store-id limit)
    (list-related-articles-via-aliases db store-id limit)))

(defn list-related-records
  "List records related to a store by type."
  [db store-id {:keys [type limit]}]
  (when-not store-id
    (throw (ex-info "store-id is required" {:status 400})))
  (let [related-type (rr/normalize-related-type type)
        related-limit (rr/clamp-related-limit limit)]
    (case related-type
      :expenses (list-related-expenses db store-id related-limit)
      :receipts (list-related-receipts db store-id related-limit)
      :articles (list-related-articles db store-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: expenses, receipts, articles."
               {:status 400 :type type})))))
