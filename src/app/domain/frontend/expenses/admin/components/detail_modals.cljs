(ns app.domain.frontend.expenses.admin.components.detail-modals
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.components.expense-form :as expense-form]
    [app.domain.frontend.expenses.components.receipt-detail-modal :as receipt-detail-ui]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.template.frontend.components.modal :refer [modal]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- detail-header
  [{:keys [title subtitle icon]}]
  ($ shared/detail-modal-header
    {:title title
     :subtitle subtitle
     :icon icon
     :icon-bg "bg-primary/10"}))

(defn- render-modal
  [{:keys [id open? loading? header on-close body]}]
  (when (or open? loading?)
    ($ modal {:id id
              :on-close on-close
              :draggable? true
              :width "960px"
              :class "max-w-[95vw] h-[85vh] flex flex-col"
              :header header
              :header-class "p-0 border-0 bg-transparent mb-3"}
      ($ :div {:class "flex-1 overflow-y-auto p-4"}
        body))))

(defui admin-supplier-detail-modal []
  (let [open? (use-subscribe [:expenses/supplier-detail-modal-open?])
        supplier-id (use-subscribe [:expenses/supplier-detail-modal-id])
        supplier (use-subscribe [:expenses/supplier supplier-id])
        loading? (use-subscribe [:expenses/supplier-detail-loading?])
        subtitle (or (:display-name supplier)
                   (when supplier-id (str "Supplier " supplier-id))
                   "Supplier details")
        header (detail-header {:title "Supplier Details"
                               :subtitle subtitle
                               :icon "S"})]
    (render-modal
      {:id "admin-supplier-detail-modal"
       :open? open?
       :loading? loading?
       :header header
       :on-close #(rf/dispatch [::suppliers-events/close-detail-modal])
       :body ($ detail-views/supplier-detail-body {:supplier-id supplier-id})})))

(defui admin-article-detail-modal []
  (let [open? (use-subscribe [:expenses/article-detail-modal-open?])
        article-id (use-subscribe [:expenses/article-detail-modal-id])
        article (use-subscribe [:expenses/article article-id])
        loading? (use-subscribe [:expenses/article-detail-loading?])
        subtitle (or (:canonical-name article)
                   (when article-id (str "Article " article-id))
                   "Article details")
        header (detail-header {:title "Article Details"
                               :subtitle subtitle
                               :icon "A"})]
    (render-modal
      {:id "admin-article-detail-modal"
       :open? open?
       :loading? loading?
       :header header
       :on-close #(rf/dispatch [::articles-events/close-detail-modal])
       :body ($ detail-views/article-detail-body {:article-id article-id})})))

(defui admin-payer-detail-modal []
  (let [open? (use-subscribe [:expenses/payer-detail-modal-open?])
        payer-id (use-subscribe [:expenses/payer-detail-modal-id])
        payer (use-subscribe [:expenses/payer payer-id])
        loading? (use-subscribe [:expenses/payer-detail-loading?])
        subtitle (or (:label payer)
                   (when payer-id (str "Payer " payer-id))
                   "Payer details")
        header (detail-header {:title "Payer Details"
                               :subtitle subtitle
                               :icon "P"})]
    (render-modal
      {:id "admin-payer-detail-modal"
       :open? open?
       :loading? loading?
       :header header
       :on-close #(rf/dispatch [::payers-events/close-detail-modal])
       :body ($ detail-views/payer-detail-body {:payer-id payer-id})})))

(defui admin-receipt-detail-modal []
  (let [ctx {:receipt-sub :expenses/receipt
             :receipt-detail-loading-sub :expenses/receipt-detail-loading?
             :receipt-action-loading-sub :expenses/receipt-action-loading?
             :receipts-error-sub :expenses/receipts-error
             :modal-open-sub :expenses/receipt-detail-modal-open?
             :modal-id-sub :expenses/receipt-detail-modal-id
             :fetch-receipt-event ::receipts-events/load-detail
             :close-modal [::receipts-events/close-detail-modal]
             :approve-form expense-form/expense-add-form-modal}]
    ($ receipt-detail-ui/receipt-detail-modal
      {:id "admin-receipt-detail-modal"
       :ctx ctx})))

(defui admin-expense-item-detail-modal []
  (let [open? (use-subscribe [:expenses/expense-item-detail-modal-open?])
        expense-item-id (use-subscribe [:expenses/expense-item-detail-modal-id])
        expense-item (use-subscribe [:expenses/expense-item expense-item-id])
        loading? (use-subscribe [:expenses/expense-item-detail-loading?])
        subtitle (or (:raw-label expense-item)
                   (when expense-item-id (str "Expense Item " expense-item-id))
                   "Expense item details")
        header (detail-header {:title "Expense Item Details"
                               :subtitle subtitle
                               :icon "EI"})]
    (render-modal
      {:id "admin-expense-item-detail-modal"
       :open? open?
       :loading? loading?
       :header header
       :on-close #(rf/dispatch [:app.domain.frontend.expenses.events.expense-items/close-detail-modal])
       :body ($ detail-views/expense-item-detail-body {:expense-item-id expense-item-id})})))

(defui admin-article-alias-detail-modal []
  (let [open? (use-subscribe [:expenses/article-alias-detail-modal-open?])
        alias-id (use-subscribe [:expenses/article-alias-detail-modal-id])
        alias (use-subscribe [:expenses/article-alias alias-id])
        loading? (use-subscribe [:expenses/article-alias-detail-loading?])
        subtitle (or (:raw-label-normalized alias)
                   (when alias-id (str "Alias " alias-id))
                   "Article alias details")
        header (detail-header {:title "Article Alias Details"
                               :subtitle subtitle
                               :icon "AA"})]
    (render-modal
      {:id "admin-article-alias-detail-modal"
       :open? open?
       :loading? loading?
       :header header
       :on-close #(rf/dispatch [::aliases-events/close-detail-modal])
       :body ($ detail-views/article-alias-detail-body {:alias-id alias-id})})))

(defui admin-price-observation-detail-modal []
  (let [open? (use-subscribe [:expenses/price-observation-detail-modal-open?])
        obs-id (use-subscribe [:expenses/price-observation-detail-modal-id])
        obs (use-subscribe [:expenses/price-observation obs-id])
        loading? (use-subscribe [:expenses/price-observation-detail-loading?])
        subtitle (or (:article-canonical-name obs)
                   (when obs-id (str "Observation " obs-id))
                   "Price observation details")
        header (detail-header {:title "Price Observation Details"
                               :subtitle subtitle
                               :icon "PO"})]
    (render-modal
      {:id "admin-price-observation-detail-modal"
       :open? open?
       :loading? loading?
       :header header
       :on-close #(rf/dispatch [::price-obs-events/close-detail-modal])
       :body ($ detail-views/price-observation-detail-body {:observation-id obs-id})})))
