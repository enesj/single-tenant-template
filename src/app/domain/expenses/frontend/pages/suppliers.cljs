(ns app.domain.expenses.frontend.pages.suppliers
  (:require
    [app.domain.expenses.frontend.events.suppliers :as suppliers-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-suppliers-page []
  (let [suppliers (use-subscribe [:expenses/suppliers])
        loading? (use-subscribe [:expenses/suppliers-loading?])
        error (use-subscribe [:expenses/suppliers-error])]
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load {:limit 50}])
        js/undefined)
      [])
    ($ :div {:class "p-6 space-y-4"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div
          ($ :h1 {:class "text-2xl font-bold"} "Suppliers")
          ($ :p {:class "text-sm text-base-content/70"} "Manage expense suppliers"))
        ($ :button {:class "ds-btn ds-btn-primary"
                    :on-click #(rf/dispatch [::suppliers-events/load {:limit 50}])}
          "Refresh"))
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))
      (if loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "table table-zebra w-full"}
            ($ :thead
              ($ :tr
                ($ :th "Name")
                ($ :th "Normalized Key")
                ($ :th "Created")))
            ($ :tbody
              (for [{:keys [id display_name normalized_key created_at]} suppliers]
                ($ :tr {:key (str id)}
                  ($ :td display_name)
                  ($ :td normalized_key)
                  ($ :td (or created_at "")))))))))))
