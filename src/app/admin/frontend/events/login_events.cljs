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
          template-per-page (paths/resolved-list-per-page db entity-key 20)
          template-page (paths/resolved-list-current-page db entity-key)
          current-filters (merge (get-in db [:admin :login-events :filters] {})
                            (get-in db (paths/list-filters entity-key) {}))
          admin-pagination (get-in db [:admin :login-events :pagination] {})
          current-pagination (merge (dissoc admin-pagination :page :current-page :per-page :limit :offset)
                               {:page template-page
                                :current-page template-page
                                :per-page template-per-page})
          template-sort (paths/resolved-list-sort-config db entity-key)
          current-sort (merge {:field :created-at :direction :desc}
                         (get-in db [:admin :login-events :sort] {})
                         template-sort)
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
      (log/info "LOGIN EVENTS LOAD →" {:pagination resolved-pagination
                                       :filters final-filters
                                       :request-params request-params})
      (if (adapters.core/admin-token db)
        {:db (-> db
               (assoc-in [:admin :login-events :loading?] true)
               (assoc-in [:admin :login-events :error] nil)
               (assoc-in [:admin :login-events :filters] final-filters)
               (assoc-in [:admin :login-events :pagination] resolved-pagination)
               (assoc-in [:admin :login-events :sort] final-sort)
               (assoc-in (conj (paths/entity-metadata :login-events) :loading?) true))
         :http-xhrio (admin-http/admin-get
                       {:uri "/admin/api/login-events"
                        :params request-params
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
    (let [response* (if (object? response) (js->clj response :keywordize-keys true) response)
          events (get response* :events [])
          total-items (or (:total response*)
                        (count events))
          metadata-path (paths/entity-metadata :login-events)]
      {:db (-> db
             (assoc-in (conj metadata-path :loading?) false)
             (assoc-in (conj metadata-path :error) nil)
             (assoc-in (paths/list-total-items :login-events) total-items))
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
    (let [token (adapters.core/admin-token db)]
      (log/info "Deleting login event:" event-id)

      (if token
        {:db (assoc-in db [:admin :login-events :deleting?] true)
         :http-xhrio {:method :delete
                      :uri "/admin/api/login-events/batch"
                      :body (js/JSON.stringify (clj->js {:ids [(str event-id)]}))
                      :headers (cond-> {"Content-Type" "application/json"}
                                 token (assoc "x-admin-token" token))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :timeout 10000
                      :on-success [:admin/login-event-deleted event-id]
                      :on-failure [:admin/login-event-delete-failed]}}
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
    (let [token (adapters.core/admin-token db)
          ;; Convert IDs to strings for JSON serialization
          ids-as-strings (mapv str event-ids)]
      (log/info "Bulk deleting login events:" (count ids-as-strings) "entries")

      (if token
        {:db (assoc-in db [:admin :login-events :bulk-deleting?] true)
         :http-xhrio {:method          :delete
                      :uri             "/admin/api/login-events/batch"
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
  :admin/hide-batch-login-event-actions
  (fn [db [_]]
    (log/info "Hiding batch login event actions panel")
    (-> db
      (assoc-in [:admin :login-events :batch-actions :visible?] false)
      (assoc-in [:admin :login-events :batch-actions :selected-ids] []))))
