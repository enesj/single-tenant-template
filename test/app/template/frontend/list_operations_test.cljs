(ns app.template.frontend.list-operations-test
  "Tests for list operations including CRUD, sorting, pagination, and selection"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.list-test-utils :as utils]
    [cljs.test :refer [deftest is run-tests testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest entity-fetching-test
  (testing "Entity fetching operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Test initial loading state
    (rf/dispatch-sync [::utils/test-set-loading :items true])
    (is (true? (get-in @rf-db/app-db (paths/entity-loading? :items)))
      "Should set loading state to true")

    ;; Test successful fetch
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::utils/test-fetch-success :items test-entities])

      (let [db @rf-db/app-db]
        ;; Check that data was normalized correctly
        (is (= {1 {:id 1 :description "Item 1" :amount 100}
                2 {:id 2 :description "Item 2" :amount 200}
                3 {:id 3 :description "Item 3" :amount 150}}
              (get-in db (paths/entity-data :items)))
          "Should store normalized entity data")

        (is (= [1 2 3] (get-in db (paths/entity-ids :items)))
          "Should store entity IDs in order")

        (is (false? (get-in db (paths/entity-loading? :items)))
          "Should set loading to false after success")

        (is (nil? (get-in db (paths/entity-error :items)))
          "Should clear any previous errors")

        (is (some? (get-in db (paths/entity-last-updated :items)))
          "Should set last updated timestamp")))

    ;; Test fetch failure
    (rf/dispatch-sync [::utils/test-fetch-failure :items "Network error"])
    (let [db @rf-db/app-db]
      (is (false? (get-in db (paths/entity-loading? :items)))
        "Should set loading to false after failure")
      (is (= "Network error" (get-in db (paths/entity-error :items)))
        "Should store error message"))))

(deftest sorting-functionality-test
  (testing "List sorting operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Zebra" :amount 100}
                         {:id 2 :description "Alpha" :amount 300}
                         {:id 3 :description "Beta" :amount 200}]]
      (rf/dispatch-sync [::utils/test-fetch-success :items test-entities])

      ;; Test sorting by description ascending
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                         {:sort {:field :description :direction :asc}
                          :current-page 1
                          :per-page 10}])

      (let [db @rf-db/app-db
            sorted-items (utils/get-visible-items-from-db db :items)]
        (is (= ["Alpha" "Beta" "Zebra"]
              (map :description sorted-items))
          "Should sort by description ascending"))

      ;; Test sorting by description descending
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                         {:sort {:field :description :direction :desc}}])

      (let [db @rf-db/app-db
            sorted-items (utils/get-visible-items-from-db db :items)]
        (is (= ["Zebra" "Beta" "Alpha"]
              (map :description sorted-items))
          "Should sort by description descending"))

      ;; Test sorting by amount ascending
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                         {:sort {:field :amount :direction :asc}}])

      (let [db @rf-db/app-db
            sorted-items (utils/get-visible-items-from-db db :items)]
        (is (= [100 200 300]
              (map :amount sorted-items))
          "Should sort by amount ascending"))

      ;; Test sorting by amount descending
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                         {:sort {:field :amount :direction :desc}}])

      (let [db @rf-db/app-db
            sorted-items (utils/get-visible-items-from-db db :items)]
        (is (= [300 200 100]
              (map :amount sorted-items))
          "Should sort by amount descending")))))

