(ns app.template.frontend.events.auth.login
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Email/Password Login Events
;; ========================================================================

;; Event to handle user login with email and password
(rf/reg-event-fx
  ids/login-with-password
  common-interceptors
  (fn [{:keys [db]} [email password]]
    {:db (assoc-in db [:session :loading?] true)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/auth/login"
                    :params {:email email :password password}
                    :on-success [ids/login-with-password-success]
                    :on-failure [ids/login-with-password-failure]})}))

;; Handle successful email/password login
(rf/reg-event-fx
  ids/login-with-password-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          user (get response :user)
          message (get response :message "Login successful")]

      (if success?
        (do
          (log/info "Email/password login successful for user:" (:email user))
          ;; Trigger auth status refresh to get complete session data
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :login-success?] true)
                 (assoc-in [:session :login-message] message)
                 ;; Clear any previous errors
                 (update :session dissoc :error))
           :fx [[:dispatch [ids/fetch-auth-status]
                 [:dispatch [:app.template.frontend.events.messages/show-success]
                  message
                  "Welcome back!"]]]})
        (do
          (log/warn "Email/password login failed:" response)
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :error] (get response :error "Invalid email or password")))
           :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
                 "Login Failed"
                 (get response :error "Invalid email or password")]]})))))

;; Handle email/password login failure
(rf/reg-event-fx
  ids/login-with-password-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [resp (:response response)
          field-error (or (get-in resp [:details :email 0])
                        (get-in resp [:details :password 0]))
          error-message (or field-error (http/extract-error-message response))]
      (log/error "Email/password login failed:" error-message)
      {:db (-> db
             (assoc-in [:session :loading?] false)
             (assoc-in [:session :error] error-message))
       :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
             "Login Failed"
             error-message]]})))

