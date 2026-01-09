(ns app.domain.frontend.expenses.admin.components.detail-views.payer
  "Payer detail view component."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui payer-detail-body
  [{:keys [payer-id]}]
  (let [payer (use-subscribe [:expenses/payer payer-id])
        loading? (use-subscribe [:expenses/payer-detail-loading?])
        error (use-subscribe [:expenses/payers-error])
        expenses (use-subscribe [:expenses/entries])]
    (use-effect
      (fn []
        (when payer-id
          (rf/dispatch [::payers-events/load-detail payer-id])
          (rf/dispatch [::expenses-events/load-list {:payer_id payer-id :limit 10 :offset 0}]))
        js/undefined)
      [payer-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? payer)
        ($ :div {:class "ds-alert"} ($ :span "Payer not found."))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (utils/label-value "Label" (:label payer))
            (utils/label-value "Type" (:type payer))
            (utils/label-value "Default" (if (true? (:is-default payer)) "Yes" "No"))
            (utils/label-value "Created At" (shared/format-date (:created-at payer)))
            (utils/label-value "ID" (:id payer)))

          ($ utils/related-table
            {:title "Recent Expenses"
             :rows expenses
             :columns [{:label "Supplier" :value-fn #(:supplier-display-name %)}
                       {:label "Purchased" :value-fn #(shared/format-date (:purchased-at %))}
                       {:label "Total" :value-fn #(utils/format-money (:total-amount %) (:currency %))}
                       {:label "Status" :value-fn #(:status %)}]
             :empty-label "No expenses for this payer yet."
             :view-all-href (when payer-id
                              (str "/admin/expenses?payer_id=" payer-id))
             :view-all-id (when payer-id
                            (str "btn-view-expenses-payer-" payer-id))}))))))
