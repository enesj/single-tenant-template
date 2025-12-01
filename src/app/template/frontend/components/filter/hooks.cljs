(ns app.template.frontend.components.filter.hooks
  (:require
    [app.shared.string :as shared-string]
    [app.template.frontend.components.filter.utils :as filter-utils]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.utils.debounce :as debounce]
    [re-frame.core :as rf]
    [uix.core :refer [use-callback use-effect use-state]]))

;; Custom hook for debounced filter updates
(defn use-debounced-filter
  "Custom hook for managing debounced filter updates"
  [entity-type field-id initial-value debounce-ms]
  (let [[local-value, set-local-value] (use-state initial-value)

        ;; Create the filter update function
        update-filter (use-callback
                        (fn [new-value]
                          (when entity-type
                            (let [field-keyword (if (string? field-id) (keyword field-id) field-id)]
                              (if (or (nil? new-value)
                                    (and (string? new-value) (empty? new-value))
                                    (and (map? new-value) (every? nil? (vals new-value))))
                                ;; Clear filter if value is empty/nil
                                (rf/dispatch [::filter-events/clear-filter entity-type field-keyword])
                                ;; Apply filter with new value
                                (rf/dispatch [::filter-events/apply-filter
                                              entity-type
                                              field-keyword
                                              new-value
                                              true])))))
                        [entity-type field-id])

        ;; Create debounced version
        debounced-update (use-callback
                           (fn [value]
                             (let [f (debounce/debounce update-filter debounce-ms)]
                               (f value)))
                           [update-filter debounce-ms])

        ;; Update local value and trigger debounced update
        update-value (use-callback
                       (fn [new-value]
                         (set-local-value new-value)
                         (debounced-update new-value))
                       [debounced-update])]

    {:local-value local-value
     :set-local-value set-local-value
     :update-value update-value}))

;; Custom hook for number range filters
(defn use-number-range-filter
  "Custom hook for managing number range filter state"
  [entity-type field-id filter-min filter-max]
  (let [[local-min, set-local-min] (use-state (str (or filter-min "")))
        [local-max, set-local-max] (use-state (str (or filter-max "")))

        ;; Update filter function
        update-filter (use-callback
                        (fn [new-min new-max]
                          (when entity-type
                            (let [min-num (shared-string/safe-parse-double new-min)
                                  max-num (shared-string/safe-parse-double new-max)
                                  field-keyword (if (string? field-id) (keyword field-id) field-id)]
                              (if (and (nil? min-num) (nil? max-num))
                                ;; Clear filter if both values are nil
                                (rf/dispatch [::filter-events/clear-filter entity-type field-keyword])
                                ;; Apply filter with range
                                (rf/dispatch [::filter-events/apply-filter
                                              entity-type
                                              field-keyword
                                              (cond-> {}
                                                (some? min-num) (assoc :min min-num)
                                                (some? max-num) (assoc :max max-num))
                                              true])))))
                        [entity-type field-id])

        ;; Debounced update
        debounced-update (use-callback
                           (fn [min max]
                             (let [f (debounce/debounce update-filter 500)]
                               (f min max)))
                           [update-filter])

        ;; Handle min change
        handle-min-change (use-callback
                            (fn [e]
                              (let [value (.. e -target -value)]
                                (set-local-min value)
                                (when (filter-utils/valid-number-string? value)
                                  (debounced-update value local-max))))
                            [local-max debounced-update])

        ;; Handle max change
        handle-max-change (use-callback
                            (fn [e]
                              (let [value (.. e -target -value)]
                                (set-local-max value)
                                (when (filter-utils/valid-number-string? value)
                                  (debounced-update local-min value))))
                            [local-min debounced-update])

        ;; Sync local state when props change
        _ (use-effect
            (fn []
              (set-local-min (str (or filter-min "")))
              (set-local-max (str (or filter-max ""))))
            [filter-min filter-max])]

    {:local-min local-min
     :local-max local-max
     :handle-min-change handle-min-change
     :handle-max-change handle-max-change
     :has-values (or (seq local-min) (seq local-max))}))

;; Custom hook for date range filters
(defn use-date-range-filter
  "Custom hook for managing date range filter state"
  [entity-type field-id filter-from filter-to]
  (let [[local-from, set-local-from] (use-state filter-from)
        [local-to, set-local-to] (use-state filter-to)
        [clearing, set-clearing] (use-state false)

        ;; Update filter function
        update-filter (use-callback
                        (fn [new-from new-to]
                          (when (and entity-type (not clearing))
                            (let [field-keyword (if (string? field-id) (keyword field-id) field-id)
                                  date-range (cond-> {}
                                               new-from (assoc :from new-from)
                                               new-to (assoc :to new-to))]
                              (if (or new-from new-to)
                                (rf/dispatch [::filter-events/apply-filter
                                              entity-type
                                              field-keyword
                                              date-range
                                              true])
                                (rf/dispatch [::filter-events/clear-filter
                                              entity-type
                                              field-keyword])))))
                        [entity-type field-id clearing])

        ;; Debounced update
        debounced-update (use-callback
                           (fn [from to]
                             (let [f (debounce/debounce update-filter 300)]
                               (f from to)))
                           [update-filter])

        ;; Handle from date change
        handle-from-change (use-callback
                             (fn [date]
                               (set-local-from date)
                               (debounced-update date local-to))
                             [local-to debounced-update])

        ;; Handle to date change
        handle-to-change (use-callback
                           (fn [date]
                             (set-local-to date)
                             (debounced-update local-from date))
                           [local-from debounced-update])

        ;; Handle clear
        handle-clear (use-callback
                       (fn []
                         (set-clearing true)
                         (set-local-from nil)
                         (set-local-to nil)
                         (when entity-type
                           (rf/dispatch [::filter-events/clear-filter
                                         entity-type
                                         (if (string? field-id) (keyword field-id) field-id)]))
                         (js/setTimeout #(set-clearing false) 50))
                       [entity-type field-id])

        ;; Sync local state when props change
        _ (use-effect
            (fn []
              (when-not clearing
                (set-local-from (filter-utils/parse-date-value filter-from))
                (set-local-to (filter-utils/parse-date-value filter-to))))
            [filter-from filter-to clearing])]

    {:local-from local-from
     :local-to local-to
     :handle-from-change handle-from-change
     :handle-to-change handle-to-change
     :handle-clear handle-clear
     :has-values (or local-from local-to)}))
