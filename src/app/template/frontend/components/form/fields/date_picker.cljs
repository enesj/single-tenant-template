(ns app.template.frontend.components.form.fields.date-picker
  "Date picker component using React Day Picker"
  (:require
    ["react-day-picker" :refer [DayPicker]]
    [app.shared.date :as date-utils]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.common :as common]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui]]))

;; parse-date-string function moved to utils.date namespace

;; process-highlighted-dates function moved to utils.date namespace

(defui date-picker
  [{:keys [id label selected-date on-date-change error required class inline mode highlighted-dates highlighted-class]}]
  (let [;; Handle different mode types (single vs range)
        is-range-mode? (= mode "range")

        ;; Process date based on mode
        valid-date (if is-range-mode?
                     ;; For range mode, keep the object structure but convert dates
                     (when selected-date
                       (let [^js selected-date selected-date
                             from (when (.-from selected-date)
                                    (if (instance? js/Date (.-from selected-date))
                                      (.-from selected-date)
                                      (js/Date. (.-from selected-date))))
                             to (when (.-to selected-date)
                                  (if (instance? js/Date (.-to selected-date))
                                    (.-to selected-date)
                                    (js/Date. (.-to selected-date))))]
                         #js {:from from :to to}))
                     ;; For single mode, convert to Date object
                     (if (and selected-date
                           (not (js/isNaN (if (instance? js/Date selected-date)
                                            selected-date
                                            (js/Date.parse selected-date)))))
                       (if (instance? js/Date selected-date)
                         selected-date
                         (js/Date. selected-date))
                       nil))

        ;; Format date for display in a user-friendly format (without time)
        format-display-date (fn [date-value]
                              (cond
                                ;; Range mode with from and to dates
                                (and is-range-mode? date-value)
                                (let [^js date-value date-value]
                                  (if (and (.-from date-value) (.-to date-value))
                                    (let [from-str (try (.toLocaleDateString (.-from date-value))
                                                     (catch :default e "?"))
                                          to-str (try (.toLocaleDateString (.-to date-value))
                                                   (catch :default e "?"))]
                                      (str from-str " - " to-str))

                                    ;; Range mode with only from date
                                    (when (.-from date-value)
                                      (try (.toLocaleDateString (.-from date-value))
                                        (catch :default e "Invalid date")))))

                                ;; This condition is now handled in the first range mode case above

                                ;; Single date mode
                                date-value
                                (try (.toLocaleDateString date-value)
                                  (catch :default e "Invalid date"))

                                :else
                                (if is-range-mode? "Select date range" "Select a date")))]

    ($ :div {:class (str "mb-4 ds-form relative" (if inline " flex flex-row items-start gap-4"
                                                   " flex flex-col items-start gap-4"))}
      ($ common/label {:text label
                       :for id
                       :required required
                       :class (str "ds-label" (when inline " mb-0 min-w-[150px] text-left"))})
      ($ :div {:class (when inline "flex-1 text-left ds-table")}
        ($ :div {:class "relative"}
          ;; Button that looks like an input and targets the popover
          ($ button {:btn-type :outline
                     :class (str class)
                     :popover-target "rdp-popover"
                     :style #js {:anchorName "--rdp"}
                     :aria-haspopup "dialog"}
            (format-display-date valid-date))

          ;; Popover with the DayPicker
          ($ :div {:popover "auto"
                   :id "rdp-popover"
                   :class "ds-dropdown p-2 bg-base-100 shadow-lg rounded-box z-50"
                   :style #js {:positionAnchor "--rdp"}}
            ($ :div {:class "p-2"}
              (let [;; Process highlighted dates if provided
                    processed-dates (when highlighted-dates
                                      (date-utils/process-highlighted-dates highlighted-dates))
                    modifier-map (cond-> {}
                                   processed-dates
                                   (assoc :highlighted processed-dates))

                    ;; Define classes for highlighted dates
                    modifier-classes (cond-> {:selected "ds-btn-primary bg-primary text-primary-content"
                                              :today "border border-primary"}
                                       (and highlighted-dates highlighted-class)
                                       (assoc :highlighted (or highlighted-class "ds-bg-secondary text-secondary-content")))]

                ($ DayPicker
                  #js {:mode mode
                       :selected valid-date
                       :onSelect on-date-change
                       :className "ds-react-day-picker"
                       :showOutsideDays false
                       :numberOfMonths 1
                       :fixedWeeks true
                       :captionLayout "buttons"
                       :pagedNavigation true
                       :showWeekNumber true
                       :style #js {:touchAction "manipulation"}
                       ;; Add modifiers for highlighted dates if provided
                       :modifiers (when (not-empty modifier-map) (clj->js modifier-map))
                       :modifiersClassNames (clj->js modifier-classes)
                       :modifiersStyles #js {:selected #js {:transform "scale(1)"}}}))))

          ;; Error message if any
          (when error
            ($ :div {:class "text-error"
                     :role "alert"}
              ($ :div (:message error)))))))))
