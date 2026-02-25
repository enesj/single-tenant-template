(ns app.domain.frontend.expenses.events.user-expenses.receipts.detail
  "Receipt detail modal and per-receipt fetch events."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:user-expenses :receipts])

;; ---------------------------------------------------------------------------
;; Receipt detail modal (UX parity with /admin/receipts)
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :user-expenses/open-receipt-detail-modal
  common-interceptors
  (fn [db [receipt-id]]
    (-> db
      (assoc-in (conj base-path :detail-modal :open?) true)
      (assoc-in (conj base-path :detail-modal :entity-id) (some-> receipt-id str)))))

(rf/reg-event-db
  :user-expenses/close-receipt-detail-modal
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in (conj base-path :detail-modal :open?) false)
      (assoc-in (conj base-path :detail-modal :entity-id) nil))))

;; ---------------------------------------------------------------------------
;; Receipt detail fetch
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id]]
    {:db (-> db
           (assoc-in (conj base-path :detail-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri (str endpoints/receipts-endpoint "/" receipt-id)
                    :on-success [:user-expenses/fetch-receipt-success receipt-id]
                    :on-failure [:user-expenses/fetch-receipt-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-receipt-success
  common-interceptors
  (fn [db [receipt-id response]]
    (let [receipt (:data response)]
      (-> db
        (assoc-in (conj base-path :detail-loading?) false)
        (assoc-in (conj base-path :error) nil)
        (assoc-in (conj base-path :by-id receipt-id) receipt)))))

(rf/reg-event-db
  :user-expenses/fetch-receipt-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch receipt" {:error error})
    (-> db
      (assoc-in (conj base-path :detail-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))
