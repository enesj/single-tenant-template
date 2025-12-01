(ns app.template.frontend.components.states
  "Loading, empty, and error state components for admin interface"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.messages :refer [error-alert]]
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Loading State Components
;; ============================================================================

(defui enhanced-loading-state
  "Enhanced loading state using template patterns with customizable messages.
   Upgraded to leverage DaisyUI loading components for consistency.

   Props:
   - :title - Main loading message (default: 'Loading data...')
   - :subtitle - Secondary loading message (default: 'Please wait while we fetch your information')
   - :spinner-color - Spinner color theme (default: 'text-primary')
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle spinner-color container-class]
    :or {title "Loading data..."
         subtitle "Please wait while we fetch your information"
         spinner-color "text-primary"
         container-class ""}}]
  ($ :div {:class (str "px-4 sm:px-6 lg:px-8 mt-12 text-center " container-class)}
    ($ :div {:class "bg-base-100/50 backdrop-blur-sm rounded-2xl p-12 shadow-xl border border-base-300"}
      ($ :div {:class (str "ds-loading ds-loading-spinner ds-loading-lg " spinner-color " mb-4")})
      ($ :p {:class "text-xl font-medium text-base-content/70"} title)
      ($ :p {:class "text-base-content/50 text-sm mt-2"} subtitle))))

;; ============================================================================
;; Activity State Components
;; ============================================================================

(defui activity-loading-state
  "Loading state component for activity tables.

   Props:
   - :message - Main loading message
   - :sub-message - Secondary loading message"
  [{:keys [message sub-message]
    :or {message "Loading recent activity..."
         sub-message "Fetching latest system events"}}]
  ($ :div {:class "text-center py-12"}
    ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg text-info mb-4"})
    ($ :p {:class "text-xl font-medium text-base-content/70"} message)
    ($ :p {:class "text-base-content/50 text-sm mt-2"} sub-message)))

(defui activity-empty-state
  "Empty state component for activity tables.

   Props:
   - :icon - Emoji or icon to display
   - :title - Main empty state message
   - :subtitle - Secondary message
   - :badge-text - Text for the status badge"
  [{:keys [icon title subtitle badge-text]
    :or {icon "📭"
         title "No recent activity"
         subtitle "System has been quiet"
         badge-text "All Systems Normal"}}]
  ($ :div {:class "text-center py-16"}
    ($ :div {:class "text-6xl mb-4 opacity-50"} icon)
    ($ :p {:class "text-xl text-base-content/70 font-medium"} title)
    ($ :p {:class "text-base-content/50 text-sm mt-2"} subtitle)
    ($ :div {:class "mt-6"}
      ($ :div {:class "ds-badge ds-badge-success"} badge-text))))

;; ============================================================================
;; Payment State Components (using template alerts)
;; ============================================================================

(defui payment-loading-state
  "Loading state for payment tables using consistent styling.

   Props:
   - :message - Main loading message
   - :sub-message - Secondary loading message
   - :color - Spinner color theme (default: 'text-error')"
  [{:keys [message sub-message color]
    :or {message "Loading failed payments..."
         sub-message "Checking for payment issues"
         color "text-error"}}]
  ($ :div {:class "text-center py-12"}
    ($ :div {:class (str "ds-loading ds-loading-spinner ds-loading-lg " color " mb-4")})
    ($ :p {:class "text-xl font-medium text-base-content/70"} message)
    ($ :p {:class "text-base-content/50 text-sm mt-2"} sub-message)))

(defui payment-success-state
  "Success state for payments using template success patterns.
   Leverages template success-alert styling for consistency.

   Props:
   - :icon - Emoji or icon to display
   - :title - Main success message
   - :subtitle - Secondary message
   - :badge-text - Text for the status badge"
  [{:keys [icon title subtitle badge-text]
    :or {icon "✅"
         title "No failed payments!"
         subtitle "All payments are processing successfully"
         badge-text "All Systems Healthy"}}]
  ($ :div {:class "text-center py-16"}
    ($ :div {:class "text-6xl mb-4 opacity-50"} icon)
    ($ :p {:class "text-xl text-base-content/70 font-medium"} title)
    ($ :p {:class "text-base-content/50 text-sm mt-2"} subtitle)
    ($ :div {:class "mt-6"}
      ($ :div {:class "ds-badge ds-badge-success"} badge-text))))

;; ============================================================================
;; Generic State Helpers
;; ============================================================================

(defui generic-loading-state
  "Generic loading state that can be customized for any context.
   Uses template loading patterns.

   Props:
   - :size - Loading spinner size ('sm', 'md', 'lg', 'xl') default: 'lg'
   - :color - Color theme ('primary', 'secondary', 'accent', 'info', 'success', 'warning', 'error')
   - :message - Optional loading message
   - :container-class - Additional container classes"
  [{:keys [size color message container-class]
    :or {size "lg"
         color "primary"
         container-class ""}}]
  ($ :div {:class (str "flex flex-col items-center justify-center py-8 " container-class)}
    ($ :div {:class (str "ds-loading ds-loading-spinner ds-loading-" size " text-" color " mb-3")})
    (when message
      ($ :p {:class "text-base-content/70 text-sm"} message))))

(defui generic-empty-state
  "Generic empty state that can be customized for any context.

   Props:
   - :icon - Icon/emoji to display
   - :title - Main message
   - :subtitle - Optional secondary message
   - :action-button - Optional action button map with :label, :on-click, :class
   - :container-class - Additional container classes"
  [{:keys [icon title subtitle action-button container-class]
    :or {icon "📄"
         title "No data available"
         container-class ""}}]
  ($ :div {:class (str "text-center py-12 " container-class)}
    ($ :div {:class "text-5xl mb-4 opacity-40"} icon)
    ($ :h3 {:class "text-lg font-medium text-base-content/80 mb-2"} title)
    (when subtitle
      ($ :p {:class "text-base-content/60 text-sm mb-4"} subtitle))
    (when action-button
      (let [btn-type (case (:class action-button)
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
                   :class "ds-btn-sm"
                   :on-click (:on-click action-button)}
          (:label action-button))))))

(defui error-state
  "Generic error state component using template error-alert.

   Props:
   - :error - Error object or string
   - :title - Optional error title
   - :retry-fn - Optional retry function
   - :container-class - Additional container classes"
  [{:keys [error title retry-fn container-class]
    :or {title "Something went wrong"
         container-class ""}}]
  ($ :div {:class (str "py-8 " container-class)}
    ($ error-alert {:error (if (string? error)
                             {:message title :details {:message error}}
                             error)
                    :entity-name "generic"})
    (when retry-fn
      ($ :div {:class "flex justify-center mt-4"}
        ($ button {:btn-type :outline
                   :class "ds-btn-sm"
                   :on-click retry-fn}
          "🔄 Try Again")))))
