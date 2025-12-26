(ns app.domain.frontend.expenses.pages.user.receipts-list
  "User-facing receipts inbox (review + approve) using template list-view UX."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.admin.frontend.components.tabs :as tabs]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-viewer]]
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal]]
    [app.template.frontend.components.action-components :refer [view-details-icon]]
    [app.template.frontend.components.dropdown :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(def ^:private receipts-entity-spec
  {:id :receipts
   :fields [{:id :original-filename :label "original filename" :type :text}
            {:id :status :label "status" :type :text}
            {:id :supplier-guess :label "supplier guess" :type :text}]})

(defn- detail-header
  [{:keys [title subtitle icon]}]
  ($ shared/detail-modal-header
    {:title title
     :subtitle subtitle
     :icon icon
     :icon-bg "bg-primary/10"}))

(defn- label-value
  [label value]
  ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
    ($ :div {:class "ds-card-body p-4"}
      ($ :div {:class "text-xs text-base-content/60"} label)
      ($ :div {:class "text-sm font-medium break-words"} (or (some-> value str) "—")))))

(defui receipt-detail-body
  [{:keys [receipt-id]}]
  (let [receipt (use-subscribe (when receipt-id [:user-expenses/receipt receipt-id]))
        loading? (boolean (use-subscribe [:user-expenses/receipt-detail-loading?]))
        action-loading? (boolean (use-subscribe [:user-expenses/receipt-action-loading?]))
        error (use-subscribe [:user-expenses/receipts-error])
        [active-tab set-active-tab!] (use-state :details)]

    (use-effect
      (fn []
        (set-active-tab! :details)
        (when receipt-id
          (rf/dispatch [:user-expenses/fetch-receipt receipt-id]))
        js/undefined)
      [receipt-id])

    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "flex justify-center p-12"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))

        (not receipt)
        ($ :div {:class "text-center p-12 text-base-content/70"}
          "Receipt not found.")

        :else
        (let [rid (or receipt-id (id-utils/extract-entity-id receipt))
              rid-str (if rid (str rid) "unknown")
              status (:status receipt)
              approve-allowed? (contains? #{"extracted" "review_required"} status)]
          ($ :div {:class "space-y-4"}
            ($ :div {:class "ds-tabs ds-tabs-boxed"}
              (tabs/tab-link {:id (str "tab-receipt-details-" rid-str)
                              :label "Details"
                              :active? (= active-tab :details)
                              :on-select #(set-active-tab! :details)})
              (tabs/tab-link {:id (str "tab-receipt-viewer-" rid-str)
                              :label "Receipt"
                              :active? (= active-tab :receipt)
                              :on-select #(set-active-tab! :receipt)})
              (tabs/tab-link {:id (str "tab-receipt-approve-" rid-str)
                              :label "Approve & Post"
                              :active? (= active-tab :approve)
                              :on-select #(set-active-tab! :approve)}))

            (case active-tab
              :receipt
              ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
                ($ :div {:class "ds-card-body p-0"}
                  ($ receipt-viewer {:receipt receipt
                                     :show-summary? false})))

              :approve
              (if approve-allowed?
                ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
                  ($ :div {:class "ds-card-body"}
                    ($ :div {:class "flex items-center justify-between gap-2"}
                      ($ :h2 {:class "text-lg font-semibold"} "Approve Receipt & Create Expense")
                      (when action-loading?
                        ($ :span {:class "text-xs text-base-content/60"} "Working...")))
                    ($ user-expense-add-form-modal
                      {:receipt-id rid-str
                       :receipt receipt
                       :on-success (fn []
                                     (set-active-tab! :details)
                                     (rf/dispatch [:user-expenses/fetch-receipt receipt-id]))
                       :on-cancel #(set-active-tab! :details)})))

                ($ :div {:class "ds-alert ds-alert-info"}
                  ($ :span
                    (str "Approval is available when status is extracted or review_required. Current status: "
                      (or status "unknown")
                      "."))))

              ;; default: :details
              ($ :div {:class "space-y-6"}
                ($ :div {:class "grid grid-cols-1 md:grid-cols-3 gap-4"}
                  (label-value "Original Filename" (:original-filename receipt))
                  (label-value "Status" status)
                  (label-value "Supplier Guess" (:supplier-guess receipt))
                  (label-value "Total Guess" (:total-amount-guess receipt))
                  (label-value "Currency" (:currency-guess receipt))
                  (label-value "Purchased At" (:purchased-at-guess receipt))
                  (label-value "Expense ID" (:expense-id receipt))
                  (label-value "Created At" (:created-at receipt))
                  (label-value "Updated At" (:updated-at receipt)))))))))))

(defui receipt-detail-modal
  []
  (let [open? (use-subscribe [:user-expenses/receipt-detail-modal-open?])
        receipt-id (use-subscribe [:user-expenses/receipt-detail-modal-id])
        receipt (use-subscribe (when receipt-id [:user-expenses/receipt receipt-id]))
        loading? (boolean (use-subscribe [:user-expenses/receipt-detail-loading?]))
        subtitle (or (:original-filename receipt)
                   (when receipt-id (str "Receipt " receipt-id))
                   "Receipt details")
        header (detail-header {:title "Receipt Details"
                               :subtitle subtitle
                               :icon "R"})]
    (when (or open? loading?)
      ($ modal {:id "user-receipt-detail-modal"
                :on-close #(rf/dispatch [:user-expenses/close-receipt-detail-modal])
                :draggable? true
                :width "960px"
                :class "max-w-[95vw] h-[85vh] flex flex-col"
                :header header
                :header-class "p-0 border-0 bg-transparent mb-3"}
        ($ :div {:class "flex-1 overflow-y-auto p-4"}
          ($ receipt-detail-body {:receipt-id receipt-id}))))))

(defn- receipt-actions
  [receipt]
  (let [receipt-id (id-utils/extract-entity-id receipt)]
    ($ dropdown/action-dropdown
      {:entity-id receipt-id
       :actions [{:group-title "View"
                  :items [{:id "view-details"
                           :icon ($ view-details-icon)
                           :label "View Details"
                           :on-click (fn [e]
                                       (.stopPropagation e)
                                       (rf/dispatch [:user-expenses/open-receipt-detail-modal receipt-id]))}]}]
       :position :portal})))

(defui receipts-list-page []
  (let [title "Receipts"
        display-settings {:show-select? true
                          :show-edit? false
                          :show-delete? false
                          :show-filtering? true
                          :show-pagination? true
                          :show-timestamps? true
                          :show-highlights? true
                          :show-add-button? true
                          :show-batch-edit? false
                          :show-batch-delete? false
                          :per-page 25}]
    ($ :<>
      ($ :div {:class "p-6 min-h-screen bg-base-100"}
        ($ :div {:class "ds-card ds-bg-base-100 ds-shadow-xl"}
          ($ :div {:class "ds-card-body p-0"}
            ($ :div {:class "w-full pb-0 [&>div>table]:w-full"}
              ($ list-view
                {:entity-name :receipts
                 :entity-spec receipts-entity-spec
                 :title title
                 :display-settings display-settings
                 :per-page 25
                 :custom-actions receipt-actions
                 :on-add-click #(rf/dispatch [:navigate-to "/expenses/upload"])})))))
      ($ receipt-detail-modal))))
