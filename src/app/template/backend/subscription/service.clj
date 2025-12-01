(ns app.template.backend.subscription.service
  "Subscription management service for multi-tenant SaaS platform"
  (:require
    [app.shared.schemas.template.subscription :as sub-schemas]
    [app.template.backend.db.protocols :as db-protocols]
    [clojure.tools.logging :as log]
    [malli.core :as m]
    [stripe-clojure.core :as stripe]
    [stripe-clojure.customers :as stripe-customers]
    [stripe-clojure.subscriptions :as stripe-subscriptions]))

;; ============================================================================
;; Subscription Tier Configuration
;; ============================================================================

(def subscription-tiers
  "Production subscription tier configuration"
  {:free {:tier :free
          :name "Free Plan"
          :description "Basic features for individuals"
          :features ["Up to 1 property" "Basic reporting" "Email support"]
          :limits {:properties 1
                   :users 1
                   :transactions 50
                   :storage-gb 1}
          :pricing {:monthly-cents 0
                    :yearly-cents 0
                    :currency "USD"}
          :stripe-price-ids {:monthly "price_free_monthly"
                             :yearly "price_free_yearly"}}
   :starter {:tier :starter
             :name "Starter Plan"
             :description "Perfect for small property owners"
             :features ["Up to 5 properties" "Advanced reporting" "Priority support" "Co-host management"]
             :limits {:properties 5
                      :users 3
                      :transactions 500
                      :storage-gb 10}
             :pricing {:monthly-cents 2900
                       :yearly-cents 29000
                       :currency "USD"}
             :stripe-price-ids {:monthly "price_starter_monthly"
                                :yearly "price_starter_yearly"}}
   :professional {:tier :professional
                  :name "Professional Plan"
                  :description "For property management companies"
                  :features ["Up to 25 properties" "Custom reporting" "API access" "White-label options"]
                  :limits {:properties 25
                           :users 10
                           :transactions 5000
                           :storage-gb 100}
                  :pricing {:monthly-cents 9900
                            :yearly-cents 99000
                            :currency "USD"}
                  :stripe-price-ids {:monthly "price_professional_monthly"
                                     :yearly "price_professional_yearly"}}
   :enterprise {:tier :enterprise
                :name "Enterprise Plan"
                :description "Custom solutions for large organizations"
                :features ["Unlimited properties" "Custom integrations" "Dedicated support" "SLA guarantees"]
                :limits {:properties 999999
                         :users 100
                         :transactions 999999
                         :storage-gb 1000}
                :pricing {:monthly-cents 29900
                          :yearly-cents 299000
                          :currency "USD"}
                :stripe-price-ids {:monthly "price_enterprise_monthly"
                                   :yearly "price_enterprise_yearly"}}})

 ;; ============================================================================
;; Stripe Client Configuration
;; ============================================================================

(defn get-stripe-client
  "Get initialized Stripe client with API key"
  []
  (stripe/init-stripe {:api-key (or (System/getenv "STRIPE_SECRET_KEY")
                                  "sk_test_placeholder")}))

;; ============================================================================
;; Core Subscription Service
;; ============================================================================

(defn get-subscription-limits
  "Get usage limits for a subscription tier"
  [tier]
  (get-in subscription-tiers [tier :limits]))

(defn get-tier-config
  "Get complete configuration for a subscription tier"
  [tier]
  (get subscription-tiers tier))

(defn create-stripe-customer
  "Create a Stripe customer for a tenant"
  [db tenant]
  (try
    (let [stripe-client (get-stripe-client)
          customer-params {:email (or (:billing-email tenant) (:name tenant))
                           :name (:name tenant)
                           :metadata {:tenant-id (str (:tenant-id tenant))
                                      :tenant-slug (:slug tenant)}}
          stripe-customer (stripe-customers/create-customer stripe-client customer-params)]
      (log/info "Created Stripe customer" {:customer-id (:id stripe-customer)
                                           :tenant-id (:tenant-id tenant)})

      ;; Update tenant with Stripe customer ID using model-naming
      (db-protocols/update-record db nil :tenants (:tenant-id tenant)
        {:stripe-customer-id (:id stripe-customer)})
      stripe-customer)
    (catch Exception e
      (log/error e "Failed to create Stripe customer" {:tenant-id (:tenant-id tenant)})
      (throw e))))

