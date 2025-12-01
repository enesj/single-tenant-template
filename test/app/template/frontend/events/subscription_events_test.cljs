(ns app.template.frontend.events.subscription-events-test
  "Tests for subscription event handlers"
  (:require
    [app.template.frontend.api.http :as http-api]
    [app.template.frontend.events.subscription]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [day8.re-frame.http-fx :as http-fx]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  []
  (reset! rf-db/app-db helpers/valid-test-db-state))

(deftest fetch-status-test
  (testing "Fetching subscription status toggles loading and issues GET"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::xhr)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [:subscription/fetch-status])
        (is (= :get (:method @captured)))
        (is (= "/api/v1/subscription/status" (:uri @captured))))))

  (testing "Success response stores status and clears error"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:subscription :error] "old")
    (rf/dispatch-sync [:subscription/fetch-status-success {:tier "pro"}])
    (let [db @rf-db/app-db]
      (is (= {:tier "pro"} (get-in db [:subscription :status])))
      (is (false? (get-in db [:subscription :loading?])))))

  (testing "Failure clears loading state"
    (reset-db!)
    (rf/dispatch-sync [:subscription/fetch-status-failure nil])
    (is (false? (get-in @rf-db/app-db [:subscription :loading?])))))

(deftest fetch-tiers-test
  (testing "fetch-tiers performs GET and stores tiers"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::xhr)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [:subscription/fetch-tiers])
        (is (= :get (:method @captured)))
        (is (= "/api/v1/subscription/tiers" (:uri @captured)))))
    (rf/dispatch-sync [:subscription/fetch-tiers-success {:tiers [{:id :basic}]}])
    (is (= [{:id :basic}] (get-in @rf-db/app-db [:subscription :tiers]))))

  (testing "fetch-tiers-failure triggers effect"
    (reset-db!)
    (rf/dispatch-sync [:subscription/fetch-tiers-failure nil])
    (is (map? @rf-db/app-db))))

(deftest create-update-cancel-tests
  (testing "Create posts payload"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::xhr)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [:subscription/create {:plan "pro"}])
        (is (= :post (:method @captured)))
        (is (= {:plan "pro"} (:params @captured)))
        (is (= "/api/v1/subscription/create" (:uri @captured))))))

  (testing "Create success clears loading"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:subscription :loading?] true)
    (rf/dispatch-sync [:subscription/create-success nil])
    (is (false? (get-in @rf-db/app-db [:subscription :loading?]))))

  (testing "Create failure clears loading"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:subscription :loading?] true)
    (rf/dispatch-sync [:subscription/create-failure nil])
    (is (false? (get-in @rf-db/app-db [:subscription :loading?]))))

  (testing "Update sends PUT and failure clears loading"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::xhr)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [:subscription/update {:plan "pro"}])
        (is (= :put (:method @captured)))
        (is (= "/api/v1/subscription/update" (:uri @captured)))))
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:subscription :loading?] true)
    (rf/dispatch-sync [:subscription/update-failure nil])
    (is (false? (get-in @rf-db/app-db [:subscription :loading?]))))

  (testing "Cancel optionally appends immediate flag"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::xhr)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [:subscription/cancel true])
        (is (= :delete (:method @captured)))
        (is (= "/api/v1/subscription/cancel?immediate=true" (:uri @captured)))))
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:subscription :loading?] true)
    (rf/dispatch-sync [:subscription/cancel-failure nil])
    (is (false? (get-in @rf-db/app-db [:subscription :loading?])))))

(deftest usage-check-tests
  (testing "Check usage limit builds correct query"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::xhr)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [:subscription/check-usage-limit :properties 12])
        (is (= "/api/v1/subscription/usage-check?metric=properties&current_value=12"
              (:uri @captured))))))

  (testing "Usage success updates metric data"
    (reset-db!)
    (rf/dispatch-sync [:subscription/check-usage-limit-success :properties {:limit 10}])
    (is (= {:limit 10} (get-in @rf-db/app-db [:subscription :usage-checks :properties]))))

  (testing "Usage failure triggers effect"
    (reset-db!)
    (rf/dispatch-sync [:subscription/check-usage-limit-failure nil])
    (is (map? @rf-db/app-db))))

(deftest ui-toggle-tests
  (testing "Upgrade and billing modal toggles"
    (reset-db!)
    (rf/dispatch-sync [:subscription/show-upgrade-modal])
    (is (true? (get-in @rf-db/app-db [:subscription :ui :show-upgrade-modal?])))
    (rf/dispatch-sync [:subscription/hide-upgrade-modal])
    (is (false? (get-in @rf-db/app-db [:subscription :ui :show-upgrade-modal?])))

    (rf/dispatch-sync [:subscription/show-billing-modal])
    (is (true? (get-in @rf-db/app-db [:subscription :ui :show-billing-modal?])))
    (rf/dispatch-sync [:subscription/hide-billing-modal])
    (is (false? (get-in @rf-db/app-db [:subscription :ui :show-billing-modal?]))))

  (testing "Initialize issues dispatch-n effect"
    (reset-db!)
    (rf/dispatch-sync [:subscription/initialize])
    (is (map? @rf-db/app-db))))
