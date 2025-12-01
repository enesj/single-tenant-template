(ns app.template.frontend.subs.subscription
  (:require
    [re-frame.core :as rf]))

;; ============================================================================
;; Subscription Status Subscriptions
;; ============================================================================

(rf/reg-sub
  :subscription/status
  (fn [db _]
    (get-in db [:subscription :status])))

(rf/reg-sub
  :subscription/tier
  (fn [db _]
    (get-in db [:subscription :status :subscription-tier] :free)))

(rf/reg-sub
  :subscription/limits
  (fn [db _]
    (or (get-in db [:subscription :status :limits])
      (get-in db [:subscription :limits]))))

(rf/reg-sub
  :subscription/usage
  (fn [db _]
    (get-in db [:subscription :usage])))

(rf/reg-sub
  :subscription/trial-ends-at
  (fn [db _]
    (get-in db [:subscription :status :trial-ends-at])))

(rf/reg-sub
  :subscription/is-trial?
  :<- [:subscription/status]
  (fn [status _]
    (= :trialing (:subscription-status status))))

(rf/reg-sub
  :subscription/is-active?
  :<- [:subscription/status]
  (fn [status _]
    (= :active (:subscription-status status))))

(rf/reg-sub
  :subscription/is-past-due?
  :<- [:subscription/status]
  (fn [status _]
    (= :past-due (:subscription-status status))))

(rf/reg-sub
  :subscription/available-tiers
  (fn [db _]
    (get-in db [:subscription :tiers])))

;; ============================================================================
;; Usage Limit Subscriptions
;; ============================================================================

(rf/reg-sub
  :subscription/usage-percentage
  :<- [:subscription/limits]
  :<- [:subscription/usage]
  (fn [[limits usage] [_ metric]]
    (when (and limits usage)
      (let [limit (get limits metric)
            current (get usage metric 0)]
        (if (and limit (> limit 0))
          (* (/ current limit) 100)
          0)))))

(rf/reg-sub
  :subscription/is-usage-limit-exceeded?
  :<- [:subscription/limits]
  :<- [:subscription/usage]
  (fn [[limits usage] [_ metric]]
    (when (and limits usage)
      (let [limit (get limits metric)
            current (get usage metric 0)]
        (and limit (>= current limit))))))

(rf/reg-sub
  :subscription/usage-warning?
  :<- [:subscription/limits]
  :<- [:subscription/usage]
  (fn [[limits usage] [_ metric]]
    (when (and limits usage)
      (let [limit (get limits metric)
            current (get usage metric 0)]
        (and limit (> (if (pos? (or limit 0)) (* (/ current limit) 100) 0) 80))))))

;; ============================================================================
;; UI State Subscriptions
;; ============================================================================

(rf/reg-sub
  :subscription/loading?
  (fn [db _]
    (get-in db [:subscription :loading?] false)))

(rf/reg-sub
  :subscription/error
  (fn [db _]
    (get-in db [:subscription :error])))

(rf/reg-sub
  :subscription/show-upgrade-modal?
  (fn [db _]
    (get-in db [:subscription :ui :show-upgrade-modal?] false)))

(rf/reg-sub
  :subscription/show-billing-modal?
  (fn [db _]
    (get-in db [:subscription :ui :show-billing-modal?] false)))
