(ns app.domain.frontend.expenses.pages.admin.receipt-detail
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-receipt-detail-page []
  ($ layout/admin-layout
    (let [current-route (use-subscribe [:current-route])
          receipt-id (get-in current-route [:path-params :id])
          receipt (use-subscribe [:expenses/receipt receipt-id])]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "space-y-1"}
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/receipts"} "Receipts"))
                ($ :li (str (or (:original-filename receipt) "Receipt")))))
            ($ :h1 {:class "text-2xl font-bold"} "Receipt Detail"))
          ($ :div {:class "flex items-center gap-2"}
            ($ :a {:id "btn-back-receipts"
                   :href "/admin/receipts"
                   :class "ds-btn ds-btn-ghost ds-btn-sm"}
              "Back")))

        ($ detail-views/receipt-detail-body {:receipt-id receipt-id})))))
