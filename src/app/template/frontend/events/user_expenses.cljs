(ns app.template.frontend.events.user-expenses
  "User-facing expense dashboard events.
   Fetches expense summary, recent expenses, and aggregates for the user scope."
  (:require
    [app.admin.frontend.adapters.expenses :as admin-expenses-adapter]
    [app.template.frontend.api :as api]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def summary-endpoint (api/versioned-endpoint "/expenses/summary"))
(def list-endpoint (api/versioned-endpoint "/expenses"))
(def by-month-endpoint (api/versioned-endpoint "/expenses/by-month"))
(def by-supplier-endpoint (api/versioned-endpoint "/expenses/by-supplier"))

;; ---------------------------------------------------------------------------
;; Dashboard bootstrap
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/init-dashboard
  common-interceptors
  (fn [{:keys [_db]} _]
    {:dispatch-n [[:user-expenses/fetch-summary]
                  [:user-expenses/fetch-by-month {:months-back 6}]
                  [:user-expenses/fetch-recent {:limit 5}]]}))

;; ---------------------------------------------------------------------------
;; Summary
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-summary
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:user-expenses :summary :loading?] true)
           (assoc-in [:user-expenses :summary :error] nil))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri summary-endpoint
                    :on-success [:user-expenses/fetch-summary-success]
                    :on-failure [:user-expenses/fetch-summary-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-summary-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :summary :loading?] false)
      (assoc-in [:user-expenses :summary :error] nil)
      (assoc-in [:user-expenses :summary :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-summary-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch user expense summary" {:error error})
    (-> db
      (assoc-in [:user-expenses :summary :loading?] false)
      (assoc-in [:user-expenses :summary :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Recent expenses (list)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-recent
  common-interceptors
  (fn [{:keys [db]} [_ {:keys [limit offset]}]]
    (let [limit* (or limit 5)
          offset* (or offset 0)]
      {:db (-> db
             (assoc-in [:user-expenses :recent :loading?] true)
             (assoc-in [:user-expenses :recent :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri list-endpoint
                      :params {:limit limit* :offset offset*}
                      :on-success [:user-expenses/fetch-recent-success]
                      :on-failure [:user-expenses/fetch-recent-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-recent-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [{:keys [data total limit offset]} response]
      {:db (-> db
             (assoc-in [:user-expenses :recent :loading?] false)
             (assoc-in [:user-expenses :recent :error] nil)
             (assoc-in [:user-expenses :recent :items] (vec data))
             (assoc-in [:user-expenses :recent :total] total)
             (assoc-in [:user-expenses :recent :limit] limit)
             (assoc-in [:user-expenses :recent :offset] offset))
       ;; Also sync into the shared template entity store so both admin and
       ;; user table views can depend on the same data source.
       :dispatch [::admin-expenses-adapter/sync-expenses data]})))

(rf/reg-event-db
  :user-expenses/fetch-recent-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch recent user expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :recent :loading?] false)
      (assoc-in [:user-expenses :recent :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Spending by month (for dashboard chart/stats)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-by-month
  common-interceptors
  (fn [{:keys [db]} [_ {:keys [months-back]}]]
    (let [months* (or months-back 6)]
      {:db (-> db
             (assoc-in [:user-expenses :by-month :loading?] true)
             (assoc-in [:user-expenses :by-month :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri by-month-endpoint
                      :params {:months_back months*}
                      :on-success [:user-expenses/fetch-by-month-success]
                      :on-failure [:user-expenses/fetch-by-month-failure]})})))

(rf/reg-event-db
  :user-expenses/fetch-by-month-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :by-month :loading?] false)
      (assoc-in [:user-expenses :by-month :error] nil)
      (assoc-in [:user-expenses :by-month :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-by-month-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch spending by month" {:error error})
    (-> db
      (assoc-in [:user-expenses :by-month :loading?] false)
      (assoc-in [:user-expenses :by-month :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Spending by supplier (future use / leaderboard)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-by-supplier
  common-interceptors
  (fn [{:keys [db]} [_ {:keys [limit from to]}]]
    (let [limit* (or limit 5)]
      {:db (-> db
             (assoc-in [:user-expenses :by-supplier :loading?] true)
             (assoc-in [:user-expenses :by-supplier :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri by-supplier-endpoint
                      :params (cond-> {:limit limit*}
                                from (assoc :from from)
                                to (assoc :to to))
                      :on-success [:user-expenses/fetch-by-supplier-success]
                      :on-failure [:user-expenses/fetch-by-supplier-failure]})})))

(rf/reg-event-db
  :user-expenses/fetch-by-supplier-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :by-supplier :loading?] false)
      (assoc-in [:user-expenses :by-supplier :error] nil)
      (assoc-in [:user-expenses :by-supplier :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-by-supplier-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch spending by supplier" {:error error})
    (-> db
      (assoc-in [:user-expenses :by-supplier :loading?] false)
      (assoc-in [:user-expenses :by-supplier :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Single expense detail
;; ---------------------------------------------------------------------------

(def expense-detail-endpoint (api/versioned-endpoint "/expenses"))

(rf/reg-event-fx
  :user-expenses/fetch-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (-> db
           (assoc-in [:user-expenses :current-expense :loading?] true)
           (assoc-in [:user-expenses :current-expense :error] nil))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri (str expense-detail-endpoint "/" expense-id)
                    :on-success [:user-expenses/fetch-expense-success]
                    :on-failure [:user-expenses/fetch-expense-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-expense-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :current-expense :loading?] false)
      (assoc-in [:user-expenses :current-expense :error] nil)
      (assoc-in [:user-expenses :current-expense :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch expense detail" {:error error})
    (-> db
      (assoc-in [:user-expenses :current-expense :loading?] false)
      (assoc-in [:user-expenses :current-expense :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Create expense
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/create-expense
  common-interceptors
  (fn [{:keys [db]} [expense-data]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri list-endpoint
                    :params expense-data
                    :on-success [:user-expenses/create-expense-success]
                    :on-failure [:user-expenses/create-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-expense-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [expense-id (get-in response [:data :id])]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil))
       :dispatch-n [[:user-expenses/fetch-recent {:limit 5}]
                    [:navigate-to (str "/expenses/" expense-id)]]})))

(rf/reg-event-db
  :user-expenses/create-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Delete expense
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (assoc-in db [:user-expenses :form :loading?] true)
     :http-xhrio (http/api-request
                   {:method :delete
                    :uri (str list-endpoint "/" expense-id)
                    :on-success [:user-expenses/delete-expense-success]
                    :on-failure [:user-expenses/delete-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-expense-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch-n [[:user-expenses/fetch-recent {:limit 25}]
                  [:navigate-to "/expenses/list"]]}))

(rf/reg-event-db
  :user-expenses/delete-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Post expense (mark as posted)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/post-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (assoc-in db [:user-expenses :form :loading?] true)
     :http-xhrio (http/api-request
                   {:method :put
                    :uri (str list-endpoint "/" expense-id)
                    :params {:is_posted true}
                    :on-success [:user-expenses/post-expense-success expense-id]
                    :on-failure [:user-expenses/post-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/post-expense-success
  common-interceptors
  (fn [{:keys [db]} [expense-id _response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-expense expense-id]}))

(rf/reg-event-db
  :user-expenses/post-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to post expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Suppliers and payers for forms
;; ---------------------------------------------------------------------------

(def suppliers-endpoint (api/versioned-endpoint "/expenses/suppliers"))
(def payers-endpoint (api/versioned-endpoint "/expenses/payers"))

(rf/reg-event-fx
  :user-expenses/fetch-suppliers
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:db (assoc-in db [:user-expenses :suppliers :loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri suppliers-endpoint
                    :params (select-keys opts [:limit :offset])
                    :on-success [:user-expenses/fetch-suppliers-success]
                    :on-failure [:user-expenses/fetch-suppliers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-suppliers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [items (or (:data response) [])]
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
     :http-xhrio (http/api-request
                   {:method :get
                    :uri payers-endpoint
                    :params (select-keys opts [:limit :offset])
                    :on-success [:user-expenses/fetch-payers-success]
                    :on-failure [:user-expenses/fetch-payers-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-payers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [items (or (:data response) [])]
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
     :dispatch [:navigate-to "/expenses/new"]}))

(rf/reg-event-db
  :user-expenses/upload-receipt-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :upload :loading?] false)
      (assoc-in [:user-expenses :upload :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Settings
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-settings
  common-interceptors
  (fn [{:keys [db]} _]
    ;; TODO: Implement actual settings fetch
    {:db (-> db
           (assoc-in [:user-expenses :settings :loading?] true)
           (assoc-in [:user-expenses :settings :data] {:default_currency "BAM"
                                                       :notifications_enabled true})
           (assoc-in [:user-expenses :settings :loading?] false))}))

(rf/reg-event-fx
  :user-expenses/save-settings
  common-interceptors
  (fn [{:keys [db]} [settings-data]]
    ;; TODO: Implement actual settings save
    {:db (-> db
           (assoc-in [:user-expenses :settings :saving?] true)
           (assoc-in [:user-expenses :settings :data] settings-data))
     :dispatch-later [{:ms 500
                       :dispatch [:user-expenses/save-settings-success settings-data]}]}))

(rf/reg-event-db
  :user-expenses/save-settings-success
  common-interceptors
  (fn [db [_settings]]
    (assoc-in db [:user-expenses :settings :saving?] false)))

;; ---------------------------------------------------------------------------
;; Export (placeholder)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/export
  common-interceptors
  (fn [_cofx [opts]]
    (log/info "Export requested" opts)
    ;; TODO: Implement actual export
    {}))

;; ---------------------------------------------------------------------------
;; Delete all (placeholder)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-all
  common-interceptors
  (fn [_cofx _]
    (log/info "Delete all requested")
    ;; TODO: Implement actual delete all
    {}))
