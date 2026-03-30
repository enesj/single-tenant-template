(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.units
  "Extract measurement units from OCR article labels.

  Bosnian POS receipts append unit suffixes after a slash:
    TUBORG 0,33 NEPOVRATNI/KO  → unit: kom, label: TUBORG 0,33 NEPOVRATNI
    Narandza grcka rinf /KG    → unit: kg,  label: Narandza grcka rinf
    PAPIR ZA PAKOVANJE/KOM     → unit: kom, label: PAPIR ZA PAKOVANJE

  When no suffix is present and qty is integer (or nil), default unit is kom.
  When qty is fractional (e.g. 0.350), assume kg (weight-based item)."
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str]))

(def default-unit "kom")

(def ^:private unit-abbreviations
  "Lowercase abbreviation -> canonical unit. Longest-first so /KOM matches before /KO and /LIT before /L."
  [["kom" "kom"]
   ["ko"  "kom"]
   ["co"  "kom"]
   ["pc"  "kom"]
   ["kg"  "kg"]
   ["gr"  "g"]
   ["lit" "l"]
   ["l"   "l"]
   ["pak" "pak"]])

(def ^:private unit-suffix-pattern
  "Regex matching a trailing unit suffix.
  Groups: (1) everything before the slash, (2) the unit abbreviation.
  Allows optional whitespace before slash and optional tax marker like (E) after."
  #"(?i)^(.*?)\s*/\s*(kom|ko|co|pc|kg|gr|lit|l|pak)\s*(?:\([A-Z]\))?\s*$")

(def ^:private package-count-suffix-pattern
  "Regex matching trailing pack-count metadata like `24/1` that should not become part of the alias label."
  #"(?i)^(.*?)\s+(\d+\s*/\s*\d+)\s*$")

(defn- strip-package-count-suffix
  [raw-label]
  (when-let [s (some-> raw-label str str/trim not-empty)]
    (when-let [[_ base _pack-count] (re-matches package-count-suffix-pattern s)]
      (some-> base str/trim not-empty))))

(defn- integer-qty?
  "True when qty is nil (unknown, assume integer) or a whole number."
  [qty]
  (let [parsed (common/parse-money qty)]
    (or (nil? parsed)
      (try
        (zero? (.remainder ^java.math.BigDecimal (bigdec parsed)
                 java.math.BigDecimal/ONE))
        (catch Exception _ false)))))

(defn parse-unit-suffix
  "Parse a unit suffix from a raw label.
  Returns {:base-label <stripped> :unit <canonical-unit>} or nil if no suffix found."
  [raw-label]
  (when-let [s (some-> raw-label str str/trim not-empty)]
    (when-let [[_ base unit-abbr] (re-matches unit-suffix-pattern s)]
      (let [base* (some-> base str/trim not-empty)
            unit-abbr-lower (str/lower-case unit-abbr)
            canonical (some (fn [[abbr canon]]
                              (when (= abbr unit-abbr-lower) canon))
                        unit-abbreviations)]
        (when (and base* canonical)
          {:base-label base*
           :unit canonical})))))

(defn extract-unit
  "Extract unit and strip suffix from a raw label, using qty as a guard.
  Returns {:base-label <label-without-suffix> :unit <canonical-unit-or-nil>}.

  Rules:
  - Detected non-piece suffix (/KG, /GR, /LIT, /L, /PAK) -> always trust.
  - Detected piece suffix (/KO, /CO, /KOM, /PC) + integer qty -> trust, unit = kom.
  - Detected piece suffix + fractional qty -> strip suffix, unit = kg
    (fractional qty means weight-based, the piece suffix was noise on receipt).
  - Trailing pack-count metadata like `24/1` -> strip it, then infer unit from qty.
  - No suffix + integer qty (or nil) -> default to kom.
  - No suffix + fractional qty -> assume kg (weight-based item)."
  [raw-label qty]
  (let [trimmed (some-> raw-label str str/trim not-empty)
        int-qty? (integer-qty? qty)
        inferred-unit (if int-qty? default-unit "kg")]
    (if-not trimmed
      {:base-label raw-label :unit inferred-unit}
      (if-let [{:keys [base-label unit] :as parsed} (parse-unit-suffix trimmed)]
        (if (= "kom" unit)
          (if int-qty?
            parsed
            {:base-label base-label :unit "kg"})
          parsed)
        (if-let [base-label (strip-package-count-suffix trimmed)]
          {:base-label base-label :unit inferred-unit}
          {:base-label trimmed :unit inferred-unit})))))

(defn process-item-unit
  "Add :unit to an extraction item and strip the unit suffix from :raw_label.
  Returns the updated item map."
  [item]
  (let [raw-label (:raw_label item)
        qty (:qty item)
        {:keys [base-label unit]} (extract-unit raw-label qty)]
    (cond-> (assoc item :raw_label base-label)
      unit (assoc :unit unit))))

(defn process-items-units
  "Process all items in an extraction, extracting units and stripping suffixes."
  [items]
  (mapv process-item-unit items))
