(ns app.domain.frontend.expenses.events.receipts
  "Receipts domain events - generated using the expenses event factory."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :receipts])

;; Register standard CRUD events for receipts using the factory
(factory/register-entity-events! configs/receipts-config)

(rf/reg-event-fx
  ::retry-extraction
  (fn [{:keys [db]} [_ receipt-id]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/retry")
                    :on-success [::retry-extraction-success receipt-id]
                    :on-failure [::retry-extraction-failure receipt-id]})}))

(rf/reg-event-db
  ::retry-extraction-success
  (fn [db [_ receipt-id response]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) nil)
      (assoc-in (conj base-path :by-id receipt-id) (:receipt response)))))

(rf/reg-event-db
  ::retry-extraction-failure
  (fn [db [_ _ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::update-status
  (fn [{:keys [db]} [_ receipt-id new-status]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/status")
                    :params {:status new-status}
                    :on-success [::update-status-success receipt-id]
                    :on-failure [::update-status-failure receipt-id]})}))

(rf/reg-event-db
  ::update-status-success
  (fn [db [_ receipt-id response]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) nil)
      (assoc-in (conj base-path :by-id receipt-id) (:receipt response)))))

(rf/reg-event-db
  ::update-status-failure
  (fn [db [_ _ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::approve-receipt
  (fn [{:keys [db]} [_ receipt-id form-data on-success]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/approve")
                    :params form-data
                    :on-success [::approve-receipt-success receipt-id on-success]
                    :on-failure [::approve-receipt-failure receipt-id]})}))

(rf/reg-event-fx
  ::approve-receipt-success
  (fn [{:keys [db]} [_ receipt-id on-success response]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     :dispatch-n [[:admin/refresh-entity :receipts (:receipt response)]
                  [:admin/refresh-entity :expenses (:expense response)]
                  [::load-detail receipt-id]]
     :fx [(when on-success [:dispatch [::expenses-events/call-modal-callback on-success]])]}))

(rf/reg-event-db
  ::approve-receipt-failure
  (fn [db [_ _ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::save-receipt-review
  (fn [{:keys [db]} [_ receipt-id form-data on-success]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/review")
                    :params form-data
                    :on-success [::save-receipt-review-success receipt-id on-success]
                    :on-failure [::save-receipt-review-failure receipt-id]})}))

(rf/reg-event-fx
  ::save-receipt-review-success
  (fn [{:keys [db]} [_ receipt-id on-success response]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     :dispatch-n [[:admin/refresh-entity :receipts (:receipt response)]
                  [::load-detail receipt-id]]
     :fx [(when on-success
            [:dispatch [::expenses-events/call-modal-callback on-success]])]}))

(rf/reg-event-db
  ::save-receipt-review-failure
  (fn [db [_ _ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; OCR Events (UI-triggered)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  ::ocr-receipt
  (fn [{:keys [db]} [_ receipt-id]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id "/ocr")
                    :on-success [::ocr-receipt-success receipt-id]
                    :on-failure [::ocr-receipt-failure receipt-id]})}))

(rf/reg-event-fx
  ::ocr-receipt-success
  (fn [{:keys [db]} [_ _receipt-id _response]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     ;; Refresh the receipts list to show updated status
     :dispatch-n [[::load-list]]}))

(rf/reg-event-db
  ::ocr-receipt-failure
  (fn [db [_ _receipt-id error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::ocr-selected
  (fn [{:keys [db]} [_ receipt-ids]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri "/admin/api/expenses/receipts/ocr"
                    :params {:receipt_ids (vec receipt-ids)}
                    :on-success [::ocr-selected-success receipt-ids]
                    :on-failure [::ocr-selected-failure]})}))

(rf/reg-event-fx
  ::ocr-selected-success
  (fn [{:keys [db]} [_ _receipt-ids _response]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     ;; Refresh the receipts list and clear selection
     :dispatch-n [[::load-list]
                  [:app.template.frontend.events.list/clear-selection :receipts]]}))

(rf/reg-event-db
  ::ocr-selected-failure
  (fn [db [_ error]]
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (admin-http/extract-error-message error)))))
