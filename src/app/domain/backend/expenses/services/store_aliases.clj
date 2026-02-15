(ns app.domain.backend.expenses.services.store-aliases
  "Store alias management (deduped raw store/address guesses -> canonical store mapping).

  Design:
  - store_aliases are globally unique by raw_label_normalized
  - receipts reference store_aliases via receipts.store_alias_id for cheap counts/joins"
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

(def config (configs/get-entity-config :store-alias))

(def service (factory/build-entity-service config))

(def ^:private try-uuid type-conv/try-parse-uuid)

(def ^:private min-alias-normalized-length
  2)

(defn find-or-create-alias!
  "Find or create a store_alias by raw_label (global uniqueness).

  Returns the alias row (with :id, :store_id, etc.)."
  [db raw-label]
  (when (str/blank? raw-label)
    (throw (ex-info "raw_label is required" {:status 400 :field :raw_label})))
  (let [raw-label* (str/trim (str raw-label))
        normalized (configs/normalize-store-key raw-label*)
        row {:id (UUID/randomUUID)
             :raw_label raw-label*
             :raw_label_normalized normalized
             :store_id nil
             :confidence 0
             :created_at [:now]}
        sql-map {:insert-into :store_aliases
                 :values [row]
                 :on-conflict [:raw_label_normalized]
                 :do-update-set {:raw_label :excluded/raw_label}
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
                 :from [:store_aliases]
                 :where [:= :id alias-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-store-aliases
  "List store aliases with optional filters.

  Supports:
  - :store-id / :store_id
  - :unmapped-only (boolean, filters to store_id IS NULL)"
  [db {:keys [limit offset order-by order-dir search store-id store_id unmapped-only]
       :or {limit 50 offset 0 order-dir :asc}
       :as opts}]
  (let [store-uuid (try-uuid (or store-id store_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       store-uuid (conj [:= :sta/store_id store-uuid])
                       unmapped-only (conj [:is :sta/store_id nil]))
        config* (assoc config :base-filters base-filters)
        base-query (factory/build-query-with-filters
                     config*
                     {:limit limit
                      :offset offset
                      :order-by order-by
                      :order-dir order-dir})
        final-query (factory/apply-search-filter base-query (:search-fields config*) search)]
    (if (or store-uuid unmapped-only)
      (jdbc/execute! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})
      ((:list service) db opts))))

(defn list-unmapped-aliases
  "List unmapped store aliases (store_id IS NULL) with occurrence counts.

  Counts are derived from receipts.store_alias_id."
  [db {:keys [limit offset]
       :or {limit 100 offset 0}}]
  (let [query {:select [[:sta.id]
                        [:sta.raw_label]
                        [:sta.raw_label_normalized]
                        [:sta.store_id]
                        [:sta.confidence]
                        [:sta.created_at]
                        [:sta.updated_at]
                        [:st.display_name :store_display_name]
                        [[:count :r.id] :occurrence_count]]
               :from [[:store_aliases :sta]]
               :left-join [[:stores :st] [:= :sta.store_id :st.id]
                           [:receipts :r] [:= :r.store_alias_id :sta.id]]
               :where [:and
                       [:is :sta.store_id nil]]
               :group-by [:sta.id :st.display_name]
               :order-by [[[:count :r.id] :desc]
                          [:sta.created_at :desc]]
               :limit limit
               :offset offset}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-store!
  "Map a store alias to a canonical store.

  Sets store_aliases.store_id for the given alias."
  ([db alias-id store-id]
   (map-alias-to-store! db alias-id store-id 100))
  ([db alias-id store-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :store_aliases
                  :set {:store_id store-id
                        :confidence (or confidence 100)}
                  :where [:= :id alias-id]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-store-if-unmapped!
  "Map a store alias to a canonical store only if it is currently unmapped.

  Safe to run during automated ingestion (OCR) because it will NOT overwrite an
  existing manual mapping.

  Returns the updated alias row when an update happened, otherwise nil."
  ([db alias-id store-id]
   (map-alias-to-store-if-unmapped! db alias-id store-id 25))
  ([db alias-id store-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :store_aliases
                  :set {:store_id store-id
                        :confidence (or confidence 25)}
                  :where [:and
                          [:= :id alias-id]
                          [:is :store_id nil]]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn unmap-alias!
  "Remove store mapping from an alias (set store_id to NULL)."
  [db alias-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :store_aliases
                 :set {:store_id nil
                       :confidence 0}
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))
