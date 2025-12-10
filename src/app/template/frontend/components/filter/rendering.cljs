(ns app.template.frontend.components.filter.rendering
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.filter.ui :refer [active-filters-display
                                                        date-range-filter
                                                        filter-actions
                                                        number-range-filter
                                                        select-field-filter
                                                        text-field-filter]]
    [uix.core :refer [$]]))

;; ============================================================================
;; Filter Component Rendering
;; ============================================================================

(defn render-text-filter
  "Render text filter component"
  [{:keys [filter-type _filter-text _set-filter-text] :as props}]
  (when (= filter-type :text)
    ($ text-field-filter
      (select-keys props [:field-id :field-label :filter-text :set-filter-text :matching-count :entity-type]))))

(defn render-number-range-filter
  "Render number range filter component"
  [{:keys [filter-type filter-min filter-max set-filter-min set-filter-max] :as props}]
  (when (= filter-type :number-range)
    ($ number-range-filter
      (-> props
        (assoc :filter-min (when (number? filter-min) filter-min)
          :filter-max (when (number? filter-max) filter-max)
          :set-filter-min (fn [v] (set-filter-min (when (some? v) (js/parseFloat v))))
          :set-filter-max (fn [v] (set-filter-max (when (some? v) (js/parseFloat v)))))))))

(defn render-date-range-filter
  "Render date range filter component"
  [{:keys [filter-type filter-from-date filter-to-date set-filter-from-date set-filter-to-date] :as props}]
  (when (= filter-type :date-range)
    ($ date-range-filter
      (-> props
        (assoc
          :filter-from-date filter-from-date
          :filter-to-date filter-to-date
          :set-filter-from-date (fn [v] (set-filter-from-date (when (some? v) (js/Date. v))))
          :set-filter-to-date (fn [v] (set-filter-to-date (when (some? v) (js/Date. v)))))))))

(defn render-select-filter
  "Render select filter component"
  [{:keys [filter-type filter-selected-options _set-filter-selected-options] :as props}]
  (when (= filter-type :select)
    ($ select-field-filter
      (-> props
        (assoc :selected-options filter-selected-options
          :set-selected-options (:set-filter-selected-options props))))))

(defn render-filter-actions
  "Render filter action buttons"
  [{:keys [field-type-str] :as props}]
  ($ filter-actions
    (assoc props :field-type field-type-str)))

(defn render-active-filters-display
  "Render active filters display component"
  [{:keys [entity-type active-filters] :as props}]
  (when (seq active-filters)
    ($ active-filters-display
      (assoc props :entity-type entity-type :active-filters active-filters))))

;; ============================================================================
;; Header and Layout Rendering
;; ============================================================================

(defn render-filter-header
  "Render filter header with title and close button"
  [{:keys [field-label _on-close] :as props}]
  ($ :div
    {:class "flex justify-between items-center mb-2"}
    ($ :h3
      {:class "text-lg font-medium"}
      (str "Filter by " field-label))
    ($ button
      {:btn-type :ghost
       :class "ds-btn-sm"
       :on-click (:on-close props)}
      "×")))

(defn render-filter-content
  "Render the main filter content area"
  [{:keys [field-id] :as props}]
  (when field-id
    ($ :div
      {:class "mt-3"}
      ;; Render appropriate filter component based on type
      (render-text-filter props)
      (render-number-range-filter props)
      (render-date-range-filter props)
      (render-select-filter props)
      ;; Action buttons
      (render-filter-actions props))))

(defn render-filter-form-layout
  "Render the complete filter form layout"
  [{:keys [_field-label _on-close] :as props}]
  ($ :div
    {:class "bg-base-100 border border-base-300 rounded-lg p-3 mb-3 shadow-sm"}

    ;; Filter header with close button
    (render-filter-header props)

    ;; Filter input based on field type
    (render-filter-content props)

    ;; Active filters display
    (render-active-filters-display props)))
