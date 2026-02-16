(ns app.domain.backend.expenses.services.price-observations
  "CRUD helpers for price observations."
  (:require
    [app.domain.backend.expenses.services.price-history :as price-history]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.shared.type-conversion :as type-conv]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :price-observation))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; NOTE: Avoid legacy alias vars like `get-price-observation`/`update-price-observation!`.
;; Admin routes resolve operations via the `service` map (or explicit overrides).
;; We keep custom wrappers below (list/create) to support filtering + price-history behavior.

(def ^:private try-uuid type-conv/try-parse-uuid)

(defn list-price-observations
  "List price observations.

  Supports optional filters (used by supplier detail views):
  - :supplier-id / :supplier_id
  - :article-id / :article_id
  - :from, :to (timestamp strings)

  Keeps the same base signature as the generated service (db, opts)."
  [db {:keys [limit offset order-by order-dir search supplier-id supplier_id article-id article_id from to]
       :or {limit 100 offset 0 order-dir :asc}
       :as opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        article-uuid (try-uuid (or article-id article_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :po/supplier_id supplier-uuid])
                       article-uuid (conj [:= :po/article_id article-uuid])
                       from (conj [:>= :po/observed_at from])
                       to (conj [:<= :po/observed_at to]))
        config* (assoc config :base-filters base-filters)
        base-query (factory/build-query-with-filters
                     config*
                     {:limit limit
                      :offset offset
                      :order-by order-by
                      :order-dir order-dir})
        final-query (factory/apply-search-filter base-query (:search-fields config*) search)]
    ;; NOTE: When no filters are provided, fall back to the base behavior.
    ;; This preserves compatibility for callers that rely on the generated list.
    (if (or supplier-uuid article-uuid from to)
      (jdbc/execute! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})
      ((:list service) db opts))))

(defn count-price-observations
  "Count price observations with optional filters.

  Supports optional filters:
  - :supplier-id / :supplier_id
  - :article-id / :article_id
  - :from, :to (timestamp strings)"
  [db {:keys [search supplier-id supplier_id article-id article_id from to] :as _opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        article-uuid (try-uuid (or article-id article_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :po/supplier_id supplier-uuid])
                       article-uuid (conj [:= :po/article_id article-uuid])
                       from (conj [:>= :po/observed_at from])
                       to (conj [:<= :po/observed_at to]))
        config* (assoc config :base-filters base-filters)]
    (if (or supplier-uuid article-uuid from to)
      (let [base-query (factory/build-base-query config*)
            count-query (-> base-query
                          (dissoc :order-by :limit :offset)
                          (assoc :select [[[:count :*] :total]]))
            final-query (factory/apply-search-filter count-query (:search-fields config*) search)]
        (:total (jdbc/execute-one! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})))
      ((:count service) db {:search search}))))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn create-price-observation!
  "Create a price observation using price-history helper."
  [db {:keys [article_id supplier_id expense_item_id observed_at unit_price currency]}]
  (price-history/record-observation!
    db {:article_id article_id
        :supplier_id supplier_id
        :expense_item_id expense_item_id
        :observed_at observed_at
        :unit_price unit_price
        :currency currency}))