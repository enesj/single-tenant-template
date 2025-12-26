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

(defn- ^:private receipt-from-response
  [response]
  (or (:data response)
    (:receipt response)
    (get-in response [:data :receipt])))

(rf/reg-event-fx
  :user-expenses/upload-receipt
  common-interceptors
  (fn [{:keys [db]} [file]]
    (let [form-data (form-data-with-file file)]
      {:db (-> db
             (assoc-in [:user-expenses :upload :batch] nil)
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
  (fn [_ [file]]
    {:dispatch [:user-expenses/upload-receipt file]}))

(rf/reg-event-db
  :user-expenses/upload-receipt-success
  common-interceptors
  (fn [db [response]]
    (let [receipt (receipt-from-response response)]
      (-> db
        (assoc-in [:user-expenses :upload :loading?] false)
        (assoc-in [:user-expenses :upload :error] nil)
        (update-in [:user-expenses :receipts :items] (fnil #(vec (cons receipt %)) []))
        (assoc-in [:user-expenses :upload :last-receipt-id] (:id receipt))))))

(rf/reg-event-db
  :user-expenses/upload-receipt-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to upload receipt" {:error error})
    (-> db
      (assoc-in [:user-expenses :upload :loading?] false)
      (assoc-in [:user-expenses :upload :error]
        (or (http/extract-error-message error)
          "Upload failed. Please sign in and try again.")))))

;; Batch upload (multiple receipts)

(defn- ^:private upload-receipts-next-fx
  [db files]
  (let [file (first files)
        remaining (vec (rest files))
        filename (or (.-name file) "receipt")
        form-data (form-data-with-file file)]
    {:db (-> db
           (assoc-in [:user-expenses :upload :loading?] true)
           (assoc-in [:user-expenses :upload :batch :current] filename))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/upload-endpoint
                    :admin-uri endpoints/admin-upload-endpoint
                    :body form-data
                    :format {:write identity :content-type false}
                    :on-success [:user-expenses/upload-receipts-success remaining]
                    :on-failure [:user-expenses/upload-receipts-failure remaining filename]})}))

(rf/reg-event-fx
  :user-expenses/upload-receipts
  common-interceptors
  (fn [{:keys [db]} [files]]
    (let [files (->> files (remove nil?) vec)
          total (count files)]
      (if (pos? total)
        (upload-receipts-next-fx
          (-> db
            (assoc-in [:user-expenses :upload :loading?] true)
            (assoc-in [:user-expenses :upload :error] nil)
            (assoc-in [:user-expenses :upload :batch] {:total total
                                                       :done 0
                                                       :failed 0
                                                       :current nil}))
          files)
        {}))))

(rf/reg-event-fx
  :user-expenses/upload-receipts-success
  common-interceptors
  (fn [{:keys [db]} [remaining response]]
    (let [receipt (receipt-from-response response)
          db' (-> db
                (update-in [:user-expenses :upload :batch :done] (fnil inc 0))
                (update-in [:user-expenses :receipts :items] (fnil #(vec (cons receipt %)) []))
                (assoc-in [:user-expenses :upload :last-receipt-id] (:id receipt)))]
      (if (seq remaining)
        (upload-receipts-next-fx db' remaining)
        {:db (-> db'
               (assoc-in [:user-expenses :upload :loading?] false)
               (assoc-in [:user-expenses :upload :batch :current] nil))}))))

(rf/reg-event-fx
  :user-expenses/upload-receipts-failure
  common-interceptors
  (fn [{:keys [db]} [remaining filename error]]
    (log/warn "Failed to upload receipt" {:file filename :error error})
    (let [msg (or (http/extract-error-message error)
                "Upload failed. Please sign in and try again.")
          db' (-> db
                (update-in [:user-expenses :upload :batch :failed] (fnil inc 0))
                (assoc-in [:user-expenses :upload :batch :current] nil)
                (assoc-in [:user-expenses :upload :error] (str filename ": " msg)))]
      (if (seq remaining)
        (upload-receipts-next-fx db' remaining)
        {:db (-> db'
               (assoc-in [:user-expenses :upload :loading?] false)
               (assoc-in [:user-expenses :upload :batch :current] nil))}))))

(rf/reg-event-db
  :user-expenses/clear-upload
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in [:user-expenses :upload] nil)
      (assoc-in [:user-expenses :receipts] nil))))