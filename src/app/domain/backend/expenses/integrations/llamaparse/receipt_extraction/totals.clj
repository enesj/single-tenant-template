(ns app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.totals
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.text :as text]
    [clojure.string :as str]))

(def total-preferred-prefixes
  ["total" "ukupan iznos" "ukupna" "ukupan" "za uplatu" "za plac" "za pla" "uplaceno" "primljeno"])

(def total-fallback-prefixes
  ["ukupno" "укупно" "gotovina" "kartica" "saldo"])

(def total-exclude-substrings
  ["bez porez" "без порез" "porez" "порез" "pdv" "пдв" "vat" "tax"])

(defn line->total-candidate
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)
        kind (cond
               (and norm (some #(str/starts-with? norm %) total-preferred-prefixes)) :preferred
               (and norm (some #(str/starts-with? norm %) total-fallback-prefixes)) :fallback
               :else nil)
        amount (common/parse-money line)]
    (when (and kind amount norm
            (not (some #(str/includes? norm %) total-exclude-substrings)))
      {:kind kind :amount (bigdec amount)})))

(defn- pick-best-total
  [candidates]
  (let [non-zero? (fn [m]
                    (and (some? m)
                      (not (zero? (.compareTo (bigdec m) 0M)))))
        pick (fn [rows]
               (let [amounts (mapv :amount rows)]
                 (or (last (filter non-zero? amounts))
                   (last amounts))))
        preferred (filterv #(= :preferred (:kind %)) candidates)]
    (when (seq candidates)
      (pick (if (seq preferred) preferred candidates)))))

(defn extract-total
  [lines]
  (when (seq lines)
    (->> lines
      (keep line->total-candidate)
      vec
      pick-best-total)))

(defn items-total
  [items]
  (when (sequential? items)
    (let [amounts (->> items
                    (keep (fn [{:keys [line_total]}]
                            (common/parse-money line_total)))
                    (map bigdec)
                    vec)]
      (when (seq amounts)
        (reduce #(.add %1 %2) 0M amounts)))))
