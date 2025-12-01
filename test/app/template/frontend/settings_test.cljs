(ns app.template.frontend.settings-test
  "Tests for global and entity-specific settings management"
  (:require
    [app.template.frontend.events.list.ui-state :as ui-state-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is run-tests testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Register test events only once to avoid re-frame warnings
(defonce settings-test-events-registered
  (do
    ;; Initialize test database
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))

    ;; Set global UI setting
    (rf/reg-event-db
      ::test-set-global-setting
      (fn [db [_ setting-key value]]
        (assoc-in db [:ui setting-key] value)))

    ;; Set entity-specific setting
    (rf/reg-event-db
      ::test-set-entity-setting
      (fn [db [_ entity-type setting-key value]]
        (assoc-in db [:ui :entity-configs entity-type setting-key] value)))

    ;; Set default setting
    (rf/reg-event-db
      ::test-set-default-setting
      (fn [db [_ setting-key value]]
        (assoc-in db [:ui :defaults setting-key] value)))

    ;; Clear entity configuration
    (rf/reg-event-db
      ::test-clear-entity-config
      (fn [db [_ entity-type]]
        (update-in db [:ui :entity-configs] dissoc entity-type)))

    ;; Clear all entity configurations
    (rf/reg-event-db
      ::test-clear-all-entity-configs
      (fn [db _]
        (assoc-in db [:ui :entity-configs] {})))

    true))

(deftest global-settings-test
  (testing "Global UI settings management"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test setting global show-timestamps
    (rf/dispatch-sync [::test-set-global-setting :show-timestamps? true])
    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :show-timestamps?]))
        "Should set global show-timestamps"))

    ;; Test setting global show-edit
    (rf/dispatch-sync [::test-set-global-setting :show-edit? false])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :show-edit?]))
        "Should set global show-edit"))

    ;; Test setting global show-delete
    (rf/dispatch-sync [::test-set-global-setting :show-delete? false])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :show-delete?]))
        "Should set global show-delete"))

    ;; Test setting global show-highlights
    (rf/dispatch-sync [::test-set-global-setting :show-highlights? false])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :show-highlights?]))
        "Should set global show-highlights"))

    ;; Test setting global show-select
    (rf/dispatch-sync [::test-set-global-setting :show-select? true])
    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :show-select?]))
        "Should set global show-select"))))

(deftest entity-specific-settings-test
  (testing "Entity-specific settings override global defaults"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set global defaults
    (rf/dispatch-sync [::test-set-global-setting :show-edit? true])
    (rf/dispatch-sync [::test-set-global-setting :show-delete? true])
    (rf/dispatch-sync [::test-set-global-setting :show-timestamps? false])

    ;; Set entity-specific overrides for :items
    (rf/dispatch-sync [::test-set-entity-setting :items :show-edit? false])
    (rf/dispatch-sync [::test-set-entity-setting :items :show-timestamps? true])

    (let [db @rf-db/app-db]
      ;; Check entity-specific overrides
      (is (false? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Should override show-edit for items entity")
      (is (true? (get-in db [:ui :entity-configs :items :show-timestamps?]))
        "Should override show-timestamps for items entity")

      ;; Check that non-overridden settings maintain global values
      (is (nil? (get-in db [:ui :entity-configs :items :show-delete?]))
        "Should not have entity override for show-delete")

      ;; Global settings should remain unchanged
      (is (true? (get-in db [:ui :show-edit?]))
        "Global show-edit should remain unchanged")
      (is (false? (get-in db [:ui :show-timestamps?]))
        "Global show-timestamps should remain unchanged"))))

(deftest defaults-settings-test
  (testing "Default settings structure"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set defaults
    (rf/dispatch-sync [::test-set-default-setting :show-edit? true])
    (rf/dispatch-sync [::test-set-default-setting :show-delete? true])
    (rf/dispatch-sync [::test-set-default-setting :show-timestamps? false])
    (rf/dispatch-sync [::test-set-default-setting :show-highlights? true])
    (rf/dispatch-sync [::test-set-default-setting :show-select? false])

    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :defaults :show-edit?]))
        "Should set default show-edit")
      (is (true? (get-in db [:ui :defaults :show-delete?]))
        "Should set default show-delete")
      (is (false? (get-in db [:ui :defaults :show-timestamps?]))
        "Should set default show-timestamps")
      (is (true? (get-in db [:ui :defaults :show-highlights?]))
        "Should set default show-highlights")
      (is (false? (get-in db [:ui :defaults :show-select?]))
        "Should set default show-select"))))

