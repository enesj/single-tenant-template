(ns app.template.frontend.components.filter.utils
  (:require
    [app.shared.date :as date-utils]
    [clojure.string :as str]))

;; Filter value formatting utilities

(defn format-number-range
  "Format a number range filter value for display"
  [filter-value]
  (cond
    (and (map? filter-value) (:min filter-value) (:max filter-value))
    (str (:min filter-value) " - " (:max filter-value))

    (and (map? filter-value) (:min filter-value))
    (str ">= " (:min filter-value))

    (and (map? filter-value) (:max filter-value))
    (str "<= " (:max filter-value))

    :else (str filter-value)))

(defn format-date-range
  "Format a date range filter value for display"
  [filter-value]
  (cond
    (and (map? filter-value) (:from filter-value) (:to filter-value))
    (str (.toLocaleDateString (:from filter-value)) " - " (.toLocaleDateString (:to filter-value)))

    (and (map? filter-value) (:from filter-value))
    (str "From " (.toLocaleDateString (:from filter-value)))

    (and (map? filter-value) (:to filter-value))
    (str "Until " (.toLocaleDateString (:to filter-value)))

    :else (str filter-value)))

(defn get-field-label
  "Get the display label for a field from entity config"
  [entity-config field-id]
  (let [field-str (name field-id)
        field-def (first (filter #(= (:id %) field-str) (:fields entity-config)))]
    (or (:label field-def) field-str)))

(defn get-value-label
  "Get display label for a field value, handling different field types"
  [entity-config all-entities field-id value]
  (let [field-str (name field-id)
        field-def (first (filter #(= (:id %) field-str) (:fields entity-config)))
        options (get field-def :options)
        foreign-key? (and (sequential? options)
                       (= 2 (count options))
                       (string? (first options)))
        related-entity (when foreign-key? (first options))
        display-field (when foreign-key? (second options))]

    (cond
      ;; For select fields with predefined options in a map
      (and options (map? options) (get options value))
      (get options value)

      ;; For foreign key relationships
      (and foreign-key? related-entity display-field all-entities)
      (if-let [related-obj (first (filter #(= (str (:id %)) (str value)) all-entities))]
        (or (get related-obj display-field) value)
        value)

      ;; Default: just return the value as a string
      :else (str value))))

(defn format-select-filter
  "Format a select/multi-select filter value for display"
  [entity-config all-entities field-id filter-value]
  (cond
    ;; Vector of selections
    (vector? filter-value)
    (if (= (count filter-value) 1)
      ;; Single selection
      (let [val (first filter-value)]
        (if (map? val)
          (:label val)
          (get-value-label entity-config all-entities field-id val)))
      ;; Multiple selections
      (let [labels (map (fn [val]
                          (if (map? val)
                            (:label val)
                            (get-value-label entity-config all-entities field-id val)))
                     filter-value)]
        (str/join ", " labels)))

    ;; Single value
    :else
    (if (map? filter-value)
      (:label filter-value)
      (get-value-label entity-config all-entities field-id filter-value))))

(defn format-filter-value
  "Main function to format any filter value for display"
  [entity-config all-entities field-id filter-value]
  (cond
    ;; Number range filters
    (and (map? filter-value) (or (:min filter-value) (:max filter-value)))
    (format-number-range filter-value)

    ;; Date range filters
    (and (map? filter-value) (or (:from filter-value) (:to filter-value)))
    (format-date-range filter-value)

    ;; Select/multi-select filters
    (or (vector? filter-value) (map? filter-value))
    (format-select-filter entity-config all-entities field-id filter-value)

    ;; Default fallback
    :else (str filter-value)))

;; Date utilities

(defn parse-date-value
  "Parse a date value that can be Date object or ISO string"
  [date-value]
  (cond
    (instance? js/Date date-value) date-value
    (string? date-value) (try (js/Date. date-value) (catch :default _ nil))
    :else date-value))

(defn date-to-input-value
  "Convert a date to the format expected by HTML date input"
  [date]
  (when date
    (let [parsed-date (parse-date-value date)]
      (when parsed-date
        (date-utils/format-iso-date parsed-date)))))

;; Validation utilities

(defn valid-number-string?
  "Check if a string is a valid number"
  [value]
  (re-matches #"^\d*\.?\d*$" value))

;; Function removed - use app.shared.string/safe-parse-double directly
