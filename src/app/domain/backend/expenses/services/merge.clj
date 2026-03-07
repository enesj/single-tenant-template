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

  :child-fks — tables that reference this child table via FK. Their references must be
  reassigned before we can delete conflicting child rows. Each entry maps a child
  table's FK column to its matching column in the deduped table (always :id)."
  {:suppliers
   [{:table :expenses            :col :supplier_id}
    {:table :stores              :col :supplier_id  :conflict-strategy :exclude-then-delete
     :unique-col :normalized_key
     :child-fks [{:table :expenses :col :store_id}
                 {:table :store_aliases :col :store_id}]}
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

(defn- conflict-resolution-order
  [col primary-id row]
  [(if (= primary-id (get row col)) 0 1)
   (or (:created_at row) (java.util.Date. Long/MAX_VALUE))
   (str (:id row))])

(defn- build-conflict-merge-map
  "Build loser-id -> keeper-id mappings for rows that would collide on unique-col
  after reassignment to the primary. Prefer an already-primary row when present;
  otherwise keep the earliest-created row."
  [rows col unique-col primary-id]
  (->> rows
    (group-by unique-col)
    vals
    (filter #(> (count %) 1))
    (mapcat (fn [matches]
              (let [ordered (sort-by #(conflict-resolution-order col primary-id %) matches)
                    keeper-id (:id (first ordered))]
                (map (fn [row] [(:id row) keeper-id]) (rest ordered)))))
    (into {})))

(defn- find-conflicting-rows
  "Find rows that would conflict on unique-col after reassigning all secondary
  rows to the primary."
  [tx table col unique-col primary-id secondary-ids]
  (jdbc/execute!
    tx
    (sql/format {:select [:id col unique-col :created_at]
                 :from [table]
                 :where [:and
                         [:in col (vec (cons primary-id secondary-ids))]
                         [:is-not unique-col nil]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- reassign-child-fks-for-conflicting-rows!
  "Reassign dependent FK references away from rows that will be deleted."
  [tx child-fks conflict-map]
  (when (and (seq child-fks) (seq conflict-map))
    (doseq [[from-id to-id] conflict-map
            {child-table :table child-col :col} child-fks]
      (let [result (jdbc/execute-one!
                     tx
                     (sql/format {:update child-table
                                  :set {child-col to-id}
                                  :where [:= child-col from-id]}))]
        (log/info "Reassigned child FK for conflicting row"
          {:child-table child-table
           :child-col child-col
           :from-id from-id
           :to-id to-id
           :updated (:next.jdbc/update-count result 0)})))))

(defn- delete-conflicting-rows!
  "Delete rows that would otherwise collide after merge."
  [tx table conflict-map]
  (when (seq conflict-map)
    (let [loser-ids (vec (keys conflict-map))
          result (jdbc/execute-one!
                   tx
                   (sql/format {:delete-from table
                                :where [:in :id loser-ids]}))]
      (log/info "Deleted conflicting rows"
        {:table table
         :deleted (:next.jdbc/update-count result 0)})
      result)))

(defn- dedupe-conflicting-rows!
  "Resolve rows that would collide on unique-col after reassignment to the primary."
  [tx table col unique-col primary-id secondary-ids child-fks]
  (let [rows (find-conflicting-rows tx table col unique-col primary-id secondary-ids)
        conflict-map (build-conflict-merge-map rows col unique-col primary-id)]
    (when (seq conflict-map)
      (reassign-child-fks-for-conflicting-rows! tx child-fks conflict-map)
      (delete-conflicting-rows! tx table conflict-map)
      conflict-map)))

;; ============================================================================
;; FK Reassignment
;; ============================================================================

(defn- reassign-fk!
  "Reassign FK references from secondary IDs to primary ID for one FK table."
  [tx {:keys [table col conflict-strategy unique-col child-fks]} primary-id secondary-ids]
  ;; Handle unique constraint conflicts that would arise after reassignment.
  (when (= conflict-strategy :exclude-then-delete)
    (dedupe-conflicting-rows! tx table col unique-col primary-id secondary-ids child-fks))
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
