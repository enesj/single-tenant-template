(ns app.template.frontend.events.user-expenses.lookups
  (:require
    [app.admin.frontend.adapters.expenses :as admin-expenses-adapter]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.events.user-expenses.xhrio :as x]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Suppliers and payers for forms
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-suppliers
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:db (assoc-in db [:user-expenses :suppliers :loading?] true)
     :http-xhrio (x/xhrio db
                  {:method :get
                   :uri endpoints/suppliers-endpoint
                   :admin-uri endpoints/admin-suppliers-endpoint
                   :params (select-keys opts [:limit :offset])
                   :on-success [:user-expenses/fetch-suppliers-success]
                   :on-failure [:user-expenses/fetch-suppliers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-suppliers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [items (or (:data response) (:suppliers response) [])]
      {:db (-> db
             (assoc-in [:user-expenses :suppliers :loading?] false)
             (assoc-in [:user-expenses :suppliers :items] items))
       ;; Also mirror suppliers into the shared template entity store so that
       ;; FK columns like expenses.supplier_id can resolve labels via
       ;; list-view + select-options on the user-facing pages.
       :dispatch [::admin-expenses-adapter/sync-suppliers items]})))

(rf/reg-event-db
  :user-expenses/fetch-suppliers-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch suppliers" {:error error})
    (assoc-in db [:user-expenses :suppliers :loading?] false)))

(rf/reg-event-fx
  :user-expenses/fetch-payers
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:db (assoc-in db [:user-expenses :payers :loading?] true)
     :http-xhrio (x/xhrio db
                  {:method :get
                   :uri endpoints/payers-endpoint
                   :admin-uri endpoints/admin-payers-endpoint
                   :params (select-keys opts [:limit :offset])
                   :on-success [:user-expenses/fetch-payers-success]
                   :on-failure [:user-expenses/fetch-payers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-payers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [items (or (:data response) (:payers response) [])]
      {:db (-> db
             (assoc-in [:user-expenses :payers :loading?] false)
             (assoc-in [:user-expenses :payers :items] items))
       ;; Mirror payers into the shared template entity store so that
       ;; FK columns like expenses.payer_id can resolve labels via the
       ;; same vector-config + list-view pipeline as admin pages.
       :dispatch [::admin-expenses-adapter/sync-payers items]})))

(rf/reg-event-db
  :user-expenses/fetch-payers-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch payers" {:error error})
    (assoc-in db [:user-expenses :payers :loading?] false)))

;; ---------------------------------------------------------------------------
;; Upload receipt (placeholder)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/upload-receipt
  common-interceptors
  (fn [{:keys [db]} [_file]]
    ;; TODO: Implement actual file upload
    {:db (-> db
           (assoc-in [:user-expenses :upload :loading?] true)
           (assoc-in [:user-expenses :upload :error] nil))
     :dispatch-later [{:ms 2000
                       :dispatch [:user-expenses/upload-receipt-success {:id "placeholder"}]}]}))

(rf/reg-event-fx
  :user-expenses/upload-receipt-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :upload :loading?] false)
     :dispatch [:navigate-to "/expenses/list"]}))

#_(rf/reg-event-db
    :user-expenses/upload-receipt-failure
    common-interceptors
    (fn [db [error]]
      (-> db
        (assoc-in [:user-expenses :upload :loading?] false)
        (assoc-in [:user-expenses :upload :error] (http/extract-error-message error)))))

