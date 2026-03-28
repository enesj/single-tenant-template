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
  "Lowercase abbreviation → canonical unit. Longest-first so /KOM matches before /KO."
  [["kom" "kom"]
   ["ko"  "kom"]
   ["pc"  "kom"]
   ["kg"  "kg"]
   ["gr"  "g"]
   ["lit" "l"]
   ["pak" "pak"]])

(def ^:private unit-suffix-pattern
  "Regex matching a trailing unit suffix.
  Groups: (1) everything before the slash, (2) the unit abbreviation.
  Allows optional whitespace before slash and optional tax marker like (E) after."
  #"(?i)^(.*?)\s*/\s*(kom|ko|pc|kg|gr|lit|pak)\s*(?:\([A-Z]\))?\s*$")

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
  - Detected non-piece suffix (/KG, /GR, /LIT, /PAK) → always trust.
  - Detected piece suffix (/KO, /KOM, /PC) + integer qty → trust, unit = kom.
  - Detected piece suffix + fractional qty → strip suffix, unit = kg
    (fractional qty means weight-based, the /KO was noise on receipt).
  - No suffix + integer qty (or nil) → default to kom.
  - No suffix + fractional qty → assume kg (weight-based item)."
  [raw-label qty]
  (let [trimmed (some-> raw-label str str/trim not-empty)
        int-qty? (integer-qty? qty)]
    (if-not trimmed
      {:base-label raw-label :unit (if int-qty? default-unit "kg")}
      (if-let [{:keys [base-label unit] :as parsed} (parse-unit-suffix trimmed)]
        (if (= "kom" unit)
          ;; Piece unit detected — only trust if qty is integer
          (if int-qty?
            parsed
            ;; Fractional qty + /KO suffix → weight-based, strip suffix, assume kg
            {:base-label base-label :unit "kg"})
          ;; Non-piece unit (kg, g, l, pak) — always trust
          parsed)
        ;; No suffix detected — default based on qty
        {:base-label trimmed :unit (if int-qty? default-unit "kg")}))))

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
