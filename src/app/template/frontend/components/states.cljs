(ns app.template.frontend.components.states
  "Loading, empty, and error state components for admin interface"
  (:require
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Loading State Components
;; ============================================================================

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

;; ============================================================================
;; Generic State Helpers
;; ============================================================================