(deftest pagination-functionality-test
  (testing "List pagination operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Set up test data with more items than per-page
    (let [test-entities (map #(hash-map :id % :description (str "Item " %) :amount (* % 10))
                          (range 1 26))]                    ; 25 items
      (rf/dispatch-sync [::utils/test-fetch-success :items test-entities])

      ;; Test first page with 10 items per page
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                         {:current-page 1
                          :per-page 10
                          :sort {:field :id :direction :asc}}])

      (let [db @rf-db/app-db
            visible-items (utils/get-visible-items-from-db db :items)
            total-pages (utils/get-total-pages-from-db db :items)]
        (is (= 10 (count visible-items))
          "Should show 10 items on first page")
        (is (= [1 2 3 4 5 6 7 8 9 10]
              (map :id visible-items))
          "Should show first 10 items by ID")
        (is (= 3 total-pages)
          "Should calculate 3 total pages for 25 items"))

      ;; Test second page
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items {:current-page 2}])

      (let [db @rf-db/app-db
            visible-items (utils/get-visible-items-from-db db :items)]
        (is (= 10 (count visible-items))
          "Should show 10 items on second page")
        (is (= [11 12 13 14 15 16 17 18 19 20]
              (map :id visible-items))
          "Should show items 11-20 on second page"))

      ;; Test last page (partial)
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items {:current-page 3}])

      (let [db @rf-db/app-db
            visible-items (utils/get-visible-items-from-db db :items)]
        (is (= 5 (count visible-items))
          "Should show 5 items on last page")
        (is (= [21 22 23 24 25]
              (map :id visible-items))
          "Should show items 21-25 on last page"))

      ;; Test different page size
      (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                         {:current-page 1
                          :per-page 7}])

      (let [db @rf-db/app-db
            visible-items (utils/get-visible-items-from-db db :items)
            total-pages (utils/get-total-pages-from-db db :items)]
        (is (= 7 (count visible-items))
          "Should show 7 items with per-page = 7")
        (is (= 4 total-pages)
          "Should calculate 4 total pages for 25 items with 7 per page")))))

(deftest selection-functionality-test
  (testing "Item selection operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::utils/test-fetch-success :items test-entities])

      ;; Test single selection
      (rf/dispatch-sync [::utils/test-set-selected-ids :items [1]])
      (let [db @rf-db/app-db
            selected-ids (utils/get-selected-ids-from-db db :items)]
        (is (= #{1} selected-ids)
          "Should select single item"))

      ;; Test multiple selection
      (rf/dispatch-sync [::utils/test-set-selected-ids :items [1 3]])
      (let [db @rf-db/app-db
            selected-ids (utils/get-selected-ids-from-db db :items)]
        (is (= #{1 3} selected-ids)
          "Should select multiple items"))

      ;; Test select all
      (rf/dispatch-sync [::utils/test-set-selected-ids :items [1 2 3]])
      (let [db @rf-db/app-db
            selected-ids (utils/get-selected-ids-from-db db :items)]
        (is (= #{1 2 3} selected-ids)
          "Should select all items"))

      ;; Test clear selection
      (rf/dispatch-sync [::utils/test-set-selected-ids :items []])
      (let [db @rf-db/app-db
            selected-ids (utils/get-selected-ids-from-db db :items)]
        (is (= #{} selected-ids)
          "Should clear all selections")))))

(deftest crud-operations-test
  (testing "CRUD operations on entity list"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Test initial empty state
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db :items)]
      (is (= [] items)
        "Should start with empty items list"))

    ;; Test create (add entity)
    (rf/dispatch-sync [::utils/test-add-entity :items {:id 1 :description "New Item" :amount 100}])
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db :items)]
      (is (= 1 (count items))
        "Should have one item after adding")
      (is (= "New Item" (:description (first items)))
        "Should add item with correct data"))

    ;; Test add multiple entities
    (rf/dispatch-sync [::utils/test-add-entity :items {:id 2 :description "Second Item" :amount 200}])
    (rf/dispatch-sync [::utils/test-add-entity :items {:id 3 :description "Third Item" :amount 150}])
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db :items)]
      (is (= 3 (count items))
        "Should have three items after adding multiple"))

    ;; Test delete operation
    (rf/dispatch-sync [::utils/test-remove-entity :items 2])
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db :items)
          item-ids (map :id items)]
      (is (= 2 (count items))
        "Should have two items after deletion")
      (is (not (contains? (set item-ids) 2))
        "Should not contain deleted item")
      (is (= #{1 3} (set item-ids))
        "Should contain remaining items"))

    ;; Test update operation (via replace)
    (rf/dispatch-sync [::utils/test-add-entity :items {:id 1 :description "Updated Item" :amount 150}])
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db :items)
          updated-item (first (filter #(= 1 (:id %)) items))]
      (is (= "Updated Item" (:description updated-item))
        "Should update item description")
      (is (= 150 (:amount updated-item))
        "Should update item amount"))))

