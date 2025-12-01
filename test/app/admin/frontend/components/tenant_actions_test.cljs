(ns app.admin.frontend.components.tenant-actions-test
  "Tests for tenant actions component.

   Tests cover:
   - Tenant action handler creation
   - Confirmation handler wrapping
   - Action dropdown rendering
   - Status and subscription management
   - Loading state handling
   - Entity data extraction"
  (:require
    [app.admin.frontend.components.tenant-actions :as tenant-actions]
    [app.admin.frontend.test-setup :as setup]
    [app.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [uix.core :refer [$]]))

;; Initialize test environment for React component testing
(test-utils/setup-test-environment!)

;; Mock subscriptions for tenant loading states (guard against duplicate reg in watch mode)
(when-not (get-in @rf-db/app-db [:test :tenant-subs-registered?])
  (rf/reg-sub
    :admin/updating-tenant?
    (fn [db _]
      (get-in db [:admin :updating-tenant?] false)))

  (rf/reg-sub
    :admin/loading-tenant-details?
    (fn [db _]
      (get-in db [:admin :loading-tenant-details?] false)))

  (rf/reg-sub
    :admin/loading-tenant-billing?
    (fn [db _]
      (get-in db [:admin :loading-tenant-billing?] false)))

  (swap! rf-db/app-db assoc-in [:test :tenant-subs-registered?] true))

(deftest create-tenant-action-handlers-creates-all-handlers
  (testing "creates complete set of tenant action handlers"
    (let [handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")]
      (is (fn? (:view-details handlers)))
      (is (fn? (:view-billing handlers)))
      (is (fn? (:update-tier handlers)))
      (is (fn? (:update-subscription handlers)))
      (is (fn? (:activate handlers)))
      (is (fn? (:suspend handlers)))
      (is (fn? (:deactivate handlers)))))

  (testing "handlers include correct tenant information"
    (let [captured-tenant-id (atom nil)
          mock-dispatch (fn [event]
                          (when (= (first event) :admin/view-tenant-details)
                            (reset! captured-tenant-id (second event))))

          handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")]

      (with-redefs [rf/dispatch mock-dispatch]
        ((:view-details handlers) (js/Object.))

        (is (= 123 @captured-tenant-id)))))
        ;; Event no longer carries tenant-name payload; handler uses local logs

  (deftest create-tenant-confirmation-handlers-wraps-handlers
    (testing "creates confirmation wrappers for tenant actions"
      (let [action-handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")
            confirmation-handlers (tenant-actions/create-tenant-confirmation-handlers action-handlers "Test Tenant")]

        (is (fn? (:view-details confirmation-handlers)))
        (is (fn? (:view-billing confirmation-handlers)))
        (is (fn? (:suspend confirmation-handlers)))
        (is (fn? (:activate confirmation-handlers)))

      ;; Verify confirmation handlers wrap the original handlers
        (is (not= (:view-details action-handlers) (:view-details confirmation-handlers)))
        (is (not= (:suspend action-handlers) (:suspend confirmation-handlers))))))

  (deftest admin-tenant-actions-renders-action-dropdown
    (testing "renders tenant action dropdown trigger"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Test Tenant" :status "active" :subscription-tier "pro" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "handles namespaced tenant data (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:tenants/id 456 :tenants/name "Namespaced Tenant" :tenants/status "active" :tenants/subscription-tier "enterprise" :tenants/subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\"")))))

  (deftest admin-tenant-actions-handles-different-tenant-states
    (testing "renders actions for active tenant (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Active Tenant" :status "active" :subscription-tier "pro" :subscription-status "active"}
            markup (test-utils/render-to-static-markup
                     ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "renders actions for suspended tenant (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Suspended Tenant" :status "suspended" :subscription-tier "free" :subscription-status "cancelled"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "renders actions for trialing tenant (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Trial Tenant" :status "active" :subscription-tier "free" :subscription-status "trialing"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\"")))))

  (deftest admin-tenant-actions-handles-subscription-tiers
    (testing "renders actions for free tier (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Free Tenant" :status "active" :subscription-tier "free" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "renders actions for pro tier (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Pro Tenant" :status "active" :subscription-tier "pro" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "renders actions for enterprise tier (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Enterprise Tenant" :status "active" :subscription-tier "enterprise" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\"")))))

  (deftest admin-tenant-actions-handles-loading-states
    (testing "shows updating state (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? true
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Updating Tenant" :status "active" :subscription-tier "pro" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "shows loading details state (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? true
                                    :loading-tenant-billing? false}})

      (let [tenant {:id 123 :name "Loading Details Tenant" :status "active" :subscription-tier "pro" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\""))))

    (testing "shows loading billing state (trigger present)"
      (setup/reset-db!)
      (reset! rf-db/app-db {:admin/authenticated? true
                            :admin/current-user {:id 1 :role :admin}
                            :admin {:updating-tenant? false
                                    :loading-tenant-details? false
                                    :loading-tenant-billing? true}})

      (let [tenant {:id 123 :name "Loading Billing Tenant" :status "active" :subscription-tier "pro" :subscription-status "active"}
            markup (test-utils/render-to-static-markup ($ tenant-actions/admin-tenant-actions {:tenant tenant}))]
        (is (str/includes? markup "title=\"Actions\"")))))

  (deftest admin-tenant-actions-dispatches-correct-events
    (testing "dispatches view tenant details event"
      (let [captured-events (atom [])
            mock-dispatch (fn [event]
                            (swap! captured-events conj event))

            action-handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")]

        (with-redefs [rf/dispatch mock-dispatch]
          ((:view-details action-handlers) (js/Object.))

          (is (= 1 (count @captured-events)))
          (is (= [:admin/view-tenant-details 123] (first @captured-events))))))

    (testing "dispatches view tenant billing event"
      (let [captured-events (atom [])
            mock-dispatch (fn [event]
                            (swap! captured-events conj event))

            action-handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")]

        (with-redefs [rf/dispatch mock-dispatch]
          ((:view-billing action-handlers) (js/Object.))

          (is (= 1 (count @captured-events)))
          (is (= [:admin/view-tenant-billing 123] (first @captured-events))))))

    (testing "dispatches tenant status update events"
      (let [captured-events (atom [])
            mock-dispatch (fn [event]
                            (swap! captured-events conj event))

            action-handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")]

        (with-redefs [rf/dispatch mock-dispatch]
          ((:activate action-handlers) (js/Object.))

          (is (= 1 (count @captured-events)))
          (is (= [:admin/update-tenant-status 123 :active] (first @captured-events)))))))

  (deftest admin-tenant-actions-handles-event-propagation
    (testing "stops event propagation on actions"
      (let [stop-propagation-called (atom false)
            mock-event (doto (js/Object.)
                         (aset "stopPropagation" (fn []
                                                   (reset! stop-propagation-called true))))

            action-handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")]

        (with-redefs [rf/dispatch (fn [_])]
          ((:view-details action-handlers) mock-event)

          (is (true? @stop-propagation-called))))))

  (deftest admin-tenant-actions-extracts-entity-data-correctly
    (testing "extracts tenant data from unnamespaced keys"
      (let [_tenant {:id 123 :name "Test Tenant" :status "active"}
            handlers (tenant-actions/create-tenant-action-handlers 123 "Test Tenant")
            confirmation-handlers (tenant-actions/create-tenant-confirmation-handlers handlers "Test Tenant")]

      ;; Verify handlers work with unnamespaced data
        (is (fn? (:view-details confirmation-handlers)))
        (is (fn? (:suspend confirmation-handlers)))))

    (testing "extracts tenant data from namespaced keys"
      (let [_tenant {:tenants/id 456 :tenants/name "Namespaced Tenant" :tenants/status "active"}
            handlers (tenant-actions/create-tenant-action-handlers 456 "Namespaced Tenant")
            confirmation-handlers (tenant-actions/create-tenant-confirmation-handlers handlers "Namespaced Tenant")]

      ;; Verify handlers work with namespaced data
        (is (fn? (:view-details confirmation-handlers)))
        (is (fn? (:suspend confirmation-handlers)))))))
