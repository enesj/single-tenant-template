(ns app.template.frontend.components.filter.date-range-picker
  (:require
    ["react-day-picker" :refer [DayPicker]]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.components.filter.utils :as filter-utils]
    [app.template.frontend.events.list.filters :as filter-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-ref use-state]]))

(defn current-filter-value
  [{:keys [active-filters field-id]}]
  (let [field-key (if (keyword? field-id) field-id (keyword field-id))]
    (or (get active-filters field-key)
      (get active-filters field-id))))

(defn partial-filter?
  [filter-value]
  (= :partial (:selection-state filter-value)))

(defn complete-filter?
  [filter-value]
  (and (map? filter-value)
    (contains? filter-value :from)
    (contains? filter-value :to)
    (not (partial-filter? filter-value))))

(defn selected-picker-value
  [filter-value]
  (cond
    (partial-filter? filter-value)
    (or (:anchor filter-value) (:from filter-value))

    (complete-filter? filter-value)
    #js {:from (filter-helpers/local-start-of-day (:from filter-value))
         :to (filter-helpers/local-start-of-day (:to filter-value))}

    :else nil))

(defn complete-range-middle?
  [filter-value day]
  (when (complete-filter? filter-value)
    (let [from-day (filter-helpers/local-start-of-day (:from filter-value))
          to-day (filter-helpers/local-start-of-day (:to filter-value))
          day-start (filter-helpers/local-start-of-day day)]
      (and from-day
        to-day
        day-start
        (< (.getTime from-day) (.getTime day-start) (.getTime to-day))))))

(defn selection-modifiers
  [filter-value]
  (cond
    (partial-filter? filter-value)
    {:selected [(or (:anchor filter-value) (:from filter-value))]}

    (complete-filter? filter-value)
    (let [from-day (filter-helpers/local-start-of-day (:from filter-value))
          to-day (filter-helpers/local-start-of-day (:to filter-value))]
      {:selected [from-day to-day]
       :range_start [from-day]
       :range_middle (fn [day]
                       (complete-range-middle? filter-value day))
       :range_end [to-day]})

    :else {}))

(defn day-in-range?
  [day from to]
  (let [day-start (filter-helpers/local-start-of-day day)
        range-start (filter-helpers/local-start-of-day from)
        range-end (filter-helpers/local-end-of-day to)]
    (and day-start
      range-start
      range-end
      (<= (.getTime range-start) (.getTime day-start) (.getTime range-end)))))

(defn partial-filter-value
  [clicked-day today]
  (let [day-start (filter-helpers/local-start-of-day clicked-day)]
    {:from day-start
     :to (filter-helpers/local-end-of-day today)
     :anchor day-start
     :selection-state :partial}))

(defn complete-filter-value
  [anchor-day clicked-day]
  (let [anchor-start (filter-helpers/local-start-of-day anchor-day)
        clicked-start (filter-helpers/local-start-of-day clicked-day)
        [from to] (if (<= (.getTime anchor-start) (.getTime clicked-start))
                    [anchor-start clicked-start]
                    [clicked-start anchor-start])]
    {:from from
     :to (filter-helpers/local-end-of-day to)
     :selection-state :complete}))

(defn next-filter-value
  [{:keys [current-filter clicked-day today]}]
  (let [today-end (filter-helpers/local-end-of-day today)
        clicked-start (filter-helpers/local-start-of-day clicked-day)
        anchor (or (:anchor current-filter)
                 (:from current-filter))]
    (cond
      (nil? clicked-start)
      current-filter

      (> (.getTime clicked-start) (.getTime today-end))
      current-filter

      (nil? current-filter)
      (partial-filter-value clicked-day today)

      (partial-filter? current-filter)
      (if (filter-helpers/same-local-day? anchor clicked-day)
        nil
        (complete-filter-value anchor clicked-day))

      (and (complete-filter? current-filter)
        (day-in-range? clicked-day (:from current-filter) (:to current-filter)))
      nil

      :else
      (partial-filter-value clicked-day today))))

(defn local-highlighted-days
  [{:keys [items active-filters field-id]}]
  (let [field-key (if (keyword? field-id) field-id (keyword field-id))
        matches-other-filters? (fn [item]
                                 (every?
                                   (fn [[active-field active-filter]]
                                     (let [active-key (if (keyword? active-field)
                                                        active-field
                                                        (keyword active-field))]
                                       (or (= active-key field-key)
                                         (filter-helpers/matches-filter?
                                           {:item item
                                            :field-id active-key
                                            :filter-value active-filter
                                            :filter-type (filter-helpers/infer-filter-type active-filter)}))))
                                   active-filters))]
    (->> items
      (filter matches-other-filters?)
      (keep (fn [item]
              (some-> (filter-helpers/get-item-field-value item field-key)
                (#(filter-helpers/parse-field-value {:value %
                                                     :field-type :date-range}))
                filter-helpers/local-start-of-day)))
      (reduce (fn [acc day]
                (assoc acc (filter-helpers/local-day-key day) day))
        {})
      vals
      vec)))

(defn server-day->date
  [day-key]
  (when (string? day-key)
    (let [[year-str month-str day-str] (.split day-key "-")
          year (js/parseInt year-str 10)
          month (js/parseInt month-str 10)
          day (js/parseInt day-str 10)]
      (when (every? false? [(js/isNaN year) (js/isNaN month) (js/isNaN day)])
        (js/Date. year (dec month) day 0 0 0 0)))))

