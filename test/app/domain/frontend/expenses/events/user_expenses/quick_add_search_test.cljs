(ns app.domain.frontend.expenses.events.user-expenses.quick-add-search-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest fetch-quick-add-search-uses-trimmed-event-args
  (testing "fetch-quick-add-search sends the intended query params"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/fetch-quick-add-search :all "bi" {:supplier_id nil}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/quick-add-search" (sup/req-uri req)))
      (is (= {:type "all"
              :q "bi"}
            (sup/req-params req))))))

(deftest fetch-quick-add-search-includes-supplier-filter-when-present
  (testing "store, article, and all quick-add searches include supplier_id when provided"
    (doseq [entity-type [:store :article :all]]
      (sup/reset-db!)
      (rf/dispatch-sync [:user-expenses/fetch-quick-add-search entity-type "kon" {:supplier_id "sup-1"}])
      (let [req (sup/last-http-request)]
        (is (= :get (sup/req-method req)))
        (is (= {:type (name entity-type)
                :q "kon"
                :supplier_id "sup-1"}
              (sup/req-params req)))))))

(deftest fetch-quick-add-related-sends-correct-related-request
  (testing "focused quick picks fetch related records for supplier/store context"
    (doseq [[entity-type expected-type] [[:supplier "suppliers"]
                                         [:store "stores"]]]
      (sup/reset-db!)
      (rf/dispatch-sync [:user-expenses/fetch-quick-add-related entity-type "entity-1" 5])
      (let [req (sup/last-http-request)]
        (is (= :get (sup/req-method req)))
        (is (= "/api/v1/expenses/search/related" (sup/req-uri req)))
        (is (= {"type" expected-type
                "id" "entity-1"
                "limit" 5}
              (sup/req-params req)))))))

(deftest fetch-quick-add-history-sends-correct-request
  (testing "phase-1 history fetch hits the dedicated endpoint and forwards supplier filters"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/fetch-quick-add-history "sup-1" 10])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/quick-add-history" (sup/req-uri req)))
      (is (= {:limit 10
              :supplier_id "sup-1"}
            (sup/req-params req)))))
  (testing "global history fetch omits supplier_id when none is selected"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/fetch-quick-add-history nil 10])
    (is (= {:limit 10}
          (sup/req-params (sup/last-http-request))))))

(deftest clear-quick-add-search-resets-the-entity-slice
  (testing "clear-quick-add-search resets the selected search bucket"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :quick-add-search :all]
      {:query "bi"
       :loading? true
       :results [{:id 1}]})
    (rf/dispatch-sync [:user-expenses/clear-quick-add-search :all])
    (is (= {:query nil
            :loading? false
            :results []}
          (get-in @rf-db/app-db [:user-expenses :quick-add-search :all])))))

(deftest clear-quick-add-history-resets-history-slice
  (testing "clear-quick-add-history removes stale phase-1 history"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :quick-add-history]
      {:loading? true
       :loaded? true
       :stores [{:id 1}]
       :articles [{:id 2}]})
    (rf/dispatch-sync [:user-expenses/clear-quick-add-history])
    (is (= {:loading? false
            :loaded? false
            :stores []
            :articles []}
          (get-in @rf-db/app-db [:user-expenses :quick-add-history])))))

(deftest clear-quick-add-related-resets-related-slice
  (testing "clear-quick-add-related resets the related candidates state"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :quick-add-related]
      {:entity-type :supplier
       :entity-id "sup-1"
       :loading? true
       :related {:stores [{:id 1}]}})
    (rf/dispatch-sync [:user-expenses/clear-quick-add-related])
    (is (= {:entity-type nil
            :entity-id nil
            :loading? false
            :related {}}
          (get-in @rf-db/app-db [:user-expenses :quick-add-related])))))
