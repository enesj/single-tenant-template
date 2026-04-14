(ns app.admin.frontend.components.receipt-approval-form
  "Admin receipt approval form used inside the admin receipt detail modal."
  (:require
    app.admin.frontend.events.receipts-approval
    app.admin.frontend.subs.receipts-detail
    [app.domain.frontend.expenses.components.form-fields.helpers :refer [current-datetime-local
                                                                         new-line-item]]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.user-expense-form.specs :as specs]
    [app.domain.frontend.expenses.ui.currencies :as currency-ui]
    [app.template.frontend.components.form :refer [form-fields]]
    [app.template.frontend.components.form.base :as base]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires: register admin lookup events and shared entity subs.
    [app.domain.frontend.expenses.events.expense-categories :as expense-categories-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    app.template.frontend.subs.entity))

(defn- payer-default?
  [payer]
  (boolean
    (or (:is-default payer)
      (:isDefault payer))))

(defn- default-payer-id
  [payers]
  (let [payers (or payers [])]
    (or (some (fn [payer]
                (when (payer-default? payer)
                  (:id payer)))
          payers)
      (:id (first payers)))))

(defui approval-form-body
  [{:keys [receipt-id receipt initial-data on-cancel on-success on-review-saved
           split-layout? suppliers payers expense-categories]}]
  (let [enabled-currencies currency-ui/fallback-currency-options
        form-error (use-subscribe [:admin/receipt-form-error])
        [validation-error set-validation-error!] (use-state nil)
        full-entity-spec (use-memo
                           #(specs/get-expense-form-spec suppliers payers
                              {:receipt-approval? true
                               :supplier-guess (some-> receipt :supplier-guess)
                               :receipt receipt
                               :receipt-id receipt-id
                               :expense-categories expense-categories
                               :enabled-currencies enabled-currencies
                               :supplier-component nil})
                           [suppliers payers expense-categories enabled-currencies receipt receipt-id])
        fields-only-spec (use-memo
                           #(specs/get-expense-form-spec suppliers payers
                              {:receipt-approval? true
                               :supplier-guess (some-> receipt :supplier-guess)
                               :receipt receipt
                               :receipt-id receipt-id
                               :expense-categories expense-categories
                               :enabled-currencies enabled-currencies
                               :exclude-line-items? true
                               :supplier-component nil})
                           [suppliers payers expense-categories enabled-currencies receipt receipt-id])
        line-items-spec (use-memo
                          #(vector specs/line-items-field-spec)
                          [])
        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency (currency-ui/default-currency {})
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])
        rid-str (or (some-> receipt-id str) "unknown")
        posted? (= "posted" (:status receipt))
        clear-errors! (fn [e]
                        (.preventDefault e)
                        (set-validation-error! nil)
                        (rf/dispatch [:admin/clear-receipt-form-error]))]
    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error flex items-center justify-between"}
          ($ :span (or validation-error form-error))
          ($ :button {:id (str "btn-clear-admin-receipt-approve-error-" rid-str)
                      :type "button"
                      :class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click clear-errors!}
            "✕")))

      ($ base/initialize-form
        {:entity-name "admin-receipt"
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
                            (if posted?
                              (rf/dispatch
                                [:admin/update-posted-receipt
                                 receipt-id
                                 (norm/prepare-expense-submit-values values)
                                 on-success])
                              (rf/dispatch
                                [:admin/approve-receipt
                                 receipt-id
                                 (norm/prepare-expense-submit-values values)
                                 on-success])))
                          (set-validation-error! (:error validation-result)))))

         :render-fn
         (fn [{:keys [form-id handle-submit submitting? values] :as form-props}]
           (let [expense-valid-now? (:ok? (norm/validate-expense-values values))
                 receipt-valid-now? (:ok? (norm/validate-receipt-review-values values))
                 can-save-receipt? (and receipt-valid-now?
                                     (norm/receipt-review-changed? form-initial-values values))]
             ($ :form {:id form-id
                       :on-submit handle-submit}
               ($ form-fields
                 (merge form-props
                   {:entity-name "admin-receipt"
                    :editing false
                    :values values
                    :form-id form-id
                    :entity-spec (if split-layout? fields-only-spec full-entity-spec)}))

               ($ :div {:class "flex justify-end gap-2 mt-4"}
                 (when on-cancel
                   ($ :button {:id (str "btn-cancel-admin-receipt-approve-" rid-str)
                               :type "button"
                               :class "ds-btn"
                               :disabled submitting?
                               :on-click (fn [e]
                                           (.preventDefault e)
                                           (when (fn? on-cancel)
                                             (on-cancel)))}
                     "Cancel"))
                 (if posted?
                   ($ :button {:id (str "btn-update-admin-posted-receipt-" rid-str)
                               :type "submit"
                               :class "ds-btn ds-btn-primary"
                               :disabled (or submitting? (not expense-valid-now?))}
                     "Update")
                   ($ :<>
                     ($ :button {:id (str "btn-save-admin-receipt-" rid-str)
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
                                                     [:admin/save-receipt-review
                                                      receipt-id
                                                      (norm/prepare-expense-submit-values values)
                                                      on-review-saved]))
                                                 (set-validation-error! (:error validation-result)))))}
                       "Save receipt")
                     ($ :button {:id (str "btn-save-admin-expense-" rid-str)
                                 :type "submit"
                                 :class "ds-btn ds-btn-primary"
                                 :disabled (or submitting? (not expense-valid-now?))}
                       "Save expense"))))

               (when split-layout?
                 ($ :div {:class "mt-6 -mx-4 px-4 pt-4 border-t border-base-300"}
                   ($ form-fields
                     (merge form-props
                       {:entity-name "admin-receipt"
                        :editing false
                        :values values
                        :form-id form-id
                        :entity-spec line-items-spec})))))))}))))

