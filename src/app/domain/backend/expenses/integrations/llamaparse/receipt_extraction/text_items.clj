(ns app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.text-items
  (:require
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.merchant :as merchant]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.table-items :as table-items]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.text :as text]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.totals :as totals]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str])
  (:import
    [java.math RoundingMode]))

(defn- embedded-qty
  "Return {:qty .. :token ..} for embedded quantities like '0,198x' or '1,000x'."
  [label]
  (when (string? label)
    (->> (re-seq #"(?iu)(\d[\d,\.]*x)\b" label)
      (keep (fn [[_ token]]
              (let [num (some-> token (subs 0 (dec (count token))))]
                (when (and num (re-find #"[\.,]" num))
                  (when-let [q (common/parse-money token)]
                    {:qty q
                     :token token})))))
      last)))

(defn- line->text-item
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (when (and line norm
            (not (table-items/summary-label? norm))
            (not (re-matches text/ba-datetime-line-re line))
            (not (re-matches text/ba-date-line-re line))
            (not (merchant/address-like-line? line))
            (not (merchant/store-line? line))
            (not (merchant/separator-noise? line)))
      (when-let [[_ label price-str] (re-matches #"(?iu)^(.+?)\s+(\d{1,9}[\.,]\d{2})\s*(?:[A-Z])?\s*(?:e|km|bam|€)?$" line)]
        (let [label (table-items/normalize-item-label label)
              price (common/parse-money price-str)
              embedded (when label (embedded-qty label))
              label (cond-> label
                      embedded (str/replace #"(?iu)\s+\d[\d,\.]*x\s*$" "")
                      true text/safe-trim)
              qty (or (:qty embedded) 1M)
              unit-price (when (and qty price (pos? (.compareTo (bigdec qty) 0M)))
                           (.divide (bigdec price) (bigdec qty) 4 RoundingMode/HALF_UP))]
          (when (and label price (re-find #"\p{L}" label) (not (table-items/summary-label? (text/normalize-text label))))
            {:raw_label label
             :qty qty
             :unit_price unit-price
             :line_total price}))))))

(def money-only-line-re
  #"(?iu)^[^\p{L}\p{N}]*(-?\d{1,9}[\.,]\d{2})\s*(?:[A-Z])?\s*(?:e|km|bam|€)?[^\p{L}\p{N}]*$")

(defn- line->money-only
  [line]
  (when-let [line (text/safe-trim line)]
    (when-let [[_ amount] (re-matches money-only-line-re line)]
      (common/parse-money amount))))

(defn- text-item-label-line?
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (boolean
      (and line norm
        (re-find #"\p{L}" line)
        (not (table-items/summary-label? norm))
        (not (re-matches text/ba-datetime-line-re line))
        (not (re-matches text/ba-date-line-re line))
        (not (merchant/address-like-line? line))
        (not (merchant/store-line? line))
        (not (merchant/separator-noise? line))
        (nil? (totals/line->total-candidate line))))))

(defn parse-text-items
  [text-content]
  (when (string? text-content)
    (let [lines (->> (str/split-lines text-content)
                  (map text/safe-trim)
                  (remove nil?)
                  vec)
          date-idx (some (fn [[idx line]]
                           (when (or (re-matches text/ba-datetime-line-re line)
                                   (re-matches text/ba-date-line-re line))
                             idx))
                     (map-indexed vector lines))
          lines (if (some? date-idx)
                  (subvec lines (inc date-idx))
                  lines)]
      (loop [remaining lines
             pending-label nil
             items []]
        (if-not (seq remaining)
          items
          (let [line (first remaining)
                inline-item (line->text-item line)
                amount-only (line->money-only line)
                pending-label (some-> pending-label table-items/normalize-item-label text/safe-trim)]
            (cond
              inline-item
              (recur (rest remaining) nil (conj items inline-item))

              (and amount-only pending-label
                (re-find #"\p{L}" pending-label)
                (not (table-items/summary-label? (text/normalize-text pending-label))))
              (let [line-total (bigdec amount-only)
                    qty 1M
                    unit-price line-total]
                (recur (rest remaining)
                  nil
                  (conj items {:raw_label pending-label
                               :qty qty
                               :unit_price unit-price
                               :line_total line-total})))

              (text-item-label-line? line)
              (let [label (some-> line table-items/normalize-item-label text/safe-trim)
                    label-norm (text/normalize-text label)]
                (recur (rest remaining)
                  (if (and label (re-find #"\p{L}" label) (not (table-items/summary-label? label-norm)))
                    label
                    pending-label)
                  items))

              :else
              (recur (rest remaining) pending-label items))))))))
