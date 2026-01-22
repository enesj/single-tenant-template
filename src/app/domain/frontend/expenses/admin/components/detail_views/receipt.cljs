(ns app.domain.frontend.expenses.admin.components.detail-views.receipt
  "Receipt detail view component with approve/post workflow."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.admin.frontend.components.tabs :as tabs]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.components.expense-form.modals :as expense-form-modals]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-preview receipt-viewer]]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.template.frontend.components.json-highlight :refer [json-display-card]]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- receipt-review-required-issues
  "Return a vector of human-readable reasons why a receipt is in review_required.

  Mirrors backend logic in `receipt-ocr/review-required?`, plus surfaces invalid
  extraction shape from :raw-extract-json when available."
  [receipt]
  (let [status (:status receipt)
        raw-extract (:raw-extract-json receipt)
        valid-shape? (:valid-shape? raw-extract)
        items (get-in raw-extract [:extraction :items])
        items-count (when (sequential? items) (count items))
        missing-supplier? (str/blank? (:supplier-guess receipt))
        missing-total? (nil? (:total-amount-guess receipt))
        missing-currency? (str/blank? (:currency-guess receipt))
        missing-items? (or (nil? items-count) (zero? (long items-count)))]
    (when (= "review_required" status)
      (cond-> []
        (false? valid-shape?) (conj "OCR extraction response did not match the expected format.")
        missing-supplier? (conj "Missing supplier guess.")
        missing-total? (conj "Missing total amount guess.")
        missing-currency? (conj "Missing currency guess.")
        missing-items? (conj "No line items were extracted.")))))

(defui receipt-problem-alert
  [{:keys [receipt]}]
  (let [status (:status receipt)
        review-issues (receipt-review-required-issues receipt)
        error-message (:error-message receipt)
        error-details (:error-details receipt)
        failed? (= "failed" status)
        show? (or (seq review-issues) (some? error-message) (some? error-details))
        details-summary (when (map? error-details)
                          (select-keys error-details [:type :status :message :error :body-snippet]))]
    (when show?
      ($ :div {:class (str "ds-alert " (if failed? "ds-alert-error" "ds-alert-warning"))}
        ($ :div {:class "space-y-1"}
          ($ :div {:class "font-semibold"}
            (if failed?
              "Receipt processing failed"
              "Receipt needs review"))
          (when (some? error-message)
            ($ :div {:class "text-sm"} (str error-message)))
          (when (seq review-issues)
            ($ :ul {:class "list-disc pl-5 text-sm"}
              (for [issue review-issues]
                ($ :li {:key issue} issue))))
          (when (some? error-details)
            ($ :pre {:class "text-xs opacity-80 whitespace-pre-wrap break-words"}
              (pr-str (or (not-empty details-summary) error-details)))))))))

