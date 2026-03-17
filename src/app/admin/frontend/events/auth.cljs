(ns app.admin.frontend.events.auth
  "Auth events (login/logout); keep token flow consistent."
  (:require
    [app.admin.frontend.auth.persistence :as auth-persist]
    [app.admin.frontend.utils.http :as admin-http]
    ;; For side effects: registers :routing/push-state and related routing handlers.
    [app.template.frontend.events.routing]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]))

(defn- event-vector?
  "True when x looks like a re-frame event vector ([:event/id ...])."
  [x]
  (and (sequential? x)
    (keyword? (first x))))

(defn- normalize-dispatch-events
  "Normalize a success-callback payload into a vector of valid event vectors.

  Accepts:
  - nil
  - a single event vector: [:event/id ...]
  - many event vectors: [[:event/a] [:event/b ...]]

  IMPORTANT: An empty vector ([]) means no events and must NOT be dispatched,
  otherwise re-frame treats it as an event with event-id nil."
  [on-auth-success]
  (let [events (cond
                 (nil? on-auth-success) []
                 (and (sequential? on-auth-success) (empty? on-auth-success)) []
                 (and (sequential? on-auth-success)
                   (sequential? (first on-auth-success))) on-auth-success
                 (sequential? on-auth-success) [on-auth-success]
                 :else [])]
    (->> events
      (filter event-vector?)
      vec)))

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
             (dissoc :admin/login-loading? :admin/login-error :admin/auth-checking?))
       ;; Refresh admin UI configs only after auth succeeds so the login page
       ;; does not hit protected /admin/api/settings* endpoints unauthenticated.
       :dispatch-n [[:admin/load-ui-configs]
                    ;; Use router-aware SPA navigation so the route match updates immediately
                    ;; (avoids rendering the public home page until a manual refresh).
                    [:admin/navigate-client "/admin/dashboard"]]})))

(rf/reg-event-db
  :admin/login-failure
  (fn [db [_ error]]
    (-> db
      (assoc :admin/login-error (or (get-in error [:response :error])
                                  "Login failed. Please try again."))
      (dissoc :admin/login-loading? :admin/auth-checking?))))

(rf/reg-event-fx
  :admin/logout
  (fn [{:keys [db]} _]
    ;; Clear persisted auth state
    (auth-persist/clear-auth-state!)
    {:db (dissoc db
           :admin/current-user
           :admin/token
           :admin/authenticated?
           :admin/current-user-role
           :admin/auth-checking?
           :admin/login-loading?
           :admin/login-error)
     :http-xhrio (admin-http/auth-request
                   {:uri "/admin/api/logout"
                    :on-success [:admin/logout-success]
                    :on-failure [:admin/logout-success]}) ; Even on failure, clear local state
     :admin/navigate "/admin/login"}))

(rf/reg-event-fx
  :admin/logout-success
  (fn [_ _]
    {:admin/navigate "/admin/login"}))

