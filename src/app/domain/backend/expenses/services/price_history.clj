(ns app.domain.backend.expenses.services.price-history
  "Record and query article price observations over time."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Persistence
;; ============================================================================

(defn record-observation!
  "Insert a price observation. Expects keys:
   :article_id, :supplier_id, :expense_item_id (optional),
   :observed_at (timestamp, defaults to now), :qty, :unit_price,
   :line_total, :currency.
   Returns inserted row (as unqualified lower-case keys)."
  [db {:keys [article_id supplier_id expense_item_id observed_at qty unit_price line_total currency] :as _obs}]
  (when (and article_id supplier_id line_total)
    (let [row {:id (UUID/randomUUID)
               :article_id article_id
               :supplier_id supplier_id
               :expense_item_id expense_item_id
               :observed_at (or observed_at [:now])
               :qty qty
               :unit_price unit_price
               :line_total line_total
               :currency (when currency [:cast currency :currency])}
          sql-map {:insert-into :price_observations
                   :values [row]
                   :returning [:*]}]
      (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))))

;; ============================================================================
;; Queries
;; ============================================================================
