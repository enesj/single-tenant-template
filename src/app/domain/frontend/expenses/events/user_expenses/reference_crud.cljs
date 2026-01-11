(ns app.domain.frontend.expenses.events.user-expenses.reference-crud
  "User-facing reference data CRUD (suppliers + payers).

  These events call the user-scoped expenses endpoints (\"/api/v1/expenses/*\")
  and refresh the shared entity store via the existing lookup fetch events."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(rf/reg-event-db
  :user-expenses/clear-form-error
  common-interceptors
  (fn [db _]
    (assoc-in db [:user-expenses :form :error] nil)))

;; ---------------------------------------------------------------------------
;; Suppliers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/create-supplier-modal
  common-interceptors
  (fn [{:keys [db]} [supplier-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/suppliers-endpoint
                    :admin-uri endpoints/admin-suppliers-endpoint
                    :params supplier-data
                    :on-success [:user-expenses/create-supplier-modal-success on-success]
                    :on-failure [:user-expenses/create-supplier-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-supplier-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [supplier (or (:data response) (:supplier response) response)
          supplier-id (or (:id supplier)
                        (get-in response [:data :id])
                        (get-in response [:supplier :id]))
          highlight-id (some-> supplier-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :suppliers highlight-id)))
       :dispatch-n [[:user-expenses/fetch-suppliers]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success supplier]}])]})))

(rf/reg-event-db
  :user-expenses/create-supplier-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create supplier" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-supplier-modal
  common-interceptors
  (fn [{:keys [db]} [supplier-id supplier-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/suppliers-endpoint "/" supplier-id)
                    :admin-uri (str endpoints/admin-suppliers-endpoint "/" supplier-id)
                    :params supplier-data
                    :on-success [:user-expenses/update-supplier-modal-success supplier-id on-success]
                    :on-failure [:user-expenses/update-supplier-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-supplier-modal-success
  common-interceptors
  (fn [{:keys [db]} [supplier-id on-success _response]]
    (let [highlight-id (some-> supplier-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-updated :suppliers highlight-id)))
       :dispatch-n [[:user-expenses/fetch-suppliers]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-supplier-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update supplier" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-supplier
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :delete
                    :uri (str endpoints/suppliers-endpoint "/" supplier-id)
                    :admin-uri (str endpoints/admin-suppliers-endpoint "/" supplier-id)
                    :on-success [:user-expenses/delete-supplier-success]
                    :on-failure [:user-expenses/delete-supplier-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-supplier-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-suppliers]}))

(rf/reg-event-db
  :user-expenses/delete-supplier-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete supplier - DEBUG" {:error error :keys (keys error) :response (:response error)})
    (log/warn "Extracted message:" (http/extract-error-message error))
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Payers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/create-payer-modal
  common-interceptors
  (fn [{:keys [db]} [payer-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/payers-endpoint
                    :admin-uri endpoints/admin-payers-endpoint
                    :params payer-data
                    :on-success [:user-expenses/create-payer-modal-success on-success]
                    :on-failure [:user-expenses/create-payer-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-payer-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [payer-id (or (get-in response [:data :id])
                     (get-in response [:payer :id]))
          highlight-id (some-> payer-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :payers highlight-id)))
       :dispatch-n [[:user-expenses/fetch-payers]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/create-payer-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create payer" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-payer-modal
  common-interceptors
  (fn [{:keys [db]} [payer-id payer-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/payers-endpoint "/" payer-id)
                    :admin-uri (str endpoints/admin-payers-endpoint "/" payer-id)
                    :params payer-data
                    :on-success [:user-expenses/update-payer-modal-success payer-id on-success]
                    :on-failure [:user-expenses/update-payer-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-payer-modal-success
  common-interceptors
  (fn [{:keys [db]} [payer-id on-success _response]]
    (let [highlight-id (some-> payer-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-updated :payers highlight-id)))
       :dispatch-n [[:user-expenses/fetch-payers]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-payer-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update payer" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-payer
  common-interceptors
  (fn [{:keys [db]} [payer-id]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :delete
                    :uri (str endpoints/payers-endpoint "/" payer-id)
                    :admin-uri (str endpoints/admin-payers-endpoint "/" payer-id)
                    :on-success [:user-expenses/delete-payer-success]
                    :on-failure [:user-expenses/delete-payer-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-payer-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-payers]}))

(rf/reg-event-db
  :user-expenses/delete-payer-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete payer" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))