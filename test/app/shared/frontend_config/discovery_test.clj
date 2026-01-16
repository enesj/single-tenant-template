(ns app.shared.frontend-config.discovery-test
  (:require
    [app.shared.frontend-config.discovery :as discovery]
    [clojure.test :refer [deftest is testing]]))

(deftest normalize-id-test
  (testing "underscores and dashes normalize to the same canonical form"
    (is (= "full-name" (discovery/normalize-id :full_name)))
    (is (= "full-name" (discovery/normalize-id "full-name")))
    (is (= "audit-logs" (discovery/normalize-entity-id :audit_logs)))))

(deftest discover-domain-names-test
  (let [root "test/fixtures/frontend-config/domains"]
    (testing "discovers domains with config/"
      (is (= ["alpha" "beta"]
            (discovery/discover-domain-names root))))
    (testing "only/skip filters"
      (is (= ["beta"]
            (discovery/discover-domain-names root {:only ["beta"]})))
      (is (= ["alpha"]
            (discovery/discover-domain-names root {:skip ["beta"]}))))))