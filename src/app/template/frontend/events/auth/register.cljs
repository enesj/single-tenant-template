(ns app.template.frontend.events.auth.register
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Email/Password Registration Events
;; ========================================================================

;; === Email/Password Registration (map-based, single definition) ===
(rf/reg-event-fx
  ids/register-user
  common-interceptors
  (fn [{:keys [db]} [{:keys [email full-name password confirm-password]}]]
    ;; Derive an effective password in case the primary field didn't bind correctly
    (let [effective-password (if (seq password) password confirm-password)
          ;; Normalize keys to the exact wire names the backend expects
          payload {:email email
                   :full-name full-name
                   :password effective-password}]
      ;; Debug: log payload going into register-user
      (js/console.log
        (str "register-user event payload "
          (js/JSON.stringify (clj->js payload))))
      {:db (assoc-in db [:session :loading?] true)
       :http-xhrio (http/post-request
                     {:uri "/api/v1/auth/register"
                      :params payload
                      :on-success [ids/register-user-success]
                      :on-failure [ids/register-user-failure]})})))

(rf/reg-event-fx
  ids/register-user-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Registration successful")
          verification-required? (get response :verification-required false)
          user (get response :user)]
      (if success?
        {:db (-> db
               (assoc-in [:session :loading?] false)
               (assoc-in [:session :registered?] true)
               (assoc-in [:session :verification-required?] verification-required?)
               (assoc-in [:session :user] user)
               (assoc-in [:session :registration-message] message)
               (update :session dissoc :error))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-success
                "Registration Successful"
                message]]]}
        {:db (-> db
               (assoc-in [:session :loading?] false)
               (assoc-in [:session :error] (get response :error "Registration failed")))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Registration Failed"
                (get response :error "Please try again.")]]]}))))

(rf/reg-event-fx
  ids/register-user-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [resp (:response response)
          ;; Prefer field-specific validation messages when present
          field-error (or (get-in resp [:details :email 0])
                        (get-in resp [:details :password 0])
                        (get-in resp [:details :full-name 0])
                        (get-in resp [:details :full_name 0]))
          error-message (or field-error (http/extract-error-message response))]
      (log/error "User registration failed:" error-message)
      {:db (-> db
             (assoc-in [:session :loading?] false)
             (assoc-in [:session :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Registration Failed"
              error-message]]]})))

