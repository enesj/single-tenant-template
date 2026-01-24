(ns app.domain.backend.expenses.services.supplier-aliases
  "Supplier alias management (deduped raw supplier guesses -> canonical supplier mapping).

  Design:
  - supplier_aliases are globally unique by raw_label_normalized
  - receipts reference supplier_aliases via receipts.supplier_alias_id for cheap counts/joins"
  (:require
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
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :supplier-alias))

(def service (factory/build-entity-service config))

(def ^:private try-uuid type-conv/try-parse-uuid)

(def ^:private min-alias-normalized-length
  "Minimum length of a normalized supplier alias key to be considered valid."
  2)

;; ============================================================================
;; Core Operations
;; ============================================================================

(defn find-or-create-alias!
  "Find or create a supplier_alias by raw_label (global uniqueness).

  Returns the alias row (with :id, :supplier_id, etc.)."
  [db raw-label]
  (when (str/blank? raw-label)
    (throw (ex-info "raw_label is required" {:status 400 :field :raw_label})))
  (let [raw-label* (str/trim (str raw-label))
        normalized (configs/normalize-supplier-key raw-label*)
        row {:id (UUID/randomUUID)
             :raw_label raw-label*
             :raw_label_normalized normalized
             :supplier_id nil
             :confidence 0
             :created_at [:now]
             :updated_at [:now]}
        sql-map {:insert-into :supplier_aliases
                 :values [row]
                 :on-conflict [:raw_label_normalized]
                 :do-update-set {:raw_label :excluded/raw_label
                                 :updated_at [:now]}
                 :returning [:*]}]
    (when (or (str/blank? normalized)
            (< (count normalized) min-alias-normalized-length))
      (throw (ex-info "raw_label normalizes to an invalid key"
               {:status 400
                :field :raw_label
                :raw_label raw-label*
                :raw_label_normalized normalized})))
    (jdbc/execute-one!
      db
      (sql/format sql-map)
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-alias
  [db alias-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:supplier_aliases]
                 :where [:= :id alias-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-supplier-aliases
  "List supplier aliases with optional filters.

  Supports:
  - :supplier-id / :supplier_id
  - :unmapped-only (boolean, filters to supplier_id IS NULL)"
  [db {:keys [limit offset order-by order-dir search supplier-id supplier_id unmapped-only]
       :or {limit 50 offset 0 order-dir :asc}
       :as opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :sa/supplier_id supplier-uuid])
                       unmapped-only (conj [:is :sa/supplier_id nil]))
        config* (assoc config :base-filters base-filters)
        base-query (factory/build-query-with-filters
                     config*
                     {:limit limit
                      :offset offset
                      :order-by order-by
                      :order-dir order-dir})
        final-query (factory/apply-search-filter base-query (:search-fields config*) search)]
    (if (or supplier-uuid unmapped-only)
      (jdbc/execute! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})
      ((:list service) db opts))))

