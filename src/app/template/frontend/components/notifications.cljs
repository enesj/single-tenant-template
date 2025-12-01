(ns app.template.frontend.components.notifications
  "Reusable notification and toast components for consistent UI across all modules"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Notification Banner Component
;; ============================================================================

(defui notification-banner
  "General purpose notification banner with type variations.

   Props:
   - :type - Notification type (:info, :success, :warning, :error)
   - :title - Notification title
   - :message - Notification message
   - :dismissible? - Whether the notification can be dismissed (default: true)
   - :on-dismiss - Function called when notification is dismissed
   - :action-button - Optional action button map with :label, :on-click, :class
   - :container-class - Additional container classes"
  [{:keys [type title message dismissible? on-dismiss action-button container-class]
    :or {type :info
         dismissible? true
         container-class ""}}]
  (let [colors (case type
                 :success {:bg "bg-green-50" :border "border-green-200" :text "text-green-800" :icon "✅"}
                 :warning {:bg "bg-yellow-50" :border "border-yellow-200" :text "text-yellow-800" :icon "⚠️"}
                 :error {:bg "bg-red-50" :border "border-red-200" :text "text-red-800" :icon "❌"}
                 {:bg "bg-blue-50" :border "border-blue-200" :text "text-blue-800" :icon "ℹ️"})]
    ($ :div {:class (str (:bg colors) " " (:border colors) " border-l-4 p-4 " container-class)}
      ($ :div {:class "flex"}
        ($ :div {:class "flex-shrink-0"}
          ($ :span {:class "text-lg"} (:icon colors)))
        ($ :div {:class "ml-3 flex-1"}
          (when title
            ($ :h3 {:class (str "text-sm font-medium " (:text colors))} title))
          ($ :div {:class (str "text-sm " (:text colors) (when title " mt-1"))}
            message)
          (when action-button
            ($ :div {:class "mt-2"}
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
        (when dismissible?
          ($ :div {:class "ml-auto pl-3"}
            ($ :div {:class "-mx-1.5 -my-1.5"}
              ($ :button {:type "button"
                          :class (str "inline-flex rounded-md p-1.5 " (:text colors) " hover:bg-gray-100")
                          :on-click on-dismiss}
                ($ :span {:class "sr-only"} "Dismiss")
                ($ :span {:class "text-sm"} "✕")))))))))

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

(defn get-notification-colors
  "Get color configuration for notification types"
  [type]
  (case type
    :success {:bg "bg-green-50" :border "border-green-200" :text "text-green-800" :icon "✅"}
    :warning {:bg "bg-yellow-50" :border "border-yellow-200" :text "text-yellow-800" :icon "⚠️"}
    :error {:bg "bg-red-50" :border "border-red-200" :text "text-red-800" :icon "❌"}
    {:bg "bg-blue-50" :border "border-blue-200" :text "text-blue-800" :icon "ℹ️"}))

(defn create-toast-position-class
  "Convert toast position string to DaisyUI class"
  [position]
  (str/replace position " " "-"))
