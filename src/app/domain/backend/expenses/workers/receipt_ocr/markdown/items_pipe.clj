(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.items-pipe
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.discounts :as discounts]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]
    [clojure.string :as str]))

(defn parse [markdown]
  (when (string? markdown)
    (let [pipe-row->cells (fn [line]
                            (->> (str/split (or line "") #"\|")
                              (map str/trim)
                              (remove str/blank?)
                              vec))
          separator-row? (fn [cells]
                           (and (seq cells)
                             (every? #(re-matches #"(?i)^-+$" %) cells)))
          header-token? (fn [norm]
                          (boolean
                            (and norm
                              (re-find
                                #"^(?:naziv|name|opis|description|cijena|price|kol\.?|qty|quantity|ukupno|total|oznaka|poreza|pdv|vat|tax|назив|опис|цијена|кол\.?|укупно|пореза|пдв)$"
                                norm))))
          header-row? (fn [cells]
                        (let [norms (->> cells (map text/normalize-text) (remove nil?) vec)
                              hits (count (filter header-token? norms))]
                          (>= hits 2)))
          header->table-kind (fn [cells]
                               (let [norms (->> cells (map text/normalize-text) (remove nil?) vec)
                                     joined (str/join " " norms)]
                                 (cond
                                   (re-find #"(?:porez|порез|pdv|пдв|vat|tax)" joined) :tax
                                   (and (re-find #"(?:naziv|name|назив|опис|description|item)" joined)
                                     (re-find #"(?:ukupno|total|укупно)" joined)) :items
                                   :else nil)))
          header->mapping (fn [cells]
                            (let [norms (map text/normalize-text cells)
                                  find-idx (fn [re]
                                             (some (fn [[i n]] (when (and n (re-find re n)) i))
                                               (map-indexed vector norms)))
                                  label-idx (or (find-idx #"^(?:naziv|name|назив|опис|description|item)$") 0)
                                  qty-idx (find-idx #"^(?:kol\.?|qty|quantity|кол\.?)$")
                                  unit-idx (find-idx #"^(?:cijena|price|цијена)$")
                                  total-idx (find-idx #"^(?:ukupno|total|укупно)$")]
                              {:label-idx label-idx
                               :qty-idx (or qty-idx 1)
                               :unit-idx (or unit-idx 2)
                               :total-idx (or total-idx 3)}))
          row-looks-like-tax? (fn [cells]
                                (let [joined (->> cells (map text/normalize-text) (remove nil?) (str/join " "))]
                                  (boolean (re-find #"(?:porez|порез|pdv|пдв|vat|tax)" joined))))
          discount-label-pattern #"(?iu)\b(popust|popost|rabat|discount|akcija)\b"
          parse-item (fn [cells {:keys [label-idx qty-idx unit-idx total-idx]}]
                       (let [raw-label (text/normalize-item-label (nth cells label-idx nil))
                             qty (common/parse-money (nth cells qty-idx nil))
                             unit-price (common/parse-money (nth cells unit-idx nil))
                             line-total (common/parse-money (nth cells total-idx nil))]
                         (when (and (text/has-letter? raw-label) line-total)
                           {:raw_label raw-label
                            :qty qty
                            :unit_price unit-price
                            :line_total line-total})))
          discount-from-item (fn [{:keys [raw_label qty line_total]}]
                               (let [label (text/normalize-text raw_label)
                                     pct (common/parse-money qty)
                                     amount (common/parse-money line_total)]
                                 (when (and (seq label)
                                         (re-find discount-label-pattern label)
                                         pct
                                         amount
                                         (pos? (double (.abs (bigdec pct)))))
                                   {:pct (.abs (bigdec pct))
                                    :amount (bigdec amount)})))
          discount-from-cells (fn [cells]
                                (let [raw-label (text/normalize-item-label (nth cells 0 nil))
                                      label (text/normalize-text raw-label)
                                      pct (common/parse-money (nth cells 1 nil))
                                      amount (common/parse-money (peek cells))]
                                  (when (and (>= (count cells) 3)
                                          (seq label)
                                          (re-find discount-label-pattern label)
                                          pct
                                          amount
                                          (pos? (double (.abs (bigdec pct)))))
                                    {:pct (.abs (bigdec pct))
                                     :amount (bigdec amount)})))
          default-mapping {:label-idx 0 :qty-idx 1 :unit-idx 2 :total-idx 3}]
      (loop [remaining (str/split-lines markdown)
             pending-header nil
             table-kind nil
             mapping nil
             items []]
        (if-not (seq remaining)
          (vec items)
          (let [line (first remaining)
                cells (pipe-row->cells line)]
            (cond
              (empty? cells)
              (recur (rest remaining) nil nil nil items)

              (separator-row? cells)
              (let [kind (header->table-kind pending-header)
                    mapping* (when (= kind :items)
                               (header->mapping pending-header))
                    mapping (or mapping* mapping)
                    table-kind (or kind table-kind)]
                (recur (rest remaining) nil table-kind mapping items))

              (header-row? cells)
              (recur (rest remaining) cells table-kind mapping items)

              (and (>= (count cells) 4) (= table-kind :tax))
              (recur (rest remaining) nil table-kind mapping items)

              (and (>= (count cells) 3)
                (seq items)
                (discount-from-cells cells))
              (let [discount (discount-from-cells cells)]
                (recur (rest remaining)
                  nil
                  table-kind
                  mapping
                  (conj (pop items) (discounts/apply-discount-to-item (peek items) discount))))

              (>= (count cells) 4)
              (let [item (when-not (row-looks-like-tax? cells)
                           (parse-item cells (or mapping default-mapping)))
                    discount (some-> item discount-from-item)]
                (recur (rest remaining)
                  nil
                  table-kind
                  mapping
                  (cond
                    (and discount (seq items))
                    (conj (pop items) (discounts/apply-discount-to-item (peek items) discount))

                    item
                    (conj items item)

                    :else
                    items)))

              :else
              (recur (rest remaining) nil nil nil items))))))))
