(ns app.domain.frontend.expenses.pages.admin.price-observation-detail
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-price-observation-detail-page []
  ($ layout/admin-layout
    (let [current-route (use-subscribe [:current-route])
          obs-id (get-in current-route [:path-params :id])
          obs (use-subscribe [:expenses/price-observation obs-id])]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "space-y-1"}
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/price-observations"} "Price Observations"))
                ($ :li (str (or (:article-canonical-name obs) "Observation")))))
            ($ :h1 {:class "text-2xl font-bold"} "Price Observation Detail"))
          ($ :div {:class "flex items-center gap-2"}
            ($ :a {:id "btn-back-price-observations"
                   :href "/admin/price-observations"
                   :class "ds-btn ds-btn-ghost ds-btn-sm"}
              "Back")
            ($ :button {:id (str "btn-refresh-price-observations-" obs-id)
                        :class "ds-btn ds-btn-outline ds-btn-sm"
                        :on-click #(when obs-id
                                     (rf/dispatch [::price-obs-events/load-detail obs-id]))}
              "Refresh")))

        ($ detail-views/price-observation-detail-body {:observation-id obs-id})))))
