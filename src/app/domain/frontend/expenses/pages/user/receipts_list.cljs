(ns app.domain.frontend.expenses.pages.user.receipts-list
  "User-facing receipts inbox (review + approve) using template list-view UX."
  (:require
    [app.domain.frontend.expenses.components.receipt-detail-modal :as receipt-detail-ui]
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal]]
    [app.template.frontend.components.action-components :refer [view-details-icon]]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
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

(defn- receipt-refine-pending?
  [receipt]
  (let [refine-pending (or (:refine-pending receipt)
                         (:refine_pending receipt)
                         (get-in receipt [:raw-extract-json :refine-pending])
                         (get-in receipt [:raw-extract-json :refine_pending])
                         (get-in receipt [:raw_extract_json :refine_pending]))]
    (true? refine-pending)))

(defn- pad2
  [n]
  (let [s (str (or n 0))]
    (if (= 1 (count s))
      (str "0" s)
      s)))

(defn- format-duration-ms
  "Format a duration in milliseconds as H:MM:SS or M:SS."
  [ms]
  (let [ms (max 0 (long (or ms 0)))
        total-sec (quot ms 1000)
        sec (mod total-sec 60)
        total-min (quot total-sec 60)
        min (mod total-min 60)
        hrs (quot total-min 60)]
    (if (pos? hrs)
      (str hrs ":" (pad2 min) ":" (pad2 sec))
      (str total-min ":" (pad2 sec)))))

(defn- format-duration
  [started-at last-checked]
  (when (and started-at last-checked
          (instance? js/Date started-at)
          (instance? js/Date last-checked))
    (format-duration-ms (- (.getTime last-checked) (.getTime started-at)))))

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
  [can-ocr? receipt]
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
                        (and can-ocr? ocr-allowed?)
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

(defui receipts-list-page
  []
  (let [title "Receipts"
        error (use-subscribe [:user-expenses/receipts-error])
        form-error (use-subscribe [:user-expenses/form-error])
        receipts (or (use-subscribe [:user-expenses/receipts]) [])
        can-ocr? (boolean (use-subscribe [:expenses/can-write?]))
        processing-count (->> receipts
                           (filter (fn [receipt]
                                     (receipt-processing? (:status receipt))))
                           count)
        refining-count (->> receipts
                         (filter receipt-refine-pending?)
                         count)
        processing? (or (pos? processing-count) (pos? refining-count))
        processing-label (if (pos? processing-count) "Processing" "Refining")
        processing-total (if (pos? processing-count) processing-count refining-count)
        [processing-started-at set-processing-started-at!] (use-state nil)
        [last-checked set-last-checked!] (use-state nil)
        refresh! (use-callback
                   (fn []
                     (rf/dispatch [:user-expenses/fetch-receipts {:limit 50 :offset 0}])
                     (set-last-checked! (js/Date.)))
                   [])
        display-settings {:show-select? false
                          :show-edit? false
                          :show-delete? false
                          :show-filtering? true
                          :show-pagination? true

                          :show-highlights? true
                          :show-add-button? can-ocr?
                          :show-batch-edit? false
                          :show-batch-delete? false}]

    ;; Initial load
    (use-effect
      (fn []
        (refresh!)
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-settings])
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

    ;; Track a processing "session" start time so we can display duration.
    (use-effect
      (fn []
        (cond
          (and processing? (nil? processing-started-at))
          (let [now (js/Date.)]
            (set-processing-started-at! now)
            (set-last-checked! now))

          (and (not processing?) (or processing-started-at last-checked))
          (do
            (set-processing-started-at! nil)
            (set-last-checked! nil)))
        js/undefined)
      [processing? processing-started-at last-checked])

    ($ :<>
      ($ :div {:class "p-6 min-h-screen bg-base-100"}
        ($ :div {:class "ds-card ds-bg-base-100 ds-shadow-xl"}
          ($ :div {:class "ds-card-body p-0"}
            (when error
              ($ :div {:class "px-4 pt-4"}
                ($ :div {:class "ds-alert ds-alert-error"}
                  ($ :span (str error)))))

            (when form-error
              ($ :div {:class "px-4 pt-4"}
                ($ :div {:class "ds-alert ds-alert-error flex items-center justify-between"}
                  ($ :span (str form-error))
                  ($ :button {:id "btn-clear-receipts-form-error"
                              :type "button"
                              :class "ds-btn ds-btn-ghost ds-btn-xs"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (rf/dispatch [:user-expenses/clear-form-error]))}
                    "✕"))))

            ;; Top bar: live processing indicator
            ($ :div {:class (str "flex items-center gap-2 px-4 pt-4 "
                              (if processing? "justify-between" "justify-end"))}
              (when processing?
                ($ :div {:id "receipt-processing-banner"
                         :class "flex items-center gap-2"}
                  ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
                  ($ :span {:class "text-sm"}
                    (str processing-label " " processing-total " receipt" (when (not= 1 processing-total) "s") "…"))
                  (when-let [duration (format-duration processing-started-at last-checked)]
                    ($ :span {:class "text-xs text-base-content/60"}
                      (str "Duration: " duration)))
                  ($ :button {:id "btn-refresh-user-receipts"
                              :class "ds-btn ds-btn-ghost ds-btn-xs"
                              :type "button"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (refresh!))}
                    "Refresh"))))

            ($ :div {:class "w-full pb-0 [&>div>table]:w-full"}
              ($ list-view
                {:entity-name :receipts
                 :entity-spec receipts-entity-spec
                 :title title
                 :display-settings display-settings
                 :custom-actions (fn [receipt]
                                   (receipt-actions can-ocr? receipt))
                 :on-add-click #(rf/dispatch [:navigate-to "/expenses/upload"])})))))
      ($ receipt-detail-modal))))
