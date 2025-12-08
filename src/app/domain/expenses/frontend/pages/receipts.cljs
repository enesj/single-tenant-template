(ns app.domain.expenses.frontend.pages.receipts
  (:require
    [app.domain.expenses.frontend.events.receipts :as receipts-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-receipts-page []
  (let [receipts (use-subscribe [:expenses/receipts])
        loading? (use-subscribe [:expenses/receipts-loading?])
        error (use-subscribe [:expenses/receipts-error])]
    (use-effect
      (fn []
        (rf/dispatch [::receipts-events/load-list {:limit 25}])
        js/undefined)
      [])
    ($ :div {:class "p-6 space-y-4"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div
          ($ :h1 {:class "text-2xl font-bold"} "Receipts")
          ($ :p {:class "text-sm text-base-content/70"} "Receipt inbox and statuses"))
        ($ :button {:class "ds-btn ds-btn-primary"
                    :on-click #(rf/dispatch [::receipts-events/load-list {:limit 25}])}
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
                ($ :th "File")
                ($ :th "Status")
                ($ :th "Guessed Supplier")
                ($ :th "Created")))
            ($ :tbody
              (for [{:keys [id original_filename status supplier_guess created_at]} receipts]
                ($ :tr {:key (str id)}
                  ($ :td (or original_filename ""))
                  ($ :td (or status ""))
                  ($ :td (or supplier_guess ""))
                  ($ :td (or created_at "")))))))))))
