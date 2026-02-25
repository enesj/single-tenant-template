(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.totals
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]
    [clojure.string :as str]))

(defn total-amount
  "Best-effort parse receipt total from OCR markdown."
  [markdown]
  (when (string? markdown)
    (let [clean-line (fn [s] (text/strip-line-markup s))
          preferred-prefixes ["total" "ukupan iznos" "ukupna" "укупан износ"]
          fallback-prefixes ["ukupno" "укупно"]
          exclude-substrings ["bez porez" "без порез" "porez" "порез" "pdv" "пдв" "vat"]
          non-zero? (fn [money]
                      (and (some? money)
                        (not (zero? (.compareTo (bigdec money) 0M)))))
          pick-best (fn [rows]
                      (let [amounts (->> rows (map :amount) vec)]
                        (or (last (filter non-zero? amounts))
                          (last amounts))))
          candidates (->> (str/split-lines markdown)
                       (keep (fn [line0]
                               (let [line (clean-line line0)
                                     norm (text/normalize-text line)
                                     prefix-kind (cond
                                                   (and norm (some #(clojure.string/starts-with? norm %) preferred-prefixes)) :preferred
                                                   (and norm (some #(clojure.string/starts-with? norm %) fallback-prefixes)) :fallback
                                                   :else nil)
                                     amount (common/parse-money line)]
                                 (when (and prefix-kind
                                         amount
                                         (not (some #(clojure.string/includes? norm %) exclude-substrings)))
                                   {:kind prefix-kind
                                    :amount amount}))))
                       vec)
          preferred (->> candidates
                      (filter (fn [{:keys [kind]}] (= :preferred kind)))
                      vec)]
      (when (seq candidates)
        (pick-best (if (seq preferred) preferred candidates))))))

(def ^:private ba-datetime-re
  #"(\d{1,2})\.(\d{1,2})\.(\d{4})\.?\s+(\d{1,2}):(\d{2})")

(def ^:private ba-date-re
  #"(\d{1,2})\.(\d{1,2})\.(\d{4})\.?(?:\s|$)")

(defn purchased-at
  "Best-effort extract purchase datetime from receipt markdown."
  [markdown]
  (when (string? markdown)
    (or
      (when-let [[_ day month year hour minute] (re-find ba-datetime-re markdown)]
        (format "%s-%02d-%02dT%02d:%02d:00"
          year
          (parse-long month)
          (parse-long day)
          (parse-long hour)
          (parse-long minute)))
      (when-let [[_ day month year] (re-find ba-date-re markdown)]
        (format "%s-%02d-%02d"
          year
          (parse-long month)
          (parse-long day))))))
