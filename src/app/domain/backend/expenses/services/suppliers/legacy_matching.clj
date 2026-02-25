(ns app.domain.backend.expenses.services.suppliers.legacy-matching
  (:require
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private legal-suffix-tokens
  #{"doo" "dd" "ad" "llc" "ltd" "inc" "gmbh" "ag"})

(def ^:private descriptor-joiner-tokens
  #{"i" "and" "with"})

(defn- legacy-suffix-like-clauses
  []
  (->> legal-suffix-tokens
    (mapcat (fn [token]
              [[:like :normalized_key (str "%-" token "-%")]
               [:like :normalized_key (str "%-" token)]]))
    vec))

(defn find-by-canonical-key-with-legacy-suffix
  [db canonical-key]
  (when (and (seq canonical-key)
          (>= (count (str canonical-key)) 5))
    (let [suffix-clauses (legacy-suffix-like-clauses)]
      (when (seq suffix-clauses)
        (jdbc/execute-one!
          db
          (sql/format
            {:select [:*]
             :from [:suppliers]
             :where [:and
                     [:like :normalized_key (str canonical-key "-%")]
                     (into [:or] suffix-clauses)]
             :order-by [[:created_at :asc]]
             :limit 1})
          {:builder-fn rs/as-unqualified-lower-maps})))))

(def ^:private branch-suffix-head-tokens
  #{"pj" "podruznica" "poslovnica" "ogranak" "filijala"})

(def ^:private branch-suffix-display-re
  #"(?iu)\b(?:pj\.?\s*\d*|podružnica|podruznica|poslovnica|ogranak|filijala)\b.*$")

(defn strip-branch-suffix
  [display-name]
  (when-let [value (some-> display-name str str/trim not-empty)]
    (let [stripped (some-> value
                     (str/replace branch-suffix-display-re "")
                     (str/replace #"[\s,;:\.\-]+$" "")
                     str/trim
                     not-empty)]
      (or stripped value))))

(defn- normalized-key-tokens
  [normalized-key]
  (when-let [key* (some-> normalized-key str str/trim not-empty)]
    (->> (str/split key* #"-")
      (remove str/blank?)
      vec)))

(defn- location-or-branch-suffix?
  [prefix-key candidate-key]
  (let [prefix-tokens (normalized-key-tokens prefix-key)
        candidate-tokens (normalized-key-tokens candidate-key)
        prefix-count (count (or prefix-tokens []))
        suffix-tokens (vec (drop prefix-count (or candidate-tokens [])))]
    (boolean
      (and (seq prefix-tokens)
        (seq suffix-tokens)
        (= prefix-tokens (vec (take prefix-count candidate-tokens)))
        (or
          (contains? branch-suffix-head-tokens (first suffix-tokens))
          (and (>= prefix-count 2)
            (<= (count suffix-tokens) 3)
            (>= (count suffix-tokens) 2)
            (every? #(re-matches #"[a-z0-9]+" %) suffix-tokens)
            (every? #(or (re-matches #"\d+" %) (>= (count %) 3)) suffix-tokens)
            (not-any? legal-suffix-tokens suffix-tokens)))))))

(defn- descriptor-tail-suffix?
  [prefix-key candidate-key]
  (let [prefix-tokens (normalized-key-tokens prefix-key)
        candidate-tokens (normalized-key-tokens candidate-key)
        prefix-count (count (or prefix-tokens []))
        suffix-tokens (vec (drop prefix-count (or candidate-tokens [])))]
    (boolean
      (and (seq prefix-tokens)
        (>= prefix-count 3)
        (>= (count (or prefix-key "")) 16)
        (seq suffix-tokens)
        (= prefix-tokens (vec (take prefix-count candidate-tokens)))
        (contains? descriptor-joiner-tokens (first suffix-tokens))
        (>= (count suffix-tokens) 3)
        (not-any? legal-suffix-tokens suffix-tokens)
        (every? #(re-matches #"[a-z0-9]+" %) suffix-tokens)))))

(defn find-by-normalized-key-with-location-suffix
  [db normalized-key]
  (when (and (seq normalized-key) (str/includes? normalized-key "-"))
    (let [rows (jdbc/execute!
                 db
                 ["SELECT * FROM suppliers WHERE ? LIKE normalized_key || '-%' ORDER BY length(normalized_key) DESC, created_at ASC LIMIT 20"
                  normalized-key]
                 {:builder-fn rs/as-unqualified-lower-maps})]
      (some (fn [row]
              (let [prefix-key (some-> (:normalized_key row) str str/trim not-empty)]
                (when (and prefix-key
                        (location-or-branch-suffix? prefix-key normalized-key))
                  row)))
        rows))))

(defn find-by-normalized-key-with-descriptor-suffix
  [db normalized-key]
  (when (and (seq normalized-key) (str/includes? normalized-key "-"))
    (let [rows (jdbc/execute!
                 db
                 ["SELECT * FROM suppliers WHERE normalized_key LIKE ? ORDER BY length(normalized_key) ASC, created_at ASC LIMIT 25"
                  (str normalized-key "-%")]
                 {:builder-fn rs/as-unqualified-lower-maps})
          matches (->> rows
                    (filter (fn [row]
                              (let [candidate-key (some-> (:normalized_key row) str str/trim not-empty)]
                                (and candidate-key
                                  (descriptor-tail-suffix? normalized-key candidate-key)))))
                    vec)]
      (when (= 1 (count matches))
        (first matches)))))

(defn legacy-normalize-supplier-key-v0
  [name]
  (when-let [name* (some-> name str str/trim not-empty)]
    (-> name*
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" " ")
      str/trim
      (str/replace #" " "-"))))
