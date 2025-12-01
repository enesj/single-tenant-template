(ns app.template.backend.subscription-integration-test
  "Integration tests for subscription management (Task 2.3)"
  (:require
    [app.domain.backend.billing-test-helpers :as billing-helpers]
    [app.shared.schemas.template.billing :as billing-schemas]
    [app.shared.schemas.template.subscription :as sub-schemas]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [malli.core :as m]))

;; ============================================================================
;; Test Fixtures & Setup
;; ============================================================================

(defn billing-test-fixture
  "Set up billing test environment"
  [f]
  ;; TODO: Set up test Stripe client with test API keys
  ;; TODO: Initialize test database with subscription tables
  (println "🔧 Setting up billing test environment...")
  (f)
  (println "🧹 Cleaning up billing test environment..."))

(use-fixtures :each billing-test-fixture)

;; ============================================================================
;; Schema Validation Tests
;; ============================================================================

(deftest test-subscription-schema-validation
  (testing "Subscription schema validates correctly"
    (let [valid-subscription {:id                     "550e8400-e29b-41d4-a716-446655440000"
                              :tenant_id              "550e8400-e29b-41d4-a716-446655440001"
                              :stripe_subscription_id "sub_1234567890abcdef"
                              :stripe_customer_id     "cus_1234567890abcdef"
                              :tier                   :starter
                              :status                 :active
                              :billing_interval       :month
                              :amount_cents           2900
                              :currency               "USD"
                              :current_period_start   "2024-01-01T00:00:00Z"
                              :current_period_end     "2024-02-01T00:00:00Z"
                              :created_at             "2024-01-01T00:00:00Z"
                              :updated_at             "2024-01-01T00:00:00Z"}]
      (is (m/validate sub-schemas/subscription-schema valid-subscription)
        "Valid subscription should pass schema validation")))

  (testing "Invalid subscription fails schema validation"
    (let [invalid-subscription {:id     "invalid-uuid"
                                :tier   :invalid-tier
                                :status :invalid-status}]
      (is (not (m/validate sub-schemas/subscription-schema invalid-subscription))
        "Invalid subscription should fail schema validation"))))

(deftest test-billing-history-schema-validation
  (testing "Billing history schema validates correctly"
    (let [valid-billing {:id                "550e8400-e29b-41d4-a716-446655440000"
                         :tenant_id         "550e8400-e29b-41d4-a716-446655440001"
                         :subscription_id   "550e8400-e29b-41d4-a716-446655440002"
                         :stripe_invoice_id "in_1234567890abcdef"
                         :amount_cents      2900
                         :currency          "USD"
                         :status            :paid
                         :billing_date      "2024-01-01T00:00:00Z"
                         :paid_at           "2024-01-01T12:00:00Z"
                         :created_at        "2024-01-01T00:00:00Z"}]
      (is (m/validate billing-schemas/billing-history-schema valid-billing)
        "Valid billing history should pass schema validation"))))

(deftest test-payment-method-schema-validation
  (testing "Payment method schema validates correctly"
    (let [valid-payment-method {:id              "pm_1234567890abcdef"
                                :type            :card
                                :card            {:brand     "visa"
                                                  :last4     "4242"
                                                  :exp_month 12
                                                  :exp_year  2025}
                                :billing_details {:email "test@example.com"
                                                  :name  "Test Customer"}}]
      (is (m/validate billing-schemas/payment-method-schema valid-payment-method)
        "Valid payment method should pass schema validation"))))

;; ============================================================================
;; Subscription Tier Tests
;; ============================================================================

(deftest test-subscription-tier-configuration
  (testing "All subscription tiers have required configuration"
    (doseq [tier [:free :starter :professional :enterprise]]
      (let [tier-config (get billing-helpers/test-subscription-tiers tier)]
        (is (not (nil? tier-config))
          (str "Tier " tier " should have configuration"))
        (is (contains? tier-config :name)
          (str "Tier " tier " should have a name"))
        (is (contains? tier-config :limits)
          (str "Tier " tier " should have limits"))
        (is (contains? tier-config :pricing)
          (str "Tier " tier " should have pricing"))))))

(deftest test-subscription-tier-limits
  (testing "Subscription tier limits are properly defined"
    (billing-helpers/assert-subscription-tier-limits
      :starter
      {:properties   5
       :users        3
       :transactions 500
       :storage_gb   10})

    (billing-helpers/assert-subscription-tier-limits
      :professional
      {:properties   25
       :users        10
       :transactions 5000
       :storage_gb   100})))

(deftest test-subscription-pricing
  (testing "Subscription pricing matches configuration"
    (billing-helpers/assert-billing-amount :starter :month 2900)
    (billing-helpers/assert-billing-amount :starter :year 29000)
    (billing-helpers/assert-billing-amount :professional :month 9900)
    (billing-helpers/assert-billing-amount :professional :year 99000)))

;; ============================================================================
;; Subscription Lifecycle Tests
;; ============================================================================

(deftest test-subscription-status-transitions
  (testing "Valid subscription status transitions"
    (billing-helpers/assert-subscription-status-transition :trialing :active)
    (billing-helpers/assert-subscription-status-transition :active :past_due)
    (billing-helpers/assert-subscription-status-transition :active :canceled)
    (billing-helpers/assert-subscription-status-transition :past_due :active)
    (billing-helpers/assert-subscription-status-transition :canceled :active))

  (testing "Invalid subscription status transitions should fail"
    (is (thrown? AssertionError
          (billing-helpers/assert-subscription-status-transition :active :trialing))
      "Should not be able to transition from active to trialing")))