(defn browser-timezone
  []
  (or (some-> (js/Intl.DateTimeFormat.) (.resolvedOptions) (.-timeZone))
    "UTC"))

(defn build-highlight-refresh-event
  [refresh-event params]
  (cond
    (keyword? refresh-event)
    [refresh-event params]

    (vector? refresh-event)
    (let [event-id (first refresh-event)
          existing (second refresh-event)]
      (cond
        (nil? event-id) nil
        (map? existing) [event-id (merge existing params)]
        :else [event-id params]))

    :else
    nil))

(defn selection-summary
  [filter-value]
  (cond
    (partial-filter? filter-value)
    (str "Filtering from "
      (filter-utils/format-local-date (:from filter-value))
      " to today. Pick an end date to complete the range.")

    (complete-filter? filter-value)
    (str "Filtering "
      (filter-utils/format-local-date (:from filter-value))
      " to "
      (filter-utils/format-local-date (:to filter-value))
      ".")

    :else
    "Select a start date, then an end date. Dates with records are shaded."))

(defui date-range-picker
  [{:keys [field-id entity-type active-filters items matching-count list-ui-state
           set-filter-from-date set-filter-to-date]}]
  (let [today (js/Date.)
        field-key (if (keyword? field-id) field-id (keyword field-id))
        filter-value (current-filter-value {:active-filters active-filters
                                            :field-id field-id})
        display-month (or (:from filter-value) today)
        [visible-month, set-visible-month] (use-state (filter-helpers/local-start-of-day display-month))
        mode (if (complete-filter? filter-value) "range" "single")
        disabled-after (filter-helpers/local-start-of-day today)
        local-days (local-highlighted-days {:items items
                                            :active-filters active-filters
                                            :field-id field-key})
        server-mode? (= :server (:pagination-mode list-ui-state))
        refresh-event (:refresh-event list-ui-state)
        timezone (browser-timezone)
        server-highlight-keys (or (get-in list-ui-state [:date-highlights field-key])
                                (get-in list-ui-state [:date-highlights (name field-key)]))
        shaded-days (if (and server-mode? (seq server-highlight-keys))
                      (->> server-highlight-keys
                        (keep server-day->date)
                        vec)
                      local-days)
        selection-matchers (selection-modifiers filter-value)
        request-signature (str entity-type "|" (name field-key) "|" timezone "|" (pr-str (dissoc active-filters field-key)))
        last-request-signature (use-ref nil)
        modifier-class-names (clj->js {:highlighted "bg-emerald-100 text-emerald-900 rounded-md"
                                       :has_data "has-data-day"
                                       :selected "bg-primary text-primary-content"
                                       :range_start "bg-primary text-primary-content rounded-l-md"
                                       :range_middle "bg-primary/10 text-base-content"
                                       :range_end "bg-primary text-primary-content rounded-r-md"})
        modifier-styles #js {:selected #js {:transform "scale(1)"}
                             :has_data #js {:boxShadow "inset 0 -0.42rem 0 0 rgba(16, 185, 129, 0.95), inset 0 0 0 2px rgba(16, 185, 129, 0.72)"}}
        handle-select (fn [_range _trigger-day _modifiers _event]
                        nil)
        handle-day-click (fn [day]
                           (let [next-value (next-filter-value {:current-filter filter-value
                                                                :clicked-day day
                                                                :today today})]
                             (when set-filter-from-date
                               (set-filter-from-date (:from next-value)))
                             (when set-filter-to-date
                               (set-filter-to-date (:to next-value)))
                             (if next-value
                               (rf/dispatch [::filter-events/apply-filter entity-type field-key next-value true])
                               (rf/dispatch [::filter-events/clear-filter entity-type field-key]))))]
    (use-effect
      (fn []
        (when (and server-mode?
                refresh-event
                (or (nil? @last-request-signature)
                  (not= request-signature @last-request-signature)))
          (reset! last-request-signature request-signature)
          (when-let [event (build-highlight-refresh-event refresh-event {:highlight-date-field (name field-key)
                                                                         :highlight-timezone timezone})]
            (rf/dispatch event)))
        js/undefined)
      [server-mode? refresh-event request-signature timezone field-key])

    ($ :div {:class "p-4 space-y-3"}
      ($ :div {:class "text-xs text-base-content/70"
               :id (str "filter-date-range-summary-" (name field-key))}
        (selection-summary filter-value))
      ($ :div {:class "rounded-lg border border-base-300 bg-base-100 p-2"
               :id (str "filter-date-range-picker-" (name field-key))}
        ($ DayPicker
          #js {:mode mode
               :month visible-month
               :selected nil
               :onSelect handle-select
               :onMonthChange set-visible-month
               :onDayClick handle-day-click
               :showOutsideDays true
               :fixedWeeks true
               :captionLayout "buttons"
               :disabled #js {:after disabled-after}
               :className "ds-react-day-picker ds-filter-date-range-day-picker"
               :modifiers (clj->js (merge {:highlighted shaded-days
                                           :has_data shaded-days}
                                     selection-matchers))
               :modifiersClassNames modifier-class-names
               :modifiersStyles modifier-styles}))
      (when matching-count
        ($ :div {:class "text-sm text-gray-600"}
          (str "Found " matching-count " matching "
            (if (= matching-count 1) "item" "items")))))))
