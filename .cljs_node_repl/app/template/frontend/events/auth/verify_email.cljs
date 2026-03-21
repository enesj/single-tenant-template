(ns app.template.frontend.events.auth.verify-email
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Email Verification Events
;; ========================================================================

;; Event to handle email verification (from link in email)
(rf/reg-event-fx
  ids/verify-email
  common-interceptors
  (fn [{:keys [db]} [_ token]]
    {:db (assoc-in db [:session :loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri (str "/api/v1/auth/verify-email?token=" token)
                    :on-success [ids/verify-email-success]
                    :on-failure [ids/verify-email-failure]})}))

;; Handle successful email verification
(rf/reg-event-fx
  ids/verify-email-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Email verification successful")]

      (if success?
        (do
          (log/info "Email verification successful")
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :email-verified?] true)
                 (assoc-in [:session :verification-message] message)
                 ;; Clear any previous errors
                 (update :session dissoc :error))
           :fx [[:dispatch [:app.template.frontend.events.messages/show-success]
                 "Email Verified"
                 message]]})
        (do
          (log/warn "Email verification failed:" response)
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :error] (get response :error "Verification failed")))
           :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
                 "Verification Failed"
                 (get response :error "Invalid or expired verification link")]]})))))

;; Handle email verification failure
(rf/reg-event-fx
  ids/verify-email-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Email verification failed:" error-message)
      {:db (-> db
             (assoc-in [:session :loading?] false)
             (assoc-in [:session :error] error-message))
       :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
             "Verification Failed"
             error-message]]})))

