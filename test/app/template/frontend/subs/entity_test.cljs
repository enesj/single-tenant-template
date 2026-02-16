(ns app.template.frontend.subs.entity-test
  "Tests for entity subscriptions (filtering/sorting/pagination)."
  (:require
    [app.template.frontend.subs.entity :as entity-subs]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  [db]
  (reset! rf-db/app-db db)
  (rf/clear-subscription-cache!))

(deftest sorted-entities-select-sorts-by-label-test
  (testing "sorted-entities sorts select/FK columns by referenced label when available"
    (reset-db!
      {:entities {:expenses {:data {1 {:id 1 :supplier-id "a"}
                                    2 {:id 2 :supplier-id "b"}}
                             :ids [1 2]}
                  :suppliers {:data {"a" {:id "a" :display-name "Zeta"}
                                     "b" {:id "b" :display-name "Alpha"}}
                              :ids ["a" "b"]}
                  :specs {:expenses [{:id "supplier-id"
                                      :type "select"
                                      :options [:suppliers :display-name]}]}}
       :ui {:lists {:expenses {:sort {:field :supplier-id :direction :asc}}}}})

    ;; Ascending by supplier label => Alpha (b) before Zeta (a)
    (is (= [2 1]
          (map :id @(rf/subscribe [::entity-subs/sorted-entities :expenses]))))

    ;; Descending should reverse that order
    (swap! rf-db/app-db assoc-in [:ui :lists :expenses :sort :direction] :desc)
    (rf/clear-subscription-cache!)
    (is (= [1 2]
          (map :id @(rf/subscribe [::entity-subs/sorted-entities :expenses]))))))

(deftest sorted-entities-select-falls-back-to-raw-id-test
  (testing "sorted-entities falls back to raw IDs when referenced entities are not loaded"
    (reset-db!
      {:entities {:expenses {:data {1 {:id 1 :supplier-id "a"}
                                    2 {:id 2 :supplier-id "b"}}
                             :ids [1 2]}
                  :specs {:expenses [{:id "supplier-id"
                                      :type "select"
                                      :options [:suppliers :display-name]}]}}
       :ui {:lists {:expenses {:sort {:field :supplier-id :direction :asc}}}}})

    ;; With no suppliers loaded, we sort by the raw supplier-id values: a then b
    (is (= [1 2]
          (map :id @(rf/subscribe [::entity-subs/sorted-entities :expenses]))))))

(deftest paginated-entities-server-mode-bypasses-client-transforms-test
  (testing "paginated-entities returns loaded rows unchanged in :server mode"
    (reset-db!
      {:entities {:expenses {:data {1 {:id 1 :description "Alpha" :amount 100}
                                    2 {:id 2 :description "Beta" :amount 300}
                                    3 {:id 3 :description "Gamma" :amount 200}}
                             :ids [1 2 3]}
                  :specs {:expenses [{:id "amount"}]}}
       :ui {:lists {:expenses {:pagination-mode :server
                               :sort {:field :amount :direction :desc}
                               :filters {:description "Alpha"}
                               :per-page 1
                               :pagination {:current-page 2 :per-page 1}}}}})

    (is (= [1 2 3]
          (map :id @(rf/subscribe [::entity-subs/paginated-entities :expenses])))
      "Server mode should bypass filter/sort/drop-take transforms")))

(deftest paginated-entities-client-mode-remains-paginated-test
  (testing "paginated-entities still applies client-side sort/pagination in :client mode"
    (reset-db!
      {:entities {:expenses {:data {1 {:id 1 :description "Alpha" :amount 100}
                                    2 {:id 2 :description "Beta" :amount 300}
                                    3 {:id 3 :description "Gamma" :amount 200}}
                             :ids [1 2 3]}
                  :specs {:expenses [{:id "amount"}]}}
       :ui {:lists {:expenses {:pagination-mode :client
                               :sort {:field :amount :direction :desc}
                               :per-page 1
                               :pagination {:current-page 2 :per-page 1}}}}})

    (is (= [3]
          (map :id @(rf/subscribe [::entity-subs/paginated-entities :expenses])))
      "Client mode should keep existing sort + page slicing behavior")))
