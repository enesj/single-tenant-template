(ns app.shared.frontend-config.core-test
  (:require
    [app.shared.frontend-config.core :as fc]
    [clojure.test :refer [deftest is testing]]))

(deftest normalize-id-test
  (testing "underscores and dashes normalize to the same canonical form"
    (is (= "full-name" (fc/normalize-id :full_name)))
    (is (= "full-name" (fc/normalize-id "full-name")))
    (is (= "audit-logs" (fc/normalize-entity-id :audit_logs)))))

(deftest discover-domain-names-test
  (let [root "test/fixtures/frontend-config/domains"]
    (testing "discovers domains with config/"
      (is (= ["alpha" "beta"]
            (fc/discover-domain-names root))))
    (testing "only/skip filters"
      (is (= ["beta"]
            (fc/discover-domain-names root {:only ["beta"]})))
      (is (= ["alpha"]
            (fc/discover-domain-names root {:skip ["beta"]}))))))
