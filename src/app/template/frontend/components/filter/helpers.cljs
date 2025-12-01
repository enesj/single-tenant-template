(ns app.template.frontend.components.filter.helpers
  (:require
    [clojure.string :as str]))

(defn get-field-type-from-spec
  "Extracts the base field type from a field spec"
  [{:keys [field-spec]}]
  ;; Debug logging

;; Extract the input type from the field spec
  (let [input-type (or (:input-type field-spec)
                     (when-let [type-info (:type field-spec)]
                       (if (= type-info "input")
                         (:input-type field-spec)
                         type-info))
                     "text")]
    ;; Debug logging for the extracted input type

    input-type))

(defn get-filter-type
  "Determines the appropriate filter type based on field specification"
  [{:keys [field-spec]}]

  (let [input-type (get-field-type-from-spec {:field-spec field-spec})
        has-options? (boolean (:options field-spec))]

    (cond
      ;; Select/enum fields - either has options or is explicitly a select
      (or has-options? (= "select" input-type))
      :select

      ;; Number fields
      (contains? #{"number" "decimal" "integer"} input-type)
      :number-range

      ;; Date/time fields
      (contains? #{"date" "datetime" "datetime-local"} input-type)
      :date-range

      ;; Text fields (default)
      :else
      :text)))

(defn get-foreign-key-entity
  "Extracts the foreign key entity name from field options"
  [{:keys [field-spec]}]
  (when-let [options (:options field-spec)]
    ;; Foreign key spec format: [:entity-type :display-field]
    (when (and (vector? options)
            (= 2 (count options))
            (keyword? (first options)))                     ;; First element should be a keyword (entity type)
      (first options))))

(defn parse-field-value
  "Safely parses a field value based on its type"
  [{:keys [value field-type]}]
  (case field-type
    :number-range
    (when (some? value)
      (js/parseFloat (str value)))

    :date-range
    (cond
      (instance? js/Date value) value
      (string? value) (let [date (js/Date. value)]
                        (if (js/isNaN (.getTime date))
                          nil
                          date))
      :else nil)

    ;; Default - return as is
    value))

(defn- normalize-field-name [s]
  (-> s
    (clojure.string/lower-case)
    (clojure.string/replace #"-" "_")
    (clojure.string/replace #"\s+" "_")))

(defn- resolve-field-value
  "Resolve a field value from an item while being tolerant to:
   - namespaced keys (e.g., :users/full-name)
   - hyphen vs underscore differences (:full-name vs :full-name)
   - whitespace differences in labels
  Tries direct lookup first, then scans keys by normalized local name."
  [item field-id]
  (let [fld (if (keyword? field-id) field-id (keyword field-id))
        direct (get item fld)
        target (normalize-field-name (name fld))]
    (if (some? direct)
      direct
      (some (fn [[k v]]
              (when (and (keyword? k)
                      (= (normalize-field-name (name k)) target))
                v))
        item))))

(defn matches-filter?
  "Checks if an item matches a filter value based on field type.
  Uses tolerant field resolution so it works with namespaced entity maps
  like {:users/email ...} as well as simple {:email ...}."
  [{:keys [item field-id filter-value filter-type]}]
  (let [field-val (resolve-field-value item field-id)]
    (case filter-type
      ;; Text filter - require 3+ characters
      :text
      (when (and field-val
              (string? filter-value)
              (>= (count filter-value) 3))
        (let [field-str (str/lower-case (str field-val))
              search-str (str/lower-case filter-value)]
          (str/includes? field-str search-str)))

      ;; Number range filter
      :number-range
      (when (and field-val
              (map? filter-value)
              (or (contains? filter-value :min)
                (contains? filter-value :max)))
        (let [field-num (js/parseFloat field-val)
              min-val (:min filter-value)
              max-val (:max filter-value)]
          (and (or (nil? min-val) (>= field-num min-val))
            (or (nil? max-val) (<= field-num max-val)))))

      ;; Date range filter
      :date-range
      (when (and field-val
              (map? filter-value)
              (or (contains? filter-value :from)
                (contains? filter-value :to)))
        (let [field-date (parse-field-value {:value field-val :field-type :date-range})
              from-date (:from filter-value)
              to-date (:to filter-value)]
          (and field-date
            (or (nil? from-date) (>= (.getTime field-date) (.getTime from-date)))
            (or (nil? to-date) (<= (.getTime field-date) (.getTime to-date))))))

      ;; Select filter - check if field value is in selected options
      :select
      (when (and field-val (vector? filter-value))
        ;; Handle both simple values and {:value :label} objects
        (let [selected-values (if (and (seq filter-value) (map? (first filter-value)))
                                ;; Extract values from {:value :label} objects
                                (mapv :value filter-value)
                                ;; Use as-is if simple values
                                filter-value)]
          (some #(= (str field-val) (str %)) selected-values)))

      ;; Default - no match
      false)))

(defn count-matching-items
  "Counts items that match a filter"
  [{:keys [items field-id filter-value filter-type]}]
  (when (and items field-id filter-value)
    ;; For select filters, convert object format to simple values
    (let [actual-filter-value (if (and (= filter-type :select)
                                    (vector? filter-value)
                                    (seq filter-value)
                                    (map? (first filter-value)))
                                ;; Extract values from {:value :label} objects
                                (mapv :value filter-value)
                                ;; Use as-is for other types
                                filter-value)]
      (->> items
        (filter #(matches-filter? {:item %
                                   :field-id field-id
                                   :filter-value actual-filter-value
                                   :filter-type filter-type}))
        count))))

(defn format-filter-value-for-display
  "Formats a filter value for display in the UI"
  [{:keys [filter-value filter-type]}]
  (case filter-type
    :text
    filter-value

    :number-range
    (str (when (:min filter-value) (str "≥ " (:min filter-value)))
      (when (and (:min filter-value) (:max filter-value)) " and ")
      (when (:max filter-value) (str "≤ " (:max filter-value))))

    :date-range
    (let [format-date (fn [date]
                        (when date
                          (.toLocaleDateString date)))]
      (str (when (:from filter-value) (str "from " (format-date (:from filter-value))))
        (when (and (:from filter-value) (:to filter-value)) " to ")
        (when (and (:to filter-value) (not (:from filter-value))) "to ")
        (when (:to filter-value) (format-date (:to filter-value)))))

    :select
    (cond
      ;; Handle vector of objects with :value and :label
      (and (vector? filter-value) (seq filter-value) (map? (first filter-value)))
      (let [labels (mapv :label filter-value)]
        (if (> (count labels) 2)
          (str (count labels) " selected")
          (str/join ", " labels)))

      ;; Handle simple vector
      (vector? filter-value)
      (str (count filter-value) " selected")

      ;; Handle single value
      :else
      (str filter-value))

    ;; Default
    (str filter-value)))
