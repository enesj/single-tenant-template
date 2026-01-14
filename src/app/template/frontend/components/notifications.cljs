(ns app.template.frontend.components.notifications
  "Reusable notification and toast components for consistent UI across all modules"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui use-effect]]))

;; ============================================================================
;; Toast Notification Component
;; ============================================================================

(defui toast-notification
  "Toast-style notification using DaisyUI toast patterns.

  Props:
  - :type - Toast type (:info, :success, :warning, :error)
  - :message - Toast message
  - :duration - Auto-dismiss duration in ms (default: 5000, set to 0 for no auto-dismiss)
  - :position - Toast position ('toast-top toast-end', etc.) default: 'toast-top toast-end'
  - :on-dismiss - Function called when toast is dismissed"
  [{:keys [type message duration position on-dismiss]
    :or {duration 5000
         position "toast-top toast-end"}}]
  (let [alert-class (case type
                      :success "ds-alert-success"
                      :warning "ds-alert-warning"
                      :error "ds-alert-error"
                      "ds-alert-info")]
    (use-effect
      (fn []
        (when (and on-dismiss (pos? duration))
          (let [timer (js/setTimeout on-dismiss duration)]
            (fn [] (js/clearTimeout timer)))))
      [on-dismiss duration])
    ($ :div {:class (str "ds-toast " position)}
      ($ :div {:class (str "ds-alert " alert-class)}
        ($ :span message)
        (when on-dismiss
          ($ button {:btn-type :ghost
                     :class "ds-btn-sm ds-btn-circle"
                     :on-click on-dismiss}
            "✕"))))))

;; ============================================================================
;; Utility Functions
;; ============================================================================
