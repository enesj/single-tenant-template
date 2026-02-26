(ns app.domain.backend.expenses.services.user-expense-reports.suppliers
  (:require
    [app.domain.backend.expenses.services.user-expense-reports.shared :as shared]))

(def ^:private default-supplier-report-limit 20)
(def ^:private default-monthly-limit 10)
(def ^:private default-alias-limit 10)

(defn top
  "Top suppliers ranked by total spending grouped by currency."
  [db user-id {:keys [limit] :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        limit* (-> (or limit default-supplier-report-limit) long (max 1) (min 100))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [supplier-rows (shared/supplier-breakdown-by-currency db user-id opts)
          totals-by-currency (->> (shared/summary-by-currency db user-id opts)
                               (map (juxt :currency (comp shared/bd :total_amount)))
                               (into {}))]
      (->> supplier-rows
        (map (fn [row]
               (let [supplier-total (shared/bd (:total_amount row))
                     grand-total (get totals-by-currency (:currency row) 0M)
                     share-pct (if (pos? (compare grand-total 0M))
                                 (* 100.0 (/ (double supplier-total) (double grand-total)))
                                 0.0)]
                 (assoc row :share_pct share-pct))))
        (sort-by (fn [row]
                   [(- (double (shared/bd (:total_amount row))))
                    (or (:supplier_name row) "")
                    (str (:supplier_id row))
                    (or (:currency row) "")]))
        (take limit*)
        vec))))

