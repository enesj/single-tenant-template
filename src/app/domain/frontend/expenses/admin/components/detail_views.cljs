(ns app.domain.frontend.expenses.admin.components.detail-views
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-viewer]]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- label-value
  [label value]
  ($ :div {:class "flex flex-col gap-1 p-3 bg-base-200 rounded-lg"}
    ($ :span {:class "text-xs uppercase tracking-wide text-base-content/70"} label)
    ($ :span {:class "text-sm font-medium"}
      (shared/format-value value "—" false))))

(defn- column-value
  [row {:keys [key value-fn]}]
  (cond
    (fn? value-fn) (value-fn row)
    key (get row key)
    :else nil))

(defn- related-table
  [{:keys [title rows columns empty-label view-all-href view-all-id]}]
  (let [rows* (take 5 (or rows []))]
    ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
      ($ :div {:class "ds-card-body space-y-3"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :h2 {:class "text-lg font-semibold"} title)
          (when view-all-href
            ($ :a {:id view-all-id
                   :href view-all-href
                   :class "ds-btn ds-btn-ghost ds-btn-xs"}
              "View all")))
        (if (seq rows*)
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table w-full"}
              ($ :thead
                ($ :tr
                  (for [{:keys [label]} columns]
                    ($ :th {:key label} label))))
              ($ :tbody
                (for [row rows*
                      :let [row-id (or (id-utils/extract-entity-id row) (hash row))]]
                  ($ :tr {:key (str row-id)}
                    (for [col columns
                          :let [cell-value (column-value row col)]]
                      ($ :td {:key (str row-id "-" (:label col))}
                        (shared/format-value cell-value "—" false))))))))
          ($ :div {:class "text-sm text-base-content/70"}
            (or empty-label "No related records found.")))))))

(defn- format-money
  [amount currency]
  (when (some? amount)
    (str amount (when currency (str " " currency)))))

(def ^:private receipt-status-options
  ["uploaded"
   "parsing"
   "parsed"
   "extracting"
   "extracted"
   "review_required"
   "approved"
   "posted"
   "failed"])

