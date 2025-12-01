(ns app.template.frontend.components.subscription
  "Subscription management UI components"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :as icons]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Subscription Status Badge
;; ============================================================================

(defui subscription-status-badge
  "Display subscription status with appropriate styling"
  [{:keys [status]}]
  (let [status-config (case status
                        :trialing {:class "ds-badge-info" :text "Trial"}
                        :active {:class "ds-badge-success" :text "Active"}
                        :past-due {:class "ds-badge-warning" :text "Past Due"}
                        :cancelled {:class "ds-badge-error" :text "Cancelled"}
                        {:class "ds-badge-ghost" :text "Unknown"})]
    ($ :span
      {:id "subscription-status-badge"
       :class (str "ds-badge " (:class status-config))}
      (:text status-config))))

;; ============================================================================
;; Subscription Tier Display
;; ============================================================================

(defui subscription-tier-card
  "Display current subscription tier with features"
  [{:keys [tier-config]}]
  (when tier-config
    ($ :div.card.bg-base-100.shadow-md
      {:id "subscription-tier-card"}
      ($ :div.card-body
        ($ :h3.card-title.flex.items-center
          ($ icons/crown {:class "w-5 h-5 mr-2 text-yellow-500"})
          (:name tier-config))
        ($ :p.text-sm.text-base-content.opacity-70
          (:description tier-config))
        ($ :div.mt-4
          ($ :h4.font-semibold.mb-2 "Features:")
          ($ :ul.list-disc.list-inside.space-y-1
            (map (fn [feature]
                   ($ :li.text-sm {:key feature} feature))
              (:features tier-config))))
        (when-let [pricing (:pricing tier-config)]
          ($ :div.mt-4.pt-4.border-t
            ($ :div.flex.justify-between.items-center
              ($ :span.text-lg.font-bold
                (if (> (:monthly-cents pricing) 0)
                  (str "$" (/ (:monthly-cents pricing) 100) "/month")
                  "Free"))
              (when (> (:yearly-cents pricing) 0)
                ($ :span.text-sm.text-base-content.opacity-70
                  (str "$" (/ (:yearly-cents pricing) 100) "/year"))))))))))

;; ============================================================================
;; Usage Meters
;; ============================================================================

(defui usage-meter
  "Display usage meter for a specific metric"
  [{:keys [metric current limit label]}]
  (let [percentage (if (and limit (> limit 0))
                     (min 100 (* (/ current limit) 100))
                     0)
        is-warning (> percentage 80)
        is-danger (>= percentage 100)]
    ($ :div.mb-4
      {:id (str "usage-meter-" (name metric))}
      ($ :div.flex.justify-between.items-center.mb-2
        ($ :span.text-sm.font-medium label)
        ($ :span.text-sm.text-base-content.opacity-70
          (if limit
            (str current " / " limit)
            (str current))))
      ($ :div.w-full.bg-base-200.rounded-full.h-2
        ($ :div.h-2.rounded-full.transition-all.duration-300
          {:class (cond
                    is-danger "bg-error"
                    is-warning "bg-warning"
                    :else "bg-primary")
           :style {:width (str percentage "%")}})))))

(defui usage-metrics-card
  "Display all usage metrics"
  []
  (let [limits (use-subscribe [:subscription/limits])
        usage (use-subscribe [:subscription/usage])]
    (when (and limits usage)
      ($ :div.card.bg-base-100.shadow-md
        {:id "usage-metrics-card"}
        ($ :div.card-body
          ($ :h3.card-title.flex.items-center
            ($ icons/chart-bar {:class "w-5 h-5 mr-2"})
            "Usage Metrics")
          ($ usage-meter
            {:metric :properties
             :current (:properties usage 0)
             :limit (:properties limits)
             :label "Properties"})
          ($ usage-meter
            {:metric :users
             :current (:users usage 0)
             :limit (:users limits)
             :label "Users"})
          ($ usage-meter
            {:metric :transactions
             :current (:transactions usage 0)
             :limit (:transactions limits)
             :label "Transactions"})
          ($ usage-meter
            {:metric :storage-gb
             :current (:storage-gb usage 0)
             :limit (:storage-gb limits)
             :label "Storage (GB)"}))))))

;; ============================================================================
;; Upgrade Prompt
;; ============================================================================

