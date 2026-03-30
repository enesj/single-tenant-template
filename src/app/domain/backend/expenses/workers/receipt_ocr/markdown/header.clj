(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.header
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.text :as text]
    [clojure.string :as str]))

(def supplier-ignore-prefixes
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
  #"(?i)\b(?:d\s*\.?\s*o\s*\.?\s*o|d\s*\.?\s*d|a\s*\.?\s*d|s\s*\.?\s*p|j\s*\.?\s*p|u\s*\.?\s*o|llc|ltd|inc|gmbh|ag)\b\.?")

(def ^:private store-name-prefixes
  ["ogranak"
   "podružnica"
   "poslovnica"
   "pj "
   "tc "
   "pc "
   "cc "
   "centar"
   "market"
   "maloprodaja"
   "prodavnica"])

(def ^:private address-prefixes
  ["ul." "ul " "ulica" "bb" "br." "br " "trg"])

(defn- extract-quoted-name [line]
  (when (string? line)
    (when-let [[_ name] (re-find #"\"([^\"]+)\"" line)]
      (str/trim name))))

(defn- looks-like-postal-city? [line]
  (when (string? line)
    (boolean (re-matches #"^\d{5}\s+\S.*$" (str/trim line)))))

(defn strip-legal-suffix [name]
  (when (string? name)
    (when-let [name* (some-> name str str/trim not-empty)]
      (let [m (re-matcher legal-suffix-re name*)]
        (if (.find m)
          (let [idx (.start m)]
            (if (pos? (long idx))
              (some-> (subs name* 0 (long idx))
                str/trim
                (str/replace #"[\\s,;:]+$" "")
                str/trim
                not-empty)
              name*))
          name*)))))

(defn- is-header-stop-line? [norm]
  (let [norm (or norm "")
        norm* (-> norm
                (str/replace #"^[^\p{L}\p{N}]+" "")
                str/trim)]
    (or (some #(str/starts-with? norm* %) header-stop-prefixes)
      (re-find #"^\d{1,2}\.\d{1,2}\.\d{2,4}" norm*))))

(defn- is-store-name-line? [norm]
  (some #(str/starts-with? norm %) store-name-prefixes))

(defn- is-address-line? [norm line]
  (or (some #(str/starts-with? norm %) address-prefixes)
    (looks-like-postal-city? line)))

(defn split-store-name-and-address [s]
  (when (string? s)
    (let [s (some-> s str str/trim not-empty)]
      (when s
        (let [parts (->> (str/split s #"\s*,\s*")
                      (map str/trim)
                      (remove str/blank?)
                      vec)
              first* (first parts)
              rest* (subvec parts 1)
              norm-first (text/normalize-text first*)]
          (when (and (>= (count parts) 2)
                  (seq norm-first)
                  (is-store-name-line? norm-first))
            (when-let [address (some->> rest*
                                 (str/join ", ")
                                 str/trim
                                 not-empty)]
              {:store_name first*
               :address address})))))))

(def ^:private receipt-datetime-re
  #"(?iu)^\d{1,2}\.\d{1,2}\.\d{2,4}\.?\s+\d{1,2}:\d{2}(?::\d{2})?$")

(def ^:private receipt-date-only-re
  #"(?iu)^\d{1,2}\.\d{1,2}\.\d{2,4}\.?$")

(def ^:private receipt-time-only-re
  #"(?iu)^\d{1,2}:\d{2}(?::\d{2})?$")

(defn- date-time-line? [s]
  (let [s (some-> s text/strip-line-markup)]
    (boolean
      (and s
        (or (re-matches receipt-datetime-re s)
          (re-matches receipt-date-only-re s)
          (re-matches receipt-time-only-re s))))))

(defn- table-boundary-line? [s]
  (let [s (some-> s str str/lower-case str/trim)]
    (boolean
      (and s
        (or (str/includes? s "<table")
          (str/includes? s "<tbody")
          (str/includes? s "<tr")
          (str/starts-with? s "|"))))))

(def ^:private metadata-role-tokens
  #{"agent"
    "blagajna"
    "blagajnik"
    "cashier"
    "clerk"
    "customer"
    "employee"
    "ime"
    "kasir"
    "name"
    "operater"
    "operator"
    "prezime"
    "prodavac"
    "prodavacica"
    "prodavač"
    "prodavačica"
    "radnik"
    "radnica"
    "seller"
    "server"
    "sluzbenik"
    "sluzbenica"
    "službenik"
    "službenica"
    "касир"
    "оператер"
    "оператор"
    "презиме"
    "продавац"
    "радник"
    "радница"
    "службеник"
    "име"})

(def ^:private metadata-label-value-re
  #"(?iu)^\s*([^:]{1,32})\s*:\s*(.+?)\s*$")

(def ^:private person-name-token-re
  #"(?iu)^\p{L}[\p{L}.'’`-]*$")

(defn- normalized-word-tokens
  [s]
  (when-let [s* (some-> s str str/trim not-empty)]
    (->> (str/split s* #"[^\p{L}\p{N}]+")
      (map str/lower-case)
      (remove str/blank?)
      vec)))

(defn- roleish-metadata-label?
  [label]
  (boolean
    (some metadata-role-tokens (normalized-word-tokens label))))

(defn- person-name-like?
  [value]
  (let [tokens (normalized-word-tokens value)]
    (boolean
      (and (seq tokens)
        (<= 2 (count tokens) 4)
        (every? #(re-matches person-name-token-re %) tokens)
        (not-any? #(re-find #"\d" %) tokens)
        (not (re-find legal-suffix-re (or value "")))))))

(defn- numeric-line?
  [s]
  (when (string? s)
    (let [s (-> s str/trim (str/replace #"\s+" ""))]
      (boolean (re-matches #"\d{6,}" s)))))

(defn plausible-merchant-name?
  [s]
  (let [s* (some-> s text/strip-line-markup str/trim not-empty)]
    (boolean
      (and s*
        (not (numeric-line? s*))
        (not (date-time-line? s*))
        (if-let [[_ label value] (re-matches metadata-label-value-re s*)]
          (not (and (roleish-metadata-label? label)
                 (person-name-like? value)))
          true)))))

(defn merchant-header
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
                               (let [line (some-> line0 text/strip-line-markup)
                                     norm (some-> line text/normalize-text strip-leading-junk)]
                                 (or (nil? line)
                                   (numeric-line? line)
                                   (date-time-line? line)
                                   (not (plausible-merchant-name? line))
                                   (and norm (some #(str/starts-with? norm %) supplier-ignore-prefixes)))))
          lines (->> (str/split-lines markdown)
                  (take-while (complement table-boundary-line?))
                  (drop-while skip-leading-line?)
                  vec)
          header-lines (take-while
                         (fn [line0]
                           (let [line (some-> line0 text/strip-line-markup)
                                 norm (text/normalize-text line)]
                             (and norm
                               (plausible-merchant-name? line)
                               (not (table-boundary-line? line0))
                               (not (is-header-stop-line? norm)))))
                         lines)
          header-lines (map #(some-> % text/strip-line-markup) header-lines)
          header-lines (remove nil? header-lines)]
      (when (seq header-lines)
        (let [first-line (first header-lines)
              quoted-name (extract-quoted-name first-line)
              merchant-name (or quoted-name
                              (strip-legal-suffix first-line)
                              first-line)
              rest-lines (rest header-lines)
              {:keys [store-name address-lines]}
              (reduce
                (fn [acc line]
                  (let [norm (text/normalize-text line)]
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

(defn supplier-guess
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
        (take-while (complement table-boundary-line?))
        (keep (fn [line0]
                (let [line (some-> line0 text/strip-line-markup)
                      norm (some-> line text/normalize-text strip-leading-junk)]
                  (when (and norm
                          (plausible-merchant-name? line)
                          (not (numeric-line? line))
                          (not (date-time-line? line))
                          (not (some #(str/starts-with? norm %) supplier-ignore-prefixes)))
                    line))))
        first))))
