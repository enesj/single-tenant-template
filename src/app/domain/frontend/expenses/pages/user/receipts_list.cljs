(ns app.domain.frontend.expenses.pages.user.receipts-list
  "User-facing receipts inbox (review + approve) using template list-view UX."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.admin.frontend.components.tabs :as tabs]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-preview receipt-viewer]]
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal]]
    [app.template.frontend.components.action-components :refer [view-details-icon]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.dropdown :as dropdown]
    [app.template.frontend.components.json-highlight :refer [json-display-card]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(def ^:private receipts-entity-spec
  {:id :receipts
   :fields [{:id :original-filename :label "original filename" :type :text}
            {:id :status :label "status" :type :text}
            {:id :supplier-guess :label "supplier guess" :type :text}
            ;; Single column: show total; include line total only when it differs.
            {:id :total-display :label "total" :type :text}]})

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
      ($ :div {:class "text-sm font-medium break-words"}
        (shared/format-value value "—" false)))))

(defn- format-money
  [amount currency]
  (when (some? amount)
    (str amount (when currency (str " " currency)))))

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

(def ^:private receipt-processing-statuses
  #{"uploaded" "parsing" "parsed" "extracting"})

(defn- receipt-processing?
  [status]
  (contains? receipt-processing-statuses status))

(defn- format-time
  [d]
  (when d
    (try
      (.toLocaleTimeString d "en-US" #js {:hour "2-digit" :minute "2-digit" :second "2-digit"})
      (catch :default _
        (str d)))))

(defn- format-bytes
  [value]
  (let [bytes (cond
                (number? value) value
                (string? value) (js/parseFloat value)
                :else nil)
        kb 1024
        mb (* 1024 1024)]
    (cond
      (nil? bytes) "—"
      (< bytes kb) (str bytes " B")
      (< bytes mb) (str (.toFixed (/ bytes kb) 1) " KB")
      :else (str (.toFixed (/ bytes mb) 1) " MB"))))

(defn- status-class
  [status]
  (case status
    "uploaded" "ds-badge ds-badge-ghost"
    "parsing" "ds-badge ds-badge-info"
    "parsed" "ds-badge ds-badge-info"
    "extracting" "ds-badge ds-badge-warning"
    "extracted" "ds-badge ds-badge-success"
    "review_required" "ds-badge ds-badge-warning"
    "approved" "ds-badge ds-badge-success"
    "posted" "ds-badge ds-badge-success"
    "failed" "ds-badge ds-badge-error"
    "ds-badge"))

(defn- capitalize-words
  [s]
  (when (string? s)
    (->> (str/split (str/replace s #"_" " ") #"\\s+")
      (map str/capitalize)
      (str/join " "))))

(defui receipt-detail-body
  [{:keys [receipt-id]}]
  (let [receipt (use-subscribe [:user-expenses/receipt receipt-id])
        loading? (boolean (use-subscribe [:user-expenses/receipt-detail-loading?]))
        action-loading? (boolean (use-subscribe [:user-expenses/receipt-action-loading?]))
        error (use-subscribe [:user-expenses/receipts-error])
        [active-tab set-active-tab!] (use-state :details)
        [preview-expanded? set-preview-expanded!] (use-state true)
        [last-checked set-last-checked!] (use-state nil)
        refresh! (use-callback
                   (fn []
                     (when receipt-id
                       (rf/dispatch [:user-expenses/fetch-receipt receipt-id])
                       (set-last-checked! (js/Date.))))
                   [receipt-id])
        status (when (map? receipt) (:status receipt))
        processing? (and (string? status) (receipt-processing? status))
        rid (or receipt-id
              (when (map? receipt)
                (id-utils/extract-entity-id receipt)))
        rid-str (if rid (str rid) "unknown")]

    ;; Reset tab + fetch on open/change
    (use-effect
      (fn []
        (set-active-tab! :details)
        (refresh!)
        js/undefined)
      [receipt-id refresh!])

    ;; Auto-poll receipt detail while OCR is processing
    (use-effect
      (fn []
        (when (and receipt-id processing?)
          (let [handle (js/setInterval refresh! 2500)]
            (fn []
              (js/clearInterval handle)))))
      [receipt-id processing? refresh!])

    ($ :div {:class "space-y-4"}
      (when processing?
        ($ :div {:id (str "receipt-detail-processing-banner-" rid-str)
                 :class "ds-alert ds-alert-info"}
          ($ :div {:class "flex items-center justify-between gap-2 w-full"}
            ($ :div {:class "flex items-center gap-2"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
              ($ :span {:class "text-sm"} "Processing…")
              (when last-checked
                ($ :span {:class "text-xs text-base-content/60"}
                  (str "Last checked: " (format-time last-checked)))))
            ($ :button {:id (str "btn-refresh-receipt-detail-" rid-str)
                        :class "ds-btn ds-btn-ghost ds-btn-xs"
                        :type "button"
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (refresh!))}
              "Refresh"))))

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
        (let [approve-allowed? (contains? #{"extracted" "review_required"} status)]
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
                                            :on-toggle #(set-preview-expanded! not)}))
                      ($ :div {:class "space-y-4"}
                        ($ :div {:class "flex items-center justify-between gap-2"}
                          ($ :h2 {:class "text-lg font-semibold"} "Approve & Post")
                          (when action-loading?
                            ($ :span {:class "text-xs text-base-content/60"} "Working...")))
                        ($ user-expense-add-form-modal
                          {:receipt-id rid-str
                           :receipt receipt
                           :on-success (fn []
                                         (set-active-tab! :details)
                                         (rf/dispatch [:user-expenses/fetch-receipt receipt-id]))
                           :on-review-saved (fn []
                                              (rf/dispatch [:user-expenses/fetch-receipt receipt-id]))
                           :on-cancel #(set-active-tab! :details)})))))

                ($ :div {:class "ds-alert ds-alert-info"}
                  ($ :span
                    (str "Approval is available when status is extracted or review_required. Current status: "
                      (or status "unknown")
                      "."))))

              ;; default: :details
              ($ :div {:class "space-y-6"}
                ($ :div {:class "grid gap-3 md:grid-cols-3"}
                  (label-value "Status" ($ :span {:class (status-class status)}
                                          (or (capitalize-words status) "—")))
                  (label-value "Original Filename" (:original-filename receipt))
                  (label-value "Content Type" (:content-type receipt))
                  (label-value "File Size" (format-bytes (:file-size receipt)))
                  (label-value "Created At" (shared/format-date (:created-at receipt)))
                  (label-value "Supplier Guess" (:supplier-guess receipt))
                  (label-value "Total Amount Guess" (when (:total-amount-guess receipt)
                                                      (str (:total-amount-guess receipt) " " (:currency-guess receipt))))
                  (label-value "Purchased At Guess" (:purchased-at-guess receipt)))

                ($ :div {:class "grid gap-6 lg:grid-cols-2"}
                  (when (seq (:raw-extract-json receipt))
                    ($ :div {:id (str "receipt-extract-json-" rid-str)}
                      ($ json-display-card
                        {:title "Extracted Data"
                         :json-value (:raw-extract-json receipt)})))
                  (when (seq (:raw-parse-json receipt))
                    ($ :div {:id (str "receipt-parse-json-" rid-str)}
                      ($ json-display-card
                        {:title "LlamaParse Results"
                         :json-value (:raw-parse-json receipt)}))))))))))))

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

(defui receipts-list-page
  []
  (let [title "Receipts"
        error (use-subscribe [:user-expenses/receipts-error])
        receipts (or (use-subscribe [:user-expenses/receipts]) [])
        processing-count (->> receipts
                           (filter (fn [receipt]
                                     (receipt-processing? (:status receipt))))
                           count)
        processing? (pos? processing-count)
        [last-checked set-last-checked!] (use-state nil)
        refresh! (use-callback
                   (fn []
                     (rf/dispatch [:user-expenses/fetch-receipts {:limit 50 :offset 0}])
                     (set-last-checked! (js/Date.)))
                   [])
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

    ;; Initial load
    (use-effect
      (fn []
        (refresh!)
        js/undefined)
      [refresh!])

    ;; Auto-poll while any receipt is processing
    (use-effect
      (fn []
        (when processing?
          (let [handle (js/setInterval refresh! 3000)]
            (fn []
              (js/clearInterval handle)))))
      [refresh! processing?])

    ($ :<>
      ($ :div {:class "p-6 min-h-screen bg-base-100"}
        ($ :div {:class "ds-card ds-bg-base-100 ds-shadow-xl"}
          ($ :div {:class "ds-card-body p-0"}
            (when error
              ($ :div {:class "px-4 pt-4"}
                ($ :div {:class "ds-alert ds-alert-error"}
                  ($ :span (str error)))))

            ;; Top bar: live processing indicator + batch parse button
            ($ :div {:class (str "flex items-center gap-2 px-4 pt-4 "
                              (if processing? "justify-between" "justify-end"))}
              (when processing?
                ($ :div {:id "receipt-processing-banner"
                         :class "flex items-center gap-2"}
                  ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
                  ($ :span {:class "text-sm"}
                    (str "Processing " processing-count " receipt" (when (not= 1 processing-count) "s") "…"))
                  (when last-checked
                    ($ :span {:class "text-xs text-base-content/60"}
                      (str "Last checked: " (format-time last-checked))))
                  ($ :button {:id "btn-refresh-user-receipts"
                              :class "ds-btn ds-btn-ghost ds-btn-xs"
                              :type "button"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (refresh!))}
                    "Refresh")))
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
