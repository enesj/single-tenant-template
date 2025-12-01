(ns app.template.frontend.events.list.filters
  "Filter management for list views - applying, clearing, and modal state"
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]))

;;; -------------------------
;;; Filter Management
;;; -------------------------

(rf/reg-event-db
  ::apply-filter
  common-interceptors
  (fn [db [entity-type field-id value & [keep-modal-open?]]]
    (if (or (nil? entity-type) (nil? field-id))
      (do
        (js/console.error "Filter error: missing" (if entity-type "field-id" "entity-type"))
        ;; Return unchanged database when missing required params
        db)
      (let [should-keep-open? (or (boolean keep-modal-open?)
                                (when (and (boolean? value) (nil? keep-modal-open?))
                                  (boolean value)))

            ;; Convert field-id to keyword for consistency
            field-key (if (keyword? field-id) field-id (keyword field-id))

            ;; Get entity config to access field definitions
            entity-config (get-in db [:ui :entity-configs entity-type])
            field-defs (:fields entity-config)
            field-def (first (filter #(= (:id %) (name field-key)) field-defs))
            input-type (get field-def :input-type)
            options (get field-def :options)
            is-select? (or (= input-type "select") (= input-type "multi-select")
                         (and options (or (map? options) (vector? options))))

            ;; Handle different filter value types - enhance with label info for select fields
            filter-value (cond
                           ;; Map with :min/:max for number ranges - keep as is
                           (and (map? value) (or (contains? value :min) (contains? value :max)))
                           value

                           ;; Date range filter
                           (and (map? value) (or (contains? value :from) (contains? value :to)))
                           value

                           ;; Select field with a vector of values - enhance with labels
                           (and is-select? (vector? value))
                           (let [values-with-labels
                                 (mapv (fn [val]
                                         (if (map? val)
                                           ;; Already has value/label format
                                           val
                                           ;; Simple value, enhance with label if possible
                                           (cond
                                             ;; From predefined options map
                                             (and options (map? options) (get options val))
                                             {:value val :label (get options val)}

                                             ;; Default case - use value as label
                                             :else
                                             {:value val :label (str val)})))
                                   value)]
                             values-with-labels)

                           ;; Single select value - enhance with label
                           is-select?
                           (if (map? value)
                             ;; Already has value/label format
                             value
                             ;; Simple value, enhance with label
                             (cond
                               ;; From predefined options map
                               (and options (map? options) (get options value))
                               {:value value :label (get options value)}

                               ;; Default case - use value as label
                               :else
                               {:value value :label (str value)}))

                           ;; Simple value (string, number, etc.)
                           :else
                           value)

            ;; Update filters map by adding/updating this field's filter
            current-filters (get-in db (conj (paths/list-ui-state entity-type) :filters) {})
            existing (get current-filters field-key)

            ;; Helper to normalize filter value for equality checks
            normalize-val (fn [v]
                            (cond
                              ;; Text
                              (string? v) v
                              ;; Number range/date range
                              (map? v) v
                              ;; Select vector -> set of string values
                              (vector? v) (->> v (map #(if (map? %) (:value %) %)) (map str) set)
                              :else v))

            same-value? (= (normalize-val existing) (normalize-val filter-value))

            updated-filters (cond
                              ;; Remove filter when nil/empty string
                              (or (nil? filter-value)
                                (and (string? filter-value)
                                  (empty? filter-value)))
                              (dissoc current-filters field-key)

                              ;; If unchanged, keep as-is to avoid re-render loops
                              same-value?
                              current-filters

                              ;; Otherwise set/replace
                              :else
                              (assoc current-filters field-key filter-value))

            updated-db (if (identical? updated-filters current-filters)
                         db
                         (assoc-in db
                           (conj (paths/list-ui-state entity-type) :filters)
                           updated-filters))]
        ;; Only close modal if explicitly NOT keeping it open
        (if should-keep-open?
          updated-db
          (assoc-in updated-db [:ui :filter-modal] {:open? false}))))))

(rf/reg-event-db
  ::clear-filter
  common-interceptors
  (fn [db [entity-type field-id]]
    ;; Check if we have the proper parameters
    (if (nil? entity-type)
      db
      (if field-id
        ;; Clear specific field filter
        (let [field-key (if (keyword? field-id) field-id (keyword field-id))
              current-filters (get-in db (conj (paths/list-ui-state entity-type) :filters) {})
              updated-filters (dissoc current-filters field-key)]
          (assoc-in db (conj (paths/list-ui-state entity-type) :filters) updated-filters))
        ;; Clear all filters - when field-id is nil
        (let [updated-db (update-in db
                           (paths/list-ui-state entity-type)
                           dissoc :filters)]
          updated-db)))))

(rf/reg-event-db
  ::set-current-entity-type
  common-interceptors
  (fn [db [entity-type]]
    (assoc-in db [:ui :current-entity-type] entity-type)))

;; Clear filter modal state (useful when navigating between entities)
(rf/reg-event-db
  ::clear-filter-modal
  common-interceptors
  (fn [db _]
    (assoc-in db [:ui :filter-modal] {:open? false})))