(defui receipt-detail-body
  [{:keys [receipt-id]}]
  (let [receipt (use-subscribe [:expenses/receipt receipt-id])
        loading? (use-subscribe [:expenses/receipt-detail-loading?])
        action-loading? (use-subscribe [:expenses/receipt-action-loading?])
        error (use-subscribe [:expenses/receipts-error])
        [active-tab set-active-tab!] (use-state :details)
        [preview-expanded? set-preview-expanded!] (use-state true)
        [next-status set-next-status!] (use-state nil)]
    (use-effect
      (fn []
        (set-active-tab! :details)
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
        (let [rid (or receipt-id (id-utils/extract-entity-id receipt))
              rid-str (if rid (str rid) "unknown")
              status (:status receipt)
              approve-allowed? (contains? #{"extracted" "review_required"} status)]
          ($ :div {:class "space-y-4"}
            ($ receipt-problem-alert {:receipt receipt})

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
                    ($ :div {:class (str "grid gap-6 " (when preview-expanded? "lg:grid-cols-2"))}
                      ($ :div {:class "space-y-4"}
                        ($ receipt-preview {:receipt receipt
                                            :title "Receipt image"
                                            :expanded? preview-expanded?
                                            :on-toggle #(set-preview-expanded! (not preview-expanded?))}))
                      ($ :div {:class "space-y-4"}
                        ($ :h2 {:class "text-lg font-semibold"}
                          "Approve & Post")
                        ($ expense-form-modals/expense-add-form-modal
                          {:receipt-id receipt-id
                           :on-success (fn []
                                         (set-active-tab! :details)
                                         (rf/dispatch [::receipts-events/load-detail receipt-id]))
                           :on-review-saved (fn []
                                              (rf/dispatch [::receipts-events/load-detail receipt-id]))
                           :on-cancel #(set-active-tab! :details)})))))

                ($ :div {:class "ds-alert ds-alert-info"}
                  ($ :span
                    (str "Approval is available when status is extracted or review_required. Current status: "
                      (or status "unknown")
                      "."))))

              ;; default: :details
              ($ :div {:class "space-y-6"}
                ;; Core Info
                ($ :div {:class "grid grid-cols-1 md:grid-cols-3 gap-4"}
                  (utils/label-value "Status" ($ :span {:class (utils/status-class status)}
                                                (utils/capitalize-words status)))
                  (utils/label-value "Original Filename" (:original-filename receipt))
                  (utils/label-value "Content Type" (:content-type receipt))
                  (utils/label-value "File Size" (utils/format-bytes (:file-size receipt)))
                  (utils/label-value "Storage Key" (:storage-key receipt))
                  (utils/label-value "Supplier Guess" (:supplier-guess receipt))
                  (utils/label-value "Total Amount Guess" (when (:total-amount-guess receipt)
                                                            (str (:total-amount-guess receipt) " " (:currency-guess receipt))))
                  (utils/label-value "Purchased At Guess" (:purchased-at-guess receipt))
                  (utils/label-value "Retry Count" (:retry-count receipt))
                  (utils/label-value "Expense ID" (:expense-id receipt))
                  (utils/label-value "Created At" (shared/format-date (:created-at receipt)))
                  (utils/label-value "Updated At" (shared/format-date (:updated-at receipt))))

                ;; Actions
                ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
                  ($ :div {:class "ds-card-body space-y-3"}
                    ($ :h2 {:class "text-lg font-semibold"} "Actions")
                    ($ :div {:class "flex flex-wrap items-center gap-2"}
                      (when approve-allowed?
                        ($ :button {:id (str "btn-approve-receipt-" rid-str)
                                    :class "ds-btn ds-btn-success ds-btn-sm"
                                    :disabled action-loading?
                                    :on-click #(set-active-tab! :approve)}
                          "Approve & Post"))

                      ($ :button {:id (str "btn-retry-receipt-" rid-str)
                                  :class "ds-btn ds-btn-outline ds-btn-sm"
                                  :disabled action-loading?
                                  :on-click #(rf/dispatch [::receipts-events/retry-extraction receipt-id])}
                        (if action-loading? "Working..." "Retry OCR"))

                      ($ :div {:class "flex items-center gap-2"}
                        ($ :select {:id (str "select-receipt-status-" rid-str)
                                    :class "ds-select ds-select-bordered ds-select-sm"
                                    :value (or next-status "")
                                    :on-change (fn [e]
                                                 (set-next-status! (.. e -target -value)))}
                          (for [s utils/receipt-status-options]
                            ($ :option {:key s :value s} s)))
                        ($ :button {:id (str "btn-set-receipt-status-" rid-str)
                                    :class "ds-btn ds-btn-primary ds-btn-sm"
                                    :disabled (or action-loading?
                                                (not (seq next-status))
                                                (= next-status status))
                                    :on-click #(rf/dispatch [::receipts-events/update-status receipt-id next-status])}
                          "Set status")))))

                ($ :div {:class "grid gap-6 lg:grid-cols-2"}
                  (when (seq (:raw-extract-json receipt))
                    ($ json-display-card
                      {:id (str "receipt-extract-json-" rid-str)
                       :title "Extracted Data"
                       :json (:raw-extract-json receipt)}))
                  (when (seq (:raw-parse-json receipt))
                    ($ json-display-card
                      {:id (str "receipt-parse-json-" rid-str)
                       :title "LlamaParse Results"
                       :json (:raw-parse-json receipt)})))))))))))
