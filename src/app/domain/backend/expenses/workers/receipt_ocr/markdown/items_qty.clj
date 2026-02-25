(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.items-qty
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.discounts :as discounts]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items-price :as items-price]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]
    [clojure.string :as str]))

(defn- line->qty-unit-total [line]
  (let [tokens (->> (str/split (str/trim (or line "")) #"\s+")
                 (remove str/blank?)
                 (remove #{"|" "¦" "│"})
                 vec)]
    (when (seq tokens)
      (or
        (some
          (fn [i]
            (let [t (get tokens i)
                  qty (when (and (string? t)
                              (re-matches #"(?i)^[0-9][0-9,\\.]*x$" t))
                        (common/parse-money t))]
              (when qty
                (let [unit-price (common/parse-money (get tokens (inc i)))
                      line-total (common/parse-money (get tokens (+ i 2)))]
                  (when unit-price
                    {:label-from-line (when (pos? i) (str/join " " (subvec tokens 0 i)))
                     :qty qty
                     :unit_price unit-price
                     :line_total line-total})))))
          (range (count tokens)))
        (some
          (fn [i]
            (let [t (get tokens i)
                  t0 (some-> t str/lower-case)
                  prev2 (when (>= i 2) (some-> (get tokens (- i 2)) str/lower-case))]
              (when (and (= "x" t0) (pos? i) (not= "x" prev2))
                (let [qty (common/parse-money (get tokens (dec i)))
                      unit-price (common/parse-money (get tokens (inc i)))
                      line-total-token (get tokens (+ i 2))
                      line-total (common/parse-money line-total-token)]
                  (when (and qty unit-price (or (nil? line-total-token) line-total))
                    {:label-from-line (when (pos? (dec i)) (str/join " " (subvec tokens 0 (dec i))))
                     :qty qty
                     :unit_price unit-price
                     :line_total line-total})))))
          (range (count tokens)))))))

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

              (items-price/ignore-item-line? norm)
              (recur (rest remaining) [] items)

              (and (seq items) (discounts/line->discount line))
              (let [discount (discounts/line->discount line)
                    last-item (peek items)
                    items (conj (pop items) (discounts/apply-discount-to-item last-item discount))]
                (recur (rest remaining) [] items))

              :else
              (if-let [{:keys [label-from-line qty unit_price line_total]} (line->qty-unit-total line)]
                (let [label-lines-raw (if-let [l (some-> label-from-line str/trim not-empty)]
                                        (if (text/has-letter? l) [l] pending)
                                        pending)
                      pending-total (->> label-lines-raw
                                      (keep (comp :money items-price/line->trailing-money))
                                      last)
                      label-lines (->> label-lines-raw
                                    (map (fn [l]
                                           (or (:prefix (items-price/line->trailing-money l)) l)))
                                    (remove str/blank?)
                                    vec)
                      label (->> label-lines
                              (map str/trim)
                              (remove str/blank?)
                              (str/join " ")
                              text/normalize-item-label)
                      line-total0 (or line_total pending-total)
                      unit-price0 (or unit_price
                                    (when (and qty line-total0 (pos? (.signum (bigdec qty))))
                                      (.divide (bigdec line-total0) (bigdec qty) 2 java.math.RoundingMode/HALF_UP)))
                      line-total1 (or line-total0
                                    (when (and qty unit-price0)
                                      (.setScale (* (bigdec qty) (bigdec unit-price0)) 2 java.math.RoundingMode/HALF_UP)))]
                  (if (and (text/has-letter? label) line-total1)
                    (recur (rest remaining)
                      []
                      (conj items {:raw_label label
                                   :qty qty
                                   :unit_price unit-price0
                                   :line_total line-total1}))
                    (recur (rest remaining) [] items)))
                (if-let [{:keys [prefix money]} (items-price/line->trailing-money line)]
                  (if (str/includes? line "|")
                    (let [pending (cond-> pending
                                    (text/has-letter? line) (conj line))
                          pending (if (> (count pending) 3)
                                    (subvec pending (- (count pending) 3))
                                    pending)]
                      (recur (rest remaining) pending items))
                    (let [label-lines-raw (cond-> pending
                                            (and prefix (text/has-letter? prefix) (not (text/unit-prefix? prefix)))
                                            (conj prefix))
                          label-lines (->> label-lines-raw
                                        (map (fn [l]
                                               (or (:prefix (items-price/line->trailing-money l)) l)))
                                        (remove str/blank?)
                                        vec)
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
                        (recur (rest remaining) [] items))))
                  (let [pending (cond-> pending
                                  (text/has-letter? line) (conj line))
                        pending (if (> (count pending) 3)
                                  (subvec pending (- (count pending) 3))
                                  pending)]
                    (recur (rest remaining) pending items)))))))))))
