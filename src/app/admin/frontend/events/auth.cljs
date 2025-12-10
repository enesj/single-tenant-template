(ns app.admin.frontend.events.auth
  (:require
    [app.admin.frontend.auth.persistence :as auth-persist]
    [app.admin.frontend.utils.http :as admin-http]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]))

(rf/reg-event-db
  :admin/clear-success-message
  (fn [db _]
    (dissoc db :admin/success-message)))

;; Initialize events
(rf/reg-event-fx
  :admin/init-login
  (fn [{:keys [db]} _]
    {:db (-> db
           (dissoc :admin/login-loading? :admin/login-error))}))

(rf/reg-event-fx
  :admin/login
  (fn [{:keys [db]} [_ email password]]
    {:db (assoc db
           :admin/login-loading? true
           :admin/login-error nil)
     :http-xhrio (admin-http/auth-request
                   {:uri "/admin/api/login"
                    :params {:email email :password password}
                    :on-success [:admin/login-success]
                    :on-failure [:admin/login-failure]})}))

(rf/reg-event-fx
  :admin/login-success
  (fn [{:keys [db]} [_ response]]
    (let [admin-data (:admin response)
          role (some-> admin-data :role keyword)]
      ;; Persist auth state to survive hot-reloads
      (auth-persist/store-auth-state!
        {:token (:token response)
         :user admin-data
         :authenticated? true})
      {:db (-> db
             (assoc :admin/current-user admin-data
               :admin/token (:token response)
               :admin/authenticated? true
               :admin/current-user-role role)
             (dissoc :admin/login-loading? :admin/login-error))
       :admin/store-token (:token response)
       ;; Navigate to admin dashboard (full reload to ensure admin shell loads)
       :admin/navigate "/admin/dashboard"})))

(rf/reg-event-db
  :admin/login-failure
  (fn [db [_ error]]
    (-> db
      (assoc :admin/login-error (or (get-in error [:response :error])
                                  "Login failed. Please try again."))
      (dissoc :admin/login-loading?))))

(rf/reg-event-fx
  :admin/logout
  (fn [{:keys [db]} _]
    ;; Clear persisted auth state
    (auth-persist/clear-auth-state!)
    {:db (dissoc db :admin/current-user :admin/token :admin/authenticated? :admin/current-user-role)
     :http-xhrio (admin-http/auth-request
                   {:uri "/admin/api/logout"
                    :on-success [:admin/logout-success]
                    :on-failure [:admin/logout-success]}) ; Even on failure, clear local state
     :admin/clear-token nil
     :admin/navigate "/admin/login"}))

(rf/reg-event-fx
  :admin/logout-success
  (fn [_ _]
    {:admin/navigate "/admin/login"}))

(rf/reg-event-fx
  :admin/check-auth
  (fn [{:keys [db]} _]
    (let [token (or (:admin/token db)
                  (auth-persist/get-persisted-token)
                  (.getItem js/localStorage "admin-token"))]
      (if token
        {:db (-> db
               (assoc :admin/token token)
               (assoc :admin/auth-checking? true))
         :http-xhrio (admin-http/dashboard-request
                       {:on-success [:admin/auth-valid]
                        :on-failure [:admin/auth-invalid]})}
        {:admin/navigate "/admin/login"}))))

(rf/reg-event-fx
  :admin/auth-valid
  (fn [{:keys [db]} [_ first-arg second-arg]]
    ;; Handle two calling patterns:
    ;; 1. [:admin/auth-valid response] - from :admin/check-auth (line 81)
    ;; 2. [:admin/auth-valid on-auth-success response] - from :admin/check-auth-protected (line 183)
    ;;
    ;; When on-auth-success is passed, the response is the second arg.
    ;; When only response is passed, second-arg is nil.
    (let [;; Determine which arg is the response and which is the callback
          [on-auth-success response] (if (map? first-arg)
                                       ;; Pattern 1: first-arg is the response
                                       [nil first-arg]
                                       ;; Pattern 2: first-arg is callback, second-arg is response
                                       [first-arg second-arg])
          ;; Extract admin info from dashboard response if present
          current-admin (when (map? response)
                          (:current-admin response))
          ;; Get role from new admin data or existing db
          admin-data (or current-admin (:admin/current-user db))
          role (some-> admin-data :role keyword)
          current-route (:admin/current-route db)]
      {:db (-> db
             (assoc :admin/authenticated? true)
             (cond-> current-admin (assoc :admin/current-user current-admin))
             (cond-> role (assoc :admin/current-user-role role))
             (dissoc :admin/auth-checking?))
       ;; Execute the success callback(s) if provided, plus any route-specific actions
       :dispatch-n (let [extras (cond
                                  (and (sequential? on-auth-success)
                                    (sequential? (first on-auth-success))) on-auth-success
                                  (sequential? on-auth-success) [on-auth-success]
                                  :else [])
                         route-events (cond
                                        (= current-route :admin-advanced-dashboard) [[:admin/load-advanced-dashboard]]
                                        (= current-route :admin-dashboard) [[:admin/load-dashboard]]
                                        :else [])]
                     (into [] (concat extras route-events)))})))

