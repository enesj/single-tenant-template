(ns app.template.frontend.components.table-headers
  "Reusable table header components for consistent UI across all modules"
  (:require
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Table Header Component
;; ============================================================================

(defui table-header
  "Reusable table header component with consistent styling for tables.

   Props:
   - :title - Header title
   - :badge-text - Badge text
   - :color - Color theme ('error', 'info', 'warning', 'success', 'primary', 'secondary', 'accent')
   - :icon-gradient - CSS classes for icon gradient (default: based on color)
   - :container-class - Additional classes for the container
   - :badge-class - Additional classes for the badge"
  [{:keys [title badge-text color icon-gradient container-class badge-class]
    :or {color "info"
         container-class ""
         badge-class ""}}]
  (let [default-gradient (case color
                           :error "from-error to-error/70"
                           :warning "from-warning to-warning/70"
                           :success "from-success to-success/70"
                           :primary "from-primary to-primary/70"
                           :secondary "from-secondary to-secondary/70"
                           :accent "from-accent to-accent/70"
                           "from-info to-info/70")
        final-gradient (or icon-gradient default-gradient)]
    ($ :div {:class (str "flex items-center justify-between mb-6 " container-class)}
      ($ :h3 {:class "ds-card-title text-xl flex items-center gap-2"}
        ($ :div {:class (str "w-2 h-8 bg-gradient-to-b " final-gradient " rounded-full")})
        title)
      ($ :div {:class (str "ds-badge ds-badge-" color " ds-badge-outline " badge-class)}
        badge-text))))

;; ============================================================================
;; Enhanced Table Header Component
;; ============================================================================

(defui enhanced-table-header
  "Enhanced table header with additional features like subtitle and actions.

   Props:
   - :title - Header title
   - :subtitle - Optional subtitle text
   - :badge-text - Badge text
   - :color - Color theme
   - :icon-gradient - CSS classes for icon gradient
   - :actions - Vector of action elements to display on the right
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle badge-text color icon-gradient actions container-class]
    :or {color "info"
         container-class ""}}]
  (let [default-gradient (case color
                           :error "from-error to-error/70"
                           :warning "from-warning to-warning/70"
                           :success "from-success to-success/70"
                           :primary "from-primary to-primary/70"
                           :secondary "from-secondary to-secondary/70"
                           :accent "from-accent to-accent/70"
                           "from-info to-info/70")
        final-gradient (or icon-gradient default-gradient)]
    ($ :div {:class (str "flex items-center justify-between mb-6 " container-class)}
      ;; Left side: title and subtitle
      ($ :div {:class "flex-1"}
        ($ :h3 {:class "ds-card-title text-xl flex items-center gap-2"}
          ($ :div {:class (str "w-2 h-8 bg-gradient-to-b " final-gradient " rounded-full")})
          title)
        (when subtitle
          ($ :p {:class "text-base-content/70 mt-1"} subtitle)))

      ;; Right side: badge and actions
      ($ :div {:class "flex items-center gap-3"}
        ($ :div {:class (str "ds-badge ds-badge-" color " ds-badge-outline")}
          badge-text)
        (when actions
          ($ :div {:class "flex items-center gap-2"}
            actions))))))

;; ============================================================================
;; Utility Functions
;; ============================================================================

(defn get-default-gradient
  "Get default gradient classes for a given color theme"
  [color]
  (case color
    :error "from-error to-error/70"
    :warning "from-warning to-warning/70"
    :success "from-success to-success/70"
    :primary "from-primary to-primary/70"
    :secondary "from-secondary to-secondary/70"
    :accent "from-accent to-accent/70"
    "from-info to-info/70"))

(defn create-color-class
  "Create DaisyUI color class for table elements"
  [color element-type]
  (case element-type
    :badge (str "ds-badge-" color)
    :text (str "text-" color)
    :bg (str "bg-" color)
    :border (str "border-" color)
    (str "ds-badge-" color)))
