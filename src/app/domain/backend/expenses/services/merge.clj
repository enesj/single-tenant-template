(ns app.domain.backend.expenses.services.merge
  "Merge duplicate canonical entities by reassigning FK references to a primary
  record and deleting secondaries.

  Supports: Suppliers, Articles, Stores, Manufacturers.

  Critical constraints:
  - expenses.supplier_id has ON DELETE RESTRICT — must reassign before delete.
  - Alias tables have unique constraints — handle conflicts via exclude-then-delete."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ============================================================================
;; FK Reassignment Configuration
;; ============================================================================

(def ^:private fk-configs
  "Per-entity map of FK tables to reassign during merge.

  :conflict-strategy
  - nil (default)          — simple UPDATE SET col = primary-id WHERE col IN secondary-ids
  - :exclude-then-delete   — delete conflicting rows first, then update remaining

  :child-fks — tables that reference this alias table via FK. Their references must be
  reassigned before we can delete conflicting alias rows. Each entry maps a child
  table's FK column to its matching column in the alias table (always :id)."
  {:suppliers
   [{:table :expenses            :col :supplier_id}
    {:table :stores              :col :supplier_id}
    {:table :supplier_aliases    :col :supplier_id  :conflict-strategy :exclude-then-delete
     :unique-col :raw_label_normalized}
    {:table :article_aliases     :col :supplier_id  :conflict-strategy :exclude-then-delete
     :unique-col :raw_label_normalized
     :child-fks [{:table :expense_items :col :alias_id}]}]

   :articles
   [{:table :expense_items       :col :article_id}
    {:table :article_aliases     :col :article_id   :conflict-strategy :exclude-then-delete
     :unique-col :raw_label_normalized
     :child-fks [{:table :expense_items :col :alias_id}]}]

   :stores
   [{:table :expenses            :col :store_id}
    {:table :store_aliases       :col :store_id     :conflict-strategy :exclude-then-delete
     :unique-col :raw_label_normalized}]

   :manufacturers
   [{:table :articles            :col :manufacturer_id}]})

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- validate-merge-args!
  [entity-type primary-id secondary-ids]
  (when-not (get fk-configs entity-type)
    (throw (ex-info (str "Unknown entity type: " entity-type)
             {:entity-type entity-type
              :valid-types (keys fk-configs)})))
  (when (empty? secondary-ids)
    (throw (ex-info "secondary-ids must not be empty"
             {:entity-type entity-type
              :primary-id primary-id})))
  (when (contains? (set secondary-ids) primary-id)
    (throw (ex-info "primary-id must not appear in secondary-ids"
             {:entity-type entity-type
              :primary-id primary-id
              :secondary-ids secondary-ids}))))

(defn- entity-table
  [entity-type]
  (keyword (name entity-type)))

(defn- count-fk-refs
  "Count rows in a FK table referencing any of the given IDs."
  [db table col ids]
  (let [result (jdbc/execute-one!
                 db
                 (sql/format {:select [[[:count :*] :cnt]]
                              :from [table]
                              :where [:in col ids]})
                 {:builder-fn rs/as-unqualified-lower-maps})]
    (:cnt result 0)))

;; ============================================================================
;; Alias Conflict Handling
;; ============================================================================

