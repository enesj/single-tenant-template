(ns app.domain.backend.expenses.routes.duplicates-routes-test
  (:require
    [app.domain.backend.expenses.routes.duplicates :as duplicates-routes]
    [app.domain.backend.expenses.services.dedup-ignored-clusters :as ignored-clusters]
    [app.domain.backend.expenses.services.duplicates :as duplicates-svc]
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