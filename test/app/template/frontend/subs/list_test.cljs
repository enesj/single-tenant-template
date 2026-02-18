(ns app.template.frontend.subs.list-test
  "Tests for list subscriptions"
  (:require
    [app.template.frontend.subs.list :as list-subs]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  [db]
  (reset! rf-db/app-db db)
  (rf/clear-subscription-cache!))

(deftest entity-list-test
  (testing "entity-list returns items with metadata"
    (reset-db! {:entities {:items {:data {1 {:id 1 :name "A"}
                                          2 {:id 2 :name "B"}}
                                   :ids [1 2]
                                   :metadata {:loading? false :error nil}}}
                :ui {:lists {:items {}}}})
    (is (= {:items [{:id 1 :name "A"} {:id 2 :name "B"}]
            :loading? false
            :error nil}
          @(rf/subscribe [::list-subs/entity-list :items])))))

(deftest visible-items-test
  (testing "visible-items applies sort and pagination"
    (reset-db! {:entities {:items {:data {1 {:id 1 :amount 200}
                                          2 {:id 2 :amount 100}
                                          3 {:id 3 :amount 150}}
                                   :ids [1 2 3]}}
                :ui {:lists {:items {:sort {:field :amount :direction :desc}
                                     :per-page 2
                                     :pagination {:current-page 1 :per-page 2}}
                             :expenses {}}}})
    (let [visible @(rf/subscribe [::list-subs/visible-items :items])]
      (is (= [200 150] (map :amount visible)) "Should sort descending and limit to per-page count"))

    (swap! rf-db/app-db assoc-in [:ui :lists :items :pagination :current-page] 2)
    (rf/clear-subscription-cache!)
    (is (= [100]
          (map :amount @(rf/subscribe [::list-subs/visible-items :items])))
      "Second page should contain remaining element")))

(deftest visible-items-server-mode-filtering-test
  (testing "visible-items applies active filters but skips local sort/pagination in :server mode"
    (reset-db! {:entities {:items {:data {1 {:id 1 :amount 200}
                                          2 {:id 2 :amount 100}
                                          3 {:id 3 :amount 150}}
                                   :ids [1 2 3]}}
                :ui {:lists {:items {:pagination-mode :server
                                     :sort {:field :amount :direction :desc}
                                     :filters {:id "1"}
                                     :per-page 1
                                     :pagination {:current-page 2 :per-page 1}}}}})
    (is (= [1]
          (map :id @(rf/subscribe [::list-subs/visible-items :items])))
      "Server mode should still respect active filters, even on page > 1")))

(deftest filtering-test
  (testing "Filtered items combine multiple filter types"
    (reset-db! {:entities {:items {:data {1 {:id 1 :description "Apple Pie" :amount 50 :status "active" :created_at "2024-03-01"}
                                          2 {:id 2 :description "Apple Orange" :amount 150 :status "pending" :created_at "2024-04-01"}
                                          3 {:id 3 :description "apple juice" :amount 200 :status "inactive" :created_at "2024-05-01"}}
                                   :ids [1 2 3]}}
                :ui {:lists {:items {:filters {:description "apple"
                                               :amount {:min 100}
                                               :status [{:value "active" :label "Active"}
                                                        {:value "pending" :label "Pending"}]}}}}})
    (let [filtered @(rf/subscribe [::list-subs/filtered-items :items])]
      (is (= [2]
            (map :id filtered))
        "Should match apple text, amount >= 100, and status in set"))

    (swap! rf-db/app-db assoc-in [:ui :lists :items :filters]
      {:created_at {:from (js/Date. "2024-04-01") :to (js/Date. "2024-05-31")}})
    (rf/clear-subscription-cache!)
    (is (= [2 3]
          (map :id @(rf/subscribe [::list-subs/filtered-items :items])))
      "Date range filter should include items inside bounds")))

(deftest total-pages-test
  (testing "total-pages calculates ceiling of total/per-page"
    (reset-db! {:entities {:items {:data (into {} (map (fn [id] [id {:id id}]) (range 1 26)))
                                   :ids (vec (range 1 26))}}
                :ui {:lists {:items {:per-page 10 :pagination {:per-page 10}}}}})
    (is (= 3 @(rf/subscribe [::list-subs/total-pages :items])))))

(deftest total-pages-server-mode-uses-stored-total-items-test
  (testing "total-pages uses stored :total-items in :server mode"
    (reset-db! {:entities {:items {:data (into {} (map (fn [id] [id {:id id}]) (range 1 6)))
                                   :ids (vec (range 1 6))}}
                :ui {:lists {:items {:pagination-mode :server
                                     :total-items 42
                                     :per-page 10
                                     :pagination {:per-page 10}}}}})
    (is (= 5 @(rf/subscribe [::list-subs/total-pages :items]))
      "Server mode should derive page count from stored total-items, not loaded row count")))

(deftest total-pages-client-mode-remains-count-based-test
  (testing "total-pages remains loaded-item count based in :client mode"
    (reset-db! {:entities {:items {:data (into {} (map (fn [id] [id {:id id}]) (range 1 26)))
                                   :ids (vec (range 1 26))}}
                :ui {:lists {:items {:pagination-mode :client
                                     :total-items 999
                                     :per-page 10
                                     :pagination {:per-page 10}}}}})
    (is (= 3 @(rf/subscribe [::list-subs/total-pages :items]))
      "Client mode should ignore stored total-items and use loaded item count")))

(deftest selected-ids-test
  (testing "selected-ids falls back to empty set"
    (reset-db! {:ui {:lists {:items {}}}})
    (is (= #{} @(rf/subscribe [::list-subs/selected-ids :items]))))

  (testing "selected-ids returns stored set"
    (reset-db! {:ui {:lists {:items {:selected-ids #{1 2}}}}})
    (is (= #{1 2} @(rf/subscribe [::list-subs/selected-ids :items])))))
