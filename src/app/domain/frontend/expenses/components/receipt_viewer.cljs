(ns app.domain.frontend.expenses.components.receipt-viewer
  "Receipt detail display for admin UI."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.template.frontend.components.json-highlight :refer [json-display-card]]
    [uix.core :refer [$ defui]]))

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

(defn- label-value
  [label value]
  ($ :div {:class "flex flex-col gap-1 p-3 bg-base-200 rounded-lg"}
    ($ :span {:class "text-xs uppercase tracking-wide text-base-content/70"} label)
    ($ :span {:class "text-sm font-medium"}
      (shared/format-value value "—" false))))

(defui receipt-viewer
  [{:keys [receipt show-summary?] :or {show-summary? true}}]
  (let [{:keys [status original-filename content-type file-size storage-key
                supplier-guess total-amount-guess currency-guess purchased-at-guess
                payment-hints error-message error-details raw-parse-json raw-extract-json
                parsed-markdown expense-id retry-count created-at updated-at]} receipt
        status-label (shared/format-value status "—" false)]
    ($ :div {:class "grid gap-6 lg:grid-cols-2"}
      ($ :div {:class "space-y-4"}
        (when show-summary?
          ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
            ($ :div {:class "ds-card-body space-y-3"}
              ($ :div {:class "flex items-center gap-2"}
                ($ :span {:class (status-class status)} status-label)
                (when (seq error-message)
                  ($ :span {:class "text-xs text-error"} error-message)))
              ($ :div {:class "grid gap-3 md:grid-cols-2"}
                (label-value "Original File" original-filename)
                (label-value "Storage Key" storage-key)
                (label-value "Content Type" content-type)
                (label-value "File Size" (format-bytes file-size))
                (label-value "Supplier Guess" supplier-guess)
                (label-value "Total Guess" total-amount-guess)
                (label-value "Currency" currency-guess)
                (label-value "Purchased At" (shared/format-date purchased-at-guess))
                (label-value "Retry Count" retry-count)
                (label-value "Expense ID" expense-id)
                (label-value "Created At" (shared/format-date created-at))
                (label-value "Updated At" (shared/format-date updated-at))))))

        ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
          ($ :div {:class "ds-card-body"}
            ($ :h3 {:class "text-sm font-semibold"} "Preview")
            ($ :p {:class "text-xs text-base-content/60"}
              "Receipt preview is not available without a download URL.")))

        (when (seq parsed-markdown)
          ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
            ($ :div {:class "ds-card-body"}
              ($ :h3 {:class "text-sm font-semibold"} "Parsed Markdown")
              ($ :pre {:class "text-xs whitespace-pre-wrap"} parsed-markdown)))))

      ($ :div {:class "space-y-4"}
        (when (seq raw-parse-json)
          ($ json-display-card
            {:title "Raw Parse JSON"
             :json-value raw-parse-json
             :max-height "max-h-96"}))
        (when (seq raw-extract-json)
          ($ json-display-card
            {:title "Raw Extract JSON"
             :json-value raw-extract-json
             :max-height "max-h-96"}))
        (when (seq payment-hints)
          ($ json-display-card
            {:title "Payment Hints"
             :json-value payment-hints
             :max-height "max-h-80"}))
        (when (seq error-details)
          ($ json-display-card
            {:title "Error Details"
             :json-value error-details
             :max-height "max-h-80"}))))))
