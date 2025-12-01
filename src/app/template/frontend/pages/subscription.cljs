(ns app.template.frontend.pages.subscription
  "Subscription management page"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :as icons]
    [app.template.frontend.components.subscription :as sub-components]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui]]))

(defui subscription-page
  "Main subscription management page"
  []
  ($ :div.container.mx-auto.px-4.py-8
    {:id "subscription-page"}

     ;; Page header
    ($ :div.flex.items-center.justify-between.mb-8
      ($ :div
        ($ :h1.text-3xl.font-bold.text-base-content
          "Subscription Management")
        ($ :p.text-base-content.opacity-70.mt-2
          "Manage your subscription plan and usage"))
      ($ :div.flex.items-center.space-x-2
        ($ button
          {:btn-type :outline
           :class "ds-btn-sm"
           :id "btn-refresh-subscription"
           :on-click #(rf/dispatch [:subscription/fetch-status])}
          ($ icons/arrow-path {:class "w-4 h-4"})
          "Refresh")))

     ;; Main subscription dashboard
    ($ sub-components/subscription-dashboard)))
