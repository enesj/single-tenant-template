(ns app.template.frontend.events.auth.status
  (:require
    [app.admin.frontend.adapters.users :as admin-users-adapter]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Authentication Status Events
;; ========================================================================

;; Event to fetch authentication status from the backend
(rf/reg-event-fx
  ids/fetch-auth-status
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:session :loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/auth/status"
                    :on-success [ids/fetch-auth-status-success]
                    :on-failure [ids/fetch-auth-status-failure]})}))

;; Handle successful auth status retrieval
(rf/reg-event-fx
  ids/fetch-auth-status-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [authenticated? (get response :authenticated false)
          session-valid? (get response :session-valid true) ; default to true for legacy sessions
          legacy-session? (get response :legacy-session false)
          provider (get response :provider)
          tokens (get response :tokens)
          user (get response :user)
          tenant (get response :tenant)
          permissions (get response :permissions)
          permissions* (cond
                         (nil? permissions) nil
                         (set? permissions) permissions
                         (sequential? permissions) (set permissions)
                         (coll? permissions) (set permissions)
                         :else #{permissions})
          current-page (get-in db [:ui :current-page])
          user-role (:role user)]

      ;; Log authentication details
      (when user
        (if legacy-session?
          (log/debug "Legacy user session:" (:name user))
          (log/debug "Multi-tenant user session:" (:full-name user) "tenant:" (:name tenant) "role:" user-role)))

      (let [updated-db (-> db
                         ;; Clear loading state
                         (assoc-in [:session :loading?] false)

                         ;; Set authentication status
                         (assoc-in [:session :authenticated?] authenticated?)
                         (assoc-in [:session :session-valid?] session-valid?)
                         (assoc-in [:session :legacy-session?] legacy-session?)

                         ;; Handle legacy OAuth tokens (backward compatibility)
                         (assoc-in [:session :oauth2/access-tokens] tokens)
                         (assoc-in [:session :provider] provider)

                         ;; Set user information (updated format for multi-tenant)
                         (assoc-in [:session :user] user)

                         ;; Set tenant information (new for multi-tenant)
                         (assoc-in [:session :tenant] tenant)

                         ;; Set user permissions (new for multi-tenant)
                         (assoc-in [:session :permissions] permissions*)

                         ;; Clear any previous errors
                         (update :session dissoc :error))
            ;; Determine redirect based on role
            redirect-path (cond
                            ;; Unassigned users go to waiting room
                            (= user-role "unassigned") "/waiting-room"
                            ;; Members and above go to expense dashboard
                            (contains? #{"member" "admin" "owner"} user-role) "/dashboard"
                            ;; Viewers or other roles go to entities
                            :else "/entities")
            base-effects (cond-> {:db updated-db}
                           (and authenticated? (= current-page :login))
                           (assoc :redirect redirect-path))]

        ;; Mirror the resolved session user into the shared template
        ;; entity store so FK table columns pointing at :users can
        ;; resolve labels via list-view + select-options.
        (cond-> base-effects
          user (update :fx (fnil conj [])
                 [:dispatch [::admin-users-adapter/sync-users-to-template [user]]]))))))

;; Handle failure to fetch auth status
(rf/reg-event-db
  ids/fetch-auth-status-failure
  common-interceptors
  (fn [db _]
    (log/error "Failed to fetch auth status")
    (-> db
      (assoc-in [:session :loading?] false)
      (assoc-in [:session :authenticated?] false)
      (assoc-in [:session :error] "Failed to fetch authentication status"))))

