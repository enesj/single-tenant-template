(ns app.domain.backend.expenses.services.manufacturer-aliases
  "Manufacturer alias management (deduped raw manufacturer strings -> canonical manufacturer mapping).

  Design:
  - manufacturer_aliases are globally unique by raw_label_normalized
  - articles reference manufacturer_aliases via articles.manufacturer_alias_id
    so we can track unmapped manufacturer strings and curate mappings"
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

(def config (configs/get-entity-config :manufacturer-alias))

(def service (factory/build-entity-service config))

(def ^:private try-uuid type-conv/try-parse-uuid)

(def ^:private min-alias-normalized-length
  "Minimum length of a normalized manufacturer alias key to be considered valid."
  2)

;; ============================================================================
;; Core Operations
;; ============================================================================

(defn find-or-create-alias!
  "Find or create a manufacturer_alias by raw_label (global uniqueness).

  Returns the alias row (with :id, :manufacturer_id, etc.)."
  [db raw-label]
  (when (str/blank? raw-label)
    (throw (ex-info "raw_label is required" {:status 400 :field :raw_label})))
  (let [raw-label* (str/trim (str raw-label))
        normalized (configs/normalize-manufacturer-key raw-label*)
        row {:id (UUID/randomUUID)
             :raw_label raw-label*
             :raw_label_normalized normalized
             :manufacturer_id nil
             :confidence 0
             :created_at [:now]
             :updated_at [:now]}
        sql-map {:insert-into :manufacturer_aliases
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
                 :from [:manufacturer_aliases]
                 :where [:= :id alias-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-manufacturer-aliases
  "List manufacturer aliases with optional filters.

  Supports:
  - :manufacturer-id / :manufacturer_id
  - :unmapped-only (boolean, filters to manufacturer_id IS NULL)"
  [db {:keys [limit offset order-by order-dir search manufacturer-id manufacturer_id unmapped-only]
       :or {limit 50 offset 0 order-dir :asc}
       :as opts}]
  (let [manufacturer-uuid (try-uuid (or manufacturer-id manufacturer_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       manufacturer-uuid (conj [:= :ma/manufacturer_id manufacturer-uuid])
                       unmapped-only (conj [:is :ma/manufacturer_id nil]))
        config* (assoc config :base-filters base-filters)
        base-query (factory/build-query-with-filters
                     config*
                     {:limit limit
                      :offset offset
                      :order-by order-by
                      :order-dir order-dir})
        final-query (factory/apply-search-filter base-query (:search-fields config*) search)]
    (if (or manufacturer-uuid unmapped-only)
      (jdbc/execute! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})
      ((:list service) db opts))))

(defn list-unmapped-aliases
  "List unmapped manufacturer aliases (manufacturer_id IS NULL) with occurrence counts.

  Counts are derived from articles.manufacturer_alias_id."
  [db {:keys [limit offset]
       :or {limit 100 offset 0}}]
  (let [query {:select [[:ma.id]
                        [:ma.raw_label]
                        [:ma.raw_label_normalized]
                        [:ma.manufacturer_id]
                        [:ma.confidence]
                        [:ma.created_at]
                        [:ma.updated_at]
                        [:m.display_name :manufacturer_display_name]
                        [[:count :a.id] :occurrence_count]]
               :from [[:manufacturer_aliases :ma]]
               :left-join [[:manufacturers :m] [:= :ma.manufacturer_id :m.id]
                           [:articles :a] [:= :a.manufacturer_alias_id :ma.id]]
               :where [:and
                       [:is :ma.manufacturer_id nil]]
               :group-by [:ma.id :m.display_name]
               :order-by [[[:count :a.id] :desc]
                          [:ma.created_at :desc]]
               :limit limit
               :offset offset}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-manufacturer!
  "Map a manufacturer alias to a canonical manufacturer.

  Sets manufacturer_aliases.manufacturer_id for the given alias."
  ([db alias-id manufacturer-id]
   (map-alias-to-manufacturer! db alias-id manufacturer-id 100))
  ([db alias-id manufacturer-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :manufacturer_aliases
                  :set {:manufacturer_id manufacturer-id
                        :confidence (or confidence 100)
                        :updated_at [:now]}
                  :where [:= :id alias-id]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-manufacturer-if-unmapped!
  "Map a manufacturer alias to a canonical manufacturer only if it is currently unmapped.

  Returns the updated alias row when an update happened, otherwise nil."
  ([db alias-id manufacturer-id]
   (map-alias-to-manufacturer-if-unmapped! db alias-id manufacturer-id 25))
  ([db alias-id manufacturer-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :manufacturer_aliases
                  :set {:manufacturer_id manufacturer-id
                        :confidence (or confidence 25)
                        :updated_at [:now]}
                  :where [:and
                          [:= :id alias-id]
                          [:is :manufacturer_id nil]]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn backfill-unmapped-alias-links!
  "Backfill manufacturer_aliases.manufacturer_id for aliases that already have a matching
  manufacturer row.

  This links by matching:
  - manufacturer_aliases.raw_label_normalized == manufacturers.normalized_key

  Returns the updated alias rows (may be empty).

  NOTE: Intentionally not called automatically; meant for one-off repair/migration."
  ([db]
   (backfill-unmapped-alias-links! db {:confidence 25}))
  ([db {:keys [confidence]
        :or {confidence 25}}]
   (jdbc/execute!
     db
     (sql/format {:update [[:manufacturer_aliases :ma]]
                  :set {:manufacturer_id :m/id
                        :confidence confidence
                        :updated_at [:now]}
                  :from [[:manufacturers :m]]
                  :where [:and
                          [:is :ma/manufacturer_id nil]
                          [:= :ma/raw_label_normalized :m/normalized_key]]
                  :returning [:ma/id :ma/manufacturer_id :ma/raw_label_normalized]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn unmap-alias!
  "Remove manufacturer mapping from an alias (set manufacturer_id to NULL)."
  [db alias-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :manufacturer_aliases
                 :set {:manufacturer_id nil
                       :confidence 0
                       :updated_at [:now]}
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))
