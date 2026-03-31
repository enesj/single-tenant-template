(ns app.admin.frontend.events.audit
  "Enhanced admin audit logs events with comprehensive audit management.
   
   This namespace handles:
   - HTTP loading of audit logs
   - Filtering, pagination, sorting
   - Batch operations (delete, export)
   - Modal management
   
   Data normalization and template sync are in app.admin.frontend.adapters.audit"
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.adapters.audit :as audit-adapter]
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]

    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Load Audit Logs
;; ============================================================================

(rf/reg-event-fx
  :admin/load-audit-logs
  (fn [{:keys [db]} [_ {:keys [filters pagination sort] :as _params}]]
    (let [entity-key :audit-logs
          parse-pos-int (fn [v]
                          (cond
                            (number? v) (when (pos? v) (long v))
                            (string? v) (let [n (js/parseInt v 10)]
                                          (when (and (number? n) (not (js/isNaN n)) (pos? n))
                                            (long n)))
                            :else nil))
          parse-non-neg-int (fn [v]
                              (cond
                                (number? v) (when (>= v 0) (long v))
                                (string? v) (let [n (js/parseInt v 10)]
                                              (when (and (number? n) (not (js/isNaN n)) (>= n 0))
                                                (long n)))
                                :else nil))
          ;; Read template system pagination first to avoid divergence
          template-per-page (paths/resolved-list-per-page db entity-key 20)
          template-page (paths/resolved-list-current-page db entity-key)

          current-filters (merge (get-in db [:admin :audit :filters] {})
                            (get-in db (paths/list-filters entity-key) {}))
          ;; Preserve any legacy pagination metadata, but never let stale page/limit
          ;; values override the canonical template list UI state.
          admin-pagination (get-in db [:admin :audit :pagination] {})
          current-pagination (merge (dissoc admin-pagination :page :current-page :per-page :limit :offset)
                               {:page template-page
                                :current-page template-page
                                :per-page template-per-page})
          ;; Align default sort with adapter default (:created-at)
          template-sort (paths/resolved-list-sort-config db entity-key)
          current-sort (merge {:field :created-at :direction :desc}
                         (get-in db [:admin :audit :sort] {})
                         template-sort)

          ;; Merge current state with any provided params
          final-filters (merge current-filters filters)
          final-pagination (merge current-pagination pagination)
          final-sort (merge current-sort sort)

          limit (or (parse-pos-int (:per-page pagination))
                  (parse-pos-int (:limit pagination))
                  (parse-pos-int (:per-page final-pagination))
                  template-per-page)
          page (or (parse-pos-int (:page pagination))
                 (parse-pos-int (:current-page pagination))
                 (parse-pos-int (:page final-pagination))
                 (parse-pos-int (:current-page final-pagination))
                 template-page)
          offset (or (parse-non-neg-int (:offset pagination))
                   (* (dec page) limit))
          resolved-pagination (assoc final-pagination
                                :page page
                                :current-page page
                                :per-page limit
                                :limit limit
                                :offset offset)
          request-sort (let [{:keys [field direction]} final-sort
                             order-dir (when (contains? #{:asc :desc "asc" "desc"} direction)
                                         (name (keyword direction)))
                             order-by (cond
                                        (keyword? field) (name field)
                                        (string? field) field
                                        (some? field) (str field)
                                        :else nil)]
                         (cond-> {}
                           (some? order-by) (assoc :order-by order-by)
                           (some? order-dir) (assoc :order-dir order-dir)))
          request-params (cond-> (merge {:limit limit
                                         :offset offset}
                                   request-sort)
                           (seq final-filters) (merge final-filters))]

      (log/info "AUDIT LOAD →" {:pagination resolved-pagination
                                :filters final-filters
                                :request-params request-params})

      (if (adapters.core/admin-token db)
        {:db (-> db
               (assoc-in [:admin :audit :loading?] true)
               (assoc-in [:admin :audit :error] nil)
               (assoc-in [:admin :audit :filters] final-filters)
               (assoc-in [:admin :audit :pagination] resolved-pagination)
               (assoc-in [:admin :audit :sort] final-sort)
               (assoc-in (conj (paths/entity-metadata :audit-logs) :loading?) true))
         :http-xhrio (admin-http/admin-get
                       {:uri "/admin/api/audit"
                        :params request-params
                        :on-success [::audit-logs-loaded]
                        :on-failure [::audit-logs-load-failed]})}
        {:db (-> db
               (assoc-in [:admin :audit :loading?] false)
               (assoc-in [:admin :audit :error] "Authentication required")
               (assoc-in (conj (paths/entity-metadata :audit-logs) :loading?) false)
               (assoc-in (conj (paths/entity-metadata :audit-logs) :error) "Authentication required"))}))))

;; HTTP success handler - syncs data to template store
(rf/reg-event-fx
  ::audit-logs-loaded
  (fn [{:keys [db]} [_ response]]
    (let [response* (if (object? response) (js->clj response :keywordize-keys true) response)
          raw-logs (get response* :logs [])
          total-items (or (:total response*)
                        (count raw-logs))
          metadata-path (paths/entity-metadata :audit-logs)]
      {:db (-> db
             (assoc-in (conj metadata-path :loading?) false)
             (assoc-in (conj metadata-path :error) nil)
             (assoc-in (paths/list-total-items :audit-logs) total-items))
       :dispatch-n [[::audit-adapter/sync-audit-logs-to-template raw-logs]
                    [:admin/audit-logs-loaded]
                    [:admin/fetch-unread-api-failures]]})))

;; HTTP failure handler
(rf/reg-event-fx
  ::audit-logs-load-failed
  (fn [{:keys [db]} [_ error]]
    (let [error-msg "Failed to load audit logs"
          path (paths/entity-metadata :audit-logs)]
      (log/error "Audit logs load failed:" error)
      {:db (-> db
             (assoc-in (conj path :loading?) false)
             (assoc-in (conj path :error) error-msg))
       :dispatch [:admin/audit-logs-load-failed error]})))

(rf/reg-event-db
  :admin/audit-logs-loaded
  (fn [db _]
    (log/info "Audit logs loaded successfully (adapter sync complete)")
    (-> db
      (assoc-in [:admin :audit :loading?] false)
      (assoc-in [:admin :audit :error] nil)
      ;; Remove legacy data storage to avoid confusion; table reads from template store
      (update-in [:admin :audit] (fn [m] (-> m (dissoc :data) (dissoc :total-count)))))))

(rf/reg-event-db
  :admin/audit-logs-load-failed
  (fn [db [_ error]]
    (let [msg (or (get-in error [:response :error]) "Failed to load audit logs")]
      (log/error "Failed to load audit logs:" msg)
      (-> db
        (assoc-in [:admin :audit :loading?] false)
        (assoc-in [:admin :audit :error] msg)))))

;; ============================================================================
;; Audit Filtering
;; ============================================================================

(rf/reg-event-fx
  :admin/apply-audit-filter
  (fn [{:keys [db]} [_ filter-key filter-value]]
    (log/info "Applying audit filter:" filter-key filter-value)
    (let [current-filters (get-in db [:admin :audit :filters] {})
          new-filters (assoc current-filters filter-key filter-value)]
      {:db (assoc-in db [:admin :audit :filters] new-filters)
       :dispatch [:admin/load-audit-logs {:filters new-filters}]})))

(rf/reg-event-fx
  :admin/remove-audit-filter
  (fn [{:keys [db]} [_ filter-key]]
    (log/info "Removing audit filter:" filter-key)
    (let [current-filters (get-in db [:admin :audit :filters] {})
          new-filters (dissoc current-filters filter-key)]
      {:db (assoc-in db [:admin :audit :filters] new-filters)
       :dispatch [:admin/load-audit-logs {:filters new-filters}]})))

(rf/reg-event-fx
  :admin/clear-audit-filters
  (fn [{:keys [db]} [_]]
    (log/info "Clearing all audit filters")
    {:db (assoc-in db [:admin :audit :filters] {})
     :dispatch [:admin/load-audit-logs {:filters {}}]}))

;; ============================================================================
;; Audit Log Details Modal
;; ============================================================================

(rf/reg-event-db
  :admin/show-audit-details
  (fn [db [_ audit-log]]
    (log/info "Showing audit details for:" (:id audit-log))
    (-> db
      (assoc-in [:admin :audit :details-modal :visible?] true)
      (assoc-in [:admin :audit :details-modal :audit-log] audit-log))))

(rf/reg-event-db
  :admin/hide-audit-details
  (fn [db [_]]
    (log/info "Hiding audit details modal")
    (-> db
      (assoc-in [:admin :audit :details-modal :visible?] false)
      (assoc-in [:admin :audit :details-modal :audit-log] nil))))

;; ============================================================================
;; Audit Log Deletion
;; ============================================================================

(rf/reg-event-fx
  :admin/delete-audit-log
  (fn [{:keys [db]} [_ audit-id]]
    (let [token (adapters.core/admin-token db)]
      (log/info "Deleting audit log:" audit-id)

      (if token
        {:db (assoc-in db [:admin :audit :deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             "/admin/api/audit/batch"
                      :body            (js/JSON.stringify (clj->js {:ids [(str audit-id)]}))
                      :headers         (cond-> {"Content-Type" "application/json"}
                                         token (assoc "x-admin-token" token))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :timeout         10000 ; 10 second timeout
                      :on-success      [:admin/audit-log-deleted audit-id]
                      :on-failure      [:admin/audit-log-delete-failed]}}
        {:db (assoc-in db [:admin :audit :error] "Authentication required")}))))

(rf/reg-event-fx
  :admin/audit-log-deleted
  (fn [{:keys [db]} [_ audit-id]]
    (log/info "Audit log deleted successfully:" audit-id)
    {:db (-> db
           (assoc-in [:admin :audit :deleting?] false)
           (assoc-in [:admin :success-message] "Audit log deleted successfully"))
     :dispatch-n [[:app.admin.frontend.adapters.audit/audit-log-deleted audit-id]
                  [:admin/load-audit-logs]]})) ; Reload to refresh the list

(rf/reg-event-db
  :admin/audit-log-delete-failed
  (fn [db [_ error]]
    (log/error "Failed to delete audit log:" error)
    (-> db
      (assoc-in [:admin :audit :deleting?] false)
      (assoc-in [:admin :audit :error] "Failed to delete audit log"))))

;; ============================================================================
;; Batch Operations
;; ============================================================================

(rf/reg-event-db
  :admin/show-batch-audit-actions
  (fn [db [_ selected-ids]]
    (log/info "Showing batch audit actions for:" (count selected-ids) "audit logs")
    (-> db
      (assoc-in [:admin :audit :batch-actions :visible?] true)
      (assoc-in [:admin :audit :batch-actions :selected-ids] selected-ids))))

(rf/reg-event-db
  :admin/hide-batch-audit-actions
  (fn [db [_]]
    (log/info "Hiding batch audit actions panel")
    (-> db
      (assoc-in [:admin :audit :batch-actions :visible?] false)
      (assoc-in [:admin :audit :batch-actions :selected-ids] []))))

(rf/reg-event-fx
  :admin/bulk-delete-audit-logs
  (fn [{:keys [db]} [_ audit-ids]]
    (let [token (adapters.core/admin-token db)
          ;; Convert IDs to strings for JSON serialization
          ids-as-strings (mapv str audit-ids)]
      (log/info "Bulk deleting audit logs:" (count ids-as-strings) "entries")

      (if token
        {:db (assoc-in db [:admin :audit :bulk-deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             "/admin/api/audit/batch"
                      :body            (js/JSON.stringify (clj->js {:ids ids-as-strings}))
                      :headers         (cond-> {"Content-Type" "application/json"}
                                         token (assoc "x-admin-token" token))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success      [:admin/bulk-audit-logs-deleted (count ids-as-strings)]
                      :on-failure      [:admin/bulk-audit-logs-delete-failed]}}
        {:db (assoc-in db [:admin :audit :error] "Authentication required")}))))

(rf/reg-event-fx
  :admin/bulk-audit-logs-deleted
  (fn [{:keys [db]} [_ count]]
    (log/info "Bulk deleted audit logs successfully, count:" count)
    {:db (-> db
           (assoc-in [:admin :audit :bulk-deleting?] false)
           (assoc-in [:admin :success-message] (str count " audit logs deleted successfully")))
     :dispatch-n [[:admin/hide-batch-audit-actions]
                  ;; Clear selection after successful batch delete
                  [:app.template.frontend.events.list.selection/select-all :audit-logs [] false]
                  [:admin/load-audit-logs]]}))

(rf/reg-event-db
  :admin/bulk-audit-logs-delete-failed
  (fn [db [_ error]]
    (log/error "Failed to bulk delete audit logs:" error)
    (-> db
      (assoc-in [:admin :audit :bulk-deleting?] false)
      (assoc-in [:admin :audit :error] "Failed to delete selected audit logs"))))

;; ============================================================================
;; Export Operations
;; ============================================================================

(rf/reg-event-fx
  :admin/export-single-audit-log
  (fn [{:keys [db]} [_ audit-log]]
    (log/info "Exporting single audit log:" (:id audit-log))

    (let [filename (str "audit-log-" (:id audit-log) "-"
                     (.toISOString (js/Date.)) ".json")
          json-data (js/JSON.stringify (clj->js audit-log) nil 2)
          blob (js/Blob. [json-data] #js {:type "application/json"})
          url (js/URL.createObjectURL blob)
          link (js/document.createElement "a")]

      (set! (.-href link) url)
      (set! (.-download link) filename)
      (.appendChild js/document.body link)
      (.click link)
      (.removeChild js/document.body link)
      (js/URL.revokeObjectURL url)

      {:db (assoc-in db [:admin :success-message] "Audit log exported successfully")})))

(rf/reg-event-fx
  :admin/export-all-audit-logs
  (fn [{:keys [db]} [_]]
    (let [token (adapters.core/admin-token db)
          current-filters (get-in db [:admin :audit :filters] {})]
      (log/info "Exporting all audit logs with filters:" current-filters)

      (if token
        {:db (assoc-in db [:admin :audit :exporting?] true)
         :http-xhrio {:method          :get
                      :uri             "/admin/api/audit/export-all"
                      :params          {:filters current-filters}
                      :headers         (when token {"x-admin-token" token})
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success      [:admin/all-audit-logs-exported]
                      :on-failure      [:admin/audit-logs-export-failed]}}
        {:db (assoc-in db [:admin :audit :error] "Authentication required")}))))

(rf/reg-event-db
  :admin/all-audit-logs-exported
  (fn [db [_ response]]
    (log/info "All audit logs exported successfully, count:" (:count response))
    (-> db
      (assoc-in [:admin :audit :exporting?] false)
      (assoc-in [:admin :success-message] (str (:count response) " audit logs exported successfully")))))

(rf/reg-event-db
  :admin/audit-logs-export-failed
  (fn [db [_ error]]
    (log/error "Failed to export audit logs:" error)
    (-> db
      (assoc-in [:admin :audit :exporting?] false)
      (assoc-in [:admin :audit :error] "Failed to export audit logs"))))

;; ============================================================================
;; API Failure Notification Badge
;; ============================================================================

(rf/reg-event-fx
  :admin/fetch-unread-api-failures
  (fn [{:keys [db]} _]
    (if (adapters.core/admin-token db)
      {:http-xhrio (admin-http/admin-get
                     {:uri "/admin/api/audit/api-failures/unread"
                      :on-success [::unread-api-failures-loaded]
                      :on-failure [::unread-api-failures-load-failed]})}
      {})))

(rf/reg-event-db
  ::unread-api-failures-loaded
  (fn [db [_ response]]
    (let [response* (if (object? response) (js->clj response :keywordize-keys true) response)
          cnt (or (:unread-count response*) 0)]
      (assoc-in db [:admin :audit :unread-api-failure-count] cnt))))

(rf/reg-event-db
  ::unread-api-failures-load-failed
  (fn [db [_ _error]]
    (log/warn "Failed to fetch unread API failure count")
    db))

(rf/reg-event-fx
  :admin/acknowledge-api-failures
  (fn [{:keys [db]} _]
    (if (adapters.core/admin-token db)
      {:db (assoc-in db [:admin :audit :unread-api-failure-count] 0)
       :http-xhrio (admin-http/admin-post
                     {:uri "/admin/api/audit/api-failures/acknowledge"
                      :on-success [::api-failures-acknowledged]
                      :on-failure [::api-failures-acknowledge-failed]})}
      {})))

(rf/reg-event-db
  ::api-failures-acknowledged
  (fn [db _]
    (log/info "API failures acknowledged")
    db))

(rf/reg-event-fx
  ::api-failures-acknowledge-failed
  (fn [{:keys [db]} [_ _error]]
    (log/warn "Failed to acknowledge API failures")
    {:db db
     :dispatch [:admin/fetch-unread-api-failures]}))

;; ============================================================================
;; Pagination
;; ============================================================================
;; ============================================================================
;; Sorting
;; ============================================================================