;; Immediate auth success event (when already authenticated)
(rf/reg-event-fx
  :admin/auth-success-immediate
  (fn [{:keys [db]} _]
    (let [current-route (:admin/current-route db)]
      (case current-route
        :admin-advanced-dashboard {:dispatch [:admin/load-advanced-dashboard]}
        :admin-dashboard {:dispatch [:admin/load-dashboard]}
        {}))))

(rf/reg-event-fx
  :admin/auth-invalid
  (fn [{:keys [db]} _]
    ;; Clear persisted auth state
    (auth-persist/clear-auth-state!)
    {:db (-> db
           (dissoc :admin/authenticated? :admin/token :admin/current-user :admin/current-user-role :admin/auth-checking?))
     :admin/clear-token nil
     :admin/navigate "/admin/login"}))

;; Navigation event
(rf/reg-event-fx
  :admin/navigated
  (fn [{:keys [db]} [_ route]]
    {:db (assoc db :admin/current-route route)}))

;; Effects for localStorage
(rf/reg-fx
 :admin/store-token
  (fn [token]
    (.setItem js/localStorage "admin-token" token)))

(rf/reg-fx
 :admin/clear-token
  (fn [_]
    (.removeItem js/localStorage "admin-token")))

;; Check auth for protected routes (doesn't redirect on login page)
(rf/reg-event-fx
  :admin/check-auth-protected
  (fn [{:keys [db]} [_ on-auth-success]]
    (let [token (or (:admin/token db)
                  (auth-persist/get-persisted-token)
                  (.getItem js/localStorage "admin-token"))
          already-authenticated? (:admin/authenticated? db)
          auth-checking? (:admin/auth-checking? db)]

      (cond
        ;; Already authenticated, trigger success callback immediately
        already-authenticated?
        (let [extras (cond
                       (and (sequential? on-auth-success)
                         (sequential? (first on-auth-success))) on-auth-success
                       (sequential? on-auth-success) [on-auth-success]
                       :else [])]
          {:dispatch-n (into [] (concat extras [[:admin/auth-success-immediate]]))})

        ;; Already checking auth, don't start another check
        auth-checking?
        {}

        ;; Has token but not authenticated yet, validate it
        token
        {:db (-> db
               (assoc :admin/token token)
               (assoc :admin/auth-checking? true))
         :http-xhrio (admin-http/dashboard-request
                       {;; Pass-through payload with the original on-auth-success callbacks
                        :on-success (conj [:admin/auth-valid] on-auth-success)
                        :on-failure [:admin/auth-invalid]})}

        ;; No token, redirect to login
        :else
        (do
          (when ^boolean goog.DEBUG
            (.log js/console "🚫 DEBUG: No token, redirecting to login"))
          {:dispatch [:admin/navigate-client "/admin/login"]})))))

;; Navigation effect using window.location (causes full page reload)
(rf/reg-fx
 :admin/navigate
  (fn [path]
    (set! (.-href (.-location js/window)) path)))

;; Client-side navigation effect (doesn't cause page reload)
(rf/reg-event-fx
  :admin/navigate-client
  (fn [_ [_ path]]
    ;; Use rtfe/push-state directly for proper navigation
    (cond
      (= path "/admin/dashboard") (rtfe/push-state :admin-dashboard)
      (= path "/admin/login") (rtfe/push-state :admin-login)
      :else (rtfe/push-state path))
    {}))

;; Message management moved to app.admin.frontend.events.users.template.messages

(rf/reg-event-db
  :admin/clear-error-message
  (fn [db _]
    (dissoc db :admin/error-message)))

(rf/reg-event-db
  :admin/set-success-message
  (fn [db [_ message]]
    (assoc db :admin/success-message message)))

(rf/reg-event-db
  :admin/set-error-message
  (fn [db [_ message]]
    (assoc db :admin/error-message message)))

(rf/reg-event-fx
  :admin/redirect-to-login
  (fn [_ _]
    (rtfe/push-state :admin-login)
    {}))

;; Authentication state restoration on app initialization
(rf/reg-event-fx
  :admin/init-auth-persistence
  (fn [{:keys [db]} _]
    (let [auth-state (auth-persist/init-auth-persistence!
                     (fn [restored-state]
                       (rf/dispatch [:admin/restore-auth-state restored-state])))]
      (if (:valid? auth-state)
        ;; Has valid persisted state, wait for async restoration
        {:db (assoc db :admin/auth-checking? true)}
        ;; No valid persisted state, proceed normally
        {}))))

(rf/reg-event-fx
  :admin/restore-auth-state
  (fn [{:keys [db]} [_ auth-state]]
    (let [{:keys [token user authenticated?]} auth-state]
      (log/info "Restoring authentication state"
               {:token-present (boolean token)
                :user-present (boolean user)
                :authenticated? authenticated?})
      {:db (-> db
             (cond-> token (assoc :admin/token token))
             (cond-> user (assoc :admin/current-user user))
             (cond-> (and user (:role user))
               (assoc :admin/current-user-role (keyword (:role user))))
             ;; Force fresh validation instead of trusting persisted state blindly
             (dissoc :admin/authenticated? :admin/auth-checking?))
       ;; Always re-validate the token with the backend to avoid stale sessions
       :dispatch (when token [:admin/check-auth])})))

;; Theme management events
