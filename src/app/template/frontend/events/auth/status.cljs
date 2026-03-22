(ns app.template.frontend.events.auth.status
  (:require
    [app.admin.frontend.adapters.users :as admin-users-adapter]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.auth.ids :as ids]
    [app.template.frontend.events.auth.utils :as auth-utils]
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
          session-valid? (get response :session-valid true)
          user (get response :user)
          provider (or (get response :provider)
                     (:auth-provider user)
                     (:auth_provider user))
          tenant (get response :tenant)
          permissions (get response :permissions)
          permissions* (cond
                         (nil? permissions) nil
                         (set? permissions) permissions
                         (sequential? permissions) (set permissions)
                         (coll? permissions) (set permissions)
                         :else #{permissions})
          ;; Multi-tenant fields from auth status response
          membership-role (get response :membership-role)
          tenant-selection-required (get response :tenant-selection-required)
          available-tenants (get response :available-tenants)
          ;; Onboarding summary (lightweight, from auth-status)
          onboarding (get response :onboarding)
          current-page (get-in db (paths/current-page))
          user-role membership-role]

      ;; Log authentication details
      (when user
        (log/debug "User session:" (:full-name user) "tenant:" (:name tenant) "role:" user-role
          "membership-role:" membership-role "tenant-selection-required:" tenant-selection-required))

      (let [no-tenant? (get response :no-tenant false)
            updated-db (-> db
                         ;; Clear loading state
                         (assoc-in [:session :loading?] false)

                         ;; Set authentication status
                         (assoc-in [:session :authenticated?] authenticated?)
                         (assoc-in [:session :session-valid?] session-valid?)

                         (assoc-in [:session :provider] provider)

                         ;; Set user information (updated format for multi-tenant)
                         (assoc-in [:session :user] user)

                         ;; Set tenant information (new for multi-tenant)
                         (assoc-in [:session :tenant] tenant)

                         ;; Set URL slug from tenant (for /t/{slug}/... display)
                         (assoc-in [:tenant :url-slug]
                           (or (:slug tenant) (:tenants/slug tenant)))

                         ;; Set user permissions (new for multi-tenant)
                         (assoc-in [:session :permissions] permissions*)

                         ;; Store membership role for current tenant
                         (assoc-in [:session :membership-role] membership-role)

                         ;; Store tenant selection state
                         (assoc-in [:session :tenant-selection-required] tenant-selection-required)
                         (assoc-in [:session :available-tenants] available-tenants)

                         ;; Store no-tenant state
                         (assoc-in [:session :no-tenant?] no-tenant?)

                         ;; Store onboarding summary
                         (assoc-in [:session :onboarding] onboarding)

                         ;; Clear any previous errors
                         (update :session dissoc :error))
            ;; Check for a `return` URL parameter (e.g. from invitation accept flow)
            return-url (when (and (exists? js/window) (exists? js/URLSearchParams))
                         (-> js/window .-location .-search
                           (js/URLSearchParams.)
                           (.get "return")))
            redirect-path (auth-utils/post-auth-redirect
                            {:return-url return-url
                             :no-tenant? no-tenant?
                             :tenant-selection-required tenant-selection-required
                             :membership-role user-role
                             :onboarding onboarding})
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
