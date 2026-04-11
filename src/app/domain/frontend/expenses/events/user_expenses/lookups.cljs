(ns app.domain.frontend.expenses.events.user-expenses.lookups
  "Lookup data events - suppliers, payers, and uploads."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.shared.pagination :as pagination]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.filter-serialization :as filter-serialization]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- current-list-page-params
  [db entity-key default-limit]
  (let [per-page (paths/resolved-list-per-page db entity-key default-limit)
        current-page (paths/resolved-list-current-page db entity-key)
        sort-config (or (get-in db [:ui :lists entity-key :sort]) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))
        filters (filter-serialization/flatten-ui-filters (or (get-in db [:ui :lists entity-key :filters]) {}))]
    (cond-> (merge {:limit per-page
                    :offset (* (max 0 (dec current-page)) per-page)}
              filters)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

(rf/reg-event-fx
  :user-expenses/refresh-suppliers-list
  common-interceptors
  (fn [{:keys [db]} _]
    {:dispatch [:user-expenses/fetch-suppliers (current-list-page-params db :suppliers pagination/default-page-size)]}))

(rf/reg-event-fx
  :user-expenses/refresh-payers-list
  common-interceptors
  (fn [{:keys [db]} _]
    (let [params (if (= :server (get-in db (paths/list-pagination-mode :payers)))
                   (current-list-page-params db :payers 200)
                   {:limit 200 :offset 0})]
      {:dispatch [:user-expenses/fetch-payers params]})))

(rf/reg-event-fx
  :user-expenses/refresh-payer-types-list
  common-interceptors
  (fn [{:keys [db]} _]
    (let [params (if (= :server (get-in db (paths/list-pagination-mode :payer-types)))
                   (current-list-page-params db :payer-types 200)
                   {:limit 200 :offset 0})]
      {:dispatch [:user-expenses/fetch-payer-types params]})))

;; ---------------------------------------------------------------------------
;; Suppliers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-suppliers
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [params* (if (map? params) params {})]
      {:db (-> db
             (assoc-in [:user-expenses :suppliers :loading?] true)
             (assoc-in [:user-expenses :suppliers :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/suppliers-endpoint

                      :params (when (seq params*) params*)
                      :on-success [:user-expenses/fetch-suppliers-success]
                      :on-failure [:user-expenses/fetch-suppliers-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-suppliers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [suppliers (vec (or (:data response) []))
          total (or (:total response) (count suppliers))]
      {:db (-> db
             (assoc-in [:user-expenses :suppliers :data] suppliers)
             (assoc-in [:user-expenses :suppliers :items] suppliers)
             (assoc-in [:user-expenses :suppliers :loading?] false)
             (assoc-in [:user-expenses :suppliers :error] nil)
             (assoc-in (paths/list-total-items :suppliers) total))
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

                    :params (when (map? params) params)
                    :on-success [:user-expenses/fetch-payers-success]
                    :on-failure [:user-expenses/fetch-payers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-payers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [payers (vec (or (:data response) []))
          total (or (:total response) (count payers))
          user-payer-id (some-> (:user_payer_id response) str)]
      {:db (cond-> (-> db
                     (assoc-in [:user-expenses :payers :data] payers)
                     (assoc-in [:user-expenses :payers :items] payers)
                     (assoc-in [:user-expenses :payers :loading?] false)
                     (assoc-in [:user-expenses :payers :error] nil)
                     (assoc-in (paths/list-total-items :payers) total))
             user-payer-id (assoc-in [:user-expenses :payers :user-payer-id] user-payer-id))
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

                    :params (when (map? params) params)
                    :on-success [:user-expenses/fetch-payer-types-success]
                    :on-failure [:user-expenses/fetch-payer-types-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-payer-types-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [payer-types (vec (or (:data response) []))
          total (or (:total response) (count payer-types))]
      {:db (-> db
             (assoc-in [:user-expenses :payer-types :data] payer-types)
             (assoc-in [:user-expenses :payer-types :items] payer-types)
             (assoc-in [:user-expenses :payer-types :loading?] false)
             (assoc-in [:user-expenses :payer-types :error] nil)
             (assoc-in (paths/list-total-items :payer-types) total))
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
   (form-data-with-file file nil nil nil))
  ([file payer-id]
   (form-data-with-file file payer-id nil nil))
  ([file payer-id expense-category-id notes]
   (let [form-data (js/FormData.)
         payer-id* (some-> payer-id str str/trim not-empty)
         category-id* (some-> expense-category-id str str/trim not-empty)
         notes* (some-> notes str str/trim not-empty)]
     (.append form-data "file" file)
     (when payer-id*
       (.append form-data "payer_id" payer-id*))
     (when category-id*
       (.append form-data "expense_category_id" category-id*))
     (when notes*
       (.append form-data "notes" notes*))
     form-data)))

(rf/reg-event-db
  :user-expenses/set-upload-payer-id
  common-interceptors
  (fn [db [payer-id]]
    (let [payer-id* (some-> payer-id str str/trim not-empty)]
      (assoc-in db [:user-expenses :upload :payer-id] payer-id*))))

(defn- ^:private receipt-from-response
  [response]
  (:data response))

(defn- ^:private ocr-receipt-xhrio
  "Build a request to queue OCR for a single receipt (POST /expenses/receipts/:id/ocr).

  We trigger this automatically after upload completes.
  The backend processes OCR asynchronously and responds 202."
  [db receipt-id]
  (x/xhrio db
    {:method :post
     :uri (str endpoints/receipts-endpoint "/" receipt-id "/ocr")
     ;; Reuse the existing OCR success/failure handlers so the receipts list refreshes.
     :on-success [:user-expenses/ocr-receipt-success receipt-id]
     :on-failure [:user-expenses/ocr-receipt-failure receipt-id]}))

(rf/reg-event-fx
  :user-expenses/upload-receipt
  common-interceptors
  (fn [{:keys [db]} [file]]
    (let [payer-id (get-in db [:user-expenses :upload :payer-id])
          settings (get-in db [:user-expenses :settings :data])
          category-id (or (get-in db [:user-expenses :upload :expense-category-id])
                        (:default-expense-category-id settings))
          notes (or (get-in db [:user-expenses :upload :notes])
                  (:default-note settings))
          form-data (form-data-with-file file payer-id category-id notes)]
      {:db (-> db
             (assoc-in [:user-expenses :upload :batch] nil)
             (assoc-in [:user-expenses :upload :loading?] true)
             (assoc-in [:user-expenses :upload :error] nil)
             (assoc-in [:user-expenses :upload :notice] nil))
       :http-xhrio (x/xhrio db
                     {:method :post
                      :uri endpoints/upload-endpoint

                      :body form-data
                      :format {:write identity :content-type false}
                      :on-success [:user-expenses/upload-receipt-success]
                      :on-failure [:user-expenses/upload-receipt-failure]})})))

(rf/reg-event-fx
  :user-expenses/upload-receipt-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [receipt (receipt-from-response response)
          duplicate? (true? (:duplicate? response))
          receipt-id (:id receipt)
          receipt-id-str (some-> receipt-id str)
          filename (or (:original-filename receipt)
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
        (assoc :http-xhrio (ocr-receipt-xhrio db' (str receipt-id)))))))

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
        category-id (get-in db [:user-expenses :upload :batch :expense-category-id])
        notes (get-in db [:user-expenses :upload :batch :notes])
        form-data (form-data-with-file file payer-id category-id notes)]
    {:db (-> db
           (assoc-in [:user-expenses :upload :loading?] true)
           (assoc-in [:user-expenses :upload :batch :current] filename))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/upload-endpoint

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
          payer-id (get-in db [:user-expenses :upload :payer-id])
          settings (get-in db [:user-expenses :settings :data])
          category-id (or (get-in db [:user-expenses :upload :expense-category-id])
                        (:default-expense-category-id settings))
          notes (or (get-in db [:user-expenses :upload :notes])
                  (:default-note settings))]
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
                                                       :expense-category-id category-id
                                                       :notes notes
                                                       ;; Track created receipt IDs so we can queue OCR after the last upload.
                                                       :receipt-ids []}))
          files)
        {}))))

(rf/reg-event-fx
  :user-expenses/upload-receipts-success
  common-interceptors
  (fn [{:keys [db]} [remaining response]]
    (let [receipt (receipt-from-response response)
          duplicate? (true? (:duplicate? response))
          receipt-id (:id receipt)
          receipt-id-str (some-> receipt-id str)
          filename (or (get-in db [:user-expenses :upload :batch :current])
                     (:original-filename receipt)
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
          ;; Queue OCR for each uploaded receipt.
          (assoc :fx (mapv (fn [rid] [:http-xhrio (ocr-receipt-xhrio db' rid)]) all-ids)))))))

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
            (assoc :fx (mapv (fn [rid] [:http-xhrio (ocr-receipt-xhrio db' rid)]) all-ids))))))))
