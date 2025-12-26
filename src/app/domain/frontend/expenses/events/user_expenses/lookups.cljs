(ns app.domain.frontend.expenses.events.user-expenses.lookups
  "Lookup data events - suppliers, payers, and uploads."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Suppliers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-suppliers
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:user-expenses :suppliers :loading?] true)
           (assoc-in [:user-expenses :suppliers :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/suppliers-endpoint
                    :admin-uri endpoints/admin-suppliers-endpoint
                    :on-success [:user-expenses/fetch-suppliers-success]
                    :on-failure [:user-expenses/fetch-suppliers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-suppliers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [suppliers (or (:data response) (:suppliers response) response)]
      {:db (-> db
             (assoc-in [:user-expenses :suppliers :data] suppliers)
             (assoc-in [:user-expenses :suppliers :items] suppliers)
             (assoc-in [:user-expenses :suppliers :loading?] false)
             (assoc-in [:user-expenses :suppliers :error] nil))
       :dispatch [::expenses-sync/sync-suppliers suppliers]})))

(rf/reg-event-db
  :user-expenses/fetch-suppliers-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch suppliers" {:error error})
    (-> db
      (assoc-in [:user-expenses :suppliers :loading?] false)
      (assoc-in [:user-expenses :suppliers :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Payers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-payers
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:user-expenses :payers :loading?] true)
           (assoc-in [:user-expenses :payers :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/payers-endpoint
                    :admin-uri endpoints/admin-payers-endpoint
                    :on-success [:user-expenses/fetch-payers-success]
                    :on-failure [:user-expenses/fetch-payers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-payers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [payers (or (:data response) (:payers response) response)]
      {:db (-> db
             (assoc-in [:user-expenses :payers :data] payers)
             (assoc-in [:user-expenses :payers :items] payers)
             (assoc-in [:user-expenses :payers :loading?] false)
             (assoc-in [:user-expenses :payers :error] nil))
       :dispatch [::expenses-sync/sync-payers payers]})))

(rf/reg-event-db
  :user-expenses/fetch-payers-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch payers" {:error error})
    (-> db
      (assoc-in [:user-expenses :payers :loading?] false)
      (assoc-in [:user-expenses :payers :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Upload (image handling)
;; ---------------------------------------------------------------------------

(defn- ^:private form-data-with-file
  [file]
  (let [form-data (js/FormData.)]
    (.append form-data "file" file)
    form-data))

(rf/reg-event-fx
  :user-expenses/upload-receipt
  common-interceptors
  (fn [{:keys [db]} [_ file]]
    (let [form-data (form-data-with-file file)]
      {:db (-> db
             (assoc-in [:user-expenses :upload :loading?] true)
             (assoc-in [:user-expenses :upload :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :post
                      :uri endpoints/upload-endpoint
                      :admin-uri endpoints/admin-upload-endpoint
                      :body form-data
                      :format {:write identity :content-type false}
                      :on-success [:user-expenses/upload-receipt-success]
                      :on-failure [:user-expenses/upload-receipt-failure]})})))

;; Backwards-compatible alias (older pages called it "upload-image")
(rf/reg-event-fx
  :user-expenses/upload-image
  common-interceptors
  (fn [_ [_ file]]
    {:dispatch [:user-expenses/upload-receipt file]}))

(rf/reg-event-db
  :user-expenses/upload-receipt-success
  common-interceptors
  (fn [db [_ response]]
    (let [receipt (or (:data response)
                    (:receipt response)
                    (get-in response [:data :receipt]))]
      (-> db
        (assoc-in [:user-expenses :upload :loading?] false)
        (assoc-in [:user-expenses :upload :error] nil)
        (update-in [:user-expenses :receipts :items] (fnil #(vec (cons receipt %)) []))
        (assoc-in [:user-expenses :upload :last-receipt-id] (:id receipt))))))

(rf/reg-event-db
  :user-expenses/upload-receipt-failure
  common-interceptors
  (fn [db [_ error]]
    (log/warn "Failed to upload receipt" {:error error})
    (-> db
      (assoc-in [:user-expenses :upload :loading?] false)
      (assoc-in [:user-expenses :upload :error]
        (or (http/extract-error-message error)
          "Upload failed. Please sign in and try again.")))))

(rf/reg-event-db
  :user-expenses/clear-upload
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in [:user-expenses :upload] nil)
      (assoc-in [:user-expenses :receipts] nil))))