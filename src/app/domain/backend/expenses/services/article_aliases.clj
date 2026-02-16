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
                       :do-update-set {:display_name :excluded/display_name}
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
             :article_id nil}
        sql-map {:insert-into :article_aliases
                 :values [row]
                 :on-conflict [:supplier_id :raw_label_normalized]
                 :do-update-set {:raw_label :excluded/raw_label}
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

(defn count-article-aliases
  "Count article aliases with optional filters.

   Supports:
   - :supplier-id / :supplier_id
   - :article-id / :article_id
   - :unmapped-only (boolean, filters to article_id IS NULL)"
  [db {:keys [search supplier-id supplier_id article-id article_id unmapped-only] :as _opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        article-uuid (try-uuid (or article-id article_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :aa/supplier_id supplier-uuid])
                       article-uuid (conj [:= :aa/article_id article-uuid])
                       unmapped-only (conj [:is :aa/article_id nil]))
        config* (assoc config :base-filters base-filters)]
    (if (or supplier-uuid article-uuid unmapped-only)
      (let [base-query (factory/build-base-query config*)
            count-query (-> base-query
                          (dissoc :order-by :limit :offset)
                          (assoc :select [[[:count :*] :total]]))
            final-query (factory/apply-search-filter count-query (:search-fields config*) search)]
        (:total (jdbc/execute-one! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})))
      ((:count service) db {:search search}))))

(defn list-unmapped-aliases
  "List unmapped article aliases (article_id IS NULL) with occurrence counts."
  [db {:keys [limit offset supplier-id]
       :or {limit 100 offset 0}}]
  (let [supplier-uuid (try-uuid supplier-id)
        base-query {:select [[:aa.id]
                             [:aa.raw_label]
                             [:aa.raw_label_normalized]
                             [:aa.supplier_id]
                             [:s.display_name :supplier_display_name]
                             [[:count :ei.id] :occurrence_count]]
                    :from [[:article_aliases :aa]]
                    :left-join [[:suppliers :s] [:= :aa.supplier_id :s.id]
                                [:expense_items :ei] [:= :ei.alias_id :aa.id]]
                    :where [:and
                            [:is :aa.article_id nil]]
                    :group-by [:aa.id :s.display_name]
                    :order-by [[[:count :ei.id] :desc]]
                    :limit limit
                    :offset offset}
        query (cond-> base-query
                supplier-uuid
                (update :where conj [:= :aa.supplier_id supplier-uuid]))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-unmapped-aliases
  "Count unmapped article aliases (article_id IS NULL), optionally by supplier."
  [db {:keys [supplier-id supplier_id]}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        query (cond-> {:select [[[:count :aa.id] :total]]
                       :from [[:article_aliases :aa]]
                       :where [:is :aa.article_id nil]}
                supplier-uuid (assoc :where [:and
                                             [:is :aa.article_id nil]
                                             [:= :aa.supplier_id supplier-uuid]]))]
    (:total (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))))

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

