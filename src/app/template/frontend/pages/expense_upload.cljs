(ns app.template.frontend.pages.expense-upload
  "User-facing receipt upload page for expense tracking."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Upload Component
;; ========================================================================

(defui file-drop-zone [{:keys [on-file-select uploading?]}]
  (let [[drag-over? set-drag-over!] (use-state false)]
    ($ :div {:class (str "border-2 border-dashed rounded-xl p-8 text-center transition-colors "
                      (if drag-over?
                        "border-primary bg-primary/5"
                        "border-base-300 hover:border-primary/50"))
             :on-drag-over (fn [e]
                             (.preventDefault e)
                             (set-drag-over! true))
             :on-drag-leave #(set-drag-over! false)
             :on-drop (fn [e]
                        (.preventDefault e)
                        (set-drag-over! false)
                        (let [files (.. e -dataTransfer -files)]
                          (when (pos? (.-length files))
                            (on-file-select (aget files 0)))))}
      (if uploading?
        ($ :div {:class "flex flex-col items-center gap-4"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})
          ($ :p {:class "text-base-content/70"} "Processing receipt..."))
        ($ :div {:class "flex flex-col items-center gap-4"}
          ($ :div {:class "text-6xl"} "📷")
          ($ :div
            ($ :p {:class "font-semibold text-lg"} "Drop your receipt here")
            ($ :p {:class "text-sm text-base-content/60 mt-1"}
              "or click to browse"))
          ($ :input {:type "file"
                     :class "hidden"
                     :id "receipt-upload"
                     :accept "image/*,.pdf"
                     :on-change (fn [e]
                                  (let [file (.. e -target -files (item 0))]
                                    (when file (on-file-select file))))})
          ($ :label {:for "receipt-upload"
                     :class "ds-btn ds-btn-primary ds-btn-sm cursor-pointer"}
            "Choose File")
          ($ :p {:class "text-xs text-base-content/50 mt-4"}
            "Supports: JPG, PNG, PDF (max 10MB)"))))))

(defui recent-uploads [{:keys [receipts]}]
  (when (seq receipts)
    ($ :div {:class "mt-8"}
      ($ :h3 {:class "font-semibold mb-4"} "Recent Uploads")
      ($ :div {:class "space-y-2"}
        (for [{:keys [id file_name status created_at]} receipts]
          ($ :div {:key id
                   :class "flex items-center justify-between p-3 bg-base-200 rounded-lg"}
            ($ :div {:class "flex items-center gap-3"}
              ($ :span {:class "text-2xl"}
                (case status
                  "processed" "✅"
                  "processing" "⏳"
                  "failed" "❌"
                  "📄"))
              ($ :div
                ($ :p {:class "font-medium text-sm"} file_name)
                ($ :p {:class "text-xs text-base-content/60"} created_at)))
            ($ :span {:class (str "ds-badge ds-badge-sm "
                               (case status
                                 "processed" "ds-badge-success"
                                 "processing" "ds-badge-warning"
                                 "failed" "ds-badge-error"
                                 "ds-badge-ghost"))}
              status)))))))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expense-upload-page []
  (let [uploading? (boolean (use-subscribe [:user-expenses/upload-loading?]))
        upload-error (use-subscribe [:user-expenses/upload-error])
        recent-receipts (or (use-subscribe [:user-expenses/recent-receipts]) [])
        [selected-file set-selected-file!] (use-state nil)
        
        handle-file-select (fn [file]
                             (set-selected-file! file)
                             (rf/dispatch [:user-expenses/upload-receipt file]))
        
        handle-manual (fn []
                        (rf/dispatch [:navigate-to "/expenses/new"]))]
    
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
          ($ file-drop-zone {:on-file-select handle-file-select
                             :uploading? uploading?})
          
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
            "Enter expense manually →"))))))
