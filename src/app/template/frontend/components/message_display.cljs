(ns app.template.frontend.components.message-display
  "Universal message display component for success/error notifications.

   This component provides consistent messaging across all application modules:
   - Admin panel, tenant UI, customer portal, etc.
   - Supports different themes and styling variants
   - Configurable behavior and appearance"

  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui use-effect]]))

(defui message-display
  "Universal success/error message display component.

   Props:
   - :success-message - string to display as success message
   - :error - string to display as error message
   - :on-clear-success - function to call when success message is cleared
   - :on-clear-error - function to call when error message is cleared
   - :variant - :admin (default), :tenant, :customer for different themes
   - :show-dismiss-button? - boolean to show dismiss buttons (default: true)
   - :auto-dismiss? - boolean to auto-dismiss messages (default: false)
   - :auto-dismiss-timeout - timeout in ms for auto-dismiss (default: 5000)
   - :class - optional additional CSS classes

   Theme Variants:
   - :admin - Uses 'ds-alert' classes (default)
   - :tenant - Uses tenant-themed styling
   - :customer - Uses customer-themed styling"

  [{:keys [success-message error
           on-clear-success on-clear-error
           variant show-dismiss-button?
           auto-dismiss? auto-dismiss-timeout
           class]
    :or {variant :admin
         show-dismiss-button? true
         auto-dismiss? false
         auto-dismiss-timeout 5000
         on-clear-success #(dispatch [:clear-success-message])
         on-clear-error #(dispatch [:clear-error-message])
         class ""}}]

  ;; Auto-dismiss functionality
  (use-effect
    (fn []
      (when (and auto-dismiss? (or success-message error))
        (let [timeout-id (js/setTimeout
                           (fn []
                             (when success-message (on-clear-success))
                             (when error (on-clear-error)))
                           auto-dismiss-timeout)]
          (fn [] (js/clearTimeout timeout-id)))))
    [success-message error on-clear-success on-clear-error auto-dismiss? auto-dismiss-timeout])

  ($ :div {:class (str "space-y-2 " class)}
    ;; Success message
    (when success-message
      ($ :div {:class (case variant
                        :admin "ds-alert ds-alert-success mb-4"
                        :tenant "tenant-alert tenant-alert-success mb-4"
                        :customer "customer-alert customer-alert-success mb-4"
                        "alert alert-success mb-4")}
        ($ :span success-message)
        (when show-dismiss-button?
          ($ button {:btn-type :ghost
                     :class (case variant
                              :admin "ds-btn-sm ml-auto"
                              :tenant "tenant-btn-sm ml-auto"
                              :customer "customer-btn-sm ml-auto"
                              "btn-sm ml-auto")
                     :on-click on-clear-success}
            "✕"))))

    ;; Error display
    (when error
      ($ :div {:class (case variant
                        :admin "ds-alert ds-alert-error mb-4"
                        :tenant "tenant-alert tenant-alert-error mb-4"
                        :customer "customer-alert customer-alert-error mb-4"
                        "alert alert-error mb-4")}
        ($ :span error)
        (when show-dismiss-button?
          ($ button {:btn-type :ghost
                     :class (case variant
                              :admin "ds-btn-sm ml-auto"
                              :tenant "tenant-btn-sm ml-auto"
                              :customer "customer-btn-sm ml-auto"
                              "btn-sm ml-auto")
                     :on-click on-clear-error}
            "✕"))))))

;; Convenience functions for common use cases

(defn admin-message-display
  "Convenience function for admin-themed message display."
  [& args]
  ($ message-display (merge {:variant :admin} (first args))))

(defn tenant-message-display
  "Convenience function for tenant-themed message display."
  [& args]
  ($ message-display (merge {:variant :tenant} (first args))))

(defn customer-message-display
  "Convenience function for customer-themed message display."
  [& args]
  ($ message-display (merge {:variant :customer} (first args))))
