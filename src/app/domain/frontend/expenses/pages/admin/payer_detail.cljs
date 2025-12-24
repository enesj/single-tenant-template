(ns app.domain.frontend.expenses.pages.admin.payer-detail
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-payer-detail-page []
  ($ layout/admin-layout
    (let [current-route (use-subscribe [:current-route])
          payer-id (get-in current-route [:path-params :id])
          payer (use-subscribe [:expenses/payer payer-id])]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "space-y-1"}
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/payers"} "Payers"))
                ($ :li (str (or (:label payer) "Payer")))))
            ($ :h1 {:class "text-2xl font-bold"} "Payer Detail"))
          ($ :div {:class "flex items-center gap-2"}
            ($ :a {:id "btn-back-payers"
                   :href "/admin/payers"
                   :class "ds-btn ds-btn-ghost ds-btn-sm"}
              "Back")
            ($ :button {:id (str "btn-refresh-payers-" payer-id)
                        :class "ds-btn ds-btn-outline ds-btn-sm"
                        :on-click #(when payer-id
                                     (rf/dispatch [::payers-events/load-detail payer-id]))}
              "Refresh")))

        ($ detail-views/payer-detail-body {:payer-id payer-id})))))
