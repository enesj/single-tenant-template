(ns app.domain.backend.expenses.services.articles.crud
  "Article CRUD operations and search."
  (:require
    [app.domain.backend.expenses.services.articles.normalization :as normalization]
    [app.shared.model-naming :as model-naming]
    [app.shared.query-builders :as shared-qb]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn create-article!
  "Create a canonical article."
  [db {:keys [canonical_name link manufacturer_id subcategory_id] :as data}]
  (when-not canonical_name
    (throw (ex-info "canonical_name is required" {:data data})))
  (let [normalized (normalization/normalize-article-key canonical_name)
        row (cond-> {:id (UUID/randomUUID)
                     :canonical_name canonical_name
                     :normalized_key normalized}

              (contains? data :subcategory_id) (assoc :subcategory_id subcategory_id)
              (contains? data :link) (assoc :link link)
              (contains? data :manufacturer_id) (assoc :manufacturer_id manufacturer_id))
        sql-map {:insert-into :articles
                 :values [row]
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-article-by-normalized-key
  "Fetch an article by its `normalized_key`."
  [db normalized-key]
  (when (seq (some-> normalized-key str str/trim))
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:articles]
                   :where [:= :normalized_key normalized-key]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-or-create-article-by-canonical-name!
  "Find or create an article based on the normalized key derived from `canonical-name`.

  This is safe under concurrency because the `articles.normalized_key` unique index
  enforces deduplication.

  Notes:
  - We do not overwrite existing canonical data if the row already exists.
  - When a unique violation happens, we fetch and return the existing row."
  [db canonical-name]
  (let [canonical-name* (some-> canonical-name str str/trim not-empty)]
    (when-not canonical-name*
      (throw (ex-info "canonical_name is required" {:canonical-name canonical-name})))
    (let [normalized (normalization/normalize-article-key canonical-name*)]
      (try
        (create-article! db {:canonical_name canonical-name*})
        (catch org.postgresql.util.PSQLException e
          (if (= "23505" (.getSQLState e))
            (or (get-article-by-normalized-key db normalized)
              (throw e))
            (throw e)))))))

(defn get-article
  [db id]
  (jdbc/execute-one! db
    (sql/format {:select [[:a.*]
                          [:m.display_name :manufacturer_display_name]
                          [:s.name :subcategory_name]
                          [:c.name :category_name]]
                 :from [[:articles :a]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :s] [:= :s.id :a.subcategory_id]
                             [:categories :c] [:= :c.id :s.category_id]]
                 :where [:= :a.id id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(def ^:private allowed-articles-order-by
  "Whitelist of client-facing order-by keys -> SQL columns.

  Keys are app keywords (kebab-case). Values are HoneySQL identifiers.

  This keeps user-facing list endpoints safe while letting the frontend sort by a
  supported subset of columns (including joined fields like manufacturer name)."
  {:canonical-name :a/canonical_name
   :manufacturer-display-name :m/display_name
   :link :a/link
   :normalized-key :a/normalized_key
   :created-at :a/created_at
   :updated-at :a/updated_at})

(defn list-articles
  "List articles with optional search/pagination.

  Sorting is allowlisted via `allowed-articles-order-by` to prevent ordering by
  arbitrary columns."
  [db {:keys [search limit offset order-by order-dir]
       :or {limit 50 offset 0 order-by :canonical_name order-dir :asc}}]
  (let [order-by* (model-naming/ensure-app-keyword order-by)
        order-col (get allowed-articles-order-by order-by* :a/canonical_name)
        order-dir* (shared-qb/normalize-order-direction order-dir {:default :asc})
        base {:select [[:a.*]
                       [:m.display_name :manufacturer_display_name]
                       [:s.name :subcategory_name]
                       [:c.name :category_name]]
              :from [[:articles :a]]
              :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                          [:subcategories :s] [:= :s.id :a.subcategory_id]
                          [:categories :c] [:= :c.id :s.category_id]]
              :order-by [[order-col order-dir*]
                         [:a.id :asc]]
              :limit limit
              :offset offset}
        query (cond-> base
                (seq search)
                (assoc :where [:or
                               [:ilike :a.canonical_name (str "%" search "%")]
                               [:ilike :a.normalized_key (str "%" search "%")]]))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-article!
  "Update a canonical article. Recomputes normalized_key when canonical_name is provided."
  [db id {:keys [canonical_name subcategory_id link manufacturer_id] :as data}]
  (let [update-map (cond-> {}
                     canonical_name (assoc :canonical_name canonical_name
                                      :normalized_key (normalization/normalize-article-key canonical_name))

                     (contains? data :subcategory_id) (assoc :subcategory_id subcategory_id)
                     (contains? data :link) (assoc :link link)
                     (contains? data :manufacturer_id) (assoc :manufacturer_id manufacturer_id))]
    (when (seq update-map)
      (jdbc/execute-one!
        db
        (sql/format {:update :articles
                     :set update-map
                     :where [:= :id id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- update-count
  "Extract next.jdbc update count from a DML result map."
  [result]
  (or (:next.jdbc/update-count result)
    (:update-count result)
    0))

(defn delete-article!
  "Delete article by id. Returns true when a row was removed."
  [db id]
  (pos?
    (update-count
      (jdbc/execute-one!
        db
        (sql/format {:delete-from :articles
                     :where [:= :id id]})))))

(defn count-articles
  "Count total articles, optionally with search filter.

  Accepts a map with optional :search key, matching the signature expected by
  the routes factory count handler."
  [db {:keys [search]}]
  (let [base-query {:select [[[:count :*] :total]]
                    :from [:articles]}
        final-query (if (seq search)
                      (assoc base-query :where [:or
                                                [:ilike :canonical_name (str "%" search "%")]
                                                [:ilike :normalized_key (str "%" search "%")]])
                      base-query)]
    {:total (or (:total (jdbc/execute-one! db (sql/format final-query)
                          {:builder-fn rs/as-unqualified-lower-maps}))
              0)}))

(defn search-articles
  "Search articles for autocomplete."
  [db query {:keys [limit] :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [search-pattern (str "%" query "%")]
      (jdbc/execute!
        db
        (sql/format {:select [:*]
                     :from [:articles]
                     :where [:or
                             [:ilike :canonical_name search-pattern]
                             [:ilike :normalized_key search-pattern]]
                     :order-by [[:canonical_name :asc]]
                     :limit limit})
        {:builder-fn rs/as-unqualified-lower-maps}))))
