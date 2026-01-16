(ns app.domain.frontend.expenses.admin.components.detail-views.supplier
  "Supplier detail view component."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    app.domain.frontend.expenses.subs.suppliers
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui supplier-detail-body
  [{:keys [supplier-id]}]
  (let [supplier (use-subscribe [:expenses/supplier supplier-id])
        loading? (use-subscribe [:expenses/supplier-detail-loading?])
        error (use-subscribe [:expenses/suppliers-error])
  archiving? (use-subscribe [:expenses/supplier-archive-loading?])
        expenses (use-subscribe [:expenses/entries])
        aliases (use-subscribe [:expenses/article-aliases])
    observations (use-subscribe [:expenses/price-observations])
    archived-at (some-> supplier :archived-at)
    archived? (some? archived-at)
    supplier-id-str (some-> supplier-id str)]
    (use-effect
      (fn []
        (when supplier-id
          (rf/dispatch [::suppliers-events/load-detail supplier-id])
          (rf/dispatch [::expenses-events/load-list {:supplier_id supplier-id :limit 10 :offset 0}])
          (rf/dispatch [::aliases-events/load-list {:supplier_id supplier-id :limit 10 :offset 0}])
          (rf/dispatch [::price-obs-events/load-list {:supplier_id supplier-id :limit 10 :offset 0}]))
        js/undefined)
      [supplier-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? supplier)
        ($ :div {:class "ds-alert"} ($ :span "Supplier not found."))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (utils/label-value "Name" (:display-name supplier))
            (utils/label-value "Normalized Key" (:normalized-key supplier))
            (utils/label-value "Address" (:address supplier))
            (utils/label-value "Tax ID" (:tax-id supplier))
            (utils/label-value "Created At" (shared/format-date (:created-at supplier)))
            (utils/label-value "Archived At" (when archived-at (shared/format-date archived-at)))
            (utils/label-value "ID" (:id supplier)))

          ($ :div {:class "flex flex-wrap items-center gap-2"}
            (cond
              archived?
              ($ :span {:class "text-xs text-base-content/60"}
                "Archived")

              :else
              ($ button
                {:id (str "btn-archive-suppliers-" supplier-id-str)
                 :btn-type :warning
                 :loading archiving?
                 :disabled archiving?
                 :on-click (fn []
                             (confirm-dialog/show-confirm
                               {:title "Archive supplier"
                                :message "Archive this supplier? You can still purge it later from the suppliers list actions menu (admin-only)."
                                :danger? true
                                :on-confirm #(rf/dispatch [::suppliers-events/archive-supplier supplier-id-str])}))}
                "Archive supplier")))

          ($ :div {:class "grid gap-4 lg:grid-cols-3"}
            ($ utils/related-table
              {:title "Recent Expenses"
               :rows expenses
               :columns [{:label "Purchased" :value-fn #(shared/format-date (:purchased-at %))}
                         {:label "Total" :value-fn #(utils/format-money (:total-amount %) (:currency %))}
                         {:label "Payer" :value-fn #(:payer-label %)}
                         {:label "Status" :value-fn #(:status %)}]
               :empty-label "No expenses for this supplier yet."
               :view-all-href (when supplier-id
                                (str "/admin/expenses?supplier_id=" supplier-id))
               :view-all-id (when supplier-id
                              (str "btn-view-expenses-supplier-" supplier-id))})
            ($ utils/related-table
              {:title "Article Aliases"
               :rows aliases
               :columns [{:label "Alias" :value-fn #(:raw-label-normalized %)}
                         {:label "Article" :value-fn #(:article-canonical-name %)}
                         {:label "Confidence" :value-fn #(:confidence %)}]
               :empty-label "No article aliases for this supplier."
               :view-all-href (when supplier-id
                                (str "/admin/article-aliases?supplier_id=" supplier-id))
               :view-all-id (when supplier-id
                              (str "btn-view-article-aliases-supplier-" supplier-id))})
            ($ utils/related-table
              {:title "Price Observations"
               :rows observations
               :columns [{:label "Observed" :value-fn #(shared/format-date (:observed-at %))}
                         {:label "Article" :value-fn #(:article-canonical-name %)}
                         {:label "Unit Price" :value-fn #(:unit-price %)}
                         {:label "Currency" :value-fn #(:currency %)}]
               :empty-label "No price observations for this supplier."
               :view-all-href (when supplier-id
                                (str "/admin/price-observations?supplier_id=" supplier-id))
               :view-all-id (when supplier-id
                              (str "btn-view-price-observations-supplier-" supplier-id))})))))))
