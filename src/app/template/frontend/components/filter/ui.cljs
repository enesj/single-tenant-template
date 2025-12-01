(ns app.template.frontend.components.filter.ui
  (:require
    [app.template.frontend.components.filter.components :as filter-components]
    [app.template.frontend.components.filter.hooks :as filter-hooks]
    [app.template.frontend.components.filter.utils :as filter-utils]
    [app.template.frontend.events.list.filters :as filter-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :as uix.re-frame]))

;; UI Component for text field filtering
(defui text-field-filter
  [{:keys [_field-id _field-label filter-text set-filter-text matching-count entity-type]}]
  ($ :div
    ;; Field input section
    ($ :div {:class "p-4"}
      ($ :div {:class "mb-2"}
        ($ filter-components/filter-label
          {:text "Contains text:"})
        ($ filter-components/filter-input
          {:type "text"
           :id "filter-text-input"
           :value filter-text
           :placeholder "Type to filter..."
           :on-change #(set-filter-text (.. % -target -value))}))

      ;; Helper text for character requirements and auto-filtering
      ($ filter-components/text-filter-helper
        {:filter-text filter-text})

      ;; Show match count when filtering is active
      (when (and matching-count (>= (count filter-text) 3))
        ($ :div {:class "text-sm text-gray-600 mt-1"}
          (str "Found " matching-count " matching "
            (if (= matching-count 1) "item" "items")))))))

;; Debounce helper
;; Debounce function moved to utils namespace

;; Number range filter component
(defui number-range-filter
  [{:keys [field-id filter-min filter-max matching-count entity-type]}]
  (let [{:keys [local-min local-max handle-min-change handle-max-change has-values]}
        (filter-hooks/use-number-range-filter entity-type field-id filter-min filter-max)]

    ($ :div
      ($ :div {:class "p-4"}
        ($ :div {:class "mb-2"}
          ($ filter-components/filter-label {:text "Min"})
          ($ filter-components/number-input
            {:id "filter-min-input"
             :value local-min
             :on-change handle-min-change
             :placeholder "Min"}))

        ($ :div
          ($ filter-components/filter-label {:text "Max"})
          ($ filter-components/number-input
            {:id "filter-max-input"
             :value local-max
             :on-change handle-max-change
             :placeholder "Max"}))

        ;; Status indicator
        ($ filter-components/filter-status-indicator
          {:has-filter? has-values
           :matching-count matching-count})))))

(defui filter-actions
  [{:keys [entity-type field-id filter-text set-filter-text field-type on-close] :as props}]
  (let [;; Extract the date filter handlers if they exist
        set-filter-from (get props :set-filter-from)
        set-filter-to (get props :set-filter-to)
        handle-date-clear (get props :handle-date-clear)
        ;; Extract the select filter setter if it exists
        set-selected-options (get props :set-selected-options)

        ;; Clear handler
        handle-clear (fn []
                       (cond
                         ;; Use handle-date-clear if it's a date filter
                         handle-date-clear
                         (handle-date-clear)

                         ;; Otherwise, handle normal clearing
                         :else
                         (when entity-type
                           ;; Always pass the field-id to clear only this specific filter
                           (rf/dispatch [::filter-events/clear-filter entity-type field-id])
                           ;; Also reset the local state for the different filter types
                           (when set-filter-text (set-filter-text ""))
                           (when set-filter-from (set-filter-from nil))
                           (when set-filter-to (set-filter-to nil))
                           (when set-selected-options (set-selected-options [])))))]

    ($ filter-components/filter-action-bar
      {:on-clear handle-clear
       :on-close on-close})))

(defui date-range-filter
  [{:keys [field-id filter-from filter-to matching-count entity-type]}]
  (let [{:keys [local-from local-to handle-from-change handle-to-change has-values]}
        (filter-hooks/use-date-range-filter entity-type field-id filter-from filter-to)

        ;; Convert date change handlers to work with input events
        handle-from-input-change (fn [e]
                                   (let [value (.. e -target -value)]
                                     (if (seq value)
                                       (handle-from-change (js/Date. value))
                                       (handle-from-change nil))))

        handle-to-input-change (fn [e]
                                 (let [value (.. e -target -value)]
                                   (if (seq value)
                                     (handle-to-change (js/Date. value))
                                     (handle-to-change nil))))]

    ($ :div
      ($ :div {:class "p-4 space-y-3"}
        ($ :div
          ($ filter-components/date-input
            {:id "filter-from-date"
             :value (filter-utils/date-to-input-value local-from)
             :on-change handle-from-input-change
             :placeholder "From Date"}))

        ($ :div
          ($ filter-components/date-input
            {:id "filter-to-date"
             :value (filter-utils/date-to-input-value local-to)
             :on-change handle-to-input-change
             :placeholder "To Date"}))

        ;; Status indicator
        ($ filter-components/filter-status-indicator
          {:has-filter? has-values
           :matching-count matching-count})))))

;; Component to display active filters as text
(defui active-filters-display
  [{:keys [entity-type active-filters on-clear-filter]}]
  (let [filter-count (count active-filters)
        ;; Subscribe to entity config to get field labels
        entity-config (uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entity-config entity-type])
        ;; Subscribe to all entities for value lookup
        all-entities (uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entities entity-type])]

    (when (pos? filter-count)
      ($ :div {:class "bg-blue-50 pt-2 pb-2 border-t border-gray-200 rounded-b-lg"}
        ($ :div {:class "text-xs font-medium text-gray-700 ml-2 mb-2"}
          (str "Active Filters (" filter-count ")"))
        ($ :div {:class "ml-2 space-y-1"}
          (for [[field-id filter-value] active-filters]
            (let [field-label (filter-utils/get-field-label entity-config field-id)
                  value-text (filter-utils/format-filter-value entity-config all-entities field-id filter-value)]
              ($ :div {:key (str field-id)
                       :class "flex items-center justify-between bg-white rounded px-2 py-1 text-xs"}
                ($ :span {:class "text-gray-600"}
                  (str field-label ": " value-text))
                ($ :button {:class "text-red-500 hover:text-red-700 ml-2 cursor-pointer"
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (when on-clear-filter
                                          (on-clear-filter field-id)))}
                  "✕")))))))))

