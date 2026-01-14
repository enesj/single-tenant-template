(ns app.template.frontend.events.auth.pages
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]))

;; ========================================================================
;; Page Initialization Events
;; ========================================================================

;; Page initialization events for login and logout
(rf/reg-event-fx
  :page/init-login
  common-interceptors
  (fn [{:keys [db]} _]
    (let [current-auth-status (get-in db [:session :authenticated?])
          loading? (get-in db [:session :loading?])]
      ;; If already authenticated, redirect to entities page
      (if (and current-auth-status (not loading?))
        {:db (assoc-in db (paths/current-page) :login)
         :redirect "/entities"}
        ;; Otherwise, fetch auth status and show login page
        {:db (-> db
               (assoc-in (paths/current-page) :login))
         :fx [[:dispatch [ids/fetch-auth-status]]]}))))

(rf/reg-event-fx
  :page/init-logout
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (paths/current-page) :logout))
     :fx [[:dispatch [ids/fetch-auth-status]]]}))

;; Page initialization for registration
(rf/reg-event-fx
  :page/init-register
  common-interceptors
  (fn [{:keys [db]} _]
    (let [current-auth-status (get-in db [:session :authenticated?])
          loading? (get-in db [:session :loading?])]
      ;; If already authenticated, redirect to entities page
      (if (and current-auth-status (not loading?))
        {:db (assoc-in db (paths/current-page) :register)
         :redirect "/entities"}
        ;; Otherwise, clear auth state and show registration page
        {:db (-> db
               (assoc-in (paths/current-page) :register))
         :fx [[:dispatch [ids/clear-auth-state]]]}))))

;; Page initialization for email verification
(rf/reg-event-fx
  :page/init-verify-email
  common-interceptors
  (fn [{:keys [db]} [_ token]]
    (let [loading? (get-in db [:session :loading?])]
      ;; If not loading, process verification
      (if-not loading?
        {:db (assoc-in db (paths/current-page) :verify-email)
         :fx [(when token
                [:dispatch [ids/verify-email token]])]}
        ;; Show loading state
        {:db (assoc-in db [:session :loading?] true)}))))

;; ========================================================================
;; Password Reset Events (Forgot Password Flow)
;; ========================================================================

;; Page initialization for forgot password
(rf/reg-event-fx
  :page/init-forgot-password
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (paths/current-page) :forgot-password)
           (assoc-in [:password-reset :loading?] false)
           (assoc-in [:password-reset :success?] false)
           (update :password-reset dissoc :error))}))

;; Page initialization for reset password (with token)
(rf/reg-event-fx
  :page/init-reset-password
  common-interceptors
  (fn [{:keys [db]} [token]]
    (let [;; If token is not passed as argument, try to get it from URL
          url-token (when (and (exists? js/window) (exists? js/URLSearchParams))
                      (-> js/window .-location .-search
                        (js/URLSearchParams.)
                        (.get "token")))
          effective-token (or token url-token)]
      {:db (-> db
             (assoc-in (paths/current-page) :reset-password)
             (assoc-in [:password-reset :token] effective-token)
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :token-verified?] false)
             (update :password-reset dissoc :error :success?))
       :fx [(when effective-token
              [:dispatch [ids/verify-reset-token effective-token]])]})))

;; Page initialization for change password (authenticated users)
(rf/reg-event-fx
  :page/init-change-password
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (paths/current-page) :change-password)
           (update :change-password dissoc :loading? :error :success? :message))}))
