(ns app.template.frontend.components.stats
  "Reusable statistics and data visualization components for multi-tenant dashboards"
  (:require
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Stats Card Component
;; ============================================================================

(defui stats-card
  "Enhanced stats card using DaisyUI components with correct ds- prefix usage.

   Props:
   - :title - The main title text
   - :value - The main value to display
   - :subtitle - Optional subtitle text
   - :color - Background color class for the icon (e.g., 'bg-primary', 'bg-success')
   - :icon - SVG path data for the icon
   - :value-class - Optional custom class for the value display
   - :loading? - Optional loading state to show spinner
   - :accent-color - Optional accent color for the stat value (e.g., 'text-primary', 'text-accent')"
  [{:keys [title value subtitle color icon value-class loading? accent-color]
    :or {accent-color "text-primary"}}]
  ($ :div {:class "ds-card bg-base-100 shadow-xl hover:shadow-2xl transition-all duration-300 border border-base-300/50 hover:scale-[1.02] group"}
    ($ :div {:class "ds-card-body p-6"}
      ;; Header with title and icon
      ($ :div {:class "flex items-start justify-between mb-4"}
        ($ :div {:class "flex-1"}
          ($ :h3 {:class "ds-card-title text-sm font-bold text-base-content/70 uppercase tracking-wider mb-2"} title)
          ($ :div {:class "w-12 h-1 bg-gradient-to-r from-primary to-transparent rounded-full"}))

        ;; Icon using DaisyUI avatar component with proper icon alignment
        ($ :div {:class "flex-shrink-0"}
          (if loading?
            ($ :div {:class "ds-avatar ds-placeholder"}
              ($ :div {:class "w-12 h-12 rounded-xl bg-neutral-focus"}
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-neutral-content"})))
            ($ :div {:class "ds-avatar"}
              ($ :div {:class (str "w-12 h-12 rounded-xl shadow-lg group-hover:shadow-xl transition-shadow duration-300 " color)}
                ($ :div {:class "w-full h-full flex items-center justify-center"}
                  ($ :svg {:class "w-6 h-6 text-white drop-shadow-sm" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                    ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon}))))))))

      ;; Value display with enhanced typography
      ($ :div {:class "mb-3"}
        (if loading?
          ($ :div {:class "ds-skeleton h-10 w-24 rounded-lg"})
          ($ :div {:class (str "text-3xl font-bold tracking-tight " accent-color " group-hover:scale-105 transition-transform duration-300")}
            value)))

      ;; Subtitle using DaisyUI badge
      (when (and subtitle (not loading?))
        ($ :div {:class "ds-card-actions justify-start"}
          ($ :div {:class "ds-badge ds-badge-ghost ds-badge-sm font-medium text-base-content/60"} subtitle)))

      ;; Progress indicator at bottom
      ($ :div {:class "absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-primary/30 via-secondary/30 to-accent/30 group-hover:from-primary/60 group-hover:via-secondary/60 group-hover:to-accent/60 transition-all duration-300 rounded-b-2xl"}))))

;; ============================================================================
;; Trend Indicator Component
;; ============================================================================

(defui trend-indicator
  "Component for displaying growth/decline trends with directional arrows.

   Props:
   - :value - The percentage value (positive for growth, negative for decline)
   - :show-arrow? - Whether to show directional arrow (default: true)
   - :precision - Number of decimal places (default: 1)
   - :positive-color - Color class for positive values (default: 'ds-text-success')
   - :negative-color - Color class for negative values (default: 'ds-text-error')"
  [{:keys [value show-arrow? precision positive-color negative-color]
    :or {show-arrow? true precision 1 positive-color "ds-text-success" negative-color "ds-text-error"}}]
  (let [is-positive (>= value 0)
        color-class (if is-positive positive-color negative-color)
        arrow (if is-positive "📈" "📉")
        formatted-value (.toFixed (js/Number value) precision)]
    ($ :span {:class (str "ds-badge ds-badge-lg ds-badge-outline ds-shadow-sm ds-transition-all ds-duration-200 ds-hover:ds-scale-105 " color-class)}
      (if show-arrow?
        (str arrow " " formatted-value "%")
        (str formatted-value "%")))))

;; ============================================================================
;; Page Header Component
;; ============================================================================

(defui page-header
  "Reusable page header component using DaisyUI components.

   Props:
   - :title - The main page title
   - :subtitle - Optional subtitle text
   - :icon - SVG path data for the icon
   - :icon-color - Gradient colors for the icon (default: 'from-primary to-secondary')
   - :bg-gradient - Background gradient colors (default: 'from-primary/10 to-secondary/10')"
  [{:keys [title subtitle icon icon-color bg-gradient]
    :or {icon "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
         icon-color "from-primary to-secondary"
         bg-gradient "from-primary/10 to-secondary/10"}}]
  ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-8"}
    ($ :div {:class (str "ds-card bg-gradient-to-r " bg-gradient " rounded-2xl p-6 border border-primary/20 shadow-lg hover:shadow-xl transition-all duration-300")}
      ($ :div {:class "ds-card-body p-0"}
        ($ :div {:class "flex items-center gap-4 mb-2"}
          ($ :div {:class "ds-avatar"}
            ($ :div {:class (str "w-12 h-12 rounded-xl bg-gradient-to-br " icon-color " shadow-lg ring-2 ring-white/50")}
              ($ :div {:class "w-full h-full flex items-center justify-center"}
                ($ :svg {:class "w-6 h-6 text-white drop-shadow-sm" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon})))))
          ($ :h1 {:class "ds-card-title text-3xl font-bold text-base-content"} title))
        (when subtitle
          ($ :p {:class "text-base-content/70 text-lg ml-16"} subtitle))))))