(defn stores
  "Store-level supplier breakdown grouped by currency."
  [db user-id {:keys [supplier-id limit] :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        supplier-id (shared/ensure-uuid supplier-id)
        limit* (-> (or limit default-supplier-report-limit) long (max 1) (min 100))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (when-not supplier-id
      (throw (ex-info "supplier-id is required" {:status 400})))
    (let [opts* (assoc opts :supplier-id supplier-id)
          totals-by-currency (->> (shared/summary-by-currency db user-id opts*)
                               (map (juxt :currency (comp shared/bd :total_amount)))
                               (into {}))
          resolved-store-id-sql
          [:raw
           "COALESCE(
              e.store_id,
              sa_direct.store_id,
              (
                SELECT sa_fallback.store_id
                FROM receipts r_fallback
                LEFT JOIN store_aliases sa_fallback ON sa_fallback.id = r_fallback.store_alias_id
                WHERE r_fallback.expense_id = e.id
                  AND sa_fallback.store_id IS NOT NULL
                ORDER BY COALESCE(r_fallback.updated_at, r_fallback.created_at) DESC,
                         r_fallback.id DESC
                LIMIT 1
              )
            )"]
          store-name-sql [:raw "COALESCE(st.display_name, 'Unmapped store')"]
          rows (shared/query-many
                 db
                 {:select [[resolved-store-id-sql :store_id]
                           [store-name-sql :store_name]
                           [:ci.name :city_name]
                           :e.currency
                           [[:sum :e.total_amount] :total_amount]
                           [[:count :*] :expense_count]]
                  :from [[:expenses :e]]
                  :left-join [[:receipts :r_direct] [:= :r_direct.id :e.receipt_id]
                              [:store_aliases :sa_direct] [:= :sa_direct.id :r_direct.store_alias_id]
                              [:stores :st] [:= :st.id resolved-store-id-sql]
                              [:cities :ci] [:= :ci.id :st.city_id]]
                  :where (shared/base-where user-id opts*)
                  :group-by [resolved-store-id-sql store-name-sql :ci.name :e.currency]
                  :order-by [[[:sum :e.total_amount] :desc]
                             [store-name-sql :asc]
                             [:e.currency :asc]]
                  :limit limit*})]
      (->> rows
        (mapv (fn [row]
                (let [currency-total (get totals-by-currency (:currency row) 0M)
                      row-total (shared/bd (:total_amount row))
                      share-pct (if (pos? (compare currency-total 0M))
                                  (* 100.0 (/ (double row-total) (double currency-total)))
                                  0.0)]
                  (assoc row :share_pct share-pct))))))))

(defn monthly-trends
  "Month-by-month supplier totals for the top suppliers in scope."
  [db user-id {:keys [limit] :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        limit* (-> (or limit default-monthly-limit) long (max 1) (min 50))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [top-supplier-ids (->> (shared/query-many
                                  db
                                  {:select [:e.supplier_id
                                            [[:sum :e.total_amount] :total_amount]]
                                   :from [[:expenses :e]]
                                   :where (conj (shared/base-where user-id opts)
                                            [:is-not :e.supplier_id nil])
                                   :group-by [:e.supplier_id]
                                   :order-by [[[:sum :e.total_amount] :desc]
                                              [:e.supplier_id :asc]]
                                   :limit limit*})
                             (keep :supplier_id)
                             vec)]
      (if (empty? top-supplier-ids)
        []
        (->> (shared/query-many
               db
               {:select [:e.supplier_id
                         [:s.display_name :supplier_name]
                         [[:to_char :e.purchased_at [:inline "YYYY-MM"]] :month]
                         :e.currency
                         [[:sum :e.total_amount] :total_amount]
                         [[:count :*] :expense_count]]
                :from [[:expenses :e]]
                :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                :where (conj (shared/base-where user-id opts)
                         [:in :e.supplier_id top-supplier-ids])
                :group-by [:e.supplier_id
                           :s.display_name
                           [:to_char :e.purchased_at [:inline "YYYY-MM"]]
                           :e.currency]
                :order-by [[[:to_char :e.purchased_at [:inline "YYYY-MM"]] :asc]
                           [[:sum :e.total_amount] :desc]
                           [:s.display_name :asc]
                           [:e.currency :asc]]})
          vec)))))

(defn deep-dive
  "Supplier deep-dive report for a specific supplier."
  [db user-id {:keys [supplier-id alias-limit] :or {alias-limit default-alias-limit} :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        supplier-id (shared/ensure-uuid supplier-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (when-not supplier-id
      (throw (ex-info "supplier-id is required" {:status 400})))
    (let [opts* (assoc opts :supplier-id supplier-id)
          supplier-name (some-> (shared/query-one db {:select [:display_name]
                                                      :from [:suppliers]
                                                      :where [:= :id supplier-id]
                                                      :limit 1})
                          :display_name)
          summary (shared/summary-by-currency db user-id opts*)
          trend (shared/query-many
                  db
                  {:select [[[:to_char :e.purchased_at [:inline "YYYY-MM"]] :month]
                            :e.currency
                            [[:sum :e.total_amount] :total_amount]
                            [[:count :*] :expense_count]]
                   :from [[:expenses :e]]
                   :where (shared/base-where user-id opts*)
                   :group-by [[:to_char :e.purchased_at [:inline "YYYY-MM"]] :e.currency]
                   :order-by [[[:to_char :e.purchased_at [:inline "YYYY-MM"]] :asc]
                              [:e.currency :asc]]})
          top-aliases (shared/query-many
                        db
                        {:select [[:aa.id :alias_id]
                                  [[:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"] :alias_label]
                                  [:a.canonical_name :article_canonical_name]
                                  :e.currency
                                  [[:sum :ei.line_total] :total_amount]
                                  [[:count [:distinct :e.id]] :line_count]]
                         :from [[:expense_items :ei]]
                         :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                         :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a] [:= :a.id :aa.article_id]
                                     [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                                     [:categories :c] [:= :c.id :sc.category_id]
                                     [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                         :where (shared/item-base-where user-id opts*)
                         :group-by [:aa.id
                                    [:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"]
                                    :a.canonical_name
                                    :e.currency]
                         :order-by [[[:sum :ei.line_total] :desc]
                                    [[:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"] :asc]]
                         :limit (-> (or alias-limit default-alias-limit) long (max 1) (min 100))})]
      {:supplier-id supplier-id
       :supplier-name supplier-name
       :summary (vec summary)
       :trend (vec trend)
       :top-aliases (vec top-aliases)})))
