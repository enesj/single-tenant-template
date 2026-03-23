(ns app.mobile.frontend.pages.receipt-review
  "Mobile receipt confirmation — review OCR results and approve."
  (:require
    [ajax.core :as ajax]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.mobile.frontend.pages.manual-entry :refer [autocomplete-field]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Events — fetch receipt detail
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-receipt
  (fn [_ [_ receipt-id]]
    {:http-xhrio {:method :get
                  :uri (str "/api/v1/expenses/receipts/" receipt-id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/receipt-loaded]
                  :on-failure [:mobile/receipt-load-failed]}}))

(rf/reg-event-db
  :mobile/receipt-loaded
  (fn [db [_ response]]
    (let [receipt (:data response)]
      (assoc-in db [:mobile :receipt-detail] receipt))))

(rf/reg-event-db
  :mobile/receipt-load-failed
  (fn [db [_ error]]
    (assoc-in db [:mobile :receipt-detail-error]
              (or (get-in error [:response :error]) "Failed to load receipt"))))

;; ========================================================================
;; Events — fetch pending receipts list
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-pending-receipts
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/receipts"
                  :params {:status "extracted,review_required"
                           :limit 20
                           :offset 0}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/pending-receipts-loaded]
                  :on-failure [:mobile/pending-receipts-failed]}}))

(rf/reg-event-db
  :mobile/pending-receipts-loaded
  (fn [db [_ response]]
    (-> db
      (assoc-in [:mobile :pending-receipts] (:data response))
      (assoc-in [:mobile :pending-review-count] (or (:total response) (count (:data response)))))))

(rf/reg-event-db
  :mobile/pending-receipts-failed
  (fn [db _] db))

;; ========================================================================
;; Events — approve receipt
;; ========================================================================

(rf/reg-event-fx
  :mobile/approve-receipt
  (fn [{:keys [db]} [_ receipt-id form-data]]
    {:db (assoc-in db [:mobile :receipt-approving?] true)
     :http-xhrio {:method :post
                  :uri (str "/api/v1/expenses/receipts/" receipt-id "/approve")
                  :params form-data
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/receipt-approved]
                  :on-failure [:mobile/receipt-approve-failed]}}))

(rf/reg-event-fx
  :mobile/receipt-approved
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:mobile :receipt-approving?] false)
           (update-in [:mobile :pending-review-count] (fnil dec 1)))
     :fx [[:dispatch [:mobile/show-toast "Expense created"]]
          [:dispatch [:mobile/navigate "/m/upload"]]]}))

(rf/reg-event-fx
  :mobile/receipt-approve-failed
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in [:mobile :receipt-approving?] false)
           (assoc-in [:mobile :receipt-approve-error]
             (or (get-in error [:response :error]) "Failed to approve receipt")))}))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/receipt-detail
  (fn [db _]
    (get-in db [:mobile :receipt-detail])))

(rf/reg-sub
  :mobile/receipt-detail-error
  (fn [db _]
    (get-in db [:mobile :receipt-detail-error])))

(rf/reg-sub
  :mobile/receipt-approving?
  (fn [db _]
    (get-in db [:mobile :receipt-approving?] false)))

(rf/reg-sub
  :mobile/pending-receipts
  (fn [db _]
    (get-in db [:mobile :pending-receipts] [])))

(rf/reg-sub
  :mobile/receipt-approve-error
  (fn [db _]
    (get-in db [:mobile :receipt-approve-error])))

;; ========================================================================
;; Components
;; ========================================================================