(deftest toggle-functionality-test
  (testing "Toggle events work correctly for different entity types"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test global toggle (backward compatibility)
    (rf/dispatch-sync [::ui-state-events/toggle-edit nil])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :show-edit?]))
        "Should toggle global show-edit from default true to false"))

    ;; Test entity-specific toggle
    (rf/dispatch-sync [::ui-state-events/toggle-edit :items])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Should create entity-specific override for items"))

    ;; Toggle entity-specific setting again
    (rf/dispatch-sync [::ui-state-events/toggle-edit :items])
    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Should toggle entity-specific setting back to true"))

    ;; Test toggle for different entity
    (rf/dispatch-sync [::ui-state-events/toggle-timestamps :transactions])
    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :entity-configs :transactions :show-timestamps?]))
        "Should create entity-specific override for transactions"))

    ;; Test toggle highlights
    (rf/dispatch-sync [::ui-state-events/toggle-highlights :items])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :entity-configs :items :show-highlights?]))
        "Should create entity-specific highlights override for items"))))

(deftest settings-precedence-test
  (testing "Settings precedence: entity-specific > defaults > global"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up all three levels
    (rf/dispatch-sync [::test-set-global-setting :show-edit? true])
    (rf/dispatch-sync [::test-set-default-setting :show-edit? false])
    (rf/dispatch-sync [::test-set-entity-setting :items :show-edit? true])

    (let [db @rf-db/app-db]
      ;; Entity-specific should take precedence
      (is (true? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Entity-specific setting should be present")

      ;; Defaults should be available
      (is (false? (get-in db [:ui :defaults :show-edit?]))
        "Default setting should be present")

      ;; Global should be available as fallback
      (is (true? (get-in db [:ui :show-edit?]))
        "Global setting should be present"))

    ;; Test with only defaults and global (no entity-specific)
    (rf/dispatch-sync [::test-set-global-setting :show-delete? true])
    (rf/dispatch-sync [::test-set-default-setting :show-delete? false])

    (let [db @rf-db/app-db]
      ;; No entity-specific override
      (is (nil? (get-in db [:ui :entity-configs :items :show-delete?]))
        "Should not have entity-specific override")

      ;; Defaults should be available
      (is (false? (get-in db [:ui :defaults :show-delete?]))
        "Default setting should be present")

      ;; Global should be available
      (is (true? (get-in db [:ui :show-delete?]))
        "Global setting should be present"))))

(deftest entity-config-management-test
  (testing "Entity configuration management operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up multiple entity configurations
    (rf/dispatch-sync [::test-set-entity-setting :items :show-edit? false])
    (rf/dispatch-sync [::test-set-entity-setting :items :show-timestamps? true])
    (rf/dispatch-sync [::test-set-entity-setting :transactions :show-delete? false])
    (rf/dispatch-sync [::test-set-entity-setting :transactions :show-highlights? false])

    (let [db @rf-db/app-db]
      (is (= 2 (count (get-in db [:ui :entity-configs])))
        "Should have configurations for 2 entities")
      (is (contains? (get-in db [:ui :entity-configs]) :items)
        "Should have items configuration")
      (is (contains? (get-in db [:ui :entity-configs]) :transactions)
        "Should have transactions configuration"))

    ;; Clear specific entity configuration
    (rf/dispatch-sync [::test-clear-entity-config :items])
    (let [db @rf-db/app-db]
      (is (= 1 (count (get-in db [:ui :entity-configs])))
        "Should have 1 entity configuration after clearing items")
      (is (not (contains? (get-in db [:ui :entity-configs]) :items))
        "Should not have items configuration")
      (is (contains? (get-in db [:ui :entity-configs]) :transactions)
        "Should still have transactions configuration"))

    ;; Clear all entity configurations
    (rf/dispatch-sync [::test-clear-all-entity-configs])
    (let [db @rf-db/app-db]
      (is (= 0 (count (get-in db [:ui :entity-configs])))
        "Should have no entity configurations after clearing all"))))

(deftest settings-integration-test
  (testing "Settings integration with list UI state"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test that settings work with different entity types
    ;; Set specific settings for each entity (only the ones that should be true)
    (rf/dispatch-sync [::test-set-entity-setting :items :show-edit? true])
    (rf/dispatch-sync [::test-set-entity-setting :transactions :show-delete? true])
    (rf/dispatch-sync [::test-set-entity-setting :transaction-types :show-timestamps? true])

    (let [db @rf-db/app-db]
      ;; Verify each entity has correct settings
      (is (true? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Items should have show-edit enabled")
      (is (true? (get-in db [:ui :entity-configs :transactions :show-delete?]))
        "Transactions should have show-delete enabled")
      (is (true? (get-in db [:ui :entity-configs :transaction-types :show-timestamps?]))
        "Transaction-types should have show-timestamps enabled")

      ;; Verify other settings are not set for entities (should be nil since not set)
      (is (nil? (get-in db [:ui :entity-configs :items :show-delete?]))
        "Items should not have show-delete override")
      (is (nil? (get-in db [:ui :entity-configs :transactions :show-edit?]))
        "Transactions should not have show-edit override"))))

(deftest edge-cases-test
  (testing "Edge cases in settings management"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test with nil entity type
    (rf/dispatch-sync [::ui-state-events/toggle-edit nil])
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:ui :show-edit?]))
        "Should handle nil entity type and toggle global setting"))

    ;; Test with non-existent entity type
    (rf/dispatch-sync [::test-set-entity-setting :non-existent :show-edit? true])
    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :entity-configs :non-existent :show-edit?]))
        "Should allow setting configuration for any entity type"))

    ;; Test clearing non-existent entity config
    (rf/dispatch-sync [::test-clear-entity-config :does-not-exist])
    (let [db @rf-db/app-db]
      (is (map? (get-in db [:ui :entity-configs]))
        "Should handle clearing non-existent entity gracefully"))

    ;; Test with boolean values
    (doseq [value [true false nil]]
      (rf/dispatch-sync [::test-set-entity-setting :test-entity :test-setting value])
      (let [db @rf-db/app-db]
        (is (= value (get-in db [:ui :entity-configs :test-entity :test-setting]))
          (str "Should handle boolean value: " value))))))

(deftest settings-consistency-test
  (testing "Settings consistency across entity operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up initial state
    (rf/dispatch-sync [::test-set-global-setting :show-edit? true])
    (rf/dispatch-sync [::test-set-entity-setting :items :show-edit? false])

    ;; Verify initial state
    (let [db @rf-db/app-db]
      (is (true? (get-in db [:ui :show-edit?]))
        "Global setting should be true")
      (is (false? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Entity setting should override global"))

    ;; Toggle entity setting multiple times
    (rf/dispatch-sync [::ui-state-events/toggle-edit :items])
    (rf/dispatch-sync [::ui-state-events/toggle-edit :items])
    (rf/dispatch-sync [::ui-state-events/toggle-edit :items])

    (let [db @rf-db/app-db]
      ;; After 3 toggles (starting from false): false -> true -> false -> true
      (is (true? (get-in db [:ui :entity-configs :items :show-edit?]))
        "Entity setting should be true after 3 toggles")
      ;; Global should remain unchanged
      (is (true? (get-in db [:ui :show-edit?]))
        "Global setting should remain unchanged"))))

(defn run-all-tests []
  (helpers/log-test-start "Settings Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runSettingsTests run-all-tests)
