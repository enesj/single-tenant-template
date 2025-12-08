(ns app.domain.expenses.services.article-aliases
  "Article alias management (admin)."
  (:require
    [app.domain.expenses.services.articles :as articles]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private allowed-order-by
  "Map kebab/underscore UI sort keys to fully-qualified columns."
  {:created-at             :aa/created_at
   :created_at             :aa/created_at
   :raw-label-normalized   :aa/raw_label_normalized
   :raw_label_normalized   :aa/raw_label_normalized
   :confidence             :aa/confidence
   :supplier-display-name  :s/display_name
   :supplier_display_name  :s/display_name
   :article-canonical-name :a/canonical_name
   :article_canonical_name :a/canonical_name})

(defn- sanitize-order-by [order-by]
  (get allowed-order-by order-by :aa/created_at))

(defn- sanitize-order-dir [order-dir]
  (if (= :asc order-dir) :asc :desc))

(defn list-aliases
  "List article aliases with optional filtering.
   opts: :limit, :offset, :supplier-id, :article-id, :search, :order-by, :order-dir"
  [db {:keys [limit offset supplier-id article-id search order-by order-dir]
       :or {limit 50 offset 0 order-by :created_at order-dir :desc}}]
  (let [where (cond-> [:and]
                supplier-id (conj [:= :aa/supplier_id supplier-id])
                article-id (conj [:= :aa/article_id article-id])
                search (conj [:ilike :aa/raw_label_normalized (str "%" search "%")]))
        base {:select [[:aa.*]
                       [:s/display_name :supplier_display_name]
                       [:a/canonical_name :article_canonical_name]]
              :from [[:article_aliases :aa]]
              :left-join [[:suppliers :s] [:= :s/id :aa/supplier_id]
                          [:articles :a] [:= :a/id :aa/article_id]]
              :order-by [[(sanitize-order-by order-by)
                          (sanitize-order-dir order-dir)]]
              :limit limit
              :offset offset}
        query (if (> (count where) 1)
                (assoc base :where where)
                base)]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-alias
  [db id]
  (jdbc/execute-one!
    db
    (sql/format {:select [[:aa.*]
                          [:s/display_name :supplier_display_name]
                          [:a/canonical_name :article_canonical_name]]
                 :from [[:article_aliases :aa]]
                 :left-join [[:suppliers :s] [:= :s/id :aa/supplier_id]
                             [:articles :a] [:= :a/id :aa/article_id]]
                 :where [:= :aa/id id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn create-alias!
  "Create or upsert an article alias."
  [db {:keys [supplier_id raw_label article_id confidence]}]
  (articles/create-alias! db supplier_id raw_label article_id {:confidence confidence}))

(defn update-alias!
  "Update article alias fields. Supports changing article_id, supplier_id, confidence,
   and raw_label (renormalizes). Returns updated row or nil if not found."
  [db id {:keys [supplier_id raw_label article_id confidence]}]
  (let [normalized (when raw_label (articles/normalize-alias-label raw_label))
        update-map (cond-> {}
                     supplier_id (assoc :supplier_id supplier_id)
                     article_id (assoc :article_id article_id)
                     confidence (assoc :confidence confidence)
                     normalized (assoc :raw_label_normalized normalized))]
    (when (seq update-map)
      (jdbc/execute-one!
        db
        (sql/format {:update :article_aliases
                     :set update-map
                     :where [:= :id id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-alias!
  "Delete alias by id. Returns true if a row was removed."
  [db id]
  (pos?
    (jdbc/execute-one!
      db
      (sql/format {:delete-from :article_aliases
                   :where [:= :id id]}))))
