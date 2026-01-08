(ns app.domain.frontend.expenses.components.user-expense-form.modals
  "Modal wrappers for user-scoped expense forms."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 new-line-item]]
    [app.domain.frontend.expenses.components.user-expense-form.forms :as forms]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.user-expense-form.specs :as specs]
    [app.template.frontend.components.form.master-detail :refer [master-detail-form]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- payer-default?
  [payer]
  (boolean
    (or (:is_default payer)
      (:is-default payer)
      (:isDefault payer))))

(defn- default-payer-id
  [payers]
  (let [payers (or payers [])]
    (or (some (fn [p]
                (when (payer-default? p)
                  (:id p)))
          payers)
      (:id (first payers)))))

(defui user-expense-add-form-modal
  [{:keys [receipt-id receipt on-success on-review-saved on-cancel]}]
  (let [payers (or (use-subscribe [:user-expenses/payers]) [])
        payers-loading? (boolean (use-subscribe [:user-expenses/payers-loading?]))
        [requested? set-requested!] (use-state false)
        [prepared-initial-data set-prepared-initial-data!] (use-state nil)
        supplier-guess (some-> receipt :supplier-guess)
        receipt-initial-data (use-memo
                               #(when receipt (norm/normalize-receipt-data receipt))
                               [receipt])
        merged-initial-data (use-memo
                              #(merge {:purchased_at (current-datetime-local)
                                       :items [(new-line-item)]}
                                 receipt-initial-data)
                              [receipt-initial-data])]

    ;; Load dependencies (suppliers/payers) early so we can set defaults before the form mounts.
    (use-effect
      (fn []
        (set-requested! true)
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ;; Lock in initial values once payers have loaded so Fork doesn't reset mid-edit.
    (use-effect
      (fn []
        (when (and requested?
                (nil? prepared-initial-data)
                (or (seq payers) (not payers-loading?)))
          (let [existing-payer-id (some-> (:payer_id merged-initial-data) str str/trim not-empty)
                default-id (some-> (default-payer-id payers) str str/trim not-empty)
                prepared (cond-> merged-initial-data
                           (and (nil? existing-payer-id) default-id)
                           (assoc :payer_id default-id))]
            (set-prepared-initial-data! prepared)))
        js/undefined)
      [requested? prepared-initial-data payers payers-loading? merged-initial-data])

    (if (nil? prepared-initial-data)
      ($ :div {:class "flex justify-center p-6"}
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"}))

      (if receipt-id
        ($ forms/receipt-approval-form
          {:receipt-id receipt-id
           :receipt receipt
           :initial-data prepared-initial-data
           :on-cancel on-cancel
           :on-review-saved on-review-saved
           :on-expense-saved on-success})

        ($ forms/user-expense-form-body
          {:mode :create
           :receipt-approval? false
           :supplier-guess supplier-guess
           :initial-data prepared-initial-data
           :on-cancel on-cancel
           :on-submit (fn [form-data]
                        (rf/dispatch [:user-expenses/create-expense-modal form-data on-success]))})))))

(defui user-expense-edit-form-modal
  "Edit user expense modal using master-detail-form wrapper for detail orchestration."
  [{:keys [expense-id initial-data on-success on-cancel]}]
  (let [expense-id-str (some-> expense-id str)
        ;; Subscribe to detail state
        current-expense (use-subscribe [:user-expenses/current-expense])
        detail-loading? (boolean (use-subscribe [:user-expenses/current-expense-loading?]))
        detail-error (use-subscribe [:user-expenses/current-expense-error])
        suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])

        ;; Memoize entity-spec
        entity-spec (use-memo
                      #(specs/get-expense-form-spec suppliers payers)
                      [suppliers payers])

        ;; Default values for expense form
        ;; Memoized to keep identity stable across renders (prevents fork resets).
        default-values (use-memo
                         (fn []
                           {:currency "BAM"
                            :purchased_at (current-datetime-local)
                            :items [(new-line-item)]})
                         [])]

    ;; Load dependencies (suppliers/payers)
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ master-detail-form
      {:mode :edit
       :entity-name "user-expense"
       :entity-spec entity-spec
       :entity-id expense-id-str

       ;; Detail orchestration
       :load-detail! (fn [id] (rf/dispatch [:user-expenses/fetch-expense id]))
       :select-detail current-expense
       :detail-loading? detail-loading?
       :detail-error detail-error

       ;; Data transformation
       :normalize-initial-data norm/normalize-initial-data
       :validate-values norm/validate-expense-values
       :prepare-submit-values norm/prepare-expense-submit-values

       ;; Callbacks
       :on-submit (fn [prepared-data]
                    (rf/dispatch [:user-expenses/update-expense-modal expense-id-str prepared-data on-success]))
       :on-cancel on-cancel

       ;; Optional
       :initial-row-data initial-data
       :default-values default-values
       :button-text "Update Expense"})))
