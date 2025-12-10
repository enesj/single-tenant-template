(ns app.template.frontend.components.filter.components
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui]]))

;; Common filter status indicators

(defui filter-status-indicator
  "Shows auto-filtering status and match count"
  [{:keys [has-filter? matching-count]}]
  (when has-filter?
    ($ :div {:class "space-y-1"}
      ;; Auto-filtering indicator
      ($ :div {:class "text-xs text-success flex items-center"}
        ($ :span {:class "mr-1"} "Auto-filtering")
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"}))

      ;; Match count
      (when matching-count
        ($ :div {:class "text-sm text-gray-600"}
          (str "Found " matching-count " matching "
            (if (= matching-count 1) "item" "items")))))))

(defui filter-input
  "Reusable input component for filters"
  [{:keys [type id class placeholder value on-change input-mode pattern title]}]
  ($ :input {:type (or type "text")
             :id id
             :class (str "ds-input ds-input-bordered w-full " (or class ""))
             :placeholder placeholder
             :value (or value "")
             :on-change on-change
             :inputMode input-mode
             :pattern pattern
             :title title}))

(defui filter-label
  "Reusable label component for filters"
  [{:keys [text class]}]
  ($ :label {:class (str "block text-sm font-medium text-gray-700 mb-1 " (or class ""))}
    text))

;; Date input components

(defui date-input
  "Date input component with proper formatting"
  [{:keys [id value on-change placeholder]}]
  ($ filter-input
    {:type "date"
     :id id
     :value value
     :on-change on-change
     :placeholder placeholder}))

;; Number input components

(defui number-input
  "Number input component with validation"
  [{:keys [id value on-change placeholder]}]
  ($ filter-input
    {:type "text"
     :id id
     :input-mode "decimal"
     :value value
     :on-change on-change
     :placeholder placeholder
     :pattern "[0-9]*\\.?[0-9]*"
     :title "Please enter a valid number"}))

;; Text input warning/helper

(defui text-filter-helper
  "Helper text for text filters showing character requirements"
  [{:keys [filter-text]}]
  (let [char-count (count filter-text)]
    (cond
      ;; Show character requirement when user has typed 1-2 characters
      (and (pos? char-count) (< char-count 3))
      ($ :div {:class "text-xs text-gray-500 mt-1"}
        (str "Type " (- 3 char-count) " more character"
          (when (= char-count 1) "s")
          " to auto-filter"))

      ;; Show auto-filtering when 3+ characters
      (>= char-count 3)
      ($ :div {:class "text-xs text-success mt-1 flex items-center"}
        ($ :span {:class "mr-1"} "Auto-filtering")
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})))))

;; Filter action buttons

(defui clear-filter-button
  "Reusable clear filter button"
  [{:keys [on-click class]}]
  ($ button
    {:btn-type :secondary
     :class (str "flex-1 mr-2 " (or class ""))
     :id "filter-clear-button"
     :on-click (fn [e]
                 (.stopPropagation e)
                 (when on-click (on-click)))}
    "Clear"))

(defui close-filter-button
  "Reusable close filter button"
  [{:keys [on-click class]}]
  ($ button
    {:btn-type :primary
     :class (str "flex-1 " (or class ""))
     :id "filter-close-button"
     :on-click (fn [e]
                 (.stopPropagation e)
                 (when on-click (on-click)))}
    "Close"))

(defui filter-action-bar
  "Action bar with clear and close buttons"
  [{:keys [on-clear on-close]}]
  ($ :div {:class "flex justify-between p-3 border-t bg-base-200"}
    ($ clear-filter-button {:on-click on-clear})
    ($ close-filter-button {:on-click on-close})))

;; Active filter chip component

(defui filter-chip
  "Individual filter chip with remove button"
  [{:keys [field-id field-label value-text on-remove]}]
  ($ :div {:class "inline-flex items-center bg-white border border-blue-300 rounded-full px-2 py-1 text-xs"}
    ($ :span {:class "text-gray-700 mr-1"}
      (str field-label ": " value-text))
    ($ :button {:id (str "filter-remove-" (name field-id))
                :class "text-red-500 hover:text-red-600 ml-1 cursor-pointer text-sm font-bold leading-none"
                :title "Remove this filter"
                :on-click (fn [e]
                            (.preventDefault e)
                            (.stopPropagation e)
                            (when on-remove (on-remove field-id)))}
      "×")))

(defui clear-all-button
  "Clear all filters button"
  [{:keys [on-click]}]
  ($ :button {:id "filter-clear-all-button"
              :class "inline-flex items-center bg-red-100 hover:bg-red-200 border border-red-300 rounded-full px-2 py-1 text-xs text-red-700 cursor-pointer"
              :title "Clear all filters"
              :on-click (fn [e]
                          (.preventDefault e)
                          (.stopPropagation e)
                          (when on-click (on-click)))}
    "Clear All"))

;; Dropdown components for select filters

(defui dropdown-toggle
  "Dropdown toggle button for select filters"
  [{:keys [selected-count field-label first-selection on-toggle dropdown-open?]}]
  ($ :button {:class "w-full px-3 py-2 text-left bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              :id "filter-dropdown-toggle"
              :on-click #(on-toggle (not dropdown-open?))}
    ($ :div {:class "flex justify-between items-center"}
      ($ :span {:class "text-sm"}
        (cond
          (zero? selected-count) (str "Select " field-label "...")
          (= selected-count 1) (:label first-selection)
          :else (str selected-count " selected")))
      ($ :svg {:class "w-4 h-4 text-gray-400" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                  :d "M19 9l-7 7-7-7"})))))

(defui dropdown-option
  "Individual option in dropdown"
  [{:keys [option is-selected on-toggle]}]
  (let [option-value (:value option)
        option-label (:label option)]
    ($ :div {:id (str "filter-option-" option-value)
             :class "flex items-center px-3 py-2 hover:bg-blue-50 cursor-pointer"
             :on-click #(on-toggle option-value)}
      ($ :input {:type "checkbox"
                 :class "mr-2 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                 :checked is-selected
                 :read-only true})
      ($ :span {:class "text-sm text-gray-900"}
        option-label))))

(defui dropdown-controls
  "Select all / clear all controls for dropdown"
  [{:keys [on-select-all _on-clear-all]}]
  ($ :div {:class "px-3 py-2 border-b border-gray-100 flex justify-between"}
    ($ :button {:class "text-xs text-blue-600 hover:text-blue-800"
                :on-click #(on-select-all true)}
      "Select All")
    ($ :button {:class "text-xs text-red-600 hover:text-red-800"
                :on-click #(on-select-all false)}
      "Clear All")))
