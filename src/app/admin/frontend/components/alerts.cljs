(ns app.admin.frontend.components.alerts
  "Alert and notification components for admin interface"
  (:require
    [app.template.frontend.components.cards :refer [chart-list-card]]
    [app.template.frontend.components.messages :refer [error-alert
                                                       success-alert]]
    [app.template.frontend.components.stats :as template-stats]
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Page Header Components (Enhanced with Template Integration)
;; ============================================================================


(defui simple-page-header
  "Simple page header with title and description.
   Enhanced to optionally use template page-header for consistency.

   Props:
   - :title - Page title
   - :description - Page description
   - :use-template? - Whether to use template page-header (default: false for backward compatibility)
   - :icon - Icon path for template header (when use-template? is true)
   - :container-class - Additional classes for the container"
  [{:keys [title description use-template? icon container-class]
    :or {use-template? false
         container-class ""}}]
  (if use-template?
    ;; Use template page-header for enhanced styling
    ($ template-stats/page-header {:title title
                                   :subtitle description
                                   :icon icon})
    ;; Original simple implementation for backward compatibility
    ($ :div {:class container-class}
      ($ :h1 {:class "text-2xl font-bold text-gray-900"} title)
      (when description
        ($ :p {:class "text-gray-600"} description)))))

;; ============================================================================
;; Alert Section Components
;; ============================================================================

(defui alert-section
  "Reusable alert section for displaying warning/info content.
   Enhanced to use template error-alert and success-alert where appropriate.

   Props:
   - :title - Section title
   - :items - Vector of alert items with :key, :title, :subtitle, :value, :date, :border-color, :bg-color, :type
   - :empty-message - Message to show when no items (default: 'No items to display')
   - :use-template-alerts? - Whether to use template alert components (default: false)
   - :container-class - Additional classes for the container"
  [{:keys [title items empty-message use-template-alerts? container-class]
    :or {empty-message "No items to display"
         items []
         use-template-alerts? false
         container-class ""}}]
  ($ :div {:class container-class}
    ($ :h4 {:class "font-medium text-gray-900 mb-2"} title)
    (if (empty? items)
      ($ :p {:class "text-sm text-gray-500"} empty-message)
      ($ :div {:class "space-y-2"}
        (for [item items]
          (if (and use-template-alerts? (contains? item :type))
            ;; Use template alerts for error/success types
            (case (:type item)
              :error ($ error-alert {:key (:key item)
                                     :error {:message (:title item)
                                             :details {:message (:subtitle item)}}
                                     :entity-name "alert-section"})
              :success ($ success-alert {:key (:key item)
                                         :message (:title item)})
              ;; Fallback to original for other types
              ($ :div {:key (:key item)
                       :class (str "p-3 " (:bg-color item "bg-yellow-50")
                                " border-l-4 " (:border-color item "border-yellow-400")
                                " rounded")}
                ($ :div {:class "flex justify-between"}
                  ($ :div
                    ($ :div {:class "font-medium"} (:title item))
                    (when (:subtitle item)
                      ($ :div {:class "text-sm text-gray-600"} (:subtitle item))))
                  ($ :div {:class "text-right"}
                    (when (:value item)
                      ($ :div {:class "font-bold text-red-600"} (:value item)))
                    (when (:date item)
                      ($ :div {:class "text-xs text-gray-500"} (:date item)))))))
            ;; Original implementation
            ($ :div {:key (:key item)
                     :class (str "p-3 " (:bg-color item "bg-yellow-50")
                              " border-l-4 " (:border-color item "border-yellow-400")
                              " rounded")}
              ($ :div {:class "flex justify-between"}
                ($ :div
                  ($ :div {:class "font-medium"} (:title item))
                  (when (:subtitle item)
                    ($ :div {:class "text-sm text-gray-600"} (:subtitle item))))
                ($ :div {:class "text-right"}
                  (when (:value item)
                    ($ :div {:class "font-bold text-red-600"} (:value item)))
                  (when (:date item)
                    ($ :div {:class "text-xs text-gray-500"} (:date item))))))))))))

;; ============================================================================
;; Status Section Components
;; ============================================================================

(defui status-section
  "Status section with success rates, recent events, and failures.
   Uses chart-list-card for consistent styling.

   Props:
   - :title - Section title
   - :subtitle - Section subtitle
   - :success-rate - Map with :total, :successful, :label, :success-threshold (default: 90)
   - :recent-events - Vector of recent event items with :key, :title, :subtitle, :date, :bg-color, :border-color
   - :failures - Vector of failure items with :key, :title, :subtitle, :value, :date
   - :use-template-alerts? - Whether to use template alerts for events and failures
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle success-rate recent-events failures use-template-alerts? container-class]
    :or {use-template-alerts? false
         container-class ""}}]
  ($ chart-list-card
    {:title title
     :subtitle subtitle
     :container-class container-class
     :children
     ($ :div {:class "space-y-4"}
       ;; Success Rate Summary (if provided)
       (when success-rate
         (let [total (:total success-rate 0)
               successful (:successful success-rate 0)
               rate (if (pos? total) (* 100 (/ successful total)) 100)
               threshold (:success-threshold success-rate 90)
               label (:label success-rate "Success Rate")]
           ($ :div {:class "bg-gray-50 p-4 rounded-lg"}
             ($ :div {:class "flex items-center justify-between"}
               ($ :div
                 ($ :div {:class "text-lg font-semibold text-gray-900"} label)
                 ($ :div {:class "text-sm text-gray-600"}
                   (str total " total attempts")))
               ($ :div {:class "text-right"}
                 ($ :div {:class (str "text-2xl font-bold "
                                   (if (>= rate threshold) "text-green-600" "text-yellow-600"))}
                   (str (.toFixed rate 1) "%"))
                 ($ :div {:class "text-sm text-gray-500"}
                   (str successful " successful")))))))

       ;; Recent Events Section (if provided)
       (when (seq recent-events)
         ($ :div
           ($ :h4 {:class "font-medium text-gray-900 mb-2"} "Recent Events")
           (if use-template-alerts?
             ;; Use template success alerts for events
             ($ :div {:class "space-y-2"}
               (for [event recent-events]
                 ($ success-alert {:key (:key event)
                                   :message (str (:title event)
                                              (when (:subtitle event) (str " - " (:subtitle event))))})))
             ;; Original implementation
             ($ :div {:class "space-y-2"}
               (for [event recent-events]
                 ($ :div {:key (:key event)
                          :class (str "p-3 " (:bg-color event "bg-blue-50")
                                   " border-l-4 " (:border-color event "border-blue-400")
                                   " rounded")}
                   ($ :div {:class "flex justify-between items-start"}
                     ($ :div
                       ($ :div {:class "font-medium text-blue-900"} (:title event))
                       (when (:subtitle event)
                         ($ :div {:class "text-sm text-blue-600"} (:subtitle event))))
                     (when (:date event)
                       ($ :div {:class "text-right"}
                         ($ :div {:class "text-xs text-gray-500"} (:date event)))))))))))

       ;; Failures Section (if provided)
       (when (seq failures)
         ($ :div
           ($ :h4 {:class "font-medium text-gray-900 mb-2"} "Failures")
           (if use-template-alerts?
             ;; Use template error alerts for failures
             ($ :div {:class "space-y-2"}
               (for [failure failures]
                 ($ error-alert {:key (:key failure)
                                 :error {:message (:title failure)
                                         :details {:message (:subtitle failure)
                                                   :value (:value failure)
                                                   :date (:date failure)}}
                                 :entity-name "status-section"})))
             ;; Original implementation
             ($ :div {:class "space-y-2"}
               (for [failure failures]
                 ($ :div {:key (:key failure) :class "p-3 bg-red-50 border-l-4 border-red-400 rounded"}
                   ($ :div {:class "flex justify-between"}
                     ($ :div
                       ($ :div {:class "font-medium text-red-900"} (:title failure))
                       (when (:subtitle failure)
                         ($ :div {:class "text-sm text-red-600"} (:subtitle failure))))
                     ($ :div {:class "text-right"}
                       (when (:value failure)
                         ($ :div {:class "font-bold text-red-600"} (:value failure)))
                       (when (:date failure)
                         ($ :div {:class "text-xs text-gray-500"} (:date failure))))))))))))}))

;; ============================================================================
;; Notification Helper Components
;; ============================================================================

;; Re-export notification-banner from template namespace for backward compatibility
(def notification-banner notification-banner)

;; Re-export toast-notification from template namespace for backward compatibility
(def toast-notification toast-notification)
