(ns app.template.frontend.components.cards
  "Card-based display components for admin interface"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Card Layout Components
;; ============================================================================

(defui glassmorphism-wrapper
  "Reusable glassmorphism card wrapper with gradient background and backdrop blur.

   Props:
   - :children - Content to wrap
   - :gradient-from - Starting gradient color (default: 'from-transparent')
   - :gradient-via - Middle gradient color (default: 'via-primary/10')
   - :gradient-to - End gradient color (default: 'to-transparent')
   - :container-class - Additional classes for outer container
   - :card-class - Additional classes for the glassmorphism card"
  [{:keys [children gradient-from gradient-via gradient-to container-class card-class]
    :or {gradient-from "from-transparent"
         gradient-via "via-primary/10"
         gradient-to "to-transparent"
         container-class ""
         card-class ""}}]
  ($ :div {:class (str "relative " container-class)}
    ($ :div {:class (str "absolute inset-0 bg-gradient-to-r " gradient-from " " gradient-via " " gradient-to " rounded-3xl")})
    ($ :div {:class (str "relative bg-base-100/30 backdrop-blur-sm rounded-3xl p-8 border border-base-300/50 shadow-xl " card-class)}
      children)))

(defui quick-actions-card
  "Reusable quick actions card with gradient styling and action buttons.

   Props:
   - :title - Card title (default: 'Quick Actions')
   - :icon-path - SVG path for the header icon
   - :icon-gradient - Icon gradient classes (default: 'from-accent to-secondary')
   - :bg-gradient - Background gradient classes (default: 'from-accent/10 to-secondary/10')
   - :actions - Vector of action maps with :label, :on-click, :button-class, and :icon-path
   - :footer-stats - Optional map with :label and :value for footer stats
   - :container-class - Additional classes for the container"
  [{:keys [title icon-path icon-gradient bg-gradient actions footer-stats container-class]
    :or {title "Quick Actions"
         icon-path "M13 10V3L4 14h7v7l9-11h-7z"
         icon-gradient "from-accent to-secondary"
         bg-gradient "from-accent/10 to-secondary/10"
         actions []
         container-class ""}}]
  ($ :div {:class (str "bg-gradient-to-br " bg-gradient " rounded-2xl shadow-xl border border-accent/20 p-8 hover:shadow-2xl transition-all duration-300 " container-class)}
    ;; Header with icon and title
    ($ :div {:class "flex items-center gap-3 mb-6"}
      ($ :div {:class (str "w-10 h-10 bg-gradient-to-br " icon-gradient " rounded-lg flex items-center justify-center shadow-md")}
        ($ :svg {:class "w-5 h-5 text-white" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon-path})))
      ($ :h3 {:class "text-2xl font-bold text-base-content"} title))

    ;; Action buttons
    ($ :div {:class "space-y-4"}
      (for [action actions]
        ($ :div {:key (:label action) :class "group"}
          (let [btn-type (case (:button-class action)
                           "ds-btn-primary" :primary
                           "ds-btn-secondary" :secondary
                           "ds-btn-accent" :accent
                           "ds-btn-success" :success
                           "ds-btn-warning" :warning
                           "ds-btn-error" :error
                           "ds-btn-outline" :outline
                           "ds-btn-ghost" :ghost
                           :primary)]
            ($ button {:btn-type btn-type
                       :class "ds-btn-block ds-btn-lg shadow-lg hover:shadow-xl transition-all duration-300 group-hover:scale-[1.02]"
                       :on-click (:on-click action)}
              (when (:icon-path action)
                ($ :svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d (:icon-path action)})))
              (:label action))))))

    ;; Optional footer stats
    (when footer-stats
      ($ :div {:class "mt-6 pt-6 border-t border-base-content/10"}
        ($ :div {:class "flex items-center justify-between text-sm"}
          ($ :span {:class "text-base-content/60"} (:label footer-stats))
          ($ :span {:class "text-base-content font-medium"} (:value footer-stats)))))))

