(ns app.domain.backend.expenses.routes.duplicates-routes-test
  (:require
    [app.domain.backend.expenses.handlers.search.entity-queries :as entity-queries]
    [app.domain.backend.expenses.routes.duplicates :as duplicates-routes]
    [app.domain.backend.expenses.services.dedup-ignored-clusters :as ignored-clusters]
    [app.domain.backend.expenses.services.duplicates :as duplicates-svc]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(deftest elevated-role-middleware-requires-admin-tier-test
  (testing "support is rejected while admin-tier roles are allowed"
    (let [wrapped (#'duplicates-routes/require-elevated-admin-role
                   (fn [_request] {:status 204}))]
      (is (= 403 (:status (wrapped {:admin {:role "support"}}))))
      (is (= 204 (:status (wrapped {:admin {:role "admin"}}))))
      (is (= 204 (:status (wrapped {:admin {:role "owner"}}))))
      (is (= 204 (:status (wrapped {:admin {:role "super_admin"}})))))))

(deftest detect-handler-clamps-fetch-limit-query-param-test
  (testing "detect handler exposes bounded fetch-limit knob"
    (let [captured-opts (atom nil)
          handler (#'duplicates-routes/detect-handler :db)]
      (with-redefs [duplicates-svc/detect-duplicates
                    (fn [_db _entity-type _strategy opts]
                      (reset! captured-opts opts)
                      [])
                    duplicates-svc/attach-cluster-ids (fn [_entity-type clusters] clusters)
                    ignored-clusters/list-ignored-cluster-ids (fn [_db _admin-id _entity-type] #{})
                    duplicates-svc/filter-ignored-clusters (fn [_ignored clusters] clusters)
                    duplicates-svc/enrich-with-usage-counts (fn [_db _entity-type clusters] clusters)]
        (doseq [[raw-value expected] [["999999" 20000]
                                      ["0" 1]
                                      ["bad" 5000]]]
          (let [response (handler {:admin {:id #uuid "00000000-0000-0000-0000-000000000001"}
                                   :query-params {"entity-type" "suppliers"
                                                  "strategy" "prefix"
                                                  "fetch-limit" raw-value}})]
            (is (= 200 (:status response)))
            (is (= expected (:fetch-limit @captured-opts)))))))))

(deftest detect-handler-defaults-prefix-words-by-entity-type-test
  (testing "supplier prefix detection defaults to one token while others stay stricter"
    (let [captured-opts (atom [])
          handler (#'duplicates-routes/detect-handler :db)]
      (with-redefs [duplicates-svc/detect-duplicates
                    (fn [_db entity-type strategy opts]
                      (swap! captured-opts conj {:entity-type entity-type
                                                 :strategy strategy
                                                 :opts opts})
                      [])
                    duplicates-svc/attach-cluster-ids (fn [_entity-type clusters] clusters)
                    ignored-clusters/list-ignored-cluster-ids (fn [_db _admin-id _entity-type] #{})
                    duplicates-svc/filter-ignored-clusters (fn [_ignored clusters] clusters)
                    duplicates-svc/enrich-with-usage-counts (fn [_db _entity-type clusters] clusters)]
        (doseq [[entity-type expected-prefix] [["suppliers" 1]
                                               ["articles" 2]]]
          (let [response (handler {:admin {:id #uuid "00000000-0000-0000-0000-000000000001"}
                                   :query-params {"entity-type" entity-type
                                                  "strategy" "prefix"}})]
            (is (= 200 (:status response)))
            (is (= expected-prefix
                  (get-in (last @captured-opts) [:opts :prefix-words])))))))))

(deftest manual-search-handler-uses-entity-search-with-bounded-limit-test
  (testing "manual search dispatches to the selected entity search function"
    (let [captured-args (atom nil)
          handler (#'duplicates-routes/manual-search-handler :db)]
      (with-redefs [entity-queries/search-articles
                    (fn [_db query limit tenant-id]
                      (reset! captured-args [query limit tenant-id])
                      [{:id #uuid "00000000-0000-0000-0000-0000000000aa"
                        :canonical_name "Greek Yogurt"}])
                    duplicates-svc/enrich-members-with-context
                    (fn [_db _entity-type members]
                      (mapv #(assoc % :price-labels ["1.99 BAM"]) members))]
        (let [response (handler {:admin {:id #uuid "00000000-0000-0000-0000-000000000001"}
                                 :query-params {"entity-type" "articles"
                                                "q" "yog"
                                                "limit" "999"}})
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (= ["yog" 50 nil] @captured-args))
          (is (= [{:id "00000000-0000-0000-0000-0000000000aa"
                   :canonical-name "Greek Yogurt"
                   :price-labels ["1.99 BAM"]}]
                (:results body))))))))

(deftest manual-search-handler-returns-empty-results-for-short-queries-test
  (testing "manual search does not hit the database until the query is long enough"
    (let [search-called? (atom false)
          handler (#'duplicates-routes/manual-search-handler :db)]
      (with-redefs [entity-queries/search-suppliers
                    (fn [& _]
                      (reset! search-called? true)
                      [])]
        (let [response (handler {:admin {:id #uuid "00000000-0000-0000-0000-000000000001"}
                                 :query-params {"entity-type" "suppliers"
                                                "q" "a"}})
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (false? @search-called?))
          (is (= [] (:results body))))))))