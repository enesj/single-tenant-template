(ns app.template.frontend.list-operations-test
  "Tests for list operations including CRUD, sorting, pagination, and selection"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.state.normalize :as normalize]
    [cljs.test :refer [deftest is run-tests testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Register test events only once to avoid re-frame warnings
#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
;; Test helper functions to replace subscription logic
(defn get-items-from-db
  "Helper function to get items directly from app-db without using subscriptions"
  [db entity-type]
  (let [entity-data (get-in db (paths/entity-data entity-type) {})
        entity-ids (get-in db (paths/entity-ids entity-type) [])]
    (map entity-data entity-ids)))

(defn get-visible-items-from-db
  "Helper function to simulate visible-items subscription logic"
  [db entity-type]
  (let [items (get-items-from-db db entity-type)
        ui-state (get-in db (paths/list-ui-state entity-type) {})
        sort-config (:sort ui-state)
        current-page (:current-page ui-state 1)
        per-page (:per-page ui-state 10)]
    (cond->> items
      sort-config (sort-by (:field sort-config))
      (= :desc (:direction sort-config)) reverse
      true (drop (* (dec current-page) per-page))
      true (take per-page))))

(defn get-total-pages-from-db
  "Helper function to calculate total pages directly from app-db"
  [db entity-type]
  (let [items (get-items-from-db db entity-type)
        ui-state (get-in db (paths/list-ui-state entity-type) {})
        per-page (:per-page ui-state 10)
        total-items (count items)]
    (max 1 (js/Math.ceil (/ total-items per-page)))))

(defn get-selected-ids-from-db
  "Helper function to get selected IDs directly from app-db"
  [db entity-type]
  (get-in db (paths/entity-selected-ids entity-type) #{}))

;; Register test events only once to avoid re-frame warnings

(defonce list-ops-test-events-registered
  (do
    ;; Initialize test database
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))

    ;; Mock HTTP success for fetch operations
    (rf/reg-event-db
      ::test-fetch-success
      (fn [db [_ entity-type response]]
        (let [normalized (normalize/normalize-entities response)]
          (-> db
            (assoc-in (paths/entity-data entity-type) (:data normalized))
            (assoc-in (paths/entity-ids entity-type) (:ids normalized))
            (assoc-in (paths/entity-metadata entity-type)
              {:loading? false
               :error nil
               :last-updated (js/Date.now)})
            (assoc-in (paths/list-total-items entity-type) (count response))))))

    ;; Mock HTTP failure for fetch operations
    (rf/reg-event-db
      ::test-fetch-failure
      (fn [db [_ entity-type error-msg]]
        (assoc-in db (paths/entity-metadata entity-type)
                  {:loading? false
                   :error error-msg
                   :last-updated nil})))

    ;; Set entity loading state
    (rf/reg-event-db
      ::test-set-loading
      (fn [db [_ entity-type loading?]]
        (assoc-in db (paths/entity-loading? entity-type) loading?)))

    ;; Set list UI state
    (rf/reg-event-db
      ::test-set-list-ui-state
      (fn [db [_ entity-type ui-state]]
        (update-in db (paths/list-ui-state entity-type) merge ui-state)))

    ;; Set selected IDs
    (rf/reg-event-db
      ::test-set-selected-ids
      (fn [db [_ entity-type selected-ids]]
        (assoc-in db (paths/entity-selected-ids entity-type) (set selected-ids))))

    ;; Add entity to data
    (rf/reg-event-db
      ::test-add-entity
      (fn [db [_ entity-type entity]]
        (let [id (:id entity)
              current-data (get-in db (paths/entity-data entity-type) {})
              current-ids (get-in db (paths/entity-ids entity-type) [])]
          (-> db
            (assoc-in (paths/entity-data entity-type) (assoc current-data id entity))
            (assoc-in (paths/entity-ids entity-type) (conj current-ids id))))))

    ;; Remove entity from data
    (rf/reg-event-db
      ::test-remove-entity
      (fn [db [_ entity-type entity-id]]
        (let [current-data (get-in db (paths/entity-data entity-type) {})
              current-ids (get-in db (paths/entity-ids entity-type) [])]
          (-> db
            (assoc-in (paths/entity-data entity-type) (dissoc current-data entity-id))
            (assoc-in (paths/entity-ids entity-type) (vec (remove #(= % entity-id) current-ids)))))))

    true))
;; (defonce list-ops-test-events-registered
;;   (do
;;     ;; Initialize test database
;;     (rf/reg-event-db
;;      ::test-initialize-db
;;      (fn [_ _]
;;        helpers/valid-test-db-state))

;;     ;; Mock HTTP success for fetch operations
;;     (rf/reg-event-db
;;      ::test-fetch-success
;;      (fn [db [_ entity-type response]]
;;        (let [normalized (normalize/normalize-entities response)]
;;          (-> db
;;              (assoc-in (paths/entity-data entity-type) (:data normalized))
;;              (assoc-in (paths/entity-ids entity-type) (:ids normalized))
;;              (assoc-in (paths/entity-metadata entity-type)
;;                        {:loading? false
;;                         :error nil
;;                         :last-updated (js/Date.now)})
;;              (assoc-in (paths/list-total-items entity-type) (count response))))))

;;     ;; Mock HTTP failure for fetch operations
;;     (rf/reg-event-db
;;      ::test-fetch-failure
;;      (fn [db [_ entity-type error-msg]]
;;        (assoc-in db (paths/entity-metadata entity-type)
;;                  {:loading? false
;;                   :error error-msg
;;                   :last-updated nil})))

;;     ;; Set entity loading state
;;     (rf/reg-event-db
;;      ::test-set-loading
;;      (fn [db [_ entity-type loading?]]
;;        (assoc-in db (paths/entity-loading? entity-type) loading?)))

;;     ;; Set list UI state
;;     (rf/reg-event-db
;;      ::test-set-list-ui-state
;;      (fn [db [_ entity-type ui-state]]
;;        (update-in db (paths/list-ui-state entity-type) merge ui-state)))

;;     ;; Set selected IDs
;;     (rf/reg-event-db
;;      ::test-set-selected-ids
;;      (fn [db [_ entity-type selected-ids]]
;;        (assoc-in db (paths/entity-selected-ids entity-type) (set selected-ids))))

;;     ;; Add entity to data
;;     (rf/reg-event-db
;;      ::test-add-entity
;;      (fn [db [_ entity-type entity]]
;;        (let [id (:id entity)
;;              current-data (get-in db (paths/entity-data entity-type) {})
;;              current-ids (get-in db (paths/entity-ids entity-type) [])]
;;          (-> db
;;              (assoc-in (paths/entity-data entity-type) (assoc current-data id entity))
;;              (assoc-in (paths/entity-ids entity-type) (conj current-ids id))))))

;;     ;; Remove entity from data
;;     (rf/reg-event-db
;;      ::test-remove-entity
;;      (fn [db [_ entity-type entity-id]]
;;        (let [current-data (get-in db (paths/entity-data entity-type) {})
;;              current-ids (get-in db (paths/entity-ids entity-type) [])]
;;          (-> db
;;              (assoc-in (paths/entity-data entity-type) (dissoc current-data entity-id))
;;              (assoc-in (paths/entity-ids entity-type) (vec (remove #(= % entity-id) current-ids)))))))

;;     true))

(deftest entity-fetching-test
  (testing "Entity fetching operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test initial loading state
    (rf/dispatch-sync [::test-set-loading :items true])
    (is (true? (get-in @rf-db/app-db (paths/entity-loading? :items)))
      "Should set loading state to true")

    ;; Test successful fetch
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::test-fetch-success :items test-entities])

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
    (rf/dispatch-sync [::test-fetch-failure :items "Network error"])
    (let [db @rf-db/app-db]
      (is (false? (get-in db (paths/entity-loading? :items)))
        "Should set loading to false after failure")
      (is (= "Network error" (get-in db (paths/entity-error :items)))
        "Should store error message"))))

(deftest sorting-functionality-test
  (testing "List sorting operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Zebra" :amount 100}
                         {:id 2 :description "Alpha" :amount 300}
                         {:id 3 :description "Beta" :amount 200}]]
      (rf/dispatch-sync [::test-fetch-success :items test-entities])

      ;; Test sorting by description ascending
      (rf/dispatch-sync [::test-set-list-ui-state :items
                         {:sort {:field :description :direction :asc}
                          :current-page 1
                          :per-page 10}])

      (let [db @rf-db/app-db
            sorted-items (get-visible-items-from-db db :items)]
        (is (= ["Alpha" "Beta" "Zebra"]
              (map :description sorted-items))
          "Should sort by description ascending"))

      ;; Test sorting by description descending
      (rf/dispatch-sync [::test-set-list-ui-state :items
                         {:sort {:field :description :direction :desc}}])

      (let [db @rf-db/app-db
            sorted-items (get-visible-items-from-db db :items)]
        (is (= ["Zebra" "Beta" "Alpha"]
              (map :description sorted-items))
          "Should sort by description descending"))

      ;; Test sorting by amount ascending
      (rf/dispatch-sync [::test-set-list-ui-state :items
                         {:sort {:field :amount :direction :asc}}])

      (let [db @rf-db/app-db
            sorted-items (get-visible-items-from-db db :items)]
        (is (= [100 200 300]
              (map :amount sorted-items))
          "Should sort by amount ascending"))

      ;; Test sorting by amount descending
      (rf/dispatch-sync [::test-set-list-ui-state :items
                         {:sort {:field :amount :direction :desc}}])

      (let [db @rf-db/app-db
            sorted-items (get-visible-items-from-db db :items)]
        (is (= [300 200 100]
              (map :amount sorted-items))
          "Should sort by amount descending")))))

(deftest pagination-functionality-test
  (testing "List pagination operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data with more items than per-page
    (let [test-entities (map #(hash-map :id % :description (str "Item " %) :amount (* % 10))
                          (range 1 26))]                    ; 25 items
      (rf/dispatch-sync [::test-fetch-success :items test-entities])

      ;; Test first page with 10 items per page
      (rf/dispatch-sync [::test-set-list-ui-state :items
                         {:current-page 1
                          :per-page 10
                          :sort {:field :id :direction :asc}}])

      (let [db @rf-db/app-db
            visible-items (get-visible-items-from-db db :items)
            total-pages (get-total-pages-from-db db :items)]
        (is (= 10 (count visible-items))
          "Should show 10 items on first page")
        (is (= [1 2 3 4 5 6 7 8 9 10]
              (map :id visible-items))
          "Should show first 10 items by ID")
        (is (= 3 total-pages)
          "Should calculate 3 total pages for 25 items"))

      ;; Test second page
      (rf/dispatch-sync [::test-set-list-ui-state :items {:current-page 2}])

      (let [db @rf-db/app-db
            visible-items (get-visible-items-from-db db :items)]
        (is (= 10 (count visible-items))
          "Should show 10 items on second page")
        (is (= [11 12 13 14 15 16 17 18 19 20]
              (map :id visible-items))
          "Should show items 11-20 on second page"))

      ;; Test last page (partial)
      (rf/dispatch-sync [::test-set-list-ui-state :items {:current-page 3}])

      (let [db @rf-db/app-db
            visible-items (get-visible-items-from-db db :items)]
        (is (= 5 (count visible-items))
          "Should show 5 items on last page")
        (is (= [21 22 23 24 25]
              (map :id visible-items))
          "Should show items 21-25 on last page"))

      ;; Test different page size
      (rf/dispatch-sync [::test-set-list-ui-state :items
                         {:current-page 1
                          :per-page 7}])

      (let [db @rf-db/app-db
            visible-items (get-visible-items-from-db db :items)
            total-pages (get-total-pages-from-db db :items)]
        (is (= 7 (count visible-items))
          "Should show 7 items with per-page = 7")
        (is (= 4 total-pages)
          "Should calculate 4 total pages for 25 items with 7 per page")))))

(deftest selection-functionality-test
  (testing "Item selection operations"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Set up test data
    (let [test-entities [{:id 1 :description "Item 1" :amount 100}
                         {:id 2 :description "Item 2" :amount 200}
                         {:id 3 :description "Item 3" :amount 150}]]
      (rf/dispatch-sync [::test-fetch-success :items test-entities])

      ;; Test single selection
      (rf/dispatch-sync [::test-set-selected-ids :items [1]])
      (let [db @rf-db/app-db
            selected-ids (get-selected-ids-from-db db :items)]
        (is (= #{1} selected-ids)
          "Should select single item"))

      ;; Test multiple selection
      (rf/dispatch-sync [::test-set-selected-ids :items [1 3]])
      (let [db @rf-db/app-db
            selected-ids (get-selected-ids-from-db db :items)]
        (is (= #{1 3} selected-ids)
          "Should select multiple items"))

      ;; Test select all
      (rf/dispatch-sync [::test-set-selected-ids :items [1 2 3]])
      (let [db @rf-db/app-db
            selected-ids (get-selected-ids-from-db db :items)]
        (is (= #{1 2 3} selected-ids)
          "Should select all items"))

      ;; Test clear selection
      (rf/dispatch-sync [::test-set-selected-ids :items []])
      (let [db @rf-db/app-db
            selected-ids (get-selected-ids-from-db db :items)]
        (is (= #{} selected-ids)
          "Should clear all selections")))))

(deftest crud-operations-test
  (testing "CRUD operations on entity list"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test initial empty state
    (let [db @rf-db/app-db
          items (get-items-from-db db :items)]
      (is (= [] items)
        "Should start with empty items list"))

    ;; Test create (add entity)
    (rf/dispatch-sync [::test-add-entity :items {:id 1 :description "New Item" :amount 100}])
    (let [db @rf-db/app-db
          items (get-items-from-db db :items)]
      (is (= 1 (count items))
        "Should have one item after adding")
      (is (= "New Item" (:description (first items)))
        "Should add item with correct data"))

    ;; Test add multiple entities
    (rf/dispatch-sync [::test-add-entity :items {:id 2 :description "Second Item" :amount 200}])
    (rf/dispatch-sync [::test-add-entity :items {:id 3 :description "Third Item" :amount 150}])
    (let [db @rf-db/app-db
          items (get-items-from-db db :items)]
      (is (= 3 (count items))
        "Should have three items after adding multiple"))

    ;; Test delete operation
    (rf/dispatch-sync [::test-remove-entity :items 2])
    (let [db @rf-db/app-db
          items (get-items-from-db db :items)
          item-ids (map :id items)]
      (is (= 2 (count items))
        "Should have two items after deletion")
      (is (not (contains? (set item-ids) 2))
        "Should not contain deleted item")
      (is (= #{1 3} (set item-ids))
        "Should contain remaining items"))

    ;; Test update operation (via replace)
    (rf/dispatch-sync [::test-add-entity :items {:id 1 :description "Updated Item" :amount 150}])
    (let [db @rf-db/app-db
          items (get-items-from-db db :items)
          updated-item (first (filter #(= 1 (:id %)) items))]
      (is (= "Updated Item" (:description updated-item))
        "Should update item description")
      (is (= 150 (:amount updated-item))
        "Should update item amount"))))

(deftest list-ui-state-test
  (testing "List UI state management"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test setting sort configuration
    (rf/dispatch-sync [::test-set-list-ui-state :items
                       {:sort {:field :description :direction :asc}}])
    (let [db @rf-db/app-db
          ui-state (get-in db (paths/list-ui-state :items))
          sort-config (:sort ui-state)]
      (is (= :description (:field sort-config))
        "Should set sort field")
      (is (= :asc (:direction sort-config))
        "Should set sort direction"))

    ;; Test updating pagination settings
    (rf/dispatch-sync [::test-set-list-ui-state :items
                       {:current-page 2 :per-page 20}])
    (let [db @rf-db/app-db
          ui-state (get-in db (paths/list-ui-state :items))]
      (is (= 2 (:current-page ui-state))
        "Should set current page")
      (is (= 20 (:per-page ui-state))
        "Should set per-page count"))

    ;; Test multiple UI state updates
    (rf/dispatch-sync [::test-set-list-ui-state :items
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
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test loading state
    (rf/dispatch-sync [::test-set-loading :items true])
    (let [db @rf-db/app-db
          loading? (get-in db (paths/entity-loading? :items))]
      (is (true? loading?)
        "Should track loading state"))

    ;; Test error state
    (rf/dispatch-sync [::test-fetch-failure :items "Test error"])
    (let [db @rf-db/app-db
          loading? (get-in db (paths/entity-loading? :items))
          error (get-in db (paths/entity-error :items))]
      (is (false? loading?)
        "Should set loading to false on error")
      (is (= "Test error" error)
        "Should store error message"))

    ;; Test successful load clears error
    (rf/dispatch-sync [::test-fetch-success :items [{:id 1 :description "Item 1"}]])
    (let [db @rf-db/app-db
          loading? (get-in db (paths/entity-loading? :items))
          error (get-in db (paths/entity-error :items))
          items (get-items-from-db db :items)]
      (is (false? loading?)
        "Should set loading to false on success")
      (is (nil? error)
        "Should clear error on success")
      (is (= 1 (count items))
        "Should have loaded items"))))

;; Test error state
;;(rf/dispatch-sync [::test-fetch-failure :items "Test error"])
;;(let [entity-list @(rf/subscribe [::list-subs/entity-list :items])]
;;  (is (false? (:loading? entity-list))
;;      "Should set loading to false on error")
;;  (is (= "Test error" (:error entity-list))
;;      "Should store error message"))

;; Test successful load clears error
;;(rf/dispatch-sync [::test-fetch-success :items [{:id 1 :description "Item 1"}]])
;;(let [entity-list @(rf/subscribe [::list-subs/entity-list :items])]
;;  (is (false? (:loading? entity-list))
;;      "Should set loading to false on success")
;;  (is (nil? (:error entity-list))
;;      "Should clear error on success")
;;  (is (= 1 (count (:items entity-list)))
;;      "Should have loaded items"))))

(deftest edge-cases-test
  (testing "Edge cases and error conditions"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Test empty data handling
    (rf/dispatch-sync [::test-fetch-success :items []])
    (let [db @rf-db/app-db
          items (get-items-from-db db :items)
          visible-items (get-visible-items-from-db db :items)]
      (is (= [] items)
        "Should handle empty items list")
      (is (= [] visible-items)
        "Should handle empty visible items"))

    ;; Test pagination with no items
    (rf/dispatch-sync [::test-set-list-ui-state :items {:current-page 2 :per-page 10}])
    (let [db @rf-db/app-db
          visible-items (get-visible-items-from-db db :items)
          total-pages (get-total-pages-from-db db :items)]
      (is (= [] visible-items)
        "Should return empty array for pagination with no items")
      (is (= 1 total-pages)
        "Should return 1 page minimum even with no items"))

    ;; Test page beyond available data
    (rf/dispatch-sync [::test-fetch-success :items [{:id 1 :description "Only item"}]])
    (rf/dispatch-sync [::test-set-list-ui-state :items {:current-page 5 :per-page 10}])
    (let [db @rf-db/app-db
          visible-items (get-visible-items-from-db db :items)]
      (is (= [] visible-items)
        "Should return empty array for page beyond data"))

    ;; Test nil entity type handling - this tests app-db paths behavior
    (let [db @rf-db/app-db
          items (get-items-from-db db nil)]
      (is (= [] items)
        "Should handle nil entity type gracefully"))))

(defn run-all-tests []
  (helpers/log-test-start "List Operations Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runListOperationsTests run-all-tests)
