(ns app.domain.expenses.services.reports
  "Reporting queries for expenses."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn- base-where
  [{:keys [from to]}]
  (cond-> [:and
           [:is :deleted_at nil]
           [:= :is_posted true]]
    from (conj [:>= :purchased_at from])
    to (conj [:<= :purchased_at to])))

(defn get-summary
  "Return aggregate totals for a date range."
  [db opts]
  (jdbc/execute-one!
    db
    (sql/format {:select [[[:sum :total_amount] :total_amount]
                          [[[:count :*]] :expense_count]]
                 :from [:expenses]
                 :where (base-where opts)})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-breakdown-by-payer
  [db opts]
  (jdbc/execute!
    db
    (sql/format {:select [:payer_id
                          [[:sum :total_amount] :total_amount]
                          [[:count :*] :expense_count]]
                 :from [:expenses]
                 :where (base-where opts)
                 :group-by [:payer_id]
                 :order-by [[:total_amount :desc]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-breakdown-by-supplier
  [db opts]
  (jdbc/execute!
    db
    (sql/format {:select [:supplier_id
                          [[:sum :total_amount] :total_amount]
                          [[:count :*] :expense_count]]
                 :from [:expenses]
                 :where (base-where opts)
                 :group-by [:supplier_id]
                 :order-by [[:total_amount :desc]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-weekly-totals
  [db opts]
  (jdbc/execute!
    db
    (sql/format {:select [[[:raw "date_trunc('week', purchased_at)"] :week_start]
                          [[:sum :total_amount] :total_amount]]
                 :from [:expenses]
                 :where (base-where opts)
                 :group-by [[:raw "date_trunc('week', purchased_at)"]]
                 :order-by [[:raw "date_trunc('week', purchased_at)" :asc]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-monthly-totals
  [db opts]
  (jdbc/execute!
    db
    (sql/format {:select [[[:raw "date_trunc('month', purchased_at)"] :month]
                          [[:sum :total_amount] :total_amount]]
                 :from [:expenses]
                 :where (base-where opts)
                 :group-by [[:raw "date_trunc('month', purchased_at)"]]
                 :order-by [[:raw "date_trunc('month', purchased_at)" :asc]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-top-suppliers
  [db {:keys [limit] :or {limit 5} :as opts}]
  (jdbc/execute!
    db
    (sql/format {:select [:supplier_id
                          [[:sum :total_amount] :total_amount]
                          [[:count :*] :expense_count]]
                 :from [:expenses]
                 :where (base-where opts)
                 :group-by [:supplier_id]
                 :order-by [[:total_amount :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))
