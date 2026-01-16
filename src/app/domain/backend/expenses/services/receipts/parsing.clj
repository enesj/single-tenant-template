(ns app.domain.backend.expenses.services.receipts.parsing
  "Value parsing and validation utilities for receipt data."
  (:require
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str])
  (:import
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset]))

(def approvable-status? #{"extracted" "review_required"})

(def allowed-currencies #{"BAM" "EUR" "USD"})

(defn blank->nil
  [v]
  (cond
    (nil? v) nil
    (and (string? v) (str/blank? v)) nil
    :else v))

(defn parse-instant!
  "Parse `v` as an Instant.

  Accepts ISO-8601 instants, OffsetDateTime strings, HTML `datetime-local`
  strings (e.g. \"2025-12-12T12:34\"), LocalDate strings, epoch millis.

  Throws ex-info {:status 400 :field field} on invalid input."
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? Instant v) v
      (number? v) (Instant/ofEpochMilli (long v))
      (string? v)
      (or
        (try
          (Instant/parse v)
          (catch Exception _ nil))
        (try
          (-> (OffsetDateTime/parse v) .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDateTime/parse v)
            (.atZone (ZoneId/systemDefault))
            .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDate/parse v)
            (.atStartOfDay ZoneOffset/UTC)
            .toInstant)
          (catch Exception _ nil))
        (throw (ex-info (str "Invalid " (name field))
                 {:status 400
                  :field field
                  :value v})))
      :else
      (throw (ex-info (str "Invalid " (name field))
               {:status 400
                :field field
                :value v})))))

(defn normalize-currency!
  "Normalize/validate currency string.

  Returns an uppercase currency code (e.g. \"BAM\") or nil.
  Throws ex-info {:status 400 :field :currency} when non-blank but invalid."
  [currency]
  (let [currency* (some-> currency blank->nil str str/trim str/upper-case)]
    (cond
      (nil? currency*) nil
      (contains? allowed-currencies currency*) currency*
      :else (throw (ex-info "Invalid currency"
                     {:status 400
                      :field :currency
                      :value currency})))))

(def try-parse-uuid type-conv/try-parse-uuid)

(defn parse-money
  "Coerce user/JSON numeric values into BigDecimal.

  Accepts numbers, BigDecimal, or numeric-ish strings. Returns nil when unparsable."
  [v]
  (cond
    (nil? v) nil
    (instance? java.math.BigDecimal v) v
    (number? v) (bigdec v)
    (string? v)
    (let [s (-> v
              str/trim
              (str/replace #"[^0-9,\.\-]" ""))
          s (cond
              (and (str/includes? s ",") (str/includes? s ".")) (str/replace s "," "")
              (str/includes? s ",") (str/replace s "," ".")
              :else s)]
      (try
        (bigdec s)
        (catch Exception _ nil)))
    :else nil))

(defn lines-total
  [items]
  (when (sequential? items)
    (let [totals (keep (fn [item]
                         (parse-money (:line-total item)))
                   items)]
      (when (seq totals)
        (reduce + 0M totals)))))
