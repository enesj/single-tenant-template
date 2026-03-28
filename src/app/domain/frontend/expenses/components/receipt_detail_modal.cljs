(ns app.domain.frontend.expenses.components.receipt-detail-modal
  "Shared Receipt Details modal UI.

  Goal: keep /receipts and /admin/receipts 'View Details' modals pixel-identical,
  while allowing different data sources (subs) and behaviors (events/endpoints).

  This component is intentionally 'context-driven' via a small config map of
  subscription keys and event keywords.

  Layout:
  - Row 1: Two columns (resizable, default 1/3 left, 2/3 right)
    - Left: Tabs for Image, Extracted HTML, Raw Extract JSON
    - Right: Approval form fields (without line items)
  - Row 2: Line items (always visible, full width)"
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.admin.frontend.components.tabs :as tabs]
    [app.domain.frontend.expenses.components.receipt-viewer :refer [receipt-preview]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.json-viewer :refer [json-copy-text json-viewer]]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-ref use-state]]
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

(defn- capitalize-words
  [s]
  (when (string? s)
    (->> (str/split (str/replace s #"_" " ") #"\\s+")
      (map str/capitalize)
      (str/join " "))))

(defui receipt-detail-body
  "Shared receipt detail body with two-row layout + polling.

  Layout:
  - Row 1: Two columns
    - Left: Tabs for Image, Extracted HTML, Raw Extract JSON
    - Right: Approval form fields (without line items)
  - Row 2: Line items (always visible, full width)

  ctx keys:
  - :receipt-sub
  - :receipt-detail-loading-sub
  - :receipt-action-loading-sub
  - :receipts-error-sub
  - :fetch-receipt-event
  - :approve-form (UIX component)

  Notes:
  - The approve form component is expected to accept keys:
    {:receipt-id :receipt :on-success :on-review-saved :on-cancel :exclude-line-items?}
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
        ;; Left column visibility toggle
        [show-left-column? set-show-left-column!] (use-state true)
        ;; Left column tabs: :image, :extracted-html, :raw-json
        [left-tab set-left-tab!] (use-state :image)
        ;; Resizable columns: left column width percentage (default 33%)
        [left-width-pct set-left-width-pct!] (use-state 40)
        [is-resizing? set-is-resizing!] (use-state false)
        container-ref (use-ref nil)
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
        processing-status? (and (string? status) (receipt-processing? status))
        refining? (receipt-refine-pending? receipt)
        processing? (or processing-status? refining?)
        processing-label (if (and (not processing-status?) refining?) "Refining" "Processing")
        rid (or receipt-id
              (when (map? receipt)
                (id-utils/extract-entity-id receipt)))
        rid-str (if rid (str rid) "unknown")
        approve-form-visible? (contains? #{"extracted" "review_required" "posted"} status)

        ;; Resize handlers
        handle-resize-start (use-callback
                              (fn [e]
                                (.preventDefault e)
                                (set-is-resizing! true))
                              [])
        handle-resize-move (use-callback
                             (fn [e]
                               (when (and is-resizing? @container-ref)
                                 (let [container-el @container-ref
                                       rect (.getBoundingClientRect container-el)
                                       container-width (.-width rect)
                                       mouse-x (- (.-clientX e) (.-left rect))
                                       new-pct (-> (/ mouse-x container-width)
                                                 (* 100)
                                                 (max 20)  ;; min 20%
                                                 (min 60))] ;; max 60%
                                   (set-left-width-pct! new-pct))))
                             [is-resizing?])
        handle-resize-end (use-callback
                            (fn [_e]
                              (set-is-resizing! false))
                            [])

        ;; Toggle left column visibility
        toggle-left-column! (use-callback
                              (fn [e]
                                (.preventDefault e)
                                (set-show-left-column! not))
                              [])]

    ;; Reset tab + fetch on open/change
    (use-effect
      (fn []
        (set-left-tab! :image)
        (refresh!)
        js/undefined)
      [receipt-id refresh!])

    ;; Attach/detach resize event listeners
    (use-effect
      (fn []
        (when is-resizing?
          (.addEventListener js/document "mousemove" handle-resize-move)
          (.addEventListener js/document "mouseup" handle-resize-end))
        (fn []
          (.removeEventListener js/document "mousemove" handle-resize-move)
          (.removeEventListener js/document "mouseup" handle-resize-end)))
      [is-resizing? handle-resize-move handle-resize-end])

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

    ($ :div {:class "space-y-4 flex-1 min-h-0 flex flex-col"}
      (when processing?
        ($ :div {:id (str "receipt-detail-processing-banner-" rid-str)
                 :class "ds-alert ds-alert-info"}
          ($ :div {:class "flex items-center justify-between gap-2 w-full"}
            ($ :div {:class "flex items-center gap-2"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
              ($ :span {:class "text-sm"} (str processing-label "…"))
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
        ($ :div {:class "space-y-4 flex-1 min-h-0 flex flex-col"}
          ($ receipt-problem-alert {:receipt receipt})

          ;; ROW 1: Two resizable columns (default 1/3 left, 2/3 right)
          ($ :div {:ref container-ref
                   :class (str "flex gap-0 flex-1 min-h-0 " (when is-resizing? "select-none cursor-col-resize"))}

            ;; LEFT COLUMN: Tabs for Image, Extracted HTML, Raw Extract JSON
            (when show-left-column?
              ($ :div {:class "flex flex-col space-y-3 min-h-0 overflow-y-auto overflow-x-hidden overscroll-contain pr-1"
                       :style {:width (str left-width-pct "%")
                               :minWidth "20%"
                               :maxWidth "60%"}}
                ($ :div {:class "flex items-center justify-between gap-2"}
                  ($ :div {:class "ds-tabs ds-tabs-boxed ds-tabs-sm flex-1"}
                    (tabs/tab-link {:id (str "tab-receipt-image-" rid-str)
                                    :label "Image"
                                    :active? (= left-tab :image)
                                    :on-select #(set-left-tab! :image)})
                    (tabs/tab-link {:id (str "tab-receipt-extracted-html-" rid-str)
                                    :label "Extracted Markdown"
                                    :active? (= left-tab :extracted-html)
                                    :on-select #(set-left-tab! :extracted-html)})
                    (tabs/tab-link {:id (str "tab-receipt-raw-json-" rid-str)
                                    :label "Raw Extract JSON"
                                    :active? (= left-tab :raw-json)
                                    :on-select #(set-left-tab! :raw-json)}))
                  ;; Hide button in left column
                  ($ :button {:id (str "btn-hide-left-column-" rid-str)
                              :type "button"
                              :title "Hide preview panel"
                              :class "ds-btn ds-btn-ghost ds-btn-xs"
                              :on-click toggle-left-column!}
                    "✕"))

                ($ :div {:class "ds-card ds-card-bordered bg-base-100 flex-1 flex flex-col overflow-hidden"}
                  ($ :div {:class "ds-card-body p-2 flex-1 overflow-hidden min-h-0"}
                    (case left-tab
                      :image
                      ($ receipt-preview {:receipt receipt
                                          :title "Receipt Image"
                                          :expanded? true
                                          :preview-height-class "flex-1 min-h-0"})

                      :extracted-html
                      (let [parsed-markdown (:parsed-markdown receipt)]
                        (if (seq parsed-markdown)
                          ($ :div {:class "space-y-2"}
                            ($ :h3 {:class "text-sm font-semibold"} "Extracted HTML / Parsed Markdown")
                            ($ :pre {:class "text-xs whitespace-pre-wrap bg-base-200 p-3 rounded-lg max-h-[400px] overflow-y-auto"}
                              parsed-markdown))
                          ($ :div {:class "text-sm text-base-content/60 p-4"}
                            "No extracted HTML available for this receipt.")))

                      :raw-json
                      (let [raw-extract (:raw-extract-json receipt)]
                        (if (seq raw-extract)
                          ($ :div {:class "flex flex-col h-full min-h-0 -m-2"}
                            ($ :div {:class "flex items-center justify-between gap-2 px-2 py-1 mb-2"}
                              ($ :div {:class "flex items-center gap-2"}
                                ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
                                ($ :h3 {:class "text-sm font-semibold"} "Raw Extract JSON"))
                              ($ :button
                                {:class "ds-btn ds-btn-xs ds-btn-ghost ds-btn-circle"
                                 :title "Copy to clipboard"
                                 :on-click (fn [e]
                                             (.stopPropagation e)
                                             (.writeText js/navigator.clipboard (json-copy-text raw-extract)))}
                                ($ :svg {:class "w-3 h-3" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                                            :d "M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"}))))
                            ($ :div {:class "flex-1 min-h-0 bg-base-200/50 rounded-md p-2 overflow-y-auto overscroll-contain border border-base-200"}
                              ($ json-viewer {:data raw-extract
                                              :class "text-xs leading-relaxed"})))
                          ($ :div {:class "text-sm text-base-content/60 p-4"}
                            "No raw extract JSON available for this receipt."))))))))

            ;; RESIZE HANDLE (only show when left column is visible)
            (when show-left-column?
              ($ :div {:class "flex-shrink-0 w-2 cursor-col-resize hover:bg-primary/20 active:bg-primary/30 transition-colors relative group"
                       :on-mouse-down handle-resize-start}
                ($ :div {:class "absolute inset-y-0 left-1/2 -translate-x-1/2 w-0.5 bg-base-300 group-hover:bg-primary/50 transition-colors"})))

            ;; RIGHT COLUMN: Approve & Post form (with split layout for line items below)
            ($ :div {:class "flex-1 min-h-0 space-y-2 overflow-y-auto overflow-x-hidden overscroll-contain pl-3 pr-1"}
              ;; Show button banner (only visible when left column is hidden)
              (when (not show-left-column?)
                ($ :button {:id (str "btn-show-left-column-" rid-str)
                            :type "button"
                            :title "Click to show receipt image and extracted data"
                            :class "w-full flex items-center justify-between ds-alert bg-primary/5 hover:bg-primary/10 border-primary/20 cursor-pointer transition-all py-2 px-3"
                            :on-click toggle-left-column!}
                  ($ :span {:class "text-sm"} "Preview panel is hidden")
                  ($ :span {:class "text-xs text-primary/70 font-medium"} "Click to show")))
              ($ :div {:class "flex items-center justify-between gap-2"}
                ($ :h2 {:class "text-base font-semibold"} "Approve & Post")
                (when action-loading?
                  ($ :span {:class "text-xs text-base-content/60"} "Working...")))

              (if (and can-approve? approve-form-visible?)
                (when approve-form
                  ($ approve-form
                    {:receipt-id rid-str
                     :receipt receipt
                     :split-layout? true
                     :on-success (fn []
                                   (close-modal-fn))
                     :on-review-saved (fn []
                                        (rf/dispatch [fetch-receipt-event receipt-id]))
                     :on-cancel nil}))

                ($ :div {:class "ds-alert ds-alert-info"}
                  ($ :span
                    (cond
                      (not can-approve?)
                      "You don't have permission to approve receipts."

                      :else
                      (str "Approval is available when status is extracted or review_required. Current status: "
                        (or (capitalize-words status) "unknown")
                        "."))))))))))))

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
    ($ modal-wrapper
      {:id id
       :visible? open?
       :on-close #(dispatch! close-modal)
       :draggable? true
       :resizable? true
       :initial-position {:y 24}
       :width "960px"
       :z-index 120
       :backdrop-opacity 20
       :class "max-w-[95vw] max-h-[85vh] h-[85vh] flex flex-col"
       :header header
       :header-class "p-0 border-0 bg-transparent mb-3"
       :content-class "p-0 overflow-hidden min-h-0 flex flex-col"
       :close-button-id (str "btn-close-" (or id "receipt-detail-modal"))}
      ($ :div {:class "flex flex-col flex-1 h-full min-h-0 overflow-hidden p-4"}
        ($ receipt-detail-body {:receipt-id receipt-id
                                :ctx ctx})))))
