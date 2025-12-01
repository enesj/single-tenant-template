(ns app.shared.schemas.api
  "API request and response schemas.
   These schemas define the structure of API endpoints for external communication."
  (:require
    [app.shared.schemas.primitives :as prim]
    [app.shared.schemas.template.subscription :as sub]))

;; Response schemas
(def subscription-list-response
  "Response schema for listing subscriptions"
  [:map
   [:subscriptions [:vector sub/subscription-schema]]
   [:total_count prim/positive-int]
   [:has_more :boolean]])

;; Request schemas
(def billing-portal-session-request
  "Request to create Stripe customer portal session"
  [:map
   [:return_url prim/url-schema]])

(def checkout-session-request
  "Request to create Stripe checkout session"
  [:map
   [:tier sub/subscription-tier]
   [:billing_interval sub/billing-interval]
   [:success_url prim/url-schema]
   [:cancel_url prim/url-schema]
   [:trial_days {:optional true} [:int {:min 0 :max 365}]]])

;; Generic API response wrappers
(def success-response
  "Generic successful API response"
  [:map
   [:success :boolean]
   [:data {:optional true} :any]
   [:message {:optional true} string?]])

(def error-response
  "Generic error API response"
  [:map
   [:success [:= false]]
   [:error
    [:map
     [:code string?]
     [:message string?]
     [:details {:optional true} :any]]]])

(def paginated-response
  "Generic paginated response wrapper"
  [:map
   [:items [:vector :any]]
   [:total_count [:int {:min 0}]]
   [:page [:int {:min 1}]]
   [:page_size [:int {:min 1 :max 100}]]
   [:has_more :boolean]])
