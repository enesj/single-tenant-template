(ns app.domain.frontend.expenses.events.user-expenses.lookups
  "Lookup data events - suppliers, payers, and uploads."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Suppliers
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :user-expenses/set-suppliers-include-archived
  common-interceptors
  (fn [db [include-archived?]]
    (assoc-in db [:user-expenses :suppliers :include-archived?] (boolean include-archived?))))

(rf/reg-event-fx
  :user-expenses/fetch-suppliers
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [stored-include-archived? (true? (get-in db [:user-expenses :suppliers :include-archived?]))
          params* (if (map? params) params {})
          include-archived? (if (contains? params* :include_archived)
                              (true? (:include_archived params*))
                              stored-include-archived?)
          request-params (cond-> (dissoc params* :include_archived)
                           include-archived? (assoc :include_archived true))]
      {:db (-> db
             (assoc-in [:user-expenses :suppliers :loading?] true)
             (assoc-in [:user-expenses :suppliers :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/suppliers-endpoint
                      :admin-uri endpoints/admin-suppliers-endpoint
                      :params (when (seq request-params) request-params)
                      :on-success [:user-expenses/fetch-suppliers-success]
                      :on-failure [:user-expenses/fetch-suppliers-failure]})})))

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
  (fn [{:keys [db]} [params]]
    {:db (-> db
           (assoc-in [:user-expenses :payers :loading?] true)
           (assoc-in [:user-expenses :payers :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/payers-endpoint
                    :admin-uri endpoints/admin-payers-endpoint
                    :params (when (map? params) params)
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
;; Payer Types
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-payer-types
  common-interceptors
  (fn [{:keys [db]} [params]]
    {:db (-> db
           (assoc-in [:user-expenses :payer-types :loading?] true)
           (assoc-in [:user-expenses :payer-types :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/payer-types-endpoint
                    :admin-uri endpoints/admin-payer-types-endpoint
                    :params (when (map? params) params)
                    :on-success [:user-expenses/fetch-payer-types-success]
                    :on-failure [:user-expenses/fetch-payer-types-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-payer-types-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [payer-types (or (:data response) (:payer-types response) response)]
      {:db (-> db
             (assoc-in [:user-expenses :payer-types :data] payer-types)
             (assoc-in [:user-expenses :payer-types :items] payer-types)
             (assoc-in [:user-expenses :payer-types :loading?] false)
             (assoc-in [:user-expenses :payer-types :error] nil))
       :dispatch [::expenses-sync/sync-payer-types payer-types]})))

(rf/reg-event-db
  :user-expenses/fetch-payer-types-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch payer types" {:error error})
    (-> db
      (assoc-in [:user-expenses :payer-types :loading?] false)
      (assoc-in [:user-expenses :payer-types :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Upload (image handling)
;; ---------------------------------------------------------------------------

(defn- ^:private form-data-with-file
  ([file]
   (form-data-with-file file nil))
  ([file payer-id]
   (let [form-data (js/FormData.)
         payer-id* (some-> payer-id str str/trim not-empty)]
     (.append form-data "file" file)
     (when payer-id*
       (.append form-data "payer_id" payer-id*))
     form-data)))

(rf/reg-event-db
  :user-expenses/set-upload-payer-id
  common-interceptors
  (fn [db [payer-id]]
    (let [payer-id* (some-> payer-id str str/trim not-empty)]
      (assoc-in db [:user-expenses :upload :payer-id] payer-id*))))

(defn- ^:private receipt-from-response
  [response]
  (or (:data response)
    (:receipt response)
    (get-in response [:data :receipt])))

(defn- ^:private ocr-receipts-xhrio
  "Build an OCR batch request (POST /expenses/receipts/ocr).

  We trigger this automatically after upload completes.
  The backend processes OCR asynchronously and responds 202."
  [db receipt-ids]
  (x/xhrio db
    {:method :post
     :uri (str endpoints/receipts-endpoint "/ocr")
     :admin-uri (str endpoints/admin-receipts-endpoint "/ocr")
     :params {:receipt_ids (vec receipt-ids)}
     ;; Reuse the existing OCR success/failure handlers so the receipts list refreshes.
     :on-success [:user-expenses/ocr-selected-success receipt-ids]
     :on-failure [:user-expenses/ocr-selected-failure]}))

(rf/reg-event-fx
  :user-expenses/upload-receipt
  common-interceptors
  (fn [{:keys [db]} [file]]
    (let [payer-id (get-in db [:user-expenses :upload :payer-id])
          form-data (form-data-with-file file payer-id)]
      {:db (-> db
             (assoc-in [:user-expenses :upload :batch] nil)
             (assoc-in [:user-expenses :upload :loading?] true)
             (assoc-in [:user-expenses :upload :error] nil)
             (assoc-in [:user-expenses :upload :notice] nil))
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

(rf/reg-event-fx
  :user-expenses/upload-receipt-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [receipt (receipt-from-response response)
          duplicate? (true? (or (:duplicate? response) (:duplicate response)))
          receipt-id (:id receipt)
          receipt-id-str (some-> receipt-id str)
          filename (or (:original-filename receipt)
                     (:original_filename receipt)
                     "receipt")
          notice (when duplicate?
                   [(str "Already uploaded: " filename ". Using the existing receipt.")])
          items (if receipt-id-str
                  (->> (or (get-in db [:user-expenses :receipts :items]) [])
                    (remove (fn [r]
                              (= receipt-id-str (some-> (:id r) str))))
                    (cons receipt)
                    vec)
                  (vec (cons receipt (or (get-in db [:user-expenses :receipts :items]) []))))
          db' (-> db
                (assoc-in [:user-expenses :upload :loading?] false)
                (assoc-in [:user-expenses :upload :error] nil)
                (assoc-in [:user-expenses :upload :notice] notice)
                (assoc-in [:user-expenses :receipts :items] items)
                (assoc-in [:user-expenses :upload :last-receipt-id] receipt-id))]
      (cond-> {:db db'}
        (and receipt-id (not duplicate?))
        ;; Automatically queue OCR for this receipt.
        ;; Do this via :http-xhrio directly (instead of :dispatch) so tests that use
        ;; dispatch-sync remain deterministic.
        (assoc :http-xhrio (ocr-receipts-xhrio db' [(str receipt-id)]))))))

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
    payer-id (or (get-in db [:user-expenses :upload :batch :payer-id])
       (get-in db [:user-expenses :upload :payer-id]))
    form-data (form-data-with-file file payer-id)]
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
          total (count files)
          payer-id (get-in db [:user-expenses :upload :payer-id])]
      (if (pos? total)
        (upload-receipts-next-fx
          (-> db
            (assoc-in [:user-expenses :upload :loading?] true)
            (assoc-in [:user-expenses :upload :error] nil)
            (assoc-in [:user-expenses :upload :notice] nil)
            (assoc-in [:user-expenses :upload :batch] {:total total
                                                       :done 0
                                                       :failed 0
                                                       :current nil
                                                       :payer-id payer-id
                                                       ;; Track created receipt IDs so we can trigger one batch OCR
                                                       ;; request at the end (uses Mistral Batch API when >1).
                                                       :receipt-ids []}))
          files)
        {}))))

(rf/reg-event-fx
  :user-expenses/upload-receipts-success
  common-interceptors
  (fn [{:keys [db]} [remaining response]]
    (let [receipt (receipt-from-response response)
          duplicate? (true? (or (:duplicate? response) (:duplicate response)))
          receipt-id (:id receipt)
          receipt-id-str (some-> receipt-id str)
          filename (or (get-in db [:user-expenses :upload :batch :current])
                     (:original-filename receipt)
                     (:original_filename receipt)
                     "receipt")
          notice-msg (when duplicate?
                       (str "Already uploaded: " filename ". Using the existing receipt."))
          items (if receipt-id-str
                  (->> (or (get-in db [:user-expenses :receipts :items]) [])
                    (remove (fn [r]
                              (= receipt-id-str (some-> (:id r) str))))
                    (cons receipt)
                    vec)
                  (vec (cons receipt (or (get-in db [:user-expenses :receipts :items]) []))))
          db' (-> db
                (update-in [:user-expenses :upload :batch :done] (fnil inc 0))
                (cond-> (and receipt-id (not duplicate?))
                  (update-in [:user-expenses :upload :batch :receipt-ids] (fnil conj []) (str receipt-id)))
                (assoc-in [:user-expenses :receipts :items] items)
                (assoc-in [:user-expenses :upload :last-receipt-id] receipt-id)
                (cond-> notice-msg
                  (update-in [:user-expenses :upload :notice] (fnil conj []) notice-msg)))
          all-ids (get-in db' [:user-expenses :upload :batch :receipt-ids])]
      (if (seq remaining)
        (upload-receipts-next-fx db' remaining)
        (cond-> {:db (-> db'
                       (assoc-in [:user-expenses :upload :loading?] false)
                       (assoc-in [:user-expenses :upload :batch :current] nil))}
          (seq all-ids)
          ;; Trigger a single batch OCR request after the last upload completes.
          ;; This ensures we use the worker's Batch API path when multiple receipts
          ;; are uploaded at once.
          (assoc :http-xhrio (ocr-receipts-xhrio db' all-ids)))))))

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
        (let [all-ids (get-in db' [:user-expenses :upload :batch :receipt-ids])]
          (cond-> {:db (-> db'
                         (assoc-in [:user-expenses :upload :loading?] false)
                         (assoc-in [:user-expenses :upload :batch :current] nil))}
            ;; Even if some uploads failed, still OCR the receipts that *did* upload.
            (seq all-ids)
            (assoc :http-xhrio (ocr-receipts-xhrio db' all-ids))))))))

(rf/reg-event-db
  :user-expenses/clear-upload
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in [:user-expenses :upload] nil)
      (assoc-in [:user-expenses :receipts] nil))))