(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.reconcile
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.shape :as shape]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]))

(defn- code-only-label?
  [raw-label]
  (let [label (some-> raw-label str str/trim not-empty)]
    (boolean
      (and label
        (re-find #"\d" label)
        (not (re-find #"\s" label))
        (re-matches #"(?i)[A-Z0-9/_-]+" label)))))

(defn- descriptive-label?
  [raw-label]
  (let [label (some-> raw-label str str/trim not-empty)]
    (boolean
      (and label
        (re-find #"\p{L}" label)
        (not (code-only-label? label))))))

(defn- best-markdown-item-match
  [markdown-items item]
  (let [item-total (common/parse-money (:line_total item))
        item-qty (common/parse-money (:qty item))
        item-unit (common/parse-money (:unit_price item))]
    (when (and item-total (seq markdown-items))
      (->> markdown-items
        (map (fn [cand]
               (let [d-total (shape/abs-decimal-diff (:line_total cand) item-total)
                     d-unit (shape/abs-decimal-diff (:unit_price cand) item-unit)
                     d-qty (shape/abs-decimal-diff (:qty cand) item-qty)
                     score (+ (* 10 (or d-total 999.0))
                             (* 2 (or d-unit 1.0))
                             (* 1 (or d-qty 1.0)))]
                 {:cand cand
                  :d-total d-total
                  :score score})))
        (filter #(<= (double (or (:d-total %) 999.0)) 0.05))
        (sort-by :score)
        first
        :cand))))

(defn reconcile-extraction-with-markdown
  [extraction markdown-content]
  (if-not (and (map? extraction) (string? markdown-content) (sequential? (:items extraction)))
    {:extraction extraction
     :changed? false
     :changes []}
    (let [markdown-items (markdown/markdown->line-item-candidates markdown-content)
          {:keys [items changes]}
          (reduce
            (fn [acc item]
              (let [raw-label (:raw_label item)]
                (cond
                  (markdown/label-present-in-markdown? markdown-content raw-label)
                  (update acc :items conj item)

                  :else
                  (if-let [match (best-markdown-item-match markdown-items item)]
                    (if (and (descriptive-label? raw-label)
                          (code-only-label? (:raw_label match)))
                      (update acc :items conj item)
                      (-> acc
                        (update :items conj (merge item (select-keys match [:raw_label :qty :unit_price :line_total])))
                        (update :changes conj {:from raw-label
                                               :to (:raw_label match)
                                               :match :ocr-markdown})))
                    (update acc :items conj item)))))
            {:items [] :changes []}
            (:items extraction))
          changed? (boolean (seq changes))]
      {:extraction (assoc extraction :items items)
       :changed? changed?
       :changes changes})))
