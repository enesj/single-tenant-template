(ns app.domain.expenses.frontend.pages.expense-detail
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.domain.expenses.frontend.events.expenses :as expenses-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- label-value [label value]
  ($ :div {:class "flex flex-col gap-1 p-3 bg-base-200 rounded-lg"}
    ($ :span {:class "text-xs uppercase tracking-wide text-base-content/70"} label)
    ($ :span {:class "text-sm font-medium"} (shared/format-value value "—" false))))

(defui admin-expense-detail-page []
  (let [current-route (use-subscribe [:current-route])
        entry-id (get-in current-route [:path-params :id])
        entry (use-subscribe [:expenses/entry entry-id])
        loading? (use-subscribe [:expenses/entry-detail-loading?])
        error (use-subscribe [:expenses/entries-error])]
    (use-effect
      (fn []
        (when entry-id
          (rf/dispatch [::expenses-events/load-detail entry-id]))
        js/undefined)
      [entry-id])
    ($ :div {:class "p-6 space-y-6"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div {:class "space-y-1"}
          ($ :div {:class "text-sm breadcrumbs"}
            ($ :ul
              ($ :li ($ :a {:href "/admin/expenses"} "Expenses"))
              ($ :li (str (or (:supplier-display-name entry) "Expense")))))
          ($ :h1 {:class "text-2xl font-bold"} "Expense Detail"))
        ($ :div {:class "flex items-center gap-2"}
          ($ :a {:href "/admin/expenses" :class "ds-btn ds-btn-ghost ds-btn-sm"} "Back")
          ($ :button {:class "ds-btn ds-btn-outline ds-btn-sm"
                      :on-click #(when entry-id (rf/dispatch [::expenses-events/load-detail entry-id]))}
            "Refresh")))

      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? entry)
        ($ :div {:class "ds-alert"} ($ :span "Expense not found."))

        :else
        ($ :div {:class "space-y-4"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (label-value "Supplier" (:supplier-display-name entry))
            (label-value "Payer" (:payer-label entry))
            (label-value "Payer Type" (:payer-type entry))
            (label-value "Total Amount" (when-let [amt (:total-amount entry)] (str amt " " (or (:currency entry) ""))))
            (label-value "Purchased" (or (shared/format-date (:purchased-at entry)) "—"))
            (label-value "Status" (if (:is-posted entry) "Posted" "Draft"))
            (label-value "Currency" (:currency entry))
            (label-value "Notes" (:notes entry)))

          ($ :div {:class "bg-base-200 rounded-xl p-4 space-y-3"}
            ($ :div {:class "flex items-center justify-between"}
              ($ :h2 {:class "text-lg font-semibold"} "Line Items")
              ($ :span {:class "text-sm text-base-content/70"}
                (str (count (:items entry)) " items")))
            (if (seq (:items entry))
              ($ :div {:class "overflow-x-auto"}
                ($ :table {:class "table w-full"}
                  ($ :thead
                    ($ :tr
                      ($ :th "Label")
                      ($ :th "Qty")
                      ($ :th "Unit Price")
                      ($ :th "Line Total")
                      ($ :th "Article")))
                  ($ :tbody
                    (for [{:keys [id raw-label qty unit-price line-total article-id]} (:items entry)]
                      ($ :tr {:key (str id)}
                        ($ :td raw-label)
                        ($ :td (shared/format-value qty "" false))
                        ($ :td (shared/format-value unit-price "" false))
                        ($ :td (shared/format-value line-total "" false))
                        ($ :td (or (some-> article-id str) "—")))))))
              ($ :div {:class "text-sm text-base-content/70"} "No items recorded."))))))))
