(ns app.template.frontend.components.advanced-fields
  "Shared advanced field components with template integration and DaisyUI styling"
  (:require
    [app.template.frontend.utils.formatting :as formatting]
    [uix.core :refer [$ defui]]))

;; ========================================================================
;; Advanced Field Display Components
;; ========================================================================

(defui status-badge
  "Renders a status as a colored badge using DaisyUI"
  [{:keys [text class]}]
  ($ :span {:class (str "ds-badge " class)} text))

(defui health-score-bar
  "Renders a health score as a progress bar with color coding"
  [{:keys [value max class label]}]
  ($ :div {:class "flex items-center gap-2"}
    ($ :div {:class "flex-1"}
      ($ :progress {:class (str "ds-progress " class)
                    :value value
                    :max max}))
    ($ :span {:class "text-sm font-medium"} label)))

(defui risk-indicator
  "Renders a risk score with appropriate styling and animation"
  [{:keys [value level class]}]
  (let [risk-text (case level
                    :low "Low Risk"
                    :medium "Medium Risk"
                    :high "High Risk"
                    :critical "CRITICAL"
                    "Unknown")]
    ($ :div {:class "flex items-center gap-2"}
      ($ :span {:class (str "ds-badge " class)} risk-text)
      ($ :span {:class "text-xs text-gray-500"} (str "(" value ")")))))

(defui trend-display
  "Renders a trend with arrow icon and color coding"
  [{:keys [value trend icon class]}]
  ($ :div {:class "flex items-center gap-1"}
    ($ :span {:class "text-lg"} icon)
    ($ :span {:class (str "text-sm font-medium " class)}
      (if (number? value)
        (formatting/format-percentage value)
        (str value)))))

(defui priority-badge
  "Renders support priority with appropriate urgency styling"
  [{:keys [level]}]
  (let [config (case level
                 :critical {:text "CRITICAL" :class "ds-badge-error animate-pulse"}
                 :high {:text "High" :class "ds-badge-warning"}
                 :medium {:text "Medium" :class "ds-badge-info"}
                 :low {:text "Low" :class "ds-badge-success"}
                 {:text "Normal" :class "ds-badge-neutral"})]
    ($ :span {:class (str "ds-badge " (:class config))} (:text config))))

(defui currency-display
  "Renders currency values with proper formatting using template utility"
  [{:keys [value currency]}]
  ($ :span {:class "font-mono text-sm"}
    (formatting/format-currency value)))

(defui date-countdown
  "Shows a date with countdown to deadline"
  [{:keys [date]}]
  (let [now (js/Date.)
        target-date (js/Date. date)
        diff-days (.floor js/Math (/ (- target-date now) (* 1000 60 60 24)))]
    ($ :div {:class "flex flex-col"}
      ($ :span {:class "text-sm"} (.toLocaleDateString target-date))
      ($ :span {:class (str "text-xs "
                         (cond
                           (< diff-days 0) "text-red-500"
                           (< diff-days 7) "text-orange-500"
                           :else "text-green-500"))}
        (cond
          (< diff-days 0) (str (Math/abs diff-days) " days overdue")
          (= diff-days 0) "Today"
          (= diff-days 1) "Tomorrow"
          :else (str diff-days " days left"))))))

;; ========================================================================
;; Enhanced Admin Dashboard Cards
;; ========================================================================

(defui metric-card
  "Card component for displaying computed metrics with trend indicators"
  [{:keys [title value trend-value trend-direction icon background-color]}]
  ($ :div {:class (str "ds-card bg-base-100 shadow-xl " background-color)}
    ($ :div {:class "ds-card-body"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div
          ($ :h2 {:class "ds-card-title text-sm"} title)
          ($ :p {:class "text-2xl font-bold"} value))
        ($ :div {:class "text-3xl opacity-20"} icon))

      (when trend-value
        ($ :div {:class "flex items-center gap-1 mt-2"}
          ($ :span {:class (case trend-direction
                             :up "text-success"
                             :down "text-error"
                             "text-base-content")}
            (case trend-direction
              :up "↗"
              :down "↘"
              "→"))
          ($ :span {:class "text-xs"}
            (str trend-value "% vs last period")))))))
