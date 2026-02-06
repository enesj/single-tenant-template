(ns app.domain.backend.expenses.workers.receipt-ocr.markdown
  "Heuristics for extracting useful signals from provider OCR markdown.

  This is intentionally best-effort and tolerant of weird OCR output."
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str]))

(defn- normalize-text [s]
  (some-> s
    str
    str/lower-case
    (str/replace #"\s+" " ")
    str/trim
    not-empty))

(defn label-present-in-markdown?
  [markdown raw-label]
  (let [m (normalize-text markdown)
        l (normalize-text raw-label)]
    (boolean (and m l (str/includes? m l)))))

(defn- has-letter? [s]
  (boolean (and (string? s) (re-find #"\p{L}" s))))

(defn- normalize-item-label [raw-label]
  (let [raw-label (some-> raw-label
                    str
                    (str/replace #"\|" " ")
                    (str/replace #"\s+" " ")
                    str/trim
                    not-empty)]
    (when raw-label
      (if-let [[_ _ rest]
               (re-matches #"(?i)^([0-9]{4,}|[A-Z][0-9]{4,})[ \t]+(.+)$" raw-label)]
        (let [rest (str/trim rest)]
          (if (has-letter? rest)
            rest
            raw-label))
        raw-label))))

(def ^:private unit-prefixes
  #{"t/pc"})

(defn- unit-prefix? [s]
  (when (string? s)
    (contains? unit-prefixes (normalize-text s))))

(def ^:private supplier-ignore-prefixes
  ["jib"
   "pib"
   "tbfm"
   "bf"
   "fiskalni"
   "racun"
   "račun"
   "фискални"
   "рачун"
   "total"
   "ukupno"
   "ukupna"
   "укупно"
   "укупан"
   "pdv"
   "пдв"
   "vat"
   "osn"
   "ve"
   "upl"
   "gotovina"
   "kartica"
   "povrat"
   "промет"
   "promet"
   "касир"
   "kasir"])

(def ^:private header-stop-prefixes
  ["jib" "pib" "ibfm" "ibem" "tbfm" "bf" "fiskalni" "racun" "račun" "фискални" "рачун"])

(def ^:private legal-suffix-re
  #"(?i)\s+(d\.?o\.?o\.?|d\.?d\.?|a\.?d\.?|s\.?p\.?|j\.?p\.?)\.?\s*$")

(def ^:private store-name-prefixes
  ["podružnica" "poslovnica" "tc " "pc " "cc " "centar" "market" "maloprodaja" "prodavnica"])

(def ^:private address-prefixes
  ["ul." "ul " "ulica" "bb" "br." "br " "trg"])

(defn- extract-quoted-name
  "Extract text inside quotes, e.g. '\"Pepco B-H\" d.o.o.' -> 'Pepco B-H'"
  [line]
  (when (string? line)
    (when-let [[_ name] (re-find #"\"([^\"]+)\"" line)]
      (str/trim name))))

(defn- looks-like-postal-city?
  "Check if line looks like '71000 Sarajevo' pattern"
  [line]
  (when (string? line)
    (boolean (re-matches #"^\d{5}\s+\S.*$" (str/trim line)))))

(defn- strip-legal-suffix
  "Remove legal suffixes like d.o.o., d.d., etc."
  [name]
  (when (string? name)
    (-> name
      (str/replace legal-suffix-re "")
      str/trim
      not-empty)))

(defn- is-header-stop-line?
  "Check if this line marks end of merchant header (tax IDs, receipt markers)"
  [norm]
  (let [norm (or norm "")
        norm* (-> norm
                (str/replace #"^[^\p{L}\p{N}]+" "")
                str/trim)]
    (or (some #(str/starts-with? norm* %) header-stop-prefixes)
      (re-find #"^\d{1,2}\.\d{1,2}\.\d{2,4}" norm*))))

(defn- is-store-name-line?
  "Check if line looks like a store/branch name"
  [norm]
  (some #(str/starts-with? norm %) store-name-prefixes))

(defn- is-address-line?
  "Check if line looks like an address"
  [norm line]
  (or (some #(str/starts-with? norm %) address-prefixes)
    (looks-like-postal-city? line)))

(defn markdown->merchant-header
  "Parse receipt header into structured merchant info.
   Returns {:merchant_name .. :store_name .. :address ..} or nil."
  [markdown]
  (when (string? markdown)
    (let [strip-leading-junk (fn [s]
                               (some-> s
                                 (str/replace #"^[^\p{L}\p{N}]+" "")
                                 str/trim
                                 not-empty))
          numeric-line? (fn [s]
                          (when (string? s)
                            (let [s (-> s str/trim (str/replace #"\s+" ""))]
                              (boolean (re-matches #"\d{6,}" s)))))
          skip-leading-line? (fn [line0]
                               (let [line (some-> line0 str str/trim not-empty)
                                     norm (some-> line normalize-text strip-leading-junk)]
                                 (or (nil? line)
                                   (numeric-line? line)
                                   (and norm (some #(str/starts-with? norm %) supplier-ignore-prefixes)))))
          lines (->> (str/split-lines markdown)
                  (drop-while skip-leading-line?)
                  vec)
          header-lines (take-while
                         (fn [line0]
                           (let [line (some-> line0 str str/trim not-empty)
                                 norm (normalize-text line)]
                             (and norm (not (is-header-stop-line? norm)))))
                         lines)
          header-lines (map #(some-> % str str/trim not-empty) header-lines)
          header-lines (remove nil? header-lines)]
      (when (seq header-lines)
        (let [first-line (first header-lines)
              quoted-name (extract-quoted-name first-line)
              merchant-name (or quoted-name
                              (strip-legal-suffix first-line)
                              first-line)
              rest-lines (if quoted-name
                           (rest header-lines)
                           (rest header-lines))
              {:keys [store-name address-lines]}
              (reduce
                (fn [acc line]
                  (let [norm (normalize-text line)]
                    (cond
                      (and (nil? (:store-name acc)) (is-store-name-line? norm))
                      (assoc acc :store-name line)

                      (is-address-line? norm line)
                      (update acc :address-lines conj line)

                      (and (nil? (:store-name acc))
                        (empty? (:address-lines acc))
                        (not (re-find legal-suffix-re line)))
                      (assoc acc :store-name line)

                      :else
                      (update acc :address-lines conj line))))
                {:store-name nil :address-lines []}
                rest-lines)
              address (when (seq address-lines)
                        (str/join ", " address-lines))]
          (cond-> {:merchant_name merchant-name}
            store-name (assoc :store_name store-name)
            address (assoc :address address)))))))

(defn markdown->supplier-guess
  "Take the first plausible merchant-like line from markdown."
  [markdown]
  (when (string? markdown)
    (let [strip-leading-junk (fn [s]
                               (some-> s
                                 (str/replace #"^[^\p{L}\p{N}]+" "")
                                 str/trim
                                 not-empty))
          numeric-line? (fn [s]
                          (when (string? s)
                            (let [s (-> s str/trim (str/replace #"\s+" ""))]
                              (boolean (re-matches #"\d{6,}" s)))))]
      (->> (str/split-lines markdown)
        (keep (fn [line0]
                (let [line (some-> line0 str str/trim not-empty)
                      norm (some-> line normalize-text strip-leading-junk)]
                  (when (and norm
                          (not (numeric-line? line))
                          (not (some #(str/starts-with? norm %) supplier-ignore-prefixes)))
                    line))))
        first))))

(defn markdown->total-amount
  "Best-effort parse receipt total from OCR markdown.

  Handles totals embedded in markdown table rows by stripping pipe characters.

  Selection strategy:
  - Prefer explicit grand-total prefixes (e.g. TOTAL / UKUPAN IZNOS).
  - Within the chosen group, prefer the latest non-zero amount.
  - Fallback to the latest parsed amount when all candidates are zero.

  This avoids false overrides from trailing balance rows such as
  `UKUPNO: 0,00` that can appear after `TOTAL: <amount>`."
  [markdown]
  (when (string? markdown)
    (let [clean-line (fn [s]
                       (some-> s
                         str
                         (str/replace #"[|¦│]" " ")
                         (str/replace #"\s+" " ")
                         str/trim
                         not-empty))
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
                                     norm (normalize-text line)
                                     prefix-kind (cond
                                                   (and norm (some #(str/starts-with? norm %) preferred-prefixes)) :preferred
                                                   (and norm (some #(str/starts-with? norm %) fallback-prefixes)) :fallback
                                                   :else nil)
                                     amount (common/parse-money line)]
                                 (when (and prefix-kind
                                         amount
                                         (not (some #(str/includes? norm %) exclude-substrings)))
                                   {:kind prefix-kind
                                    :amount amount}))))
                       vec)
          preferred (->> candidates
                      (filter (fn [{:keys [kind]}] (= :preferred kind)))
                      vec)]
      (when (seq candidates)
        (pick-best (if (seq preferred) preferred candidates))))))

(def ^:private ba-datetime-re
  "Regex for BA receipt datetime pattern: dd.mm.yyyy. hh:mm or dd.mm.yyyy hh:mm"
  #"(\d{1,2})\.(\d{1,2})\.(\d{4})\.?\s+(\d{1,2}):(\d{2})")

(def ^:private ba-date-re
  "Regex for BA receipt date pattern: dd.mm.yyyy (optionally with trailing dot)"
  #"(\d{1,2})\.(\d{1,2})\.(\d{4})\.?(?:\s|$)")

(defn markdown->purchased-at
  "Best-effort extract purchase datetime from receipt markdown.

  Looks for BA date patterns like '29.01.2026. 14:31' or '29.01.2026' and
  returns an ISO8601 string compatible with common/parse-instant."
  [markdown]
  (when (string? markdown)
    (or
      ;; Try datetime first (more specific)
      (when-let [[_ day month year hour minute] (re-find ba-datetime-re markdown)]
        (format "%s-%02d-%02dT%02d:%02d:00"
          year
          (parse-long month)
          (parse-long day)
          (parse-long hour)
          (parse-long minute)))
      ;; Fall back to date only
      (when-let [[_ day month year] (re-find ba-date-re markdown)]
        (format "%s-%02d-%02d"
          year
          (parse-long month)
          (parse-long day))))))

(defn- markdown->pipe-line-items [markdown]
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
                        (let [norms (->> cells (map normalize-text) (remove nil?) vec)
                              hits (count (filter header-token? norms))]
                          (>= hits 2)))
          header->table-kind (fn [cells]
                               (let [norms (->> cells (map normalize-text) (remove nil?) vec)
                                     joined (str/join " " norms)]
                                 (cond
                                   (re-find #"(?:porez|порез|pdv|пдв|vat|tax)" joined) :tax
                                   (and (re-find #"(?:naziv|name|назив|опис|description|item)" joined)
                                     (re-find #"(?:ukupno|total|укупно)" joined)) :items
                                   :else nil)))
          header->mapping (fn [cells]
                            (let [norms (map normalize-text cells)
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
                                (let [joined (->> cells (map normalize-text) (remove nil?) (str/join " "))]
                                  (boolean (re-find #"(?:porez|порез|pdv|пдв|vat|tax)" joined))))
          parse-item (fn [cells {:keys [label-idx qty-idx unit-idx total-idx]}]
                       (let [raw-label (normalize-item-label (nth cells label-idx nil))
                             qty (common/parse-money (nth cells qty-idx nil))
                             unit-price (common/parse-money (nth cells unit-idx nil))
                             line-total (common/parse-money (nth cells total-idx nil))]
                         (when (and (has-letter? raw-label) line-total)
                           {:raw_label raw-label
                            :qty qty
                            :unit_price unit-price
                            :line_total line-total})))
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

              (>= (count cells) 4)
              (let [item (when-not (row-looks-like-tax? cells)
                           (parse-item cells (or mapping default-mapping)))]
                (recur (rest remaining) nil table-kind mapping (cond-> items item (conj item))))

              :else
              (recur (rest remaining) nil nil nil items))))))))

(defn- line->qty-unit-total [line]
  (let [tokens (->> (str/split (str/trim (or line "")) #"\s+")
                 (remove str/blank?)
                 (remove #{"|" "¦" "│"})
                 vec)]
    (when (seq tokens)
      (or
        ;; qty token already contains "x", e.g. "1.000x"
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
        ;; separate "x" token, e.g. "1.000 x 2,10 2,10"
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

(def ^:private discount-line-re
  #"(?i)^\s*-?\s*([0-9]{1,3}(?:[\.,][0-9]{1,2})?)\s*%\s*:?:?\s*(-?[0-9]{1,9}[\.,][0-9]{2})\s*(?:e|km|bam|€)?\s*$")

(defn- line->discount [line]
  (when (string? line)
    (when-let [[_ pct amount] (re-matches discount-line-re (str/trim line))]
      (let [pct (common/parse-money pct)
            amount (common/parse-money amount)]
        (when (and pct amount (pos? (double pct)) (<= (double pct) 100.0))
          {:pct (.abs (bigdec pct))
           :amount (bigdec amount)})))))

(defn- apply-discount-to-item
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

(declare line->trailing-money item-ignore-prefixes ignore-item-line?)

(defn- markdown->qty-line-items [markdown]
  (when (string? markdown)
    (let [lines (str/split-lines markdown)]
      (loop [remaining lines
             pending []
             items []]
        (if-not (seq remaining)
          (vec items)
          (let [line0 (first remaining)
                line (some-> line0 str str/trim not-empty)
                norm (normalize-text line)]
            (cond
              (nil? line)
              (recur (rest remaining) [] items)

              (ignore-item-line? norm)
              (recur (rest remaining) [] items)

              (and (seq items) (line->discount line))
              (let [discount (line->discount line)
                    last-item (peek items)
                    items (conj (pop items) (apply-discount-to-item last-item discount))]
                (recur (rest remaining) [] items))

              :else
              (if-let [{:keys [label-from-line qty unit_price line_total]} (line->qty-unit-total line)]
                (let [label-lines-raw (if-let [l (some-> label-from-line str/trim not-empty)]
                                        (if (has-letter? l) [l] pending)
                                        pending)
                      pending-total (->> label-lines-raw
                                      (keep (comp :money line->trailing-money))
                                      last)
                      label-lines (->> label-lines-raw
                                    (map (fn [l]
                                           (or (:prefix (line->trailing-money l)) l)))
                                    (remove str/blank?)
                                    vec)
                      label (->> label-lines
                              (map str/trim)
                              (remove str/blank?)
                              (str/join " ")
                              normalize-item-label)
                      line-total0 (or line_total pending-total)
                      unit-price0 (or unit_price
                                    (when (and qty line-total0 (pos? (.signum (bigdec qty))))
                                      (.divide (bigdec line-total0) (bigdec qty) 2 java.math.RoundingMode/HALF_UP)))
                      line-total1 (or line-total0
                                    (when (and qty unit-price0)
                                      (.setScale (* (bigdec qty) (bigdec unit-price0)) 2 java.math.RoundingMode/HALF_UP)))]
                  (if (and (has-letter? label) line-total1)
                    (recur (rest remaining)
                      []
                      (conj items {:raw_label label
                                   :qty qty
                                   :unit_price unit-price0
                                   :line_total line-total1}))
                    (recur (rest remaining) [] items)))
                (if-let [{:keys [prefix money]} (line->trailing-money line)]
                  (if (str/includes? line "|")
                    (let [pending (cond-> pending
                                    (has-letter? line) (conj line))
                          pending (if (> (count pending) 3)
                                    (subvec pending (- (count pending) 3))
                                    pending)]
                      (recur (rest remaining) pending items))
                    (let [label-lines-raw (cond-> pending
                                            (and prefix (has-letter? prefix) (not (unit-prefix? prefix)))
                                            (conj prefix))
                          label-lines (->> label-lines-raw
                                        (map (fn [l]
                                               (or (:prefix (line->trailing-money l)) l)))
                                        (remove str/blank?)
                                        vec)
                          label (->> label-lines
                                  (map str/trim)
                                  (remove str/blank?)
                                  (str/join " ")
                                  normalize-item-label)]
                      (if (and money (has-letter? label))
                        (recur (rest remaining)
                          []
                          (conj items {:raw_label label
                                       :qty 1M
                                       :unit_price money
                                       :line_total money}))
                        (recur (rest remaining) [] items))))
                  (let [pending (cond-> pending
                                  (has-letter? line) (conj line))
                        pending (if (> (count pending) 3)
                                  (subvec pending (- (count pending) 3))
                                  pending)]
                    (recur (rest remaining) pending items)))))))))))

(def ^:private trailing-money-re
  #"(?i)^(.*?)(\d{1,9}[\.,]\d{2})\s*(?:[A-Z])?\s*(?:e|km|bam|€)?\s*$")

(defn- line->trailing-money [line]
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
  (-> supplier-ignore-prefixes
    (conj "ibfm")
    (conj "ibem")
    (conj "tbfm")
    (conj "pdu")))

(def ^:private payment-summary-line-re
  #"^(?:pov(?:$|\s|:)|(?:cek|ček)(?:$|\s|:)|kortica(?:$|\s|:))")

(defn- ignore-item-line? [norm]
  (boolean
    (or (nil? norm)
      (some #(str/starts-with? norm %) item-ignore-prefixes)
      (re-find payment-summary-line-re norm)
      (and (str/starts-with? norm "umla")
        (re-find #"(?:kortica|kartica)" norm)))))

(defn- markdown->price-line-items [markdown]
  (when (string? markdown)
    (let [lines (str/split-lines markdown)]
      (loop [remaining lines
             pending []
             items []]
        (if-not (seq remaining)
          (vec items)
          (let [line0 (first remaining)
                line (some-> line0 str str/trim not-empty)
                norm (normalize-text line)]
            (cond
              (nil? line)
              (recur (rest remaining) [] items)

              (ignore-item-line? norm)
              (recur (rest remaining) [] items)

              (and (seq items) (line->discount line))
              (let [discount (line->discount line)
                    last-item (peek items)
                    items (conj (pop items) (apply-discount-to-item last-item discount))]
                (recur (rest remaining) [] items))

              :else
              (if-let [{:keys [prefix money]} (line->trailing-money line)]
                (let [label-lines (cond-> pending
                                    (and prefix (has-letter? prefix) (not (unit-prefix? prefix)))
                                    (conj prefix))
                      label (->> label-lines
                              (map str/trim)
                              (remove str/blank?)
                              (str/join " ")
                              normalize-item-label)]
                  (if (and money (has-letter? label))
                    (recur (rest remaining)
                      []
                      (conj items {:raw_label label
                                   :qty 1M
                                   :unit_price money
                                   :line_total money}))
                    (recur (rest remaining) [] items)))
                (let [pending (cond-> pending
                                (has-letter? line) (conj line))
                      pending (if (> (count pending) 3)
                                (subvec pending (- (count pending) 3))
                                pending)]
                  (recur (rest remaining) pending items))))))))))

(defn markdown->line-item-candidates
  "Parse markdown to best-effort line item candidates.

  Prefers pipe-table rows (common in OCR markdown), then qty lines (e.g. 1.000x),
  then label + price heuristics."
  [markdown]
  (when (string? markdown)
    (let [pipe-items (markdown->pipe-line-items markdown)]
      (if (seq pipe-items)
        pipe-items
        (let [qty-items (markdown->qty-line-items markdown)]
          (if (seq qty-items)
            qty-items
            (markdown->price-line-items markdown)))))))
