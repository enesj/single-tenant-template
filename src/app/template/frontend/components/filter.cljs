(ns app.template.frontend.components.filter
  (:require
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.components.filter.logic :as filter-logic]
    [app.template.frontend.components.filter.rendering :as filter-rendering]
    [app.template.frontend.components.filter.ui]
    [app.template.frontend.subs.entity :as entity-subs]
    [app.template.frontend.subs.list :as list-subs]
    [uix.core :refer [defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(def filter-form-props
  {:entity-type {:type :keyword :required true}
   :field-spec {:type :map :required true}
   :initial-value {:type :any :required false}
   :on-close {:type :function :required true}
   :on-apply {:type :function :required true}
   :on-field-switch {:type :function :required false}})

(defui filter-form
  "Inline filter form component that accepts props instead of using modal state"
  {:prop-types filter-form-props}
  [{:keys [entity-type field-spec initial-value on-close on-apply on-field-switch] :as props}]
  (let [;; Subscribe to required data
        all-entities (use-subscribe [::entity-subs/entities entity-type])
        items (use-subscribe [::entity-subs/entities entity-type])
        active-filters (use-subscribe [::list-subs/active-filters entity-type])

        ;; Calculate filter configuration
        filter-type (filter-helpers/get-filter-type {:field-spec field-spec})
        foreign-key-entity (filter-helpers/get-foreign-key-entity {:field-spec field-spec})
        foreign-key-entities (use-subscribe [::entity-subs/entities (or foreign-key-entity :placeholder)])
        filtered-foreign-key-entities (when foreign-key-entity foreign-key-entities)

        ;; Extract field information
        field-id (get field-spec :id)
        field-label (or (get field-spec :label) "Unknown Field")
        field-type-str (or (get field-spec :input-type) "text")

        ;; Calculate available options for select fields
        available-options (filter-logic/calculate-available-options
                            {:filter-type filter-type
                             :field-spec field-spec
                             :foreign-key-entity foreign-key-entity
                             :foreign-key-entities filtered-foreign-key-entities
                             :field-id field-id
                             :items items})

        ;; Initialize filter state based on initial value and filter type
        initial-state (filter-logic/initialize-filter-state
                        {:initial-value initial-value
                         :filter-type filter-type
                         :entity-type entity-type
                         :field-spec field-spec
                         :all-entities all-entities
                         :foreign-key-entity foreign-key-entity
                         :available-options available-options})

        ;; Set up state hooks for different filter types
        [filter-text, set-filter-text] (use-state (:filter-text initial-state))
        [filter-min, set-filter-min] (use-state (:filter-min initial-state))
        [filter-max, set-filter-max] (use-state (:filter-max initial-state))
        [filter-from-date, set-filter-from-date] (use-state (:filter-from-date initial-state))
        [filter-to-date, set-filter-to-date] (use-state (:filter-to-date initial-state))
        [filter-selected-options, set-filter-selected-options] (use-state (:filter-selected-options initial-state))

        ;; Calculate matching count for all filter types
        matching-count (filter-logic/calculate-matching-count
                         {:filter-type filter-type
                          :items items
                          :field-id field-id
                          :filter-text filter-text
                          :filter-min filter-min
                          :filter-max filter-max
                          :filter-from-date filter-from-date
                          :filter-to-date filter-to-date
                          :filter-selected-options filter-selected-options})]

    ;; Set up effect hooks for entity fetching and auto-apply functionality
    (filter-logic/use-entity-fetching entity-type
      foreign-key-entity
      (seq all-entities)
      (seq filtered-foreign-key-entities))
    (filter-logic/use-debug-logging filter-type field-id foreign-key-entity filtered-foreign-key-entities)
    (filter-logic/sync-state-with-initial-value
      {:filter-type filter-type
       :initial-value initial-value
       :available-options available-options
       :field-id field-id
       :entity-type entity-type
       :foreign-key-entity foreign-key-entity
       :set-filter-text set-filter-text
       :set-filter-min set-filter-min
       :set-filter-max set-filter-max
       :set-filter-from-date set-filter-from-date
       :set-filter-to-date set-filter-to-date
       :set-filter-selected-options set-filter-selected-options})

    ;; Auto-apply effects for different filter types
    (filter-logic/use-text-filter-auto-apply
      {:filter-type filter-type
       :filter-text filter-text
       :entity-type entity-type
       :field-id field-id
       :on-apply on-apply})

    (filter-logic/use-number-range-auto-apply
      {:filter-type filter-type
       :filter-min filter-min
       :filter-max filter-max
       :entity-type entity-type
       :field-id field-id
       :on-apply on-apply})

    (filter-logic/use-date-range-auto-apply
      {:filter-type filter-type
       :filter-from-date filter-from-date
       :filter-to-date filter-to-date
       :entity-type entity-type
       :field-id field-id
       :on-apply on-apply})

    ;; Render the complete filter form layout
    (filter-rendering/render-filter-form-layout
      {:field-label field-label
       :on-close on-close
       :field-id field-id
       :filter-type filter-type
       :filter-text filter-text
       :set-filter-text set-filter-text
       :filter-min filter-min
       :filter-max filter-max
       :set-filter-min set-filter-min
       :set-filter-max set-filter-max
       :filter-from-date filter-from-date
       :filter-to-date filter-to-date
       :set-filter-from-date set-filter-from-date
       :set-filter-to-date set-filter-to-date
       :available-options available-options
       :filter-selected-options filter-selected-options
       :set-filter-selected-options set-filter-selected-options
       :matching-count matching-count
       :entity-type entity-type
       :field-type-str field-type-str
       :active-filters active-filters})))
