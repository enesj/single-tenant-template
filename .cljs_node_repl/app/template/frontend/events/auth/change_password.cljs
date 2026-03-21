(ns app.template.frontend.events.auth.change-password
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Change Password Events (Authenticated Users)
;; ========================================================================

;; Event to change password for authenticated user
(rf/reg-event-fx
  ids/change-password
  common-interceptors
  (fn [{:keys [db]} [_ current-password new-password]]
    {:db (-> db
           (assoc-in [:change-password :loading?] true)
           (update :change-password dissoc :error :success?))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/auth/change-password"
                    :params {:current-password current-password
                             :new-password new-password}
                    :on-success [ids/change-password-success]
                    :on-failure [ids/change-password-failure]})}))

;; Handle successful password change
(rf/reg-event-fx
  ids/change-password-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Password changed successfully")]
      (if success?
        (do
          (log/info "Password change successful")
          {:db (-> db
                 (assoc-in [:change-password :loading?] false)
                 (assoc-in [:change-password :success?] true)
                 (assoc-in [:change-password :message] message)
                 (update :change-password dissoc :error))
           :fx [[:dispatch
                 [:app.template.frontend.events.messages/show-success
                  "Password Changed"
                  message]]]})
        {:db (-> db
               (assoc-in [:change-password :loading?] false)
               (assoc-in [:change-password :error] (get response :error "Password change failed")))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Change Failed"
                (get response :error "Please try again.")]]]}))))

;; Handle password change failure
(rf/reg-event-fx
  ids/change-password-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [resp (:response response)
          field-error (or (get-in resp [:details :current-password 0])
                        (get-in resp [:details :new-password 0]))
          error-message (or field-error (http/extract-error-message response))]
      (log/error "Password change failed:" error-message)
      {:db (-> db
             (assoc-in [:change-password :loading?] false)
             (assoc-in [:change-password :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Change Failed"
              error-message]]]})))

