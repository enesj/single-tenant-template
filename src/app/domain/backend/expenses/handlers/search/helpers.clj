(ns app.domain.backend.expenses.handlers.search.helpers
  "Shared helpers for cross-entity search: fuzzy matching, request parsing."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn pattern [term] (str "%" term "%"))

(def ^:const fuzzy-threshold
  "Minimum word_similarity score for fuzzy matching (0.0–1.0).
   0.5 balances typo tolerance against noise — ILIKE still catches exact substrings."
  0.5)

(defn fuzzy-text-where
  "Build a WHERE clause combining ILIKE substring match and pg_trgm fuzzy match.
   Matches if any column contains the term as a substring (ILIKE)
   OR has word_similarity above the threshold."
  [columns term pattern]
  (into [:or]
    (mapcat (fn [col]
              [[:ilike col pattern]
               [:> [:word_similarity term col] fuzzy-threshold]])
      columns)))

(defn relevance-score-expr
  "HoneySQL expression: best word_similarity across `columns` for `term`.
   Used in SELECT + ORDER BY to rank results by relevance."
  [term columns]
  (if (= 1 (count columns))
    [:word_similarity term (first columns)]
    (into [:greatest] (map #(vector :word_similarity term %) columns))))

(defn search-entity
  "Run `entity-fn` safely, returning [] on any exception."
  [entity-fn]
  (try (entity-fn) (catch Exception e (log/warn "Search entity failed" {:error (.getMessage e)}) [])))

(defn parse-search-limit
  [request default-limit]
  (let [raw (h/get-param (:query-params request) :limit)
        parsed (some-> raw str parse-long)]
    (-> (or parsed default-limit)
      long
      (max 1)
      (min 25))))

(defn parse-entity-type
  [request]
  (some-> (h/get-param (:query-params request) :type)
    str
    str/trim
    str/lower-case
    not-empty))

(defn parse-query
  [request]
  (some-> (h/get-param (:query-params request) :q)
    str
    str/trim
    not-empty))
