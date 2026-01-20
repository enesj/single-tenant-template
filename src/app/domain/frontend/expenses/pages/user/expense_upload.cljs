(ns app.domain.frontend.expenses.pages.user.expense-upload
  "User-facing receipt upload page for expense tracking."
  (:require
    [app.domain.frontend.expenses.components.page-guard :as page-guard]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.auth-guard :refer [auth-guard]]
    [app.template.frontend.components.file-drop-zone :refer [file-drop-zone]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- payer-default?
  [payer]
  (boolean (or (:is-default payer)
             (:isDefault payer))))

(defn- default-payer-id
  [payers]
  (let [payers (or payers [])]
    (or (some (fn [p]
                (when (payer-default? p)
                  (:id p)))
          payers)
      (:id (first payers)))))

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
                        "posted" "ds-badge-primary"
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

  payers (or (use-subscribe [:user-expenses/payers]) [])
  selected-payer-id (use-subscribe [:user-expenses/upload-payer-id])

        uploading? (boolean (use-subscribe [:user-expenses/upload-loading?]))
        upload-batch (use-subscribe [:user-expenses/upload-batch])
        upload-error (use-subscribe [:user-expenses/upload-error])
        upload-notice (use-subscribe [:user-expenses/upload-notice])
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

    (use-effect
      (fn []
        ;; Ensure payer options exist for the upload-scoped payer chooser.
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    (use-effect
      (fn []
        ;; If user hasn't chosen an upload payer yet, default to the "default" payer.
        (when (and (seq payers)
                (str/blank? (some-> selected-payer-id str)))
          (when-let [default-id (some-> (default-payer-id payers) str str/trim not-empty)]
            (rf/dispatch [:user-expenses/set-upload-payer-id default-id])))
        js/undefined)
      [payers selected-payer-id])

    ($ auth-guard
      {:authenticated? authenticated?
       :loading? auth-loading?
       :error auth-error
       :auth-type :customer
       :login-redirect-path "/login?redirect=/expenses/upload"
       :login-message "Please sign in to upload a receipt."
       :children
       ($ page-guard/expenses-page-guard
         {:capability :expenses/upload
          :children
          ($ :div {:class "min-h-screen bg-base-100"}
            ;; Header
            ($ :header {:class "bg-white border-b border-base-200"}
              ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
                ($ :div {:class "flex items-center justify-between"}
                  ($ :div
                    ($ :div {:class "text-sm ds-breadcrumbs"}
                      ($ :ul
                        ($ :li ($ :a {:id "link-expenses-breadcrumb-upload-receipt"
                                      :href "/expenses"}
                                 "Expenses"))
                        ($ :li "Upload Receipt")))
                    ($ :h1 {:class "text-xl sm:text-2xl font-bold"} "Upload Receipt"))
                  ($ :div {:class "flex gap-2"}
                    ($ button {:id "btn-dashboard-expense-upload"
                               :btn-type :ghost
                               :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                      "Dashboard")
                    ($ button {:id "btn-manual-entry-expense-upload"
                               :btn-type :outline
                               :on-click handle-manual}
                      "Manual Entry")))))

            ;; Notice
            (when (seq upload-notice)
              ($ :div {:class "max-w-4xl mx-auto px-4 mt-4"}
                ($ :div {:class "ds-alert ds-alert-warning"}
                  (if (sequential? upload-notice)
                    ($ :ul {:class "list-disc pl-5 text-sm"}
                      (for [msg upload-notice]
                        ($ :li {:key msg} msg)))
                    ($ :span (str upload-notice))))))

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

                ;; Payer selection (upload-scoped)
                ($ :div {:class "mb-6"}
                  ($ :label {:id "label-payer-expense-upload"
                             :class "ds-label"}
                    ($ :span {:class "ds-label-text font-medium"} "Payer"))
                  ($ :select {:id "select-payer-expense-upload"
                              :class "ds-select ds-select-bordered w-full max-w-md"
                              :disabled uploading?
                              :value (or (some-> selected-payer-id str) "")
                              :on-change (fn [e]
                                           (let [v (-> e .-target .-value)]
                                             (rf/dispatch [:user-expenses/set-upload-payer-id v])))}
                    ($ :option {:value ""} "Select payer")
                    (for [{:keys [id label]} payers]
                      ($ :option {:key (str id) :value (str id)} (or label (str id)))))
                  ($ :div {:id "help-payer-expense-upload"
                           :class "text-xs text-base-content/60 mt-2"}
                    "This applies only to the receipts you're uploading now and does not change your default payer."))

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
                ($ :a {:id "link-expense-new-from-upload"
                       :href "/expenses/new"
                       :class "text-sm text-primary hover:underline"}
                  "Enter expense manually →"))))})})))
