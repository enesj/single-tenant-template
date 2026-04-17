(ns app.template.frontend.components.filter.utils
  (:require
    [app.shared.date :as date-utils]
    [app.shared.model-naming :as model-naming]
    [clojure.string :as str]))

;; Filter value formatting utilities

(defn- translated-copy
  "Resolve translated UI text when a translator function is available."
  [t translation-key fallback]
  (if-not t
    fallback
    (let [translated (try
                       (t translation-key)
                       (catch :default _
                         nil))
          translated-str (some-> translated str str/trim)]
      (if (and (seq translated-str)
            (not= translated translation-key)
            (not= translated-str (str translation-key)))
        translated-str
        fallback))))

(defn- normalize-translation-key [translation-key]
  (cond
    (keyword? translation-key) translation-key
    (string? translation-key) (keyword translation-key)
    :else nil))

(defn format-number-range
  "Format a number range filter value for display."
  ([filter-value]
   (format-number-range filter-value nil))
  ([filter-value t]
   (let [min-label (translated-copy t :common/min "Min")
         max-label (translated-copy t :common/max "Max")]
     (cond
       (and (map? filter-value) (:min filter-value) (:max filter-value))
       (str (:min filter-value) " - " (:max filter-value))

       (and (map? filter-value) (:min filter-value))
       (str min-label " " (:min filter-value))

       (and (map? filter-value) (:max filter-value))
       (str max-label " " (:max filter-value))

       :else (str filter-value)))))

(defn parse-date-value
  "Parse a date value that can be Date object or ISO string."
  [date-value]
  (cond
    (instance? js/Date date-value) date-value
    (string? date-value) (try (js/Date. date-value) (catch :default _ nil))
    :else date-value))

(defn format-local-date
  "Format a date-like value as a local calendar date."
  [date-value]
  (some-> date-value
    parse-date-value
    date-utils/format-iso-date))

(defn format-date-range
  "Format a date range filter value for display."
  ([filter-value]
   (format-date-range filter-value nil))
  ([filter-value t]
   (let [from-label (translated-copy t :common/from "From")
         to-label (translated-copy t :common/to "To")]
     (cond
       (and (map? filter-value) (:from filter-value) (:to filter-value))
       (str (format-local-date (:from filter-value))
         " - "
         (format-local-date (:to filter-value)))

       (and (map? filter-value) (:from filter-value))
       (str from-label " " (format-local-date (:from filter-value)))

       (and (map? filter-value) (:to filter-value))
       (str to-label " " (format-local-date (:to filter-value)))

       :else (str filter-value)))))

(defn- translated-field-label
  [t field-def target-id]
  (let [explicit-key (some-> (:label-key field-def) normalize-translation-key)
        common-key (when target-id
                     (keyword "common" (name target-id)))]
    (or (when explicit-key
          (translated-copy t explicit-key nil))
      (when common-key
        (translated-copy t common-key nil)))))

(defn get-field-label
  "Get the display label for a field from entity config. Prefer localized common labels when available."
  ([entity-config field-id]
   (get-field-label entity-config field-id nil))
  ([entity-config field-id t]
   (let [target-id (some-> field-id model-naming/ensure-app-keyword)
         field-def (some (fn [field]
                           (when (= target-id (some-> (:id field) model-naming/ensure-app-keyword))
                             field))
                     (:fields entity-config))]
     (or (translated-field-label t field-def target-id)
       (some-> (:label field-def) str str/trim not-empty)
       (some-> target-id name)
       (when (some? field-id) (name field-id))))))

(defn get-value-label
  "Get display label for a field value, handling different field types."
  [entity-config all-entities field-id value]
  (let [target-id (some-> field-id model-naming/ensure-app-keyword)
        field-def (some (fn [field]
                          (when (= target-id (some-> (:id field) model-naming/ensure-app-keyword))
                            field))
                    (:fields entity-config))
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
  "Format a select/multi-select filter value for display."
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
  "Main function to format any filter value for display."
  ([entity-config all-entities field-id filter-value]
   (format-filter-value entity-config all-entities field-id filter-value nil))
  ([entity-config all-entities field-id filter-value t]
   (cond
     ;; Number range filters
     (and (map? filter-value) (or (:min filter-value) (:max filter-value)))
     (format-number-range filter-value t)

     ;; Date range filters
     (and (map? filter-value) (or (:from filter-value) (:to filter-value)))
     (format-date-range filter-value t)

     ;; Select/multi-select filters
     (or (vector? filter-value) (map? filter-value))
     (format-select-filter entity-config all-entities field-id filter-value)

     ;; Default fallback
     :else (str filter-value))))

(defn date-to-input-value
  "Convert a date to the format expected by HTML date input."
  [date]
  (when date
    (let [parsed-date (parse-date-value date)]
      (when parsed-date
        (date-utils/format-iso-date parsed-date)))))

;; Validation utilities

(defn valid-number-string?
  "Check if a string is a valid number."
  [value]
  (re-matches #"^\d*\.?\d*$" value))

;; Function removed - use app.shared.string/safe-parse-double directly
