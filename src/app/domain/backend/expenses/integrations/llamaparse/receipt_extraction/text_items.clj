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
    (->> (re-seq #"(?iu)(\d[\d,\.]*)x\b" label)
      (keep (fn [[_ num]]
              (when (re-find #"[\.,]" num)
                (when-let [q (common/parse-money num)]
                  {:qty q
                   :token (str num "x")}))))
      last)))

(defn- address-noise-line?
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (boolean
      (and line norm
        (or (some #(str/starts-with? norm %) merchant/address-prefixes)
          (re-matches #"(?iu)^\d{5}\s+\S.*$" line)
          (re-find #"(?iu)\b(?:br\.?|broj[a]?)\s*\d+[a-z]?\b" line))))))

(defn- continuation-label?
  [label]
  (let [label (some-> label table-items/normalize-item-label text/safe-trim)
        norm (text/normalize-text label)]
    (boolean
      (and label norm
        (or (not (str/includes? label " "))
          (re-find #"(?iu)^(?:x\d|\d+[x×]|\d+\s*[x×])" label)
          (re-find #"(?iu)\b(?:cm|mm|ml|gr|kg|xl|xxl)\b" norm))))))

(defn- line->text-item
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (when (and line norm
            (not (table-items/summary-label? norm))
            (not (re-matches text/ba-datetime-line-re line))
            (not (re-matches text/ba-date-line-re line))
            (not (address-noise-line? line))
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
        (not (address-noise-line? line))
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
                  lines)
          combine-labels
          (fn [pending-label inline-item]
            (let [pending (some-> pending-label table-items/normalize-item-label text/safe-trim)
                  raw-label (some-> (:raw_label inline-item) table-items/normalize-item-label text/safe-trim)]
              (cond
                (nil? pending)
                inline-item

                (nil? raw-label)
                (assoc inline-item :raw_label pending)

                (not (continuation-label? raw-label))
                inline-item

                (str/includes? (str/lower-case pending) (str/lower-case raw-label))
                (assoc inline-item :raw_label pending)

                (str/includes? (str/lower-case raw-label) (str/lower-case pending))
                inline-item

                :else
                (assoc inline-item :raw_label (str pending " " raw-label)))))]
      (loop [remaining lines
             pending-label nil
             items []]
        (if-not (seq remaining)
          items
          (let [line (first remaining)
                inline-item0 (line->text-item line)
                inline-item (when inline-item0
                              (combine-labels pending-label inline-item0))
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