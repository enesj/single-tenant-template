(ns app.shared.specs.frontend-config-files-test
  (:require
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]))

(defn- read-edn
  [path]
  (-> path io/file slurp edn/read-string))

(deftest admin-config-edns-validate
  (testing "Admin frontend config EDNs validate against shared specs"
    (is (:valid?
         (entities-spec/validate-admin-entities-strict
           (read-edn "src/app/admin/frontend/config/entities.edn"))))

    (is (:valid?
         (view-options-spec/validate-view-options-strict
           (read-edn "src/app/admin/frontend/config/view-options.edn"))))

    (is (:valid?
         (form-fields-spec/validate-form-fields-strict
           (read-edn "src/app/admin/frontend/config/form-fields.edn"))))

    (is (:valid?
         (table-columns-spec/validate-table-columns-strict
           (read-edn "src/app/admin/frontend/config/table-columns.edn"))))))

(deftest domain-config-edns-validate
  (testing "Domain (user-facing) config EDNs validate against shared specs"
    (is (:valid?
         (entities-spec/validate-user-entities
           (read-edn "src/app/domain/frontend/expenses/config/entities.edn"))))

    (is (:valid?
         (view-options-spec/validate-view-options-strict
           (read-edn "src/app/domain/frontend/expenses/config/view-options.edn"))))

    (is (:valid?
         (form-fields-spec/validate-form-fields-strict
           (read-edn "src/app/domain/frontend/expenses/config/form-fields.edn"))))

    (is (:valid?
         (table-columns-spec/validate-table-columns-strict
           (read-edn "src/app/domain/frontend/expenses/config/table-columns.edn"))))))

(deftest table-columns-strict-subset-check
  (testing "Strict validation rejects non-subset columns"
    (is (false?
          (:valid?
           (table-columns-spec/validate-table-columns-strict
             {:expenses {:available-columns ["a"]
                         :default-visible-columns ["b"]}})))))

  (testing "Strict validation treats keyword/string IDs consistently"
    (is (:valid?
         (table-columns-spec/validate-table-columns-strict
           {:expenses {:available-columns [:id]
                       :default-visible-columns ["id"]
                       :always-visible ["id"]}})))))

(deftest form-fields-strict-detects-snake-and-kebab-duplicates
  (testing "Strict validation rejects duplicate field ids across snake_case and kebab-case spellings"
    (let [result (form-fields-spec/validate-form-fields-strict
                   {:expenses {:create-fields ["purchased_at" "purchased-at"]
                               :edit-fields [:supplier_id "supplier-id"]}})]
      (is (false? (:valid? result)))
      (is (re-find #"create-fields" (str (:errors result))))
      (is (re-find #"edit-fields" (str (:errors result)))))))

(deftest sanitize-form-fields-normalizes-and-dedupes-runtime-snapshots
  (testing "sanitize-form-fields canonicalizes mixed ids while preserving first-seen id style"
    (is (= {:expenses {:create-fields ["purchased_at" :supplier_id]
                       :edit-fields [:purchased_at "total_amount"]
                       :field-config {:purchased_at {:type "datetime-local"}}}}
          (form-fields-spec/sanitize-form-fields
            {:expenses {:create-fields ["purchased-at" "purchased_at" :supplier-id]
                        :edit-fields [:purchased_at "purchased-at" "total-amount" "total_amount"]
                        :field-config {:purchased-at {:type "datetime-local"}}}})))))
