(ns app.domain.backend.expenses.services.articles.aliases
  "Article alias lookup, creation, and batch upsert."
  (:require
    [app.domain.backend.expenses.services.articles.normalization :as normalization]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn find-article-by-alias
  "Find article by supplier-specific alias."
  [db supplier-id raw-label]
  (when (and supplier-id raw-label)
    (let [normalized (normalization/normalize-alias-label raw-label)
          query {:select [[:a.*] [:aa.raw_label_normalized]]
                 :from [[:article_aliases :aa]]
                 :join [[:articles :a] [:= :a.id :aa.article_id]]
                 :where [:and
                         [:= :aa.supplier_id supplier-id]
                         [:= :aa.raw_label_normalized normalized]]
                 :limit 1}]
      (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))))

(defn create-alias!
  "Create or update an article alias for a supplier."
  [db supplier-id raw-label article-id]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (normalization/normalize-alias-label raw-label*)
        row {:id (UUID/randomUUID)
             :supplier_id supplier-id
             :raw_label raw-label*
             :raw_label_normalized normalized
             :article_id article-id}
        sql-map {:insert-into :article_aliases
                 :values [row]
                 :on-conflict [:supplier_id :raw_label_normalized]
                 :do-update-set {:article_id article-id
                                 :raw_label :excluded/raw_label}
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(def ^:private min-alias-normalized-length
  "Minimum length of a normalized alias label to be considered valid.

  We keep this small but non-trivial to avoid adding garbage aliases like empty
  strings or a single dash.
  "
  2)

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
  [db supplier-id raw-label-normalized]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:article_aliases]
                 :where [:and
                         [:= :supplier_id supplier-id]
                         [:= :raw_label_normalized raw-label-normalized]]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- insert-alias!
  [db {:keys [supplier-id article-id raw-label raw-label-normalized]}]
  (let [row {:id (UUID/randomUUID)
             :supplier_id supplier-id
             :raw_label raw-label
             :raw_label_normalized raw-label-normalized
             :article_id article-id}]
    (jdbc/execute-one!
      db
      (sql/format {:insert-into :article_aliases
                   :values [row]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- update-alias-article!
  [db alias-id {:keys [article-id raw-label]}]
  (jdbc/execute-one!
    db
    (sql/format {:update :article_aliases
                 :set (cond-> {:article_id article-id}
                        (some? raw-label) (assoc :raw_label raw-label))
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn batch-create-aliases!
  "Batch-create aliases for a single supplier -> single article.

  Input:
  - supplier-id (UUID)
  - article-id (UUID)
  - raw-labels (seq of strings, or a newline-separated string)

  Options:
  - allow-reassign? (default false): when true, existing aliases that point to a
    different article will be updated to the new article.

  Returns:
  {:created [...]
   :skipped [...]
   :conflicts [...]
   :reassigned [...]}

  Notes:
  - Skips blanks/invalid labels.
  - Dedupes by normalized key.
  - Does NOT silently reassign conflicts unless allow-reassign? is true.
  "
  [db {:keys [supplier-id article-id raw-labels allow-reassign?]
       :or {allow-reassign? false}}]
  (when-not supplier-id
    (throw (ex-info "supplier-id is required" {:status 400})))
  (when-not article-id
    (throw (ex-info "article-id is required" {:status 400})))

  (jdbc/with-transaction [tx db]
    (let [inputs (raw-labels->seq raw-labels)
          normalized (map (fn [raw]
                            (let [raw* (when raw (str/trim raw))
                                  n (normalization/normalize-alias-label raw*)]
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
                     (let [existing (get-alias-by-normalized tx supplier-id raw-label-normalized)
                           acc* (update acc :seen conj raw-label-normalized)]
                       (cond
                         (nil? existing)
                         (let [inserted (insert-alias!
                                          tx
                                          {:supplier-id supplier-id
                                           :article-id article-id
                                           :raw-label raw-label
                                           :raw-label-normalized raw-label-normalized})]
                           (update acc* :created conj inserted))

                         (= (:article_id existing) article-id)
                         (do
                           (update-alias-article!
                             tx
                             (:id existing)
                             {:article-id article-id
                              :raw-label raw-label})
                           (update acc* :skipped conj {:raw-label raw-label
                                                       :raw-label-normalized raw-label-normalized
                                                       :reason :already-present
                                                       :alias-id (:id existing)}))

                         (false? allow-reassign?)
                         (update acc* :conflicts conj {:raw-label raw-label
                                                       :raw-label-normalized raw-label-normalized
                                                       :alias-id (:id existing)
                                                       :existing-article-id (:article_id existing)
                                                       :article-id article-id})

                         :else
                         (let [updated (update-alias-article!
                                         tx
                                         (:id existing)
                                         {:article-id article-id
                                          :raw-label raw-label})]
                           (update acc* :reassigned conj {:alias-id (:id updated)
                                                          :raw-label-normalized raw-label-normalized
                                                          :existing-article-id (:article_id existing)
                                                          :article-id article-id}))))))
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
