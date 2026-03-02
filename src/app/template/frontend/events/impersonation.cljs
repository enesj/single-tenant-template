(ns app.template.frontend.events.impersonation
  "Re-frame events and subscriptions for impersonation grant management.
   Tenant owners can list, create, and revoke impersonation grants."
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub
  :impersonation/grants
  (fn [db _]
    (get-in db [:impersonation :grants])))

(rf/reg-sub
  :impersonation/loading?
  (fn [db _]
    (get-in db [:impersonation :loading?] false)))

(rf/reg-sub
  :impersonation/error
  (fn [db _]
    (get-in db [:impersonation :error])))

(rf/reg-sub
  :impersonation/success-message
  (fn [db _]
    (get-in db [:impersonation :success-message])))

;; ============================================================================
;; Fetch Grants
;; ============================================================================

(rf/reg-event-fx
  ::fetch-grants
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:impersonation :loading?] true)
           (assoc-in [:impersonation :error] nil))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/tenant/impersonation-grants"
                    :on-success [::fetch-grants-success]
                    :on-failure [::fetch-grants-failure]})}))

(rf/reg-event-db
  ::fetch-grants-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:impersonation :loading?] false)
      (assoc-in [:impersonation :grants] (:grants response)))))

(rf/reg-event-db
  ::fetch-grants-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to fetch impersonation grants:" response)
    (-> db
      (assoc-in [:impersonation :loading?] false)
      (assoc-in [:impersonation :error]
        (or (get-in response [:response :message])
          "Failed to fetch impersonation grants")))))

;; ============================================================================
;; Create Grant
;; ============================================================================

(rf/reg-event-fx
  ::create-grant
  common-interceptors
  (fn [{:keys [db]} [{:keys [admin-email role]}]]
    {:db (-> db
           (assoc-in [:impersonation :loading?] true)
           (assoc-in [:impersonation :error] nil)
           (assoc-in [:impersonation :success-message] nil))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/tenant/impersonation-grants"
                    :params {:admin-email admin-email :role (or role "viewer")}
                    :on-success [::create-grant-success]
                    :on-failure [::create-grant-failure]})}))

(rf/reg-event-fx
  ::create-grant-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (-> db
           (assoc-in [:impersonation :loading?] false)
           (assoc-in [:impersonation :success-message] "Impersonation grant created"))
     :fx [[:dispatch [::fetch-grants]]]}))

(rf/reg-event-db
  ::create-grant-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to create impersonation grant:" response)
    (-> db
      (assoc-in [:impersonation :loading?] false)
      (assoc-in [:impersonation :error]
        (or (get-in response [:response :message])
          (get-in response [:response :errors :admin-email 0])
          "Failed to create impersonation grant")))))

;; ============================================================================
;; Revoke Grant
;; ============================================================================

(rf/reg-event-fx
  ::revoke-grant
  common-interceptors
  (fn [{:keys [db]} [{:keys [id]}]]
    {:db (-> db
           (assoc-in [:impersonation :error] nil)
           (assoc-in [:impersonation :success-message] nil))
     :http-xhrio (http/api-request
                   {:method :delete
                    :uri (str "/api/v1/tenant/impersonation-grants/" id)
                    :on-success [::revoke-grant-success]
                    :on-failure [::revoke-grant-failure]})}))

(rf/reg-event-fx
  ::revoke-grant-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:impersonation :success-message] "Impersonation grant revoked")
     :fx [[:dispatch [::fetch-grants]]]}))

(rf/reg-event-db
  ::revoke-grant-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to revoke impersonation grant:" response)
    (assoc-in db [:impersonation :error]
              (or (get-in response [:response :message])
                "Failed to revoke impersonation grant"))))

;; ============================================================================
;; Clear messages
;; ============================================================================

(rf/reg-event-db
  ::clear-messages
  common-interceptors
  (fn [db _]
    (-> db
      (update :impersonation dissoc :error :success-message))))

(comment
  ;; (require 'app.template.frontend.events.impersonation :reload)
  :rcf)
