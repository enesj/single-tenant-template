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

(defn- find-alias-by-normalized-key
  [db normalized]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:supplier_aliases]
                 :where [:= :raw_label_normalized normalized]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-or-create-alias!
  "Find or create a supplier_alias by raw_label (global uniqueness).

  Returns the alias row (with :id, :supplier_id, etc.) and `:created?` to
  indicate whether this call inserted a new alias row."
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
             :created_at [:now]}]
    (when (or (str/blank? normalized)
            (< (count normalized) min-alias-normalized-length))
      (throw (ex-info "raw_label normalizes to an invalid key"
               {:status 400
                :field :raw_label
                :raw_label raw-label*
                :raw_label_normalized normalized})))
    (if-let [inserted (jdbc/execute-one!
                        db
                        (sql/format {:insert-into :supplier_aliases
                                     :values [row]
                                     :on-conflict [:raw_label_normalized]
                                     :do-nothing true
                                     :returning [:*]})
                        {:builder-fn rs/as-unqualified-lower-maps})]
      (assoc inserted :created? true)
      (assoc (or (jdbc/execute-one!
                   db
                   (sql/format {:update :supplier_aliases
                                :set {:raw_label raw-label*}
                                :where [:= :raw_label_normalized normalized]
                                :returning [:*]})
                   {:builder-fn rs/as-unqualified-lower-maps})
               (find-alias-by-normalized-key db normalized))
        :created? false))))

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

(defn count-supplier-aliases
  "Count supplier aliases with optional filters.

  Supports:
  - :supplier-id / :supplier_id
  - :unmapped-only (boolean, filters to supplier_id IS NULL)"
  [db {:keys [supplier-id supplier_id unmapped-only] :as opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :sa/supplier_id supplier-uuid])
                       unmapped-only (conj [:is :sa/supplier_id nil]))
        config* (assoc config :base-filters base-filters)]
    ((factory/build-count-function config*) db opts)))

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
                        :confidence (or confidence 100)}
                  :where [:= :id alias-id]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(def ^:private default-ocr-confidence
  "Confidence score used when OCR auto-maps an alias; lower than manual mappings (100)."
  25)

(defn map-alias-to-supplier-if-unmapped!
  "Map a supplier alias to a canonical supplier only if it is currently unmapped.

  This is safe to run during automated ingestion (OCR) because it will NOT
  overwrite an existing manual mapping.

  Returns the updated alias row when an update happened, otherwise nil."
  ([db alias-id supplier-id]
   (map-alias-to-supplier-if-unmapped! db alias-id supplier-id default-ocr-confidence))
  ([db alias-id supplier-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :supplier_aliases
                  :set {:supplier_id supplier-id
                        :confidence (or confidence default-ocr-confidence)}
                  :where [:and
                          [:= :id alias-id]
                          [:is :supplier_id nil]]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

;; ============================================================================
;; Batch Operations
;; ============================================================================




