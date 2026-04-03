(ns app.shared.frontend-config.export-test
  (:require
    [app.shared.frontend-config.export :as export]
    [clojure.test :refer [deftest is testing]]))

(deftest split-user-runtime-data-preserves-template-and-domain-ownership
  (let [bundles [{:scope :domain
                  :domain "template"
                  :paths {:entities "template-entities.edn"}
                  :data {:entities {:tenant-members {:title "Tenant Members"}
                                    :receipts {:title "Template Receipts"}}}}
                 {:scope :domain
                  :domain "expenses"
                  :paths {:entities "expenses-entities.edn"}
                  :data {:entities {:receipts {:title "Expenses Receipts"}
                                    :expenses {:title "Expenses"}}}}]
        runtime-data {:tenant-members {:title "Tenant Members (DB)"}
                      :receipts {:title "Expenses Receipts (DB)"}
                      :expenses {:title "Expenses (DB)"}
                      :suppliers {:title "Suppliers (DB)"}}
        split (export/split-user-runtime-data bundles :entities runtime-data)]
    (testing "template-owned keys stay in template"
      (is (= {:tenant-members {:title "Tenant Members (DB)"}}
            (:template split))))
    (testing "domain-owned keys and unknown keys land in the primary domain bundle"
      (is (= {:receipts {:title "Expenses Receipts (DB)"}
              :expenses {:title "Expenses (DB)"}
              :suppliers {:title "Suppliers (DB)"}}
            (:expenses split))))))

(deftest export-plan-builds-target-files-for-admin-and-user-runtime-data
  (let [bundles [{:scope :admin
                  :domain nil
                  :paths {:view-options "admin-view-options.edn"
                          :form-fields "admin-form-fields.edn"
                          :table-columns "admin-table-columns.edn"}
                  :data {:view-options {}
                         :form-fields {}
                         :table-columns {}}}
                 {:scope :domain
                  :domain "template"
                  :paths {:entities "template-entities.edn"}
                  :data {:entities {:tenant-members {:title "Tenant Members"}}}}
                 {:scope :domain
                  :domain "expenses"
                  :paths {:entities "expenses-entities.edn"
                          :view-options "expenses-view-options.edn"}
                  :data {:entities {:expenses {:title "Expenses"}}
                         :view-options {:expenses {:display-defaults {:show-filtering? true}}}}}]
        runtime-config {:admin {:view-options {:admins {:display-locks {:show-edit? true}}}
                                :form-fields {}
                                :table-columns {}}
                        :user {:entities {:tenant-members {:title "Tenant Members"}
                                          :expenses {:title "Expenses"}}
                               :view-options {:expenses {:display-defaults {:show-filtering? false}}}
                               :form-fields {}
                               :table-columns {}}}
        plan (export/export-plan bundles runtime-config)]
    (testing "admin runtime config exports to admin file targets"
      (is (= "admin-view-options.edn"
            (:path (first (filter #(and (= :admin (:scope %))
                                     (= :view-options (:kind %)))
                           plan))))))
    (testing "user runtime config exports to template and domain targets"
      (is (= {:tenant-members {:title "Tenant Members"}}
            (:data (first (filter #(= "template" (:domain %)) plan)))))
      (is (= {:expenses {:title "Expenses"}}
            (:data (first (filter #(and (= "expenses" (:domain %))
                                     (= :entities (:kind %)))
                           plan))))))))
