(ns app.domain.frontend.expenses.pages.admin.supplier-detail
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-supplier-detail-page []
  ($ layout/admin-layout
    (let [current-route (use-subscribe [:current-route])
          supplier-id (get-in current-route [:path-params :id])
          supplier (use-subscribe [:expenses/supplier supplier-id])]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "space-y-1"}
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/suppliers"} "Suppliers"))
                ($ :li (str (or (:display-name supplier) "Supplier")))))
            ($ :h1 {:class "text-2xl font-bold"} "Supplier Detail"))
          ($ :div {:class "flex items-center gap-2"}
            ($ :a {:id "btn-back-suppliers"
                   :href "/admin/suppliers"
                   :class "ds-btn ds-btn-ghost ds-btn-sm"}
              "Back")
            ($ :button {:id (str "btn-refresh-suppliers-" supplier-id)
                        :class "ds-btn ds-btn-outline ds-btn-sm"
                        :on-click #(when supplier-id
                                     (rf/dispatch [::suppliers-events/load-detail supplier-id]))}
              "Refresh")))

        ($ detail-views/supplier-detail-body {:supplier-id supplier-id})))))
