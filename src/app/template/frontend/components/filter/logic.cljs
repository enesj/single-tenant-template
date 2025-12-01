(ns app.template.frontend.components.filter.logic
  (:require
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.events.list.crud :as crud-events]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [use-effect]]))

;; ============================================================================
;; State Initialization Logic
;; ============================================================================

(defn- calculate-number-range-defaults
  "Calculate default min/max values for number range filters from entity data"
  [entity-type field-spec all-entities filter-type]
  (if (and entity-type field-spec (= filter-type :number-range) (seq all-entities))
    (let [field-kw (keyword (:id field-spec))
          values (keep #(get % field-kw) all-entities)
          numeric-values (keep #(when (number? %) %) values)]
      (if (seq numeric-values)
        [(apply min numeric-values) (apply max numeric-values)]
        [nil nil]))
    [nil nil]))

(defn parse-initial-number-range
  "Parse initial value for number range filters"
  [initial-value entity-type field-spec all-entities filter-type]
  (if (and (map? initial-value)
        (or (contains? initial-value :min) (contains? initial-value :max)))
    [(when (some? (:min initial-value)) (js/parseFloat (str (:min initial-value))))
     (when (some? (:max initial-value)) (js/parseFloat (str (:max initial-value))))]
    (calculate-number-range-defaults entity-type field-spec all-entities filter-type)))

(defn parse-initial-select-options
  "Parse initial value for select filters into proper format"
  [initial-value foreign-key-entity available-options]
  (if (vector? initial-value)
    ;; Convert simple values to {value, label} format if needed
    (if (and (seq initial-value) (map? (first initial-value)))
      initial-value                                         ;; Already in correct format
      ;; Convert simple values to {value, label} format
      (if (and foreign-key-entity available-options)
        (mapv (fn [val]
                (or (first (filter #(= (:value %) val) available-options))
                  {:value val :label (str val)}))
          initial-value)
        (mapv (fn [v] {:value v :label (str v)}) initial-value)))
    []))

(defn initialize-filter-state
  "Initialize local state for all filter types based on initial value"
  [{:keys [initial-value filter-type entity-type field-spec all-entities foreign-key-entity available-options]}]
  (let [[min-value max-value] (parse-initial-number-range initial-value entity-type field-spec all-entities filter-type)]
    {:filter-text (if (string? initial-value) initial-value "")
     :filter-min min-value
     :filter-max max-value
     :filter-from-date (when (map? initial-value)
                         (filter-helpers/parse-field-value {:value (:from initial-value)
                                                            :field-type :date-range}))
     :filter-to-date (when (map? initial-value)
                       (filter-helpers/parse-field-value {:value (:to initial-value)
                                                          :field-type :date-range}))
     :filter-selected-options (parse-initial-select-options initial-value foreign-key-entity available-options)}))

;; ============================================================================
;; Options Calculation Logic
;; ============================================================================

(defn calculate-foreign-key-options
  "Calculate options for foreign key select fields"
  [field-spec foreign-key-entities]
  (when (seq foreign-key-entities)
    (let [unique-field (second (:options field-spec))]
      (mapv (fn [entity]
              {:value (:id entity)
               :label (or (get entity unique-field)
                        (str (:id entity)))})
        (sort-by unique-field foreign-key-entities)))))

(defn calculate-enum-options
  "Calculate options for enum select fields with predefined options"
  [field-spec]
  (when-let [options (:options field-spec)]
    ;; Check if this is a foreign key spec (vector of 2 elements with first being keyword)
    ;; Foreign key spec format: [:entity-type :display-field]
    (if (and (vector? options)
          (= 2 (count options))
          (keyword? (first options)))
      nil                                                   ;; This is a foreign key spec, not enum options
      ;; Otherwise, these are enum options
      ;; Options should already be in [{:value ... :label ...}] format from EnumField
      options)))

(defn calculate-dynamic-options
  "Calculate options by extracting unique values from current entity data"
  [field-id items]
  (when items
    (let [field-kw (keyword field-id)
          values (keep #(get % field-kw) items)
          unique-values (distinct values)]
      ;; Convert to {value, label} format for simple fields
      (mapv (fn [v] {:value v :label (str v)})
        (sort unique-values)))))

(defn calculate-available-options
  "Calculate available options for select fields based on data source"
  [{:keys [filter-type field-spec foreign-key-entity foreign-key-entities field-id items]}]
  (when (= filter-type :select)
    (cond
      ;; Handle foreign key relationships
      (and foreign-key-entity (seq foreign-key-entities))
      (calculate-foreign-key-options field-spec foreign-key-entities)

      ;; Handle enum fields with predefined options
      (:options field-spec)
      (calculate-enum-options field-spec)

      ;; Default: extract unique values from current entity
      items
      (calculate-dynamic-options field-id items)

      :else
      [])))

;; ============================================================================
;; State Synchronization Logic
;; ============================================================================

(defn sync-state-with-initial-value
  "Synchronize local state with initial value when props change. Runs only when initial props change,
  not on every keystroke, to avoid resetting user input."
  [{:keys [filter-type initial-value available-options field-id entity-type foreign-key-entity
           set-filter-text set-filter-min set-filter-max set-filter-from-date set-filter-to-date
           set-filter-selected-options]}]
  (use-effect
    (fn []
      (case filter-type
        :text
        (set-filter-text (if (string? initial-value) initial-value ""))

        :number-range
        (if (and (map? initial-value) (or (contains? initial-value :min) (contains? initial-value :max)))
          (let [min-val (:min initial-value)
                max-val (:max initial-value)]
            (set-filter-min (when (some? min-val) (js/parseFloat (str min-val))))
            (set-filter-max (when (some? max-val) (js/parseFloat (str max-val)))))
          ;; Clear values when initial-value is nil or empty
          (do
            (set-filter-min nil)
            (set-filter-max nil)))

        :date-range
        (if (and (map? initial-value) (or (contains? initial-value :from) (contains? initial-value :to)))
          (let [from-val (:from initial-value)
                to-val (:to initial-value)]
            (set-filter-from-date (when from-val (filter-helpers/parse-field-value {:value from-val :field-type :date-range})))
            (set-filter-to-date (when to-val (filter-helpers/parse-field-value {:value to-val :field-type :date-range}))))
          ;; Clear values when initial-value is nil or empty
          (do
            (set-filter-from-date nil)
            (set-filter-to-date nil)))

        :select
        (if (vector? initial-value)
          (let [converted-options (parse-initial-select-options initial-value foreign-key-entity available-options)]
            (set-filter-selected-options converted-options))
          ;; Clear values when initial-value is nil or empty
          (set-filter-selected-options []))

        nil)
      ;; No cleanup needed
      (fn [] nil))
    [set-filter-selected-options set-filter-to-date set-filter-from-date set-filter-max set-filter-min set-filter-text available-options field-id entity-type initial-value filter-type foreign-key-entity]))

;; ============================================================================
;; Auto-Apply Effect Logic
;; ============================================================================

(defn- create-debounced-auto-apply
  "Create a debounced auto-apply function with timeout cleanup"
  ([]
   ;; Default values for common use case
   (create-debounced-auto-apply {} 300))
  ([callback delay-ms]
   ;; For simple callback usage
   (let [timeout-id (js/setTimeout callback delay-ms)]
     ;; Return cleanup function
     (fn [] (js/clearTimeout timeout-id))))
  ([entity-type field-id filter-value keep-open? on-apply delay-ms]
   ;; Legacy signature for backward compatibility
   (when (and entity-type field-id on-apply)
     (let [field-name (if (string? field-id) (keyword field-id) field-id)
           timeout-id (js/setTimeout
                        (fn []
                          (on-apply entity-type field-name filter-value keep-open?))
                        delay-ms)]
       ;; Return cleanup function
       (fn []
         (js/clearTimeout timeout-id))))))

(defn use-text-filter-auto-apply
  "Auto-apply text filter when input changes"
  [{:keys [filter-type filter-text entity-type field-id on-apply]}]
  (use-effect
    (fn []
      (when (and (= filter-type :text) (>= (count filter-text) 3) on-apply)
        ;; Return cleanup so the previous timer is cancelled on next render
        (create-debounced-auto-apply entity-type field-id filter-text true on-apply 300)))
    [filter-type filter-text entity-type field-id on-apply]))

(defn use-number-range-auto-apply
  "Auto-apply effect for number range filters"
  [{:keys [filter-type filter-min filter-max entity-type field-id on-apply]}]
  (use-effect
    (fn []
      (when (and (= filter-type :number-range) (or (some? filter-min) (some? filter-max)) on-apply)
        (let [filter-value (cond-> {}
                             (some? filter-min) (assoc :min filter-min)
                             (some? filter-max) (assoc :max filter-max))
              debounced-apply (create-debounced-auto-apply)]
          ;; Return cleanup from debounced apply
          (debounced-apply #(on-apply entity-type field-id filter-value true)))))
    [filter-type filter-min filter-max entity-type field-id on-apply]))

(defn use-date-range-auto-apply
  "Auto-apply effect for date range filters"
  [{:keys [filter-type filter-from-date filter-to-date entity-type field-id on-apply]}]
  (use-effect
    (fn []
      (when (and (= filter-type :date-range) (or filter-from-date filter-to-date) on-apply)
        (let [filter-value (cond-> {}
                             filter-from-date (assoc :from filter-from-date)
                             filter-to-date (assoc :to filter-to-date))
              debounced-apply (create-debounced-auto-apply)]
          ;; Return cleanup from debounced apply
          (debounced-apply #(on-apply entity-type field-id filter-value true)))))
    [filter-type filter-from-date filter-to-date entity-type field-id on-apply]))

;; ============================================================================
;; Matching Count Calculation
;; ============================================================================

(defn calculate-matching-count
  "Calculate matching count for all filter types"
  [{:keys [filter-type items field-id filter-text filter-min filter-max filter-from-date filter-to-date filter-selected-options]}]
  (cond
    ;; Text filter with 3+ characters
    (and (= filter-type :text) (>= (count filter-text) 3))
    (filter-helpers/count-matching-items {:items items
                                          :field-id field-id
                                          :filter-value filter-text
                                          :filter-type filter-type})

    ;; Number range filter with values
    (and (= filter-type :number-range) (or (some? filter-min) (some? filter-max)))
    (let [filter-value (cond-> {}
                         (some? filter-min) (assoc :min filter-min)
                         (some? filter-max) (assoc :max filter-max))]
      (filter-helpers/count-matching-items {:items items
                                            :field-id field-id
                                            :filter-value filter-value
                                            :filter-type filter-type}))

    ;; Date range filter with dates
    (and (= filter-type :date-range) (or filter-from-date filter-to-date))
    (let [filter-value (cond-> {}
                         filter-from-date (assoc :from filter-from-date)
                         filter-to-date (assoc :to filter-to-date))]
      (filter-helpers/count-matching-items {:items items
                                            :field-id field-id
                                            :filter-value filter-value
                                            :filter-type filter-type}))

    ;; Select filter with selected options
    (and (= filter-type :select) (seq filter-selected-options))
    (let [selected-values (mapv :value filter-selected-options)]
      (filter-helpers/count-matching-items {:items items
                                            :field-id field-id
                                            :filter-value selected-values
                                            :filter-type filter-type}))

    ;; No active filter
    :else nil))

;; ============================================================================
;; Entity Fetching Logic
;; ============================================================================

(defn use-entity-fetching
  "Effect hook to fetch entities and related foreign key entities when needed.
  Only dispatches when data appears absent to avoid re-fetch loops."
  [entity-type foreign-key-entity have-entities? have-foreign?]
  (use-effect
    (fn []
      (when (and entity-type (keyword? entity-type) (not have-entities?))
        (println "📤 FILTER ENTITY-FETCHING: Dispatching fetch-entities for:" entity-type)
        (rf/dispatch [::crud-events/fetch-entities entity-type]))
      ;; If this is a foreign key field, fetch the related entities (when missing)
      (when (and foreign-key-entity (keyword? foreign-key-entity) (not have-foreign?))
        (log/info "Fetching foreign key entities:" foreign-key-entity)
        (println "📤 FILTER FOREIGN-KEY: Dispatching fetch-entities for:" foreign-key-entity)
        (rf/dispatch [::crud-events/fetch-entities foreign-key-entity]))
      ;; Cleanup function
      (fn [] nil))
    [entity-type foreign-key-entity have-entities? have-foreign?]))

;; ============================================================================
;; Debug Logging
;; ============================================================================

(defn use-debug-logging
  "Effect hook for debug logging filter information"
  [filter-type field-id foreign-key-entity foreign-key-entities]
  (use-effect
    (fn []
      (log/info "Filter type:" filter-type "for field:" field-id)
      (when foreign-key-entity
        (log/info "Foreign key entity:" foreign-key-entity)
        (log/info "Foreign key entities count:" (count (or foreign-key-entities []))))
      (fn [] nil))
    [filter-type field-id foreign-key-entity foreign-key-entities]))
