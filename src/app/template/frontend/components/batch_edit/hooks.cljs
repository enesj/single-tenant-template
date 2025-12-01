(ns app.template.frontend.components.batch-edit.hooks
  (:require
    [app.shared.validation.builder :as validation-builder]
    [app.template.frontend.components.batch-edit.utils :as utils]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.subs.form :as form-subs]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn use-initial-values
  "Hook to manage initial values calculation and updates"
  [items entity-spec & [entity-name]]
  (let [[initial-values set-initial-values] (use-state
                                              (if (and (seq items) (seq entity-spec))
                                                (utils/find-identical-values items entity-spec entity-name)
                                                {}))
        [original-values _set-original-values] (use-state initial-values)]

    ;; Effect to update initial values when items or entity spec changes
    (use-effect
      (fn []
        (when (and (seq items) (seq entity-spec))
          (set-initial-values (utils/find-identical-values items entity-spec entity-name)))
        ;; No cleanup function needed
        (fn [] nil))
      [items entity-spec entity-name])

    {:initial-values initial-values
     :set-initial-values set-initial-values
     :original-values original-values}))

(defn use-batch-form-state
  "Hook to manage batch form state including values and dirty tracking"
  [initial-values]
  (let [[form-values set-form-values] (use-state initial-values)
        [dirty-fields set-dirty-fields] (use-state #{})]

    {:form-values form-values
     :set-form-values set-form-values
     :dirty-fields dirty-fields
     :set-dirty-fields set-dirty-fields}))

(defn use-form-validation
  "Hook to manage form validation state"
  [entity-name]
  (let [models-data (use-subscribe [:models-data])
        validators (when models-data (validation-builder/create-enhanced-validators models-data))
        form-success? (use-subscribe [::form-subs/form-success entity-name])
        submitted? (use-subscribe [::form-subs/submitted? entity-name])
        form-errors (use-subscribe [::form-subs/form-errors entity-name])
        has-form-errors? (utils/has-form-errors? form-errors)]

    {:validators validators
     :form-success? form-success?
     :submitted? submitted?
     :form-errors form-errors
     :has-form-errors? has-form-errors?}))

(defn use-form-effects
  "Hook to handle form-related side effects"
  [entity-name entity-spec form-values initial-values _dirty-fields set-dirty-fields]

  ;; Effect to track form values and update dirty fields
  (use-effect
    (fn []
      ;; If we have form values and they differ from initial, update dirty fields
      (when (and form-values (not= form-values initial-values))
        (let [changed-keys (->> (keys form-values)
                             (filter #(not= (get form-values %)
                                        (get initial-values %)))
                             (into #{}))]
          (when (seq changed-keys)
            ;; Update our local dirty fields state
            (set-dirty-fields changed-keys))))
      ;; No cleanup function needed
      (fn [] nil))
    [form-values entity-name initial-values set-dirty-fields])

  ;; Effect to clear form errors on mount and when entity-name changes
  (use-effect
    (fn []
      ;; Clear form errors when component mounts
      (rf/dispatch [::form-events/clear-form-errors entity-name])

      ;; Also explicitly clear all field errors
      (when entity-spec
        (doseq [field entity-spec]
          (rf/dispatch [::form-events/clear-field-error entity-name (keyword (:id field))])))

      ;; Clean up on unmount
      (fn []
        (rf/dispatch [::form-events/clear-form-errors entity-name])))
    [entity-name entity-spec]))

(defn use-field-validation
  "Hook to get validation state for a specific field"
  [field-id entity-name]
  (let [server-validation (use-subscribe [::form-subs/field-server-validation-state field-id entity-name])
        field-validation (use-subscribe [::form-subs/field-validation-state entity-name field-id])
        error (or (:error field-validation) (:error server-validation))]

    {:server-validation server-validation
     :field-validation field-validation
     :error error}))
