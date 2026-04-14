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

(deftest plan-sync-keeps-allowlisted-frontend-only-entities
  (let [schema-index {:entities #{"receipts"}
                      :entity->fields {"receipts" {:raw ["id"]
                                                    :canonical #{"id"}}}}
        allowlist {:unmapped-aliases ["supplier_display_name"
                                      "raw_label"
                                      "occurrence_count"
                                      "supplier_id"]
                   :tenant-members ["member_name"
                                    "member_email"
                                    "joined_on"]}
        domain-bundle {:scope :domain
                       :domain "expenses"
                       :paths {:entities "tmp/domain-entities.edn"
                               :table-columns "tmp/domain-table-columns.edn"
                               :view-options "tmp/domain-view-options.edn"}
                       :data {:entities {:unmapped-aliases {:title "Unmapped aliases"}}
                              :table-columns {:unmapped-aliases {:available-columns ["supplier_display_name"
                                                                                     "raw_label"
                                                                                     "occurrence_count"]
                                                                  :default-visible-columns ["raw_label"]
                                                                  :filterable-columns ["supplier_display_name"]
                                                                  :sortable-columns ["occurrence_count"]
                                                                  :always-visible ["raw_label"]
                                                                  :column-config {:supplier_id {:type "text"}}}}
                              :view-options {:unmapped-aliases {:column-defaults {}
                                                                :column-locks {}}}}}
        template-bundle {:scope :domain
                         :domain "template"
                         :paths {:entities "tmp/template-entities.edn"}
                         :data {:entities {:tenant-members {:title "Tenant members"}}}}
        patches (sync/plan-sync [domain-bundle template-bundle] schema-index allowlist)]
    (testing "allowlisted synthetic entities are preserved"
      (is (every? (comp empty? :unknown-entities) patches))
      (is (every? (comp not :has-changes?) patches))
      (is (every? empty? (map #(or (:remove-entities %) []) patches))))))
