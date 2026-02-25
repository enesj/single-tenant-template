(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.discounts
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str]))

(def ^:private discount-line-re
  #"(?i)^\s*-?\s*([0-9]{1,3}(?:[\.,][0-9]{1,2})?)\s*%\s*:?:?\s*(-?[0-9]{1,9}[\.,][0-9]{2})\s*(?:e|km|bam|€)?\s*$")

(defn line->discount [line]
  (when (string? line)
    (when-let [[_ pct amount] (re-matches discount-line-re (str/trim line))]
      (let [pct (common/parse-money pct)
            amount (common/parse-money amount)]
        (when (and pct amount (pos? (double pct)) (<= (double pct) 100.0))
          {:pct (.abs (bigdec pct))
           :amount (bigdec amount)})))))

(defn apply-discount-to-item
  [{:keys [line_total qty] :as item} {:keys [pct amount]}]
  (let [base-total (common/parse-money line_total)
        qty (common/parse-money qty)]
    (if-not (and base-total pct amount)
      item
      (let [base-total (bigdec base-total)
            pct (.abs (bigdec pct))
            amount (bigdec amount)
            amount-abs (.abs amount)
            expected-discount (* base-total (/ pct 100M))
            expected-final (.subtract base-total expected-discount)
            treat-as-final?
            (and (not (neg? (.signum amount)))
              (let [d-discount (double (.abs (.subtract amount expected-discount)))
                    d-final (double (.abs (.subtract amount expected-final)))]
                (<= d-final d-discount)))
            new-total (cond
                        (neg? (.signum amount)) (.subtract base-total amount-abs)
                        treat-as-final? amount
                        :else (.subtract base-total amount))]
        (if (neg? (.signum (bigdec new-total)))
          item
          (let [new-unit (when (and qty (pos? (.signum (bigdec qty))))
                           (.divide (bigdec new-total) (bigdec qty) 2 java.math.RoundingMode/HALF_UP))]
            (cond-> (assoc item :line_total (bigdec new-total))
              new-unit (assoc :unit_price new-unit))))))))
