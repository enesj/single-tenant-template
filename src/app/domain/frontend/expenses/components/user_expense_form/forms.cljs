(ns app.domain.frontend.expenses.components.user-expense-form.forms
  "UI building blocks for user-scoped expense forms (form body + receipt approval)."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 new-line-item]]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.user-expense-form.specs :as specs]
    [app.template.frontend.components.form :refer [form form-fields]]
    [app.template.frontend.components.form.base :as base]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- dirty?
  [dirty]
  (cond
    (nil? dirty) false
    (map? dirty) (seq dirty)
    (set? dirty) (seq dirty)
    (sequential? dirty) (seq dirty)
    :else true))

(defui user-expense-form-body
  [{:keys [mode initial-data on-submit on-cancel receipt-approval? supplier-guess]}]
  (let [suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        form-error (use-subscribe [:user-expenses/form-error])
        [validation-error set-validation-error!] (use-state nil)

        ;; Memoize entity-spec to avoid recreating on every render.
        ;; Only rebuild when suppliers or payers content actually changes.
        entity-spec (use-memo
                      #(specs/get-expense-form-spec suppliers payers
                         {:receipt-approval? receipt-approval?
                          :supplier-guess supplier-guess
                          :receipt nil
                          :receipt-id nil})
                      [suppliers payers receipt-approval? supplier-guess])

        ;; Memoize initial values so fork/form doesn't reset on every render.
        ;; Use initial-data identity as the dependency (it's passed from parent).
        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency "BAM"
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])

        clear-errors! (fn [e]
                        (.preventDefault e)
                        (set-validation-error! nil)
                        (rf/dispatch [:user-expenses/clear-form-error]))

        handle-submit (fn [{:keys [values]}]
                        (let [validation-result (norm/validate-expense-values values)]
                          (if (:ok? validation-result)
                            (do
                              (set-validation-error! nil)
                              (on-submit (norm/prepare-expense-submit-values values)))
                            (set-validation-error! (:error validation-result)))))]

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error flex items-center justify-between"}
          ($ :span (or validation-error form-error))
          ($ :button {:id "btn-clear-expense-form-error"
                      :type "button"
                      :class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click clear-errors!}
            "✕")))

      ($ form
        {:entity-name "user-expense"
         :entity-spec entity-spec
         :editing (= mode :edit)
         :initial-values form-initial-values
         :on-cancel on-cancel
         :on-submit handle-submit
         :save-disabled? (fn [values]
                           (empty? (norm/prepare-line-items (:items values))))
         :button-text (if (= mode :edit) "Update Expense" "Save Expense")}))))

(defui receipt-approval-form
  [{:keys [receipt-id receipt initial-data on-cancel on-expense-saved on-review-saved]}]
  (let [suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        form-error (use-subscribe [:user-expenses/form-error])
        [validation-error set-validation-error!] (use-state nil)

        entity-spec (use-memo
                      #(specs/get-expense-form-spec suppliers payers
                         {:receipt-approval? true
                          :supplier-guess (some-> receipt :supplier-guess)
                          :receipt receipt
                          :receipt-id receipt-id})
                      [suppliers payers receipt receipt-id])

        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency "BAM"
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])

        rid-str (or (some-> receipt-id str) "unknown")
        clear-errors! (fn [e]
            (.preventDefault e)
            (set-validation-error! nil)
            (rf/dispatch [:user-expenses/clear-form-error]))]

    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error flex items-center justify-between"}
          ($ :span (or validation-error form-error))
          ($ :button {:id (str "btn-clear-receipt-approve-error-" rid-str)
                      :type "button"
                      :class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click clear-errors!}
            "✕")))

      ($ base/initialize-form
        {:entity-name "user-expense"
         :entity-spec entity-spec
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
                              [:user-expenses/approve-receipt
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
               ($ form-fields
                 (merge form-props
                   {:entity-name "user-expense"
                    :editing false
                    :values values
                    :form-id form-id
                    :entity-spec entity-spec}))

               ($ :div {:class "flex justify-end gap-2"}
                 ($ :button {:id (str "btn-cancel-receipt-approve-" rid-str)
                             :type "button"
                             :class "ds-btn"
                             :disabled submitting?
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (when (fn? on-cancel) (on-cancel)))}
                   "Cancel")
                 ($ :button {:id (str "btn-save-receipt-" rid-str)
                             :type "button"
                             :class "ds-btn ds-btn-outline"
                             :disabled (or submitting? (not can-save-receipt?))
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (.stopPropagation e)
                                         (let [validation-result (norm/validate-receipt-review-values values)]
                                           (if (:ok? validation-result)
                                             (do
                                               (set-validation-error! nil)
                                               (rf/dispatch
                                                 [:user-expenses/save-receipt-review
                                                  receipt-id
                                                  (norm/prepare-expense-submit-values values)
                                                  on-review-saved]))
                                             (set-validation-error! (:error validation-result)))))}
                   "Save receipt")
                 ($ :button {:id (str "btn-save-expense-" rid-str)
                             :type "submit"
                             :class "ds-btn ds-btn-primary"
                             :disabled (or submitting? (not expense-valid-now?))}
                   "Save expense")))))}))))
