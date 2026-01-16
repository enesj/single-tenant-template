(ns app.admin.frontend.components.alerts
  "Alert and notification components for admin interface"
  (:require
    [app.template.frontend.components.messages :refer [error-alert
                                                       success-alert]]
    [app.template.frontend.components.notifications :as notifications]
    [app.template.frontend.components.stats :as template-stats]
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Page Header Components (Enhanced with Template Integration)
;; ============================================================================

(defui simple-page-header
  "Simple page header with title and description.

   Props:
   - :title - Page title
   - :description - Page description
   - :icon - Icon path for template header
   - :container-class - Additional classes for the container"
  [{:keys [title description icon container-class]
    :or {container-class ""}}]
  ($ :div {:class container-class}
    ($ template-stats/page-header {:title title
                                   :subtitle description
                                   :icon icon})))

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
   - :container-class - Additional classes for the container"
  [{:keys [title items empty-message container-class]
    :or {empty-message "No items to display"
         items []
         container-class ""}}]
  ($ :div {:class container-class}
    ($ :h4 {:class "font-medium text-gray-900 mb-2"} title)
    (if (empty? items)
      ($ :p {:class "text-sm text-gray-500"} empty-message)
      ($ :div {:class "space-y-2"}
        (for [item items]
          (if (contains? item :type)
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

;; ============================================================================
;; Notification Helper Components
;; ============================================================================

(def toast-notification notifications/toast-notification)
