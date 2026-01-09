(ns app.domain.frontend.expenses.admin.components.detail-views.price-observation
  "Price observation detail view component."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui price-observation-detail-body
  [{:keys [observation-id]}]
  (let [obs (use-subscribe [:expenses/price-observation observation-id])
        loading? (use-subscribe [:expenses/price-observation-detail-loading?])
        error (use-subscribe [:expenses/price-observations-error])]
    (use-effect
      (fn []
        (when observation-id
          (rf/dispatch [::price-obs-events/load-detail observation-id]))
        js/undefined)
      [observation-id])

    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? obs)
        ($ :div {:class "ds-alert"} ($ :span "Observation not found."))

        :else
        ($ :div {:class "grid gap-3 md:grid-cols-3"}
          (utils/label-value "Article" (:article-canonical-name obs))
          (utils/label-value "Supplier" (:supplier-display-name obs))
          (utils/label-value "Observed At" (shared/format-date (:observed-at obs)))
          (utils/label-value "Qty" (:qty obs))
          (utils/label-value "Unit Price" (:unit-price obs))
          (utils/label-value "Line Total" (:line-total obs))
          (utils/label-value "Currency" (:currency obs))
          (utils/label-value "Expense Item ID" (:expense-item-id obs))
          (utils/label-value "Article ID" (:article-id obs))
          (utils/label-value "Supplier ID" (:supplier-id obs))
          (utils/label-value "Created At" (shared/format-date (:created-at obs)))
          (utils/label-value "ID" (:id obs)))))))
