(ns app.domain.frontend.expenses.admin.components.detail-views.expense-item
  "Expense item detail view component."
  (:require
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui expense-item-detail-body
  [{:keys [expense-item-id]}]
  (let [expense-item (use-subscribe [:expenses/expense-item expense-item-id])
        loading? (use-subscribe [:expenses/expense-item-detail-loading?])
        error (use-subscribe [:expenses/expense-items-error])]
    (use-effect
      (fn []
        (when expense-item-id
          (rf/dispatch [:app.domain.frontend.expenses.events.expense-items/load-detail expense-item-id]))
        js/undefined)
      [expense-item-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "flex justify-center p-12"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))

        (not expense-item)
        ($ :div {:class "text-center p-12 text-base-content/70"}
          "Expense item not found.")

        :else
        ($ :div {:class "space-y-6"}
          ;; Core Info
          ($ :div {:class "grid grid-cols-1 md:grid-cols-3 gap-4"}
            (utils/label-value "Raw Label" (:raw-label expense-item))
            (utils/label-value "Normalized Label" (:raw-label-normalized expense-item))
            (utils/label-value "Quantity" (:qty expense-item))
            (utils/label-value "Unit Price" (:unit-price expense-item))
            (utils/label-value "Line Total" (:line-total expense-item))
            (utils/label-value "Expense ID" (:expense-id expense-item))
            (utils/label-value "Article ID" (:article-id expense-item))
            (utils/label-value "Created At" (:created-at expense-item))
            (utils/label-value "Updated At" (:updated-at expense-item))))))))