(defui upgrade-prompt
  "Prompt user to upgrade subscription"
  [{:keys [message show-button?]}]
  ($ :div.ds-alert.ds-alert-info.mb-4
    {:id "subscription-upgrade-prompt"}
    ($ :div.flex.items-center
      ($ icons/default-provider-icon {:class "w-6 h-6 mr-2"})
      ($ :div.flex-1
        ($ :p message)))
    (when show-button?
      ($ button
        {:btn-type :primary
         :class "ds-btn-sm"
         :id "btn-upgrade-subscription"
         :on-click #(rf/dispatch [:subscription/show-upgrade-modal])}
        "Upgrade Now"))))

;; ============================================================================
;; Subscription Actions
;; ============================================================================

(defui subscription-actions-card
  "Action buttons for subscription management"
  []
  (let [tier (use-subscribe [:subscription/tier])
        status (use-subscribe [:subscription/status])
        is-loading (use-subscribe [:subscription/loading?])]
    ($ :div.card.bg-base-100.shadow-md
      {:id "subscription-actions-card"}
      ($ :div.card-body
        ($ :h3.card-title "Manage Subscription")
        ($ :div.flex.flex-wrap.gap-2
          (when (not= tier :enterprise)
            ($ button
              {:btn-type :primary
               :id "btn-upgrade"
               :disabled is-loading
               :on-click #(rf/dispatch [:subscription/show-upgrade-modal])}
              ($ icons/arrow-up {:class "w-4 h-4"})
              "Upgrade"))
          ($ button
            {:btn-type :outline
             :id "btn-billing"
             :disabled is-loading
             :on-click #(rf/dispatch [:subscription/show-billing-modal])}
            ($ icons/credit-card {:class "w-4 h-4"})
            "Billing")
          (when (and (not= (:subscription-status status) :cancelled)
                  (not= tier :free))
            ($ button
              {:btn-type :error
               :class "ds-btn-outline"
               :id "btn-cancel-subscription"
               :disabled is-loading
               :on-click #(when (js/confirm "Are you sure you want to cancel your subscription?")
                            (rf/dispatch [:subscription/cancel false]))}
              ($ icons/x-mark {:class "w-4 h-4"})
              "Cancel")))))))

;; ============================================================================
;; Main Subscription Dashboard
;; ============================================================================

(defui subscription-dashboard
  "Main subscription management dashboard"
  []
  (let [status (use-subscribe [:subscription/status])
        tier-config (use-subscribe [:subscription/tier-config])
        is-loading (use-subscribe [:subscription/loading?])
        error (use-subscribe [:subscription/error])
        is-trial (use-subscribe [:subscription/is-trial?])
        trial-ends-at (use-subscribe [:subscription/trial-ends-at])]

    ;; Initialize subscription data on component mount
    (uix/use-effect
      (fn []
        (rf/dispatch [:subscription/initialize]))
      [])

    ($ :div.space-y-6
      {:id "subscription-dashboard"}

       ;; Loading state
      (when is-loading
        ($ :div.flex.justify-center.items-center.py-8
          ($ :span.ds-loading.ds-loading-spinner.ds-loading-lg)))

       ;; Error state
      (when error
        ($ :div.ds-alert.ds-alert-error
          ($ :div.flex.items-center
            ($ icons/exclamation-triangle {:class "w-6 h-6 mr-2"})
            ($ :span error))))

       ;; Trial warning
      (when (and is-trial trial-ends-at)
        ($ upgrade-prompt
          {:message (str "Your trial ends on " trial-ends-at ". Upgrade now to continue using all features.")
           :show-button? true}))

       ;; Status overview
      ($ :div.flex.items-center.justify-between.mb-6
        ($ :div
          ($ :h2.text-2xl.font-bold "Subscription")
          ($ :div.flex.items-center.mt-2
            ($ subscription-status-badge
              {:status (:subscription-status status)})
            (when status
              ($ :span.ml-2.text-sm.text-base-content.opacity-70
                (str "Tenant ID: " (:tenant-id status))))))
        ($ :div.text-right
          (when tier-config
            ($ :div.text-sm.text-base-content.opacity-70
              "Current Plan")
            ($ :div.font-semibold.text-lg
              (:name tier-config)))))

       ;; Main content
      (when (not is-loading)
        ($ :div.grid.gap-6 {:class "lg:grid-cols-2"}
            ;; Left column
          ($ :div.space-y-6
            ($ subscription-tier-card {:tier-config tier-config})
            ($ subscription-actions-card))

            ;; Right column
          ($ :div.space-y-6
            ($ usage-metrics-card)))))))

;; ============================================================================
;; Usage Limit Warning Component
;; ============================================================================

(defui usage-limit-warning
  "Show warning when approaching usage limits"
  [{:keys [metric]}]
  (let [is-warning (use-subscribe [:subscription/usage-warning? metric])
        is-exceeded (use-subscribe [:subscription/is-usage-limit-exceeded? metric])
        percentage (use-subscribe [:subscription/usage-percentage metric])]
    (when (or is-warning is-exceeded)
      ($ :div.ds-alert.mb-4
        {:id (str "usage-warning-" (name metric))
         :class (if is-exceeded "ds-alert-error" "ds-alert-warning")}
        ($ :div.flex.items-center
          ($ (if is-exceeded icons/exclamation-triangle icons/exclamation-circle)
            {:class "w-6 h-6 mr-2"})
          ($ :div
            ($ :p.font-semibold
              (if is-exceeded
                (str "Usage limit exceeded for " (name metric))
                (str "Approaching usage limit for " (name metric))))
            ($ :p.text-sm
              (str "Currently at " (Math/round percentage) "% of your limit"))
            (when is-exceeded
              ($ :p.text-sm.mt-1
                "Upgrade your plan to increase limits."))))
        (when is-exceeded
          ($ button
            {:btn-type :primary
             :class "ds-btn-sm"
             :id (str "btn-upgrade-from-" (name metric))
             :on-click #(rf/dispatch [:subscription/show-upgrade-modal])}
            "Upgrade Now"))))))
