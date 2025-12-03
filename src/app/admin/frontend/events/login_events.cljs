(ns app.admin.frontend.events.login-events
  "Admin events for global login events table.
   
   This namespace handles:
   - HTTP loading of login events
   - Filtering, pagination, sorting
   
   Data normalization and template sync are in app.admin.frontend.adapters.login-events"
  (:require
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
