(ns app.shared.schemas.template.subscription
  "Subscription-related schemas for the SaaS platform.
   This includes subscription tiers, statuses, and related configurations."
  (:require
    [app.shared.schemas.primitives :as prim]))

(def subscription-tier
  "Available subscription tiers for the SaaS platform"
  [:enum :free :starter :professional :enterprise])

(def subscription-status
  "Stripe subscription status values"
  [:enum :active :canceled :incomplete :incomplete_expired :past_due :paused :trialing :unpaid])

(def billing-interval
  "Billing frequency options"
  [:enum :month :year])

(def stripe-id
  "Stripe object ID pattern (starts with prefix like cus_, sub_, price_, etc.)"
  [:and
   string?
   [:re #"^[a-z]{2,}_[a-zA-Z0-9]{14,}$"]])

(def subscription-schema
  "Complete subscription information"
  [:map
   [:id prim/uuid-schema]
   [:tenant_id prim/uuid-schema]
   [:stripe_subscription_id {:optional true} stripe-id]
   [:stripe_customer_id {:optional true} stripe-id]
   [:tier subscription-tier]
   [:status subscription-status]
   [:billing_interval billing-interval]
   [:amount_cents prim/positive-int]
   [:currency prim/currency-code]
   [:trial_end {:optional true} prim/datetime-schema]
   [:current_period_start prim/datetime-schema]
   [:current_period_end prim/datetime-schema]
   [:cancel_at_period_end {:optional true} :boolean]
   [:canceled_at {:optional true} prim/datetime-schema]
   [:created_at prim/datetime-schema]
   [:updated_at prim/datetime-schema]])

(def subscription-tier-config
  "Configuration for each subscription tier"
  [:map
   [:tier subscription-tier]
   [:name string?]
   [:description string?]
   [:features [:vector string?]]
   [:limits
    [:map
     [:properties prim/positive-int]
     [:users prim/positive-int]
     [:transactions prim/positive-int]
     [:storage_gb prim/positive-int]]]
   [:pricing
    [:map
     [:monthly_cents prim/positive-int]
     [:yearly_cents prim/positive-int]
     [:currency prim/currency-code]]]
   [:stripe_price_ids
    [:map
     [:monthly stripe-id]
     [:yearly stripe-id]]]])

(def subscription-create-request
  "Schema for creating a new subscription"
  [:map
   [:tier subscription-tier]
   [:billing_interval billing-interval]
   [:payment_method_id {:optional true} string?]
   [:trial_days {:optional true} [:int {:min 0 :max 365}]]])

(def subscription-update-request
  "Schema for updating subscription details"
  [:map
   [:tier {:optional true} subscription-tier]
   [:billing_interval {:optional true} billing-interval]
   [:cancel_at_period_end {:optional true} :boolean]])
