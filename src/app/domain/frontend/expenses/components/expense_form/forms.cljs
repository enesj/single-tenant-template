(ns app.domain.frontend.expenses.components.expense-form.forms
  "UI building blocks for admin expense forms (form body + receipt approval)."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :refer [current-datetime-local
                                                                         new-line-item]]
    [app.domain.frontend.expenses.components.expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.expense-form.specs :as specs]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.template.frontend.components.form :refer [form form-fields]]
    [app.template.frontend.components.form.base :as base]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui expense-form-body
  "Internal form body component. Used by both add and edit modals."
  [{:keys [mode
           initial-data
           on-submit
           on-cancel
           _loading?
           new-supplier-default-display-name
           receipt-approval?
           receipt
           receipt-id]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])
        form-error (use-subscribe [:expenses/entries-error])
        [validation-error set-validation-error!] (use-state nil)

        ;; Memoize entity-spec to avoid recreating on every render.
        entity-spec (use-memo
                      #(specs/get-expense-form-spec suppliers payers
                         {:new-supplier-default-display-name new-supplier-default-display-name
                          :receipt-approval? receipt-approval?
                          :receipt receipt
                          :receipt-id receipt-id})
                      [suppliers
                       payers
                       new-supplier-default-display-name
                       receipt-approval?
                       receipt
                       receipt-id])

        ;; Memoize initial values so fork/form doesn't reset on every render.
        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency "BAM"
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])

        handle-submit (fn [{:keys [values]}]
                        (let [validation-result (norm/validate-expense-values values)]
                          (if (:ok? validation-result)
                            (do
                              (set-validation-error! nil)
                              (on-submit (norm/prepare-expense-submit-values values)))
                            (set-validation-error! (:error validation-result)))))

        save-disabled? (fn [values]
                         (empty? (norm/prepare-line-items (:items values))))]

    ;; Load dependencies.
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (or validation-error form-error))))

      ($ form
        {:entity-name "expense"
         :entity-spec entity-spec
         :editing (= mode :edit)
         :initial-values form-initial-values
         :on-cancel on-cancel
         :on-submit handle-submit
         :save-disabled? save-disabled?
         :button-text (if (= mode :edit) "Update Expense" "Save Expense")}))))

(defn- dirty?
  [dirty]
  (cond
    (nil? dirty) false
    (map? dirty) (seq dirty)
    (set? dirty) (seq dirty)
    (sequential? dirty) (seq dirty)
    :else true))

(defui receipt-approval-form
  "Receipt approval form with optional split layout for two-row modal display.

  Props:
  - :receipt-id - The receipt ID
  - :receipt - The receipt data
  - :initial-data - Initial form values
  - :on-cancel - Callback when cancel is clicked
  - :on-expense-saved - Callback when expense is saved
  - :on-review-saved - Callback when receipt review is saved
  - :split-layout? - If true, render fields in right column and line items get separate container
  - :line-items-container-ref - React ref to portal line items into (for split layout)"
  [{:keys [receipt-id receipt initial-data on-cancel on-expense-saved on-review-saved
           split-layout?]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])
        form-error (use-subscribe [:expenses/entries-error])
        [validation-error set-validation-error!] (use-state nil)

        ;; Full spec for the form
        full-entity-spec (use-memo
                           #(specs/get-expense-form-spec suppliers payers
                              {:new-supplier-default-display-name (norm/receipt-merchant-name receipt)
                               :receipt-approval? true
                               :receipt receipt
                               :receipt-id receipt-id})
                           [suppliers payers receipt receipt-id])

        ;; Spec without line items for split layout
        fields-only-spec (use-memo
                           #(specs/get-expense-form-spec suppliers payers
                              {:new-supplier-default-display-name (norm/receipt-merchant-name receipt)
                               :receipt-approval? true
                               :receipt receipt
                               :receipt-id receipt-id
                               :exclude-line-items? true})
                           [suppliers payers receipt receipt-id])

        ;; Line items spec
        line-items-spec (use-memo
                          #(vector specs/line-items-field-spec)
                          [])

        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency "BAM"
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])

        rid-str (or (some-> receipt-id str) "unknown")
        posted? (= "posted" (:status receipt))]

    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (or validation-error form-error))))

      ($ base/initialize-form
        {:entity-name "user-expense"
         :entity-spec full-entity-spec
         :editing false
         :initial-values form-initial-values
         :prevent-default? true
         :keywordize-keys true

         :on-submit (fn [{:keys [values]}]
                      (let [validation-result (norm/validate-expense-values values)]
                        (if (:ok? validation-result)
                          (do
                            (set-validation-error! nil)
                            (rf/dispatch
                              [:app.domain.frontend.expenses.events.receipts/approve-receipt
                               receipt-id
                               (norm/prepare-expense-submit-values values)
                               on-expense-saved]))
                          (set-validation-error! (:error validation-result)))))

         :render-fn
         (fn [{:keys [form-id handle-submit dirty submitting? values] :as form-props}]
           (let [expense-valid-now? (:ok? (norm/validate-expense-values values))
                 receipt-valid-now? (:ok? (norm/validate-receipt-review-values values))
                 can-save-receipt? (and receipt-valid-now? (dirty? dirty))]
             ($ :form {:id form-id
                       :on-submit handle-submit}
               ;; Fields section (without line items if split layout)
               ($ form-fields
                 (merge form-props
                   {:entity-name "user-expense"
                    :editing false
                    :values values
                    :form-id form-id
                    :entity-spec (if split-layout? fields-only-spec full-entity-spec)}))

               ;; Buttons
               ($ :div {:class "flex justify-end gap-2 mt-4"}
                 (when on-cancel
                   ($ :button {:id (str "btn-cancel-receipt-approve-" rid-str)
                               :type "button"
                               :class "ds-btn"
                               :disabled submitting?
                               :on-click (fn [e]
                                           (.preventDefault e)
                                           (when (fn? on-cancel) (on-cancel)))}
                     "Cancel"))
                 ($ :button {:id (str "btn-save-receipt-" rid-str)
                             :type "button"
                             :class "ds-btn ds-btn-outline"
                             :disabled (or posted? submitting? (not can-save-receipt?))
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (.stopPropagation e)
                                         (let [validation-result (norm/validate-receipt-review-values values)]
                                           (if (:ok? validation-result)
                                             (do
                                               (set-validation-error! nil)
                                               (rf/dispatch
                                                 [:app.domain.frontend.expenses.events.receipts/save-receipt-review
                                                  receipt-id
                                                  (norm/prepare-expense-submit-values values)
                                                  on-review-saved]))
                                             (set-validation-error! (:error validation-result)))))
                             :title (cond
                                      posted?
                                      "Receipt is already posted."

                                      (and (not submitting?) (not can-save-receipt?))
                                      "Update receipt fields before saving")}
                   "Save receipt")
                 ($ :button {:id (str "btn-save-expense-" rid-str)
                             :type "submit"
                             :class "ds-btn ds-btn-primary"
                             :disabled (or posted? submitting? (not expense-valid-now?))
                             :title (cond
                                      posted?
                                      "Receipt is already posted."

                                      (and (not submitting?) (not expense-valid-now?))
                                      "Supplier, payer, date, line items, and totals must be valid")}
                   "Save expense"))

               ;; Line items section for split layout (rendered after buttons, full width)
               (when split-layout?
                 ($ :div {:class "mt-6 -mx-4 px-4 pt-4 border-t border-base-300"}
                   ($ form-fields
                     (merge form-props
                       {:entity-name "user-expense"
                        :editing false
                        :values values
                        :form-id form-id
                        :entity-spec line-items-spec})))))))}))))