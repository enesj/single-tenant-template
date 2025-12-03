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
    [app.template.frontend.events.list.ui-state :as ui-events]
    [clojure.string :as str]
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
          ;; Read template system pagination first to avoid divergence
          template-per-page (or (get-in db (paths/list-per-page entity-key))
                              (get-in db (conj (paths/list-ui-state entity-key) :per-page))
                              (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page))
                              20)
          template-page (or (get-in db (paths/list-current-page entity-key))
                          (get-in db (conj (paths/list-ui-state entity-key) :current-page))
                          (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page))
                          1)

          current-filters (get-in db [:admin :audit :filters] {})
          ;; Keep admin path but prefer template values when present
          admin-pagination (get-in db [:admin :audit :pagination] {:page 1 :per-page template-per-page})
          current-pagination (merge {:page template-page :per-page template-per-page} admin-pagination)
          ;; Align default sort with adapter default (:timestamp)
          current-sort (get-in db [:admin :audit :sort] {:field :timestamp :direction :desc})

          ;; Merge current state with any provided params
          final-filters (merge current-filters filters)
          final-pagination (merge current-pagination pagination)
          final-sort (merge current-sort sort)

          params-to-send (cond-> {}
                           (seq final-filters) (assoc :filters final-filters)
                           final-pagination (assoc :pagination final-pagination)
                           final-sort (assoc :sort final-sort))]

      (log/info "AUDIT LOAD → template per-page:" template-per-page
        "template page:" template-page
        "admin current pagination:" admin-pagination)
      (log/info "AUDIT LOAD → final pagination to send:" final-pagination)

      (if (adapters.core/admin-token db)
        {:db (-> db
               (assoc-in [:admin :audit :loading?] true)
               (assoc-in [:admin :audit :error] nil)
               (assoc-in [:admin :audit :filters] final-filters)
               (assoc-in [:admin :audit :pagination] final-pagination)
               (assoc-in [:admin :audit :sort] final-sort)
               (assoc-in (conj (paths/entity-metadata :audit-logs) :loading?) true))
         :http-xhrio (admin-http/admin-get
                       {:uri "/admin/api/audit"
                        :params params-to-send
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
    (let [raw-logs (get response :logs [])
          metadata-path (paths/entity-metadata :audit-logs)]
      {:db (-> db
             (assoc-in (conj metadata-path :loading?) false)
             (assoc-in (conj metadata-path :error) nil))
       :dispatch-n [[::audit-adapter/sync-audit-logs-to-template raw-logs]
                    [:admin/audit-logs-loaded]]})))

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
    (let [token (or (get-in db [:admin :token])
                  (.getItem js/localStorage "admin-token"))]
      (log/info "Deleting audit log:" audit-id)

      (if token
        {:db (assoc-in db [:admin :audit :deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             (str "/admin/api/audit/" audit-id)
                      :headers         (when token {"x-admin-token" token})
                      :format          (ajax/json-request-format)
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
    (let [token (or (get-in db [:admin :token])
                  (.getItem js/localStorage "admin-token"))
          ;; Convert IDs to strings for JSON serialization
          ids-as-strings (mapv str audit-ids)]
      (log/info "Bulk deleting audit logs:" (count ids-as-strings) "entries")

      (if token
        {:db (assoc-in db [:admin :audit :bulk-deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             "/admin/api/audit/bulk"
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
  :admin/export-selected-audit-logs
  (fn [{:keys [db]} [_ audit-ids format]]
    (let [token (or (get-in db [:admin :token])
                  (.getItem js/localStorage "admin-token"))]
      (log/info "Exporting selected audit logs, count:" (count audit-ids) "format:" format)

      (if token
        {:db (assoc-in db [:admin :audit :exporting?] true)
         :http-xhrio {:method          :post
                      :uri             "/admin/api/audit/export"
                      :params          {:ids audit-ids :format format}
                      :headers         (when token {"x-admin-token" token})
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success      [:admin/audit-logs-exported format (count audit-ids)]
                      :on-failure      [:admin/audit-logs-export-failed]}}
        {:db (assoc-in db [:admin :audit :error] "Authentication required")}))))

(rf/reg-event-fx
  :admin/export-all-audit-logs
  (fn [{:keys [db]} [_]]
    (let [token (or (get-in db [:admin :token])
                  (.getItem js/localStorage "admin-token"))
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
  :admin/audit-logs-exported
  (fn [db [_ format count]]
    (log/info "Audit logs exported successfully, format:" format "count:" count)
    (-> db
      (assoc-in [:admin :audit :exporting?] false)
      (assoc-in [:admin :success-message] (str count " audit logs exported as " (str/upper-case format))))))

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
;; Pagination
;; ============================================================================

(rf/reg-event-fx
  :admin/audit-change-page
  (fn [_ [_ page]]
    (log/info "Changing audit logs page to:" page)
    (let [safe-page (max 1 (or page 1))]
      {:dispatch-n [[::ui-events/set-current-page :audit-logs safe-page]
                    [:admin/load-audit-logs]]})))

(rf/reg-event-fx
  :admin/audit-change-page-size
  (fn [_ [_ page-size]]
    (log/info "Changing audit logs page size to:" page-size)
    (let [parsed (cond
                   (number? page-size) page-size
                   (string? page-size) (js/parseInt page-size 10)
                   :else page-size)
          clamped (if (and parsed (pos? parsed)) parsed 10)]
      {:dispatch-n [[::ui-events/set-per-page :audit-logs clamped]
                    [:admin/load-audit-logs]]})))

;; ============================================================================
;; Sorting
;; ============================================================================

(rf/reg-event-fx
  :admin/audit-sort-by
  (fn [_ [_ field direction]]
    (log/info "Sorting audit logs by:" field direction)
    (let [new-sort {:field field :direction direction}]
      {:dispatch [:admin/load-audit-logs {:sort new-sort}]})))
