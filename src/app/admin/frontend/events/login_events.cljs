(ns app.admin.frontend.events.login-events
  "Admin events for global login events table.
   
   This namespace handles:
   - HTTP loading of login events
   - Filtering, pagination, sorting
   - Single and bulk delete operations
   
   Data normalization and template sync are in app.admin.frontend.adapters.login-events"
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.adapters.login-events :as login-events-adapter]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Load Login Events
;; =============================================================================

(rf/reg-event-fx
  :admin/load-login-events
  (fn [{:keys [db]} [_ {:keys [filters pagination sort] :as _params}]]
    (let [entity-key :login-events
          template-per-page (or (get-in db (paths/list-per-page entity-key))
                              (get-in db (conj (paths/list-ui-state entity-key) :per-page))
                              (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page))
                              20)
          template-page (or (get-in db (paths/list-current-page entity-key))
                          (get-in db (conj (paths/list-ui-state entity-key) :current-page))
                          (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page))
                          1)
          current-filters (get-in db [:admin :login-events :filters] {})
          current-pagination (get-in db [:admin :login-events :pagination]
                               {:page template-page :per-page template-per-page})
          current-sort (get-in db [:admin :login-events :sort]
                         {:field :created-at :direction :desc})
          final-filters (merge current-filters filters)
          final-pagination (merge current-pagination pagination)
          final-sort (merge current-sort sort)
          params-to-send (cond-> {}
                           (seq final-filters) (assoc :filters final-filters)
                           final-pagination (assoc :pagination final-pagination)
                           final-sort (assoc :sort final-sort))]
      (log/info "LOGIN EVENTS LOAD →" {:pagination final-pagination
                                       :filters final-filters})
      (if (adapters.core/admin-token db)
        {:db (-> db
               (assoc-in [:admin :login-events :loading?] true)
               (assoc-in [:admin :login-events :error] nil)
               (assoc-in [:admin :login-events :filters] final-filters)
               (assoc-in [:admin :login-events :pagination] final-pagination)
               (assoc-in [:admin :login-events :sort] final-sort)
               (assoc-in (conj (paths/entity-metadata :login-events) :loading?) true))
         :http-xhrio (admin-http/admin-get
                       {:uri "/admin/api/login-events"
                        :params params-to-send
                        :on-success [::login-events-loaded]
                        :on-failure [::login-events-load-failed]})}
        {:db (-> db
               (assoc-in [:admin :login-events :loading?] false)
               (assoc-in [:admin :login-events :error] "Authentication required")
               (assoc-in (conj (paths/entity-metadata :login-events) :loading?) false)
               (assoc-in (conj (paths/entity-metadata :login-events) :error) "Authentication required"))}))))

;; HTTP success handler - syncs data to template store
(rf/reg-event-fx
  ::login-events-loaded
  (fn [{:keys [db]} [_ response]]
    (let [events (get response :events [])
          metadata-path (paths/entity-metadata :login-events)]
      {:db (-> db
             (assoc-in (conj metadata-path :loading?) false)
             (assoc-in (conj metadata-path :error) nil))
       :dispatch-n [[::login-events-adapter/sync-login-events-to-template events]
                    [:admin/login-events-loaded]]})))

;; HTTP failure handler
(rf/reg-event-fx
  ::login-events-load-failed
  (fn [{:keys [db]} [_ error]]
    (let [error-msg "Failed to load login events"
          path (paths/entity-metadata :login-events)]
      (log/error "Login events load failed:" error)
      {:db (-> db
             (assoc-in (conj path :loading?) false)
             (assoc-in (conj path :error) error-msg))
       :dispatch [:admin/login-events-load-failed error]})))

;; =============================================================================
;; Admin State Handlers
;; =============================================================================

(rf/reg-event-db
  :admin/login-events-loaded
  (fn [db _]
    (log/info "Login events loaded successfully")
    (-> db
      (assoc-in [:admin :login-events :loading?] false)
      (assoc-in [:admin :login-events :error] nil))))

(rf/reg-event-db
  :admin/login-events-load-failed
  (fn [db [_ error]]
    (let [msg (or (get-in error [:response :error]) "Failed to load login events")]
      (log/error "Failed to load login events:" msg)
      (-> db
        (assoc-in [:admin :login-events :loading?] false)
        (assoc-in [:admin :login-events :error] msg)))))

