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

(def ^:private default-unit "kom")

(defn- normalize-unit
  [unit]
  (or (some-> unit str str/trim str/lower-case not-empty)
    default-unit))

(defn create-article!
  "Create a canonical article."
  [db {:keys [canonical_name link manufacturer_id subcategory_id unit] :as data}]
  (when-not canonical_name
    (throw (ex-info "canonical_name is required" {:data data})))
  (let [normalized (normalization/normalize-article-key canonical_name)
        unit* (normalize-unit unit)
        row (cond-> {:id (UUID/randomUUID)
                     :canonical_name canonical_name
                     :normalized_key normalized
                     :unit unit*}

              (contains? data :subcategory_id) (assoc :subcategory_id subcategory_id)
              (contains? data :link) (assoc :link link)
              (contains? data :manufacturer_id) (assoc :manufacturer_id manufacturer_id))
        sql-map {:insert-into :articles
                 :values [row]
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-article-by-normalized-key
  "Fetch an article by its `normalized_key` and unit."
  ([db normalized-key]
   (get-article-by-normalized-key db normalized-key nil))
  ([db normalized-key unit]
   (when (seq (some-> normalized-key str str/trim))
     (let [unit* (normalize-unit unit)]
       (jdbc/execute-one!
         db
         (sql/format {:select [:*]
                      :from [:articles]
                      :where [:and
                              [:= :normalized_key normalized-key]
                              [:= :unit unit*]]
                      :limit 1})
         {:builder-fn rs/as-unqualified-lower-maps})))))

(defn- insert-article-if-absent!
  "Insert an article row when it does not already exist for normalized-key + unit.

  Returns the inserted row, or nil when an existing row already owns the unique key."
  [db row]
  (jdbc/execute-one!
    db
    (sql/format {:insert-into :articles
                 :values [row]
                 :on-conflict [:normalized_key :unit]
                 :do-nothing true
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-or-create-article-by-canonical-name!
  "Find or create an article based on canonical name + unit.

  This stays safe under concurrency by relying on `ON CONFLICT DO NOTHING`
  against the `articles(normalized_key, unit)` unique index, then reading the
  existing row when another transaction won the race.

  Notes:
  - We do not overwrite existing canonical data if the row already exists.
  - Blank/nil units normalize to the default `kom` unit."
  ([db canonical-name]
   (find-or-create-article-by-canonical-name! db canonical-name nil))
  ([db canonical-name unit]
   (let [canonical-name* (some-> canonical-name str str/trim not-empty)]
     (when-not canonical-name*
       (throw (ex-info "canonical_name is required" {:canonical-name canonical-name})))
     (let [normalized (normalization/normalize-article-key canonical-name*)
           unit* (normalize-unit unit)
           row {:id (UUID/randomUUID)
                :canonical_name canonical-name*
                :normalized_key normalized
                :unit unit*}]
       (or (get-article-by-normalized-key db normalized unit*)
         (insert-article-if-absent! db row)
         (get-article-by-normalized-key db normalized unit*))))))

(def ^:private allowed-articles-order-by
  "Whitelist of client-facing order-by keys -> SQL columns.

  Keys are app keywords (kebab-case). Values are HoneySQL identifiers.

  This keeps user-facing list endpoints safe while letting the frontend sort by a
  supported subset of columns (including joined fields like manufacturer,
  category, and subcategory names)."
  {:canonical-name :a/canonical_name
   :unit :a/unit
   :category-name :c/name
   :subcategory-name :s/name
   :manufacturer-display-name :m/display_name
   :link :a/link
   :normalized-key :a/normalized_key
   :created-at :a/created_at
   :updated-at :a/updated_at})

(defn- apply-column-filters
  "Apply optional per-column ILIKE filters to a HoneySQL query map.
  Each filter key maps to a qualified column on the joined article query."
  [query {:keys [search unit category-name subcategory-name manufacturer-display-name]}]
  (cond-> query
    (seq search)
    (update :where shared-qb/merge-where-and
      [:or
       [:ilike :a.canonical_name (str "%" search "%")]
       [:ilike :a.normalized_key (str "%" search "%")]
       [:ilike :a.unit (str "%" search "%")]])

    (seq unit)
    (update :where shared-qb/merge-where-and
      [:ilike :a.unit (str "%" unit "%")])

    (seq category-name)
    (update :where shared-qb/merge-where-and
      [:ilike :c.name (str "%" category-name "%")])

    (seq subcategory-name)
    (update :where shared-qb/merge-where-and
      [:ilike :s.name (str "%" subcategory-name "%")])

    (seq manufacturer-display-name)
    (update :where shared-qb/merge-where-and
      [:ilike :m.display_name (str "%" manufacturer-display-name "%")])))

(defn- apply-extra-filters
  "Apply a seq of HoneySQL WHERE clauses (e.g. date-range filters from the
  routes factory) to a query map."
  [query extra-filters]
  (reduce (fn [q clause]
            (update q :where shared-qb/merge-where-and clause))
    query
    (or extra-filters [])))

(defn list-articles
  "List articles with optional search/pagination and per-column filters.

  Sorting is allowlisted via `allowed-articles-order-by` to prevent ordering by
  arbitrary columns.

  Accepts :extra-filters — a seq of HoneySQL WHERE clauses injected by the
  routes factory (e.g. date-range filters)."
  [db {:keys [search unit category-name subcategory-name manufacturer-display-name
              extra-filters limit offset order-by order-dir]
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
        query (-> base
                (apply-column-filters
                  {:search search
                   :unit unit
                   :category-name category-name
                   :subcategory-name subcategory-name
                   :manufacturer-display-name manufacturer-display-name})
                (apply-extra-filters extra-filters))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-article!
  "Update a canonical article. Recomputes normalized_key when canonical_name is provided."
  [db id {:keys [canonical_name subcategory_id link manufacturer_id unit] :as data}]
  (let [update-map (cond-> {}
                     canonical_name (assoc :canonical_name canonical_name
                                      :normalized_key (normalization/normalize-article-key canonical_name))

                     (contains? data :unit) (assoc :unit (normalize-unit unit))

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
  "Count total articles, with the same column filters as list-articles.

  JOINs are included so that category/subcategory/manufacturer ILIKE filters
  work identically to the list query. All JOINs are 1:1, so the count is correct.

  Accepts :extra-filters — a seq of HoneySQL WHERE clauses (e.g. date-range)."
  [db {:keys [search unit category-name subcategory-name manufacturer-display-name
              extra-filters]}]
  (let [base-query {:select [[[:count :*] :total]]
                    :from [[:articles :a]]
                    :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                                [:subcategories :s] [:= :s.id :a.subcategory_id]
                                [:categories :c] [:= :c.id :s.category_id]]}
        final-query (-> base-query
                      (apply-column-filters
                        {:search search
                         :unit unit
                         :category-name category-name
                         :subcategory-name subcategory-name
                         :manufacturer-display-name manufacturer-display-name})
                      (apply-extra-filters extra-filters))]
    {:total (or (:total (jdbc/execute-one! db (sql/format final-query)
                          {:builder-fn rs/as-unqualified-lower-maps}))
              0)}))


