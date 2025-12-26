(ns app.domain.frontend.expenses.events.user-expenses.receipts
  "User receipts inbox events (list/detail/approve)."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:user-expenses :receipts])

;; ---------------------------------------------------------------------------
;; List receipts
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-receipts
  common-interceptors
  (fn [{:keys [db]} [payload]]
    (let [{:keys [limit offset status]} (or payload {})
          limit* (or limit 50)
          offset* (or offset 0)]
      {:db (-> db
             (assoc-in (paths/entity-loading? :receipts) true)
             (assoc-in (paths/entity-error :receipts) nil)
             (assoc-in (conj base-path :loading?) true)
             (assoc-in (conj base-path :error) nil)
             (assoc-in (conj base-path :limit) limit*)
             (assoc-in (conj base-path :offset) offset*))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/receipts-endpoint
                      :admin-uri endpoints/admin-receipts-endpoint
                      :params (cond-> {:limit limit* :offset offset*}
                                (some? status) (assoc :status status))
                      :on-success [:user-expenses/fetch-receipts-success]
                      :on-failure [:user-expenses/fetch-receipts-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-receipts-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [rows (or (:data response) (:receipts response) [])
          limit (or (:limit response) (get-in db (conj base-path :limit)))
          offset (or (:offset response) (get-in db (conj base-path :offset)))]
      {:db (-> db
             (assoc-in (paths/entity-loading? :receipts) false)
             (assoc-in (paths/entity-error :receipts) nil)
             (assoc-in (conj base-path :loading?) false)
             (assoc-in (conj base-path :error) nil)
             (assoc-in (conj base-path :items) (vec rows))
             (assoc-in (conj base-path :limit) limit)
             (assoc-in (conj base-path :offset) offset))
       :dispatch [::expenses-sync/sync-receipts rows]})))

(rf/reg-event-db
  :user-expenses/fetch-receipts-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch receipts" {:error error})
    (-> db
      (assoc-in (paths/entity-loading? :receipts) false)
      (assoc-in (paths/entity-error :receipts) (http/extract-error-message error))
      (assoc-in (conj base-path :loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

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
;; Receipt detail
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
                    :admin-uri (str endpoints/admin-receipts-endpoint "/" receipt-id)
                    :on-success [:user-expenses/fetch-receipt-success receipt-id]
                    :on-failure [:user-expenses/fetch-receipt-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-receipt-success
  common-interceptors
  (fn [db [receipt-id response]]
    (let [receipt (or (:data response) (:receipt response) response)]
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

;; ---------------------------------------------------------------------------
;; Approve & post
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/approve-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil)
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri (str endpoints/receipts-endpoint "/" receipt-id "/approve")
                    :admin-uri (str endpoints/admin-receipts-endpoint "/" receipt-id "/approve")
                    :params form-data
                    :on-success [:user-expenses/approve-receipt-success receipt-id on-success]
                    :on-failure [:user-expenses/approve-receipt-failure]})}))

(rf/reg-event-fx
  :user-expenses/approve-receipt-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [expense (or (get-in response [:data :expense])
                    (:expense response))
          receipt (or (get-in response [:data :receipt])
                    (:receipt response))
          expense-id (or (:id expense) (get-in expense [:id]))
          fx (cond-> []
               on-success (conj [:dispatch-later {:ms 100}
                                 :dispatch [:user-expenses/call-modal-callback on-success]]))]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :form :loading?] false)
                     (assoc-in [:user-expenses :form :error] nil)
                     (assoc-in (conj base-path :action-loading?) false)
                     (assoc-in (conj base-path :error) nil)
                     (cond-> receipt
                       (assoc-in (conj base-path :by-id receipt-id) receipt)))
               :dispatch-n (cond-> [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                                    [:user-expenses/fetch-receipts {:limit 50 :offset 0}]
                                    [:user-expenses/fetch-receipt receipt-id]]
                             expense-id (conj [:navigate-to (str "/expenses/" expense-id)]))
               :fx fx}
        expense
        (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-db
  :user-expenses/approve-receipt-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to approve receipt" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error))
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))
