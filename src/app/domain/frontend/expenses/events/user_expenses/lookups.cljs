(ns app.domain.frontend.expenses.events.user-expenses.lookups
  "Lookup data events - suppliers, payers, and uploads."
  (:require
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

(rf/reg-event-db
  :user-expenses/fetch-suppliers-success
  common-interceptors
  (fn [db [response]]
    (let [suppliers (or (:data response) (:suppliers response) response)]
      (-> db
        (assoc-in [:user-expenses :suppliers :data] suppliers)
        (assoc-in [:user-expenses :suppliers :loading?] false)
        (assoc-in [:user-expenses :suppliers :error] nil)))))

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

(rf/reg-event-db
  :user-expenses/fetch-payers-success
  common-interceptors
  (fn [db [response]]
    (let [payers (or (:data response) (:payers response) response)]
      (-> db
        (assoc-in [:user-expenses :payers :data] payers)
        (assoc-in [:user-expenses :payers :loading?] false)
        (assoc-in [:user-expenses :payers :error] nil)))))

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

(rf/reg-event-fx
  :user-expenses/upload-image
  common-interceptors
  (fn [{:keys [db]} [file]]
    (let [form-data (js/FormData.)]
      (.append form-data "file" file)
      {:db (-> db
             (assoc-in [:user-expenses :upload :loading?] true)
             (assoc-in [:user-expenses :upload :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :post
                      :uri endpoints/upload-endpoint
                      :admin-uri endpoints/admin-upload-endpoint
                      :body form-data
                      :on-success [:user-expenses/upload-image-success]
                      :on-failure [:user-expenses/upload-image-failure]})})))

(rf/reg-event-db
  :user-expenses/upload-image-success
  common-interceptors
  (fn [db [response]]
    (let [url (or (:url response) (get-in response [:data :url]))]
      (-> db
        (assoc-in [:user-expenses :upload :url] url)
        (assoc-in [:user-expenses :upload :loading?] false)
        (assoc-in [:user-expenses :upload :error] nil)))))

(rf/reg-event-db
  :user-expenses/upload-image-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to upload image" {:error error})
    (-> db
      (assoc-in [:user-expenses :upload :loading?] false)
      (assoc-in [:user-expenses :upload :error] (http/extract-error-message error)))))

(rf/reg-event-db
  :user-expenses/clear-upload
  common-interceptors
  (fn [db _]
    (assoc-in db [:user-expenses :upload] nil)))