(defui supplier-detail-body
  [{:keys [supplier-id]}]
  (let [supplier (use-subscribe [:expenses/supplier supplier-id])
        loading? (use-subscribe [:expenses/supplier-detail-loading?])
        error (use-subscribe [:expenses/suppliers-error])
        expenses (use-subscribe [:expenses/entries])
        aliases (use-subscribe [:expenses/article-aliases])
        observations (use-subscribe [:expenses/price-observations])]
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
            (label-value "Name" (:display-name supplier))
            (label-value "Normalized Key" (:normalized-key supplier))
            (label-value "Address" (:address supplier))
            (label-value "Tax ID" (:tax-id supplier))
            (label-value "Created At" (shared/format-date (:created-at supplier)))
            (label-value "ID" (:id supplier)))

          ($ :div {:class "grid gap-4 lg:grid-cols-3"}
            (related-table
              {:title "Recent Expenses"
               :rows expenses
               :columns [{:label "Purchased" :value-fn #(shared/format-date (:purchased-at %))}
                         {:label "Total" :value-fn #(format-money (:total-amount %) (:currency %))}
                         {:label "Payer" :value-fn #(:payer-label %)}
                         {:label "Status" :value-fn #(:status %)}]
               :empty-label "No expenses for this supplier yet."
               :view-all-href (when supplier-id
                                (str "/admin/expenses?supplier_id=" supplier-id))
               :view-all-id (when supplier-id
                              (str "btn-view-expenses-supplier-" supplier-id))})
            (related-table
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
            (related-table
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

(defui article-detail-body
  [{:keys [article-id]}]
  (let [article (use-subscribe [:expenses/article article-id])
        loading? (use-subscribe [:expenses/article-detail-loading?])
        error (use-subscribe [:expenses/articles-error])
        aliases (use-subscribe [:expenses/article-aliases])
        observations (use-subscribe [:expenses/price-observations])]
    (use-effect
      (fn []
        (when article-id
          (rf/dispatch [::articles-events/load-detail article-id])
          (rf/dispatch [::aliases-events/load-list {:article_id article-id :limit 10 :offset 0}])
          (rf/dispatch [::price-obs-events/load-list {:article_id article-id :limit 10 :offset 0}]))
        js/undefined)
      [article-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? article)
        ($ :div {:class "ds-alert"} ($ :span "Article not found."))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (label-value "Name" (:canonical-name article))
            (label-value "Barcode" (:barcode article))
            (label-value "Category" (:category article))
            (label-value "Normalized Key" (:normalized-key article))
            (label-value "Created At" (shared/format-date (:created-at article)))
            (label-value "ID" (:id article)))

          ($ :div {:class "grid gap-4 lg:grid-cols-2"}
            (related-table
              {:title "Article Aliases"
               :rows aliases
               :columns [{:label "Alias" :value-fn #(:raw-label-normalized %)}
                         {:label "Supplier" :value-fn #(:supplier-display-name %)}
                         {:label "Confidence" :value-fn #(:confidence %)}]
               :empty-label "No aliases mapped to this article."
               :view-all-href (when article-id
                                (str "/admin/article-aliases?article_id=" article-id))
               :view-all-id (when article-id
                              (str "btn-view-article-aliases-article-" article-id))})
            (related-table
              {:title "Price Observations"
               :rows observations
               :columns [{:label "Observed" :value-fn #(shared/format-date (:observed-at %))}
                         {:label "Supplier" :value-fn #(:supplier-display-name %)}
                         {:label "Unit Price" :value-fn #(:unit-price %)}
                         {:label "Currency" :value-fn #(:currency %)}]
               :empty-label "No price observations for this article."
               :view-all-href (when article-id
                                (str "/admin/price-observations?article_id=" article-id))
               :view-all-id (when article-id
                              (str "btn-view-price-observations-article-" article-id))})))))))

(defui payer-detail-body
  [{:keys [payer-id]}]
  (let [payer (use-subscribe [:expenses/payer payer-id])
        loading? (use-subscribe [:expenses/payer-detail-loading?])
        error (use-subscribe [:expenses/payers-error])
        expenses (use-subscribe [:expenses/entries])]
    (use-effect
      (fn []
        (when payer-id
          (rf/dispatch [::payers-events/load-detail payer-id])
          (rf/dispatch [::expenses-events/load-list {:payer_id payer-id :limit 10 :offset 0}]))
        js/undefined)
      [payer-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? payer)
        ($ :div {:class "ds-alert"} ($ :span "Payer not found."))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (label-value "Label" (:label payer))
            (label-value "Type" (:type payer))
            (label-value "Default" (if (true? (:is-default payer)) "Yes" "No"))
            (label-value "Created At" (shared/format-date (:created-at payer)))
            (label-value "ID" (:id payer)))

          (related-table
            {:title "Recent Expenses"
             :rows expenses
             :columns [{:label "Supplier" :value-fn #(:supplier-display-name %)}
                       {:label "Purchased" :value-fn #(shared/format-date (:purchased-at %))}
                       {:label "Total" :value-fn #(format-money (:total-amount %) (:currency %))}
                       {:label "Status" :value-fn #(:status %)}]
             :empty-label "No expenses for this payer yet."
             :view-all-href (when payer-id
                              (str "/admin/expenses?payer_id=" payer-id))
             :view-all-id (when payer-id
                            (str "btn-view-expenses-payer-" payer-id))}))))))

(defui article-alias-detail-body
  [{:keys [alias-id]}]
  (let [alias (use-subscribe [:expenses/article-alias alias-id])
        loading? (use-subscribe [:expenses/article-alias-detail-loading?])
        error (use-subscribe [:expenses/article-aliases-error])]
    (use-effect
      (fn []
        (when alias-id
          (rf/dispatch [::aliases-events/load-detail alias-id]))
        js/undefined)
      [alias-id])

    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? alias)
        ($ :div {:class "ds-alert"} ($ :span "Alias not found."))

        :else
        ($ :div {:class "grid gap-3 md:grid-cols-3"}
          (label-value "Supplier" (:supplier-display-name alias))
          (label-value "Article" (:article-canonical-name alias))
          (label-value "Raw Label" (:raw-label-normalized alias))
          (label-value "Confidence" (:confidence alias))
          (label-value "Supplier ID" (:supplier-id alias))
          (label-value "Article ID" (:article-id alias))
          (label-value "Created At" (shared/format-date (:created-at alias)))
          (label-value "ID" (:id alias)))))))

(defui price-observation-detail-body
  [{:keys [observation-id]}]
  (let [obs (use-subscribe [:expenses/price-observation observation-id])
        loading? (use-subscribe [:expenses/price-observation-detail-loading?])
        error (use-subscribe [:expenses/price-observations-error])]
    (use-effect
      (fn []
        (when observation-id
          (rf/dispatch [::price-obs-events/load-detail observation-id]))
        js/undefined)
      [observation-id])

    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? obs)
        ($ :div {:class "ds-alert"} ($ :span "Observation not found."))

        :else
        ($ :div {:class "grid gap-3 md:grid-cols-3"}
          (label-value "Article" (:article-canonical-name obs))
          (label-value "Supplier" (:supplier-display-name obs))
          (label-value "Observed At" (shared/format-date (:observed-at obs)))
          (label-value "Qty" (:qty obs))
          (label-value "Unit Price" (:unit-price obs))
          (label-value "Line Total" (:line-total obs))
          (label-value "Currency" (:currency obs))
          (label-value "Expense Item ID" (:expense-item-id obs))
          (label-value "Article ID" (:article-id obs))
          (label-value "Supplier ID" (:supplier-id obs))
          (label-value "Created At" (shared/format-date (:created-at obs)))
          (label-value "ID" (:id obs)))))))

(defui receipt-detail-body
  [{:keys [receipt-id]}]
  (let [receipt (use-subscribe [:expenses/receipt receipt-id])
        loading? (use-subscribe [:expenses/receipt-detail-loading?])
        action-loading? (use-subscribe [:expenses/receipt-action-loading?])
        error (use-subscribe [:expenses/receipts-error])
        [next-status set-next-status!] (use-state nil)]
    (use-effect
      (fn []
        (when receipt-id
          (rf/dispatch [::receipts-events/load-detail receipt-id]))
        js/undefined)
      [receipt-id])

    (use-effect
      (fn []
        (when-let [status (:status receipt)]
          (set-next-status! status))
        js/undefined)
      [receipt (:status receipt)])

    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (not receipt)
        ($ :div {:class "text-center p-12 text-base-content/70"}
          "Receipt not found.")

        :else
        ($ :div {:class "space-y-6"}
          ;; Core Info
          ($ :div {:class "grid grid-cols-1 md:grid-cols-3 gap-4"}
            (label-value "Original Filename" (:original-filename receipt))
            (label-value "Status" (:status receipt))
            (label-value "Content Type" (:content-type receipt))
            (label-value "File Size" (str (:file-size receipt) " bytes"))
            (label-value "Created At" (:created-at receipt))
            (label-value "Updated At" (:updated-at receipt)))

          ;; Receipt Viewer
          ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
            ($ :div {:class "ds-card-body p-0"}
              ($ receipt-viewer {:receipt-id receipt-id}))))))))

(defui expense-item-detail-body
  [{:keys [expense-item-id]}]
  (let [expense-item (use-subscribe [:expenses/expense-item expense-item-id])
        loading? (use-subscribe [:expenses/expense-item-detail-loading?])
        error (use-subscribe [:expenses/expense-items-error])]
    (use-effect
      (fn []
        (when expense-item-id
          (rf/dispatch [:app.domain.frontend.expenses.events.expense-items/load-detail expense-item-id]))
        js/undefined)
      [expense-item-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "flex justify-center p-12"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))

        (not expense-item)
        ($ :div {:class "text-center p-12 text-base-content/70"}
          "Expense item not found.")

        :else
        ($ :div {:class "space-y-6"}
          ;; Core Info
          ($ :div {:class "grid grid-cols-1 md:grid-cols-3 gap-4"}
            (label-value "Raw Label" (:raw-label expense-item))
            (label-value "Normalized Label" (:raw-label-normalized expense-item))
            (label-value "Quantity" (:qty expense-item))
            (label-value "Unit Price" (:unit-price expense-item))
            (label-value "Line Total" (:line-total expense-item))
            (label-value "Expense ID" (:expense-id expense-item))
            (label-value "Article ID" (:article-id expense-item))
            (label-value "Created At" (:created-at expense-item))
            (label-value "Updated At" (:updated-at expense-item))))))))
