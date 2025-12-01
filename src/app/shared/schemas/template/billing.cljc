(ns app.shared.schemas.template.billing
  "Billing and payment-related schemas.
   This includes payment methods, billing history, and Stripe webhook events."
  (:require
    [app.shared.schemas.primitives :as prim]
    [app.shared.schemas.template.subscription :as sub]))

(def billing-history-schema
  "Billing history record"
  [:map
   [:id prim/uuid-schema]
   [:tenant_id prim/uuid-schema]
   [:subscription_id prim/uuid-schema]
   [:stripe_invoice_id {:optional true} sub/stripe-id]
   [:amount_cents prim/positive-int]
   [:currency prim/currency-code]
   [:status [:enum :draft :open :paid :void :uncollectible]]
   [:description {:optional true} string?]
   [:invoice_pdf {:optional true} prim/url-schema]
   [:billing_date prim/datetime-schema]
   [:paid_at {:optional true} prim/datetime-schema]
   [:created_at prim/datetime-schema]])

(def payment-method-schema
  "Payment method information from Stripe"
  [:map
   [:id sub/stripe-id]
   [:type [:enum :card :bank_account :sepa_debit]]
   [:card {:optional true}
    [:map
     [:brand string?]
     [:last4 [:string {:min 4 :max 4}]]
     [:exp_month [:int {:min 1 :max 12}]]
     [:exp_year [:int {:min 2024}]]]]
   [:billing_details {:optional true}
    [:map
     [:email {:optional true} prim/email-schema]
     [:name {:optional true} string?]
     [:phone {:optional true} prim/phone-number]
     [:address {:optional true}
      [:map
       [:city {:optional true} string?]
       [:country {:optional true} [:string {:min 2 :max 2}]]
       [:line1 {:optional true} string?]
       [:line2 {:optional true} string?]
       [:postal_code {:optional true} string?]
       [:state {:optional true} string?]]]]]])

(def webhook-event-schema
  "Stripe webhook event structure"
  [:map
   [:id sub/stripe-id]
   [:type string?]
   [:data
    [:map
     [:object :any]]]
   [:created [:int {:min 0}]]
   [:livemode :boolean]
   [:pending_webhooks prim/positive-int]
   [:request
    [:map
     [:id {:optional true} [:maybe string?]]
     [:idempotency_key {:optional true} [:maybe string?]]]]])
