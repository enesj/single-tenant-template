(ns app.domain.frontend.expenses.pages.user.receipts-list
  "User-facing receipts inbox (review + approve) using template list-view UX."
  (:require
    [app.domain.frontend.expenses.components.receipt-detail-modal :as receipt-detail-ui]
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal]]
    [app.template.frontend.components.action-components :refer [view-details-icon]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.dropdown :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.utils.id :as id-utils]
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

(def ^:private receipt-detail-ctx
  {:receipt-sub :user-expenses/receipt
   :receipt-detail-loading-sub :user-expenses/receipt-detail-loading?
   :receipt-action-loading-sub :user-expenses/receipt-action-loading?
   :receipts-error-sub :user-expenses/receipts-error
   :modal-open-sub :user-expenses/receipt-detail-modal-open?
   :modal-id-sub :user-expenses/receipt-detail-modal-id
   :fetch-receipt-event :user-expenses/fetch-receipt
   :close-modal [:user-expenses/close-receipt-detail-modal]
   :approve-form user-expense-add-form-modal})

(defui receipt-detail-body
  [{:keys [receipt-id]}]
  ($ receipt-detail-ui/receipt-detail-body
    {:receipt-id receipt-id
     :ctx receipt-detail-ctx}))

(defui receipt-detail-modal
  []
  ($ receipt-detail-ui/receipt-detail-modal
    {:id "user-receipt-detail-modal"
     :ctx receipt-detail-ctx}))

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
