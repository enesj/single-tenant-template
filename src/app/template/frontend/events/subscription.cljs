(ns app.template.frontend.events.subscription
  "Subscription-related events for re-frame"
  (:require
    [app.template.frontend.api.http :as http]
    [re-frame.core :as rf]))

;; ============================================================================
;; Event Handlers
;; ============================================================================

(rf/reg-event-db
  :subscription/set-loading
  (fn [db [_ loading?]]
    (assoc-in db [:subscription :loading?] loading?)))

(rf/reg-event-db
  :subscription/set-error
  (fn [db [_ error]]
    (assoc-in db [:subscription :error] error)))

(rf/reg-event-db
  :subscription/clear-error
  (fn [db _]
    (assoc-in db [:subscription :error] nil)))

;; ============================================================================
;; Subscription Status Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/fetch-status
  (fn [{:keys [db]} _]
    {:dispatch [:subscription/set-loading true]
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/subscription/status"
                    :on-success [:subscription/fetch-status-success]
                    :on-failure [:subscription/fetch-status-failure]})}))

(rf/reg-event-fx
  :subscription/fetch-status-success
  (fn [{:keys [db]} [_ response]]
    {:db (-> db
           (assoc-in [:subscription :status] response)
           (assoc-in [:subscription :loading?] false))
     :dispatch [:subscription/clear-error]}))

(rf/reg-event-fx
  :subscription/fetch-status-failure
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch [:subscription/set-error "Failed to load subscription status"]}))

;; ============================================================================
;; Subscription Tiers Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/fetch-tiers
  (fn [_ _]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/subscription/tiers"
                    :on-success [:subscription/fetch-tiers-success]
                    :on-failure [:subscription/fetch-tiers-failure]})}))

(rf/reg-event-db
  :subscription/fetch-tiers-success
  (fn [db [_ response]]
    (assoc-in db [:subscription :tiers] (:tiers response))))

(rf/reg-event-fx
  :subscription/fetch-tiers-failure
  (fn [_ [_ _]]
    {:dispatch [:subscription/set-error "Failed to load subscription tiers"]}))

;; ============================================================================
;; Subscription Creation Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/create
  (fn [{:keys [db]} [_ subscription-request]]
    {:dispatch [:subscription/set-loading true]
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/subscription/create"
                    :params subscription-request
                    :on-success [:subscription/create-success]
                    :on-failure [:subscription/create-failure]})}))

(rf/reg-event-fx
  :subscription/create-success
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch-n [[:subscription/fetch-status]
                  [:subscription/clear-error]
                  [:notification/show {:type :success
                                       :message "Subscription created successfully!"}]]}))

(rf/reg-event-fx
  :subscription/create-failure
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch [:subscription/set-error "Failed to create subscription"]}))

;; ============================================================================
;; Subscription Update Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/update
  (fn [{:keys [db]} [_ update-request]]
    {:dispatch [:subscription/set-loading true]
     :http-xhrio (http/api-request
                   {:method :put
                    :uri "/api/v1/subscription/update"
                    :params update-request
                    :on-success [:subscription/update-success]
                    :on-failure [:subscription/update-failure]})}))

(rf/reg-event-fx
  :subscription/update-success
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch-n [[:subscription/fetch-status]
                  [:subscription/clear-error]
                  [:notification/show {:type :success
                                       :message "Subscription updated successfully!"}]]}))

(rf/reg-event-fx
  :subscription/update-failure
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch [:subscription/set-error "Failed to update subscription"]}))

;; ============================================================================
;; Subscription Cancellation Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/cancel
  (fn [{:keys [db]} [_ immediate?]]
    {:dispatch [:subscription/set-loading true]
     :http-xhrio (http/api-request
                   {:method :delete
                    :uri (str "/api/v1/subscription/cancel"
                           (when immediate? "?immediate=true"))
                    :on-success [:subscription/cancel-success]
                    :on-failure [:subscription/cancel-failure]})}))

(rf/reg-event-fx
  :subscription/cancel-success
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch-n [[:subscription/fetch-status]
                  [:subscription/clear-error]
                  [:notification/show {:type :success
                                       :message "Subscription cancelled successfully!"}]]}))

(rf/reg-event-fx
  :subscription/cancel-failure
  (fn [{:keys [db]} [_ _]]
    {:db (assoc-in db [:subscription :loading?] false)
     :dispatch [:subscription/set-error "Failed to cancel subscription"]}))

;; ============================================================================
;; Usage Tracking Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/check-usage-limit
  (fn [_ [_ metric current-value]]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri (str "/api/v1/subscription/usage-check"
                           "?metric=" (name metric)
                           "&current_value=" current-value)
                    :on-success [:subscription/check-usage-limit-success metric]
                    :on-failure [:subscription/check-usage-limit-failure]})}))

(rf/reg-event-db
  :subscription/check-usage-limit-success
  (fn [db [_ metric response]]
    (assoc-in db [:subscription :usage-checks metric] response)))

(rf/reg-event-fx
  :subscription/check-usage-limit-failure
  (fn [_ [_ _]]
    {:dispatch [:subscription/set-error "Failed to check usage limits"]}))

(rf/reg-event-db
  :subscription/update-usage
  (fn [db [_ usage-data]]
    (assoc-in db [:subscription :usage] usage-data)))

;; ============================================================================
;; UI Events
;; ============================================================================

(rf/reg-event-db
  :subscription/show-upgrade-modal
  (fn [db _]
    (assoc-in db [:subscription :ui :show-upgrade-modal?] true)))

(rf/reg-event-db
  :subscription/hide-upgrade-modal
  (fn [db _]
    (assoc-in db [:subscription :ui :show-upgrade-modal?] false)))

(rf/reg-event-db
  :subscription/show-billing-modal
  (fn [db _]
    (assoc-in db [:subscription :ui :show-billing-modal?] true)))

(rf/reg-event-db
  :subscription/hide-billing-modal
  (fn [db _]
    (assoc-in db [:subscription :ui :show-billing-modal?] false)))

;; ============================================================================
;; Initialization Events
;; ============================================================================

(rf/reg-event-fx
  :subscription/initialize
  (fn [_ _]
    {:dispatch-n [[:subscription/fetch-status]
                  [:subscription/fetch-tiers]]}))