(rf/reg-event-fx
  :admin/check-auth
  (fn [{:keys [db]} _]
    (let [token (or (:admin/token db)
                  (auth-persist/get-persisted-token))]
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
    ;; 1. [:admin/auth-valid response] - from :admin/check-auth (no on-auth-success)
    ;; 2. [:admin/auth-valid on-auth-success response] - from :admin/check-auth-protected
    (let [[on-auth-success response] (if (map? first-arg)
                                       [nil first-arg]
                                       [first-arg second-arg])
          current-admin (when (map? response)
                          (:current-admin response))
          admin-data (or current-admin (:admin/current-user db))
          role (some-> admin-data :role keyword)
          current-route (get-in db [:current-route :data :name])
          ;; Combine explicit extras with any pending callbacks queued
          ;; by check-auth-protected while auth was in progress.
          pending (get db :admin/pending-auth-callbacks [])
          extras (into (normalize-dispatch-events on-auth-success) pending)
          ;; The auth check already hit /admin/api/dashboard and got the response.
          ;; Reuse it instead of firing another load-dashboard API call.
          on-dashboard? (contains? #{:admin-dashboard :admin-dashboard-alt} current-route)
          has-dashboard-response? (and (map? response) (some? (:current-admin response)))
          ;; Replace :admin/load-dashboard in extras with :admin/dashboard-loaded
          rewritten-extras (if (and has-dashboard-response? on-dashboard?)
                             (->> extras
                               (remove #(= (first %) :admin/load-dashboard))
                               (into [[:admin/dashboard-loaded response]]))
                             extras)
          ;; For non-protected auth (check-auth with no extras), add route-events
          route-events (if (seq extras)
                         []
                         (cond
                           (and on-dashboard? has-dashboard-response?)
                           [[:admin/dashboard-loaded response]]
                           on-dashboard? [[:admin/load-dashboard]]
                           (= current-route :admin-advanced-dashboard)
                           (if has-dashboard-response?
                             [[:admin/advanced-dashboard-loaded response]]
                             [[:admin/load-advanced-dashboard]])
                           :else []))]
      {:db (-> db
             (assoc :admin/authenticated? true)
             (cond-> current-admin (assoc :admin/current-user current-admin))
             (cond-> role (assoc :admin/current-user-role role))
             (dissoc :admin/auth-checking? :admin/pending-auth-callbacks))
       :dispatch-n (into [] (concat rewritten-extras route-events))})))

;; Immediate auth success event (when already authenticated).
;; Route-specific data loading is handled by the caller
;; (check-auth-protected dispatches on-auth-success extras),
;; so this event only needs to exist as a no-op hook for
;; any future global side effects on re-authentication.
(rf/reg-event-fx
  :admin/auth-success-immediate
  (fn [_ _]
    {}))

(rf/reg-event-fx
  :admin/auth-invalid
  (fn [{:keys [db]} _]
    ;; Clear persisted auth state
    (auth-persist/clear-auth-state!)
    {:db (-> db
           (dissoc :admin/authenticated? :admin/token :admin/current-user :admin/current-user-role :admin/auth-checking?))
     :admin/navigate "/admin/login"}))

;; Navigation event

;; Check auth for protected routes (doesn't redirect on login page)
(rf/reg-event-fx
  :admin/check-auth-protected
  (fn [{:keys [db]} [_ on-auth-success]]
    (let [token (or (:admin/token db)
                  (auth-persist/get-persisted-token))
          already-authenticated? (:admin/authenticated? db)
          auth-checking? (:admin/auth-checking? db)]

      (cond
        ;; Already authenticated, trigger success callback immediately
        already-authenticated?
        (let [extras (normalize-dispatch-events on-auth-success)]
          {:dispatch-n (into [] (concat extras [[:admin/auth-success-immediate]]))})

        ;; Already checking auth — queue the callbacks to be dispatched when auth completes.
        ;; Without this, on-auth-success events from route controllers are silently dropped.
        auth-checking?
        (let [extras (normalize-dispatch-events on-auth-success)]
          (if (seq extras)
            {:db (update db :admin/pending-auth-callbacks (fnil into []) extras)}
            {}))

        ;; Has token but not authenticated yet, validate it
        token
        {:db (-> db
               (assoc :admin/token token)
               (assoc :admin/auth-checking? true))
         :http-xhrio (admin-http/dashboard-request
                       {;; Pass-through payload with the original on-auth-success callbacks
                        :on-success (conj [:admin/auth-valid] (when (seq (normalize-dispatch-events on-auth-success))
                                                                on-auth-success))
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
  (fn [_ [_ to]]
    ;; Delegate to the shared router-aware navigation effect.
    ;; It supports:
    ;; - keywords/vectors (route-name navigation)
    ;; - string paths ("/admin/dashboard") via router match
    {:routing/push-state to}))

;; Message management moved to app.admin.frontend.events.users.template.messages

(rf/reg-event-db
  :admin/clear-error-message
  (fn [db _]
    (dissoc db :admin/error-message)))

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
             ;; Force fresh validation instead of trusting persisted state blindly.
             ;; Keep :admin/auth-checking? true so that check-auth-protected
             ;; (from the route controller) sees the guard and doesn't fire
             ;; a duplicate auth validation request.
             (dissoc :admin/authenticated?)
             (assoc :admin/auth-checking? true))
       ;; Always re-validate the token with the backend to avoid stale sessions
       :dispatch (when token [:admin/check-auth])})))

;; Theme management events
