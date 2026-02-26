(ns app.domain.backend.expenses.services.related-records
  "Shared helpers for related records queries across entities."
  (:require
    [clojure.string :as str]))

(defn clamp-related-limit
  "Clamp a pagination limit to the range [1, 500]."
  [limit]
  (-> (or limit 100)
    (max 1)
    (min 500)))

(defn normalize-related-type
  "Normalize a related type parameter to a canonical keyword.

  Also resolves common singular/alternate spellings:
  - :supplier / :suppliers / :provider  -> :providers
  - :manufacturer                        -> :manufacturers
  - :subcategory                         -> :subcategories

  Throws when related-type is not a keyword or string."
  [related-type]
  (let [raw (cond
              (keyword? related-type) (name related-type)
              (string? related-type) related-type
              :else (throw (ex-info "normalize-related-type expects a keyword or string"
                             {:related-type related-type
                              :type (type related-type)})))
        kw (some-> raw str/trim str/lower-case keyword)]
    (case kw
      :supplier :providers
      :suppliers :providers
      :provider :providers
      :manufacturer :manufacturers
      :subcategory :subcategories
      kw)))

(defn merge-related-rows
  "Merge ordered related-record result sets, de-duplicating by `:id` while
  preserving the order of higher-signal sources passed first."
  [limit & row-groups]
  (->> row-groups
    (apply concat)
    (reduce (fn [{:keys [seen rows] :as acc} row]
              (let [row-id (:id row)]
                (if (contains? seen row-id)
                  acc
                  {:seen (conj seen row-id)
                   :rows (conj rows row)})))
      {:seen #{}
       :rows []})
    :rows
    (take limit)
    vec))