(deftest list-ui-state-test
  (testing "List UI state management"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Test setting sort configuration
    (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                       {:sort {:field :description :direction :asc}}])
    (let [db @rf-db/app-db
          ui-state (get-in db (paths/list-ui-state :items))
          sort-config (:sort ui-state)]
      (is (= :description (:field sort-config))
        "Should set sort field")
      (is (= :asc (:direction sort-config))
        "Should set sort direction"))

    ;; Test updating pagination settings
    (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                       {:current-page 2 :per-page 20}])
    (let [db @rf-db/app-db
          ui-state (get-in db (paths/list-ui-state :items))]
      (is (= 2 (:current-page ui-state))
        "Should set current page")
      (is (= 20 (:per-page ui-state))
        "Should set per-page count"))

    ;; Test multiple UI state updates
    (rf/dispatch-sync [::utils/test-set-list-ui-state :items
                       {:sort {:field :amount :direction :desc}
                        :current-page 1
                        :per-page 15}])
    (let [db @rf-db/app-db
          ui-state (get-in db (paths/list-ui-state :items))]
      (is (= :amount (get-in ui-state [:sort :field]))
        "Should update sort field")
      (is (= :desc (get-in ui-state [:sort :direction]))
        "Should update sort direction")
      (is (= 1 (:current-page ui-state))
        "Should update current page")
      (is (= 15 (:per-page ui-state))
        "Should update per-page count"))))

(deftest entity-metadata-test
  (testing "Entity metadata tracking"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Test loading state
    (rf/dispatch-sync [::utils/test-set-loading :items true])
    (let [db @rf-db/app-db
          loading? (get-in db (paths/entity-loading? :items))]
      (is (true? loading?)
        "Should track loading state"))

    ;; Test error state
    (rf/dispatch-sync [::utils/test-fetch-failure :items "Test error"])
    (let [db @rf-db/app-db
          loading? (get-in db (paths/entity-loading? :items))
          error (get-in db (paths/entity-error :items))]
      (is (false? loading?)
        "Should set loading to false on error")
      (is (= "Test error" error)
        "Should store error message"))

    ;; Test successful load clears error
    (rf/dispatch-sync [::utils/test-fetch-success :items [{:id 1 :description "Item 1"}]])
    (let [db @rf-db/app-db
          loading? (get-in db (paths/entity-loading? :items))
          error (get-in db (paths/entity-error :items))
          items (utils/get-items-from-db db :items)]
      (is (false? loading?)
        "Should set loading to false on success")
      (is (nil? error)
        "Should clear error on success")
      (is (= 1 (count items))
        "Should have loaded items"))))

(deftest edge-cases-test
  (testing "Edge cases and error conditions"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::utils/test-initialize-db])

    ;; Test empty data handling
    (rf/dispatch-sync [::utils/test-fetch-success :items []])
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db :items)
          visible-items (utils/get-visible-items-from-db db :items)]
      (is (= [] items)
        "Should handle empty items list")
      (is (= [] visible-items)
        "Should handle empty visible items"))

    ;; Test pagination with no items
    (rf/dispatch-sync [::utils/test-set-list-ui-state :items {:current-page 2 :per-page 10}])
    (let [db @rf-db/app-db
          visible-items (utils/get-visible-items-from-db db :items)
          total-pages (utils/get-total-pages-from-db db :items)]
      (is (= [] visible-items)
        "Should return empty array for pagination with no items")
      (is (= 1 total-pages)
        "Should return 1 page minimum even with no items"))

    ;; Test page beyond available data
    (rf/dispatch-sync [::utils/test-fetch-success :items [{:id 1 :description "Only item"}]])
    (rf/dispatch-sync [::utils/test-set-list-ui-state :items {:current-page 5 :per-page 10}])
    (let [db @rf-db/app-db
          visible-items (utils/get-visible-items-from-db db :items)]
      (is (= [] visible-items)
        "Should return empty array for page beyond data"))

    ;; Test nil entity type handling - this tests app-db paths behavior
    (let [db @rf-db/app-db
          items (utils/get-items-from-db db nil)]
      (is (= [] items)
        "Should handle nil entity type gracefully"))))

(defn run-all-tests []
  (helpers/log-test-start "List Operations Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runListOperationsTests run-all-tests)
