(ns app.domain.backend.expenses.integrations.llamaparse.receipt-extraction
  "LlamaParse-specific receipt extraction.

  LlamaParse returns structured `items` that include table `rows` and header text.
  We use those to build a provider-specific extraction map, rather than relying
  on markdown heuristics designed for the Mistral workflow."
  (:require
    [app.domain.backend.expenses.integrations.llamaparse.http :as http]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str])
  (:import
    [java.math RoundingMode]))

(defn- safe-trim
  [s]
  (some-> s str str/trim not-empty))

(defn- normalize-text
  [s]
  (some-> s safe-trim str/lower-case (str/replace #"\s+" " ") str/trim not-empty))

(def ^:private ba-datetime-line-re
  #"(?iu)^(\d{1,2})\.(\d{1,2})\.(\d{2,4})\.?\s+(\d{1,2}):(\d{2})(?::(\d{2}))?$")

(def ^:private ba-date-line-re
  #"(?iu)^(\d{1,2})\.(\d{1,2})\.(\d{2,4})\.?$")

(defn- normalize-year
  [year-str]
  (let [year-str (some-> year-str str/trim)]
    (cond
      (nil? year-str) nil
      (= 4 (count year-str)) year-str
      (= 3 (count year-str)) (str "2" year-str)
      (= 2 (count year-str)) (str "20" year-str)
      :else year-str)))

(defn- text->date-line
  [text]
  (when (string? text)
    (->> (str/split-lines text)
      (map safe-trim)
      (remove nil?)
      (filter (fn [line]
                (or (re-matches ba-datetime-line-re line)
                  (re-matches ba-date-line-re line))))
      first)))

(defn- date-line->iso
  [date-line]
  (when-let [date-line (safe-trim date-line)]
    (or
      (when-let [[_ day month year hour minute sec] (re-matches ba-datetime-line-re date-line)]
        (let [year (normalize-year year)
              sec (or sec "00")]
          (when (and year (re-matches #"\d{4}" year))
            (format "%s-%02d-%02dT%02d:%02d:%02d"
              year
              (parse-long month)
              (parse-long day)
              (parse-long hour)
              (parse-long minute)
              (parse-long sec)))))
      (when-let [[_ day month year] (re-matches ba-date-line-re date-line)]
        (let [year (normalize-year year)]
          (when (and year (re-matches #"\d{4}" year))
            (format "%s-%02d-%02d" year (parse-long month) (parse-long day))))))))

(def ^:private merchant-ignore-prefixes
  ["jib" "pib" "ibfm" "ibem" "tbfm" "bf" "fiskalni" "racun" "račun" "фискални" "рачун"
   "ve" "osn" "pdv" "vat" "total" "ukupno" "ukupna" "ukupan" "uplac" "upl" "gotovina" "kartica" "povrat"])

(def ^:private merchant-ignore-exact
  #{"fiskalni racun" "fiskalni račun" "racun" "račun" "фискални рачун" "рачун"})

(def ^:private item-like-leading-code-re
  #"(?iu)^(?:[A-Z]?\d{4,})(?:\s+|\b).*$")

(def ^:private item-like-money-re
  #"(?iu)\d{1,9}[\.,]\d{2}\s*(?:[A-Z])?\s*(?:e|km|bam|€)\b")

(def ^:private item-like-unit-re
  #"(?iu)(?:/\s*(?:kom|ko|pc)|\bkom/kom\b|\bt/pc\b)")

(def ^:private item-like-qty-re
  #"(?iu)\b\d+[\d,\.]*\s*x\b")

(defn- item-like-line?
  [line]
  (let [line (some-> line safe-trim)
        norm (normalize-text line)]
    (boolean
      (and line norm
        (or (re-find item-like-leading-code-re line)
          (re-find item-like-money-re line)
          (re-find item-like-unit-re norm)
          (re-find item-like-qty-re norm))))))

(def ^:private address-prefixes
  ["ul." "ul " "ulica" "trg" "bb" "br." "br " "broj" "street" "st."])

(defn- address-like-line?
  [line]
  (let [line (some-> line safe-trim)
        norm (normalize-text line)]
    (boolean
      (and line norm
        (or (some #(str/starts-with? norm %) address-prefixes)
          (re-matches #"(?iu)^\d{5}\s+\S.*$" line))))))

(defn- separator-noise?
  "Detect separator-like OCR noise lines (e.g. '====E=EE=5SE')."
  [line]
  (let [line (some-> line safe-trim)
        compact (some-> line (str/replace #"\s+" ""))]
    (boolean
      (and line compact
        (or
          (re-matches #"^-{3,}$" compact)
          (re-matches #"^[=-]{3,}$" compact)
          (re-matches #"^(?:-\s*){6,}$" line)
          (re-matches #"^(?:=\s*){6,}$" line)
          (let [letters (count (re-seq #"\p{L}" compact))
                digits (count (re-seq #"\p{N}" compact))
                alnum (+ letters digits)
                other (- (count compact) alnum)]
            (and (pos? (count compact))
              (<= letters 4)
              (>= other 5)
              (>= other (* 2 (max 1 alnum))))))))))

(def ^:private legal-suffix-re
  #"(?i)\b(?:d\s*\.?\s*o\s*\.?\s*o|d\s*\.?\s*d|a\s*\.?\s*d|s\s*\.?\s*p|j\s*\.?\s*p|u\s*\.?\s*o|llc|ltd|inc|gmbh|ag)\b\.?")

(defn- strip-legal-suffix
  [name]
  (when (string? name)
    (let [name (some-> name safe-trim)]
      (when name
        (let [m (re-matcher legal-suffix-re name)]
          (if (.find m)
            (let [idx (.start m)]
              (if (pos? (long idx))
                (some-> (subs name 0 (long idx))
                  (str/replace #"[\s,;:]+$" "")
                  safe-trim)
                name))
            name))))))

(defn- extract-quoted-name
  [s]
  (when (string? s)
    (when-let [[_ name] (re-find #"\"([^\"]+)\"" s)]
      (safe-trim name))))

(defn- merchant-candidate?
  [line]
  (let [line (some-> line safe-trim)
        norm (normalize-text line)]
    (boolean
      (and line norm
        (re-find #"\p{L}" line)
        (not (separator-noise? line))
        (not (re-matches ba-datetime-line-re line))
        (not (re-matches ba-date-line-re line))
        (not (address-like-line? line))
        (not (item-like-line? line))
        (not (contains? merchant-ignore-exact norm))
        (not (some #(str/starts-with? norm %) merchant-ignore-prefixes))))))

(def ^:private store-line-prefixes
  ["podruznica" "podružnica" "poslovnica" "filijala" "prodavnica" "radnja" "maloprodaja" "maloprodajna"])

(def ^:private store-line-substrings
  ["trzni centar" "tržni centar" "city center" "shopping center"])

(defn- store-line?
  [line]
  (let [norm (normalize-text line)]
    (boolean
      (and norm
        (or (some #(str/starts-with? norm %) store-line-prefixes)
          (some #(str/includes? norm %) store-line-substrings))))))

(defn- merchant-score
  [line]
  (let [line (some-> line safe-trim)
        compact (some-> line (str/replace #"\s+" ""))
        stripped (some-> line (str/replace #"^[^\p{L}\p{N}]+" "") safe-trim)
        letters (count (re-seq #"\p{L}" (or compact "")))
        digits (count (re-seq #"\p{N}" (or compact "")))
        other (- (count (or compact "")) (+ letters digits))
        words (count (remove str/blank? (str/split (or line "") #"\s+")))
        starts-letter? (boolean (and stripped (re-find #"^\p{L}" stripped)))
        has-legal? (boolean (and line (re-find legal-suffix-re line)))
        has-quotes? (boolean (and line (re-find #"\".+\"" line)))
        store-penalty (if (store-line? line) 25 0)]
    (+ (* 2 letters)
      (min 10 words)
      (if starts-letter? 6 0)
      (if has-legal? 10 0)
      (if has-quotes? 6 0)
      (- other)
      (- store-penalty)
      (- (long (/ digits 3))))))

(defn- text->merchant-name
  [text]
  (when (string? text)
    (let [stop-line?
          (fn [line]
            (let [norm (normalize-text line)]
              (or (re-matches ba-datetime-line-re line)
                (re-matches ba-date-line-re line)
                (and norm
                  (or (str/starts-with? norm "bf")
                    (str/starts-with? norm "tbfm"))))))
          lines (->> (str/split-lines text)
                  (map safe-trim)
                  (remove nil?)
                  (remove separator-noise?)
                  (take 80)
                  (take-while (complement stop-line?))
                  vec)
          candidates (->> lines
                       (map-indexed vector)
                       (filter (fn [[_idx line]]
                                 (merchant-candidate? line)))
                       vec)
          quoted (->> candidates
                   (keep (fn [[idx line]]
                           (when-let [q (extract-quoted-name line)]
                             [idx q])))
                   vec)
          legal (->> candidates
                  (filter (fn [[_idx line]]
                            (and (string? line)
                              (re-find legal-suffix-re line))))
                  vec)
          best
          (cond
            (seq quoted)
            (second (apply min-key first quoted))

            (seq legal)
            (second (apply min-key first legal))

            (seq candidates)
            (->> candidates
              (sort-by (fn [[idx line]]
                         [(+ (merchant-score line)
                            (max 0 (- 20 idx)))
                          (- idx)]))
              last
              second)

            :else
            nil)
          best (or (strip-legal-suffix best) best)
          best (some-> best
                 (str/replace #"(?is)<br\s*/?>" " ")
                 (str/replace #"(?is)<[^>]+>" " ")
                 (str/replace #"[_]+" " ")
                 (str/replace #"\s+" " ")
                 (str/replace #"[\s,;:]+$" "")
                 safe-trim)]
      (when (and best (re-find #"\p{L}" best))
        best))))

(def ^:private total-preferred-prefixes
  ["total" "ukupan iznos" "ukupna" "ukupan" "za uplatu" "za plac" "za pla" "uplaceno" "primljeno"])

(def ^:private total-fallback-prefixes
  ["ukupno" "укупно" "gotovina" "kartica" "saldo"])

(def ^:private total-exclude-substrings
  ["bez porez" "без порез" "porez" "порез" "pdv" "пдв" "vat" "tax"])

(defn- line->total-candidate
  [line]
  (let [line (some-> line safe-trim)
        norm (normalize-text line)
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

(defn- extract-total
  [lines]
  (when (seq lines)
    (->> lines
      (keep line->total-candidate)
      vec
      pick-best-total)))

(defn- response->pages
  [resp-json]
  (let [pages (get-in resp-json [:items :pages])]
    (if (sequential? pages) pages [])))

(defn- flatten-items
  [items]
  (letfn [(walk [item]
            (lazy-seq
              (cons item
                (when (sequential? (:items item))
                  (mapcat walk (:items item))))))]
    (->> (or items [])
      (mapcat walk)
      (remove nil?)
      vec)))

(defn- response->all-items
  [resp-json]
  (->> (response->pages resp-json)
    (mapcat :items)
    flatten-items
    vec))

(defn response->header-text
  [resp-json]
  (let [items (response->all-items resp-json)]
    (->> items
      (filter (fn [{:keys [type]}]
                (= "header" (some-> type str/lower-case))))
      (keep (fn [item]
              (or (safe-trim (:md item))
                (safe-trim (:value item)))))
      (remove str/blank?)
      (str/join "\n\n")
      not-empty)))

(defn response->combined-text
  [resp-json]
  (let [header (response->header-text resp-json)
        text (http/response->text resp-json)]
    (->> [header text]
      (map safe-trim)
      (remove nil?)
      (str/join "\n\n")
      not-empty)))

(def ^:private unit-prefixes
  #{"t/pc"})

(defn- unit-prefix-row?
  [cells]
  (let [label (normalize-text (first cells))
        rest* (rest cells)]
    (boolean
      (and label
        (contains? unit-prefixes label)
        (every? (comp nil? safe-trim) rest*)))))

(def ^:private summary-prefixes
  ["ve" "osn" "pdv" "vat" "tax" "jib" "pib" "ibfm" "bf" "uplac" "upl" "gotovina" "kartica" "povrat" "ukupno" "total"
   "popust" "pupust" "rabat" "discount" "akcija" "snizenje" "sniženje"])

(defn- summary-label?
  [label]
  (let [label (normalize-text label)
        label* (some-> label (str/replace #"^[^\p{L}\p{N}]+" "") safe-trim)]
    (boolean
      (and label*
        (some (fn [prefix]
                (cond
                  (= prefix "ve")
                  (boolean (re-find #"(?iu)^ve\b" label*))

                  :else
                  (str/starts-with? label* prefix)))
          summary-prefixes)))))

(defn- normalize-item-label
  [raw]
  (let [raw (some-> raw
              str
              (str/replace #"(?is)<br\s*/?>" " ")
              (str/replace #"(?is)<[^>]+>" " ")
              (str/replace #"[_]+" " ")
              (str/replace #"\s+" " ")
              (str/replace #"\|" " ")
              str/trim
              not-empty)
        raw (when raw
              (if-let [[_ _ rest]
                       (re-matches #"(?i)^([0-9]{4,}|[A-Z][0-9]{4,})[ \t]+(.+)$" raw)]
                (let [rest (str/trim rest)]
                  (if (re-find #"\p{L}" rest)
                    rest
                    raw))
                raw))]
    raw))

(defn- header-token?
  [norm]
  (boolean
    (and norm
      (re-find
        #"^(?:label|naziv|name|opis|description|cijena|price|kol\.?|qty|quantity|ukupno|total|oznaka|pdv|vat|tax|назив|опис|цијена|кол\.?|укупно|пдв)$"
        norm))))

(defn- header-row?
  [cells]
  (let [norms (->> cells (map normalize-text) (remove nil?) vec)
        hits (count (filter header-token? norms))]
    (>= hits 2)))

(defn- header->mapping
  [cells]
  (let [norms (map normalize-text cells)
        find-idx (fn [re]
                   (some (fn [[i n]] (when (and n (re-find re n)) i))
                     (map-indexed vector norms)))
        label-idx (or (find-idx #"^(?:label|naziv|name|oznaka|назив|opis|опис|description|item)$") 0)
        qty-idx (find-idx #"^(?:kol\.?|qty|quantity|кол\.?|kol)$")
        unit-idx (find-idx #"^(?:cijena|price|цијена)$")
        total-idx (find-idx #"^(?:ukupno|total|укупно)$")]
    {:label-idx label-idx
     :qty-idx qty-idx
     :unit-idx unit-idx
     :total-idx total-idx}))

(defn- abs-decimal-diff
  [a b]
  (when (and a b)
    (double (.abs (.subtract (bigdec a) (bigdec b))))))

(defn- apply-discount
  [{:keys [line_total qty] :as item} {:keys [pct amount]}]
  (let [base (common/parse-money line_total)
        pct* (common/parse-money pct)
        amount* (common/parse-money amount)]
    (cond
      (not (and base amount*))
      item

      pct*
      (let [base (bigdec base)
            pct (-> pct* bigdec .abs)
            amount (-> amount* bigdec .abs)
            expected-discount (* base (/ pct 100M))
            expected-final (.subtract base expected-discount)
            treat-as-discount?
            (or (<= (or (abs-decimal-diff amount expected-discount) 1e9) 0.05)
              (> (or (abs-decimal-diff amount expected-final) 1e9) 0.05))
            new-total (if treat-as-discount?
                        (.subtract base amount)
                        amount)
            qty* (common/parse-money qty)
            unit (when (and qty* (pos? (.compareTo (bigdec qty*) 0M)))
                   (.divide new-total (bigdec qty*) 4 RoundingMode/HALF_UP))]
        (cond-> item
          true (assoc :line_total new-total)
          unit (assoc :unit_price unit)))

      (neg? (bigdec amount*))
      (let [base (bigdec base)
            discount (-> amount* bigdec .abs)
            new-total (.subtract base discount)
            qty* (common/parse-money qty)
            unit (when (and qty* (pos? (.compareTo (bigdec qty*) 0M)))
                   (.divide new-total (bigdec qty*) 4 RoundingMode/HALF_UP))]
        (cond-> item
          true (assoc :line_total new-total)
          unit (assoc :unit_price unit)))

      :else
      item)))

(defn- discount-row
  [cells]
  (let [cells* (->> cells (map safe-trim) (remove nil?) vec)
        norms (map normalize-text cells*)
        discount-label?
        (boolean
          (some (fn [n]
                  (and n
                    (re-find #"(?iu)\b(?:popust|pupust|rabat|discount|akcija|snizenje|sniženje)\b" n)))
            norms))
        pct-token?
        (fn [s]
          (let [s (some-> s safe-trim)]
            (boolean
              (and s
                (re-matches #"(?iu)^[^\p{L}]*-?\d[\d,\.]*\s*%:?\s*$" s)))))
        pct-cell (some (fn [c]
                         (when (and (string? c) (pct-token? c))
                           c))
                   cells*)
        pct (when pct-cell (common/parse-money pct-cell))
        amount-cell (some (fn [c]
                            (when (common/parse-money c)
                              c))
                      (->> cells*
                        reverse
                        (remove #(= % pct-cell))))
        amount (when amount-cell (common/parse-money amount-cell))
        label-norm (first norms)
        ignore-summary? (and label-norm (summary-label? label-norm) (not discount-label?))]
    (cond
      ignore-summary?
      nil

      (and pct amount (or discount-label? (= pct-cell (first cells*))))
      {:pct pct
       :amount amount}

      (and amount discount-label?)
      {:pct nil
       :amount amount}

      :else
      nil)))

(defn- qty-row
  [cells]
  (let [cells* (->> cells (map safe-trim) (remove nil?) vec)
        token0 (first cells*)
        token1 (second cells*)
        token2 (nth cells* 2 nil)]
    (when (and (string? token0) (re-matches #"(?i)^[0-9][0-9,\.]*x$" token0))
      {:qty (common/parse-money token0)
       :unit_price (common/parse-money token1)
       :line_total (common/parse-money token2)})))

(defn- apply-qty-row
  [{:keys [line_total] :as item} q]
  (let [qty* (common/parse-money (:qty q))
        unit-price* (common/parse-money (:unit_price q))
        q-total (common/parse-money (:line_total q))
        item-total (common/parse-money line_total)
        line-total (or q-total item-total)
        unit-price (or unit-price*
                     (when (and qty* line-total (pos? (.compareTo (bigdec qty*) 0M)))
                       (.divide (bigdec line-total) (bigdec qty*) 4 RoundingMode/HALF_UP)))]
    (cond-> item
      qty* (assoc :qty qty*)
      line-total (assoc :line_total line-total)
      unit-price (assoc :unit_price unit-price))))

(defn- embedded-qty
  "Return {:qty .. :token ..} for embedded quantities like '0,198x' or '1,000x'.

  We only treat tokens as quantities when the numeric part contains a decimal
  separator (comma/dot), to avoid matching dimension patterns like '2X9X60'."
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

(defn- parse-item-row
  [cells mapping]
  (let [{:keys [label-idx qty-idx unit-idx total-idx]} mapping
        label0 (nth cells label-idx nil)
        label (normalize-item-label label0)
        embedded (when (and (nil? qty-idx) label)
                   (embedded-qty label))
        label (cond-> label
                embedded (str/replace #"(?iu)\s+\d[\d,\.]*x\s*$" "")
                true safe-trim)
        label-norm (normalize-text label)
        qty0 (when qty-idx (common/parse-money (nth cells qty-idx nil)))
        qty (or qty0 (:qty embedded) 1M)
        unit-price0 (when unit-idx (common/parse-money (nth cells unit-idx nil)))
        line-total (or (when total-idx (common/parse-money (nth cells total-idx nil)))
                     (common/parse-money (last cells)))
        unit-price (or unit-price0
                     (when (and qty line-total (pos? (.compareTo (bigdec qty) 0M)))
                       (.divide (bigdec line-total) (bigdec qty) 4 RoundingMode/HALF_UP)))]
    (when (and label (re-find #"\p{L}" label) (not (summary-label? label-norm)) line-total)
      {:raw_label label
       :qty qty
       :unit_price unit-price
       :line_total line-total})))

(defn- table-item->rows
  [{:keys [rows md]}]
  (cond
    (sequential? rows) rows

    (and (string? md) (str/includes? md "|"))
    (->> (str/split-lines md)
      (keep (fn [line]
              (when (and (string? line) (str/starts-with? (str/trim line) "|"))
                (->> (str/split line #"\|")
                  (mapv (fn [c] (or (safe-trim c) "")))))))
      vec)

    :else
    nil))

(defn- response->table-items
  [resp-json]
  (->> (response->all-items resp-json)
    (filter (fn [{:keys [type]}]
              (= "table" (some-> type str/lower-case))))
    vec))

(defn- combine-label-lines
  [a b]
  (let [a (safe-trim a)
        b (safe-trim b)]
    (cond
      (nil? a) b
      (nil? b) a
      :else
      (let [a* (str/lower-case a)
            b* (str/lower-case b)]
        (cond
          (str/includes? a* b*) a
          (str/includes? b* a*) b
          :else (str a " " b))))))

(defn- parse-table-items
  [table-items]
  (reduce
    (fn [acc table]
      (let [rows (table-item->rows table)]
        (if-not (sequential? rows)
          acc
          (loop [remaining rows
                 mapping {:label-idx 0 :qty-idx nil :unit-idx nil :total-idx 1}
                 pending-label nil
                 items (:items acc)
                 total-lines (:total-lines acc)]
            (if-not (seq remaining)
              (assoc acc :items items :total-lines total-lines)
              (let [row (first remaining)
                    cells (if (sequential? row) (mapv #(or (some-> % str/trim) "") row) [])
                    as-line (when (seq cells) (str/join " " (remove str/blank? cells)))
                    total-lines (cond-> total-lines
                                  (line->total-candidate as-line) (conj as-line))
                    header? (header-row? cells)
                    unit? (unit-prefix-row? cells)
                    discount (when-not (or header? unit?) (discount-row cells))
                    q (when-not (or header? unit? discount) (qty-row cells))
                    mapping
                    (let [n (count cells)
                          label (normalize-item-label (nth cells (:label-idx mapping) nil))
                          label-norm (normalize-text label)]
                      (if (and (not header?)
                            (not unit?)
                            (nil? discount)
                            (nil? q)
                            (nil? (:unit-idx mapping))
                            (= 1 (:total-idx mapping))
                            (>= n 3)
                            (re-find #"\p{L}" (or label ""))
                            (common/parse-money (nth cells 1 nil))
                            (common/parse-money (last cells))
                            (not (summary-label? label-norm)))
                        (assoc mapping :unit-idx 1 :total-idx (dec n))
                        mapping))]
                (cond
                  (empty? cells)
                  (recur (rest remaining) mapping pending-label items total-lines)

                  header?
                  (recur (rest remaining) (merge mapping (header->mapping cells)) nil items total-lines)

                  unit?
                  (recur (rest remaining) mapping pending-label items total-lines)

                  discount
                  (if (seq items)
                    (let [last-item (peek items)
                          items (conj (pop items) (apply-discount last-item discount))]
                      (recur (rest remaining) mapping pending-label items total-lines))
                    (recur (rest remaining) mapping pending-label items total-lines))

                  q
                  (cond
                    pending-label
                    (let [qty* (or (:qty q) 1M)
                          unit-price* (:unit_price q)
                          q-total (:line_total q)
                          line-total (or q-total
                                       (when (and qty* unit-price*)
                                         (.multiply (bigdec qty*) (bigdec unit-price*))))
                          unit-price (or unit-price*
                                       (when (and qty* line-total (pos? (.compareTo (bigdec qty*) 0M)))
                                         (.divide (bigdec line-total) (bigdec qty*) 4 RoundingMode/HALF_UP)))
                          item (when (and pending-label line-total)
                                 {:raw_label pending-label
                                  :qty qty*
                                  :unit_price unit-price
                                  :line_total line-total})
                          items (cond-> items item (conj item))]
                      (recur (rest remaining) mapping nil items total-lines))

                    (seq items)
                    (let [last-item (peek items)
                          items (conj (pop items) (apply-qty-row last-item q))]
                      (recur (rest remaining) mapping nil items total-lines))

                    :else
                    (recur (rest remaining) mapping nil items total-lines))

                  :else
                  (let [item0 (parse-item-row cells mapping)
                        item (when item0
                               (if pending-label
                                 (update item0 :raw_label (fn [raw]
                                                            (combine-label-lines pending-label raw)))
                                 item0))
                        items (cond-> items item (conj item))
                        pending-label
                        (cond
                          item
                          nil

                          :else
                          (let [label (normalize-item-label (nth cells (:label-idx mapping) nil))
                                label-norm (normalize-text label)]
                            (cond
                              (and label (re-find #"\p{L}" label) (not (summary-label? label-norm)))
                              label

                              :else
                              pending-label)))]
                    (recur (rest remaining) mapping pending-label items total-lines)))))))))
    {:items [] :total-lines []}
    (or table-items [])))

(defn- items-total
  [items]
  (when (sequential? items)
    (let [amounts (->> items
                    (keep (fn [{:keys [line_total]}]
                            (common/parse-money line_total)))
                    (map bigdec)
                    vec)]
      (when (seq amounts)
        (reduce #(.add %1 %2) 0M amounts)))))

(defn- line->text-item
  [line]
  (let [line (some-> line safe-trim)
        norm (normalize-text line)]
    (when (and line norm
            (not (summary-label? norm))
            (not (re-matches ba-datetime-line-re line))
            (not (re-matches ba-date-line-re line))
            (not (address-like-line? line))
            (not (store-line? line))
            (not (separator-noise? line)))
      (when-let [[_ label price-str] (re-matches #"(?iu)^(.+?)\s+(\d{1,9}[\.,]\d{2})\s*(?:[A-Z])?\s*(?:e|km|bam|€)?$" line)]
        (let [label (normalize-item-label label)
              price (common/parse-money price-str)
              embedded (when label (embedded-qty label))
              label (cond-> label
                      embedded (str/replace #"(?iu)\s+\d[\d,\.]*x\s*$" "")
                      true safe-trim)
              qty (or (:qty embedded) 1M)
              unit-price (when (and qty price (pos? (.compareTo (bigdec qty) 0M)))
                           (.divide (bigdec price) (bigdec qty) 4 RoundingMode/HALF_UP))]
          (when (and label price (re-find #"\p{L}" label) (not (summary-label? (normalize-text label))))
            {:raw_label label
             :qty qty
             :unit_price unit-price
             :line_total price}))))))

(def ^:private money-only-line-re
  #"(?iu)^[^\p{L}\p{N}]*(-?\d{1,9}[\.,]\d{2})\s*(?:[A-Z])?\s*(?:e|km|bam|€)?[^\p{L}\p{N}]*$")

(defn- line->money-only
  [line]
  (when-let [line (safe-trim line)]
    (when-let [[_ amount] (re-matches money-only-line-re line)]
      (common/parse-money amount))))

(defn- text-item-label-line?
  [line]
  (let [line (some-> line safe-trim)
        norm (normalize-text line)]
    (boolean
      (and line norm
        (re-find #"\p{L}" line)
        (not (summary-label? norm))
        (not (re-matches ba-datetime-line-re line))
        (not (re-matches ba-date-line-re line))
        (not (address-like-line? line))
        (not (store-line? line))
        (not (separator-noise? line))
        (nil? (line->total-candidate line))))))

(defn- parse-text-items
  [text]
  (when (string? text)
    (let [lines (->> (str/split-lines text)
                  (map safe-trim)
                  (remove nil?)
                  vec)
          date-idx (some (fn [[idx line]]
                           (when (or (re-matches ba-datetime-line-re line)
                                   (re-matches ba-date-line-re line))
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
                pending-label (some-> pending-label normalize-item-label safe-trim)]
            (cond
              inline-item
              (recur (rest remaining) nil (conj items inline-item))

              (and amount-only pending-label
                (re-find #"\p{L}" pending-label)
                (not (summary-label? (normalize-text pending-label))))
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
              (let [label (some-> line normalize-item-label safe-trim)
                    label-norm (normalize-text label)]
                (recur (rest remaining)
                  (if (and label (re-find #"\p{L}" label) (not (summary-label? label-norm)))
                    label
                    pending-label)
                  items))

              :else
              (recur (rest remaining) pending-label items))))))))

(defn response->extraction
  "Build a ReceiptExtraction map from a LlamaParse result response JSON."
  [resp-json]
  (let [header (response->header-text resp-json)
        text (http/response->text resp-json)
        combined (response->combined-text resp-json)
        date-line (text->date-line combined)
        purchased-at (date-line->iso date-line)
        merchant (or (text->merchant-name header)
                   (text->merchant-name text))
        table-items (response->table-items resp-json)
        {:keys [items total-lines]} (parse-table-items table-items)
        items (if (empty? items)
                (or (parse-text-items combined) [])
                items)
        total (or (extract-total (concat (when combined (str/split-lines combined)) total-lines))
                (items-total items))]
    {:merchant (when merchant {:name merchant})
     :purchased_at purchased-at
     :currency nil
     :totals (when total {:total total})
     :items (vec (or items []))}))

(defn extraction->markdown
  "Build a stable, receipt-like markdown representation from an extraction.

  `date-line` is the original receipt date line (e.g. '2.12.2025. 19:46')."
  [{:keys [merchant totals items]} {:keys [date-line]}]
  (let [merchant-name (some-> merchant :name safe-trim)
        date-line (safe-trim date-line)
        total (some-> totals :total common/parse-money)
        format-money (fn [m]
                       (when-let [m (common/parse-money m)]
                         (format "%.2f" (double (bigdec m)))))
        fmt-row (fn [row]
                  (str "| " (str/join " | " row) " |"))
        rows (->> (or items [])
               (mapv (fn [{:keys [raw_label qty unit_price line_total]}]
                       [(or (safe-trim raw_label) "")
                        (or (some-> qty common/parse-money str) "")
                        (or (format-money unit_price) "")
                        (or (format-money line_total) "")])))
        table (when (seq rows)
                (let [header ["Label" "Qty" "Unit" "Total"]
                      sep ["---" "---" "---" "---"]]
                  (str/join "\n" (concat [(fmt-row header)
                                          (fmt-row sep)]
                                   (map fmt-row rows)))))
        blocks (cond-> []
                 merchant-name (conj merchant-name)
                 date-line (conj date-line)
                 table (conj table)
                 total (conj (str "TOTAL: " (format "%.2f" (double (bigdec total))))))]
    (->> blocks
      (map safe-trim)
      (remove nil?)
      (str/join "\n\n")
      not-empty)))

(defn response->receipt
  "Return {:extraction .. :parsed-markdown .. :date-line ..} from LlamaParse response."
  [resp-json]
  (let [combined (response->combined-text resp-json)
        date-line (text->date-line combined)
        extraction (response->extraction resp-json)
        md (extraction->markdown extraction {:date-line date-line})]
    {:extraction extraction
     :parsed-markdown md
     :date-line date-line}))
