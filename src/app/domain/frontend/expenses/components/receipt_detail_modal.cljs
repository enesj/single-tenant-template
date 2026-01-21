(ns app.domain.frontend.expenses.components.receipt-detail-modal
  "Shared Receipt Details modal UI.

  Goal: keep /receipts and /admin/receipts 'View Details' modals pixel-identical,
  while allowing different data sources (subs) and behaviors (events/endpoints).

  This component is intentionally 'context-driven' via a small config map of
  subscription keys and event keywords."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.admin.frontend.components.tabs :as tabs]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-preview receipt-viewer]]
    [app.template.frontend.components.json-highlight :refer [json-display-card]]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- dispatch!
  [on-close]
  (cond
    (fn? on-close) (on-close)
    (vector? on-close) (rf/dispatch on-close)
    (and (sequential? on-close) (seq on-close)) (rf/dispatch (vec on-close))
    :else nil))

(defn- detail-header
  [{:keys [title subtitle icon]}]
  ($ shared/detail-modal-header
    {:title title
     :subtitle subtitle
     :icon icon
     :icon-bg "bg-primary/10"}))

(defui receipt-problem-alert
  "Identical warning/error banner for both user/admin receipt review."
  [{:keys [receipt]}]
  (let [status (:status receipt)
        raw-extract (:raw-extract-json receipt)
        valid-shape? (:valid-shape? raw-extract)
        items (get-in raw-extract [:extraction :items])
        items-count (when (sequential? items) (count items))
        missing-supplier? (str/blank? (:supplier-guess receipt))
        missing-total? (nil? (:total-amount-guess receipt))
        missing-currency? (str/blank? (:currency-guess receipt))
        missing-items? (or (nil? items-count) (zero? (long items-count)))
        review-issues (when (= "review_required" status)
                        (cond-> []
                          (false? valid-shape?) (conj "OCR extraction response did not match the expected format.")
                          missing-supplier? (conj "Missing supplier guess.")
                          missing-total? (conj "Missing total amount guess.")
                          missing-currency? (conj "Missing currency guess.")
                          missing-items? (conj "No line items were extracted.")))
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

(defn- label-value
  [label value]
  ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
    ($ :div {:class "ds-card-body p-4"}
      ($ :div {:class "text-xs text-base-content/60"} label)
      ($ :div {:class "text-sm font-medium break-words"}
        (shared/format-value value "—" false)))))

(def ^:private receipt-processing-statuses
  #{"uploaded" "parsing" "parsed" "extracting"})

(defn- receipt-processing?
  [status]
  (contains? receipt-processing-statuses status))

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
  "Shared receipt detail body with user-style tabs + polling.

  ctx keys:
  - :receipt-sub
  - :receipt-detail-loading-sub
  - :receipt-action-loading-sub
  - :receipts-error-sub
  - :fetch-receipt-event
  - :approve-form (UIX component)

  Notes:
  - The approve form component is expected to accept keys:
    {:receipt-id :receipt :on-success :on-review-saved :on-cancel}
    and may ignore any it doesn't use."
  [{:keys [receipt-id ctx]}]
  (let [{:keys [receipt-sub
                receipt-detail-loading-sub
                receipt-action-loading-sub
                receipts-error-sub
                fetch-receipt-event
                approve-form
                close-modal
                poll-interval-ms]
         :or {poll-interval-ms 2500}} ctx
        receipt (use-subscribe [receipt-sub receipt-id])
        loading? (boolean (use-subscribe [receipt-detail-loading-sub]))
        action-loading? (boolean (use-subscribe [receipt-action-loading-sub]))
        error (use-subscribe [receipts-error-sub])
        can-approve? (boolean (use-subscribe [:expenses/can? :expenses/receipts.approve]))
        [active-tab set-active-tab!] (use-state (if can-approve? :approve :details))
        [preview-expanded? set-preview-expanded!] (use-state false)
        [processing-started-at set-processing-started-at!] (use-state nil)
        [last-checked set-last-checked!] (use-state nil)
        refresh! (use-callback
                   (fn []
                     (when receipt-id
                       (rf/dispatch [fetch-receipt-event receipt-id])
                       (set-last-checked! (js/Date.))))
                   [receipt-id fetch-receipt-event])
        close-modal-fn (use-callback
                         (fn []
                           (dispatch! close-modal))
                         [close-modal])
        status (when (map? receipt) (:status receipt))
        processing? (and (string? status) (receipt-processing? status))
        rid (or receipt-id
              (when (map? receipt)
                (id-utils/extract-entity-id receipt)))
        rid-str (if rid (str rid) "unknown")]

    ;; Reset tab + fetch on open/change
    (use-effect
      (fn []
        (set-active-tab! (if can-approve? :approve :details))
        (set-preview-expanded! false)
        (refresh!)
        js/undefined)
      [receipt-id refresh! can-approve?])

    ;; Auto-poll receipt detail while OCR is processing
    (use-effect
      (fn []
        (when (and receipt-id processing?)
          (let [handle (js/setInterval refresh! poll-interval-ms)]
            (fn []
              (js/clearInterval handle)))))
      [receipt-id processing? refresh! poll-interval-ms])

    ;; Track processing duration for the banner.
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

    ($ :div {:class "space-y-4"}
      (when processing?
        ($ :div {:id (str "receipt-detail-processing-banner-" rid-str)
                 :class "ds-alert ds-alert-info"}
          ($ :div {:class "flex items-center justify-between gap-2 w-full"}
            ($ :div {:class "flex items-center gap-2"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
              ($ :span {:class "text-sm"} "Processing…")
              (when-let [duration (format-duration processing-started-at last-checked)]
                ($ :span {:class "text-xs text-base-content/60"}
                  (str "Duration: " duration))))
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
              (when can-approve?
                (tabs/tab-link {:id (str "tab-receipt-approve-" rid-str)
                                :label "Approve & Post"
                                :active? (= active-tab :approve)
                                :on-select #(set-active-tab! :approve)})))

            (case active-tab
              :receipt
              ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
                ($ :div {:class "ds-card-body p-0"}
                  ($ receipt-viewer {:receipt receipt
                                     :show-summary? false})))

              :approve
              (if (and can-approve? approve-allowed?)
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
                        (when approve-form
                          ($ approve-form
                            {:receipt-id rid-str
                             :receipt receipt
                             :on-success (fn []
                                           (close-modal-fn))
                             :on-review-saved (fn []
                                                (rf/dispatch [fetch-receipt-event receipt-id]))
                             :on-cancel #(set-active-tab! :details)}))))))

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
  "Shared receipt details modal.

  Required ctx keys:
  - :modal-open-sub
  - :modal-id-sub
  - :receipt-sub
  - :receipt-detail-loading-sub
  - :close-modal (event vector or function)

  Plus all keys required by `receipt-detail-body`."
  [{:keys [id ctx]}]
  (let [{:keys [modal-open-sub
                modal-id-sub
                receipt-sub
                close-modal]}
        ctx
        open? (use-subscribe [modal-open-sub])
        receipt-id (use-subscribe [modal-id-sub])
        receipt (use-subscribe [receipt-sub receipt-id])
        subtitle (or (:original-filename receipt)
                   (when receipt-id (str "Receipt " receipt-id))
                   "Receipt details")
        header (detail-header {:title "Receipt Details"
                               :subtitle subtitle
                               :icon "R"})]
    (when open?
      ($ modal {:id id
                :on-close #(dispatch! close-modal)
                :draggable? true
                :width "960px"
                :z-index 120
                :class "max-w-[95vw] h-[85vh] flex flex-col"
                :header header
                :header-class "p-0 border-0 bg-transparent mb-3"}
        ($ :div {:class "flex-1 overflow-y-auto p-4"}
          ($ receipt-detail-body {:receipt-id receipt-id
                                  :ctx ctx}))))))