(defui compact-active-filters
  "Compact active filters display that's always visible when filters are active"
  [{:keys [entity-type active-filters on-clear-filter class]}]
  (let [filter-count (count active-filters)
        ;; Subscribe to entity config to get field labels
        entity-config (uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entity-config entity-type])
        ;; Subscribe to all entities to get possible option values
        all-entities (uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entities entity-type])]

    (when (pos? filter-count)
      ($ :div {:class (str "bg-blue-50 border border-blue-200 rounded-lg px-3 py-2 mb-3 " (or class ""))}
        ($ :div {:class "flex flex-wrap items-center gap-2"}
          ($ :span {:class "text-xs font-medium text-blue-700 mr-2"}
            (str "Active Filters (" filter-count "):"))

          ;; Render filter chips
          (for [[field-id filter-value] active-filters]
            (let [field-label (filter-utils/get-field-label entity-config field-id)
                  value-text (filter-utils/format-filter-value entity-config all-entities field-id filter-value)]
              ($ filter-components/filter-chip
                {:key (str field-id)
                 :field-id field-id
                 :field-label field-label
                 :value-text value-text
                 :on-remove on-clear-filter})))

          ;; Clear all button
          (when (> filter-count 1)
            ($ filter-components/clear-all-button
              {:on-click #(rf/dispatch [::filter-events/clear-filter entity-type nil])})))))))

(defui select-field-filter
  "Multi-select dropdown filter for enum/select fields with predefined options"
  [{:keys [field-id field-label available-options selected-options set-selected-options matching-count entity-type]}]
  (let [;; State for dropdown open/closed
        [dropdown-open?, set-dropdown-open] (use-state false)

        ;; Handle option toggle
        handle-option-toggle (fn [option-value]
                               (let [current-set (set (map :value selected-options))
                                     new-set (if (contains? current-set option-value)
                                               (disj current-set option-value)
                                               (conj current-set option-value))
                                     new-options (vec (filter #(contains? new-set (:value %)) available-options))]
                                 (set-selected-options new-options)
                                 ;; Auto-apply the filter with the new selection
                                 (rf/dispatch [::filter-events/apply-filter
                                               entity-type
                                               (if (string? field-id) (keyword field-id) field-id)
                                               (if (seq new-options) new-options nil)
                                               true])))

        ;; Handle select all / clear all
        handle-select-all (fn [select-all?]
                            (let [new-options (if select-all? available-options [])]
                              (set-selected-options new-options)
                              ;; Auto-apply the filter
                              (rf/dispatch [::filter-events/apply-filter
                                            entity-type
                                            (if (string? field-id) (keyword field-id) field-id)
                                            (if (seq new-options)
                                              ;; Extract just the values from option objects
                                              (mapv :value new-options)
                                              nil)
                                            true])))

        selected-values (set (map :value selected-options))
        selected-count (count selected-options)
        total-count (count available-options)]

    ($ :div {:class "p-4 space-y-3"}
      ;; Multi-select dropdown
      ($ :div {:class "relative"}
        ;; Dropdown toggle
        ($ filter-components/dropdown-toggle
          {:selected-count selected-count
           :field-label field-label
           :first-selection (first selected-options)
           :on-toggle set-dropdown-open
           :dropdown-open? dropdown-open?})

        ;; Dropdown content
        (when dropdown-open?
          ($ :div {:class "absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-md shadow-lg"
                   :style {:max-height "250px" :overflow-y "auto"}}
            ;; Select/clear all controls
            ($ filter-components/dropdown-controls
              {:on-select-all handle-select-all})

            ;; Options list
            (for [option available-options]
              ($ filter-components/dropdown-option
                {:key (:value option)
                 :option option
                 :is-selected (contains? selected-values (:value option))
                 :on-toggle handle-option-toggle})))))

      ;; Selection summary and status
      (when (seq selected-options)
        ($ :div {:class "pt-2 border-t border-gray-100 space-y-1"}
          ($ filter-components/filter-status-indicator
            {:has-filter? true
             :matching-count nil})
          ($ :div {:class "text-xs text-gray-600"}
            (str "Selected " selected-count " of " total-count " options"))))

      ;; Match count
      (when matching-count
        ($ :div {:class "text-sm text-gray-600"}
          (str "Found " matching-count " matching "
            (if (= matching-count 1) "item" "items")))))))
