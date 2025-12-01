(ns app.template.frontend.components.batch-edit.ui
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [cancel-icon save-icon]]
    [app.template.frontend.components.messages :as messages]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui]]))

(defui batch-edit-header
  "Header component for batch edit with title and close button"
  [{:keys [entity-name selected-ids selected-count on-close]}]
  (let [item-count (or selected-count (count selected-ids) 0)]
    ($ :div {:class "flex items-center justify-between mb-4"}
      ($ :div {:class "flex items-center"}
        ($ :svg {:xmlns "http://www.w3.org/2000/svg"
                 :class "h-5 w-5 mr-2 text-primary"
                 :fill "none"
                 :viewBox "0 0 24 24"
                 :stroke "currentColor"}
          ($ :path {:stroke-linecap "round"
                    :stroke-linejoin "round"
                    :stroke-width "2"
                    :d "M8 9h8M8 13h8M8 17h8M8 5h8"}))
        ($ :h4 {:class "text-md font-semibold text-primary"}
          (str "Batch Edit " item-count " "
            (or entity-name "items")
            (when (> item-count 1) "s"))))
      ($ button {:btn-type :outline
                 :size "sm"
                 :shape "circle"
                 :class "text-gray-500 hover:text-gray-700"
                 :on-click on-close} "×"))))

;; Component removed - use batch-edit-inline instead

(defui batch-edit-buttons
  "Action buttons for batch edit (Cancel and Update All)"
  [{:keys [entity-name submitting? has-form-errors? no-changes? on-close]}]
  ($ :div {:class "flex justify-end gap-2 pt-2 border-t border-base-200"}
    ($ button
      {:btn-type :cancel
       :size "sm"
       :type "button"
       :disabled submitting?
       :on-click (fn []
                   (rf/dispatch [::form-events/set-submitted entity-name false])
                   (rf/dispatch [::crud-events/clear-error (keyword entity-name)])
                   (rf/dispatch [::form-events/clear-form-errors entity-name])
                   (on-close))
       :children ["Cancel" ($ cancel-icon)]})

    ($ button
      {:btn-type :save
       :size "sm"
       :type "submit"
       ;; Disable button when:
       ;; 1. Form is submitting
       ;; 2. There are validation errors
       ;; 3. No changes (no dirty fields)
       :disabled (boolean (or submitting?
                            has-form-errors?
                            no-changes?))
       :children ["Update All" ($ save-icon)]})))

(defui batch-edit-notifications
  "Success and error message components for batch edit"
  [{:keys [form-success? submitted? form-errors entity-name]}]
  ($ :div
    (when-let [form-error (get-in form-errors [:form])]
      ($ messages/error-alert
        {:error form-error
         :entity-name entity-name}))

    (when (and form-success? submitted?)
      ($ messages/success-alert
        {:message "Batch update successful!"
         :entity-name entity-name}))))

(defui batch-edit-container
  "Main container component for batch edit UI"
  [{:keys [children]}]
  ($ :div
    {:class "bg-base-100 border border-base-300 rounded-lg p-4 mb-4 shadow-sm"}
    children))