;; =============================================================================
;; Single Delete Login Event
;; =============================================================================

(rf/reg-event-fx
  :admin/delete-login-event
  (fn [{:keys [db]} [_ event-id]]
    (let [token (or (get-in db [:admin :token])
                  (.getItem js/localStorage "admin-token"))]
      (log/info "Deleting login event:" event-id)

      (if token
        {:db (assoc-in db [:admin :login-events :deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             (str "/admin/api/login-events/" event-id)
                      :headers         (when token {"x-admin-token" token})
                      :format          (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :timeout         10000
                      :on-success      [:admin/login-event-deleted event-id]
                      :on-failure      [:admin/login-event-delete-failed]}}
        {:db (assoc-in db [:admin :login-events :error] "Authentication required")}))))

(rf/reg-event-fx
  :admin/login-event-deleted
  (fn [{:keys [db]} [_ event-id]]
    (log/info "Login event deleted successfully:" event-id)
    {:db (-> db
           (assoc-in [:admin :login-events :deleting?] false)
           (assoc-in [:admin :success-message] "Login event deleted successfully"))
     :dispatch-n [[::login-events-adapter/login-event-deleted event-id]
                  [:admin/load-login-events]]}))

(rf/reg-event-db
  :admin/login-event-delete-failed
  (fn [db [_ error]]
    (log/error "Failed to delete login event:" error)
    (-> db
      (assoc-in [:admin :login-events :deleting?] false)
      (assoc-in [:admin :login-events :error] "Failed to delete login event"))))

;; =============================================================================
;; Bulk Delete Login Events
;; =============================================================================

(rf/reg-event-fx
  :admin/bulk-delete-login-events
  (fn [{:keys [db]} [_ event-ids]]
    (let [token (or (get-in db [:admin :token])
                  (.getItem js/localStorage "admin-token"))
          ;; Convert IDs to strings for JSON serialization
          ids-as-strings (mapv str event-ids)]
      (log/info "Bulk deleting login events:" (count ids-as-strings) "entries")

      (if token
        {:db (assoc-in db [:admin :login-events :bulk-deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             "/admin/api/login-events/bulk"
                      :body            (js/JSON.stringify (clj->js {:ids ids-as-strings}))
                      :headers         (cond-> {"Content-Type" "application/json"}
                                         token (assoc "x-admin-token" token))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :timeout         30000
                      :on-success      [:admin/bulk-login-events-deleted (count ids-as-strings)]
                      :on-failure      [:admin/bulk-login-events-delete-failed]}}
        {:db (assoc-in db [:admin :login-events :error] "Authentication required")}))))

(rf/reg-event-fx
  :admin/bulk-login-events-deleted
  (fn [{:keys [db]} [_ count]]
    (log/info "Bulk deleted login events successfully, count:" count)
    {:db (-> db
           (assoc-in [:admin :login-events :bulk-deleting?] false)
           (assoc-in [:admin :success-message] (str count " login events deleted successfully")))
     :dispatch-n [[:admin/hide-batch-login-event-actions]
                  ;; Clear selection after successful batch delete
                  [:app.template.frontend.events.list.selection/select-all :login-events [] false]
                  [:admin/load-login-events]]}))

(rf/reg-event-db
  :admin/bulk-login-events-delete-failed
  (fn [db [_ error]]
    (log/error "Failed to bulk delete login events:" error)
    (-> db
      (assoc-in [:admin :login-events :bulk-deleting?] false)
      (assoc-in [:admin :login-events :error] "Failed to delete selected login events"))))

;; =============================================================================
;; Batch Actions UI
;; =============================================================================

(rf/reg-event-db
  :admin/show-batch-login-event-actions
  (fn [db [_ selected-ids]]
    (log/info "Showing batch login event actions for:" (count selected-ids) "events")
    (-> db
      (assoc-in [:admin :login-events :batch-actions :visible?] true)
      (assoc-in [:admin :login-events :batch-actions :selected-ids] selected-ids))))

(rf/reg-event-db
  :admin/hide-batch-login-event-actions
  (fn [db [_]]
    (log/info "Hiding batch login event actions panel")
    (-> db
      (assoc-in [:admin :login-events :batch-actions :visible?] false)
      (assoc-in [:admin :login-events :batch-actions :selected-ids] []))))
