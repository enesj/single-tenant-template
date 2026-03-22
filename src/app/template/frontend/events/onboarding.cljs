(ns app.template.frontend.events.onboarding
  "Re-frame events for workspace onboarding:
   fetch progress, complete/skip steps, dismiss checklist."
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub
  :onboarding/data
  (fn [db _]
    (get db :onboarding)))

(rf/reg-sub
  :onboarding/progress
  :<- [:onboarding/data]
  (fn [data _]
    (:progress data)))

(rf/reg-sub
  :onboarding/steps
  :<- [:onboarding/progress]
  (fn [progress _]
    (:steps progress)))

(rf/reg-sub
  :onboarding/differences
  :<- [:onboarding/data]
  (fn [data _]
    (:differences data)))

(rf/reg-sub
  :onboarding/loading?
  :<- [:onboarding/data]
  (fn [data _]
    (:loading? data false)))

(rf/reg-sub
  :onboarding/error
  :<- [:onboarding/data]
  (fn [data _]
    (:error data)))

(rf/reg-sub
  :onboarding/dismissed?
  :<- [:onboarding/progress]
  (fn [progress _]
    (:dismissed progress false)))

(rf/reg-sub
  :onboarding/active-step
  :<- [:onboarding/steps]
  (fn [steps _]
    (first (filter #(= "pending" (:status %)) steps))))

(rf/reg-sub
  :onboarding/completed-count
  :<- [:onboarding/steps]
  (fn [steps _]
    (count (filter #(= "completed" (:status %)) steps))))

(rf/reg-sub
  :onboarding/total-count
  :<- [:onboarding/steps]
  (fn [steps _]
    (count steps)))

(rf/reg-sub
  :onboarding/all-done?
  :<- [:onboarding/steps]
  (fn [steps _]
    (and (seq steps)
      (every? #(not= "pending" (:status %)) steps))))

;; Summary from auth-status (lightweight, always available)
(rf/reg-sub
  :onboarding/summary
  (fn [db _]
    (get-in db [:session :onboarding])))

(rf/reg-sub
  :onboarding/active?
  :<- [:onboarding/summary]
  (fn [summary _]
    (:active? summary false)))

(rf/reg-sub
  :onboarding/redirect?
  :<- [:onboarding/summary]
  (fn [summary _]
    (:redirect-to-onboarding? summary false)))

;; ============================================================================
;; Fetch Progress
;; ============================================================================

(rf/reg-event-fx
  ::fetch-progress
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:onboarding :loading?] true)
           (assoc-in [:onboarding :error] nil))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/onboarding/progress"
                    :on-success [::fetch-progress-success]
                    :on-failure [::fetch-progress-failure]})}))

(rf/reg-event-db
  ::fetch-progress-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:onboarding :loading?] false)
      (assoc-in [:onboarding :progress] (:progress response))
      (assoc-in [:onboarding :differences] (:differences response)))))

(rf/reg-event-db
  ::fetch-progress-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to fetch onboarding progress:" response)
    (-> db
      (assoc-in [:onboarding :loading?] false)
      (assoc-in [:onboarding :error] "Failed to load onboarding"))))

;; ============================================================================
;; Complete Step
;; ============================================================================

(rf/reg-event-fx
  ::complete-step
  common-interceptors
  (fn [{:keys [db]} [{:keys [step-kind]}]]
    {:db (assoc-in db [:onboarding :step-loading?] step-kind)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri (str "/api/v1/onboarding/steps/" step-kind "/complete")
                    :params {}
                    :on-success [::step-action-success]
                    :on-failure [::step-action-failure]})}))

;; ============================================================================
;; Skip Step
;; ============================================================================

(rf/reg-event-fx
  ::skip-step
  common-interceptors
  (fn [{:keys [db]} [{:keys [step-kind]}]]
    {:db (assoc-in db [:onboarding :step-loading?] step-kind)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri (str "/api/v1/onboarding/steps/" step-kind "/skip")
                    :params {}
                    :on-success [::step-action-success]
                    :on-failure [::step-action-failure]})}))

;; ============================================================================
;; Skip All
;; ============================================================================

(rf/reg-event-fx
  ::skip-all
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:onboarding :loading?] true)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/onboarding/skip-all"
                    :params {}
                    :on-success [::skip-all-success]
                    :on-failure [::step-action-failure]})}))

(rf/reg-event-fx
  ::skip-all-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:onboarding :loading?] false)
     :fx [[:dispatch [::fetch-progress]]]}))

;; ============================================================================
;; Dismiss
;; ============================================================================

(rf/reg-event-fx
  ::dismiss
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:onboarding :loading?] true)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/onboarding/dismiss"
                    :params {}
                    :on-success [::dismiss-success]
                    :on-failure [::step-action-failure]})}))

(rf/reg-event-fx
  ::dismiss-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:onboarding :loading?] false)
           (assoc-in [:session :onboarding :active?] false)
           (assoc-in [:session :onboarding :dismissed] true)
           (assoc-in [:session :onboarding :redirect-to-onboarding?] false))
     :fx [[:dispatch [:navigate-to "/dashboard"]]]}))

;; ============================================================================
;; Shared Success/Failure for Step Actions
;; ============================================================================

(rf/reg-event-fx
  ::step-action-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:onboarding :step-loading?] nil)
     :fx [[:dispatch [::fetch-progress]]]}))

(rf/reg-event-db
  ::step-action-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Onboarding step action failed:" response)
    (-> db
      (assoc-in [:onboarding :step-loading?] nil)
      (assoc-in [:onboarding :loading?] false)
      (assoc-in [:onboarding :error]
        (or (get-in response [:response :message])
          "Action failed")))))

(comment
  ;; (require 'app.template.frontend.events.onboarding :reload)
  :rcf)
