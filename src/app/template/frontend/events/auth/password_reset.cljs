(ns app.template.frontend.events.auth.password-reset
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Password Reset Events (Forgot Password Flow)
;; ========================================================================

;; Event to request password reset (forgot password form submission)
(rf/reg-event-fx
  ids/request-password-reset
  common-interceptors
  (fn [{:keys [db]} [email]]
    (let [request-params {:email email}]
      (log/info "Password reset request dispatched")
      {:db (-> db
             (assoc-in [:password-reset :loading?] true)
             (update :password-reset dissoc :error :success?))
       :http-xhrio (http/api-request
                     {:method :post
                      :uri "/api/v1/auth/forgot-password"
                      :params request-params
                      :on-success [ids/request-password-reset-success]
                      :on-failure [ids/request-password-reset-failure]})})))

;; Handle successful password reset request
(rf/reg-event-fx
  ids/request-password-reset-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [message (get response :message "Password reset instructions sent")]
      (log/info "Password reset request successful")
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :success?] true)
             (assoc-in [:password-reset :message] message)
             (update :password-reset dissoc :error))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-success
              "Email Sent"
              message]]]})))

;; Handle password reset request failure
(rf/reg-event-fx
  ids/request-password-reset-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Password reset request failed:" error-message)
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Request Failed"
              error-message]]]})))

;; Event to verify reset token
(rf/reg-event-fx
  ids/verify-reset-token
  common-interceptors
  (fn [{:keys [db]} [token]]
    {:db (-> db
           (assoc-in [:password-reset :loading?] true)
           (update :password-reset dissoc :error))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri (str "/api/v1/auth/verify-reset-token?token=" token)
                    :on-success [ids/verify-reset-token-success]
                    :on-failure [ids/verify-reset-token-failure]})}))

;; Handle successful token verification
(rf/reg-event-fx
  ids/verify-reset-token-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [valid? (get response :valid false)]
      (if valid?
        {:db (-> db
               (assoc-in [:password-reset :loading?] false)
               (assoc-in [:password-reset :token-verified?] true)
               (update :password-reset dissoc :error))}
        {:db (-> db
               (assoc-in [:password-reset :loading?] false)
               (assoc-in [:password-reset :token-verified?] false)
               (assoc-in [:password-reset :error] "Invalid or expired reset link"))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Invalid Link"
                "This password reset link is invalid or has expired."]]]}))))

;; Handle token verification failure
(rf/reg-event-fx
  ids/verify-reset-token-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Reset token verification failed:" error-message)
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :token-verified?] false)
             (assoc-in [:password-reset :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Invalid Link"
              "This password reset link is invalid or has expired."]]]})))

;; Event to reset password with token
(rf/reg-event-fx
  ids/reset-password-with-token
  common-interceptors
  (fn [{:keys [db]} [token new-password]]
    {:db (-> db
           (assoc-in [:password-reset :loading?] true)
           (update :password-reset dissoc :error :success?))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/auth/reset-password"
                    :params {:token token :new-password new-password}
                    :on-success [ids/reset-password-with-token-success]
                    :on-failure [ids/reset-password-with-token-failure]})}))

;; Handle successful password reset
(rf/reg-event-fx
  ids/reset-password-with-token-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Password reset successful")]
      (if success?
        (do
          (log/info "Password reset successful")
          {:db (-> db
                 (assoc-in [:password-reset :loading?] false)
                 (assoc-in [:password-reset :success?] true)
                 (assoc-in [:password-reset :message] message)
                 (update :password-reset dissoc :error))
           :fx [[:dispatch
                 [:app.template.frontend.events.messages/show-success
                  "Password Reset"
                  message]]]})
        {:db (-> db
               (assoc-in [:password-reset :loading?] false)
               (assoc-in [:password-reset :error] (get response :error "Password reset failed")))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Reset Failed"
                (get response :error "Please try again.")]]]}))))

;; Handle password reset failure
(rf/reg-event-fx
  ids/reset-password-with-token-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Password reset failed:" error-message)
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Reset Failed"
              error-message]]]})))

