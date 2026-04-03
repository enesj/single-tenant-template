(ns app.shared.frontend-config.sync-test
  (:require
    [app.shared.frontend-config.sync :as sync]
    [clojure.test :refer [deftest is testing]]))

(deftest plan-sync-removes-stale-entities-entries
  (let [schema-index {:entities #{"receipts" "users"}}
        domain-bundle {:scope :domain
                       :domain "expenses"
                       :paths {:entities "tmp/domain-entities.edn"}
                       :data {:entities {:receipts {:title "Receipts"}
                                         :legacy-items {:title "Legacy Items"}}}}
        admin-bundle {:scope :admin
                      :domain nil
                      :paths {:entities "tmp/admin-entities.edn"}
                      :data {:entities {:users {:entity-key :users}
                                        :legacy-admin {:entity-key :legacy-admin}}}}
        patches (sync/plan-sync [domain-bundle admin-bundle] schema-index nil)
        domain-patch (some #(when (and (= :entities (:kind %))
                                    (= :domain (:scope %)))
                              %)
                       patches)
        admin-patch (some #(when (and (= :entities (:kind %))
                                   (= :admin (:scope %)))
                             %)
                      patches)]
    (testing "domain entities bundles mark unknown top-level entities for removal"
      (is (= #{:legacy-items} (set (:remove-entities domain-patch))))
      (is (= {:legacy-items {:remove-entity? true}}
            (:summary domain-patch)))
      (is (true? (:has-changes? domain-patch))))
    (testing "admin entities bundles also clean stale registry entries"
      (is (= #{:legacy-admin} (set (:remove-entities admin-patch))))
      (is (= {:legacy-admin {:remove-entity? true}}
            (:summary admin-patch)))
      (is (true? (:has-changes? admin-patch))))))
