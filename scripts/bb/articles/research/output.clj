(ns articles.research.output
  "Output generation: brand rule learning, article EDN building,
  and mapping entry generation."
  (:require
    [articles.db :as db]
    [articles.research.heuristics :as heuristics]
    [articles.research.validation :as validation]
    [clojure.string :as str]))

;; ============================================================
;; Brand rule learning
;; ============================================================

(defn learn-brand-rules!
  "Scan Perplexity-resolved items for new brand patterns and append to brand-rules.edn.
  Skips generic product words (from blocklist) and OCR artifact manufacturers.
  Returns count of new rules learned."
  [perplexity-results]
  (let [existing-patterns (set (map first heuristics/brand-rules))
        new-rules (atom [])]
    (doseq [{:keys [canonical-name manufacturer-name resolution]} perplexity-results
            :when (and (= resolution :perplexity)
                    manufacturer-name
                    (validation/sanitize-manufacturer manufacturer-name))]
      (let [first-word (first (str/split (str/trim (or canonical-name "")) #"\s+"))
            lower-word (str/lower-case (or first-word ""))
            pattern (str "(?i)^" (java.util.regex.Pattern/quote lower-word) "\\b")]
        (when (and (not (str/blank? first-word))
                (> (count first-word) 2)
                ;; Block generic product words from becoming brand patterns
                (not (contains? heuristics/generic-first-words lower-word))
                (not (contains? heuristics/generic-first-words (heuristics/strip-diacritics lower-word)))
                (not (contains? existing-patterns pattern))
                (not (some #(= (first %) pattern) @new-rules)))
          (swap! new-rules conj
            [pattern (validation/sanitize-manufacturer manufacturer-name)
             (:category-name (first (filter #(= (:canonical-name %) canonical-name) perplexity-results)))
             nil]))))
    (when (seq @new-rules)
      (let [current (or (heuristics/load-taxonomy "brand-rules.edn") [])
            updated (vec (concat current @new-rules))]
        (heuristics/save-taxonomy! "brand-rules.edn" updated)))
    (count @new-rules)))

;; ============================================================
;; Article & mapping output
;; ============================================================

(defn suggestion->article-edn
  [{:keys [canonical-name manufacturer-name category-name subcategory-name]}]
  (cond-> {:canonical-name canonical-name
           :category-name (or category-name "Ostalo")
           :subcategory-name (or subcategory-name "Opste")}
    manufacturer-name (assoc :manufacturer-name manufacturer-name)))

(defn suggestion->mapping-entries
  [{:keys [alias-ids canonical-name]}]
  (let [article-key (db/normalize-key canonical-name)]
    (mapv (fn [aid] {:alias-id (str aid) :article-key article-key})
      alias-ids)))
