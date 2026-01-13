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

;; NOTE: Suppliers are *archived* (soft-deleted) instead of hard-deleted, because
;; expenses are soft-deleted and keep FK references to suppliers.
;;
;; We therefore override list/search/count/delete to respect `archived_at`.

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
   :updated_at :updated_at
   :archived-at :archived_at
   :archived_at :archived_at})

(defn- suppliers-where
  [{:keys [include_archived search]}]
  (let [archived-filter (when-not include_archived
                          [:is :archived_at nil])
        search-filter (when (seq search)
                        [:or
                         [:ilike :display_name (str "%" search "%")]
                         [:ilike :normalized_key (str "%" search "%")]])
        filters (->> [archived-filter search-filter]
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
  - :include_archived (boolean)"
  [db {:keys [limit offset order-by order-dir include_archived search]
       :or {limit 50 offset 0 order-dir :asc}}]
  (let [order-column (get supplier-order-by order-by :display_name)
        order-direction (if (= :asc order-dir) :asc :desc)
        where (suppliers-where {:include_archived include_archived
                                :search search})]
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
  "Return a map of supplier-id -> active expenses count.

  Active expenses are expenses with `deleted_at IS NULL`.

  Intended for UI affordances (e.g. disabling purge actions) without needing to
  call purge-preview for each row." 
  [db supplier-ids]
  (if (seq supplier-ids)
    (let [rows
          (jdbc/execute!
            db
            (sql/format {:select [:supplier_id [[:count :*] :active_expenses_count]]
                         :from [:expenses]
                         :where [:and
                                 [:in :supplier_id supplier-ids]
                                 [:is :deleted_at nil]]
                         :group-by [:supplier_id]})
            {:builder-fn rs/as-unqualified-lower-maps})]
      (reduce (fn [acc {:keys [supplier_id active_expenses_count]}]
                (assoc acc supplier_id (long (or active_expenses_count 0))))
        {}
        rows))
    {}))

(defn count-suppliers
  "Count suppliers.

  Accepts the same filtering keys as `list-suppliers` (notably :search and
  :include_archived)."
  [db {:keys [include_archived search]}]
  (let [where (suppliers-where {:include_archived include_archived
                                :search search})]
    (:total
      (jdbc/execute-one!
        db
        (sql/format
          (cond-> {:select [[[:count :*] :total]]
                   :from [:suppliers]}
            where (assoc :where where)))
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn search-suppliers
  "Autocomplete supplier search (2+ chars).

  Respects :include_archived and defaults to excluding archived suppliers."
  [db query {:keys [limit include_archived]
             :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [where (suppliers-where {:include_archived include_archived
                                  :search query})]
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
  "Archive a supplier by setting `archived_at`.

  Returns true when the supplier exists (whether newly archived or already archived),
  otherwise false."
  [db supplier-id]
  (let [archived-now?
        (pos?
          (::jdbc/update-count
            (jdbc/execute-one!
              db
              (sql/format {:update :suppliers
                           :set {:archived_at [:now]
                                 :updated_at [:now]}
                           :where [:and
                                   [:= :id supplier-id]
                                   [:is :archived_at nil]]}))))]
    (if archived-now?
      true
      ;; Idempotent delete: if it's already archived, treat as success.
      (boolean
        (jdbc/execute-one!
          db
          (sql/format {:select [:id]
                       :from [:suppliers]
                       :where [:and
                               [:= :id supplier-id]
                               [:is-not :archived_at nil]]
                       :limit 1}))))))

(defn unarchive-supplier!
  "Un-archive a supplier (sets archived_at to NULL)."
  [db supplier-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :suppliers
                 :set {:archived_at nil
                       :updated_at [:now]}
                 :where [:= :id supplier-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn purge-supplier!
  "Permanently delete a supplier.

  Safety rules:
  - Supplier must be archived first.
  - Supplier must have *no active* expenses (expenses.deleted_at IS NULL).
  - Soft-deleted expenses (expenses.deleted_at IS NOT NULL) for this supplier
    are hard-deleted first (expense_items will be removed via FK cascade).

  Returns a summary map.

  Throws ex-info with :status 404/409 for expected conflicts."
  [db supplier-id]
  (jdbc/with-transaction [tx db]
    (let [supplier
          (jdbc/execute-one!
            tx
            (sql/format {:select [:id :archived_at]
                         :from [:suppliers]
                         :where [:= :id supplier-id]
                         :limit 1})
            {:builder-fn rs/as-unqualified-lower-maps})]
      (when-not supplier
        (throw (ex-info "Supplier not found" {:status 404 :supplier-id supplier-id})))
      (when-not (:archived_at supplier)
        (throw (ex-info "Supplier must be archived before it can be purged"
                 {:status 409 :supplier-id supplier-id})))

      (let [active-expenses
            (:total
              (jdbc/execute-one!
                tx
                (sql/format {:select [[[:count :*] :total]]
                             :from [:expenses]
                             :where [:and
                                     [:= :supplier_id supplier-id]
                                     [:is :deleted_at nil]]})
                {:builder-fn rs/as-unqualified-lower-maps}))]
        (when (pos? active-expenses)
          (throw (ex-info "Cannot purge supplier: it has active expenses. Archive or delete those expenses first."
                   {:status 409
                    :supplier-id supplier-id
                    :active-expenses active-expenses}))))

      (let [soft-deleted-expense-items
             (:total
               (jdbc/execute-one!
                 tx
                 (sql/format {:select [[[:count :ei.id] :total]]
                              :from [[:expense_items :ei]]
                              :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                              :where [:and
                                      [:= :e.supplier_id supplier-id]
                                      [:is-not :e.deleted_at nil]]})
                 {:builder-fn rs/as-unqualified-lower-maps}))

            deleted-expenses
            (::jdbc/update-count
              (jdbc/execute-one!
                tx
                (sql/format {:delete-from :expenses
                             :where [:and
                                     [:= :supplier_id supplier-id]
                                     [:is-not :deleted_at nil]]})))
            deleted-supplier
            (::jdbc/update-count
              (jdbc/execute-one!
                tx
                (sql/format {:delete-from :suppliers
                             :where [:= :id supplier-id]})))]

        (when-not (pos? deleted-supplier)
          (throw (ex-info "Failed to purge supplier" {:status 500 :supplier-id supplier-id})))

        {:purged true
         :supplier-id supplier-id
         :deleted-expenses (or deleted-expenses 0)
         :deleted-expense-items (or soft-deleted-expense-items 0)}))))

(def ^:private purge-preview-expenses-limit 50)

(defn purge-supplier-preview
  "Return what would be deleted by `purge-supplier!`.

  Intended for admin UI confirmation prompts.

  Notes:
  - `:can-purge?` requires supplier to be archived and have zero *active* expenses.
  - Soft-deleted expenses are safe to purge; their expense_items will be removed via FK cascade.

  Throws ex-info with :status 404 for missing supplier."
  [db supplier-id]
  (let [supplier
        (jdbc/execute-one!
          db
          (sql/format {:select [:id :archived_at]
                       :from [:suppliers]
                       :where [:= :id supplier-id]
                       :limit 1})
          {:builder-fn rs/as-unqualified-lower-maps})]
    (when-not supplier
      (throw (ex-info "Supplier not found" {:status 404 :supplier-id supplier-id})))

    (let [archived? (some? (:archived_at supplier))
          active-expenses
          (:total
            (jdbc/execute-one!
              db
              (sql/format {:select [[[:count :*] :total]]
                           :from [:expenses]
                           :where [:and
                                   [:= :supplier_id supplier-id]
                                   [:is :deleted_at nil]]})
              {:builder-fn rs/as-unqualified-lower-maps}))

          soft-deleted-expenses-total
          (:total
            (jdbc/execute-one!
              db
              (sql/format {:select [[[:count :*] :total]]
                           :from [:expenses]
                           :where [:and
                                   [:= :supplier_id supplier-id]
                                   [:is-not :deleted_at nil]]})
              {:builder-fn rs/as-unqualified-lower-maps}))

          soft-deleted-expense-items-total
          (:total
            (jdbc/execute-one!
              db
              (sql/format {:select [[[:count :ei.id] :total]]
                           :from [[:expense_items :ei]]
                           :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                           :where [:and
                                   [:= :e.supplier_id supplier-id]
                                   [:is-not :e.deleted_at nil]]})
              {:builder-fn rs/as-unqualified-lower-maps}))

          soft-deleted-expenses-rows
          (jdbc/execute!
            db
            (sql/format {:select [[:e.id :id]
                                  [:e.purchased_at :purchased_at]
                                  [:e.total_amount :total_amount]
                                  [:e.currency :currency]
                                  [:e.deleted_at :deleted_at]
                                  [[:count :ei.id] :items_total]]
                         :from [[:expenses :e]]
                         :left-join [[:expense_items :ei] [:= :ei.expense_id :e.id]]
                         :where [:and
                                 [:= :e.supplier_id supplier-id]
                                 [:is-not :e.deleted_at nil]]
                         :group-by [:e.id :e.purchased_at :e.total_amount :e.currency :e.deleted_at]
                         :order-by [[:e.deleted_at :desc]
                                    [:e.purchased_at :desc]]
                         :limit purge-preview-expenses-limit})
            {:builder-fn rs/as-unqualified-lower-maps})

          soft-deleted-expenses
          (mapv
            (fn [{:keys [id purchased_at total_amount currency deleted_at items_total]}]
              {:id id
               :purchased-at (some-> purchased_at str)
               :deleted-at (some-> deleted_at str)
               :total-amount total_amount
               :currency currency
               :expense-items-count (long (or items_total 0))})
            (or soft-deleted-expenses-rows []))]

      {:supplier-id supplier-id
       :archived? archived?
       :can-purge? (and archived? (zero? (long (or active-expenses 0))))
       :active-expenses (long (or active-expenses 0))
       :soft-deleted-expenses-total (long (or soft-deleted-expenses-total 0))
       :soft-deleted-expense-items-total (long (or soft-deleted-expense-items-total 0))
       :soft-deleted-expenses soft-deleted-expenses
       :soft-deleted-expenses-truncated?
       (and (number? soft-deleted-expenses-total)
         (> (long soft-deleted-expenses-total) purge-preview-expenses-limit))})))

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
  [db display-name & [{:keys [address tax_id]}]]
  (let [normalized (normalize-supplier-key display-name)]
    (if-let [existing (find-by-normalized-key db normalized)]
      {:existing? true :supplier existing}
      {:existing? false
       :supplier ((:create! service) db {:display_name display-name
                                         :address address
                                         :tax_id tax_id})})))

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
