(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.items-price
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.discounts :as discounts]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.header :as header]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]
    [clojure.string :as str]))

(def ^:private trailing-money-re
  #"(?i)^(.*?)(\d{1,9}[\.,]\d{2})\s*(?:[A-Z])?\s*(?:e|km|bam|€)?\s*$")

(defn line->trailing-money [line]
  (when (string? line)
    (let [line (-> line
                 str
                 (str/replace #"\|" " ")
                 (str/replace #"\s+" " ")
                 str/trim)]
      (when-let [[_ prefix amount _vat] (re-matches trailing-money-re line)]
        (let [money (common/parse-money amount)
              prefix (some-> prefix str/trim not-empty)]
          (when money
            {:prefix prefix
             :money money}))))))

(def ^:private item-ignore-prefixes
  (-> header/supplier-ignore-prefixes
    (conj "ibfm")
    (conj "ibem")
    (conj "tbfm")
    (conj "pdu")))

(def ^:private payment-summary-line-re
  #"^(?:pov(?:$|\s|:)|(?:cek|ček)(?:$|\s|:)|kortica(?:$|\s|:))")

(defn ignore-item-line? [norm]
  (boolean
    (or (nil? norm)
      (some #(str/starts-with? norm %) item-ignore-prefixes)
      (re-find payment-summary-line-re norm)
      (and (str/starts-with? norm "umla")
        (re-find #"(?:kortica|kartica)" norm)))))

(defn parse [markdown]
  (when (string? markdown)
    (let [lines (str/split-lines markdown)]
      (loop [remaining lines
             pending []
             items []]
        (if-not (seq remaining)
          (vec items)
          (let [line0 (first remaining)
                line (some-> line0 str str/trim not-empty)
                norm (text/normalize-text line)]
            (cond
              (nil? line)
              (recur (rest remaining) [] items)

              (ignore-item-line? norm)
              (recur (rest remaining) [] items)

              (and (seq items) (discounts/line->discount line))
              (let [discount (discounts/line->discount line)
                    last-item (peek items)
                    items (conj (pop items) (discounts/apply-discount-to-item last-item discount))]
                (recur (rest remaining) [] items))

              :else
              (if-let [{:keys [prefix money]} (line->trailing-money line)]
                (let [label-lines (cond-> pending
                                    (and prefix (text/has-letter? prefix) (not (text/unit-prefix? prefix)))
                                    (conj prefix))
                      label (->> label-lines
                              (map str/trim)
                              (remove str/blank?)
                              (str/join " ")
                              text/normalize-item-label)]
                  (if (and money (text/has-letter? label))
                    (recur (rest remaining)
                      []
                      (conj items {:raw_label label
                                   :qty 1M
                                   :unit_price money
                                   :line_total money}))
                    (recur (rest remaining) [] items)))
                (let [pending (cond-> pending
                                (text/has-letter? line) (conj line))
                      pending (if (> (count pending) 3)
                                (subvec pending (- (count pending) 3))
                                pending)]
                  (recur (rest remaining) pending items))))))))))