(defui receipt-approval-form
  [{:keys [receipt-id receipt initial-data on-success on-review-saved on-cancel
           split-layout?]}]
  (let [suppliers (or (use-subscribe [:app.template.frontend.subs.entity/entities :suppliers]) [])
        payers (or (use-subscribe [:app.template.frontend.subs.entity/entities :payers]) [])
        payers-loading? (boolean (use-subscribe [:app.template.frontend.subs.entity/loading? :payers]))
        expense-categories (or (use-subscribe [:app.template.frontend.subs.entity/entities :expense-categories]) [])
        [requested? set-requested!] (use-state false)
        [prepared-initial-data set-prepared-initial-data!] (use-state nil)
        posted? (= "posted" (:status receipt))
        linked-expense (:linked-expense receipt)
        receipt-initial-data (use-memo
                               #(cond
                                  initial-data initial-data
                                  (and posted? (map? linked-expense))
                                  (norm/normalize-initial-data linked-expense)
                                  receipt
                                  (norm/normalize-receipt-data receipt)
                                  :else nil)
                               [initial-data receipt posted? linked-expense])
        merged-initial-data (use-memo
                              #(merge {:purchased_at (current-datetime-local)
                                       :items [(new-line-item)]}
                                 receipt-initial-data)
                              [receipt-initial-data])]

    (use-effect
      (fn []
        (set-requested! false)
        (set-prepared-initial-data! nil)
        (when receipt-id
          (set-requested! true)
          (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
          (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
          (rf/dispatch [::expense-categories-events/load-list {:limit 500 :offset 0}]))
        js/undefined)
      [receipt-id])

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

    (if (and receipt-id (nil? prepared-initial-data))
      ($ :div {:class "flex justify-center p-6"}
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"}))

      ($ approval-form-body
        {:receipt-id receipt-id
         :receipt receipt
         :initial-data prepared-initial-data
         :on-cancel on-cancel
         :on-success on-success
         :on-review-saved on-review-saved
         :split-layout? split-layout?
         :suppliers suppliers
         :payers payers
         :expense-categories expense-categories}))))