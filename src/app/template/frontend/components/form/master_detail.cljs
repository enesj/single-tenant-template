(ns app.template.frontend.components.form.master-detail
  "Reusable master/detail form wrapper component.
   
   This component provides orchestration for edit forms that need to:
   1. Fetch detail data (master + detail rows) for edit mode
   2. Normalize backend data into form initial values
   3. Maintain stable fork initialization (no dirty resets while typing)
   4. Prepare submit payload with validation
   
   Usage:
   ($ master-detail-form
     {:mode :edit  ; or :create
      :entity-name \"expense\"
      :entity-spec [...field specs...]
      :entity-id \"123\"
      
      ;; Detail fetch orchestration (for :edit mode)
      :load-detail! (fn [id] (rf/dispatch [...]))
      :select-detail (use-subscribe [...])  ; or pass the value directly
      :detail-loading? false
      :detail-error nil
      
      ;; Data transformation
      :normalize-initial-data (fn [entity] {...form values...})
      :validate-values (fn [values] {:ok? true} or {:ok? false :error \"...\"})
      :prepare-submit-values (fn [values] {...prepared payload...})
      
      ;; Callbacks
      :on-submit (fn [prepared-payload] ...)
      :on-cancel (fn [] ...)
      
      ;; Optional
      :initial-row-data {...}  ; fallback while detail is loading
      :default-values {...}    ; defaults for create mode
      :button-text \"Update\"
      :form-body-component user-expense-form-body  ; optional custom form body
      })"
  (:require
    [app.template.frontend.components.form :refer [form]]
    [uix.core :refer [$ defui use-effect use-memo use-ref use-state]]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn- entity-id-matches?
  "Check if the loaded entity matches the requested entity-id.
   Handles various key formats from different sources.
   
   The entity-name is used to check for namespaced keys like :expense/id."
  [entity entity-id entity-name]
  (when (and entity entity-id)
    (let [entity-id-str (str entity-id)
          ;; Build potential namespaced keys based on entity-name
          singular-ns-key (when entity-name
                            (keyword (name entity-name) "id"))
          plural-ns-key (when entity-name
                          (keyword (str (name entity-name) "s") "id"))
          loaded-id (or (:id entity)
                      (get entity singular-ns-key)
                      (get entity plural-ns-key))]
      (and loaded-id (= (str loaded-id) entity-id-str)))))

;; =============================================================================
;; Main Component
;; =============================================================================

(defui master-detail-form
  "Reusable wrapper for master/detail forms with detail fetch orchestration.
   
   Props:
   - :mode - :create or :edit
   - :entity-name - string for form identification (e.g. \"expense\")
   - :entity-spec - vector of field specifications for the form
   - :entity-id - string/uuid (required for :edit mode)
   
   Detail orchestration (for :edit mode):
   - :load-detail! - fn (fn [entity-id] ...) to dispatch detail load
   - :select-detail - the currently loaded detail entity (from subscription)
   - :detail-loading? - boolean indicating detail is being fetched
   - :detail-error - string error message or nil
   
   Data transformation:
   - :normalize-initial-data - fn (fn [raw-entity] form-values-map)
   - :validate-values - fn (fn [form-values] {:ok? true} or {:ok? false :error \"...\"})
   - :prepare-submit-values - fn (fn [form-values] prepared-payload-map)
   
   Callbacks:
   - :on-submit - fn (fn [prepared-payload] ...)
   - :on-cancel - fn (fn [] ...)
   
   Optional:
   - :initial-row-data - fallback data while detail is loading
   - :default-values - defaults for create mode
   - :button-text - submit button text
   - :form-body-component - custom UIX component to render form body"
  [{:keys [mode entity-name entity-spec entity-id
           load-detail! select-detail detail-error
           normalize-initial-data validate-values prepare-submit-values
           on-submit on-cancel
           initial-row-data default-values button-text
           form-body-component]}]

  (let [editing? (= mode :edit)
        entity-id-str (some-> entity-id str)
      ;; Track which entity-id we requested detail for.
      ;; This avoids stale `requested?` when `entity-id` changes.
        [requested-entity-id set-requested-entity-id!] (use-state nil)
        requested? (= requested-entity-id entity-id-str)
        [validation-error set-validation-error!] (use-state nil)

        ;; Determine if detail is loaded for the current entity
        detail-loaded? (and editing?
                         entity-id-str
                         select-detail
                         (entity-id-matches? select-detail entity-id-str entity-name))

        ;; Compute effective entity data:
        ;; - For edit: use loaded detail if available, else fall back to initial-row-data
        ;; - For create: use default-values
        effective-data (cond
                         (and editing? detail-loaded?) select-detail
                         editing? initial-row-data
                         :else default-values)

        ;; Memoize entity-spec to prevent fork resets
        ;; Only rebuild when spec content actually changes
        memo-spec (use-memo
                    (fn [] entity-spec)
                    [entity-spec])

        ;; Memoize initial values to prevent fork resets while typing
        ;; Use effective-data identity as dependency
        memo-initial-values (use-memo
                              (fn []
                                (let [normalized (if normalize-initial-data
                                                   (normalize-initial-data effective-data)
                                                   effective-data)]
                                  (merge default-values normalized)))
                              [effective-data normalize-initial-data default-values])

        ;; Handle form submission with validation
        handle-submit (fn [{:keys [values]}]
                        ;; Run custom validation if provided
                        (let [validation-result (when validate-values
                                                  (validate-values values))]
                          (cond
                            ;; Validation failed
                            (and validation-result (not (:ok? validation-result)))
                            (set-validation-error! (:error validation-result))

                            ;; Validation passed or no validation - prepare and submit
                            :else
                            (do
                              (set-validation-error! nil)
                              (let [prepared (if prepare-submit-values
                                               (prepare-submit-values values)
                                               values)]
                                (on-submit prepared))))))

        ;; NOTE: `load-detail!` is often passed as an inline fn.
        ;; Depending on it (or a callback derived from it) causes the load effect
        ;; to rerun on every render => repeated GETs + modal flicker.
        load-detail-ref (use-ref load-detail!)
        _ (reset! load-detail-ref load-detail!)]

    ;; Effect: Load detail when in edit mode
    (use-effect
      (fn []
        (when (and editing? entity-id-str)
          (when-let [f @load-detail-ref]
            ;; Mark requested before dispatching so errors can render deterministically.
            (set-requested-entity-id! entity-id-str)
            (f entity-id-str)))
        js/undefined)
      [entity-id-str editing?])

    ;; Render
    ($ :div {:class "space-y-2"}
      ;; Error alerts
      (when (and requested? detail-error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span detail-error)))

      (when validation-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span validation-error)))

      (cond
        ;; Edit mode: if we have no fallback row data, wait until the detail entity is loaded.
        ;; If `initial-row-data` is present, keep the form visible while detail loads to
        ;; avoid flicker (loading -> form -> loading -> form).
        (and editing? (not detail-loaded?) (nil? initial-row-data))
        ($ :div {:class "text-sm text-base-content/60"}
          "Loading...")

        ;; Custom form body component provided
        form-body-component
        ($ form-body-component
          {:key (str entity-name "-" (or entity-id-str "new"))
           :mode mode
           :initial-data memo-initial-values
           :on-cancel on-cancel
           :on-submit handle-submit})

        ;; Default: render standard form
        :else
        ($ form
          {:key (str entity-name "-" (or entity-id-str "new"))
           :entity-name entity-name
           :entity-spec memo-spec
           :editing editing?
           :initial-values memo-initial-values
           :on-cancel on-cancel
           :on-submit handle-submit
           :button-text (or button-text (if editing? "Update" "Save"))})))))
