(ns app.domain.backend.expenses.handlers.search.expense-combinations
  "Top expense combinations from manual purchase history, grouped by
   (store + exact article set) and ranked by repetition frequency.

   Each chip represents a unique shopping pattern: the same articles
   bought at the same store. Qty/price differences are ignored for
   grouping. Supplier and category come from the most recent
   representative expense in each group."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ── helpers ─────────────────────────────────────────────────────────

(defn- fetch-combo-groups
  "Find top manual expense combinations grouped by (store + article set).

   Uses STRING_AGG of sorted alias_ids as a fingerprint for the article
   set so that expenses with identical items (regardless of qty/price)
   are grouped together. Returns up to `limit` groups, each with the
   most-recent representative expense_id."
  [db tenant-id limit]
  (jdbc/execute! db
    ["WITH expense_fingerprints AS (
        SELECT
          e.id AS expense_id,
          e.store_id,
          e.purchased_at,
          STRING_AGG(ei.alias_id::text, ',' ORDER BY ei.alias_id) AS article_fingerprint
        FROM expenses e
        JOIN expense_items ei ON ei.expense_id = e.id
        WHERE e.tenant_id = ?
          AND e.receipt_id IS NULL
          AND e.store_id IS NOT NULL
          AND e.purchased_at >= NOW() - INTERVAL '6 months'
        GROUP BY e.id, e.store_id, e.purchased_at
      ),
      combo_groups AS (
        SELECT
          store_id,
          article_fingerprint,
          COUNT(*) AS frequency,
          MAX(purchased_at) AS last_purchased_at,
          (ARRAY_AGG(expense_id ORDER BY purchased_at DESC))[1] AS representative_expense_id
        FROM expense_fingerprints
        GROUP BY store_id, article_fingerprint
        ORDER BY frequency DESC, last_purchased_at DESC
        LIMIT ?
      )
      SELECT
        cg.store_id,
        st.display_name AS store_label,
        st.address AS store_address,
        e.supplier_id,
        s.display_name AS supplier_label,
        e.expense_category_id,
        ec.name AS category_label,
        cg.frequency,
        cg.representative_expense_id
      FROM combo_groups cg
      JOIN expenses e ON e.id = cg.representative_expense_id
      JOIN stores st ON st.id = cg.store_id
      LEFT JOIN suppliers s ON s.id = e.supplier_id
      LEFT JOIN expense_categories ec ON ec.id = e.expense_category_id
      ORDER BY cg.frequency DESC, cg.last_purchased_at DESC"
     tenant-id limit]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- fetch-items-for-expenses
  "Batch-load items for the given representative expense IDs.
   Returns {expense-id [{:article_id :label :qty :unit_price} ...]}."
  [db expense-ids]
  (if (empty? expense-ids)
    {}
    (let [placeholders (str/join "," (repeat (count expense-ids) "?"))
          query (str "SELECT ei.expense_id, aa.article_id,
                             COALESCE(a.canonical_name, aa.raw_label) AS label,
                             ei.qty, ei.unit_price
                      FROM expense_items ei
                      JOIN article_aliases aa ON aa.id = ei.alias_id
                      LEFT JOIN articles a ON a.id = aa.article_id
                      WHERE ei.expense_id IN (" placeholders ")
                      ORDER BY ei.expense_id, ei.created_at")
          rows (jdbc/execute! db
                 (into [query] expense-ids)
                 {:builder-fn rs/as-unqualified-lower-maps})]
      (group-by :expense_id rows))))

(defn- assemble-combination
  "Turn a combo row + its items into the response shape."
  [combo items]
  {:supplier_id    (:supplier_id combo)
   :supplier_label (:supplier_label combo)
   :store_id       (:store_id combo)
   :store_label    (:store_label combo)
   :store_address  (:store_address combo)
   :category_id    (:expense_category_id combo)
   :category_label (:category_label combo)
   :frequency      (:frequency combo)
   :source         "manual"
   :items          (vec (map (fn [item]
                               {:article_id (:article_id item)
                                :label      (:label item)
                                :qty        (:qty item)
                                :unit_price (:unit_price item)})
                          items))})

;; ── public ──────────────────────────────────────────────────────────

(defn expense-combinations
  "Return up to `limit` manual expense combination templates from the
   last 6 months, grouped by (store + exact article set) and ranked by
   how often that combination was entered."
  [db tenant-id limit]
  (let [combos      (fetch-combo-groups db tenant-id limit)
        expense-ids (keep :representative_expense_id combos)
        items-by-eid (fetch-items-for-expenses db expense-ids)]
    (mapv (fn [combo]
            (let [eid   (:representative_expense_id combo)
                  items (get items-by-eid eid [])]
              (assemble-combination combo items)))
      combos)))

(defn expense-combinations-handler
  "GET /api/v1/expenses/quick-add-combinations

   Returns top manual expense combinations from recent history,
   grouped by (store + article set), ranked by frequency.
   Query params:
   - limit: optional (default 10, max 10)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              limit (min (max (or (some-> (h/get-param (:query-params request) :limit)
                                    parse-long) 10) 1) 10)]
          (if (nil? tenant-id)
            (h/json-response {:combinations []})
            (try
              (let [combos (expense-combinations db tenant-id limit)]
                (h/json-response {:combinations combos}))
              (catch Exception e
                (log/error e "Error fetching expense combinations" {:tenant-id tenant-id})
                (h/json-response {:error "Expense combinations failed"} 500))))))
      (h/unauthorized-response))))