(defn- format-date-for-input
  "Convert ISO timestamp string to datetime-local format."
  [s]
  (when s
    (let [s (str s)]
      (cond
        ;; Already in datetime-local format (YYYY-MM-DDTHH:MM)
        (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}.*" s)
        (subs s 0 16)

        ;; Date only (YYYY-MM-DD) — add midnight
        (re-matches #"\d{4}-\d{2}-\d{2}" s)
        (str s "T00:00")

        :else s))))

(defn- format-currency [amount currency]
  (when amount
    (str (.toFixed (js/parseFloat (str amount)) 2) " " (or currency "BAM"))))

(defui receipt-image [{:keys [receipt-id]}]
  (let [[img-loaded? set-img-loaded!] (uix/use-state false)
        [img-error? set-img-error!] (uix/use-state false)]
    ($ :div {:class "bg-base-200 rounded-lg overflow-hidden"}
      (if img-error?
        ($ :div {:class "flex items-center justify-center h-48 text-base-content/40"}
          ($ :svg {:class "w-12 h-12" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                      :d "M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z"})))
        ($ :img {:src (str "/api/v1/expenses/receipts/" receipt-id "/download?preview=true")
                 :class (str "w-full max-h-64 object-contain "
                          (when-not img-loaded? "hidden"))
                 :on-load #(set-img-loaded! true)
                 :on-error #(set-img-error! true)}))
      (when (and (not img-loaded?) (not img-error?))
        ($ :div {:class "flex items-center justify-center h-48"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"}))))))

(defui line-items-list [{:keys [items]}]
  (when (seq items)
    ($ :div {:class "space-y-1"}
      (for [[idx item] (map-indexed vector items)]
        (let [label (or (:description item) (:raw_label item) (:label item) "—")
              total (or (:line_total item) (:total item))]
          ($ :div {:key idx
                   :class "flex items-center justify-between py-1.5 px-2 bg-base-100 rounded"}
            ($ :span {:class "text-sm truncate flex-1 mr-2"} label)
            (when total
              ($ :span {:class "text-sm font-medium text-base-content/70 whitespace-nowrap"}
                (.toFixed (js/parseFloat (str total)) 2)))))))))

;; ========================================================================
;; Pending receipts list (used on upload page)
;; ========================================================================

(defui pending-receipt-card [{:keys [receipt]}]
  (let [t (use-t)
        id (or (:id receipt) (str (random-uuid)))
        supplier (or (:supplier_guess receipt) (:supplier-guess receipt)
                   (t :mobile/unknown-supplier "Unknown supplier"))
        total (:total_amount_guess receipt)
        currency (or (:currency_guess receipt) (:currency-guess receipt) "BAM")
        status (or (:status receipt) "extracted")
        needs-review? (= status "review_required")]
    ($ :button
      {:class "flex items-center w-full p-3 bg-base-100 rounded-xl shadow-sm active:bg-base-200 transition-colors"
       :on-click #(rf/dispatch [:mobile/navigate (str "/m/upload/review/" id)])}
      ($ :div {:class "flex-shrink-0 w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center mr-3"}
        ($ :svg {:class "w-5 h-5 text-primary" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                    :d "M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"})))
      ($ :div {:class "flex-1 text-left min-w-0"}
        ($ :p {:class "text-sm font-medium truncate"} supplier)
        (when total
          ($ :p {:class "text-xs text-base-content/60"} (format-currency total currency))))
      (when needs-review?
        ($ :span {:class "ds-badge ds-badge-warning ds-badge-sm ml-2"} "!"))
      ($ :svg {:class "w-4 h-4 text-base-content/40 ml-2 flex-shrink-0" :fill "none" :viewBox "0 0 24 24" :stroke-width "2" :stroke "currentColor"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M8.25 4.5l7.5 7.5-7.5 7.5"})))))

(defui pending-receipts-section []
  (let [t (use-t)
        pending (use-subscribe [:mobile/pending-receipts])
        pending-count (use-subscribe [:mobile/pending-review-count])]

    ;; Fetch on mount
    (uix/use-effect
      (fn [] (rf/dispatch [:mobile/fetch-pending-receipts]))
      [])

    (when (and pending-count (pos? pending-count))
      ($ :div {:class "space-y-3 mt-4"}
        ($ :h2 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
          (str (t :mobile/pending-reviews "Pending Reviews") " (" pending-count ")"))
        ($ :div {:class "space-y-2"}
          (for [receipt pending]
            ($ pending-receipt-card
              {:key (or (:id receipt) (random-uuid))
               :receipt receipt})))))))

;; ========================================================================
;; Main review page
;; ========================================================================

(defui receipt-review-page []
  (let [t (use-t)
        route (use-subscribe [:mobile/current-route])
        receipt-id (get-in route [:path-params :id])
        receipt (use-subscribe [:mobile/receipt-detail])
        load-error (use-subscribe [:mobile/receipt-detail-error])
        approve-error (use-subscribe [:mobile/receipt-approve-error])
        approving? (use-subscribe [:mobile/receipt-approving?])
        [form set-form!] (uix/use-state nil)

        update-field! (fn [k v] (set-form! (fn [f] (assoc f k v))))

        extraction (or (get-in receipt [:raw_extract_json :extraction])
                     (get-in receipt [:raw-extract-json :extraction]))
        items (or (:items extraction) [])]

    ;; Fetch receipt on mount or ID change
    (uix/use-effect
      (fn []
        (when receipt-id
          (rf/dispatch [:mobile/fetch-receipt receipt-id])))
      [receipt-id])

    ;; Initialize form when receipt loads
    (uix/use-effect
      (fn []
        (when (and receipt (nil? form))
          (let [supplier-obj (or (:supplier_guess_supplier receipt)
                               (:supplier-guess-supplier receipt))]
            (set-form!
              {:supplier-search (or (:supplier_guess receipt)
                                  (:supplier-guess receipt) "")
               :supplier-id (or (:id supplier-obj) nil)
               :purchased-at (format-date-for-input
                               (or (:purchased_at_guess receipt)
                                 (:purchased-at-guess receipt)))
               :total-amount (str (or (:total_amount_guess receipt)
                                    (:total-amount-guess receipt) ""))
               :currency (or (:currency_guess receipt)
                           (:currency-guess receipt) "BAM")
               :payer-search ""
               :payer-id (or (:payer_id receipt) (:payer-id receipt))
               :notes (or (:notes receipt) "")}))))
      [form receipt])

    ($ :<>
      ($ mobile-header {:title (t :mobile/review-receipt "Review Receipt") :show-back? true})

      ($ :div {:class "p-4 pb-24 space-y-4"}
        ;; Error banners
        (when load-error
          ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span load-error)))
        (when approve-error
          ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span approve-error)))

        ;; Loading state
        (when (and (not receipt) (not load-error))
          ($ :div {:class "flex items-center justify-center h-48"}
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})))

        (when (and receipt form)
          ($ :<>
            ;; Receipt image
            ($ receipt-image {:receipt-id receipt-id})

            ;; Supplier (autocomplete)
            ($ autocomplete-field
              {:label (t :common/supplier "Supplier")
               :placeholder (t :mobile/search-supplier "Search supplier...")
               :entity-type :supplier
               :search-value (:supplier-search form)
               :on-search-change #(update-field! :supplier-search %)
               :on-select (fn [id label]
                            (update-field! :supplier-id id)
                            (update-field! :supplier-search (or label "")))
               :selected-label (when (:supplier-id form) (:supplier-search form))})

            ;; Date
            ($ :div
              ($ :label {:class "ds-label text-sm"} (t :common/date "Date"))
              ($ :input {:type "datetime-local"
                         :class "ds-input ds-input-bordered w-full"
                         :value (or (:purchased-at form) "")
                         :on-change #(update-field! :purchased-at (.. % -target -value))}))

            ;; Amount + Currency
            ($ :div {:class "grid grid-cols-3 gap-3"}
              ($ :div {:class "col-span-2"}
                ($ :label {:class "ds-label text-sm"} (t :common/amount "Amount"))
                ($ :input {:type "number"
                           :step "0.01"
                           :class "ds-input ds-input-bordered w-full"
                           :placeholder "0.00"
                           :value (or (:total-amount form) "")
                           :on-change #(update-field! :total-amount (.. % -target -value))}))
              ($ :div
                ($ :label {:class "ds-label text-sm"} (t :common/currency "Currency"))
                ($ :select {:class "ds-select ds-select-bordered w-full"
                            :value (or (:currency form) "BAM")
                            :on-change #(update-field! :currency (.. % -target -value))}
                  ($ :option {:value "BAM"} "BAM")
                  ($ :option {:value "EUR"} "EUR")
                  ($ :option {:value "USD"} "USD")
                  ($ :option {:value "GBP"} "GBP")
                  ($ :option {:value "CHF"} "CHF")
                  ($ :option {:value "HRK"} "HRK")
                  ($ :option {:value "RSD"} "RSD"))))

            ;; Payer (autocomplete)
            ($ autocomplete-field
              {:label (t :common/payer "Payer")
               :placeholder (t :mobile/search-payer "Search payer...")
               :entity-type :payer
               :search-value (:payer-search form)
               :on-search-change #(update-field! :payer-search %)
               :on-select (fn [id label]
                            (update-field! :payer-id id)
                            (update-field! :payer-search (or label "")))
               :selected-label (when (:payer-id form) (:payer-search form))})

            ;; Notes
            ($ :div
              ($ :label {:class "ds-label text-sm"} (t :common/notes "Notes"))
              ($ :textarea {:class "ds-textarea ds-textarea-bordered w-full"
                            :rows 2
                            :placeholder (t :mobile/notes-placeholder "Optional notes...")
                            :value (or (:notes form) "")
                            :on-change #(update-field! :notes (.. % -target -value))}))

            ;; Line Items (read-only)
            (when (seq items)
              ($ :div {:class "space-y-2"}
                ($ :h3 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
                  (t :common/line-items "Line Items"))
                ($ :div {:class "bg-base-200 rounded-lg p-2"}
                  ($ line-items-list {:items items}))))

            ;; Confirm button (fixed bottom)
            ($ :div {:class "fixed bottom-16 left-0 right-0 p-4 bg-base-100 border-t border-base-300"}
              ($ :button
                {:class (str "ds-btn ds-btn-primary w-full h-12 text-base "
                          (when approving? "ds-loading"))
                 :disabled approving?
                 :on-click (fn []
                             (let [{:keys [supplier-id purchased-at total-amount
                                           currency payer-id notes]} form
                                   parse-num (fn [s] (when (seq (str s))
                                                       (let [n (js/parseFloat s)]
                                                         (when-not (js/isNaN n) n))))
                                   body (cond-> {}
                                          supplier-id (assoc :supplier_id (str supplier-id))
                                          purchased-at (assoc :purchased_at purchased-at)
                                          (parse-num total-amount) (assoc :total_amount (parse-num total-amount))
                                          currency (assoc :currency currency)
                                          payer-id (assoc :payer_id (str payer-id))
                                          (seq notes) (assoc :notes notes)
                                          (seq items) (assoc :items items))]
                               (rf/dispatch [:mobile/approve-receipt receipt-id body])))}
                (when-not approving?
                  (t :mobile/confirm-approve "Confirm & Save"))))))))))
