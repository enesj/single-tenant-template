(ns app.template.frontend.components.batch-edit
  (:require
    [app.frontend.utils.id :as id-utils]
    [app.template.frontend.components.batch-edit.fields :as batch-fields]
    [app.template.frontend.components.batch-edit.hooks :as batch-hooks]
    [app.template.frontend.components.batch-edit.ui :as batch-ui]
    [app.template.frontend.components.form.base :as base]
    [app.template.frontend.subs.entity :as entity-subs]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :as uix :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; find-identical-values moved to app.frontend.components.batch-edit.utils

;; REMOVED: render-batch-field component - no longer used
;; The batch-edit-inline component now handles all field rendering inline
;; to avoid React Hooks issues and improve performance

; Removed render-batch-field component - now using app.frontend.components.batch-edit.fields/render-batch-field-component

(defui batch-edit-inline
  "Inline batch editing form for table integration"
  [{:keys [entity-name selected-ids on-close]}]

  (let [;; Get entity configuration
        form-entity-spec (use-subscribe [:form-entity-specs/by-name (when entity-name entity-name)])
        entity-spec form-entity-spec

        ;; Get all selected items
        entity-items (use-subscribe [::entity-subs/entities (when entity-name entity-name)])
        items (filter (fn [item]
                        (let [;; Use the generic ID extraction utility
                              item-id (id-utils/extract-entity-id item)]
                          (contains? selected-ids item-id)))
                (or entity-items []))

        ;; Use hooks for state management
        initial-state (batch-hooks/use-initial-values items entity-spec entity-name)
        initial-values (:initial-values initial-state)
        original-values (:original-values initial-state)

        form-state (batch-hooks/use-batch-form-state initial-values)
        _form-values (:form-values form-state)
        update-form-values (:update-form-values form-state)]

    (when (and (seq items) (seq entity-spec))
      ;; Use the existing UI container component

      ($ batch-ui/batch-edit-container
        ;; Use the existing header component
        ($ batch-ui/batch-edit-header
          {:entity-name entity-name
           :selected-ids selected-ids
           :selected-count (count selected-ids)
           :on-close on-close})

        ;; Form with proper batch update functionality
        ($ base/initialize-form
          {:render-fn (fn [form-props]
                        (let [{:keys [form-id values errors set-values dirty]} form-props]
                          ($ :form
                            {:on-submit (fn [e]
                                          (.preventDefault e)
                                          ;; Only send fields that have actually changed
                                          (let [changed-values (into {}
                                                                 (filter (fn [[k v]]
                                                                           (not= v (get original-values k)))
                                                                   values))]
                                            (log/info "Batch update - only sending changed fields:" changed-values)
                                            ;; Dispatch batch update event with only changed values
                                            (rf/dispatch [:app.template.frontend.events.list.batch/batch-update
                                                          {:entity-name entity-name
                                                           :item-ids selected-ids
                                                           :values changed-values}]))
                                          ;; Close form after submission
                                          (when on-close (on-close)))}

                            ;; Use existing notifications component
                            ($ batch-ui/batch-edit-notifications
                              {:form-success? false
                               :submitted? false
                               :form-errors errors
                               :entity-name entity-name})

                            ;; Form grid using existing field components
                            ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6"}
                              (map-indexed (fn [_idx field-spec]
                                             ($ :div {:key (:id field-spec)
                                                      :class "flex flex-col"}
                                               ;; Use the fixed render-batch-field-component
                                               ($ batch-fields/render-batch-field-component
                                                 {:field-spec field-spec
                                                  :entity-name entity-name
                                                  :form-id form-id
                                                  :values values
                                                  :errors errors
                                                  :set-values set-values
                                                  :set-form-values update-form-values})))
                                entity-spec))

                            ;; Use existing buttons component with proper dirty tracking
                            ($ batch-ui/batch-edit-buttons
                              {:entity-name entity-name
                               :submitting? false
                               :has-form-errors? (boolean (seq errors))
                               :no-changes? (not dirty)  ;; Use fork's dirty property
                               :on-close on-close}))))

           :entity-name entity-name
           :entity-spec entity-spec
           :initial-values initial-values
           :editing true})))))
