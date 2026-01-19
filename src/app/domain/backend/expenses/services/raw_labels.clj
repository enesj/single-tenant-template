(ns app.domain.backend.expenses.services.raw-labels
  "Raw label deduplication.

  A raw label is the original line-item label extracted from a receipt (e.g. \"MILK 1L\").
  We dedupe these into `raw_labels` using `normalized_key` so we can store a stable
  FK from `expense_items`."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    (java.util UUID)))

(def ^:private normalized-key-max-len 255)

(defn normalize-raw-label-key
  "Normalize a raw label for deduplication.

  Keeps the same normalization semantics as article alias labels." 
  [raw-label]
  (when (some? raw-label)
    (let [normalized (some-> raw-label str str/trim articles/normalize-alias-label)]
      (when (some? normalized)
        (subs normalized 0 (min normalized-key-max-len (count normalized)))))))

(defn find-or-create-raw-label!
  "Upsert a row in `raw_labels` and return it.

  Dedupes by `normalized_key`.
  - Always returns a row (insert or update)
  - Updates `updated_at` on conflict so callers can safely use RETURNING.

  Throws when `raw-label` is nil." 
  [db raw-label]
  (when (nil? raw-label)
    (throw (ex-info "raw_label is required" {:status 400 :field :raw_label})))
  (let [raw-label* (-> raw-label str str/trim)
        normalized (or (normalize-raw-label-key raw-label*) "")
        row {:id (UUID/randomUUID)
             :raw_label raw-label*
             :normalized_key normalized}
        sql-map {:insert-into :raw_labels
                 :values [row]
                 :on-conflict [:normalized_key]
                 :do-update-set {:raw_label :excluded/raw_label
                                 :updated_at [:now]}
                 :returning [:*]}]
    (jdbc/execute-one!
      db
      (sql/format sql-map)
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn backfill-expense-items-raw-label-ids!
  "Backfill `expense_items.raw_label_id` for rows where it is NULL.

  Uses `expense_items.raw_label` (text) as the source label.

  Returns {:updated <n> :labels <n-distinct>}." 
  [db]
  (jdbc/with-transaction [tx db]
    (let [items (jdbc/execute!
                  tx
                  (sql/format {:select [:id :raw_label]
                               :from [:expense_items]
                               ;; Include soft-deleted rows too; NOT NULL constraints
                               ;; apply to the whole table.
                               :where [:is :raw_label_id nil]})
                  {:builder-fn rs/as-unqualified-lower-maps})
          distinct-labels (->> items (map :raw_label) distinct)
          cache (into {}
                  (map (fn [lbl]
                         [lbl (:id (find-or-create-raw-label! tx lbl))]))
                  distinct-labels)
          updated (reduce
                    (fn [n {:keys [id raw_label]}]
                      (let [rid (get cache raw_label)]
                        (+ n
                          (::jdbc/update-count
                            (jdbc/execute-one!
                              tx
                              (sql/format {:update :expense_items
                                           :set {:raw_label_id rid}
                                           :where [:= :id id]}))))))
                    0
                    items)]
      {:updated updated
        :labels (count distinct-labels)})))

;; ---------------------------------------------------------------------------
;; Queries
;; ---------------------------------------------------------------------------

(def ^:private raw-labels-orderable-cols
  #{:raw_label :normalized_key :created_at :updated_at})

(defn list-raw-labels
  "List raw labels for the tenant.

  opts:
  - :limit/:offset
  - :search (matches raw_label or normalized_key)
  - :order-by (one of :raw_label/:normalized_key/:created_at/:updated_at)
  - :order-dir (:asc/:desc)"
  [db {:keys [limit offset search order-by order-dir]
       :or {limit 200 offset 0 order-by :updated_at order-dir :desc}}]
  (let [order-by* (if (contains? raw-labels-orderable-cols order-by)
                    order-by
                    :updated_at)
        order-dir* (if (contains? #{:asc :desc} order-dir) order-dir :desc)
        search* (some-> search str str/trim)
        where-clause (when (seq search*)
                       [:or
                        [:ilike :raw_label (str "%" search* "%")]
                        [:ilike :normalized_key (str "%" search* "%")]])
        sql-map (cond-> {:select [:*]
                         :from [:raw_labels]
                         :order-by [[order-by* order-dir*]]
                         :limit limit
                         :offset offset}
                  where-clause (assoc :where where-clause))]
    (jdbc/execute!
      db
      (sql/format sql-map)
      {:builder-fn rs/as-unqualified-lower-maps})))
