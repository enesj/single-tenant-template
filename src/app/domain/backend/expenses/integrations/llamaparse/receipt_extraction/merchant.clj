(ns app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.merchant
  (:require
    [clojure.string :as str]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.text :as text]))

(def merchant-ignore-prefixes
  ["jib" "pib" "ibfm" "ibem" "tbfm" "bf" "fiskalni" "racun" "račun" "фискални" "рачун"
   "ve" "osn" "pdv" "vat" "total" "ukupno" "ukupna" "ukupan" "uplac" "upl" "gotovina" "kartica" "povrat"
   "kasir" "касир" "cashier" "operator" "operater" "оператер" "оператор"])

(def merchant-ignore-exact
  #{"fiskalni racun" "fiskalni račun" "racun" "račun" "фискални рачун" "рачун"
    "merchant information"})

(def item-like-leading-code-re
  #"(?iu)^(?:[A-Z]?\d{4,})(?:\s+|\b).*$")

(def item-like-money-re
  #"(?iu)\d{1,9}[\.,]\d{2}\s*(?:[A-Z])?\s*(?:e|km|bam|€)\b")

(def item-like-unit-re
  #"(?iu)(?:/\s*(?:kom|ko|pc)|\bkom/kom\b|\bt/pc\b)")

(def item-like-qty-re
  #"(?iu)\b\d+[\d,\.]*\s*x\b")

(defn item-like-line?
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (boolean
      (and line norm
        (or (re-find item-like-leading-code-re line)
          (re-find item-like-money-re line)
          (re-find item-like-unit-re norm)
          (re-find item-like-qty-re norm))))))

(def address-prefixes
  ["ul." "ul " "ulica" "trg" "bb" "br." "br " "broj" "street" "st." "bulevar" "avenija" "av." "av " "put" "cesta"])

(defn address-like-line?
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (boolean
      (and line norm
        (or (some #(str/starts-with? norm %) address-prefixes)
          (re-matches #"(?iu)^\d{5}\s+\S.*$" line)
          (re-find #"(?iu)\b(?:br\.?|broj[a]?)\s*\d+[a-z]?\b" line)
          (re-matches #"(?iu)^[\p{L}\s\.'’-]{3,}\s+\d+[a-z]?(?:/\d+)?$" line))))))

(defn separator-noise?
  "Detect separator-like OCR noise lines (e.g. '====E=EE=5SE')."
  [line]
  (let [line (some-> line text/safe-trim)
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

(def legal-suffix-re
  #"(?i)\b(?:d\s*\.?\s*o\s*\.?\s*o|d\s*\.?\s*d|a\s*\.?\s*d|s\s*\.?\s*p|j\s*\.?\s*p|u\s*\.?\s*o|llc|ltd|inc|gmbh|ag)\b\.?")

(defn strip-legal-suffix
  [name]
  (when (string? name)
    (let [name (some-> name text/safe-trim)]
      (when name
        (let [m (re-matcher legal-suffix-re name)]
          (if (.find m)
            (let [idx (.start m)]
              (if (pos? (long idx))
                (some-> (subs name 0 (long idx))
                  (str/replace #"[\s,;:]+$" "")
                  text/safe-trim)
                name))
            name))))))

(defn- extract-quoted-name
  [s]
  (when (string? s)
    (when-let [[_ name] (re-find #"\"([^\"]+)\"" s)]
      (text/safe-trim name))))

(defn- merchant-candidate?
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)
        embedded-store-line? (boolean
                               (and line
                                 (re-find legal-suffix-re line)
                                 (some #(str/includes? norm %)
                                   ["podruznica" "podružnica" "подружница"
                                    "ogranak" "огранак"
                                    "poslovnica" "пословница"
                                    "filijala" "филијала"
                                    "prodavnica" "продавница"])))
        address-like? (address-like-line? line)]
    (boolean
      (and line norm
        (re-find #"\p{L}" line)
        (not (separator-noise? line))
        (not (re-matches text/ba-datetime-line-re line))
        (not (re-matches text/ba-date-line-re line))
        (or (not address-like?) embedded-store-line?)
        (not (item-like-line? line))
        (not (contains? merchant-ignore-exact norm))
        (not (some #(str/starts-with? norm %) merchant-ignore-prefixes))))))

(def store-line-prefixes
  ["ogranak" "огранак"
   "pj" "pj." "пј" "п.ј"
   "podruznica" "podružnica" "подружница"
   "poslovnica" "пословница"
   "filijala" "филијала"
   "prodavnica" "продавница"
   "radnja" "радња"
   "maloprodaja" "малопродаја"
   "maloprodajna" "малопродајна"
   "apoteka" "апотека"])

(def store-line-regexes
  [#"(?iu)^p\s*\.?\s*j\s*\.?(?:\s*(?:broj|br\.?))?\s*\d{0,4}\b"
   #"(?iu)^p\s*\.?\s*o\s*\.?(?:\s*(?:broj|br\.?))?\s*\d{0,4}\b"])

(def store-line-substrings
  ["trzni centar" "tržni centar" "city center" "shopping center"])

(defn store-line?
  [line]
  (let [line (some-> line text/safe-trim)
        norm (text/normalize-text line)]
    (boolean
      (and norm
        (or (some #(str/starts-with? norm %) store-line-prefixes)
          (and line (some #(re-find % line) store-line-regexes))
          (some #(str/includes? norm %) store-line-substrings))))))

(defn- merchant-score
  [line]
  (let [line (some-> line text/safe-trim)
        compact (some-> line (str/replace #"\s+" ""))
        stripped (some-> line (str/replace #"^[^\p{L}\p{N}]+" "") text/safe-trim)
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

(defn text->merchant-name
  [text-content]
  (when (string? text-content)
    (let [stop-line?
          (fn [line]
            (let [norm (text/normalize-text line)]
              (or (re-matches text/ba-datetime-line-re line)
                (re-matches text/ba-date-line-re line)
                (and norm
                  (or (str/starts-with? norm "bf")
                    (str/starts-with? norm "tbfm"))))))
          lines (->> (str/split-lines text-content)
                  (map text/safe-trim)
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
                           (when (and (not (store-line? line))
                                   (not (address-like-line? line)))
                             (when-let [q (extract-quoted-name line)]
                               [idx q]))))
                   vec)
          legal (->> candidates
                  (filter (fn [[_idx line]]
                            (and (string? line)
                              (re-find legal-suffix-re line))))
                  vec)
          best
          (cond
            (seq legal)
            (second (apply min-key first legal))

            (seq quoted)
            (second (apply min-key first quoted))

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
                 (str/replace #"^[\"'`]+|[\"'`]+$" "")
                 (str/replace #"[\s,;:]+$" "")
                 text/safe-trim)]
      (when (and best (re-find #"\p{L}" best))
        best))))

(defn- dedupe-lines-by-normalized
  [lines]
  (->> lines
    (reduce (fn [{:keys [seen out]} line]
              (let [key (text/normalize-text line)]
                (if (or (nil? key) (contains? seen key))
                  {:seen seen :out out}
                  {:seen (conj seen key)
                   :out (conj out line)})))
      {:seen #{}
       :out []})
    :out
    vec))

(defn text->merchant-context
  "Best-effort merchant location extraction from OCR header/body text.

  Returns a merchant map that can include:
  - :name
  - :store_name
  - :address
  - :raw_address (stable alias key candidate from store/address lines)"
  [header text-content]
  (let [header-stop-line?
        (fn [line]
          (let [norm (text/normalize-text line)]
            (or (re-matches text/ba-datetime-line-re line)
              (re-matches text/ba-date-line-re line)
              (and norm
                (or (str/starts-with? norm "bf")
                  (str/starts-with? norm "tbfm"))))))
        marker-line?
        (fn [line]
          (let [norm (text/normalize-text line)]
            (boolean
              (and norm
                (some #(str/starts-with? norm %) merchant-ignore-prefixes)))))
        address-candidate-line?
        (fn [line]
          (let [line (some-> line text/safe-trim)
                norm (text/normalize-text line)]
            (boolean
              (and line norm
                (re-find #"\p{L}" line)
                (not (separator-noise? line))
                (not (re-matches text/ba-datetime-line-re line))
                (not (re-matches text/ba-date-line-re line))
                (or (address-like-line? line)
                  (not (item-like-line? line)))
                (not (store-line? line))
                (not (marker-line? line))))))
        text->lines
        (fn [s]
          (when (string? s)
            (->> (str/split-lines s)
              (map text/safe-trim)
              (remove nil?)
              (remove separator-noise?)
              (take 80)
              (take-while (complement header-stop-line?))
              dedupe-lines-by-normalized)))
        header-lines (or (text->lines header) [])
        text-lines (or (text->lines text-content) [])
        source-lines (if (seq header-lines) header-lines text-lines)
        merchant-name (or (text->merchant-name header)
                        (text->merchant-name text-content))
        merchant-normalized (some-> merchant-name text/normalize-text)
        merchant-idx
        (when (seq merchant-normalized)
          (->> source-lines
            (map-indexed vector)
            (some (fn [[idx line]]
                    (when (= merchant-normalized (text/normalize-text line))
                      idx)))))
        trailing-lines (if (some? merchant-idx)
                         (subvec (vec source-lines) (inc merchant-idx))
                         (vec source-lines))
        store-idx
        (->> trailing-lines
          (map-indexed vector)
          (some (fn [[idx line]]
                  (when (store-line? line)
                    idx))))
        store-name (when (some? store-idx)
                     (some-> (nth trailing-lines store-idx nil) text/safe-trim))
        address-lines (if (some? store-idx)
                        (->> (subvec trailing-lines (inc store-idx))
                          (take-while (complement marker-line?))
                          (filter address-candidate-line?)
                          (map text/safe-trim)
                          (remove nil?)
                          (take 3)
                          vec)
                        (->> trailing-lines
                          (filter address-like-line?)
                          (map text/safe-trim)
                          (remove nil?)
                          vec))
        address (when (seq address-lines)
                  (str/join ", " address-lines))
        raw-address (when (or (seq store-name) (seq address))
                      (str/join ", " (remove nil? [store-name address])))]
    (cond-> {}
      (seq merchant-name) (assoc :name merchant-name)
      (seq store-name) (assoc :store_name store-name)
      (seq address) (assoc :address address)
      (seq raw-address) (assoc :raw_address raw-address))))
