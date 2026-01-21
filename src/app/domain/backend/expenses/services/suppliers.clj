(ns app.domain.backend.expenses.services.suppliers
  "Supplier CRUD services using factory pattern."
  (:require
   [app.domain.backend.expenses.services.service-configs :as configs]
   [app.domain.backend.expenses.services.services-factory :as factory]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :supplier))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; NOTE: We intentionally avoid legacy alias vars like `get-supplier`/`create-supplier!`.
;; Admin routes and user handlers resolve operations via the `service` map or
;; explicit overrides in this namespace.

;; NOTE: Suppliers are hard-deleted. Deletion is blocked by FK RESTRICT when
;; expenses exist for the supplier.

;; ============================================================================
;; Normalization (re-exported for external use)
;; ============================================================================

(def normalize-supplier-key configs/normalize-supplier-key)

;; ==========================================================================
;; Archived supplier-aware operations
;; ==========================================================================

(def ^:private supplier-order-by
  {;; Accept both kebab-case and snake_case query values.
   :display-name :display_name
   :display_name :display_name
   :normalized-key :normalized_key
   :normalized_key :normalized_key
   :created-at :created_at
   :created_at :created_at
   :updated-at :updated_at
   :updated_at :updated_at})

(defn- suppliers-where
  [{:keys [search]}]
  (let [search-filter (when (seq search)
                        [:or
                         [:ilike :display_name (str "%" search "%")]
                         [:ilike :normalized_key (str "%" search "%")]])
        filters (->> [search-filter]
                  (remove nil?))]
    (when (seq filters)
      (into [:and] filters))))

(defn list-suppliers
  "List suppliers.

  Options:
  - :limit/:offset
  - :order-by (keyword)
  - :order-dir (:asc|:desc)
  - :search (string)
    "
    [db {:keys [limit offset order-by order-dir search]
       :or {limit 50 offset 0 order-dir :asc}}]
  (let [order-column (get supplier-order-by order-by :display_name)
        order-direction (if (= :asc order-dir) :asc :desc)
      where (suppliers-where {:search search})]
    (jdbc/execute!
      db
      (sql/format
        (cond-> {:select [:*]
                 :from [:suppliers]
                 :limit limit
                 :offset offset
                 :order-by [[order-column order-direction]]}
          where (assoc :where where)))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn active-expenses-counts-by-supplier
  "Return a map of supplier-id -> expenses count." 
  [db supplier-ids]
  (if (seq supplier-ids)
    (let [rows
          (jdbc/execute!
            db
            (sql/format {:select [:supplier_id [[:count :*] :active_expenses_count]]
                         :from [:expenses]
                         :where [:and
                                 [:in :supplier_id supplier-ids]]
                         :group-by [:supplier_id]})
            {:builder-fn rs/as-unqualified-lower-maps})]
      (reduce (fn [acc {:keys [supplier_id active_expenses_count]}]
                (assoc acc supplier_id (long (or active_expenses_count 0))))
        {}
        rows))
    {}))

(defn count-suppliers
  "Count suppliers.

  Accepts the same filtering keys as `list-suppliers` (notably :search)."
  [db {:keys [search]}]
  (let [where (suppliers-where {:search search})]
    (:total
      (jdbc/execute-one!
        db
        (sql/format
          (cond-> {:select [[[:count :*] :total]]
                   :from [:suppliers]}
            where (assoc :where where)))
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn search-suppliers
  "Autocomplete supplier search (2+ chars)."
  [db query {:keys [limit]
             :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [where (suppliers-where {:search query})]
      (jdbc/execute!
        db
        (sql/format
          (cond-> {:select [:*]
                   :from [:suppliers]
                   :order-by [[:display_name :asc]]
                   :limit limit}
            where (assoc :where where)))
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-supplier!
  "Hard delete a supplier. Returns the deleted row or nil."
  [db supplier-id]
  (jdbc/execute-one!
    db
    (sql/format {:delete-from :suppliers
                 :where [:= :id supplier-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn find-by-normalized-key
  "Find supplier by normalized key for deduplication."
  [db normalized-key]
  (when normalized-key
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:suppliers]
                   :where [:= :normalized_key normalized-key]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-or-create-supplier!
  "Find supplier by normalized name or create new one.
   Returns {:existing? bool :supplier {...}}"
  [db display-name & [{:keys [address]}]]
  (let [normalized (normalize-supplier-key display-name)]
    (if-let [existing (find-by-normalized-key db normalized)]
      {:existing? true :supplier existing}
      {:existing? false
       :supplier ((:create! service) db {:display_name display-name
                                         :address address})})))

(defn search-suppliers-autocomplete
  "Search suppliers for autocomplete with fuzzy matching."
  [db query {:keys [limit] :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [search-pattern (str "%" query "%")]
      (jdbc/execute!
        db
        (sql/format {:select [:*]
                     :from [:suppliers]
                     :where [:or
                             [:ilike :display_name search-pattern]
                             [:ilike :normalized_key search-pattern]]
                     :order-by [[:display_name :asc]]
                     :limit limit})
        {:builder-fn rs/as-unqualified-lower-maps}))))