(defn create-subscription
  "Create a new subscription for a tenant"
  [db tenant-id subscription-request]
  (try
    ;; Validate request
    (when-not (m/validate sub-schemas/subscription-create-request subscription-request)
      (throw (ex-info "Invalid subscription request"
               {:errors (m/explain sub-schemas/subscription-create-request subscription-request)})))

    ;; Get tenant
    (let [tenant (db-protocols/find-by-id db :tenants tenant-id)]
      (when-not tenant
        (throw (ex-info "Tenant not found" {:tenant-id tenant-id})))

      ;; Ensure Stripe customer exists
      (let [stripe-client (get-stripe-client)
            stripe-customer-id (or (:stripe-customer-id tenant)
                                 (:id (create-stripe-customer db tenant)))
            tier (:tier subscription-request)
            billing-interval (:billing-interval subscription-request)
            price-id (get-in subscription-tiers [tier :stripe-price-ids billing-interval])

            ;; Create Stripe subscription
            subscription-params {:customer stripe-customer-id
                                 :items [{:price price-id}]
                                 :trial-period-days (:trial-days subscription-request 30)
                                 :payment-behavior "default_incomplete"
                                 :payment-settings {:save-default-payment-method "on_subscription"}
                                 :expand ["latest_invoice.payment_intent"]}
            stripe-subscription (stripe-subscriptions/create-subscription stripe-client subscription-params)]

        (log/info "Created Stripe subscription"
          {:subscription-id (:id stripe-subscription)
           :tenant-id tenant-id
           :tier tier})

        ;; Update tenant with subscription details
        (db-protocols/update-record db nil :tenants tenant-id
          {:subscription-tier (name tier)
           :subscription-status (name (keyword (:status stripe-subscription)))
           :stripe-subscription-id (:id stripe-subscription)
           :trial-ends-at (:trial_end stripe-subscription)})

        stripe-subscription))
    (catch Exception e
      (log/error e "Failed to create subscription" {:tenant-id tenant-id})
      (throw e))))

(defn update-subscription
  "Update an existing subscription"
  [db tenant-id subscription-update]
  (try
    ;; Validate request
    (when-not (m/validate sub-schemas/subscription-update-request subscription-update)
      (throw (ex-info "Invalid subscription update request"
               {:errors (m/explain sub-schemas/subscription-update-request subscription-update)})))

    ;; Get tenant
    (let [tenant (db-protocols/find-by-id db :tenants tenant-id)]
      (when-not tenant
        (throw (ex-info "Tenant not found" {:tenant-id tenant-id})))

      (when-not (:stripe-subscription-id tenant)
        (throw (ex-info "No active subscription found" {:tenant-id tenant-id})))

      ;; Update Stripe subscription
      (let [stripe-client (get-stripe-client)
            subscription-id (:stripe-subscription-id tenant)
            update-params (cond-> {}
                            (:tier subscription-update)
                            (assoc :items [{:price (get-in subscription-tiers
                                                     [(:tier subscription-update)
                                                      :stripe-price-ids
                                                      (:billing-interval subscription-update :monthly)])}])
                            (:cancel-at-period-end subscription-update)
                            (assoc :cancel_at_period_end (:cancel-at-period-end subscription-update)))
            stripe-subscription (stripe-subscriptions/update-subscription stripe-client subscription-id update-params)]

        (log/info "Updated Stripe subscription"
          {:subscription-id subscription-id
           :tenant-id tenant-id})

        ;; Update tenant record
        (when (:tier subscription-update)
          (db-protocols/update-record db nil :tenants tenant-id
            {:subscription-tier (name (:tier subscription-update))
             :subscription-status (name (keyword (:status stripe-subscription)))}))

        stripe-subscription))
    (catch Exception e
      (log/error e "Failed to update subscription" {:tenant-id tenant-id})
      (throw e))))

