# Expenses Data Model Simplification - Implementation Guide

**Generated**: 2026-01-20
**Based on**: EXPENSES-DATA-MODEL-SIMPLIFICATION.md

This document contains concrete code changes for implementing the data model simplification.

---

## Table of Contents

1. [Phase 1: Additive Schema Changes](#phase-1-additive-schema-changes)
2. [Phase 2: Data Backfill Migration](#phase-2-data-backfill-migration)
3. [Phase 3: Finalize Schema](#phase-3-finalize-schema)
4. [Backend Code Changes](#backend-code-changes)
5. [Frontend Code Changes](#frontend-code-changes)
6. [Configuration Updates](#configuration-updates)

---

## Alignment Summary (Decisions)

- Expense item create/update accepts `raw_label` strings; backend resolves/creates `(supplier_id, raw_label_normalized)` aliases and persists `expense_items.alias_id`.
- `article_aliases.article_id` must be nullable (`NULL = unmapped`) before Phase 2, because the backfill creates/updates aliases that may not have a deterministic article mapping.
- `article_aliases.raw_label` stores **latest-seen representative** label text (no per-item auditability). `article_aliases.confidence` is dropped in Phase 3.
- “Unknown Supplier” is identified by `suppliers.normalized_key = 'unknown-supplier'` (migration may insert a fixed UUID, but runtime always looks up by normalized_key).

## Phase 1: Additive Schema Changes

### 1.1 Update `resources/db/domain/models.edn`

Add new columns without removing existing ones:

```clojure
;; ----------------------------------------------------------------------------
;; Article Aliases (mapping raw labels → canonical articles)
;; PHASE 1: Add :raw_label (temporarily nullable) + allow NULL :article_id
;; ----------------------------------------------------------------------------

:article_aliases {:fields [[:id :uuid {:primary-key true}]
                           [:supplier_id :uuid {:foreign-key :suppliers/id :on-delete :cascade}]
                           [:raw_label :text]  ; NEW: temporarily nullable for Phase 1
                           [:raw_label_normalized [:varchar 255] {:null false}]
                           [:article_id :uuid {:foreign-key :articles/id :on-delete :cascade}]  ; CHANGED: drop :null false (NULL = unmapped)
                           [:confidence :integer {:default 100}]  ; Will be removed in Phase 3
                           [:created_at :timestamptz {:default "NOW()"}]]
                  :indexes [[:idx_article_aliases_supplier_label :btree
                             {:fields [:supplier_id :raw_label_normalized] :unique true}]
                            [:idx_article_aliases_article :btree {:fields [:article_id]}]]}

;; ----------------------------------------------------------------------------
;; Expense Items (line items on receipts)
;; PHASE 1: Add :alias_id column (temporarily nullable)
;; ----------------------------------------------------------------------------

:expense_items {:fields [[:id :uuid {:primary-key true}]
                         [:expense_id :uuid {:foreign-key :expenses/id :null false :on-delete :cascade}]
                         [:raw_label_id :uuid {:foreign-key :raw_labels/id :null false}]  ; Will be removed in Phase 3
                         [:alias_id :uuid {:foreign-key :article_aliases/id}]  ; NEW: temporarily nullable for Phase 1
                         [:article_id :uuid {:foreign-key :articles/id :on-delete :set-null}]  ; Will be removed in Phase 3
                         [:qty [:decimal 10 3]]
                         [:unit_price [:decimal 12 2]]
                         [:line_total [:decimal 12 2] {:null false}]
                         [:deleted_at :timestamptz]
                         [:created_at :timestamptz {:default "NOW()"}]]
                :indexes [[:idx_expense_items_expense :btree {:fields [:expense_id]}]
                          [:idx_expense_items_article :btree {:fields [:article_id]}]
                          [:idx_expense_items_raw_label_id :btree {:fields [:raw_label_id]}]
                          [:idx_expense_items_alias :btree {:fields [:alias_id]}]]}  ; NEW index
```

### 1.2 Generate and Apply Phase 1 Migration

```clojure
;; In REPL:
(require '[app.template.backend.migrations.simple-repl :as mig])
(mig/make-all-migrations!)
(mig/migrate!)
```

---

## Phase 2: Data Backfill Migration

### 2.1 Create Empty SQL Migration

```clojure
;; In REPL:
(require '[app.template.backend.migrations.simple-repl :as mig])
(mig/make-migration! :type :empty-sql :name "backfill_raw_labels_data")
```

### 2.2 SQL Migration Content

Edit the generated file (e.g., `resources/db/migrations/NNNN_backfill_raw_labels_data.sql`):

```sql
-- ============================================================================
-- FORWARD MIGRATION: Backfill raw_labels data into article_aliases
-- ============================================================================

-- 0. Ensure Unknown Supplier exists (safe upsert on normalized_key)
INSERT INTO suppliers (id, display_name, normalized_key, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'Unknown Supplier', 'unknown-supplier', NOW(), NOW())
ON CONFLICT (normalized_key) DO UPDATE
  SET display_name = EXCLUDED.display_name,
      updated_at = EXCLUDED.updated_at;

-- 1. Create conflict tracking table for manual review
CREATE TABLE IF NOT EXISTS expense_alias_article_conflicts (
  supplier_id uuid NOT NULL,
  raw_label_normalized text NOT NULL,
  article_ids uuid[] NOT NULL,
  item_count bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT NOW(),
  UNIQUE (supplier_id, raw_label_normalized)
);

-- 2. Upsert article_aliases from expense_items + raw_labels
-- Handles historical article_id mappings and records conflicts
WITH unknown_supplier AS (
  SELECT id
  FROM suppliers
  WHERE normalized_key = 'unknown-supplier'
),
item_rows AS (
  SELECT
    COALESCE(e.supplier_id, (SELECT id FROM unknown_supplier)) AS supplier_id,
    rl.raw_label AS raw_label,
    rl.normalized_key AS raw_label_normalized,
    ei.article_id AS existing_article_id
  FROM expense_items ei
  JOIN raw_labels rl ON ei.raw_label_id = rl.id
  JOIN expenses e ON ei.expense_id = e.id
  -- Include soft-deleted items so alias_id backfill covers all rows
),
grouped AS (
  SELECT
    supplier_id,
    raw_label_normalized,
    MAX(raw_label) AS raw_label_sample,
    -- Deterministic UUID generation without extensions (no pgcrypto/uuid-ossp required)
    md5(supplier_id::text || ':' || raw_label_normalized) AS alias_hash,
    ARRAY_AGG(DISTINCT existing_article_id) FILTER (WHERE existing_article_id IS NOT NULL) AS article_ids,
    COUNT(*) AS item_count
  FROM item_rows
  GROUP BY supplier_id, raw_label_normalized
),
conflicts AS (
  INSERT INTO expense_alias_article_conflicts (supplier_id, raw_label_normalized, article_ids, item_count)
  SELECT supplier_id, raw_label_normalized, article_ids, item_count
  FROM grouped
  WHERE COALESCE(array_length(article_ids, 1), 0) > 1
  ON CONFLICT DO NOTHING
  RETURNING 1
)
INSERT INTO article_aliases (id, supplier_id, raw_label, raw_label_normalized, article_id, created_at)
SELECT
  (substr(alias_hash, 1, 8) || '-' ||
   substr(alias_hash, 9, 4) || '-' ||
   substr(alias_hash, 13, 4) || '-' ||
   substr(alias_hash, 17, 4) || '-' ||
   substr(alias_hash, 21, 12))::uuid AS id,
  supplier_id,
  raw_label_sample,
  raw_label_normalized,
  CASE WHEN array_length(article_ids, 1) = 1 THEN article_ids[1] ELSE NULL END AS article_id,
  NOW()
FROM grouped
ON CONFLICT (supplier_id, raw_label_normalized)
DO UPDATE SET raw_label = EXCLUDED.raw_label;

-- 3. Link expense_items to article_aliases via alias_id
WITH unknown_supplier AS (
  SELECT id
  FROM suppliers
  WHERE normalized_key = 'unknown-supplier'
)
UPDATE expense_items ei
SET alias_id = aa.id
FROM raw_labels rl, expenses e, article_aliases aa, unknown_supplier us
WHERE ei.raw_label_id = rl.id
  AND ei.expense_id = e.id
  AND aa.raw_label_normalized = rl.normalized_key
  AND aa.supplier_id = COALESCE(e.supplier_id, us.id);

-- 4. Verify all items are linked (should return 0)
-- SELECT COUNT(*) FROM expense_items WHERE alias_id IS NULL;

-- 5. Review conflicts if any exist
-- SELECT * FROM expense_alias_article_conflicts ORDER BY item_count DESC;

-- ============================================================================
-- BACKWARD MIGRATION (rollback)
-- ============================================================================
-- Phase 2 is not cleanly reversible (it mutates aliases + links items).
-- Preferred rollback: restore a DB backup.
-- Best-effort rollback:
--   1) UPDATE expense_items SET alias_id = NULL;
--   2) DROP TABLE IF EXISTS expense_alias_article_conflicts;
```

---

## Phase 3: Finalize Schema

### 3.1 Update `resources/db/domain/models.edn` - Final Schema

```clojure
;; REMOVE the :raw_labels table definition entirely

;; ----------------------------------------------------------------------------
;; Article Aliases (unified: stores raw_label + maps to articles)
;; ----------------------------------------------------------------------------

:article_aliases {:fields [[:id :uuid {:primary-key true}]
                           [:supplier_id :uuid {:foreign-key :suppliers/id :null false :on-delete :cascade}]
                           [:raw_label :text {:null false}]
                           [:raw_label_normalized [:varchar 255] {:null false}]
                           [:article_id :uuid {:foreign-key :articles/id :on-delete :cascade}]  ; nullable = unmapped
                           [:created_at :timestamptz {:default "NOW()"}]]
                  :indexes [[:idx_article_aliases_supplier_label :btree
                             {:fields [:supplier_id :raw_label_normalized] :unique true}]
                            [:idx_article_aliases_article :btree {:fields [:article_id]}]
                            [:idx_article_aliases_raw_label_normalized :btree {:fields [:raw_label_normalized]}]]}

;; ----------------------------------------------------------------------------
;; Expense Items (line items on receipts)
;; ----------------------------------------------------------------------------

:expense_items {:fields [[:id :uuid {:primary-key true}]
                         [:expense_id :uuid {:foreign-key :expenses/id :null false :on-delete :cascade}]
                         [:alias_id :uuid {:foreign-key :article_aliases/id :null false}]
                         [:qty [:decimal 10 3]]
                         [:unit_price [:decimal 12 2]]
                         [:line_total [:decimal 12 2] {:null false}]
                         [:deleted_at :timestamptz]
                         [:created_at :timestamptz {:default "NOW()"}]]
                :indexes [[:idx_expense_items_expense :btree {:fields [:expense_id]}]
                          [:idx_expense_items_alias :btree {:fields [:alias_id]}]]}
```

---

## Backend Code Changes

### 4.1 DELETE: `src/app/domain/backend/expenses/services/raw_labels.clj`

Remove the entire file.

### 4.2 DELETE: `test/app/domain/backend/expenses/services/raw_labels_test.clj`

Remove the entire test file (if exists).

### 4.3 DELETE: `src/app/domain/backend/expenses/handlers/user_raw_labels.clj`

Remove the entire file.

### 4.4 DELETE: `src/app/domain/backend/expenses/routes/raw_labels.clj`

Remove the entire file.

### 4.5 UPDATE: `src/app/domain/backend/expenses/routes/core.clj`

```clojure
(ns app.domain.backend.expenses.routes.core
  "Expenses backend route assembly; add new expense endpoints here."
  (:require
    [app.domain.backend.expenses.handlers.receipt-upload :as receipt-upload]
    [app.domain.backend.expenses.routes.articles :as articles]
    [app.domain.backend.expenses.routes.article-aliases :as article-aliases]
    [app.domain.backend.expenses.routes.expense-items :as expense-items]
    [app.domain.backend.expenses.routes.expenses :as expenses]
    [app.domain.backend.expenses.routes.payers :as payers]
    [app.domain.backend.expenses.routes.payer-types :as payer-types]
    [app.domain.backend.expenses.routes.price-observations :as price-observations]
    ;; REMOVED: [app.domain.backend.expenses.routes.raw-labels :as raw-labels]
    [app.domain.backend.expenses.routes.receipts :as receipts]
    [app.domain.backend.expenses.routes.reports :as reports]
    [app.domain.backend.expenses.routes.suppliers :as suppliers]))

(defn routes
  [db & [app-config]]
  ["/expenses"
   ["/upload" {:post {:handler (receipt-upload/admin-upload-handler db)}}]
   (suppliers/routes db)
   (payers/routes db)
   (payer-types/routes db)
   (receipts/routes db app-config)
   (article-aliases/routes db)
   (price-observations/routes db)
   ;; REMOVED: (raw-labels/routes db)
   (expenses/routes db)
   (expense-items/routes db)
   (articles/routes db)
   (reports/routes db)])
```

### 4.6 UPDATE: `src/app/domain/backend/expenses/routes/user_api.clj`

```clojure
(ns app.domain.backend.expenses.routes.user-api
  (:require
    [app.domain.backend.expenses.handlers.receipt-upload :as receipt-upload]
    [app.domain.backend.expenses.handlers.user-expenses.supplier-detail :as supplier-detail]
    [app.domain.backend.expenses.handlers.user-articles :as user-articles]
    ;; REMOVED: [app.domain.backend.expenses.handlers.user-raw-labels :as user-raw-labels]
    [app.domain.backend.expenses.handlers.user-price-observations :as user-price-observations]
    [app.domain.backend.expenses.handlers.user-expenses.batch :as user-expenses-batch]
    [app.domain.backend.expenses.handlers.user-expenses.crud :as user-expenses-crud]
    [app.domain.backend.expenses.handlers.user-expenses.expense-items :as user-expenses-expense-items]
    [app.domain.backend.expenses.handlers.user-expenses.reference-data :as user-expenses-reference-data]
    [app.domain.backend.expenses.handlers.user-expenses.summary :as user-expenses-summary]
    [app.domain.backend.expenses.handlers.user-expenses.settings :as settings]
    [app.domain.backend.expenses.handlers.user-receipts :as user-receipts]))

(defn routes
  [db wrap-user-authentication & [app-config]]
  ["/expenses"
   {:middleware [wrap-user-authentication]}

   ;; ... existing routes ...

   ;; REMOVED: ["/raw-labels" {:get {:handler (user-raw-labels/list-raw-labels-handler db)}}]

   ;; ... rest of routes ...
   ])
```

### 4.7 UPDATE: `src/app/domain/backend/registry.clj`

Remove the `"/raw-labels"` entry from the registry (around line 44).

### 4.8 UPDATE: `src/app/domain/backend/expenses/services/article_aliases.clj`

Replace with new implementation:

```clojure
(ns app.domain.backend.expenses.services.article-aliases
  "Article alias management (unified raw_label + article mapping)."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Constants
;; ============================================================================

(def ^:private unknown-supplier-normalized-key "unknown-supplier")

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :article-alias))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

(def ^:private try-uuid type-conv/try-parse-uuid)

;; ============================================================================
;; Unknown Supplier Helper
;; ============================================================================

(defn get-unknown-supplier-id
  "Returns the ID of the 'Unknown Supplier' record.
   Creates it if it doesn't exist."
  [db]
  (let [existing (jdbc/execute-one!
                   db
                   (sql/format {:select [:id]
                                :from [:suppliers]
                                :where [:= :normalized_key unknown-supplier-normalized-key]
                                :limit 1})
                   {:builder-fn rs/as-unqualified-lower-maps})]
    (if existing
      (:id existing)
      (let [new-id (UUID/randomUUID)]
        (jdbc/execute-one!
          db
          (sql/format {:insert-into :suppliers
                       :values [{:id new-id
                                 :display_name "Unknown Supplier"
                                 :normalized_key unknown-supplier-normalized-key}]
                       :on-conflict [:normalized_key]
                       :do-update-set {:display_name :excluded/display_name
                                       :updated_at [:now]}
                       :returning [:id]})
          {:builder-fn rs/as-unqualified-lower-maps})
        new-id))))

;; ============================================================================
;; Core Operations
;; ============================================================================

(defn find-or-create-alias!
  "Find or create an article_alias by (supplier_id, raw_label).

   Returns the alias row (with :id, :article_id, etc.).

   Parameters:
   - db: database connection
   - supplier-id: UUID or nil (uses Unknown Supplier if nil)
   - raw-label: the raw label text (required)

   The normalized key is computed from raw-label using articles/normalize-alias-label."
  [db supplier-id raw-label]
  (when (str/blank? raw-label)
    (throw (ex-info "raw_label is required" {:status 400 :field :raw_label})))
  (let [raw-label* (str/trim raw-label)
        normalized (articles/normalize-alias-label raw-label*)
        effective-supplier-id (or supplier-id (get-unknown-supplier-id db))
        row {:id (UUID/randomUUID)
             :supplier_id effective-supplier-id
             :raw_label raw-label*
             :raw_label_normalized normalized
             :article_id nil}  ; unmapped by default
        sql-map {:insert-into :article_aliases
                 :values [row]
                 :on-conflict [:supplier_id :raw_label_normalized]
                 :do-update-set {:raw_label :excluded/raw_label}  ; keep latest-seen label
                 :returning [:*]}]
    (jdbc/execute-one!
      db
      (sql/format sql-map)
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-article-aliases
  "List article aliases with optional filters.

   Supports:
   - :supplier-id / :supplier_id
   - :article-id / :article_id
   - :unmapped-only (boolean, filters to article_id IS NULL)"
  [db {:keys [limit offset order-by order-dir search supplier-id supplier_id article-id article_id unmapped-only]
       :or {limit 50 offset 0 order-dir :asc}
       :as opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        article-uuid (try-uuid (or article-id article_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :aa/supplier_id supplier-uuid])
                       article-uuid (conj [:= :aa/article_id article-uuid])
                       unmapped-only (conj [:is :aa/article_id nil]))
        config* (assoc config :base-filters base-filters)
        base-query (factory/build-query-with-filters
                    config*
                    {:limit limit
                     :offset offset
                     :order-by order-by
                     :order-dir order-dir})
        final-query (factory/apply-search-filter base-query (:search-fields config*) search)]
    (if (or supplier-uuid article-uuid unmapped-only)
      (jdbc/execute! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})
      ((:list service) db opts))))

(defn list-unmapped-aliases
  "List unmapped article aliases (article_id IS NULL) with occurrence counts."
  [db {:keys [limit offset supplier-id]
       :or {limit 100 offset 0}}]
  (let [supplier-uuid (try-uuid supplier-id)
        query (cond-> {:select [[:aa.id]
                                [:aa.raw_label]
                                [:aa.raw_label_normalized]
                                [:aa.supplier_id]
                                [:s.display_name :supplier_display_name]
                                [[:count :ei.id] :occurrence_count]]
                       :from [[:article_aliases :aa]]
                       :left-join [[:suppliers :s] [:= :aa.supplier_id :s.id]
                                   [:expense_items :ei] [:= :ei.alias_id :aa.id]]
                       :where [:is :aa.article_id nil]
                       :group-by [:aa.id :s.display_name]
                       :order-by [[[:count :ei.id] :desc]]
                       :limit limit
                       :offset offset}
                supplier-uuid
                (update :where conj [:= :aa.supplier_id supplier-uuid]))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-article!
  "Map an alias to an article.

   Updates article_aliases.article_id for the given alias."
  [db alias-id article-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :article_aliases
                 :set {:article_id article-id}
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn unmap-alias!
  "Remove article mapping from an alias (set article_id to NULL)."
  [db alias-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :article_aliases
                 :set {:article_id nil}
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

;; ============================================================================
;; Batch Operations
;; ============================================================================

(defn batch-map-aliases!
  "Map multiple aliases to a single article.

   Parameters:
   - alias-ids: seq of alias UUIDs
   - article-id: the target article UUID"
  [db alias-ids article-id]
  (when (seq alias-ids)
    (jdbc/execute!
      db
      (sql/format {:update :article_aliases
                   :set {:article_id article-id}
                   :where [:in :id alias-ids]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))
```

### 4.9 UPDATE: `src/app/domain/backend/expenses/services/service_configs.clj`

Update `expense-item-config` to use `alias_id` instead of `raw_label_id`:

```clojure
(ns app.domain.backend.expenses.services.service-configs
  "Service configuration maps for expenses domain entities."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.price-history :as price-history]
    ;; REMOVED: [app.domain.backend.expenses.services.raw-labels :as raw-labels]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [clojure.string :as str])
  (:import
    [java.util UUID]))

;; ... keep existing configs unchanged ...

;; UPDATE article-alias-config to include raw_label in required-fields
(def article-alias-config
  {:table-name "article_aliases"
   :table-alias :aa
   :primary-key :aa/id
   :required-fields [:supplier_id :raw_label :raw_label_normalized]
   :allowed-order-by {:created-at :aa/created_at
                      :raw-label :aa/raw_label
                      :raw-label-normalized :aa/raw_label_normalized
                      :supplier-display-name :s/display_name
                      :article-canonical-name :a/canonical_name}
   :default-order-by :aa/created_at
   :search-fields [:aa/raw_label :aa/raw_label_normalized :s/display_name :a/canonical_name]
   :joins [[:suppliers :s] [:= :s/id :aa/supplier_id]
           [:articles :a] [:= :a/id :aa/article_id]]
   :select-fields [[:aa.*]
                   [:s/display_name :supplier_display_name]
                   [:a/canonical_name :article_canonical_name]]
   :field-transformers {:raw_label_normalized articles/normalize-alias-label}
   :has-search? true
   :has-count? true})

;; UPDATE expense-item-config to use alias_id
(def expense-item-config
  {:table-name "expense_items"
   :table-alias :ei
   :primary-key :ei/id
   :base-filters [[:is :ei/deleted_at nil]
                  [:is :e/deleted_at nil]]
   ;; We accept :raw_label (string) and resolve to :alias_id server-side
   :required-fields [:expense_id :line_total]
   :allowed-order-by {:expense-id :ei/expense_id
                      :raw-label :aa/raw_label
                      :alias-id :ei/alias_id
                      :created-at :ei/created_at
                      :qty :ei/qty
                      :unit-price :ei/unit_price
                      :line-total :ei/line_total
                      :expense-purchased-at :e/purchased_at
                      :article-canonical-name :a/canonical_name}
   :default-order-by :ei/created_at
   :search-fields [:aa/raw_label :a/canonical_name]
   :joins [[:expenses :e] [:= :e/id :ei/expense_id]
           [:article_aliases :aa] [:= :aa/id :ei/alias_id]
           [:articles :a] [:= :a/id :aa/article_id]]
   :select-fields [[:ei.*]
                   [:aa/raw_label :raw_label]
                   [:aa/raw_label_normalized :raw_label_normalized]
                   [:e/purchased_at :expense_purchased_at]
                   [:a/canonical_name :article_canonical_name]]
   :before-insert (fn [db data]
                    (let [alias-svc (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/find-or-create-alias!)
                          raw-label (some-> (:raw_label data) str str/trim)
                          supplier-id (:supplier_id data)]  ; passed from expense context
                      (cond
                        (some? (:alias_id data))
                        (dissoc data :raw_label :supplier_id)

                        (some? raw-label)
                        (let [alias (alias-svc db supplier-id raw-label)]
                          (-> data
                            (dissoc :raw_label :supplier_id)
                            (assoc :alias_id (:id alias))))

                        :else
                        (throw (ex-info "raw_label or alias_id is required"
                                 {:entity "expense_items"
                                  :missing-field :raw_label})))))
   :before-update (fn [db _id updates]
                    (let [alias-svc (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/find-or-create-alias!)
                          raw-label (some-> (:raw_label updates) str str/trim)
                          supplier-id (:supplier_id updates)]
                      (cond
                        (some? raw-label)
                        (let [alias (alias-svc db supplier-id raw-label)]
                          (-> updates
                            (dissoc :raw_label :supplier_id)
                            (assoc :alias_id (:id alias))))

                        :else
                        (dissoc updates :raw_label :supplier_id))))
   :has-search? true
   :has-count? true})
```

### 4.10 UPDATE: `src/app/domain/backend/expenses/handlers/user_expenses/expense_items.clj`

```clojure
(ns app.domain.backend.expenses.handlers.user-expenses.expense-items
  "User-facing expense items list (power-user tool)."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.article-aliases :as aliases]  ; NEW
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ... keep helper functions unchanged ...

(defn list-expense-items-handler
  "GET /api/v1/expenses/expense-items"
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can access expense items")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (clamp-limit (some-> (h/get-param params :limit) parse-long))
                offset (max 0 (long (or (some-> (h/get-param params :offset) parse-long) 0)))
                search (some-> (h/get-param params :search) str)
                search* (when (and (string? search) (not (str/blank? search)))
                          (str "%" search "%"))
                where (cond-> [:and
                               [:= :e.user_id user-id]
                               [:is :ei.deleted_at nil]
                               [:is :e.deleted_at nil]]
                        search*
                        (conj [:or
                               [:ilike :aa.raw_label search*]
                               [:ilike :a.canonical_name search*]]))
                ;; UPDATED: join article_aliases instead of raw_labels
                query {:select [[:ei.*]
                                [:aa.raw_label :raw_label]
                                [:aa.raw_label_normalized :raw_label_normalized]
                                [:e.purchased_at :expense_purchased_at]
                                [:a.canonical_name :article_canonical_name]]
                       :from [[:expense_items :ei]]
                       :left-join [[:expenses :e] [:= :e.id :ei.expense_id]
                                   [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                   [:articles :a] [:= :a.id :aa.article_id]]
                       :where where
                       :order-by [[:ei.created_at :desc]]
                       :limit limit
                       :offset offset}
                items (jdbc/execute! db (sql/format query)
                        {:builder-fn rs/as-unqualified-lower-maps})]
            (h/json-response {:data (vec items)}))
          (catch Exception e
            (log/error e "Error listing expense items" {:user-id user-id})
            (h/json-response {:error "Failed to list expense items"} 500))))
      (h/unauthorized-response))))

(defn- fetch-expense-item
  "Fetch a single expense item with alias + article joins."
  [db user-id item-id]
  (jdbc/execute-one!
    db
    (sql/format
      {:select [[:ei.*]
                [:aa.raw_label :raw_label]
                [:aa.raw_label_normalized :raw_label_normalized]
                [:e.purchased_at :expense_purchased_at]
                [:a.canonical_name :article_canonical_name]]
       :from [[:expense_items :ei]]
       :left-join [[:expenses :e] [:= :e.id :ei.expense_id]
                   [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                   [:articles :a] [:= :a.id :aa.article_id]]
       :where [:and
               [:= :ei.id item-id]
               [:= :e.user_id user-id]
               [:is :ei.deleted_at nil]
               [:is :e.deleted_at nil]]
       :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn update-expense-item-handler
  "PUT /api/v1/expenses/expense-items/:id"
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can modify expense items")]
        forbidden
        (if-let [item-id (expense-item-id request)]
          (try
            (let [body (h/read-body-params request)
                  raw-label (some-> (h/get-param body :raw_label) str str/trim)
                  raw-label* (when-not (str/blank? raw-label) raw-label)
                  ;; Get supplier_id from the expense for alias resolution
                  expense-row (jdbc/execute-one! db
                                (sql/format {:select [:e.supplier_id]
                                             :from [[:expense_items :ei]]
                                             :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                             :where [:= :ei.id item-id]})
                                {:builder-fn rs/as-unqualified-lower-maps})
                  supplier-id (:supplier_id expense-row)
                  ;; UPDATED: use aliases/find-or-create-alias! instead of raw-labels
                  alias-id (when raw-label*
                             (:id (aliases/find-or-create-alias! db supplier-id raw-label*)))
                  updates {:alias_id (or alias-id
                                      (throw (ex-info "raw_label is required"
                                               {:status 400
                                                :field :raw_label})))
                           :qty (parse-decimal! :qty (h/get-param body :qty))
                           :unit_price (parse-decimal! :unit_price (h/get-param body :unit_price))
                           :line_total (or (parse-decimal! :line_total (h/get-param body :line_total))
                                         (throw (ex-info "line_total is required"
                                                  {:status 400
                                                   :field :line_total})))}]
              (if-let [updated (update-expense-item! db user-id item-id updates)]
                (h/json-response {:data (or (fetch-expense-item db user-id item-id) updated)})
                (h/not-found-response "Expense item not found or access denied")))
            (catch clojure.lang.ExceptionInfo e
              (let [{:keys [status]} (ex-data e)]
                (if (number? status)
                  (h/json-response {:error (.getMessage e)} status)
                  (do
                    (log/error e "Error updating expense item" {:user-id user-id :item-id item-id})
                    (h/json-response {:error "Failed to update expense item"} 500)))))
            (catch Exception e
              (log/error e "Error updating expense item" {:user-id user-id :item-id item-id})
              (h/json-response {:error "Failed to update expense item"} 500)))
          (h/json-response {:error "Invalid expense item ID"} 400)))
      (h/unauthorized-response))))

;; ... keep delete handler unchanged (it doesn't touch raw_label) ...
```

### 4.11 UPDATE: `src/app/domain/backend/expenses/services/expenses.clj`

This is the core ingestion path that currently persists `raw_label_id` + optional `article_id`.
Update it to persist `alias_id` only, and to read label text via joined alias rows.

Key changes:

1) Replace `resolve-raw-label-id!` with alias resolution:

```clojure
;; NEW helper (replaces resolve-raw-label-id!)
(defn- resolve-alias!
  [tx supplier-id {:keys [raw_label alias_id]}]
  (or alias_id
    (when-let [label (some-> raw_label str str/trim not-empty)]
      (:id (aliases/find-or-create-alias! tx supplier-id label)))))
```

2) In `create-expense!` / `update-expense!`, stop writing `:raw_label_id` and `:article_id`:

```clojure
(let [alias (aliases/find-or-create-alias! tx supplier-id raw-label)
      alias-id (:id alias)]
  {:id (UUID/randomUUID)
   :expense_id expense-id
   :alias_id alias-id
   :qty qty
   :unit_price unit_price
   :line_total line_total})
```

3) Update `get-expense-with-items` to join aliases (and optionally articles):

```clojure
{:select [[:ei.*]
          [:aa.raw_label :raw_label]
          [:aa.raw_label_normalized :raw_label_normalized]
          [:a.canonical_name :article_canonical_name]]
 :from [[:expense_items :ei]]
 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
             [:articles :a] [:= :a.id :aa.article_id]]
 ;; ... where/order ...
 }
```

> Note: price observation recording should use the alias’ `article_id` (when present), since `expense_items.article_id` is removed.

### 4.12 UPDATE: `src/app/domain/backend/expenses/routes/route_configs.clj`

Align route-required fields with the new semantics:

- `expense-items`: keep `:required-fields` minimal and treat `raw_label` as input-only (alias resolved server-side).
- `article-aliases`: remove `:article_id` from required fields (unmapped aliases are allowed).

### 4.13 UPDATE: `src/app/domain/backend/expenses/routes/articles.clj` and `src/app/domain/backend/expenses/handlers/user_articles.clj`

The legacy “unmapped items” + “map item to article” endpoints implement per-item overrides. They must be removed/refactored to operate on aliases:

- Replace “unmapped items” listing with “unmapped aliases” listing (`article_aliases.article_id IS NULL`).
- Replace “map item” endpoint with “map alias” (update `article_aliases.article_id`).
- Remove `confidence` from any request/response path.

---

## 

### 5.1 DELETE: `src/app/domain/frontend/expenses/pages/user/raw_labels.cljs`

Remove the entire file.

### 5.2 UPDATE: `src/app/domain/frontend/expenses/pages.cljs`

```clojure
(ns app.domain.frontend.expenses.pages
  (:require
    ;; ... other requires ...
    ;; REMOVED: [app.domain.frontend.expenses.pages.user.raw-labels :refer [raw-labels-page]]
    ;; ... other requires ...
    ))

(def pages
  {;; ... other pages ...
   ;; REMOVED: :expense-raw-labels raw-labels-page
   ;; ... other pages ...
   })
```

### 5.3 UPDATE: `src/app/domain/frontend/expenses/routes/user.cljs`

Remove the `/raw-labels` route definition (around lines 99-102).

### 5.4 UPDATE: `src/app/template/frontend/events/routing.cljs`

Remove `:page/init-expense-raw-labels` event handler (around lines 238-243).

### 5.5 UPDATE: `src/app/template/frontend/components/layout.cljs`

Remove the raw-labels sidebar navigation item (around lines 145-150).

### 5.6 UPDATE: `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs`

Remove `:user-expenses/fetch-raw-labels` and related events (around lines 202-224).

### 5.7 UPDATE: `src/app/domain/frontend/expenses/admin/adapters/sync.cljs`

Remove raw-labels sync handler registration (around lines 71-72 and 103-104).

### 5.8 UPDATE: `src/app/domain/frontend/expenses/admin/adapters/normalize.cljs`

Remove raw-labels entity normalization (around line 189).

### 5.9 UPDATE: `src/app/domain/frontend/expenses/admin/adapters/specs.cljs`

Remove `raw-labels-entity-spec` definition and registration (around lines 67-68 and 114-115).

### 5.10 UPDATE: `src/app/domain/frontend/expenses/events/user_expenses/endpoints.cljs`

Remove raw-labels and legacy unmapped-items endpoints, and prefer alias-based endpoints for the unmapped queue:

```clojure
;; REMOVE
;; (def raw-labels-endpoint ...)
;; (def admin-raw-labels-endpoint ...)
;; (def articles-unmapped-items-endpoint ...)
;; (def admin-articles-unmapped-items-endpoint ...)

;; KEEP
(def article-aliases-endpoint (api/versioned-endpoint "/expenses/article-aliases"))
(def admin-article-aliases-endpoint "/admin/api/expenses/article-aliases")
```

### 5.11 UPDATE: `src/app/domain/frontend/expenses/events/unmapped_items.cljs` (and related `subs/*` + `components/*`)

Refactor the “unmapped items” UX to operate on **aliases**:

- Load data from `article-aliases` filtered to `article_id IS NULL` (or a dedicated `/article-aliases/unmapped` endpoint if you keep occurrence counts server-side).
- Selection + mapping should update `article_aliases.article_id` for selected alias IDs (no per-item mapping calls).
- Remove the “create aliases?” flow entirely (aliases are auto-created during ingestion).

### 5.12 VERIFY: `src/app/domain/frontend/expenses/pages/user/expense_detail.cljs`

Ensure the expense detail view renders item labels from `:raw_label` (now coming from joined alias data) and treats `:article_canonical_name` as optional (unmapped aliases show no article name).

---

## Configuration Updates

### 6.1 UPDATE: `src/app/domain/frontend/expenses/config/entities.edn`

```clojure
{:expenses {:title "My Expenses"},
 :receipts {:title "Receipts"},
 :suppliers {:title "Suppliers"},
 :payers {:title "Payers"},
 :payer-types {:title "Payer Types"},
 :expense-items {:title "Expense Items"},
 :articles {:title "Articles"},
 :article-aliases {:title "Article Aliases"},
 ;; REMOVED: :raw-labels {:title "Raw Labels"},
 :price-observations {:title "Price Observations"}}
```

### 6.2 UPDATE: `src/app/domain/frontend/expenses/config/table-columns.edn`

Remove the `:raw-labels` entry (around lines 89-98).

Update `:expense-items` to use `alias_id` instead of `raw_label_id`:

```clojure
:expense-items
 {:available-columns
  ["expense_purchased_at"
   "article_canonical_name"
   "raw_label"
   "raw_label_normalized"
   "alias_id"  ; CHANGED from raw_label_id
   "qty"
   "unit_price"
   "line_total"
   "created_at"
   "deleted_at"
   "expense_id"
   "id"],
  :default-visible-columns
  ["expense_purchased_at"
   "article_canonical_name"
   "raw_label"
   "qty"
   "unit_price"
   "line_total"
   "created_at"],
  :filterable-columns
  ["expense_purchased_at"
   "article_canonical_name"
   "raw_label"
   "alias_id"
   "qty"
   "unit_price"
   "line_total"
   "created_at"
   "expense_id"],
  :sortable-columns
  ["expense_purchased_at"
   "article_canonical_name"
   "raw_label"
   "qty"
   "unit_price"
   "line_total"
   "created_at"],
  :always-visible ["raw_label"],
  :computed-fields
  {:raw_label {}, :expense_purchased_at {}, :article_canonical_name {}}}
```

Update `:article-aliases` to include `raw_label`:

```clojure
:article-aliases
 {:available-columns
  ["supplier_display_name" "article_canonical_name" "raw_label" "raw_label_normalized" "created_at" "id" "supplier_id" "article_id"],
  :default-visible-columns ["supplier_display_name" "raw_label" "article_canonical_name"],
  :filterable-columns
  ["supplier_display_name"
   "article_canonical_name"
   "raw_label"
   "raw_label_normalized"
   "created_at"],
  :sortable-columns
  ["supplier_display_name"
   "article_canonical_name"
   "raw_label"
   "raw_label_normalized"
   "created_at"],
  :always-visible ["raw_label"],
  :computed-fields {:supplier_display_name {}, :article_canonical_name {}},
  :column-config {}}
```

### 6.3 UPDATE: `src/app/domain/frontend/expenses/config/form-fields.edn`

Update `:article-aliases` edit-fields (remove `confidence`):

```clojure
:article-aliases {:edit-fields ["raw_label" "raw_label_normalized" "article_id"]}
```

### 6.4 UPDATE: `src/app/domain/frontend/expenses/authz.cljs`

Remove `:expenses/raw-labels.manage` capability (around line 29).

---

## Verification Checklist

After all changes are applied:

- [ ] Run Phase 1 migration: `(mig/make-all-migrations!)` then `(mig/migrate!)`
- [ ] Run Phase 2 migration: Create SQL file, then `(mig/migrate!)`
- [ ] Verify data: `SELECT COUNT(*) FROM expense_items WHERE alias_id IS NULL;` (should be 0)
- [ ] Review conflicts: `SELECT * FROM expense_alias_article_conflicts;`
- [ ] Run Phase 3 migration after confirming data integrity
- [ ] Run backend tests: `bb be-test 2>&1 | tee /tmp/be-test-migration.txt`
- [ ] Run frontend tests: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-migration.txt`
- [ ] Verify admin UI: `/admin/article-aliases` displays with `raw_label` column
- [ ] Verify unmapped aliases queue works correctly (lists aliases; mapping updates `article_aliases.article_id`)
- [ ] Verify expense item creation/update works with new `alias_id` flow

---

## Rollback Procedure

If issues arise during migration:

1. **Phase 3 rollback**: Restore models.edn to Phase 1 state, generate reverse migration
2. **Phase 2 rollback**: Restore a DB backup (preferred). Best-effort rollback: `UPDATE expense_items SET alias_id = NULL;` and drop `expense_alias_article_conflicts`
3. **Phase 1 rollback**: Restore models.edn to original state, generate reverse migration

Always back up the database before running migrations.