(defn list-unmapped-aliases
  "List unmapped supplier aliases (supplier_id IS NULL) with occurrence counts.

  Counts are derived from receipts.supplier_alias_id."
  [db {:keys [limit offset]
       :or {limit 100 offset 0}}]
  (let [query {:select [[:sa.id]
                        [:sa.raw_label]
                        [:sa.raw_label_normalized]
                        [:sa.supplier_id]
                        [:sa.confidence]
                        [:sa.created_at]
                        [:sa.updated_at]
                        [:s.display_name :supplier_display_name]
                        [[:count :r.id] :occurrence_count]]
               :from [[:supplier_aliases :sa]]
               :left-join [[:suppliers :s] [:= :sa.supplier_id :s.id]
                           [:receipts :r] [:= :r.supplier_alias_id :sa.id]]
               :where [:and
                       [:is :sa.supplier_id nil]]
               :group-by [:sa.id :s.display_name]
               :order-by [[[:count :r.id] :desc]
                          [:sa.created_at :desc]]
               :limit limit
               :offset offset}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-supplier!
  "Map a supplier alias to a canonical supplier.

  Sets supplier_aliases.supplier_id for the given alias."
  ([db alias-id supplier-id]
   (map-alias-to-supplier! db alias-id supplier-id 100))
  ([db alias-id supplier-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :supplier_aliases
                  :set {:supplier_id supplier-id
                        :confidence (or confidence 100)
                        :updated_at [:now]}
                  :where [:= :id alias-id]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-supplier-if-unmapped!
  "Map a supplier alias to a canonical supplier only if it is currently unmapped.

  This is safe to run during automated ingestion (OCR) because it will NOT
  overwrite an existing manual mapping.

  Returns the updated alias row when an update happened, otherwise nil." 
  ([db alias-id supplier-id]
   (map-alias-to-supplier-if-unmapped! db alias-id supplier-id 25))
  ([db alias-id supplier-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :supplier_aliases
                  :set {:supplier_id supplier-id
                        :confidence (or confidence 25)
                        :updated_at [:now]}
                  :where [:and
                          [:= :id alias-id]
                          [:is :supplier_id nil]]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

  (defn backfill-unmapped-alias-links!
    "Backfill supplier_aliases.supplier_id for aliases that already have a matching
    supplier row.

    This links by matching:
    - supplier_aliases.raw_label_normalized == suppliers.normalized_key

    Returns the updated alias rows (may be empty).

    Intended for one-off repair after enabling OCR-created supplier_aliases." 
    ([db]
     (backfill-unmapped-alias-links! db {:confidence 25}))
    ([db {:keys [confidence]
      :or {confidence 25}}]
     (jdbc/execute!
       db
       (sql/format {:update [[:supplier_aliases :sa]]
        :set {:supplier_id :s/id
          :confidence confidence
          :updated_at [:now]}
        :from [[:suppliers :s]]
        :where [:and
            [:is :sa/supplier_id nil]
            [:= :sa/raw_label_normalized :s/normalized_key]]
        :returning [:sa/id :sa/supplier_id :sa/raw_label_normalized]})
       {:builder-fn rs/as-unqualified-lower-maps})))

(defn unmap-alias!
  "Remove supplier mapping from an alias (set supplier_id to NULL)."
  [db alias-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :supplier_aliases
                 :set {:supplier_id nil
                       :confidence 0
                       :updated_at [:now]}
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

;; ============================================================================
;; Batch Operations
;; ============================================================================

(defn- raw-labels->seq
  "Accept raw labels in either vector form or as a newline-separated string."
  [raw-labels]
  (cond
    (nil? raw-labels) []
    (string? raw-labels) (->> (str/split-lines raw-labels)
                           (map str/trim)
                           (remove str/blank?))
    (sequential? raw-labels) (->> raw-labels (map (fn [x] (when x (str x)))))
    :else []))

(defn- get-alias-by-normalized
  [db raw-label-normalized]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:supplier_aliases]
                 :where [:= :raw_label_normalized raw-label-normalized]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- insert-alias!
  [db {:keys [raw-label raw-label-normalized supplier-id confidence]}]
  (let [row {:id (UUID/randomUUID)
             :raw_label raw-label
             :raw_label_normalized raw-label-normalized
             :supplier_id supplier-id
             :confidence (or confidence 100)
             :created_at [:now]
             :updated_at [:now]}]
    (jdbc/execute-one!
      db
      (sql/format {:insert-into :supplier_aliases
                   :values [row]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- update-alias-supplier!
  [db alias-id {:keys [supplier-id raw-label confidence]}]
  (jdbc/execute-one!
    db
    (sql/format {:update :supplier_aliases
                 :set (cond-> {:supplier_id supplier-id
                               :updated_at [:now]}
                        (some? raw-label) (assoc :raw_label raw-label)
                        (some? confidence) (assoc :confidence confidence))
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn batch-create-aliases!
  "Batch-create supplier aliases for a single canonical supplier.

  Input:
  - supplier-id (UUID)
  - raw-labels (seq of strings, or a newline-separated string)

  Options:
  - allow-reassign? (default false): when true, existing aliases mapped to a
    different supplier will be reassigned.

  Returns:
  {:created [...]
   :skipped [...]
   :conflicts [...]
   :reassigned [...]}

  Notes:
  - Skips blanks/invalid labels.
  - Dedupes by normalized key.
  - Does NOT silently reassign conflicts unless allow-reassign? is true."
  [db {:keys [supplier-id raw-labels allow-reassign?]
       :or {allow-reassign? false}}]
  (when-not supplier-id
    (throw (ex-info "supplier-id is required" {:status 400})))

  (jdbc/with-transaction [tx db]
    (let [inputs (raw-labels->seq raw-labels)
          ;; Keep both the original raw label and normalized key for user feedback.
          ;; Normalize early so we can dedupe.
          normalized (map (fn [raw]
                            (let [raw* (when raw (str/trim raw))
                                  n (configs/normalize-supplier-key raw*)]
                              {:raw-label raw*
                               :raw-label-normalized n}))
                       inputs)
          step (reduce
                 (fn [{:keys [seen] :as acc} {:keys [raw-label raw-label-normalized]}]
                   (cond
                     (str/blank? raw-label)
                     (update acc :skipped conj {:raw-label raw-label
                                                :reason :blank})

                     (str/blank? raw-label-normalized)
                     (update acc :skipped conj {:raw-label raw-label
                                                :reason :normalizes-to-blank})

                     (< (count raw-label-normalized) min-alias-normalized-length)
                     (update acc :skipped conj {:raw-label raw-label
                                                :raw-label-normalized raw-label-normalized
                                                :reason :too-short})

                     (contains? seen raw-label-normalized)
                     (update acc :skipped conj {:raw-label raw-label
                                                :raw-label-normalized raw-label-normalized
                                                :reason :duplicate})

                     :else
                     (let [existing (get-alias-by-normalized tx raw-label-normalized)
                           acc* (update acc :seen conj raw-label-normalized)]
                       (cond
                         (nil? existing)
                         (let [inserted (insert-alias!
                                          tx
                                          {:supplier-id supplier-id
                                           :raw-label raw-label
                                           :raw-label-normalized raw-label-normalized
                                           :confidence 100})]
                           (update acc* :created conj inserted))

                         (= (:supplier_id existing) supplier-id)
                         (do
                           (update-alias-supplier!
                             tx
                             (:id existing)
                             {:supplier-id supplier-id
                              :raw-label raw-label})
                           (update acc* :skipped conj {:raw-label raw-label
                                                       :raw-label-normalized raw-label-normalized
                                                       :reason :already-present
                                                       :alias-id (:id existing)}))

                         (nil? (:supplier_id existing))
                         (let [updated (update-alias-supplier!
                                         tx
                                         (:id existing)
                                         {:supplier-id supplier-id
                                          :raw-label raw-label
                                          :confidence 100})]
                           (update acc* :reassigned conj {:alias-id (:id updated)
                                                          :raw-label-normalized raw-label-normalized
                                                          :existing-supplier-id nil
                                                          :supplier-id supplier-id}))

                         (false? allow-reassign?)
                         (update acc* :conflicts conj {:raw-label raw-label
                                                       :raw-label-normalized raw-label-normalized
                                                       :alias-id (:id existing)
                                                       :existing-supplier-id (:supplier_id existing)
                                                       :supplier-id supplier-id})

                         :else
                         (let [updated (update-alias-supplier!
                                         tx
                                         (:id existing)
                                         {:supplier-id supplier-id
                                          :raw-label raw-label
                                          :confidence 100})]
                           (update acc* :reassigned conj {:alias-id (:id updated)
                                                          :raw-label-normalized raw-label-normalized
                                                          :existing-supplier-id (:supplier_id existing)
                                                          :supplier-id supplier-id}))))))
                 {:seen #{}
                  :created []
                  :skipped []
                  :conflicts []
                  :reassigned []}
                 normalized)
          {:keys [created skipped conflicts reassigned]} step]
      {:created created
       :skipped skipped
       :conflicts conflicts
       :reassigned reassigned})))
