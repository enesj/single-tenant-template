(ns app.domain.expenses.frontend.pages.expense-list
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.domain.expenses.frontend.events.expenses :as expenses-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- posted-badge [posted?]
  ($ shared/status-badge
    (if posted? "posted" "draft")
    {:default-class (if posted? "ds-badge ds-badge-success" "ds-badge ds-badge-warning")
     :capitalize? false
     :show-nil? true
     :nil-text "draft"}))

(defui admin-expense-list-page []
  (let [entries (use-subscribe [:expenses/entries])
        loading? (use-subscribe [:expenses/entries-loading?])
        error (use-subscribe [:expenses/entries-error])]
    (use-effect
      (fn []
        (rf/dispatch [::expenses-events/load-list {:limit 25}])
        js/undefined)
      [])
    ($ :div {:class "p-6 space-y-4"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div
          ($ :h1 {:class "text-2xl font-bold"} "Expenses")
          ($ :p {:class "text-sm text-base-content/70"} "Expense history"))
        ($ :div {:class "flex items-center gap-3"}
          ($ :button {:class "ds-btn ds-btn-outline"
                      :on-click #(rf/dispatch [::expenses-events/load-list {:limit 25}])}
            "Refresh")))
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))
      (if loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "table w-full"}
            ($ :thead
              ($ :tr
                ($ :th {:class "w-48"} "Supplier")
                ($ :th {:class "w-48"} "Payer")
                ($ :th {:class "w-28 text-right"} "Total")
                ($ :th {:class "w-36"} "Purchased")
                ($ :th {:class "w-24"} "Status")
                ($ :th {:class "w-24 text-right"} "")))
            ($ :tbody
              (for [{:keys [id supplier-display-name payer-label payer-type total-amount currency purchased-at is-posted] :as entry} entries]
                ($ :tr {:key (str id)}
                  ($ :td
                    ($ :div {:class "flex flex-col"}
                      ($ :span {:class "font-medium"} (shared/format-value supplier-display-name "—" false))
                      ($ :span {:class "text-xs text-base-content/70"}
                        (shared/format-value (:supplier-normalized-key entry) "" false))))
                  ($ :td
                    ($ :div {:class "flex flex-col"}
                      ($ :span {:class "font-medium"} (shared/format-value payer-label "—" false))
                      ($ :span {:class "text-xs text-base-content/70"}
                        (shared/format-value payer-type "" true))))
                  ($ :td {:class "text-right"}
                    ($ :span {:class "font-semibold"}
                      (shared/format-value total-amount "—" false))
                    (when currency
                      ($ :span {:class "ml-1 text-sm text-base-content/70"}
                        (str currency))))
                  ($ :td {:class "whitespace-nowrap"}
                    (or (shared/format-date purchased-at) "—"))
                  ($ :td (posted-badge is-posted))
                  ($ :td {:class "text-right"}
                    ($ :a {:href (str "/admin/expenses/" id)
                           :class "ds-btn ds-btn-ghost ds-btn-xs"}
                      "View")))))))))))
