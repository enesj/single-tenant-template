(ns app.admin.frontend.events.password
  "Admin password reset events"
  (:require
    [ajax.core :as ajax]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Helper - Public (no-auth) request builder
;; ============================================================================

(defn- public-request
  "Creates a request config for public endpoints (no auth required)"
  [{:keys [method uri params on-success on-failure]}]
  {:method method
   :uri uri
   :params params
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :timeout 10000
   :on-success on-success
   :on-failure on-failure})

;; ============================================================================
;; Forgot Password Events
;; ============================================================================

(rf/reg-event-fx
  :admin/request-password-reset
  (fn [{:keys [db]} [_ email]]
    {:db (-> db
           (assoc :admin/password-reset-loading? true)
           (dissoc :admin/password-reset-error :admin/password-reset-success?))
     :http-xhrio (public-request
                   {:method :post
                    :uri "/admin/api/auth/forgot-password"
                    :params {:email email}
                    :on-success [:admin/request-password-reset-success]
                    :on-failure [:admin/request-password-reset-failure]})}))

(rf/reg-event-db
  :admin/request-password-reset-success
  (fn [db [_ _response]]
    (log/info "Admin password reset request successful")
    (-> db
      (assoc :admin/password-reset-loading? false)
      (assoc :admin/password-reset-success? true)
      (dissoc :admin/password-reset-error))))

(rf/reg-event-db
  :admin/request-password-reset-failure
  (fn [db [_ error]]
    (log/error "Admin password reset request failed:" error)
    (-> db
      (assoc :admin/password-reset-loading? false)
      (assoc :admin/password-reset-error
        (or (get-in error [:response :error])
          "Failed to send reset email. Please try again.")))))

;; ============================================================================
;; Verify Reset Token Events
;; ============================================================================

(rf/reg-event-fx
  :admin/verify-reset-token
  (fn [{:keys [db]} [_ token]]
    {:db (-> db
           (assoc :admin/password-reset-loading? true)
           (dissoc :admin/password-reset-error :admin/password-reset-token-verified?))
     :http-xhrio (public-request
                   {:method :get
                    :uri (str "/admin/api/auth/verify-reset-token?token=" token)
                    :on-success [:admin/verify-reset-token-success]
                    :on-failure [:admin/verify-reset-token-failure]})}))

(rf/reg-event-db
  :admin/verify-reset-token-success
  (fn [db [_ response]]
    (let [valid? (:valid response)]
      (log/info "Admin reset token verification:" (if valid? "valid" "invalid"))
      (-> db
        (assoc :admin/password-reset-loading? false)
        (assoc :admin/password-reset-token-verified? valid?)
        (cond-> (not valid?)
          (assoc :admin/password-reset-error (or (:error response) "Invalid or expired reset link")))))))

(rf/reg-event-db
  :admin/verify-reset-token-failure
  (fn [db [_ error]]
    (log/error "Admin reset token verification failed:" error)
    (-> db
      (assoc :admin/password-reset-loading? false)
      (assoc :admin/password-reset-token-verified? false)
      (assoc :admin/password-reset-error "Failed to verify reset link"))))

;; ============================================================================
;; Reset Password Events
;; ============================================================================

(rf/reg-event-fx
  :admin/reset-password-with-token
  (fn [{:keys [db]} [_ token password]]
    {:db (-> db
           (assoc :admin/password-reset-loading? true)
           (dissoc :admin/password-reset-error :admin/password-reset-success?))
     :http-xhrio (public-request
                   {:method :post
                    :uri "/admin/api/auth/reset-password"
                    :params {:token token :password password}
                    :on-success [:admin/reset-password-with-token-success]
                    :on-failure [:admin/reset-password-with-token-failure]})}))

(rf/reg-event-db
  :admin/reset-password-with-token-success
  (fn [db [_ response]]
    (if (:success response)
      (do
        (log/info "Admin password reset successful")
        (-> db
          (assoc :admin/password-reset-loading? false)
          (assoc :admin/password-reset-success? true)
          (dissoc :admin/password-reset-error)))
      (-> db
        (assoc :admin/password-reset-loading? false)
        (assoc :admin/password-reset-error (or (:error response) "Password reset failed"))))))

(rf/reg-event-db
  :admin/reset-password-with-token-failure
  (fn [db [_ error]]
    (log/error "Admin password reset failed:" error)
    (-> db
      (assoc :admin/password-reset-loading? false)
      (assoc :admin/password-reset-error
        (or (get-in error [:response :error])
          "Failed to reset password. Please try again.")))))

;; ============================================================================
;; Clear State Event
;; ============================================================================

(rf/reg-event-db
  :admin/clear-password-reset-state
  (fn [db _]
    (dissoc db
      :admin/password-reset-loading?
      :admin/password-reset-error
      :admin/password-reset-success?
      :admin/password-reset-token-verified?)))

;; ============================================================================
;; Initialize Events
;; ============================================================================

(rf/reg-event-db
  :admin/init-forgot-password
  (fn [db _]
    (dissoc db
      :admin/password-reset-loading?
      :admin/password-reset-error
      :admin/password-reset-success?
      :admin/password-reset-token-verified?)))

(rf/reg-event-db
  :admin/init-reset-password
  (fn [db _]
    (dissoc db
      :admin/password-reset-loading?
      :admin/password-reset-error
      :admin/password-reset-success?
      :admin/password-reset-token-verified?)))