(defn cancel-subscription
  "Cancel a subscription with optional immediate or at period end"
  [db tenant-id immediate?]
  (try
    (let [tenant (db-protocols/find-by-id db :tenants tenant-id)]
      (when-not tenant
        (throw (ex-info "Tenant not found" {:tenant-id tenant-id})))

      (when-not (:stripe-subscription-id tenant)
        (throw (ex-info "No active subscription found" {:tenant-id tenant-id})))

      (let [stripe-client (get-stripe-client)
            subscription-id (:stripe-subscription-id tenant)
            stripe-subscription (if immediate?
                                  (stripe-subscriptions/cancel-subscription stripe-client subscription-id)
                                  (stripe-subscriptions/update-subscription stripe-client subscription-id
                                    {:cancel_at_period_end true}))]

        (log/info "Cancelled Stripe subscription"
          {:subscription-id subscription-id
           :tenant-id tenant-id
           :immediate immediate?})

        ;; Update tenant status
        (db-protocols/update-record db nil :tenants tenant-id
          {:subscription-status (if immediate? "cancelled" "active")})

        stripe-subscription))
    (catch Exception e
      (log/error e "Failed to cancel subscription" {:tenant-id tenant-id})
      (throw e))))

(defn get-subscription-status
  "Get current subscription status for a tenant"
  [db tenant-id]
  (when-let [tenant (db-protocols/find-by-id db :tenants tenant-id)]
    (let [tier-value (or (:subscription-tier tenant) (:subscription_tier tenant))
          status-value (or (:subscription-status tenant) (:subscription_status tenant))
          stripe-customer-id (or (:stripe-customer-id tenant) (:stripe_customer_id tenant))
          stripe-subscription-id (or (:stripe-subscription-id tenant) (:stripe_subscription_id tenant))
          trial-ends-at (or (:trial-ends-at tenant) (:trial_ends_at tenant))
          tier (some-> tier-value name keyword)
          status (some-> status-value name keyword)]
      {:tenant-id tenant-id
       :subscription-tier tier
       :subscription-status status
       :stripe-customer-id stripe-customer-id
       :stripe-subscription-id stripe-subscription-id
       :trial-ends-at trial-ends-at
       :limits (when tier (get-subscription-limits tier))
       :tier-config (when tier (get-tier-config tier))})))

;; ============================================================================
;; Usage Limits Enforcement
;; ============================================================================

(defn check-usage-limit
  "Check if usage is within subscription limits"
  [db tenant-id metric-name current-value]
  (let [tenant (db-protocols/find-by-id db :tenants tenant-id)
        tier (keyword (:subscription-tier tenant))
        limits (get-subscription-limits tier)
        limit (get limits metric-name)]
    (if limit
      {:within-limit (< current-value limit)
       :current-usage current-value
       :limit limit
       :tier tier}
      {:within-limit true
       :current-usage current-value
       :limit nil
       :tier tier})))

(defn enforce-usage-limit
  "Enforce usage limit, throwing exception if exceeded"
  [db tenant-id metric-name current-value]
  (let [usage-check (check-usage-limit db tenant-id metric-name current-value)]
    (when-not (:within-limit usage-check)
      (throw (ex-info "Usage limit exceeded"
               {:metric metric-name
                :current-usage current-value
                :limit (:limit usage-check)
                :tier (:tier usage-check)})))
    usage-check))

;; ============================================================================
;; Webhook Handling
;; ============================================================================

(defn handle-subscription-webhook
  "Handle Stripe subscription webhook events"
  [db webhook-event]
  (try
    (let [event-type (:type webhook-event)
          subscription-data (get-in webhook-event [:data :object])
          customer-id (:customer subscription-data)]

      ;; Find tenant by Stripe customer ID
      (when-let [tenant (db-protocols/find-by-field db :tenants :stripe-customer-id customer-id)]
        (case event-type
          "customer.subscription.created"
          (do
            (log/info "Subscription created webhook" {:tenant-id (:tenant-id tenant)})
            (db-protocols/update-record db nil :tenants (:tenant-id tenant)
              {:subscription-status (name (keyword (:status subscription-data)))
               :stripe-subscription-id (:id subscription-data)}))

          "customer.subscription.updated"
          (do
            (log/info "Subscription updated webhook" {:tenant-id (:tenant-id tenant)})
            (db-protocols/update-record db nil :tenants (:tenant-id tenant)
              {:subscription-status (name (keyword (:status subscription-data)))}))

          "customer.subscription.deleted"
          (do
            (log/info "Subscription deleted webhook" {:tenant-id (:tenant-id tenant)})
            (db-protocols/update-record db nil :tenants (:tenant-id tenant)
              {:subscription-status "cancelled"
               :subscription-tier "free"}))

          (log/debug "Unhandled webhook event" {:type event-type}))))

    {:status :processed}
    (catch Exception e
      (log/error e "Failed to process subscription webhook" {:event-type (:type webhook-event)})
      {:status :error :error (.getMessage e)})))

