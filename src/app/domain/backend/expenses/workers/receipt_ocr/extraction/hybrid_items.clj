(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.hybrid-items
  (:require
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.table-items :as llamaparse-table-items]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.items :as extraction-items]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.shape :as shape]
    [clojure.string :as str]))

(defn structured-response-items
  [raw-response]
  (when (map? raw-response)
    (let [table-items (llamaparse-table-items/response->table-items raw-response)
          {:keys [items]} (llamaparse-table-items/parse-table-items table-items)]
      (vec (or items [])))))

(defn- normalized-label
  [item]
  (extraction-items/normalize-item-label (:raw_label item)))

(defn- label-score
  [provider-item structured-item]
  (let [provider-label (normalized-label provider-item)
        structured-label (normalized-label structured-item)]
    (cond
      (and provider-label structured-label (= provider-label structured-label)) 0.0
      (and provider-label structured-label
        (or (str/includes? provider-label structured-label)
          (str/includes? structured-label provider-label))) 0.5
      :else nil)))

(defn- total-diff
  [provider-item structured-item]
  (shape/abs-decimal-diff (common/parse-money (:line_total provider-item))
    (common/parse-money (:line_total structured-item))))

(defn- item-consistent?
  [item]
  (let [qty (common/parse-money (:qty item))
        unit-price (common/parse-money (:unit_price item))
        line-total (common/parse-money (:line_total item))
        expected (when (and qty unit-price)
                   (.multiply ^java.math.BigDecimal (bigdec qty)
                     ^java.math.BigDecimal (bigdec unit-price)))
        diff (when (and expected line-total)
               (shape/abs-decimal-diff expected line-total))]
    (and qty
      unit-price
      line-total
      (pos? (.signum ^java.math.BigDecimal (bigdec qty)))
      (some? diff)
      (<= diff 0.01))))

(defn- fill-missing-fields
  [provider-item structured-item]
  (cond-> provider-item
    (and (nil? (:qty provider-item)) (:qty structured-item))
    (assoc :qty (:qty structured-item))

    (and (nil? (:unit_price provider-item)) (:unit_price structured-item))
    (assoc :unit_price (:unit_price structured-item))

    (and (nil? (:line_total provider-item)) (:line_total structured-item))
    (assoc :line_total (:line_total structured-item))

    (and (str/blank? (or (:raw_label provider-item) ""))
      (seq (:raw_label structured-item)))
    (assoc :raw_label (:raw_label structured-item))))

(defn- safe-to-overlay?
  [provider-item structured-item]
  (let [label-score (label-score provider-item structured-item)
        diff (total-diff provider-item structured-item)]
    (or (some? label-score)
      (and (some? diff) (<= diff 0.05)))))

(defn- match-score
  [provider-idx provider-item structured-idx structured-item]
  (let [label-score (label-score provider-item structured-item)
        diff (total-diff provider-item structured-item)]
    (when (safe-to-overlay? provider-item structured-item)
      (+ (* 0.1 (Math/abs (double (- provider-idx structured-idx))))
        (* 10.0 (double (or diff 0.05)))
        (double (or label-score 1.0))))))

(defn- best-structured-match
  [provider-idx provider-item structured-items used-indexes]
  (->> structured-items
    (map-indexed (fn [structured-idx structured-item]
                   (when-not (contains? used-indexes structured-idx)
                     {:index structured-idx
                      :item structured-item
                      :score (match-score provider-idx provider-item structured-idx structured-item)})))
    (remove nil?)
    (filter :score)
    (sort-by :score)
    first))

(defn- merge-item
  [provider-item structured-item]
  (let [provider-item (fill-missing-fields provider-item structured-item)
        provider-consistent? (item-consistent? provider-item)
        structured-consistent? (item-consistent? structured-item)
        repaired? (and (not provider-consistent?) structured-consistent?)
        merged-item (if repaired?
                      (-> provider-item
                        (assoc :qty (:qty structured-item))
                        (assoc :unit_price (:unit_price structured-item))
                        (assoc :line_total (or (:line_total structured-item) (:line_total provider-item)))
                        (cond-> (seq (:raw_label structured-item))
                          (assoc :raw_label (:raw_label structured-item))))
                      provider-item)]
    {:item merged-item
     :repaired? repaired?
     :filled? (not= provider-item merged-item)}))

(defn merge-structured-items
  [provider-items raw-response]
  (let [provider-items (vec (or provider-items []))
        structured-items (vec (or (structured-response-items raw-response) []))]
    (cond
      (empty? structured-items)
      {:items provider-items
       :meta nil}

      (empty? provider-items)
      {:items structured-items
       :meta {:structured-items (count structured-items)
              :matched-count 0
              :repaired-count 0
              :filled-count 0
              :used-structured-only? true}}

      :else
      (let [{:keys [items used-indexes repaired-count filled-count matched-count]}
            (reduce
              (fn [acc [provider-idx provider-item]]
                (if-let [{:keys [index item]} (best-structured-match provider-idx provider-item structured-items (:used-indexes acc))]
                  (let [{merged-item :item repaired? :repaired? filled? :filled?}
                        (merge-item provider-item item)]
                    (-> acc
                      (update :items conj merged-item)
                      (update :used-indexes conj index)
                      (update :matched-count inc)
                      (update :repaired-count (fnil + 0) (if repaired? 1 0))
                      (update :filled-count (fnil + 0) (if (and filled? (not repaired?)) 1 0))))
                  (update acc :items conj provider-item)))
              {:items []
               :used-indexes #{}
               :matched-count 0
               :repaired-count 0
               :filled-count 0}
              (map-indexed vector provider-items))
            items (vec items)
            meta {:structured-items (count structured-items)
                  :matched-count matched-count
                  :repaired-count repaired-count
                  :filled-count filled-count
                  :unused-structured-count (- (count structured-items) (count used-indexes))}]
        {:items items
         :meta (when (pos? matched-count) meta)}))))