(defui overview-metrics-card
  "Four-metric overview card with color-coded stats.
   Enhanced to use template stats-card where appropriate.

   Props:
   - :title - Card title (default: 'Overview')
   - :subtitle - Card subtitle
   - :metrics - Vector of metric maps with :label, :value, :sub-value, :bg-color, :text-color, :sub-color
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle metrics container-class]
    :or {title "Overview"
         metrics []
         container-class ""}}]
  ($ :div {:class (str "ds-card bg-base-100 shadow-xl p-6 " container-class)}
    ($ :div {:class "mb-4"}
      ($ :h3 {:class "text-lg font-semibold text-gray-900"} title)
      (when subtitle
        ($ :p {:class "text-sm text-gray-600"} subtitle)))

    ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4"}
      (for [metric metrics]
        ($ :div {:key (:label metric) :class (str (:bg-color metric "bg-gray-50") " p-4 rounded-lg")}
          ($ :div {:class (str "text-2xl font-bold " (:text-color metric "text-gray-900"))}
            (:value metric))
          ($ :div {:class (str "text-sm " (:sub-text-color metric "text-gray-600"))} (:label metric))
          (when (:sub-value metric)
            ($ :div {:class (str "text-xs " (:sub-color metric "text-gray-500"))}
              (:sub-value metric))))))))

(defui chart-list-card
  "Consistent card wrapper for charts and lists with optional scrolling.

   Props:
   - :title - Card title
   - :subtitle - Card subtitle
   - :max-height - Optional max height class for scrollable content (e.g. 'max-h-96')
   - :scroll-y - Enable vertical scrolling (default: false)
   - :children - Card content
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle max-height scroll-y children container-class]
    :or {scroll-y false
         container-class ""}}]
  ($ :div {:class (str "ds-card bg-base-100 shadow-xl p-6 " container-class)}
    ($ :div {:class "mb-4"}
      ($ :h3 {:class "text-lg font-semibold text-gray-900"} title)
      (when subtitle
        ($ :p {:class "text-sm text-gray-600"} subtitle)))

    ($ :div {:class (str "space-y-3 "
                      (when max-height max-height)
                      (when scroll-y "overflow-y-auto"))}
      children)))

(defui performance-trends-card
  "Performance trends card with frequency data and error rates.

   Props:
   - :title - Card title (default: 'Performance Trends')
   - :subtitle - Card subtitle
   - :frequency-data - Vector of frequency items with :key, :label, :value, :sub-value
   - :frequency-title - Title for frequency section (default: 'Activity Frequency')
   - :error-data - Vector of error items with :key, :label, :total, :errors, :error-threshold
   - :error-title - Title for error section (default: 'Error Rates')
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle frequency-data frequency-title error-data error-title container-class]
    :or {title "Performance Trends"
         frequency-title "Activity Frequency"
         error-title "Error Rates"
         container-class ""}}]
  ($ chart-list-card
    {:title title
     :subtitle subtitle
     :container-class container-class
     :children
     ($ :div {:class "space-y-4"}
       ;; Frequency Section
       (when (seq frequency-data)
         ($ :div
           ($ :h4 {:class "font-medium text-gray-900 mb-2"} frequency-title)
           (if (empty? frequency-data)
             ($ :p {:class "text-sm text-gray-500"} "No activity recorded")
             ($ :div {:class "space-y-2"}
               (for [item frequency-data]
                 ($ :div {:key (:key item) :class "flex items-center justify-between p-2 bg-gray-50 rounded"}
                   ($ :div {:class "text-sm text-gray-600"} (:label item))
                   ($ :div {:class "flex items-center space-x-4"}
                     ($ :div {:class "text-sm font-medium"} (:value item))
                     (when (:sub-value item)
                       ($ :div {:class "text-xs text-gray-500"} (:sub-value item))))))))))

       ;; Error Rates Section
       (when (seq error-data)
         ($ :div
           ($ :h4 {:class "font-medium text-gray-900 mb-2"} error-title)
           (if (empty? error-data)
             ($ :p {:class "text-sm text-gray-500"} "No errors recorded")
             ($ :div {:class "space-y-2"}
               (for [error error-data]
                 (let [total (:total error 0)
                       errors (:errors error 0)
                       error-rate (if (pos? total) (* 100 (/ errors total)) 0)
                       threshold (:error-threshold error 5)]
                   ($ :div {:key (:key error) :class "flex items-center justify-between p-2 bg-gray-50 rounded"}
                     ($ :div {:class "text-sm font-medium text-gray-900"} (:label error))
                     ($ :div {:class "flex items-center space-x-4"}
                       ($ :div {:class "text-sm"} (str total " calls"))
                       ($ :div {:class (str "text-sm font-medium "
                                         (if (> error-rate threshold) "text-red-600" "text-green-600"))}
                         (str (.toFixed error-rate 1) "% errors")))))))))))}))
