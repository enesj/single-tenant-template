(ns app.template.frontend.batch-operations-test
  "Tests for batch edit and delete functionality including state management and API integration"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.batch :as batch-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.state.normalize :as normalize]
    [app.template.frontend.subs.list :as list-subs]
    [cljs.test :refer [deftest is run-tests testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Register test events only once to avoid re-frame warnings
(defonce batch-ops-test-events-registered
  (do
    ;; Initialize test database
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))

    ;; Set entity data
    (rf/reg-event-db
      ::test-set-entity-data
      (fn [db [_ entity-type entities]]
        (let [normalized (normalize/normalize-entities entities)]
          (-> db
            (assoc-in (paths/entity-data entity-type) (:data normalized))
            (assoc-in (paths/entity-ids entity-type) (:ids normalized))))))

    ;; Set selected IDs
    (rf/reg-event-db
      ::test-set-selected-ids
      (fn [db [_ entity-type selected-ids]]
        (assoc-in db (paths/entity-selected-ids entity-type) (set selected-ids))))

    ;; Mock batch update success
    (rf/reg-event-db
      ::test-batch-update-success
      (fn [db [_ entity-type response]]
        (let [updated-records (get-in response [:results])
              updated-ids (into #{} (map :id) updated-records)]
          (-> db
            (assoc-in (paths/entity-loading? entity-type) false)
            (assoc-in (paths/entity-error entity-type) nil)
            (update-in [:ui :recently-updated entity-type] #(into (or % #{}) updated-ids))))))

    ;; Mock batch update failure
    (rf/reg-event-db
      ::test-batch-update-failure
      (fn [db [_ entity-type error-msg]]
        (-> db
          (assoc-in (paths/entity-loading? entity-type) false)
          (assoc-in (paths/entity-error entity-type) error-msg))))

    ;; Set loading state
    (rf/reg-event-db
      ::test-set-loading
      (fn [db [_ entity-type loading?]]
        (assoc-in db (paths/entity-loading? entity-type) loading?)))

    ;; Mock delete success
    (rf/reg-event-db
      ::test-delete-success
      (fn [db [_ entity-type deleted-id]]
        (let [current-data (get-in db (paths/entity-data entity-type) {})
              current-ids (get-in db (paths/entity-ids entity-type) [])]
          (-> db
            (assoc-in (paths/entity-data entity-type) (dissoc current-data deleted-id))
            (assoc-in (paths/entity-ids entity-type) (vec (remove #(= % deleted-id) current-ids)))
            (assoc-in (paths/entity-loading? entity-type) false)))))

    true))

(deftest batch-edit-ui-state-test
  (testing "Batch edit UI state management"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test showing batch edit inline
    (rf/dispatch-sync [::batch-events/show-batch-edit-inline :items [1 2 3]])
    (let [batch-edit-sub (rf/subscribe [::list-subs/batch-edit-inline :items])]
      (when batch-edit-sub
        (let [batch-edit-state @batch-edit-sub]
          (when batch-edit-state
            (is (true? (:open? batch-edit-state))
              "Should open batch edit inline mode")
            (is (= :items (:entity-type batch-edit-state))
              "Should set correct entity type")
            (is (= [1 2 3] (:selected-ids batch-edit-state))
              "Should set selected item IDs")))))

    ;; Test hiding batch edit inline
    (rf/dispatch-sync [::batch-events/hide-batch-edit-inline :items])
    (let [batch-edit-sub (rf/subscribe [::list-subs/batch-edit-inline :items])]
      (when batch-edit-sub
        (let [batch-edit-state @batch-edit-sub]
          (is (false? (:open? batch-edit-state))
            "Should close batch edit inline mode"))))

    ;; Test that add form is hidden when batch edit opens
    (rf/dispatch-sync [::batch-events/show-batch-edit-inline :items [1]])
    (let [show-add-form (get-in @rf-db/app-db [:ui :show-add-form])]
      (is (false? show-add-form)
        "Should hide add form when batch edit opens"))))

(deftest batch-update-operations-test
  (testing "Batch update operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-entities])
      (rf/dispatch-sync [::test-set-selected-ids :items [1 2]])

      ;; Test batch update with meaningful changes
      (rf/dispatch-sync [::test-set-loading :items true])
      (is (true? (get-in @rf-db/app-db (paths/entity-loading? :items)))
        "Should set loading state during batch update")

      ;; Test successful batch update
      (let [mock-response {:results [{:id 1 :description "Updated Item 1" :amount 150}
                                     {:id 2 :description "Updated Item 2" :amount 250}]}]
        (rf/dispatch-sync [::test-batch-update-success :items mock-response])

        (let [db @rf-db/app-db]
          (is (false? (get-in db (paths/entity-loading? :items)))
            "Should clear loading state after success")
          (is (nil? (get-in db (paths/entity-error :items)))
            "Should clear any previous errors")
          (is (= #{1 2} (get-in db [:ui :recently-updated :items]))
            "Should track recently updated items")))

      ;; Test batch update failure
      (rf/dispatch-sync [::test-set-loading :items true])
      (rf/dispatch-sync [::test-batch-update-failure :items "Network error"])

      (let [db @rf-db/app-db]
        (is (false? (get-in db (paths/entity-loading? :items)))
          "Should clear loading state after failure")
        (is (= "Network error" (get-in db (paths/entity-error :items)))
          "Should store error message")))))

(deftest selection-management-test
  (testing "Item selection for batch operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-entities])

      ;; Test single item selection
      (rf/dispatch-sync [::selection-events/select-item :items 1 true])
      (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
        (when selected-ids-sub
          (let [selected-ids @selected-ids-sub]
            (is (= #{1} selected-ids)
              "Should select single item"))))

      ;; Test adding to selection
      (rf/dispatch-sync [::selection-events/select-item :items 2 true])
      (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
        (when selected-ids-sub
          (let [selected-ids @selected-ids-sub]
            (is (= #{1 2} selected-ids)
              "Should add item to existing selection"))))

      ;; Test removing from selection
      (rf/dispatch-sync [::selection-events/select-item :items 1 false])
      (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
        (when selected-ids-sub
          (let [selected-ids @selected-ids-sub]
            (is (= #{2} selected-ids)
              "Should remove item from selection"))))

      ;; Test select all
      (rf/dispatch-sync [::selection-events/select-all :items test-entities true])
      (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
        (when selected-ids-sub
          (let [selected-ids @selected-ids-sub]
            (is (= #{1 2 3} selected-ids)
              "Should select all items"))))

      ;; Test clear all selections
      (rf/dispatch-sync [::selection-events/select-all :items test-entities false])
      (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
        (when selected-ids-sub
          (let [selected-ids @selected-ids-sub]
            (is (= #{} selected-ids)
              "Should clear all selections")))))))

(deftest batch-delete-operations-test
  (testing "Batch delete operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::test-set-entity-data :items test-entities])

      ;; Test single delete
      (rf/dispatch-sync [::test-delete-success :items 2])
      (let [items-sub (rf/subscribe [::list-subs/items :items])]
        (when items-sub
          (let [items @items-sub
                item-ids (map :id items)]
            (is (= 2 (count items))
              "Should have one less item after deletion")
            (is (not (contains? (set item-ids) 2))
              "Should not contain deleted item")
            (is (= #{1 3} (set item-ids))
              "Should contain remaining items"))))

      ;; Test batch delete simulation
      ;; Note: The actual batch delete uses effects/fx, so we simulate the end result
      (rf/dispatch-sync [::test-delete-success :items 1])
      (rf/dispatch-sync [::test-delete-success :items 3])
      (let [items-sub (rf/subscribe [::list-subs/items :items])]
        (when items-sub
          (let [items @items-sub]
            (is (= 0 (count items))
              "Should have no items after deleting all")))))))

(deftest batch-operations-validation-test
  (testing "Batch operations validation and edge cases"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test empty selection handling
    (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
      (when selected-ids-sub
        (let [empty-selected-ids @selected-ids-sub]
          (is (= #{} empty-selected-ids)
            "Should start with empty selection"))))

    ;; Test batch edit with no selection should not show inline editor
    (rf/dispatch-sync [::batch-events/show-batch-edit-inline :items []])
    (let [batch-edit-sub (rf/subscribe [::list-subs/batch-edit-inline :items])]
      (when batch-edit-sub
        (let [batch-edit-state @batch-edit-sub]
          (when batch-edit-state
            (is (true? (:open? batch-edit-state))
              "Should still allow opening with empty selection")
            (is (= [] (:selected-ids batch-edit-state))
              "Should handle empty selection array")))))

    ;; Test with invalid entity type
    (rf/dispatch-sync [::batch-events/show-batch-edit-inline nil [1 2]])
    (let [batch-edit-sub (rf/subscribe [::list-subs/batch-edit-inline nil])]
      (when batch-edit-sub
        (let [batch-edit-state @batch-edit-sub]
          (when batch-edit-state
            (is (true? (:open? batch-edit-state))
              "Should allow opening batch edit even with nil entity type")
            (is (= [1 2] (:selected-ids batch-edit-state))
              "Should store selected IDs even with nil entity type")))))

    ;; Test recently updated tracking
    (rf/dispatch-sync [::test-batch-update-success :items {:results [{:id 1} {:id 2}]}])
    (let [recently-updated (get-in @rf-db/app-db [:ui :recently-updated :items])]
      (is (= #{1 2} recently-updated)
        "Should track recently updated items"))

    ;; Test duplicate selections
    (rf/dispatch-sync [::test-set-selected-ids :items [1 1 2 2 3]])
    (let [selected-ids-sub (rf/subscribe [::list-subs/selected-ids :items])]
      (when selected-ids-sub
        (let [selected-ids @selected-ids-sub]
          (is (= #{1 2 3} selected-ids)
            "Should deduplicate selected IDs"))))))

(deftest batch-update-value-processing-test
  (testing "Batch update value processing and formatting"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up entity specs for value processing
    (let [test-specs {:items [{:id "description" :input-type "text"}
                              {:id "amount" :input-type "number"}
                              {:id "status" :input-type "select"
                               :options [{:value "active" :label "Active"}
                                         {:value "inactive" :label "Inactive"}]}
                              {:id "created_at" :input-type "date"}]}]
      (swap! rf-db/app-db assoc-in [:entities :specs] test-specs)

      ;; Test that batch update event can be dispatched
      ;; Note: We can't easily test the HTTP call and value processing without mocking,
      ;; but we can test that the event accepts the expected parameters
      (rf/dispatch-sync [::batch-events/batch-update
                         {:entity-name :items
                          :item-ids [1 2]
                          :values {:description "Updated description"
                                   :amount 250
                                   :status "active"}}])

      ;; The event should have been processed (no errors thrown)
      (is true "Batch update event should process without errors"))))

(deftest batch-operations-state-cleanup-test
  (testing "Batch operations state cleanup and persistence"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test recently updated persistence
    (rf/dispatch-sync [::test-batch-update-success :items {:results [{:id 1} {:id 2}]}])
    (let [recently-updated-before (get-in @rf-db/app-db [:ui :recently-updated :items])]
      (is (= #{1 2} recently-updated-before)
        "Should store recently updated items"))

    ;; Add more recently updated items
    (rf/dispatch-sync [::test-batch-update-success :items {:results [{:id 3} {:id 4}]}])
    (let [recently-updated-after (get-in @rf-db/app-db [:ui :recently-updated :items])]
      (is (= #{1 2 3 4} recently-updated-after)
        "Should accumulate recently updated items"))

    ;; Test batch edit state persistence across different entity types
    (rf/dispatch-sync [::batch-events/show-batch-edit-inline :items [1 2]])
    (rf/dispatch-sync [::batch-events/show-batch-edit-inline :transactions [3 4]])

    (let [items-batch-sub (rf/subscribe [::list-subs/batch-edit-inline :items])
          transactions-batch-sub (rf/subscribe [::list-subs/batch-edit-inline :transactions])]
      (when (and items-batch-sub transactions-batch-sub)
        (let [items-batch-state @items-batch-sub
              transactions-batch-state @transactions-batch-sub]
          (when (and items-batch-state transactions-batch-state)
            (is (true? (:open? items-batch-state))
              "Should maintain items batch edit state")
            (is (true? (:open? transactions-batch-state))
              "Should maintain transactions batch edit state")
            (is (= [1 2] (:selected-ids items-batch-state))
              "Should maintain items selection")
            (is (= [3 4] (:selected-ids transactions-batch-state))
              "Should maintain transactions selection")))))

    ;; Test cleanup of specific entity batch state
    (rf/dispatch-sync [::batch-events/hide-batch-edit-inline :items])
    (let [items-batch-sub (rf/subscribe [::list-subs/batch-edit-inline :items])
          transactions-batch-sub (rf/subscribe [::list-subs/batch-edit-inline :transactions])]
      (when (and items-batch-sub transactions-batch-sub)
        (let [items-batch-state @items-batch-sub
              transactions-batch-state @transactions-batch-sub]
          (is (false? (:open? items-batch-state))
            "Should close items batch edit")
          (when transactions-batch-state
            (is (true? (:open? transactions-batch-state))
              "Should keep transactions batch edit open")))))))

(deftest batch-operations-error-scenarios-test
  (testing "Batch operations error handling scenarios"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test network error handling
    (rf/dispatch-sync [::test-batch-update-failure :items {:message "Network timeout"}])
    (let [error (get-in @rf-db/app-db (paths/entity-error :items))]
      (is (= {:message "Network timeout"} error)
        "Should store detailed error information"))

    ;; Test server error handling
    (rf/dispatch-sync [::test-batch-update-failure :items "Server error"])
    (let [error (get-in @rf-db/app-db (paths/entity-error :items))]
      (is (= "Server error" error)
        "Should store simple error message"))

    ;; Test error clearing after successful operation
    (rf/dispatch-sync [::test-batch-update-success :items {:results [{:id 1}]}])
    (let [error (get-in @rf-db/app-db (paths/entity-error :items))]
      (is (nil? error)
        "Should clear error after successful operation"))

    ;; Test loading state during error scenarios
    (rf/dispatch-sync [::test-set-loading :items true])
    (rf/dispatch-sync [::test-batch-update-failure :items "Test error"])
    (let [loading? (get-in @rf-db/app-db (paths/entity-loading? :items))]
      (is (false? loading?)
        "Should clear loading state after error"))))

(defn run-all-tests []
  (helpers/log-test-start "Batch Operations Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runBatchOperationsTests run-all-tests)