;; ============================================================================
;; Compatibility Layer - Old snake_case Interface
;; ============================================================================

(defn get-subscription-status-legacy
  "Legacy version of get-subscription-status for backward compatibility.
   Returns snake_case keys as before the migration."
  [db tenant-id]
  (let [new-status (get-subscription-status db tenant-id)]
    (when new-status
      ;; Convert kebab-case keys back to snake_case
      {:tenant_id (:tenant-id new-status)
       :subscription_tier (:subscription-tier new-status)
       :subscription_status (:subscription-status new-status)
       :stripe_customer_id (:stripe-customer-id new-status)
       :stripe_subscription_id (:stripe-subscription-id new-status)
       :trial_ends_at (:trial-ends-at new-status)
       :limits (:limits new-status)
       :tier_config (:tier-config new-status)})))

(defn check-usage-limit-legacy
  "Legacy version of check-usage-limit for backward compatibility."
  [db tenant-id metric-name current-value]
  (let [new-check (check-usage-limit db tenant-id metric-name current-value)]
    ;; Convert kebab-case keys back to snake_case
    (if (:within-limit new-check)
      {:within_limit (:within-limit new-check)
       :current_usage (:current-usage new-check)
       :limit (:limit new-check)
       :tier (:tier new-check)}
      ;; Handle the other case
      {:within_limit (:within-limit new-check)
       :current_usage (:current-usage new-check)
       :limit (:limit new-check)
       :tier (:tier new-check)})))

(defn enforce-usage-limit-legacy
  "Legacy version of enforce-usage-limit for backward compatibility."
  [db tenant-id metric-name current-value]
  (check-usage-limit-legacy db tenant-id metric-name current-value))

;; Legacy subscription-tiers with snake_case keys for backward compatibility
(def subscription-tiers-legacy
  "Legacy subscription tier configuration with snake_case keys"
  {:free {:tier :free
          :name "Free Plan"
          :description "Basic features for individuals"
          :features ["Up to 1 property" "Basic reporting" "Email support"]
          :limits {:properties 1
                   :users 1
                   :transactions 50
                   :storage_gb 1}
          :pricing {:monthly_cents 0
                    :yearly_cents 0
                    :currency "USD"}
          :stripe_price_ids {:monthly "price_free_monthly"
                             :yearly "price_free_yearly"}}
   :starter {:tier :starter
             :name "Starter Plan"
             :description "Perfect for small property owners"
             :features ["Up to 5 properties" "Advanced reporting" "Priority support" "Co-host management"]
             :limits {:properties 5
                      :users 3
                      :transactions 500
                      :storage_gb 10}
             :pricing {:monthly_cents 2900
                       :yearly_cents 29000
                       :currency "USD"}
             :stripe_price_ids {:monthly "price_starter_monthly"
                                :yearly "price_starter_yearly"}}
   :professional {:tier :professional
                  :name "Professional Plan"
                  :description "For property management companies"
                  :features ["Up to 25 properties" "Custom reporting" "API access" "White-label options"]
                  :limits {:properties 25
                           :users 10
                           :transactions 5000
                           :storage_gb 100}
                  :pricing {:monthly_cents 9900
                            :yearly_cents 99000
                            :currency "USD"}
                  :stripe_price_ids {:monthly "price_professional_monthly"
                                     :yearly "price_professional_yearly"}}
   :enterprise {:tier :enterprise
                :name "Enterprise Plan"
                :description "Custom solutions for large organizations"
                :features ["Unlimited properties" "Custom integrations" "Dedicated support" "SLA guarantees"]
                :limits {:properties 999999
                         :users 100
                         :transactions 999999
                         :storage_gb 1000}
                :pricing {:monthly_cents 29900
                          :yearly_cents 299000
                          :currency "USD"}
                :stripe_price_ids {:monthly "price_enterprise_monthly"
                                   :yearly "price_enterprise_yearly"}}})