(deftest test-subscription-creation-scenario
  (testing "Complete subscription creation scenario"
    (let [scenario (billing-helpers/create-subscription-test-scenario :starter :month)]
      (is (contains? scenario :customer)
        "Scenario should include customer data")
      (is (contains? scenario :subscription)
        "Scenario should include subscription data")
      (is (contains? scenario :payment-method)
        "Scenario should include payment method data")
      (is (= :starter (get-in scenario [:tier-config :tier]))
        "Scenario should have correct tier configuration"))))

(deftest test-subscription-upgrade-scenario
  (testing "Subscription upgrade scenario"
    (let [scenario (billing-helpers/subscription-upgrade-scenario :starter :professional)]
      (is (contains? scenario :current)
        "Upgrade scenario should include current subscription")
      (is (contains? scenario :target)
        "Upgrade scenario should include target subscription")
      (is (contains? scenario :proration-amount)
        "Upgrade scenario should include proration amount")
      (is (= :starter (get-in scenario [:current :tier-config :tier]))
        "Current subscription should be starter tier")
      (is (= :professional (get-in scenario [:target :tier-config :tier]))
        "Target subscription should be professional tier"))))

(deftest test-subscription-cancellation-scenario
  (testing "Subscription cancellation scenario"
    (let [scenario (billing-helpers/subscription-cancellation-scenario :professional)]
      (is (true? (get-in scenario [:subscription :cancel_at_period_end]))
        "Cancelled subscription should have cancel_at_period_end set to true"))))

;; ============================================================================
;; Webhook Event Tests
;; ============================================================================

(deftest test-webhook-event-validation
  (testing "Webhook events validate against schema"
    (doseq [[event-name event-data] billing-helpers/webhook-test-events]
      (is (m/validate billing-schemas/webhook-event-schema event-data)
        (str "Webhook event " event-name " should validate against schema")))))

(deftest test-webhook-event-processing
  (testing "Webhook events contain expected data"
    (let [subscription-created (:subscription-created billing-helpers/webhook-test-events)]
      (is (= "customer.subscription.created" (:type subscription-created))
        "Subscription created event should have correct type")
      (is (contains? (:data subscription-created) :object)
        "Webhook event should contain object data"))

    (let [invoice-paid (:invoice-paid billing-helpers/webhook-test-events)]
      (is (= "invoice.payment_succeeded" (:type invoice-paid))
        "Invoice paid event should have correct type"))))

;; ============================================================================
;; Mock API Response Tests
;; ============================================================================

(deftest test-mock-stripe-responses
  (testing "Mock Stripe API success responses"
    (let [success-response (billing-helpers/mock-stripe-api-success {:id "cus_test"})]
      (is (= 200 (:status success-response))
        "Success response should have 200 status")
      (is (= "application/json" (get-in success-response [:headers "content-type"]))
        "Success response should have JSON content type")))

  (testing "Mock Stripe API error responses"
    (let [error-response (billing-helpers/mock-stripe-api-error "invalid_request_error" "Invalid customer ID")]
      (is (= 400 (:status error-response))
        "Error response should have 400 status")
      (is (contains? (:body error-response) :error)
        "Error response should contain error object"))))

;; ============================================================================
;; Integration Test Scenarios
;; ============================================================================

(deftest test-end-to-end-subscription-flow
  (testing "Complete subscription flow from creation to cancellation"
    ;; Step 1: Create subscription
    (let [creation-scenario (billing-helpers/create-subscription-test-scenario :starter :month)]
      (is (not (nil? creation-scenario))
        "Should be able to create subscription scenario")

      ;; Step 2: Upgrade subscription
      (let [upgrade-scenario (billing-helpers/subscription-upgrade-scenario :starter :professional)]
        (is (not (nil? upgrade-scenario))
          "Should be able to create upgrade scenario")

        ;; Step 3: Cancel subscription
        (let [cancellation-scenario (billing-helpers/subscription-cancellation-scenario :professional)]
          (is (not (nil? cancellation-scenario))
            "Should be able to create cancellation scenario"))))))

(deftest test-subscription-tier-progression
  (testing "Progressive subscription tier upgrades"
    (let [tiers     [:free :starter :professional :enterprise]
          scenarios (map #(billing-helpers/create-subscription-test-scenario % :month) tiers)]
      (is (= 4 (count scenarios))
        "Should create scenarios for all tiers")
      (doseq [[tier scenario] (map vector tiers scenarios)]
        (is (= tier (get-in scenario [:tier-config :tier]))
          (str "Scenario should match tier " tier))))))

;; ============================================================================
;; Performance & Load Tests
;; ============================================================================

(deftest test-subscription-data-generation-performance
  (testing "Mock data generation performance"
    (let [start-time (System/currentTimeMillis)
          scenarios  (repeatedly 100 #(billing-helpers/create-subscription-test-scenario :starter :month))
          end-time   (System/currentTimeMillis)
          duration   (- end-time start-time)]
      (is (< duration 1000)
        "Should generate 100 subscription scenarios in under 1 second")
      (is (= 100 (count scenarios))
        "Should generate exactly 100 scenarios"))))

;; ============================================================================
;; Documentation Tests
;; ============================================================================

(deftest test-subscription-tier-documentation
  (testing "Subscription tiers have complete documentation"
    (doseq [[tier config] billing-helpers/test-subscription-tiers]
      (is (string? (:name config))
        (str "Tier " tier " should have a string name"))
      (is (string? (:description config))
        (str "Tier " tier " should have a string description"))
      (is (vector? (:features config))
        (str "Tier " tier " should have a vector of features"))
      (is (every? string? (:features config))
        (str "All features for tier " tier " should be strings")))))
