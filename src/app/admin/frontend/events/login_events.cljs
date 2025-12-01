(ns app.admin.frontend.events.login-events
  "Admin events for global login events table"
  (:require
    [app.admin.frontend.adapters.login-events]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

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
      {:db (-> db
             (assoc-in [:admin :login-events :loading?] true)
             (assoc-in [:admin :login-events :error] nil)
             (assoc-in [:admin :login-events :filters] final-filters)
             (assoc-in [:admin :login-events :pagination] final-pagination)
             (assoc-in [:admin :login-events :sort] final-sort))
       :dispatch [:app.admin.frontend.adapters.login-events/load-login-events-direct params-to-send]})))

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
