(ns app.domain.frontend.expenses.pages.user.receipts-list
  "User-facing receipts inbox (review + approve) using template list-view UX."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.admin.frontend.components.tabs :as tabs]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-viewer]]
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal]]
    [app.template.frontend.components.action-components :refer [view-details-icon]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.dropdown :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
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

(defn- review-required-issues
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
        review-issues (review-required-issues receipt)
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
  (let [receipt (use-subscribe [:user-expenses/receipt receipt-id])
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
        receipt (use-subscribe [:user-expenses/receipt receipt-id])
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

(defn- receipt-ocr-allowed?
  "Check if OCR action should be shown for a receipt.
  Returns true for statuses where OCR makes sense."
  [receipt]
  (let [status (or (:status receipt) (:receipts/status receipt))]
    (contains? #{"uploaded" "failed" "review_required" "extracted" "parsing" "parsed" "extracting"}
      status)))

(defn- receipt-actions
  [receipt]
  (let [receipt-id (id-utils/extract-entity-id receipt)
        ocr-allowed? (receipt-ocr-allowed? receipt)
        action-groups (cond-> [{:group-title "View"
                                :items [{:id "view-details"
                                         :icon ($ view-details-icon)
                                         :label "View Details"
                                         :on-click (fn [e]
                                                     (.stopPropagation e)
                                                     (rf/dispatch [:user-expenses/open-receipt-detail-modal receipt-id]))}]}]
                        ;; Add OCR group when allowed
                        ocr-allowed?
                        (conj {:group-title "OCR"
                               :items [{:id "parse-ocr"
                                        :icon "🔍"
                                        :label "Parse (OCR)"
                                        :tooltip "Run OCR to extract receipt data. Status will update asynchronously."
                                        :on-click (fn [e]
                                                    (.stopPropagation e)
                                                    (rf/dispatch [:user-expenses/ocr-receipt receipt-id]))}]}))]
    ($ dropdown/action-dropdown
      {:entity-id receipt-id
       :actions action-groups
       :position :portal})))

(defui batch-parse-button
  "Batch parse button shown when receipts are selected."
  []
  (let [selected-ids (use-subscribe [::list-subs/selected-ids :receipts])
        action-loading? (boolean (use-subscribe [:user-expenses/receipt-action-loading?]))
        has-selection? (and (seq selected-ids) (pos? (count selected-ids)))]
    (when has-selection?
      ($ button
        {:id "btn-batch-parse-user-receipts"
         :btn-type :primary
         :class "ds-btn-sm"
         :disabled action-loading?
         :on-click (fn [e]
                     (.stopPropagation e)
                     (rf/dispatch [:user-expenses/ocr-selected selected-ids]))}
        (if action-loading?
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
          "🔍 Batch Parse (OCR)")
        (when-not action-loading?
          ($ :span {:class "ds-badge ds-badge-sm ml-1"}
            (count selected-ids)))))))

(defui receipts-list-page []
  (let [title "Receipts"
        error (use-subscribe [:user-expenses/receipts-error])
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
            (when error
              ($ :div {:class "px-4 pt-4"}
                ($ :div {:class "ds-alert ds-alert-error"}
                  ($ :span (str error)))))
            ;; Batch parse button above the list
            ($ :div {:class "flex justify-end px-4 pt-4"}
              ($ batch-parse-button))
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
