(ns app.domain.expenses.frontend.pages.payers
  (:require
    [app.domain.expenses.frontend.events.payers :as payers-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-payers-page []
  (let [payers (use-subscribe [:expenses/payers])
        loading? (use-subscribe [:expenses/payers-loading?])
        error (use-subscribe [:expenses/payers-error])]
    (use-effect
      (fn []
        (rf/dispatch [::payers-events/load {}])
        js/undefined)
      [])
    ($ :div {:class "p-6 space-y-4"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div
          ($ :h1 {:class "text-2xl font-bold"} "Payers")
          ($ :p {:class "text-sm text-base-content/70"} "Payment sources and defaults"))
        ($ :button {:class "ds-btn ds-btn-primary"
                    :on-click #(rf/dispatch [::payers-events/load {}])}
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
                ($ :th "Label")
                ($ :th "Type")
                ($ :th "Default?")))
            ($ :tbody
              (for [{:keys [id label type is_default]} payers]
                ($ :tr {:key (str id)}
                  ($ :td label)
                  ($ :td (or type ""))
                  ($ :td (if is_default "Yes" "")))))))))))