(defn- get-primary-unique-vals
  "Get the set of unique-col values that already exist for the primary."
  [tx table col unique-col primary-id]
  (->> (jdbc/execute!
         tx
         (sql/format {:select [unique-col]
                      :from [table]
                      :where [:= col primary-id]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (map unique-col)
    set))

(defn- reassign-child-fks-for-conflicting-aliases!
  "Before deleting conflicting alias rows, reassign child FK references
  (e.g. expense_items.alias_id) from secondary aliases to the corresponding
  primary aliases (matched by unique-col).

  Without this step, deleting a secondary alias that is still referenced by
  expense_items would violate the FK constraint."
  [tx alias-table alias-col unique-col primary-id secondary-ids child-fks]
  (when (seq child-fks)
    ;; Build a mapping: secondary-alias-id -> primary-alias-id
    ;; for conflicting aliases (same unique-col value)
    (let [primary-aliases (jdbc/execute!
                            tx
                            (sql/format {:select [:id unique-col]
                                         :from [alias-table]
                                         :where [:= alias-col primary-id]})
                            {:builder-fn rs/as-unqualified-lower-maps})
          primary-by-key (zipmap (map unique-col primary-aliases)
                           (map :id primary-aliases))
          secondary-aliases (jdbc/execute!
                              tx
                              (sql/format {:select [:id unique-col]
                                           :from [alias-table]
                                           :where [:and
                                                   [:in alias-col secondary-ids]
                                                   [:in unique-col (vec (keys primary-by-key))]]})
                              {:builder-fn rs/as-unqualified-lower-maps})]
      (doseq [sec-alias secondary-aliases]
        (let [sec-id (:id sec-alias)
              pri-id (get primary-by-key (get sec-alias unique-col))]
          (when pri-id
            (doseq [{child-table :table child-col :col} child-fks]
              (let [result (jdbc/execute-one!
                             tx
                             (sql/format {:update child-table
                                          :set {child-col pri-id}
                                          :where [:= child-col sec-id]}))]
                (log/info "Reassigned child FK for conflicting alias"
                  {:child-table child-table
                   :child-col child-col
                   :from-alias sec-id
                   :to-alias pri-id
                   :updated (:next.jdbc/update-count result 0)})))))))))

(defn- delete-conflicting-aliases!
  "Delete secondary alias rows whose unique-col value already exists for primary.

  First reassigns any child FK references (e.g. expense_items.alias_id) to the
  primary's equivalent alias so the delete doesn't violate FK constraints."
  [tx table col unique-col primary-id secondary-ids child-fks]
  (let [primary-vals (get-primary-unique-vals tx table col unique-col primary-id)]
    (when (seq primary-vals)
      ;; Step 1: reassign child FKs before deleting
      (reassign-child-fks-for-conflicting-aliases!
        tx table col unique-col primary-id secondary-ids child-fks)
      ;; Step 2: now safe to delete conflicting aliases
      (let [result (jdbc/execute-one!
                     tx
                     (sql/format {:delete-from table
                                  :where [:and
                                          [:in col secondary-ids]
                                          [:in unique-col primary-vals]]}))]
        (log/info "Deleted conflicting aliases"
          {:table table :deleted (:next.jdbc/update-count result 0)})
        result))))

;; ============================================================================
;; FK Reassignment
;; ============================================================================

(defn- reassign-fk!
  "Reassign FK references from secondary IDs to primary ID for one FK table."
  [tx {:keys [table col conflict-strategy unique-col child-fks]} primary-id secondary-ids]
  ;; Handle alias unique constraint conflicts
  (when (= conflict-strategy :exclude-then-delete)
    (delete-conflicting-aliases! tx table col unique-col primary-id secondary-ids child-fks))
  ;; Reassign remaining rows
  (let [result (jdbc/execute-one!
                 tx
                 (sql/format {:update table
                              :set {col primary-id}
                              :where [:in col secondary-ids]}))]
    (log/info "Reassigned FK references"
      {:table table :col col :updated (:next.jdbc/update-count result 0)})
    (:next.jdbc/update-count result 0)))

(defn- delete-secondary-entities!
  "Delete the secondary entities after all FKs have been reassigned."
  [tx entity-type secondary-ids]
  (let [table (entity-table entity-type)
        result (jdbc/execute-one!
                 tx
                 (sql/format {:delete-from table
                              :where [:in :id secondary-ids]}))]
    (log/info "Deleted secondary entities"
      {:entity-type entity-type :deleted (:next.jdbc/update-count result 0)})
    (:next.jdbc/update-count result 0)))

;; ============================================================================
;; Public API
;; ============================================================================

(defn merge-preview
  "Read-only preview of what a merge would affect.

  Returns a map of {:table-name affected-row-count} for each FK table."
  [db entity-type primary-id secondary-ids]
  (validate-merge-args! entity-type primary-id secondary-ids)
  (let [fk-specs (get fk-configs entity-type)]
    (reduce
      (fn [acc {:keys [table col]}]
        (assoc acc (keyword (name table))
          (count-fk-refs db table col secondary-ids)))
      {}
      fk-specs)))

(defn merge-entities!
  "Merge secondary entities into a primary by reassigning all FK references
  and deleting the secondaries.

  Must be called within a transaction for safety. Wraps in its own transaction
  if not already in one."
  [db entity-type primary-id secondary-ids]
  (validate-merge-args! entity-type primary-id secondary-ids)
  (log/info "Starting entity merge"
    {:entity-type entity-type
     :primary-id primary-id
     :secondary-ids secondary-ids})
  (jdbc/with-transaction [tx db]
    (let [fk-specs (get fk-configs entity-type)
          reassigned (reduce
                       (fn [acc fk-spec]
                         (let [updated (reassign-fk! tx fk-spec primary-id secondary-ids)]
                           (assoc acc (keyword (name (:table fk-spec))) updated)))
                       {}
                       fk-specs)
          deleted (delete-secondary-entities! tx entity-type secondary-ids)]
      (log/info "Entity merge complete"
        {:entity-type entity-type
         :primary-id primary-id
         :reassigned reassigned
         :deleted deleted})
      {:primary-id primary-id
       :reassigned reassigned
       :deleted-count deleted})))

(comment
  ;; REPL usage — use rollback-only for safe testing:
  ;; (jdbc/with-transaction [tx db {:rollback-only true}]
  ;;   (merge-entities! tx :suppliers primary-id [secondary-id]))
  :rcf)
