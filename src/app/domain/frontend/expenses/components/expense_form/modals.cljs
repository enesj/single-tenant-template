(ns app.domain.frontend.expenses.components.expense-form.modals
  "Modal wrappers for admin expense forms."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 new-line-item]]
    [app.domain.frontend.expenses.components.expense-form.forms :as forms]
    [app.domain.frontend.expenses.components.expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.expense-form.specs :as specs]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
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

(defui expense-add-form-modal
  [{:keys [receipt-id initial-data on-success on-review-saved on-cancel]}]
  (let [loading? (use-subscribe [:expenses/form-loading?])
        receipt (use-subscribe [:expenses/receipt receipt-id])
        payers (or (use-subscribe [:expenses/payers]) [])
        payers-loading? (boolean (use-subscribe [:expenses/payers-loading?]))
        [requested? set-requested!] (use-state false)
        [prepared-initial-data set-prepared-initial-data!] (use-state nil)
        default-supplier-display-name (use-memo
                                        #(when receipt (norm/receipt-merchant-name receipt))
                                        [receipt])

        ;; If we have a receipt, normalize its data as initial values.
        receipt-initial-data (use-memo
                               #(when receipt (norm/normalize-receipt-data receipt))
                               [receipt])

        ;; Merge provided initial-data with receipt data.
        merged-initial-data (use-memo
                              #(merge receipt-initial-data initial-data)
                              [receipt-initial-data initial-data])]

    ;; For receipt approval, load dependencies early so we can preselect defaults
    ;; before the form mounts (avoids Fork resetting mid-edit).
    (use-effect
      (fn []
        (when receipt-id
          (set-requested! true)
          (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
          (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}]))
        js/undefined)
      [receipt-id])

    ;; Lock in initial values once payers have loaded so the payer default is selected.
    (use-effect
      (fn []
        (when (and receipt-id
                requested?
                (nil? prepared-initial-data)
                (or (seq payers) (not payers-loading?)))
          (let [existing-payer-id (some-> (:payer_id merged-initial-data) str str/trim not-empty)
                default-id (some-> (default-payer-id payers) str str/trim not-empty)
                prepared (cond-> merged-initial-data
                           (and (nil? existing-payer-id) default-id)
                           (assoc :payer_id default-id))]
            (set-prepared-initial-data! prepared)))
        js/undefined)
      [receipt-id requested? prepared-initial-data payers payers-loading? merged-initial-data])

    (if receipt-id
      (if (nil? prepared-initial-data)
        ($ :div {:class "flex justify-center p-6"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"}))

        ($ forms/receipt-approval-form
          {:receipt-id receipt-id
           :receipt receipt
           :initial-data prepared-initial-data
           :on-cancel on-cancel
           :on-review-saved on-review-saved
           :on-expense-saved on-success}))

      ($ forms/expense-form-body
        {:mode :create
         :loading? loading?
         :initial-data merged-initial-data
         :new-supplier-default-display-name default-supplier-display-name
         :receipt-approval? false
         :receipt nil
         :receipt-id nil
         :on-cancel on-cancel
         :on-submit (fn [form-data]
                      (rf/dispatch [::expenses-events/create-entry-modal form-data on-success]))}))))

(defui expense-edit-form-modal
  "Edit expense modal using master-detail-form wrapper for detail orchestration."
  [{:keys [expense-id initial-data on-success on-cancel]}]
  (let [expense-id-str (some-> expense-id str)
        ;; Subscribe to detail state.
        detail-entity (use-subscribe [:expenses/entry expense-id-str])
        detail-loading? (boolean (use-subscribe [:expenses/entry-detail-loading?]))
        detail-error (use-subscribe [:expenses/entries-error])
        suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])

        ;; Memoize entity-spec.
        entity-spec (use-memo
                      #(specs/get-expense-form-spec suppliers payers)
                      [suppliers payers])

        ;; Default values for expense form.
        ;; Memoized to keep identity stable across renders (prevents fork resets).
        default-values (use-memo
                         (fn []
                           {:currency "BAM"
                            :purchased_at (current-datetime-local)
                            :items [(new-line-item)]})
                         [])]

    ;; Load dependencies (suppliers/payers).
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ master-detail-form
      {:mode :edit
       :entity-name "expense"
       :entity-spec entity-spec
       :entity-id expense-id-str

       ;; Detail orchestration.
       :load-detail! (fn [id] (rf/dispatch [::expenses-events/load-detail id]))
       :select-detail detail-entity
       :detail-loading? detail-loading?
       :detail-error detail-error

       ;; Data transformation.
       :normalize-initial-data norm/normalize-initial-data
       :validate-values norm/validate-expense-values
       :prepare-submit-values norm/prepare-expense-submit-values

       ;; Callbacks.
       :on-submit (fn [prepared-data]
                    (rf/dispatch [::expenses-events/update-entry-modal expense-id-str prepared-data on-success]))
       :on-cancel on-cancel

       ;; Optional.
       :initial-row-data initial-data
       :default-values default-values
       :button-text "Update Expense"})))
