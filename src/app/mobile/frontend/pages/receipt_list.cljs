(ns app.mobile.frontend.pages.receipt-list
  "Mobile receipt list — scrollable list of receipts with status badges."
  (:require
    [ajax.core :as ajax]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Events
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-receipts
  (fn [{:keys [db]} [_ {:keys [append?]}]]
    (let [offset (if append?
                   (count (get-in db [:mobile :receipts :data] []))
                   0)]
      {:db (assoc-in db [:mobile :receipts :loading?] true)
       :http-xhrio {:method :get
                    :uri "/api/v1/expenses/receipts"
                    :params {:limit 20 :offset offset}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/receipts-loaded append?]
                    :on-failure [:mobile/receipts-failed]}})))

(rf/reg-event-db
  :mobile/receipts-loaded
  (fn [db [_ append? response]]
    (let [new-data (:data response)
          existing (if append? (get-in db [:mobile :receipts :data] []) [])]
      (-> db
        (assoc-in [:mobile :receipts :data] (vec (concat existing new-data)))
        (assoc-in [:mobile :receipts :total] (:total response))
        (assoc-in [:mobile :receipts :loading?] false)))))

(rf/reg-event-db
  :mobile/receipts-failed
  (fn [db [_ error]]
    (-> db
      (assoc-in [:mobile :receipts :loading?] false)
      (assoc-in [:mobile :receipts :error]
        (or (get-in error [:response :error]) "Failed to load receipts")))))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/receipts
  (fn [db _]
    (get-in db [:mobile :receipts :data] [])))

(rf/reg-sub
  :mobile/receipts-total
  (fn [db _]
    (get-in db [:mobile :receipts :total] 0)))

(rf/reg-sub
  :mobile/receipts-loading?
  (fn [db _]
    (get-in db [:mobile :receipts :loading?] false)))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- format-date [s]
  (when s
    (try
      (let [d (js/Date. s)]
        (str (.getDate d) "." (inc (.getMonth d)) "." (.getFullYear d)))
      (catch :default _ (str s)))))

(def ^:private status-styles
  {"uploaded"        {:class "ds-badge-info"    :key :mobile/status-uploaded}
   "parsing"         {:class "ds-badge-info"    :key :mobile/status-processing}
   "parsed"          {:class "ds-badge-info"    :key :mobile/status-processing}
   "extracting"      {:class "ds-badge-info"    :key :mobile/status-extracting}
   "extracted"       {:class "ds-badge-success" :key :mobile/status-ready}
   "review_required" {:class "ds-badge-warning" :key :mobile/status-review}
   "approved"        {:class "ds-badge-success" :key :mobile/status-approved}
   "posted"          {:class "ds-badge-primary" :key :mobile/status-posted}
   "failed"          {:class "ds-badge-error"   :key :mobile/status-failed}})

;; ========================================================================
;; Components
;; ========================================================================

(defui receipt-card [{:keys [receipt]}]
  (let [t (use-t)
        supplier (or (:supplier_guess receipt) (:supplier-guess receipt) "—")
        total (or (:total_amount_guess receipt) (:total-amount-guess receipt))
        currency (or (:currency_guess receipt) (:currency-guess receipt) "BAM")
        date (or (:created_at receipt) (:created-at receipt))
        status (or (:status receipt) "uploaded")
        style (get status-styles status {:class "ds-badge-ghost" :key nil})]
    ($ :div {:class "flex items-center p-3 bg-base-100 rounded-xl shadow-sm"}
      ;; Thumbnail placeholder
      ($ :div {:class "flex-shrink-0 w-12 h-12 bg-base-200 rounded-lg flex items-center justify-center mr-3"}
        ($ :svg {:class "w-6 h-6 text-base-content/30" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                    :d "M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"})))
      ;; Details
      ($ :div {:class "flex-1 min-w-0"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :p {:class "text-sm font-medium truncate flex-1 mr-2"} supplier)
          ($ :span {:class (str "ds-badge ds-badge-sm " (:class style))}
            (if-let [k (:key style)] (t k) status)))
        ($ :div {:class "flex items-center gap-2 mt-0.5"}
          ($ :span {:class "text-xs text-base-content/50"} (format-date date))
          (when total
            ($ :span {:class "text-xs font-medium"}
              (str (.toFixed (js/parseFloat (str total)) 2) " " currency))))))))

;; ========================================================================
;; Main page
;; ========================================================================

(defui receipt-list-page []
  (let [t (use-t)
        receipts (use-subscribe [:mobile/receipts])
        total (use-subscribe [:mobile/receipts-total])
        loading? (use-subscribe [:mobile/receipts-loading?])]

    ;; Fetch on mount
    (uix/use-effect
      (fn [] (rf/dispatch [:mobile/fetch-receipts {}]))
      [])

    ($ :<>
      ($ mobile-header {:title (t :mobile/receipts-title "Receipts") :show-back? true})

      ($ :div {:class "p-4 space-y-2 pb-24"}
        ;; Loading
        (when (and loading? (empty? receipts))
          ($ :div {:class "space-y-2"}
            (for [i (range 5)]
              ($ :div {:key i :class "bg-base-100 rounded-xl p-4 shadow-sm animate-pulse h-16"}))))

        ;; Empty state
        (when (and (empty? receipts) (not loading?))
          ($ :div {:class "text-center py-12"}
            ($ :p {:class "text-base-content/50 text-sm"}
              (t :mobile/no-receipts "No receipts found"))))

        ;; Receipt list
        (for [receipt receipts]
          ($ receipt-card {:key (or (:id receipt) (random-uuid))
                           :receipt receipt}))

        ;; Load more
        (when (and (seq receipts) (< (count receipts) total))
          ($ :div {:class "text-center pt-2"}
            ($ :button
              {:class (str "ds-btn ds-btn-ghost ds-btn-sm "
                        (when loading? "ds-loading"))
               :disabled loading?
               :on-click #(rf/dispatch [:mobile/fetch-receipts {:append? true}])}
              (when-not loading?
                (t :mobile/load-more "Load more")))))))))
