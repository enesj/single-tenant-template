(ns app.domain.frontend.expenses.pages.user.expense-upload
  "User-facing receipt upload page for expense tracking."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.auth-guard :refer [auth-guard]]
    [app.template.frontend.components.file-drop-zone :refer [file-drop-zone]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui recent-uploads [{:keys [receipts]}]
  (when (seq receipts)
    ($ :div {:class "mt-8"}
      ($ :h3 {:class "font-semibold mb-4"} "Recent Uploads")
      ($ :div {:class "space-y-2"}
        (for [{:keys [id status
                      original_filename original-filename
                      created_at created-at
                      error_message error-message]} receipts]
          (let [name (or original-filename original_filename "Receipt")
                created (or created-at created_at "")
                err (or error-message error_message)
                icon (case status
                       "uploaded" "📤"
                       "parsing" "⏳"
                       "parsed" "📝"
                       "extracting" "⏳"
                       "extracted" "✅"
                       "review_required" "🟡"
                       "posted" "📌"
                       "failed" "❌"
                       "📄")
                badge (case status
                        "extracted" "ds-badge-success"
                        "posted" "ds-badge-success"
                        "review_required" "ds-badge-warning"
                        "failed" "ds-badge-error"
                        "ds-badge-ghost")]
            ($ :div {:key id
                     :class "flex items-center justify-between p-3 bg-base-200 rounded-lg"}
              ($ :div {:class "flex items-center gap-3"}
                ($ :span {:class "text-2xl"} icon)
                ($ :div
                  ($ :p {:class "font-medium text-sm"} name)
                  ($ :p {:class "text-xs text-base-content/60"} created)
                  (when (and (= status "failed") err)
                    ($ :p {:class "text-xs text-error"} err))))
              ($ :span {:class (str "ds-badge ds-badge-sm " badge)} status))))))))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expense-upload-page []
  (let [auth-status (use-subscribe [:auth-status])
        authenticated? (boolean (:authenticated auth-status))
        auth-loading? (boolean (:loading? auth-status))
        auth-error (:error auth-status)

        uploading? (boolean (use-subscribe [:user-expenses/upload-loading?]))
        upload-batch (use-subscribe [:user-expenses/upload-batch])
        upload-error (use-subscribe [:user-expenses/upload-error])
        recent-receipts (or (use-subscribe [:user-expenses/recent-receipts]) [])
        uploading-label (when (and uploading? (map? upload-batch))
                          (let [{:keys [total done failed current]} upload-batch
                                processed (+ (or done 0) (or failed 0))
                                idx (inc processed)]
                                (when (and (number? total) (> total 1))
                              (str "Uploading " idx " of " total
                                (when current (str ": " current))
                                "..."))))

        handle-files-select (fn [files]
                              (when (seq files)
                                (rf/dispatch [:user-expenses/upload-receipts files])))

        handle-manual (fn []
                        (rf/dispatch [:navigate-to "/expenses/new"]))]

    ($ auth-guard
      {:authenticated? authenticated?
       :loading? auth-loading?
       :error auth-error
       :auth-type :customer
       :login-redirect-path "/login?redirect=/expenses/upload"
       :login-message "Please sign in to upload a receipt."
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ;; Header
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
             ($ :div {:class "flex items-center justify-between"}
               ($ :div
                 ($ :div {:class "text-sm ds-breadcrumbs"}
                   ($ :ul
                     ($ :li ($ :a {:href "/expenses"} "Expenses"))
                     ($ :li "Upload Receipt")))
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold"} "Upload Receipt"))
               ($ :div {:class "flex gap-2"}
                 ($ button {:btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")
                 ($ button {:btn-type :outline
                            :on-click handle-manual}
                   "Manual Entry")))))

         ;; Error
         (when upload-error
           ($ :div {:class "max-w-4xl mx-auto px-4 mt-4"}
             ($ :div {:class "ds-alert ds-alert-error"}
               ($ :span upload-error))))

         ;; Content
         ($ :main {:class "max-w-4xl mx-auto px-4 py-6"}
           ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-6"}
             ;; Instructions
             ($ :div {:class "mb-6"}
               ($ :p {:class "text-base-content/80"}
                 "Upload a photo of your receipt and we'll extract the expense details automatically. "
                 "You can review and edit the extracted information before saving."))

             ;; Upload zone
             ($ file-drop-zone {:dropzone-id "dropzone-receipt-upload"
                                :input-id "receipt-upload"
                                :choose-button-id "btn-choose-receipt-upload"
                                :on-files-select handle-files-select
                                :uploading? uploading?
                                :uploading-label (or uploading-label "Processing receipts...")
                                :accept "image/*,.pdf"
                                :multiple? true
                                :title "Drop your receipts here"
                                :subtitle "or click to browse"
                                :choose-label "Choose Files"
                                :icon "📷"
                                :help-text "Supports: JPG, PNG, PDF (max 10MB)"})

             ;; Tips
             ($ :div {:class "mt-6 bg-base-200 rounded-lg p-4"}
               ($ :h4 {:class "font-medium text-sm mb-2"} "📌 Tips for best results:")
               ($ :ul {:class "text-sm text-base-content/70 space-y-1 list-disc list-inside"}
                 ($ :li "Make sure the receipt is well-lit and in focus")
                 ($ :li "Include the entire receipt in the frame")
                 ($ :li "Avoid wrinkled or damaged receipts when possible")
                 ($ :li "PDF receipts from email work great too!")))

             ;; Recent uploads
             ($ recent-uploads {:receipts recent-receipts}))

           ;; Alternative action
           ($ :div {:class "mt-6 text-center"}
             ($ :p {:class "text-sm text-base-content/60"}
               "Don't have a receipt? ")
             ($ :a {:href "/expenses/new"
                    :class "text-sm text-primary hover:underline"}
               "Enter expense manually →"))))})))
